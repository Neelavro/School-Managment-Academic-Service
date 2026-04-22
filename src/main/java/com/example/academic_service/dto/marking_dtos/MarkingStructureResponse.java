// Response
package com.example.academic_service.dto.marking_dtos;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class MarkingStructureResponse {
    private Integer id;
    private Integer examTypeId;
    private String examTypeName;
    private Integer classId;
    private String className;
    private Integer subjectId;
    private String subjectName;
    private Integer groupId;
    private String groupName;
    private Integer totalMarks;
    private Boolean isActive;
    private List<MarkingStructureComponentResponse> components;
}