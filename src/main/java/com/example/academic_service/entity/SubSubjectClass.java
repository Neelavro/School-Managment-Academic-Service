package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * A class a sub-subject applies to (user, 2026-11-12). A parent subject is split into its sub-subjects only in the
 * classes they're assigned to; in other classes it's an ordinary subject.
 */
@Entity
@Table(name = "sub_subject_class",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sub_subject_id", "class_id"}))
@Getter
@Setter
public class SubSubjectClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "sub_subject_id", nullable = false)
    private Integer subSubjectId;

    @Column(name = "class_id", nullable = false)
    private Integer classId;
}
