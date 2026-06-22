package com.example.academic_service.service;

import com.example.academic_service.dto.ChartOfAccountRequest;
import com.example.academic_service.dto.ChartOfAccountResponse;
import com.example.academic_service.dto.CsvImportResponse;
import com.example.academic_service.entity.AccountType;
import com.example.academic_service.entity.ChartOfAccount;
import com.example.academic_service.repository.ChartOfAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChartOfAccountService {

    private final ChartOfAccountRepository repo;

    // ── Reads ──────────────────────────────────────────────────────────────

    public List<ChartOfAccountResponse> getAll() {
        List<ChartOfAccount> all = repo.findAll();
        Map<Long, String> codeById = new HashMap<>();
        for (ChartOfAccount a : all) codeById.put(a.getId(), a.getAccountCode());
        return all.stream()
                .map(a -> ChartOfAccountResponse.from(a, codeById.get(a.getParentId())))
                .toList();
    }

    public ChartOfAccountResponse getOne(Long id) {
        ChartOfAccount a = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        String parentCode = a.getParentId() != null
                ? repo.findById(a.getParentId()).map(ChartOfAccount::getAccountCode).orElse(null)
                : null;
        return ChartOfAccountResponse.from(a, parentCode);
    }

    // ── Writes ─────────────────────────────────────────────────────────────

    @Transactional
    public ChartOfAccountResponse create(ChartOfAccountRequest req) {
        validateForCreate(req);
        ChartOfAccount a = new ChartOfAccount();
        a.setAccountCode(req.getAccountCode().trim());
        a.setAccountName(req.getAccountName().trim());
        a.setAccountType(req.getAccountType());
        a.setParentId(req.getParentId());
        a.setIsGroup(req.getIsGroup() != null ? req.getIsGroup() : false);
        a.setIsActive(req.getIsActive() != null ? req.getIsActive() : true);
        a.setDescription(req.getDescription());
        ChartOfAccount saved = repo.save(a);
        String parentCode = saved.getParentId() != null
                ? repo.findById(saved.getParentId()).map(ChartOfAccount::getAccountCode).orElse(null)
                : null;
        return ChartOfAccountResponse.from(saved, parentCode);
    }

    @Transactional
    public ChartOfAccountResponse update(Long id, ChartOfAccountRequest req) {
        ChartOfAccount existing = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        // Code uniqueness: only check if changed.
        if (req.getAccountCode() != null
                && !req.getAccountCode().trim().equalsIgnoreCase(existing.getAccountCode())
                && repo.existsByAccountCode(req.getAccountCode().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "accountCode '" + req.getAccountCode() + "' already exists");
        }

        boolean typeChanged = req.getAccountType() != null && req.getAccountType() != existing.getAccountType();
        long childCount = repo.countByParentId(id);

        // Cannot change account_type if this account has children
        // (would break the parent-type-must-match-child-type invariant).
        if (typeChanged && childCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot change account_type — this account has children of the current type");
        }

        // Always validate parent if request specifies one (covers both reparent
        // and type-change scenarios where the existing parent might no longer match).
        if (req.getParentId() != null) {
            validateParentForChildType(req.getParentId(), req.getAccountType(), id);
        }

        // Cannot demote a group to leaf if it has children.
        if (Boolean.FALSE.equals(req.getIsGroup())
                && Boolean.TRUE.equals(existing.getIsGroup())
                && childCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot convert to leaf — this account has children");
        }

        existing.setAccountCode(req.getAccountCode().trim());
        existing.setAccountName(req.getAccountName().trim());
        existing.setAccountType(req.getAccountType());
        existing.setParentId(req.getParentId());
        if (req.getIsGroup() != null) existing.setIsGroup(req.getIsGroup());
        if (req.getIsActive() != null) existing.setIsActive(req.getIsActive());
        existing.setDescription(req.getDescription());
        ChartOfAccount saved = repo.save(existing);

        String parentCode = saved.getParentId() != null
                ? repo.findById(saved.getParentId()).map(ChartOfAccount::getAccountCode).orElse(null)
                : null;
        return ChartOfAccountResponse.from(saved, parentCode);
    }

    @Transactional
    public void delete(Long id) {
        ChartOfAccount existing = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        if (repo.countByParentId(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete — this account has children. Delete or reparent them first.");
        }
        // NOTE: once journal entries reference this account, deletion should be blocked here too.
        repo.delete(existing);
    }

    // ── CSV Import ─────────────────────────────────────────────────────────

    /**
     * Imports a CSV file in a single transaction.
     * Columns (header row required, case-insensitive):
     *     account_code, account_name, account_type, parent_code, is_group, description
     *
     * Validation:
     *  - account_code: required, unique vs DB AND within the file
     *  - account_name: required
     *  - account_type: required, one of ASSET, LIABILITY, INCOME, EXPENSE
     *  - parent_code (optional): must exist in DB OR earlier in the file
     *  - parent's account_type must match this row's account_type
     *  - parent must be is_group = true
     *  - is_group: optional boolean, defaults to false
     *
     * If ANY row fails validation, no rows are persisted (the response carries errors).
     * All parents are resolved by code, then rows are saved in dependency order.
     */
    @Transactional
    public CsvImportResponse importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        CsvImportResponse response = new CsvImportResponse();
        response.setOk(false);

        // 1) Read CSV into rows.
        List<String[]> rows = new ArrayList<>();
        Map<String, Integer> headerIndex;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV is empty");
            }
            if (!headerLine.isEmpty() && headerLine.charAt(0) == '﻿') {
                headerLine = headerLine.substring(1);
            }
            headerIndex = parseHeader(headerLine);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                rows.add(parseCsvLine(line));
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read CSV: " + e.getMessage());
        }

        // 2) Seed lookups with existing DB rows (codes keyed lowercase for case-insensitivity).
        Map<String, AccountType> futureCodeToType = new HashMap<>();
        Map<String, Boolean> futureCodeToIsGroup = new HashMap<>();
        Map<String, Long> dbCodeToId = new HashMap<>();
        for (ChartOfAccount existing : repo.findAll()) {
            String key = existing.getAccountCode().toLowerCase(Locale.ROOT);
            futureCodeToType.put(key, existing.getAccountType());
            futureCodeToIsGroup.put(key, existing.getIsGroup());
            dbCodeToId.put(key, existing.getId());
        }

        // 3) Validate each row. Stage entities + their parent code.
        Map<String, ChartOfAccount> stagedByCode = new LinkedHashMap<>();
        Map<String, String> pendingParents = new HashMap<>(); // childCodeLower -> parentCodeLower
        Set<String> seenInFile = new HashSet<>();
        boolean hasErrors = false;
        int rowNumber = 1; // header is row 1

        for (String[] cols : rows) {
            rowNumber++;
            String accountCode = readCol(cols, headerIndex, "account_code");
            String accountName = readCol(cols, headerIndex, "account_name");
            String accountTypeStr = readCol(cols, headerIndex, "account_type");
            String parentCode = readCol(cols, headerIndex, "parent_code");
            String isGroupStr = readCol(cols, headerIndex, "is_group");
            String description = readCol(cols, headerIndex, "description");

            if (isBlank(accountCode)) {
                addErr(response, rowNumber, "account_code is required"); hasErrors = true; continue;
            }
            if (isBlank(accountName)) {
                addErr(response, rowNumber, "account_name is required"); hasErrors = true; continue;
            }
            if (isBlank(accountTypeStr)) {
                addErr(response, rowNumber, "account_type is required"); hasErrors = true; continue;
            }

            String codeKey = accountCode.trim().toLowerCase(Locale.ROOT);

            if (dbCodeToId.containsKey(codeKey)) {
                addErr(response, rowNumber, "account_code '" + accountCode + "' already exists in database");
                hasErrors = true; continue;
            }
            if (!seenInFile.add(codeKey)) {
                addErr(response, rowNumber, "account_code '" + accountCode + "' is duplicated in this file");
                hasErrors = true; continue;
            }

            AccountType accountType;
            try {
                accountType = AccountType.valueOf(accountTypeStr.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                addErr(response, rowNumber,
                        "account_type must be one of ASSET, LIABILITY, INCOME, EXPENSE (got '" + accountTypeStr + "')");
                hasErrors = true; continue;
            }

            boolean isGroup = parseBool(isGroupStr, false);

            ChartOfAccount entity = new ChartOfAccount();
            entity.setAccountCode(accountCode.trim());
            entity.setAccountName(accountName.trim());
            entity.setAccountType(accountType);
            entity.setIsGroup(isGroup);
            entity.setIsActive(true);
            entity.setDescription(isBlank(description) ? null : description.trim());

            if (!isBlank(parentCode)) {
                String parentKey = parentCode.trim().toLowerCase(Locale.ROOT);
                if (parentKey.equals(codeKey)) {
                    addErr(response, rowNumber, "parent_code cannot equal account_code");
                    hasErrors = true; continue;
                }
                AccountType parentType = futureCodeToType.get(parentKey);
                Boolean parentIsGroup = futureCodeToIsGroup.get(parentKey);
                if (parentType == null) {
                    addErr(response, rowNumber,
                            "parent_code '" + parentCode + "' not found (must exist in DB or appear earlier in this file)");
                    hasErrors = true; continue;
                }
                if (parentType != accountType) {
                    addErr(response, rowNumber,
                            "parent account_type (" + parentType + ") must match this row's account_type (" + accountType + ")");
                    hasErrors = true; continue;
                }
                if (!Boolean.TRUE.equals(parentIsGroup)) {
                    addErr(response, rowNumber,
                            "parent '" + parentCode + "' is a leaf account; only group accounts can have children");
                    hasErrors = true; continue;
                }
                pendingParents.put(codeKey, parentKey);
            }

            futureCodeToType.put(codeKey, accountType);
            futureCodeToIsGroup.put(codeKey, isGroup);
            stagedByCode.put(codeKey, entity);
        }

        if (hasErrors) {
            return response; // nothing persisted; @Transactional rolls back even safe rows.
        }

        // 4) Persist in dependency order: rows whose parent is null or already in DB first,
        //    then iterate until every row has a parentId. Detect cycles if no progress is made.
        int remaining = stagedByCode.size();
        while (remaining > 0) {
            boolean progress = false;
            for (Map.Entry<String, ChartOfAccount> e : stagedByCode.entrySet()) {
                ChartOfAccount entity = e.getValue();
                if (entity.getId() != null) continue;
                String parentKey = pendingParents.get(e.getKey());
                if (parentKey == null) {
                    ChartOfAccount saved = repo.save(entity);
                    dbCodeToId.put(e.getKey(), saved.getId());
                    progress = true;
                    remaining--;
                } else {
                    Long parentId = dbCodeToId.get(parentKey);
                    if (parentId != null) {
                        entity.setParentId(parentId);
                        ChartOfAccount saved = repo.save(entity);
                        dbCodeToId.put(e.getKey(), saved.getId());
                        progress = true;
                        remaining--;
                    }
                }
            }
            if (!progress) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Circular parent reference detected — no rows could be persisted");
            }
        }

        response.setOk(true);
        response.setCreatedCount(stagedByCode.size());
        return response;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void validateForCreate(ChartOfAccountRequest req) {
        if (req.getAccountCode() == null || req.getAccountCode().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "accountCode is required");
        }
        if (repo.existsByAccountCode(req.getAccountCode().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "accountCode '" + req.getAccountCode() + "' already exists");
        }
        if (req.getParentId() != null) {
            validateParentForChildType(req.getParentId(), req.getAccountType(), null);
        }
    }

    /**
     * Parent rules:
     *  - parent must exist
     *  - parent must be is_group = true
     *  - parent's account_type must equal the child's account_type
     *  - parent cannot be the row itself
     */
    private void validateParentForChildType(Long parentId, AccountType childType, Long excludeChildId) {
        ChartOfAccount parent = repo.findById(parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "parent account not found"));
        if (excludeChildId != null && parent.getId().equals(excludeChildId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "An account cannot be its own parent");
        }
        if (!Boolean.TRUE.equals(parent.getIsGroup())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "parent must be a group account");
        }
        if (parent.getAccountType() != childType) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "parent account_type must match the child's account_type");
        }
    }

    private static Map<String, Integer> parseHeader(String header) {
        String[] cols = parseCsvLine(header);
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < cols.length; i++) {
            index.put(cols[i].trim().toLowerCase(Locale.ROOT), i);
        }
        for (String required : new String[]{"account_code", "account_name", "account_type"}) {
            if (!index.containsKey(required)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "CSV header missing required column: " + required);
            }
        }
        return index;
    }

    /**
     * Minimal CSV line parser supporting:
     *  - comma delimiter
     *  - double-quoted fields containing commas
     *  - "" within a quoted field as an escaped quote
     */
    private static String[] parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"'); i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == ',') {
                    out.add(cur.toString()); cur.setLength(0);
                } else if (c == '"') {
                    inQuotes = true;
                } else {
                    cur.append(c);
                }
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    private static String readCol(String[] cols, Map<String, Integer> index, String name) {
        Integer i = index.get(name);
        if (i == null || i >= cols.length) return null;
        return cols[i];
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static boolean parseBool(String s, boolean defaultValue) {
        if (s == null) return defaultValue;
        String v = s.trim().toLowerCase(Locale.ROOT);
        if (v.isEmpty()) return defaultValue;
        return v.equals("true") || v.equals("yes") || v.equals("y") || v.equals("1");
    }

    private static void addErr(CsvImportResponse r, int row, String msg) {
        r.getErrors().add(new CsvImportResponse.RowError(row, msg));
    }
}
