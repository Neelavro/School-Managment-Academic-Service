package com.example.academic_service.service;

import com.example.academic_service.entity.Designation;
import com.example.academic_service.entity.Staff;
import com.example.academic_service.entity.StaffPromotion;
import com.example.academic_service.repository.DesignationRepository;
import com.example.academic_service.repository.StaffPromotionRepository;
import com.example.academic_service.repository.StaffRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffPromotionService {

    private final StaffPromotionRepository promotionRepository;
    private final StaffRepository staffRepository;
    private final DesignationRepository designationRepository;

    public List<StaffPromotion> getHistory(Long staffId) {
        return promotionRepository.findByStaffIdOrderByPromotionDateDesc(staffId);
    }

    @Transactional
    public StaffPromotion record(Long staffId, StaffPromotion req) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));

        Designation toDesignation = designationRepository.findById(req.getToDesignation().getId())
                .orElseThrow(() -> new IllegalArgumentException("Designation not found"));

        StaffPromotion promotion = new StaffPromotion();
        promotion.setStaff(staff);
        promotion.setFromDesignation(staff.getCurrentDesignation());
        promotion.setToDesignation(toDesignation);
        promotion.setPromotionDate(req.getPromotionDate());
        promotion.setNotes(req.getNotes());
        promotion.setRecordedAt(LocalDateTime.now());

        staff.setCurrentDesignation(toDesignation);
        staffRepository.save(staff);

        return promotionRepository.save(promotion);
    }
}
