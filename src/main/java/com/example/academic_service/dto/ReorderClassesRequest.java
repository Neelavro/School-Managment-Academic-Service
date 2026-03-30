package com.example.academic_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReorderClassesRequest {
    private Integer shiftId;
    private List<Integer> classIds; // ordered list of class IDs
}