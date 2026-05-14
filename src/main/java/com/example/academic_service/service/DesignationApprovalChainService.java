package com.example.academic_service.service;

import com.example.academic_service.entity.DesignationApprovalChain;
import com.example.academic_service.repository.DesignationApprovalChainRepository;
import com.example.academic_service.repository.DesignationRepository;
import com.example.academic_service.repository.FbacRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DesignationApprovalChainService {
    private final DesignationApprovalChainRepository chainRepository;
    private final DesignationRepository designationRepository;
    private final FbacRoleRepository fbacRoleRepository;

    public List<DesignationApprovalChain> getChain(Integer designationId) {
        return chainRepository.findByDesignationIdOrderByTierOrder(designationId);
    }

    public DesignationApprovalChain addTier(Integer designationId, String tierLabel, Integer approverRoleId) {
        int nextOrder = chainRepository.countByDesignationId(designationId) + 1;
        DesignationApprovalChain tier = new DesignationApprovalChain();
        tier.setDesignation(designationRepository.findById(designationId)
                .orElseThrow(() -> new IllegalArgumentException("Designation not found: " + designationId)));
        tier.setTierOrder(nextOrder);
        tier.setTierLabel(tierLabel);
        if (approverRoleId != null)
            tier.setApproverRole(fbacRoleRepository.findById(approverRoleId)
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: " + approverRoleId)));
        return chainRepository.save(tier);
    }

    @Transactional
    public void deleteChain(Integer designationId) {
        chainRepository.deleteByDesignationId(designationId);
    }

    public void deleteTier(Integer tierId) {
        chainRepository.deleteById(tierId);
    }

    // Replace entire chain for a designation
    @Transactional
    public List<DesignationApprovalChain> replaceChain(Integer designationId,
            List<Map<String, Object>> tiers) {
        chainRepository.deleteByDesignationId(designationId);
        int order = 1;
        var designation = designationRepository.findById(designationId)
                .orElseThrow(() -> new IllegalArgumentException("Designation not found: " + designationId));
        for (Map<String, Object> t : tiers) {
            DesignationApprovalChain tier = new DesignationApprovalChain();
            tier.setDesignation(designation);
            tier.setTierOrder(order++);
            tier.setTierLabel((String) t.get("tierLabel"));
            Object roleId = t.get("approverRoleId");
            if (roleId != null)
                tier.setApproverRole(fbacRoleRepository.findById(((Number) roleId).intValue())
                        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId)));
            chainRepository.save(tier);
        }
        return chainRepository.findByDesignationIdOrderByTierOrder(designationId);
    }
}
