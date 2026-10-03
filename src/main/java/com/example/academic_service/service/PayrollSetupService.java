package com.example.academic_service.service;

import com.example.academic_service.dto.PayrollDtos.*;
import com.example.academic_service.entity.*;
import com.example.academic_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Salary setup. Accounts sets the salary parts, each designation's standard salary and
 * festival bonus, and the payroll accounts. HR then sets employees' own amounts (from a
 * month onward) and bonuses. An employee's part = their own amount if they have one for
 * that month, else the designation's.
 */
@Service
@RequiredArgsConstructor
public class PayrollSetupService {

    private static final long SETTINGS_ID = 1L;

    private final SalaryComponentRepository componentRepo;
    private final DesignationSalaryRepository designationSalaryRepo;
    private final StaffSalaryRepository staffSalaryRepo;
    private final BonusRuleRepository bonusRepo;
    private final PayrollSettingsRepository settingsRepo;
    private final PayslipLineRepository payslipLineRepo;
    private final DesignationRepository designationRepo;
    private final StaffRepository staffRepo;
    private final ChartOfAccountRepository coaRepo;

    // ── Salary parts ───────────────────────────────────────────────────────

    public List<ComponentResponse> components() {
        List<SalaryComponent> all = componentRepo.findAllByOrderBySortOrderAscIdAsc();
        Map<Long, ChartOfAccount> ledgers = new HashMap<>();
        coaRepo.findAllById(all.stream().map(SalaryComponent::getExpenseLedgerId).collect(Collectors.toSet()))
                .forEach(a -> ledgers.put(a.getId(), a));
        return all.stream().map(c -> toResponse(c, ledgers.get(c.getExpenseLedgerId()))).toList();
    }

    @Transactional
    public ComponentResponse saveComponent(Long id, ComponentRequest req) {
        SalaryComponent c = id == null ? new SalaryComponent()
                : componentRepo.findById(id).orElseThrow(() -> notFound("Salary part not found"));
        String code = req.code() == null ? "" : req.code().trim().toUpperCase();
        String name = req.name() == null ? "" : req.name().trim();
        if (code.isEmpty() || name.isEmpty()) throw bad("Code and name are required");
        boolean codeTaken = componentRepo.findAll().stream()
                .anyMatch(o -> !o.getId().equals(id) && o.getCode().equalsIgnoreCase(code));
        if (codeTaken) throw new ResponseStatusException(HttpStatus.CONFLICT, "Code '" + code + "' already exists");
        ChartOfAccount ledger = requireLeaf(req.expenseLedgerId(), AccountType.EXPENSE, "Expense account");
        c.setCode(code);
        c.setName(name);
        c.setExpenseLedgerId(ledger.getId());
        c.setIsBasic(Boolean.TRUE.equals(req.isBasic()));
        c.setSortOrder(req.sortOrder() != null ? req.sortOrder() : (id == null ? componentRepo.findAll().size() : c.getSortOrder()));
        c.setIsActive(req.isActive() == null || req.isActive());
        if (c.getIsBasic()) {
            // Only one basic: a festival bonus percentage needs one clear base.
            for (SalaryComponent other : componentRepo.findAll()) {
                if (!other.getId().equals(id) && Boolean.TRUE.equals(other.getIsBasic())) {
                    other.setIsBasic(false);
                    componentRepo.save(other);
                }
            }
        }
        SalaryComponent saved = componentRepo.save(c);
        return toResponse(saved, ledger);
    }

