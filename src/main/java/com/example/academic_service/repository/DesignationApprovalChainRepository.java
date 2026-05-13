package com.example.academic_service.repository;

import com.example.academic_service.entity.DesignationApprovalChain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DesignationApprovalChainRepository extends JpaRepository<DesignationApprovalChain, Integer> {
    List<DesignationApprovalChain> findByDesignationIdOrderByTierOrder(Integer designationId);
    Optional<DesignationApprovalChain> findByDesignationIdAndTierOrder(Integer designationId, Integer tierOrder);
    int countByDesignationId(Integer designationId);
    void deleteByDesignationId(Integer designationId);
    List<DesignationApprovalChain> findByApproverRoleIdIn(List<Integer> roleIds);
}
