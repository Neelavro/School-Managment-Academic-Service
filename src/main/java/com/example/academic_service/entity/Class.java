package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "class")
@Getter
@Setter
public class Class {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shift_id", nullable = true)
    private Shift shift;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "grading_policy_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private GradingPolicy gradingPolicy;

    @Column(name = "use_gpa_for_result")
    private Boolean useGpaForResult = false;

    // ✅ Replace single studentGroup with a collection
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "class_student_group",                          // junction table name
            joinColumns = @JoinColumn(name = "class_id"),          // FK to this entity
            inverseJoinColumns = @JoinColumn(name = "student_group_id") // FK to the other
    )
    private Set<StudentGroup> studentGroups = new HashSet<>();

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "is_active")
    private Boolean isActive = true;
}