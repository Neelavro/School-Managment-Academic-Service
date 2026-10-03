package com.example.academic_service.repository;

import com.example.academic_service.entity.AccountingSettings;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountingSettingsRepository extends JpaRepository<AccountingSettings, Long> {

    /** Locks the settings row until the transaction ends: journal entries are numbered one at a time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AccountingSettings s WHERE s.id = :id")
    Optional<AccountingSettings> lockById(@Param("id") Long id);
}
