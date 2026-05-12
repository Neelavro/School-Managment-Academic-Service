package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.entity.EmployeeType;
import com.example.academic_service.entity.Staff;
import com.example.academic_service.entity.StaffEmergencyContact;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.StaffService;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "READ")
    public ResponseEntity<ApiResponse> getAll(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) EmployeeType employeeType) {
        return ResponseEntity.ok(new ApiResponse("OK", staffService.getAll(isActive, employeeType)));
    }

    @GetMapping("/{id}")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "READ")
    public ResponseEntity<ApiResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse("OK", staffService.getById(id)));
    }

    @GetMapping("/system/{systemId}")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "READ")
    public ResponseEntity<ApiResponse> getBySystemId(@PathVariable String systemId) {
        return ResponseEntity.ok(new ApiResponse("OK", staffService.getBySystemId(systemId)));
    }

    @PostMapping
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "CREATE")
    public ResponseEntity<ApiResponse> create(@RequestBody Map<String, Object> body) {
        Staff staff = extractStaff(body);
        List<StaffEmergencyContact> contacts = extractContacts(body);
        return ResponseEntity.ok(new ApiResponse("Created", staffService.create(staff, contacts)));
    }

    @PutMapping("/{id}")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "UPDATE")
    public ResponseEntity<ApiResponse> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Staff staff = extractStaff(body);
        List<StaffEmergencyContact> contacts = extractContacts(body);
        return ResponseEntity.ok(new ApiResponse("Updated", staffService.update(id, staff, contacts)));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "DELETE")
    public ResponseEntity<ApiResponse> deactivate(@PathVariable Long id) {
        staffService.deactivate(id);
        return ResponseEntity.ok(new ApiResponse("Deactivated", null));
    }

    @GetMapping("/{id}/emergency-contacts")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "READ")
    public ResponseEntity<ApiResponse> getEmergencyContacts(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse("OK", staffService.getEmergencyContacts(id)));
    }

    @GetMapping("/{id}/documents")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "READ")
    public ResponseEntity<ApiResponse> getDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse("OK", staffService.getDocuments(id)));
    }

    @PostMapping("/{id}/documents")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "CREATE")
    public ResponseEntity<ApiResponse> addDocument(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(new ApiResponse("Added",
                staffService.addDocument(id, body.get("documentType"), body.get("fileUrl"))));
    }

    @DeleteMapping("/documents/{documentId}")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "DELETE")
    public ResponseEntity<ApiResponse> deleteDocument(@PathVariable Long documentId) {
        staffService.deleteDocument(documentId);
        return ResponseEntity.ok(new ApiResponse("Deleted", null));
    }

    @SuppressWarnings("unchecked")
    private Staff extractStaff(Map<String, Object> body) {
        Staff s = new Staff();
        if (body.get("nameEnglish") != null) s.setNameEnglish((String) body.get("nameEnglish"));
        if (body.get("nameBangla") != null) s.setNameBangla((String) body.get("nameBangla"));
        if (body.get("employeeType") != null) s.setEmployeeType(EmployeeType.valueOf((String) body.get("employeeType")));
        if (body.get("phone") != null) s.setPhone((String) body.get("phone"));
        if (body.get("email") != null) s.setEmail((String) body.get("email"));
        if (body.get("nationalId") != null) s.setNationalId((String) body.get("nationalId"));
        if (body.get("joiningDate") != null) s.setJoiningDate(java.time.LocalDate.parse((String) body.get("joiningDate")));
        if (body.get("dob") != null) s.setDob(java.time.LocalDate.parse((String) body.get("dob")));
        if (body.get("designationId") != null) {
            com.example.academic_service.entity.Designation d = new com.example.academic_service.entity.Designation();
            d.setId(((Number) body.get("designationId")).intValue());
            s.setCurrentDesignation(d);
        }
        return s;
    }

    @SuppressWarnings("unchecked")
    private List<StaffEmergencyContact> extractContacts(Map<String, Object> body) {
        Object raw = body.get("emergencyContacts");
        if (raw == null) return null;
        return ((List<Map<String, String>>) raw).stream().map(m -> {
            StaffEmergencyContact c = new StaffEmergencyContact();
            c.setName(m.get("name"));
            c.setRelationship(m.get("relationship"));
            c.setPhone(m.get("phone"));
            return c;
        }).toList();
    }
}
