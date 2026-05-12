package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "fbac_permission",
        uniqueConstraints = @UniqueConstraint(columnNames = {"fbac_role_id", "submodule"}),
        indexes = @Index(name = "idx_fbac_permission_role", columnList = "fbac_role_id"))
@Getter
@Setter
public class FbacPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fbac_role_id", nullable = false)
    private FbacRole fbacRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "submodule", nullable = false)
    private Submodule submodule;

    @Column(name = "can_create")
    private Boolean canCreate = false;

    @Column(name = "can_read")
    private Boolean canRead = false;

    @Column(name = "can_update")
    private Boolean canUpdate = false;

    @Column(name = "can_delete")
    private Boolean canDelete = false;
}
