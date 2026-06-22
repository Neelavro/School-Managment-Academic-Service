package com.example.academic_service.repository;

import com.example.academic_service.entity.AccountingSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountingSettingsRepository extends JpaRepository<AccountingSettings, Long> {
}
