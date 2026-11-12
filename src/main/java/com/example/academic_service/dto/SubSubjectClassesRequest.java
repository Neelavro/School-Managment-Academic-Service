package com.example.academic_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Assign sub-subjects to classes (user, 2026-11-12). By default the classes are added to each sub-subject; with
 * replace=true each sub-subject ends up assigned to exactly these classes (an empty list unassigns it everywhere).
 */
@Getter
@Setter
public class SubSubjectClassesRequest {
    private List<Integer> subSubjectIds;
    private List<Integer> classIds;
    private Boolean replace;
}
