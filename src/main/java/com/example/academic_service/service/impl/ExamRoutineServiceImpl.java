package com.example.academic_service.service.impl;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.ExamRoutineRequestDto;
import com.example.academic_service.entity.AcademicYear;
import com.example.academic_service.entity.ExamRoutine;
import com.example.academic_service.entity.ExamType;
import com.example.academic_service.entity.RoutineStatus;
import com.example.academic_service.repository.AcademicYearRepository;
import com.example.academic_service.repository.ExamRoutineRepository;
import com.example.academic_service.repository.ExamTypeRepository;
import com.example.academic_service.service.ExamRoutineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamRoutineServiceImpl implements ExamRoutineService {

    private final ExamRoutineRepository examRoutineRepository;
    private final ExamTypeRepository examTypeRepository;
    private final AcademicYearRepository academicYearRepository;

    @Override
    public ApiResponse<ExamRoutine> create(ExamRoutineRequestDto dto) {
        ExamType examType = examTypeRepository.findById(dto.getExamTypeId()).orElse(null);
        if (examType == null) return ApiResponse.error("Exam type not found");
        if (!examType.getIsActive()) return ApiResponse.error("Cannot use an inactive exam type");

        AcademicYear academicYear = academicYearRepository.findById(dto.getAcademicYearId()).orElse(null);
        if (academicYear == null) return ApiResponse.error("Academic year not found");

        if (examRoutineRepository.existsByExamTypeIdAndAcademicYearIdAndIsActiveTrue(
                dto.getExamTypeId(), dto.getAcademicYearId())) {
            return ApiResponse.error("An active routine for this exam type and academic year already exists");
        }
        ExamRoutine routine = new ExamRoutine();
        routine.setTitle(dto.getTitle());
        routine.setExamType(examType);
        routine.setAcademicYear(academicYear);
        return ApiResponse.success("Exam routine created successfully", examRoutineRepository.save(routine));
    }

    @Override
    public ApiResponse<ExamRoutine> update(Integer id, ExamRoutineRequestDto dto) {
        ExamRoutine routine = examRoutineRepository.findById(id).orElse(null);
        if (routine == null) return ApiResponse.error("Exam routine not found");

        if (!routine.getIsActive()) return ApiResponse.error("Cannot update an inactive exam routine");

        ExamType examType = examTypeRepository.findById(dto.getExamTypeId()).orElse(null);
        if (examType == null) return ApiResponse.error("Exam type not found");
        if (!examType.getIsActive()) return ApiResponse.error("Cannot use an inactive exam type");

        AcademicYear academicYear = academicYearRepository.findById(dto.getAcademicYearId()).orElse(null);
        if (academicYear == null) return ApiResponse.error("Academic year not found");

        if (examRoutineRepository.existsByExamTypeIdAndAcademicYearIdAndIsActiveTrueAndIdNot(
                dto.getExamTypeId(), dto.getAcademicYearId(), id)) {
            return ApiResponse.error("Another active routine for this exam type and academic year already exists");
        }
        routine.setTitle(dto.getTitle());
        routine.setExamType(examType);
        routine.setAcademicYear(academicYear);
        routine.setLastModifiedAt(LocalDateTime.now());
        return ApiResponse.success("Exam routine updated successfully", examRoutineRepository.save(routine));
    }

    @Override
    public ApiResponse<ExamRoutine> getById(Integer id) {
        ExamRoutine routine = examRoutineRepository.findById(id).orElse(null);
        if (routine == null) return ApiResponse.error("Exam routine not found");
        return ApiResponse.success("Exam routine fetched successfully", routine);
    }

    @Override
    public ApiResponse<List<ExamRoutine>> getAll(Boolean active) {
        List<ExamRoutine> result;
        if (active == null) {
            result = examRoutineRepository.findAll();
        } else if (active) {
            result = examRoutineRepository.findByIsActiveTrueOrderByCreatedAtDesc();
        } else {
            result = examRoutineRepository.findByIsActiveFalseOrderByCreatedAtDesc();
        }
        return ApiResponse.success("Exam routines fetched successfully", result);
    }

    @Override
    public ApiResponse<List<ExamRoutine>> getByAcademicYear(Integer academicYearId) {
        return ApiResponse.success("Exam routines fetched successfully",
                examRoutineRepository.findByAcademicYearIdAndIsActiveTrue(academicYearId));
    }

    @Override
    public ApiResponse<List<ExamRoutine>> getByExamType(Integer examTypeId) {
        return ApiResponse.success("Exam routines fetched successfully",
                examRoutineRepository.findByExamTypeIdAndIsActiveTrue(examTypeId));
    }

    @Override
    public ApiResponse<ExamRoutine> publish(Integer id) {
        ExamRoutine routine = examRoutineRepository.findById(id).orElse(null);
        if (routine == null) return ApiResponse.error("Exam routine not found");

        if (!routine.getIsActive()) return ApiResponse.error("Cannot publish an inactive exam routine");

        if (routine.getStatus() == RoutineStatus.PUBLISHED) {
            return ApiResponse.error("Exam routine is already published");
        }
        routine.setStatus(RoutineStatus.PUBLISHED);
        routine.setPublishedAt(LocalDateTime.now());
        return ApiResponse.success("Exam routine published successfully", examRoutineRepository.save(routine));
    }

    @Override
    public ApiResponse<ExamRoutine> reactivate(Integer id) {
        ExamRoutine routine = examRoutineRepository.findById(id).orElse(null);
        if (routine == null) return ApiResponse.error("Exam routine not found");

        if (routine.getIsActive()) return ApiResponse.error("Exam routine is already active");

        if (examRoutineRepository.existsByExamTypeIdAndAcademicYearIdAndIsActiveTrueAndIdNot(
                routine.getExamType().getId(), routine.getAcademicYear().getId(), id)) {
            return ApiResponse.error("An active routine for '" + routine.getExamType().getName()
                    + "' in this academic year already exists. Deactivate it before reactivating this one");
        }
        routine.setIsActive(true);
        routine.setStatus(RoutineStatus.DRAFT);
        routine.setPublishedAt(null);
        routine.setLastModifiedAt(LocalDateTime.now());
        return ApiResponse.success("Exam routine reactivated successfully", examRoutineRepository.save(routine));
    }
    @Override
    public ApiResponse<ExamRoutine> unpublish(Integer id) {
        ExamRoutine routine = examRoutineRepository.findById(id).orElse(null);
        if (routine == null) return ApiResponse.error("Exam routine not found");

        if (!routine.getIsActive()) return ApiResponse.error("Cannot unpublish an inactive exam routine");

        if (routine.getStatus() == RoutineStatus.DRAFT) {
            return ApiResponse.error("Exam routine is not published yet");
        }
        routine.setStatus(RoutineStatus.DRAFT);
        routine.setPublishedAt(null);
        routine.setLastModifiedAt(LocalDateTime.now());
        return ApiResponse.success("Exam routine unpublished successfully", examRoutineRepository.save(routine));
    }

    @Override
    public ApiResponse<Void> delete(Integer id) {
        ExamRoutine routine = examRoutineRepository.findById(id).orElse(null);
        if (routine == null) return ApiResponse.error("Exam routine not found");

        if (!routine.getIsActive()) return ApiResponse.error("Exam routine is already inactive");

        if (routine.getStatus() == RoutineStatus.PUBLISHED) {
            return ApiResponse.error("Cannot deactivate a published routine. Unpublish it first");
        }
        routine.setIsActive(false);
        examRoutineRepository.save(routine);
        return ApiResponse.success("Exam routine deleted successfully", null);
    }
}