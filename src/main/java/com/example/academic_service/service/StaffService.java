package com.example.academic_service.service;

import com.example.academic_service.entity.*;
import com.example.academic_service.repository.*;
import com.example.academic_service.util.AuditHelper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;
    private final StaffEmergencyContactRepository emergencyContactRepository;
    private final StaffDocumentRepository documentRepository;
    private final DesignationRepository designationRepository;
    private final AuditLogService auditLogService;

    private String generateStaffSystemId() {
        String prefix = "STF" + Year.now().getValue();
        String maxId = staffRepository.findMaxStaffSystemIdByPrefix(prefix);
        int next = (maxId == null) ? 1 : Integer.parseInt(maxId.substring(prefix.length())) + 1;
        return prefix + String.format("%04d", next);
    }

    public List<Staff> getAll(Boolean isActive, EmployeeType employeeType) {
        if (isActive != null && employeeType != null)
            return staffRepository.findByIsActiveAndEmployeeType(isActive, employeeType);
        if (isActive != null) return staffRepository.findByIsActive(isActive);
        if (employeeType != null) return staffRepository.findByEmployeeType(employeeType);
        return staffRepository.findAll();
    }

    public Staff getById(Long id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + id));
    }

    public Staff getBySystemId(String systemId) {
        return staffRepository.findByStaffSystemId(systemId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + systemId));
    }

    @Transactional
    public Staff create(Staff staff, List<StaffEmergencyContact> contacts) {
        if (staff.getPhone() != null && !staff.getPhone().isBlank()
                && staffRepository.existsByPhone(staff.getPhone())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already in use: " + staff.getPhone());
        }
        staff.setStaffSystemId(generateStaffSystemId());
        staff.setIsActive(true);
        if (staff.getCurrentDesignation() != null && staff.getCurrentDesignation().getId() != null)
            staff.setCurrentDesignation(designationRepository.findById(staff.getCurrentDesignation().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Designation not found: " + staff.getCurrentDesignation().getId())));
        Staff saved = staffRepository.save(staff);
        if (contacts != null) {
            contacts.forEach(c -> { c.setStaff(saved); emergencyContactRepository.save(c); });
        }
        auditLogService.log(AuditHelper.getUserId(), AuditHelper.getIp(),
            AuditActionType.CREATE, Submodule.HR_STAFF, "Staff", saved.getId().toString(),
            "Created staff: " + saved.getNameEnglish() + " (" + saved.getStaffSystemId() + ")",
            null, AuditHelper.toJson(staffSnapshot(saved)));
        return saved;
    }

    @Transactional
    public Staff update(Long id, Staff req, List<StaffEmergencyContact> contacts) {
        Staff existing = getById(id);
        if (req.getPhone() != null && !req.getPhone().isBlank()
                && staffRepository.existsByPhoneAndIdNot(req.getPhone(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already in use: " + req.getPhone());
        }
        existing.setNameEnglish(req.getNameEnglish());
        existing.setNameBangla(req.getNameBangla());
        existing.setEmployeeType(req.getEmployeeType());
        existing.setJoiningDate(req.getJoiningDate());
        existing.setDob(req.getDob());
        existing.setPhone(req.getPhone());
        existing.setEmail(req.getEmail());
        existing.setNationalId(req.getNationalId());
        if (req.getCurrentDesignation() != null && req.getCurrentDesignation().getId() != null)
            existing.setCurrentDesignation(designationRepository.findById(req.getCurrentDesignation().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Designation not found: " + req.getCurrentDesignation().getId())));
        String before = AuditHelper.toJson(staffSnapshot(existing));
        Staff saved = staffRepository.save(existing);
        if (contacts != null) {
            emergencyContactRepository.deleteByStaffId(id);
            contacts.forEach(c -> { c.setStaff(saved); emergencyContactRepository.save(c); });
        }
        auditLogService.log(AuditHelper.getUserId(), AuditHelper.getIp(),
            AuditActionType.UPDATE, Submodule.HR_STAFF, "Staff", id.toString(),
            "Updated staff: " + saved.getNameEnglish() + " (" + saved.getStaffSystemId() + ")",
            before, AuditHelper.toJson(staffSnapshot(saved)));
        return saved;
    }

    public void deactivate(Long id) {
        Staff s = getById(id);
        String before = AuditHelper.toJson(staffSnapshot(s));
        s.setIsActive(false);
        staffRepository.save(s);
        auditLogService.log(AuditHelper.getUserId(), AuditHelper.getIp(),
            AuditActionType.DELETE, Submodule.HR_STAFF, "Staff", id.toString(),
            "Deactivated staff: " + s.getNameEnglish() + " (" + s.getStaffSystemId() + ")",
            before, null);
    }

    public List<StaffEmergencyContact> getEmergencyContacts(Long staffId) {
        return emergencyContactRepository.findByStaffId(staffId);
    }

    public List<StaffDocument> getDocuments(Long staffId) {
        return documentRepository.findByStaffId(staffId);
    }

    public StaffDocument addDocument(Long staffId, String documentType, String fileUrl) {
        StaffDocument doc = new StaffDocument();
        doc.setStaff(staffRepository.getReferenceById(staffId));
        doc.setDocumentType(documentType);
        doc.setFileUrl(fileUrl);
        doc.setUploadedAt(LocalDateTime.now());
        return documentRepository.save(doc);
    }

    public void deleteDocument(Long documentId) {
        documentRepository.deleteById(documentId);
    }

    private Map<String, Object> staffSnapshot(Staff s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("staffSystemId", s.getStaffSystemId());
        m.put("nameEnglish", s.getNameEnglish());
        m.put("nameBangla", s.getNameBangla());
        m.put("phone", s.getPhone());
        m.put("email", s.getEmail());
        m.put("employeeType", s.getEmployeeType() != null ? s.getEmployeeType().name() : null);
        m.put("designation", s.getCurrentDesignation() != null ? s.getCurrentDesignation().getName() : null);
        m.put("isActive", s.getIsActive());
        return m;
    }
}
