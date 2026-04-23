package com.example.academic_service.dto.marking_dtos;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class BulkMarkingStructureRequest {
    private Integer examTypeId;
    private List<Integer> classIds;
    private Integer subjectId;
    private Integer groupId; // nullable
    private Integer totalMarks;
    private Integer passMarks; // nullable — overall pass mark
    private List<MarkingStructureComponentRequest> components;
}