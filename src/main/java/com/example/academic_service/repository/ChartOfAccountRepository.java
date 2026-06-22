package com.example.academic_service.repository;

import com.example.academic_service.entity.AccountType;
import com.example.academic_service.entity.ChartOfAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, Long> {

    Optional<ChartOfAccount> findByAccountCode(String accountCode);

    boolean existsByAccountCode(String accountCode);

    List<ChartOfAccount> findByParentId(Long parentId);

    List<ChartOfAccount> findByParentIdIsNull();

    List<ChartOfAccount> findByAccountType(AccountType accountType);

    long countByParentId(Long parentId);
}
