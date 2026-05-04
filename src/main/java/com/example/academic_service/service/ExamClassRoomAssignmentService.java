package com.example.academic_service.service;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.exam_dtos.ExamClassRoomAssignmentRequestDto;
import com.example.academic_service.dto.exam_dtos.ExamClassRoomAssignmentResponseDto;

import java.util.List;

public interface ExamClassRoomAssignmentService {
    ApiResponse<List<ExamClassRoomAssignmentResponseDto>> assign(ExamClassRoomAssignmentRequestDto dto);
    ApiResponse<List<ExamClassRoomAssignmentResponseDto>> getByRoutine(Integer routineId);
    ApiResponse<Void> removeByClassAndRoutine(Integer routineId, Integer classId);
}
