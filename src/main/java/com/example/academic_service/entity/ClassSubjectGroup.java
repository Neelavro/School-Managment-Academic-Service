package com.example.academic_service.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.util.HashSet;
import java.util.Set;
@Entity
@Table(name = "class_subject_group",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"class_id", "subject_id", "student_group_id"}
        ))
@Getter
@Setter
public class ClassSubjectGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "class_id", nullable = false)
    private Class studentClass;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_group_id", nullable = true)
    @NotFound(action = NotFoundAction.IGNORE)
    private StudentGroup studentGroup;

    @Column(name = "is_active")
    private Boolean isActive = true;
}