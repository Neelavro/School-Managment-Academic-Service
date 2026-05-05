package com.example.academic_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TransferRequestDto {
    private List<Long> enrollmentIds;
    private Integer toAcademicYearId;
    private Integer toClassId;
    private Integer toShiftId;
    private Integer toGenderSectionId;
    private Long toSectionId;
    private Integer toGroupId;
}
