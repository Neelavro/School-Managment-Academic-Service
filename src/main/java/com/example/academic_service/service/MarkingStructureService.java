package com.example.academic_service.service;

import com.example.academic_service.dto.marking_dtos.*;
import com.example.academic_service.entity.*;
import com.example.academic_service.entity.Class;
import com.example.academic_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MarkingStructureService {

    private final MarkingStructureRepository markingStructureRepository;
    private final MarkingStructureComponentRepository markingStructureComponentRepository;
    private final ExamTypeRepository examTypeRepository;
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final ExamComponentRepository examComponentRepository;

    @Transactional
    public Map<String, Object> create(MarkingStructureRequest request) {
        ExamType examType = examTypeRepository.findByIdAndIsActiveTrue(request.getExamTypeId())
                .orElseThrow(() -> new RuntimeException("Exam type not found with id: " + request.getExamTypeId()));

        Class examClass = classRepository.findByIdAndIsActiveTrue(request.getClassId())
                .orElseThrow(() -> new RuntimeException("Class not found with id: " + request.getClassId()));

        Subject subject = subjectRepository.findByIdAndIsActiveTrue(request.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found with id: " + request.getSubjectId()));

        StudentGroup group = null;
        if (request.getGroupId() != null) {
            group = studentGroupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new RuntimeException("Group not found with id: " + request.getGroupId()));
        }

        boolean exists = markingStructureRepository
                .existsByExamTypeAndExamClassAndSubjectAndGroupAndDeletedAtIsNull(
                        examType, examClass, subject, group);
        if (exists) {
            throw new RuntimeException("Marking structure already exists for this exam type, class, subject and group combination");
        }

        validateComponents(request.getComponents(), request.getTotalMarks());

        MarkingStructure structure = new MarkingStructure();
        structure.setExamType(examType);
        structure.setExamClass(examClass);
        structure.setSubject(subject);
        structure.setGroup(group);
        structure.setTotalMarks(request.getTotalMarks());
        markingStructureRepository.save(structure);

        saveComponents(structure, request.getComponents());

        return Map.of("message", "Marking structure created successfully", "data", toResponse(structure));
    }

    public Map<String, Object> getByFilters(
            Integer examTypeId, Integer classId, Integer subjectId, Integer groupId) {
        List<MarkingStructureResponse> list = markingStructureRepository
                .findAllByFiltersAndDeletedAtIsNull(examTypeId, classId, subjectId, groupId)
                .stream().map(this::toResponse).toList();
        return Map.of("message", "Marking structures fetched successfully", "data", list);
    }

    @Transactional
    public Map<String, Object> update(Integer id, MarkingStructureRequest request) {
        MarkingStructure structure = markingStructureRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Marking structure not found with id: " + id));

        validateComponents(request.getComponents(), request.getTotalMarks());

        structure.setTotalMarks(request.getTotalMarks());
        structure.setLastModifiedAt(LocalDateTime.now());
        markingStructureRepository.save(structure);

        // hard delete old components
        List<MarkingStructureComponent> existing =
                markingStructureComponentRepository.findAllByMarkingStructureAndDeletedAtIsNull(structure);
        markingStructureComponentRepository.deleteAll(existing);

        saveComponents(structure, request.getComponents());

        return Map.of("message", "Marking structure updated successfully", "data", toResponse(structure));
    }

    @Transactional
    public Map<String, String> delete(Integer id) {
        MarkingStructure structure = markingStructureRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Marking structure not found with id: " + id));

        List<MarkingStructureComponent> components =
                markingStructureComponentRepository.findAllByMarkingStructureAndDeletedAtIsNull(structure);
        components.forEach(c -> {
            c.setDeletedAt(LocalDateTime.now());
            c.setIsActive(false);
        });
        markingStructureComponentRepository.saveAll(components);

        structure.setDeletedAt(LocalDateTime.now());
        structure.setIsActive(false);
        markingStructureRepository.save(structure);

        return Map.of("message", "Marking structure for " + structure.getExamType().getName()
                + " - " + structure.getExamClass().getName()
                + " - " + structure.getSubject().getName() + " deleted successfully");
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    private void validateComponents(List<MarkingStructureComponentRequest> components, int totalMarks) {
        if (components == null || components.isEmpty()) {
            throw new RuntimeException("At least one exam component is required");
        }
        int sum = components.stream().mapToInt(MarkingStructureComponentRequest::getMaxMarks).sum();
        if (sum > totalMarks) {
            throw new RuntimeException(
                    "Sum of component marks (" + sum + ") exceeds total marks (" + totalMarks + ")");
        }
    }

    private void saveComponents(MarkingStructure structure, List<MarkingStructureComponentRequest> components) {
        List<MarkingStructureComponent> toSave = components.stream().map(req -> {
            ExamComponent examComponent = examComponentRepository.findByIdAndDeletedAtIsNull(req.getExamComponentId())
                    .orElseThrow(() -> new RuntimeException("Exam component not found with id: " + req.getExamComponentId()));

            MarkingStructureComponent msc = new MarkingStructureComponent();
            msc.setMarkingStructure(structure);
            msc.setExamComponent(examComponent);
            msc.setMaxMarks(req.getMaxMarks());
            return msc;
        }).toList();
        markingStructureComponentRepository.saveAll(toSave);
    }

    private MarkingStructureResponse toResponse(MarkingStructure s) {
        MarkingStructureResponse res = new MarkingStructureResponse();
        res.setId(s.getId());
        res.setExamTypeId(s.getExamType().getId());
        res.setExamTypeName(s.getExamType().getName());
        res.setClassId(s.getExamClass().getId());
        res.setClassName(s.getExamClass().getName());
        res.setSubjectId(s.getSubject().getId());
        res.setSubjectName(s.getSubject().getName());
        res.setGroupId(s.getGroup() != null ? s.getGroup().getId() : null);
        res.setGroupName(s.getGroup() != null ? s.getGroup().getGroupName() : null);
        res.setTotalMarks(s.getTotalMarks());
        res.setIsActive(s.getIsActive());

        List<MarkingStructureComponent> components =
                markingStructureComponentRepository.findAllByMarkingStructureAndDeletedAtIsNull(s);
        res.setComponents(components.stream().map(c -> {
            MarkingStructureComponentResponse cr = new MarkingStructureComponentResponse();
            cr.setId(c.getId());
            cr.setExamComponentId(c.getExamComponent().getId());
            cr.setExamComponentName(c.getExamComponent().getName());
            cr.setMaxMarks(c.getMaxMarks());
            return cr;
        }).toList());

        return res;
    }
    @Transactional
    public Map<String, Object> bulkCreate(BulkMarkingStructureRequest request) {
        if (request.getClassIds() == null || request.getClassIds().isEmpty()) {
            throw new RuntimeException("At least one class must be selected");
        }

        ExamType examType = examTypeRepository.findByIdAndIsActiveTrue(request.getExamTypeId())
                .orElseThrow(() -> new RuntimeException("Exam type not found with id: " + request.getExamTypeId()));

        Subject subject = subjectRepository.findByIdAndIsActiveTrue(request.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found with id: " + request.getSubjectId()));

        StudentGroup group = null;
        if (request.getGroupId() != null) {
            group = studentGroupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new RuntimeException("Group not found with id: " + request.getGroupId()));
        }

        validateComponents(request.getComponents(), request.getTotalMarks());

        List<MarkingStructureResponse> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (Integer classId : request.getClassIds()) {
            Class examClass = classRepository.findByIdAndIsActiveTrue(classId)
                    .orElse(null);
            if (examClass == null) {
                skipped.add("Class id " + classId + " not found");
                continue;
            }

            boolean exists = markingStructureRepository
                    .existsByExamTypeAndExamClassAndSubjectAndGroupAndDeletedAtIsNull(
                            examType, examClass, subject, group);
            if (exists) {
                skipped.add(examClass.getName() + " already has a structure for this combination");
                continue;
            }

            MarkingStructure structure = new MarkingStructure();
            structure.setExamType(examType);
            structure.setExamClass(examClass);
            structure.setSubject(subject);
            structure.setGroup(group);
            structure.setTotalMarks(request.getTotalMarks());
            markingStructureRepository.save(structure);

            saveComponents(structure, request.getComponents());
            created.add(toResponse(structure));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("message", created.size() + " marking structure(s) created successfully" +
                (skipped.isEmpty() ? "" : ", " + skipped.size() + " skipped"));
        result.put("data", created);
        if (!skipped.isEmpty()) result.put("skipped", skipped);
        return result;
    }
}