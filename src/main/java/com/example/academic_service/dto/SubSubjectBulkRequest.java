package com.example.academic_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Several sub-subjects of one parent created together (user, 2026-11-12), optionally assigned to classes. */
@Getter
@Setter
public class SubSubjectBulkRequest {
    private Integer parentSubjectId;
    private List<Item> subSubjects;
    private List<Integer> classIds; // optional

    @Getter
    @Setter
    public static class Item {
        private String name;
        private String code; // optional
    }
}
