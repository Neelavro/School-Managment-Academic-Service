package com.example.academic_service.controller;

import com.example.academic_service.entity.Gender;
import com.example.academic_service.entity.GenderSection;
import com.example.academic_service.service.GenderService;
import com.example.academic_service.service.impl.GenderSectionServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/genders")
public class GenderController {

    private final GenderSectionServiceImpl genderService;

    public GenderController(GenderSectionServiceImpl genderService) {
        this.genderService = genderService;
    }


    // Read all
    @GetMapping
    public ResponseEntity<List<GenderSection>> getAllGenders() {
        List<GenderSection> genders = genderService.getAllGenderSections();
        return ResponseEntity.ok(genders);
    }

    // Read by ID

}
