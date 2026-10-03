package com.example.academic_service.service;

import com.example.academic_service.dto.JournalEntryRequest;
import com.example.academic_service.dto.JournalLineRequest;
import com.example.academic_service.dto.PayrollDtos.*;
import com.example.academic_service.entity.*;
import com.example.academic_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Payroll runs. A run is a month's salaries (one per month) or a festival bonus.
 *   DRAFT      — payslips made from the salary setup; amounts can be changed, people removed,
 *                or the whole draft rebuilt from the setup.
 *   FINALISED  — Dr each expense account / Cr Salaries Payable. Payslips are then paid one,
 *                several or all at a time, always in full: Dr Salaries Payable / Cr Cash or Bank.
 *   CANCELLED  — a finalised run reversed before anyone was paid.
 */
@Service
@RequiredArgsConstructor
public class PayrollRunService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy");

    private final PayrollRunRepository runRepo;
    private final PayslipRepository payslipRepo;
    private final PayslipLineRepository lineRepo;
    private final StaffRepository staffRepo;
    private final PayrollSetupService setup;
    private final JournalEntryService journalService;

    // ── Read ───────────────────────────────────────────────────────────────

    public List<RunSummary> list() {
        List<PayrollRun> runs = runRepo.findAllByOrderByPeriodDescIdDesc();
        Map<Long, List<Payslip>> slips = payslipRepo.findByPayrollRunIdIn(runs.stream().map(PayrollRun::getId).toList())
                .stream().collect(Collectors.groupingBy(Payslip::getPayrollRunId));
        return runs.stream().map(r -> summary(r, slips.getOrDefault(r.getId(), List.of()))).toList();
    }

    public RunDetail get(Long id) {
        return detail(find(id), List.of());
    }

    /** One employee's payslips from finalised runs, newest first. */
    public List<StaffPayslip> forStaff(Long staffId) {
        List<Payslip> slips = payslipRepo.findByStaffIdOrderByIdDesc(staffId);
        Map<Long, PayrollRun> runs = runRepo.findAllById(slips.stream().map(Payslip::getPayrollRunId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(PayrollRun::getId, r -> r));
        Map<Long, List<PayslipLine>> lines = linesOf(slips);
        Staff staff = staffRepo.findById(staffId).orElse(null);
        return slips.stream()
                .filter(p -> runs.containsKey(p.getPayrollRunId()) && runs.get(p.getPayrollRunId()).getStatus() == PayrollRunStatus.FINALISED)
                .map(p -> new StaffPayslip(summary(runs.get(p.getPayrollRunId()), List.of(p)), toDto(p, lines.get(p.getId()), staff)))
                .toList();
    }

    // ── Create / rebuild a draft ───────────────────────────────────────────

    @Transactional
    public RunDetail create(RunCreateRequest req, String user) {
        if (req.runType() == null) throw bad("Choose salary or festival bonus");
        if (req.period() == null) throw bad("Choose the month");
        LocalDate period = PayrollSetupService.firstOfMonth(req.period());
        String title;
        if (req.runType() == PayrollRunType.SALARY) {
            if (!runRepo.findByRunTypeAndPeriodAndStatusNot(PayrollRunType.SALARY, period, PayrollRunStatus.CANCELLED).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "There is already a salary payroll for " + period.format(MONTH));
            }
            title = "Salary — " + period.format(MONTH);
        } else {
            title = req.title() == null || req.title().isBlank() ? null : req.title().trim();
            if (title == null) throw bad("Name the bonus, e.g. Eid-ul-Fitr Bonus 2026");
            if (title.length() > 200) throw bad("The name is too long");
            if (setup.ensureSettings().getBonusExpenseAccountId() == null) {
                throw bad("Set the festival bonus expense account in Payroll Setup first");
            }
        }
        PayrollRun run = new PayrollRun();
        run.setRunType(req.runType());
        run.setPeriod(period);
        run.setTitle(title);
        run.setCreatedBy(user);
        run = runRepo.save(run);
        List<String> warnings = fill(run);
        return detail(run, warnings);
    }

    /** Throws the draft's payslips away and makes them again from the current setup. */
    @Transactional
    public RunDetail rebuild(Long id) {
        PayrollRun run = draft(id);
        for (Payslip p : payslipRepo.findByPayrollRunIdOrderByStaffNameAsc(id)) {
            lineRepo.deleteByPayslipId(p.getId());
            payslipRepo.delete(p);
        }
        payslipRepo.flush();
        List<String> warnings = fill(run);
        return detail(run, warnings);
    }

    private List<String> fill(PayrollRun run) {
        PayrollSetupService.PayContext ctx = setup.context(run.getPeriod());
        Map<Long, SalaryComponent> components = ctx.components().stream()
                .collect(Collectors.toMap(SalaryComponent::getId, c -> c));
        PayrollSettings settings = setup.ensureSettings();
        Map<Long, BonusRule> staffRules = setup.staffBonusRules();
        Map<Integer, BonusRule> designationRules = setup.designationBonusRules();
        List<String> skipped = new ArrayList<>();
        int made = 0;
        for (Staff s : staffRepo.findByIsActive(true)) {
            List<PayslipLine> lines = new ArrayList<>();
            if (run.getRunType() == PayrollRunType.SALARY) {
                int order = 0;
                for (Map.Entry<Long, BigDecimal> e : ctx.amountsFor(s).entrySet()) {
                    SalaryComponent c = components.get(e.getKey());
                    lines.add(line(c.getId(), c.getName(), e.getValue(), c.getExpenseLedgerId(), order++));
                }
            } else {
                BigDecimal bonus = ctx.bonusFor(s, staffRules, designationRules);
                if (bonus.signum() > 0) lines.add(line(null, run.getTitle(), bonus, settings.getBonusExpenseAccountId(), 0));
            }
            if (lines.isEmpty()) {
                skipped.add(s.getNameEnglish());
                continue;
            }
            Payslip p = new Payslip();
            p.setPayrollRunId(run.getId());
            p.setStaffId(s.getId());
            p.setStaffSystemId(s.getStaffSystemId());
            p.setStaffName(s.getNameEnglish() != null ? s.getNameEnglish() : "Staff #" + s.getId());
            p.setDesignationName(s.getCurrentDesignation() != null ? s.getCurrentDesignation().getName() : null);
            p.setTotalAmount(lines.stream().map(PayslipLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
            p = payslipRepo.save(p);
            for (PayslipLine l : lines) {
                l.setPayslipId(p.getId());
                lineRepo.save(l);
            }
            made++;
        }
        List<String> warnings = new ArrayList<>();
        if (made == 0) {
            warnings.add(run.getRunType() == PayrollRunType.SALARY
                    ? "Nobody has a salary yet. Set designation salaries in Payroll Setup, or employees' own salaries in HR."
                    : "Nobody gets this bonus yet. Set festival bonuses for designations in Payroll Setup, or for employees in HR.");
        } else if (!skipped.isEmpty()) {
            warnings.add(skipped.size() + " active employee(s) left out because they have "
                    + (run.getRunType() == PayrollRunType.SALARY ? "no salary" : "no festival bonus") + " set: "
                    + String.join(", ", skipped.subList(0, Math.min(10, skipped.size())))
                    + (skipped.size() > 10 ? "…" : ""));
        }
        return warnings;
    }

    // ── Edit a draft ───────────────────────────────────────────────────────

    @Transactional
    public PayslipDto updateLines(Long payslipId, PayslipLinesRequest req) {
        Payslip p = payslipRepo.findById(payslipId).orElseThrow(() -> notFound("Payslip not found"));
        PayrollRun run = draft(p.getPayrollRunId());
        if (req.lines() == null || req.lines().isEmpty()) throw bad("A payslip needs at least one amount");
        PayrollSettings settings = setup.ensureSettings();
        Map<Long, SalaryComponent> components = setup.context(run.getPeriod()).components().stream()
                .collect(Collectors.toMap(SalaryComponent::getId, c -> c));
        List<PayslipLine> lines = new ArrayList<>();
        int order = 0;
        for (LineAmount la : req.lines()) {
            if (la.amount() == null || la.amount().signum() < 0) throw bad("Amounts must be zero or more");
            if (la.amount().signum() == 0) continue;
            if (run.getRunType() == PayrollRunType.BONUS) {
                lines.add(line(null, run.getTitle(), la.amount(), settings.getBonusExpenseAccountId(), order++));
            } else {
                SalaryComponent c = components.get(la.componentId());
                if (c == null) throw bad("Unknown or inactive salary part");
                lines.add(line(c.getId(), c.getName(), la.amount(), c.getExpenseLedgerId(), order++));
            }
        }
        if (lines.isEmpty()) throw bad("A payslip needs at least one amount. Remove the employee from the payroll instead.");
        lineRepo.deleteByPayslipId(p.getId());
        lineRepo.flush();
        for (PayslipLine l : lines) {
            l.setPayslipId(p.getId());
            lineRepo.save(l);
        }
        p.setTotalAmount(lines.stream().map(PayslipLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        payslipRepo.save(p);
        return toDto(p, lines, staffRepo.findById(p.getStaffId()).orElse(null));
    }

    /** Draft only: leaves one employee out of this payroll. */
    @Transactional
    public void removePayslip(Long payslipId) {
        Payslip p = payslipRepo.findById(payslipId).orElseThrow(() -> notFound("Payslip not found"));
        draft(p.getPayrollRunId());
        lineRepo.deleteByPayslipId(p.getId());
        payslipRepo.delete(p);
    }

    @Transactional
    public void deleteDraft(Long id) {
        PayrollRun run = draft(id);
        for (Payslip p : payslipRepo.findByPayrollRunIdOrderByStaffNameAsc(id)) {
            lineRepo.deleteByPayslipId(p.getId());
            payslipRepo.delete(p);
        }
        runRepo.delete(run);
    }

    // ── Finalise / cancel ──────────────────────────────────────────────────

    @Transactional
    public RunDetail finalise(Long id, String user) {
        PayrollRun run = draft(id);
        List<Payslip> slips = payslipRepo.findByPayrollRunIdOrderByStaffNameAsc(id);
        if (slips.isEmpty()) throw bad("There is nobody on this payroll");
        PayrollSettings settings = setup.ensureSettings();
        if (settings.getSalaryPayableAccountId() == null) {
            throw bad("Set the salaries payable account in Payroll Setup first");
        }
        Map<Long, BigDecimal> byLedger = new LinkedHashMap<>();
        for (PayslipLine l : lineRepo.findByPayslipIdInOrderBySortOrderAscIdAsc(slips.stream().map(Payslip::getId).toList())) {
            byLedger.merge(l.getExpenseLedgerId(), l.getAmount(), BigDecimal::add);
        }
        BigDecimal total = byLedger.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() <= 0) throw bad("This payroll adds up to nothing");

        JournalEntryRequest j = new JournalEntryRequest();
        j.setEntryDate(LateFeeService.today());
        j.setReferenceType(JournalReferenceType.PAYROLL);
        j.setReferenceId(run.getId());
        j.setDescription(run.getTitle() + " — " + slips.size() + " employee(s)");
        List<JournalLineRequest> lines = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> e : byLedger.entrySet()) {
            JournalLineRequest dr = new JournalLineRequest();
            dr.setAccountId(e.getKey());
            dr.setDebit(e.getValue());
            dr.setDescription(run.getTitle());
            lines.add(dr);
        }
        JournalLineRequest cr = new JournalLineRequest();
        cr.setAccountId(settings.getSalaryPayableAccountId());
        cr.setCredit(total);
        cr.setDescription("Salaries payable — " + run.getTitle());
        lines.add(cr);
        j.setLines(lines);
        run.setJournalEntryId(journalService.post(j, user).getId());
        run.setStatus(PayrollRunStatus.FINALISED);
        run.setFinalisedBy(user);
        run.setFinalisedAt(LocalDateTime.now(LateFeeService.ZONE));
        runRepo.save(run);
        return detail(run, List.of());
    }

    /** A finalised run nobody has been paid from: reverse it so it can be made again. */
    @Transactional
    public RunDetail cancel(Long id, String user) {
        PayrollRun run = find(id);
        if (run.getStatus() != PayrollRunStatus.FINALISED) throw conflict("Only a finalised payroll can be cancelled; delete a draft instead");
        if (payslipRepo.findByPayrollRunIdOrderByStaffNameAsc(id).stream().anyMatch(p -> Boolean.TRUE.equals(p.getIsPaid()))) {
            throw conflict("Some payslips are already paid, so this payroll can't be cancelled");
        }
        if (run.getJournalEntryId() != null) {
            journalService.reverse(run.getJournalEntryId(), run.getTitle() + " cancelled", user);
        }
        run.setStatus(PayrollRunStatus.CANCELLED);
        runRepo.save(run);
        return detail(run, List.of());
    }

    // ── Pay ────────────────────────────────────────────────────────────────

    /** Pays the chosen payslips in full, together: one entry Dr Salaries Payable / Cr Cash or Bank. */
    @Transactional
    public RunDetail pay(Long runId, PayRequest req, String user) {
        PayrollRun run = find(runId);
        if (run.getStatus() != PayrollRunStatus.FINALISED) throw conflict("Finalise the payroll before paying it");
        if (req.payslipIds() == null || req.payslipIds().isEmpty()) throw bad("Choose who to pay");
        String method = req.method() == null ? "" : req.method().trim().toUpperCase();
        PayrollSettings settings = setup.ensureSettings();
        Long creditAccount = switch (method) {
            case "CASH" -> settings.getCashAccountId();
            case "BANK" -> settings.getBankAccountId();
            default -> throw bad("Pay by CASH or BANK");
        };
        if (creditAccount == null) throw bad("Set the " + method.toLowerCase() + " account in Payroll Setup first");
        if (settings.getSalaryPayableAccountId() == null) throw bad("Set the salaries payable account in Payroll Setup first");

        Set<Long> wanted = new HashSet<>(req.payslipIds());
        List<Payslip> slips = payslipRepo.findByPayrollRunIdOrderByStaffNameAsc(runId).stream()
                .filter(p -> wanted.contains(p.getId())).toList();
        if (slips.size() != wanted.size()) throw bad("Some payslips are not on this payroll");
        if (slips.stream().anyMatch(p -> Boolean.TRUE.equals(p.getIsPaid()))) throw conflict("Some of these are already paid. Refresh and try again.");
        BigDecimal total = slips.stream().map(Payslip::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate paidOn = req.paidOn() != null ? req.paidOn() : LateFeeService.today();

        JournalEntryRequest j = new JournalEntryRequest();
        j.setEntryDate(paidOn);
        j.setReferenceType(JournalReferenceType.PAYROLL_PAYMENT);
        j.setReferenceId(run.getId());
        j.setDescription(run.getTitle() + " paid by " + method.toLowerCase() + " — "
                + (slips.size() == 1 ? slips.get(0).getStaffName() : slips.size() + " employees"));
        JournalLineRequest dr = new JournalLineRequest();
        dr.setAccountId(settings.getSalaryPayableAccountId());
        dr.setDebit(total);
        dr.setDescription("Salaries payable — " + run.getTitle());
        JournalLineRequest cr = new JournalLineRequest();
        cr.setAccountId(creditAccount);
        cr.setCredit(total);
        cr.setDescription(run.getTitle());
        j.setLines(List.of(dr, cr));
        Long entryId = journalService.post(j, user).getId();

        for (Payslip p : slips) {
            p.setIsPaid(true);
            p.setPaidOn(paidOn);
            p.setPaymentMethod(method);
            p.setPaymentJournalEntryId(entryId);
            p.setPaidBy(user);
            payslipRepo.save(p);
        }
        return detail(run, List.of());
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private PayrollRun find(Long id) {
        return runRepo.findById(id).orElseThrow(() -> notFound("Payroll not found"));
    }

    private PayrollRun draft(Long id) {
        PayrollRun run = find(id);
        if (run.getStatus() != PayrollRunStatus.DRAFT) throw conflict("This payroll is finalised and can't be changed");
        return run;
    }

    private static PayslipLine line(Long componentId, String name, BigDecimal amount, Long ledgerId, int order) {
        PayslipLine l = new PayslipLine();
        l.setComponentId(componentId);
        l.setName(name);
        l.setAmount(PayrollSetupService.money(amount));
        l.setExpenseLedgerId(ledgerId);
        l.setSortOrder(order);
        return l;
    }

    private Map<Long, List<PayslipLine>> linesOf(List<Payslip> slips) {
        if (slips.isEmpty()) return Map.of();
        return lineRepo.findByPayslipIdInOrderBySortOrderAscIdAsc(slips.stream().map(Payslip::getId).toList())
                .stream().collect(Collectors.groupingBy(PayslipLine::getPayslipId));
    }

    private RunDetail detail(PayrollRun run, List<String> warnings) {
        List<Payslip> slips = payslipRepo.findByPayrollRunIdOrderByStaffNameAsc(run.getId());
        Map<Long, List<PayslipLine>> lines = linesOf(slips);
        Map<Long, Staff> staff = staffRepo.findAllById(slips.stream().map(Payslip::getStaffId).toList())
                .stream().collect(Collectors.toMap(Staff::getId, s -> s));
        List<PayslipDto> dtos = slips.stream()
                .map(p -> toDto(p, lines.getOrDefault(p.getId(), List.of()), staff.get(p.getStaffId()))).toList();
        return new RunDetail(summary(run, slips), dtos, warnings);
    }

    private static RunSummary summary(PayrollRun r, List<Payslip> slips) {
        BigDecimal total = slips.stream().map(Payslip::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Payslip> paid = slips.stream().filter(p -> Boolean.TRUE.equals(p.getIsPaid())).toList();
        BigDecimal paidTotal = paid.stream().map(Payslip::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RunSummary(r.getId(), r.getRunType(), r.getPeriod(), r.getTitle(), r.getStatus(), slips.size(), total,
                paid.size(), paidTotal, r.getJournalEntryId(), r.getCreatedBy(), r.getCreatedAt(), r.getFinalisedBy(), r.getFinalisedAt());
    }

    private static PayslipDto toDto(Payslip p, List<PayslipLine> lines, Staff s) {
        List<LineDto> ls = (lines == null ? List.<PayslipLine>of() : lines).stream()
                .map(l -> new LineDto(l.getId(), l.getComponentId(), l.getName(), l.getAmount())).toList();
        return new PayslipDto(p.getId(), p.getPayrollRunId(), p.getStaffId(), p.getStaffSystemId(), p.getStaffName(),
                p.getDesignationName(), p.getTotalAmount(), Boolean.TRUE.equals(p.getIsPaid()), p.getPaidOn(),
                p.getPaymentMethod(), p.getPaymentJournalEntryId(), p.getPaidBy(),
                s != null ? s.getPhone() : null, s != null ? s.getBankName() : null, s != null ? s.getBankAccountNumber() : null, ls);
    }

    private static ResponseStatusException bad(String m) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
    }

    private static ResponseStatusException conflict(String m) {
        return new ResponseStatusException(HttpStatus.CONFLICT, m);
    }

    private static ResponseStatusException notFound(String m) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, m);
    }
}