    @Transactional
    public void deleteComponent(Long id) {
        SalaryComponent c = componentRepo.findById(id).orElseThrow(() -> notFound("Salary part not found"));
        if (payslipLineRepo.countByComponentId(id) > 0 || staffSalaryRepo.countByComponentId(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This salary part is on payslips or employees' salaries. Turn it off instead of deleting it.");
        }
        designationSalaryRepo.deleteAll(designationSalaryRepo.findByComponentId(id));
        componentRepo.delete(c);
    }

    // ── Designation salaries + bonus ───────────────────────────────────────

    public List<DesignationPay> designations() {
        List<Designation> designations = designationRepo.findAll();
        designations.sort(Comparator.comparing(Designation::getName, String.CASE_INSENSITIVE_ORDER));
        Map<Integer, Map<Long, BigDecimal>> amounts = designationAmounts();
        Set<Long> active = activeComponentIds();
        Map<Integer, Long> staffCounts = staffRepo.findByIsActive(true).stream()
                .filter(s -> s.getCurrentDesignation() != null)
                .collect(Collectors.groupingBy(s -> s.getCurrentDesignation().getId(), Collectors.counting()));
        Map<Integer, BonusRule> bonuses = bonusRepo.findAll().stream()
                .filter(b -> b.getDesignationId() != null)
                .collect(Collectors.toMap(BonusRule::getDesignationId, b -> b));
        return designations.stream().map(d -> {
            Map<Long, BigDecimal> a = amounts.getOrDefault(d.getId(), Map.of());
            BigDecimal total = a.entrySet().stream().filter(e -> active.contains(e.getKey()))
                    .map(Map.Entry::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new DesignationPay(d.getId(), d.getName(), !Boolean.FALSE.equals(d.getIsActive()), a, total,
                    toDto(bonuses.get(d.getId())), staffCounts.getOrDefault(d.getId(), 0L));
        }).toList();
    }

    @Transactional
    public DesignationPay saveDesignation(Integer designationId, DesignationPayRequest req) {
        Designation d = designationRepo.findById(designationId).orElseThrow(() -> notFound("Designation not found"));
        Map<Long, SalaryComponent> components = componentRepo.findAll().stream()
                .collect(Collectors.toMap(SalaryComponent::getId, c -> c));
        Map<Long, DesignationSalary> existing = designationSalaryRepo.findByDesignationId(designationId).stream()
                .collect(Collectors.toMap(DesignationSalary::getComponentId, x -> x));
        if (req.amounts() != null) {
            for (Map.Entry<Long, BigDecimal> e : req.amounts().entrySet()) {
                if (!components.containsKey(e.getKey())) throw bad("Unknown salary part " + e.getKey());
                BigDecimal amount = e.getValue();
                if (amount != null && amount.signum() < 0) throw bad("Amounts can't be negative");
                DesignationSalary row = existing.get(e.getKey());
                if (amount == null || amount.signum() == 0) {
                    if (row != null) designationSalaryRepo.delete(row);
                } else {
                    if (row == null) {
                        row = new DesignationSalary();
                        row.setDesignationId(designationId);
                        row.setComponentId(e.getKey());
                    }
                    row.setAmount(money(amount));
                    designationSalaryRepo.save(row);
                }
            }
        }
        saveBonus(bonusRepo.findByDesignationId(designationId).orElse(null), req.bonus(), b -> b.setDesignationId(designationId));
        return designations().stream().filter(x -> x.designationId().equals(d.getId())).findFirst().orElseThrow();
    }

    // ── Payroll accounts ───────────────────────────────────────────────────

    public SettingsResponse settings() {
        PayrollSettings s = ensureSettings();
        return new SettingsResponse(
                s.getSalaryPayableAccountId(), accountLabel(s.getSalaryPayableAccountId()),
                s.getBonusExpenseAccountId(), accountLabel(s.getBonusExpenseAccountId()),
                s.getCashAccountId(), accountLabel(s.getCashAccountId()),
                s.getBankAccountId(), accountLabel(s.getBankAccountId()));
    }

    @Transactional
    public SettingsResponse saveSettings(SettingsDto req) {
        PayrollSettings s = ensureSettings();
        s.setSalaryPayableAccountId(optionalLeaf(req.salaryPayableAccountId(), AccountType.LIABILITY, "Salaries payable"));
        s.setBonusExpenseAccountId(optionalLeaf(req.bonusExpenseAccountId(), AccountType.EXPENSE, "Festival bonus"));
        s.setCashAccountId(optionalLeaf(req.cashAccountId(), AccountType.ASSET, "Cash"));
        s.setBankAccountId(optionalLeaf(req.bankAccountId(), AccountType.ASSET, "Bank"));
        settingsRepo.save(s);
        return settings();
    }

    public PayrollSettings ensureSettings() {
        return settingsRepo.findById(SETTINGS_ID).orElseGet(() -> {
            PayrollSettings s = new PayrollSettings();
            s.setId(SETTINGS_ID);
            return settingsRepo.save(s);
        });
    }

    // ── Staff salaries (HR) ────────────────────────────────────────────────

    public List<StaffPayRow> staff(LocalDate month) {
        LocalDate m = firstOfMonth(month);
        PayContext ctx = context(m);
        Map<Long, BonusRule> staffBonus = bonusRepo.findAll().stream().filter(b -> b.getStaffId() != null)
                .collect(Collectors.toMap(BonusRule::getStaffId, b -> b));
        Map<Integer, BonusRule> designationBonus = bonusRepo.findAll().stream().filter(b -> b.getDesignationId() != null)
                .collect(Collectors.toMap(BonusRule::getDesignationId, b -> b));
        List<Staff> all = staffRepo.findAll();
        all.sort(Comparator.comparing((Staff s) -> !Boolean.FALSE.equals(s.getIsActive()) ? 0 : 1)
                .thenComparing(s -> s.getNameEnglish() == null ? "" : s.getNameEnglish(), String.CASE_INSENSITIVE_ORDER));
        return all.stream().map(s -> {
            Map<Long, BigDecimal> amounts = ctx.amountsFor(s);
            BigDecimal total = amounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            Integer dId = s.getCurrentDesignation() != null ? s.getCurrentDesignation().getId() : null;
            BonusRule own = staffBonus.get(s.getId());
            BonusRule fromDesignation = dId != null ? designationBonus.get(dId) : null;
            return new StaffPayRow(s.getId(), s.getStaffSystemId(), s.getNameEnglish(), dId,
                    s.getCurrentDesignation() != null ? s.getCurrentDesignation().getName() : null,
                    s.getEmployeeType() != null ? s.getEmployeeType().name() : null,
                    !Boolean.FALSE.equals(s.getIsActive()), total, ctx.hasOwnAmounts(s.getId()),
                    toDto(own != null ? own : fromDesignation), own != null ? "STAFF" : fromDesignation != null ? "DESIGNATION" : null);
        }).toList();
    }

    public StaffPayDetail staffDetail(Long staffId, LocalDate month) {
        Staff s = staffRepo.findById(staffId).orElseThrow(() -> notFound("Staff not found"));
        LocalDate m = firstOfMonth(month);
        PayContext ctx = context(m);
        Integer dId = s.getCurrentDesignation() != null ? s.getCurrentDesignation().getId() : null;
        Map<Long, BigDecimal> desig = dId != null ? ctx.designationAmounts.getOrDefault(dId, Map.of()) : Map.of();
        List<StaffComponentPay> parts = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (SalaryComponent c : ctx.components) {
            StaffSalary own = ctx.latestOwn(s.getId(), c.getId());
            BigDecimal amount = own != null && own.getAmount() != null ? own.getAmount() : desig.get(c.getId());
            parts.add(new StaffComponentPay(c.getId(), c.getName(), Boolean.TRUE.equals(c.getIsBasic()), desig.get(c.getId()),
                    own != null ? own.getAmount() : null, own != null ? own.getEffectiveFrom() : null, amount));
            if (amount != null) total = total.add(amount);
        }
        Map<Long, String> names = componentRepo.findAll().stream()
                .collect(Collectors.toMap(SalaryComponent::getId, SalaryComponent::getName));
        List<StaffSalaryChange> history = staffSalaryRepo.findByStaffIdOrderByEffectiveFromAsc(staffId).stream()
                .sorted(Comparator.comparing(StaffSalary::getEffectiveFrom).reversed())
                .map(r -> new StaffSalaryChange(r.getEffectiveFrom(), r.getComponentId(), names.get(r.getComponentId()),
                        r.getAmount(), r.getCreatedBy(), r.getCreatedAt()))
                .toList();
        return new StaffPayDetail(s.getId(), s.getStaffSystemId(), s.getNameEnglish(), dId,
                s.getCurrentDesignation() != null ? s.getCurrentDesignation().getName() : null, m, parts, total, history,
                dId != null ? toDto(bonusRepo.findByDesignationId(dId).orElse(null)) : null,
                toDto(bonusRepo.findByStaffId(staffId).orElse(null)));
    }

    @Transactional
    public StaffPayDetail saveStaffSalary(Long staffId, StaffSalaryRequest req, String user) {
        staffRepo.findById(staffId).orElseThrow(() -> notFound("Staff not found"));
        if (req.effectiveFrom() == null) throw bad("Choose the month the new salary starts");
        LocalDate from = firstOfMonth(req.effectiveFrom());
        Set<Long> known = componentRepo.findAll().stream().map(SalaryComponent::getId).collect(Collectors.toSet());
        Map<Long, StaffSalary> sameMonth = staffSalaryRepo.findByStaffIdAndEffectiveFrom(staffId, from).stream()
                .collect(Collectors.toMap(StaffSalary::getComponentId, r -> r));
        PayContext before = context(from.minusMonths(1));
        if (req.amounts() != null) {
            for (Map.Entry<Long, BigDecimal> e : req.amounts().entrySet()) {
                if (!known.contains(e.getKey())) throw bad("Unknown salary part " + e.getKey());
                BigDecimal amount = e.getValue();
                if (amount != null && amount.signum() < 0) throw bad("Amounts can't be negative");
                StaffSalary row = sameMonth.get(e.getKey());
                StaffSalary previous = before.latestOwn(staffId, e.getKey());
                boolean hadOwnBefore = previous != null && previous.getAmount() != null;
                if (amount == null && !hadOwnBefore) {
                    // Following the designation already: nothing to record for this month.
                    if (row != null) staffSalaryRepo.delete(row);
                    continue;
                }
                if (row == null) {
                    row = new StaffSalary();
                    row.setStaffId(staffId);
                    row.setComponentId(e.getKey());
                    row.setEffectiveFrom(from);
                }
                row.setAmount(amount == null ? null : money(amount));
                row.setCreatedBy(user);
                staffSalaryRepo.save(row);
            }
        }
        return staffDetail(staffId, from);
    }

    /** Removes everything recorded for one month (the months before and after stay). */
    @Transactional
    public void deleteStaffSalaryMonth(Long staffId, LocalDate month) {
        staffSalaryRepo.deleteAll(staffSalaryRepo.findByStaffIdAndEffectiveFrom(staffId, firstOfMonth(month)));
    }

    @Transactional
    public StaffPayDetail saveStaffBonus(Long staffId, BonusRuleDto bonus) {
        staffRepo.findById(staffId).orElseThrow(() -> notFound("Staff not found"));
        saveBonus(bonusRepo.findByStaffId(staffId).orElse(null), bonus, b -> b.setStaffId(staffId));
        return staffDetail(staffId, LateFeeService.today());
    }

    // ── Calculation (shared with payroll runs) ─────────────────────────────

    /** Everything needed to work out salaries for one month, loaded once. */
    public final class PayContext {
        final LocalDate month;
        final List<SalaryComponent> components;
        final Map<Integer, Map<Long, BigDecimal>> designationAmounts;
        /** staffId → componentId → rows with effectiveFrom ≤ month, oldest first. */
        final Map<Long, Map<Long, List<StaffSalary>>> own;

        PayContext(LocalDate month) {
            this.month = month;
            this.components = componentRepo.findAllByOrderBySortOrderAscIdAsc().stream()
                    .filter(c -> Boolean.TRUE.equals(c.getIsActive())).toList();
            this.designationAmounts = designationAmounts();
            this.own = new HashMap<>();
            for (StaffSalary r : staffSalaryRepo.findByEffectiveFromLessThanEqual(month)) {
                own.computeIfAbsent(r.getStaffId(), k -> new HashMap<>())
                        .computeIfAbsent(r.getComponentId(), k -> new ArrayList<>()).add(r);
            }
            own.values().forEach(m -> m.values().forEach(l -> l.sort(Comparator.comparing(StaffSalary::getEffectiveFrom))));
        }

        public List<SalaryComponent> components() { return components; }

        StaffSalary latestOwn(Long staffId, Long componentId) {
            List<StaffSalary> rows = own.getOrDefault(staffId, Map.of()).get(componentId);
            return rows == null || rows.isEmpty() ? null : rows.get(rows.size() - 1);
        }

        boolean hasOwnAmounts(Long staffId) {
            return own.getOrDefault(staffId, Map.of()).keySet().stream()
                    .anyMatch(c -> { StaffSalary r = latestOwn(staffId, c); return r != null && r.getAmount() != null; });
        }

        /** componentId → amount (> 0 only), in the parts' order. */
        public Map<Long, BigDecimal> amountsFor(Staff s) {
            Integer dId = s.getCurrentDesignation() != null ? s.getCurrentDesignation().getId() : null;
            Map<Long, BigDecimal> desig = dId != null ? designationAmounts.getOrDefault(dId, Map.of()) : Map.of();
            Map<Long, BigDecimal> out = new LinkedHashMap<>();
            for (SalaryComponent c : components) {
                StaffSalary r = latestOwn(s.getId(), c.getId());
                BigDecimal amount = r != null && r.getAmount() != null ? r.getAmount() : desig.get(c.getId());
                if (amount != null && amount.signum() > 0) out.put(c.getId(), amount);
            }
            return out;
        }

        /** The festival bonus for one employee (their own rule, else their designation's); 0 = none. */
        public BigDecimal bonusFor(Staff s, Map<Long, BonusRule> staffRules, Map<Integer, BonusRule> designationRules) {
            BonusRule rule = staffRules.get(s.getId());
            if (rule == null && s.getCurrentDesignation() != null) rule = designationRules.get(s.getCurrentDesignation().getId());
            if (rule == null || rule.getValue() == null) return BigDecimal.ZERO;
            if (rule.getBonusType() == BonusType.FIXED) return money(rule.getValue());
            Map<Long, BigDecimal> amounts = amountsFor(s);
            BigDecimal basic = components.stream().filter(c -> Boolean.TRUE.equals(c.getIsBasic()))
                    .map(c -> amounts.getOrDefault(c.getId(), BigDecimal.ZERO)).findFirst().orElse(BigDecimal.ZERO);
            return basic.multiply(rule.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
    }

    public PayContext context(LocalDate month) {
        return new PayContext(firstOfMonth(month));
    }

    public Map<Long, BonusRule> staffBonusRules() {
        return bonusRepo.findAll().stream().filter(b -> b.getStaffId() != null)
                .collect(Collectors.toMap(BonusRule::getStaffId, b -> b));
    }

    public Map<Integer, BonusRule> designationBonusRules() {
        return bonusRepo.findAll().stream().filter(b -> b.getDesignationId() != null)
                .collect(Collectors.toMap(BonusRule::getDesignationId, b -> b));
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    public static LocalDate firstOfMonth(LocalDate d) {
        return (d != null ? d : LateFeeService.today()).withDayOfMonth(1);
    }

    private Map<Integer, Map<Long, BigDecimal>> designationAmounts() {
        Map<Integer, Map<Long, BigDecimal>> out = new HashMap<>();
        for (DesignationSalary r : designationSalaryRepo.findAll()) {
            out.computeIfAbsent(r.getDesignationId(), k -> new HashMap<>()).put(r.getComponentId(), r.getAmount());
        }
        return out;
    }

    private Set<Long> activeComponentIds() {
        return componentRepo.findAll().stream().filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .map(SalaryComponent::getId).collect(Collectors.toSet());
    }

    private void saveBonus(BonusRule existing, BonusRuleDto dto, java.util.function.Consumer<BonusRule> owner) {
        if (dto == null || dto.bonusType() == null || dto.value() == null || dto.value().signum() == 0) {
            if (existing != null) bonusRepo.delete(existing);
            return;
        }
        if (dto.value().signum() < 0) throw bad("The bonus can't be negative");
        if (dto.bonusType() == BonusType.PERCENT_OF_BASIC && dto.value().compareTo(BigDecimal.valueOf(1000)) > 0) {
            throw bad("The bonus percentage looks too high");
        }
        BonusRule b = existing != null ? existing : new BonusRule();
        owner.accept(b);
        b.setBonusType(dto.bonusType());
        b.setValue(money(dto.value()));
        bonusRepo.save(b);
    }

    private static BonusRuleDto toDto(BonusRule b) {
        return b == null ? null : new BonusRuleDto(b.getBonusType(), b.getValue());
    }

    private ComponentResponse toResponse(SalaryComponent c, ChartOfAccount ledger) {
        return new ComponentResponse(c.getId(), c.getCode(), c.getName(), c.getExpenseLedgerId(),
                ledger != null ? ledger.getAccountCode() : null, ledger != null ? ledger.getAccountName() : null,
                Boolean.TRUE.equals(c.getIsBasic()), c.getSortOrder() != null ? c.getSortOrder() : 0,
                Boolean.TRUE.equals(c.getIsActive()));
    }

    private String accountLabel(Long id) {
        if (id == null) return null;
        return coaRepo.findById(id).map(a -> a.getAccountCode() + " — " + a.getAccountName()).orElse(null);
    }

    private Long optionalLeaf(Long id, AccountType type, String label) {
        return id == null ? null : requireLeaf(id, type, label).getId();
    }

    ChartOfAccount requireLeaf(Long id, AccountType type, String label) {
        if (id == null) throw bad(label + " is required");
        ChartOfAccount a = coaRepo.findById(id).orElseThrow(() -> bad(label + ": account not found"));
        if (a.getAccountType() != type) throw bad(label + " must be a " + type + " account");
        if (Boolean.TRUE.equals(a.getIsGroup())) throw bad(label + " must be a ledger account, not a group");
        if (Boolean.FALSE.equals(a.getIsActive())) throw bad(label + " account is inactive");
        return a;
    }

    static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static ResponseStatusException bad(String m) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
    }

    private static ResponseStatusException notFound(String m) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, m);
    }
}
