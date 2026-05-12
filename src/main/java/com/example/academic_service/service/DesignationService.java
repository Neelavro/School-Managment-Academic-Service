package com.example.academic_service.service;

import com.example.academic_service.entity.Designation;
import com.example.academic_service.entity.DesignationPromotion;
import com.example.academic_service.repository.DesignationPromotionRepository;
import com.example.academic_service.repository.DesignationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DesignationService {

    private final DesignationRepository designationRepository;
    private final DesignationPromotionRepository promotionRepository;

    public List<Designation> getAll() {
        return designationRepository.findAll();
    }

    public List<Designation> getActive() {
        return designationRepository.findByIsActive(true);
    }

    public Designation create(Designation designation) {
        if (designationRepository.existsByName(designation.getName()))
            throw new IllegalArgumentException("Designation already exists: " + designation.getName());
        designation.setIsActive(true);
        return designationRepository.save(designation);
    }

    public Designation update(Integer id, Designation req) {
        Designation existing = designationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Designation not found: " + id));
        existing.setName(req.getName());
        existing.setDescription(req.getDescription());
        return designationRepository.save(existing);
    }

    public void deactivate(Integer id) {
        Designation d = designationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Designation not found: " + id));
        d.setIsActive(false);
        designationRepository.save(d);
    }

    public List<DesignationPromotion> getPromotionMatrix() {
        return promotionRepository.findAll();
    }

    public DesignationPromotion addPromotionPath(Integer fromId, Integer toId) {
        DesignationPromotion dp = new DesignationPromotion();
        dp.setFromDesignation(designationRepository.getReferenceById(fromId));
        dp.setToDesignation(designationRepository.getReferenceById(toId));
        return promotionRepository.save(dp);
    }

    public void removePromotionPath(Integer id) {
        promotionRepository.deleteById(id);
    }
}
