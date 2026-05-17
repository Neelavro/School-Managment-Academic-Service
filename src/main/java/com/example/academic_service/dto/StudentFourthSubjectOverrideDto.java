package com.example.academic_service.dto;

import com.example.academic_service.entity.StudentFourthSubjectOverride;
import lombok.Getter;
import lombok.Setter;

public class StudentFourthSubjectOverrideDto {

    @Getter
    @Setter
    public static class Request {
        private Long enrollmentId;
        private Integer subjectId;
    }

    @Getter
    @Setter
    public static class Response {
        private Long id;
        private Long enrollmentId;
        private Integer subjectId;
        private String subjectName;

        public static Response from(StudentFourthSubjectOverride e) {
            Response r = new Response();
            r.setId(e.getId());
            r.setEnrollmentId(e.getEnrollmentId());
            r.setSubjectId(e.getSubject().getId());
            r.setSubjectName(e.getSubject().getName());
            return r;
        }
    }
}
