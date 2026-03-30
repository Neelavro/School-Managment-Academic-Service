package com.example.academic_service.service;

import com.example.academic_service.entity.Gender;

import java.util.List;

public interface GenderService {
    Gender createGender(Gender gender);
    List<Gender> getAllGenders();
    Gender getGenderById(Integer id);
    Gender updateGender(Integer id, Gender gender);
    void deleteGender(Integer id);
}
