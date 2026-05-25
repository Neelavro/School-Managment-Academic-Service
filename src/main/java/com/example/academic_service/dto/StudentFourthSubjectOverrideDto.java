package com.example.academic_service.dto;

import com.example.academic_service.entity.StudentFourthSubjectOverride;
import com.example.academic_service.entity.Subject;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class StudentFourthSubjectOverrideDto {

    @Getter
    @Setter
    public static class Request {
        private Long enrollmentId;
        private Integer subjectId;
        private List<Integer> compulsorySubjectIds;
    }

    @Getter
    @Setter
    public static class Response {
        private Long id;
        private Long enrollmentId;
        private Integer subjectId;
        private String subjectName;
        private List<Integer> compulsorySubjectIds;
        private List<String> compulsorySubjectNames;

        public static Response from(StudentFourthSubjectOverride e) {
            Response r = new Response();
            r.setId(e.getId());
            r.setEnrollmentId(e.getEnrollmentId());
            if (e.getSubject() != null) {
                r.setSubjectId(e.getSubject().getId());
                r.setSubjectName(e.getSubject().getName());
            }
            if (e.getCompulsorySubjects() != null && !e.getCompulsorySubjects().isEmpty()) {
                r.setCompulsorySubjectIds(e.getCompulsorySubjects().stream()
                        .map(Subject::getId).collect(Collectors.toList()));
                r.setCompulsorySubjectNames(e.getCompulsorySubjects().stream()
                        .map(Subject::getName).collect(Collectors.toList()));
            } else {
                r.setCompulsorySubjectIds(Collections.emptyList());
                r.setCompulsorySubjectNames(Collections.emptyList());
            }
            return r;
        }
    }
}
