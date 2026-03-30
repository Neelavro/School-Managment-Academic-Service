package com.example.academic_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "student_group")
@Getter
@Setter
public class StudentGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "group_name")
    private String groupName;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // ✅ "mappedBy" points to the field name in Class
    @JsonIgnore  // ✅ breaks the infinite loop
    @ManyToMany(mappedBy = "studentGroups", fetch = FetchType.EAGER)
    private Set<Class> classes = new HashSet<>();
}
