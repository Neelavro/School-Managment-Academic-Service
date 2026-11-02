package com.example.academic_service.controller;

import com.example.academic_service.entity.EmployeeType;
import com.example.academic_service.entity.Staff;
import com.example.academic_service.repository.StaffRepository;
import com.example.academic_service.repository.TeacherProfileRepository;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Teacher list for the school's public website (permitAll in SecurityConfig).
 * Active TEACHING staff only, in Staff ID order. Only what a school publishes about its faculty:
 * names, designation and MPO index number. Never phone, email, NID, address, bank or salary.
 */
@RestController
@RequiredArgsConstructor
public class PublicTeacherController {

    private final StaffRepository staffRepository;
    private final TeacherProfileRepository teacherProfileRepository;

    public record PublicTeacher(Long id, String nameEnglish, String nameBangla, String designation, String mpoIndexNumber) {}

    @GetMapping("/api/public/teachers")
    public ResponseEntity<ApiResponse> teachers() {
        List<Staff> staff = staffRepository.findByIsActiveAndEmployeeType(true, EmployeeType.TEACHING);

        Map<Long, String> mpoIndex = new HashMap<>();
        if (!staff.isEmpty()) {
            for (Object[] row : teacherProfileRepository.findMpoIndexByStaffIds(staff.stream().map(Staff::getId).toList())) {
                mpoIndex.put((Long) row[0], (String) row[1]);
            }
        }

        List<PublicTeacher> list = staff.stream()
                .sorted(Comparator.comparing(Staff::getStaffSystemId, PublicTeacherController::compareStaffIds)
                        .thenComparing(s -> s.getNameEnglish() == null ? "" : s.getNameEnglish(), String.CASE_INSENSITIVE_ORDER))
                .map(s -> new PublicTeacher(
                        s.getId(),
                        blankToNull(s.getNameEnglish()),
                        blankToNull(s.getNameBangla()),
                        s.getCurrentDesignation() == null ? null : blankToNull(s.getCurrentDesignation().getName()),
                        blankToNull(mpoIndex.get(s.getId()))))
                .toList();
        return ResponseEntity.ok(new ApiResponse("OK", list));
    }

    /** Same rule as the panel's compareStaffIds: numeric-aware (9 before 10), no Staff ID last. */
    static int compareStaffIds(String a, String b) {
        boolean noA = a == null || a.isBlank(), noB = b == null || b.isBlank();
        if (noA || noB) return Boolean.compare(noA, noB);
        String x = a.trim(), y = b.trim();
        if (x.matches("\\d+") && y.matches("\\d+")) {
            String nx = x.replaceFirst("^0+(?=.)", ""), ny = y.replaceFirst("^0+(?=.)", "");
            if (nx.length() != ny.length()) return Integer.compare(nx.length(), ny.length());
            int c = nx.compareTo(ny);
            if (c != 0) return c;
        }
        return x.compareToIgnoreCase(y);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
