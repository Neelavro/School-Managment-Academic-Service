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
import java.util.Objects;
import java.util.stream.Collectors;

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
    private final StudentMarkRepository studentMarkRepository;

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

        // Hard-delete any soft-deleted record with same combination so the unique constraint is cleared
        markingStructureRepository
                .findByExamTypeAndExamClassAndSubjectAndGroupAndDeletedAtIsNotNull(examType, examClass, subject, group)
                .ifPresent(softDeleted -> {
                    markingStructureComponentRepository.deleteAll(
                            markingStructureComponentRepository.findAllByMarkingStructure(softDeleted));
                    markingStructureComponentRepository.flush();
                    markingStructureRepository.delete(softDeleted);
                    markingStructureRepository.flush();
                });

        validateComponents(request.getComponents(), request.getTotalMarks());

        MarkingStructure structure = new MarkingStructure();
        structure.setExamType(examType);
        structure.setExamClass(examClass);
        structure.setSubject(subject);
        structure.setGroup(group);
        structure.setTotalMarks(request.getTotalMarks());
        structure.setPassMarks(request.getPassMarks());
        markingStructureRepository.save(structure);

        saveComponents(structure, request.getComponents());

        return Map.of("message", "Marking structure created successfully", "data", toResponse(structure));
    }

    public Map<String, Object> getByFilters(
            Integer examTypeId, Integer classId, Integer subjectId, Integer groupId) {
        List<MarkingStructure> structures = groupId != null
                ? markingStructureRepository.findAllByGroupIdOrNullAndFilters(examTypeId, classId, subjectId, groupId)
                : markingStructureRepository.findAllByFiltersAndDeletedAtIsNull(examTypeId, classId, subjectId);
        List<MarkingStructureResponse> list = structures.stream().map(this::toResponse).toList();
        return Map.of("message", "Marking structures fetched successfully", "data", list);
    }

    @Transactional
    public Map<String, Object> update(Integer id, MarkingStructureRequest request) {
        MarkingStructure structure = markingStructureRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Marking structure not found with id: " + id));

        // handle group change
        Integer newGroupId = request.getGroupId();
        Integer currentGroupId = structure.getGroup() != null ? structure.getGroup().getId() : null;
        if (!Objects.equals(newGroupId, currentGroupId)) {
            // block if marks have already been entered using this structure's components
            List<Integer> componentIds = markingStructureComponentRepository
                    .findAllByMarkingStructureAndDeletedAtIsNull(structure).stream()
                    .map(c -> c.getExamComponent().getId())
                    .collect(Collectors.toList());
            if (!componentIds.isEmpty() && studentMarkRepository
                    .existsBySubjectIdAndExamComponentIdInAndClassIdAndExamTypeId(
                            structure.getSubject().getId(), componentIds,
                            structure.getExamClass().getId(), structure.getExamType().getId())) {
                throw new RuntimeException(
                        "Cannot change group: marks have already been entered using this marking structure. " +
                        "Please delete and recreate instead.");
            }

            StudentGroup newGroup = null;
            if (newGroupId != null) {
                newGroup = studentGroupRepository.findById(newGroupId)
                        .orElseThrow(() -> new RuntimeException("Group not found with id: " + newGroupId));
            }
            boolean exists = markingStructureRepository
                    .existsByExamTypeAndExamClassAndSubjectAndGroupAndDeletedAtIsNull(
                            structure.getExamType(), structure.getExamClass(), structure.getSubject(), newGroup);
            if (exists) {
                throw new RuntimeException("A marking structure already exists for this exam type, class, subject and group combination");
            }
            final StudentGroup finalNewGroup = newGroup;
            markingStructureRepository
                    .findByExamTypeAndExamClassAndSubjectAndGroupAndDeletedAtIsNotNull(
                            structure.getExamType(), structure.getExamClass(), structure.getSubject(), finalNewGroup)
                    .ifPresent(softDeleted -> {
                        markingStructureComponentRepository.deleteAll(
                                markingStructureComponentRepository.findAllByMarkingStructure(softDeleted));
                        markingStructureComponentRepository.flush();
                        markingStructureRepository.delete(softDeleted);
                        markingStructureRepository.flush();
                    });
            structure.setGroup(newGroup);
        }

        validateComponents(request.getComponents(), request.getTotalMarks());

        structure.setTotalMarks(request.getTotalMarks());
        structure.setPassMarks(request.getPassMarks());
        structure.setLastModifiedAt(LocalDateTime.now());
        markingStructureRepository.save(structure);

        // hard delete old components, flush before re-inserting to avoid unique constraint violation
        List<MarkingStructureComponent> existing =
                markingStructureComponentRepository.findAllByMarkingStructureAndDeletedAtIsNull(structure);
        markingStructureComponentRepository.deleteAll(existing);
        markingStructureComponentRepository.flush();

        saveComponents(structure, request.getComponents());

        return Map.of("message", "Marking structure updated successfully", "data", toResponse(structure));
    }

    public Map<String, Object> hasMarks(Integer id) {
        MarkingStructure structure = markingStructureRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Marking structure not found with id: " + id));
        List<Integer> componentIds = markingStructureComponentRepository
                .findAllByMarkingStructureAndDeletedAtIsNull(structure).stream()
                .map(c -> c.getExamComponent().getId())
                .collect(Collectors.toList());
        boolean has = !componentIds.isEmpty() && studentMarkRepository
                .existsBySubjectIdAndExamComponentIdInAndClassIdAndExamTypeId(
                        structure.getSubject().getId(), componentIds,
                        structure.getExamClass().getId(), structure.getExamType().getId());
        return Map.of("hasMarks", has);
    }

    @Transactional
    public Map<String, String> clearMarks(Integer id) {
        MarkingStructure structure = markingStructureRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Marking structure not found with id: " + id));
        List<Integer> componentIds = markingStructureComponentRepository
                .findAllByMarkingStructureAndDeletedAtIsNull(structure).stream()
                .map(c -> c.getExamComponent().getId())
                .collect(Collectors.toList());
        if (!componentIds.isEmpty()) {
            studentMarkRepository.deleteBySubjectIdAndExamComponentIdInAndClassIdAndExamTypeId(
                    structure.getSubject().getId(), componentIds,
                    structure.getExamClass().getId(), structure.getExamType().getId());
        }
        return Map.of("message", "Marks cleared successfully");
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
            msc.setPassMarks(req.getPassMarks());
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
        res.setPassMarks(s.getPassMarks());
        res.setIsActive(s.getIsActive());

        List<MarkingStructureComponent> components =
                markingStructureComponentRepository.findAllByMarkingStructureAndDeletedAtIsNull(s);
        res.setComponents(components.stream().map(c -> {
            MarkingStructureComponentResponse cr = new MarkingStructureComponentResponse();
            cr.setId(c.getId());
            cr.setExamComponentId(c.getExamComponent().getId());
            cr.setExamComponentName(c.getExamComponent().getName());
            cr.setMaxMarks(c.getMaxMarks());
            cr.setPassMarks(c.getPassMarks());
            return cr;
        }).toList());

        return res;
    }
    @Transactional
    public Map<String, Object> copyFromExamType(CopyMarkingStructureRequest request) {
        if (request.getFromExamTypeId() == null || request.getToExamTypeId() == null) {
            throw new RuntimeException("Both source and target exam types are required");
        }
        if (Objects.equals(request.getFromExamTypeId(), request.getToExamTypeId())) {
            throw new RuntimeException("Source and target exam types must be different");
        }

        ExamType fromExamType = examTypeRepository.findByIdAndIsActiveTrue(request.getFromExamTypeId())
                .orElseThrow(() -> new RuntimeException("Source exam type not found with id: " + request.getFromExamTypeId()));
        ExamType toExamType = examTypeRepository.findByIdAndIsActiveTrue(request.getToExamTypeId())
                .orElseThrow(() -> new RuntimeException("Target exam type not found with id: " + request.getToExamTypeId()));

        List<Integer> classIds = request.getClassIds();
        List<MarkingStructure> sources = classIds == null || classIds.isEmpty()
                ? markingStructureRepository.findAllByExamType_IdAndDeletedAtIsNull(fromExamType.getId())
                : markingStructureRepository.findAllByExamType_IdAndExamClass_IdInAndDeletedAtIsNull(
                        fromExamType.getId(), classIds);

        List<MarkingStructureResponse> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (MarkingStructure src : sources) {
            Class examClass = src.getExamClass();
            Subject subject = src.getSubject();
            StudentGroup group = src.getGroup();
            String label = examClass.getName() + " / " + subject.getName()
                    + (group != null ? " / " + group.getGroupName() : "");

            boolean exists = markingStructureRepository
                    .existsByExamTypeAndExamClassAndSubjectAndGroupAndDeletedAtIsNull(
                            toExamType, examClass, subject, group);
            if (exists) {
                skipped.add(label + " already has a structure under " + toExamType.getName());
                continue;
            }

            // Same collision-with-soft-deleted handling as create/bulkCreate
            markingStructureRepository
                    .findByExamTypeAndExamClassAndSubjectAndGroupAndDeletedAtIsNotNull(
                            toExamType, examClass, subject, group)
                    .ifPresent(softDeleted -> {
                        markingStructureComponentRepository.deleteAll(
                                markingStructureComponentRepository.findAllByMarkingStructure(softDeleted));
                        markingStructureComponentRepository.flush();
                        markingStructureRepository.delete(softDeleted);
                        markingStructureRepository.flush();
                    });

            MarkingStructure copy = new MarkingStructure();
            copy.setExamType(toExamType);
            copy.setExamClass(examClass);
            copy.setSubject(subject);
            copy.setGroup(group);
            copy.setTotalMarks(src.getTotalMarks());
            copy.setPassMarks(src.getPassMarks());
            markingStructureRepository.save(copy);

            List<MarkingStructureComponent> srcComponents =
                    markingStructureComponentRepository.findAllByMarkingStructureAndDeletedAtIsNull(src);
            List<MarkingStructureComponent> newComponents = srcComponents.stream().map(sc -> {
                MarkingStructureComponent nc = new MarkingStructureComponent();
                nc.setMarkingStructure(copy);
                nc.setExamComponent(sc.getExamComponent());
                nc.setMaxMarks(sc.getMaxMarks());
                nc.setPassMarks(sc.getPassMarks());
                return nc;
            }).toList();
            markingStructureComponentRepository.saveAll(newComponents);

            created.add(toResponse(copy));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("message", created.size() + " marking structure(s) copied from "
                + fromExamType.getName() + " to " + toExamType.getName()
                + (skipped.isEmpty() ? "" : ", " + skipped.size() + " skipped"));
        result.put("data", created);
        if (!skipped.isEmpty()) result.put("skipped", skipped);
        return result;
    }

    @Transactional
    public Map<String, Object> bulkCreate(BulkMarkingStructureRequest request) {
        if (request.getSubjectIds() == null || request.getSubjectIds().isEmpty()) {
            throw new RuntimeException("At least one subject must be selected");
        }

        ExamType examType = examTypeRepository.findByIdAndIsActiveTrue(request.getExamTypeId())
                .orElseThrow(() -> new RuntimeException("Exam type not found with id: " + request.getExamTypeId()));

        Class examClass = classRepository.findByIdAndIsActiveTrue(request.getClassId())
                .orElseThrow(() -> new RuntimeException("Class not found with id: " + request.getClassId()));

        StudentGroup group = null;
        if (request.getGroupId() != null) {
            group = studentGroupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new RuntimeException("Group not found with id: " + request.getGroupId()));
        }

        validateComponents(request.getComponents(), request.getTotalMarks());

        List<MarkingStructureResponse> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (Integer subjectId : request.getSubjectIds()) {
            Subject subject = subjectRepository.findByIdAndIsActiveTrue(subjectId)
                    .orElse(null);
            if (subject == null) {
                skipped.add("Subject id " + subjectId + " not found");
                continue;
            }

            boolean exists = markingStructureRepository
                    .existsByExamTypeAndExamClassAndSubjectAndGroupAndDeletedAtIsNull(
                            examType, examClass, subject, group);
            if (exists) {
                skipped.add(subject.getName() + " already has a structure for this combination");
                continue;
            }

            markingStructureRepository
                    .findByExamTypeAndExamClassAndSubjectAndGroupAndDeletedAtIsNotNull(examType, examClass, subject, group)
                    .ifPresent(softDeleted -> {
                        markingStructureComponentRepository.deleteAll(
                                markingStructureComponentRepository.findAllByMarkingStructure(softDeleted));
                        markingStructureComponentRepository.flush();
                        markingStructureRepository.delete(softDeleted);
                        markingStructureRepository.flush();
                    });

            MarkingStructure structure = new MarkingStructure();
            structure.setExamType(examType);
            structure.setExamClass(examClass);
            structure.setSubject(subject);
            structure.setGroup(group);
            structure.setTotalMarks(request.getTotalMarks());
            structure.setPassMarks(request.getPassMarks());
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