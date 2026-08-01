package com.example.academic_service.dto.marking_dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CopyMarkingStructureRequest {
    private Integer fromExamTypeId;
    private Integer toExamTypeId;
    // Empty or null means every class that already has a structure under fromExamTypeId.
    private List<Integer> classIds;
}
