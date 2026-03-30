package com.example.academic_service.service.impl;

import com.example.academic_service.entity.Gender;
import com.example.academic_service.repository.GenderRepository;
import com.example.academic_service.service.GenderService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GenderServiceImpl implements GenderService {

    private final GenderRepository genderRepository;

    public GenderServiceImpl(GenderRepository genderRepository) {
        this.genderRepository = genderRepository;
    }

    @Override
    public Gender createGender(Gender gender) {
        return genderRepository.save(gender);
    }

    @Override
    public List<Gender> getAllGenders() {
        return genderRepository.findAll();
    }

    @Override
    public Gender getGenderById(Integer id) {
        Optional<Gender> gender = genderRepository.findById(id);
        return gender.orElse(null); // or throw exception
    }

    @Override
    public Gender updateGender(Integer id, Gender gender) {
        Gender existing = getGenderById(id);
        if (existing != null) {
            existing.setGender(gender.getGender());
            existing.setIsActive(gender.getIsActive());
            return genderRepository.save(existing);
        }
        return null; // or throw exception
    }

    @Override
    public void deleteGender(Integer id) {
        genderRepository.deleteById(id);
    }
}
