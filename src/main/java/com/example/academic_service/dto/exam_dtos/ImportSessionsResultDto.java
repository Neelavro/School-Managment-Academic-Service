package com.example.academic_service.dto.exam_dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImportSessionsResultDto {
    private int imported;
    private int skipped;
    private List<ExamSessionResponseDto> sessions;
}
