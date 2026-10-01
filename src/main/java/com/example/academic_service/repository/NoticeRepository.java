package com.example.academic_service.repository;

import com.example.academic_service.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /** The board order: pinned first, then newest date, then newest written. */
    List<Notice> findAllByOrderByPinnedDescNoticeDateDescIdDesc();

    List<Notice> findAllByPublishedTrueOrderByPinnedDescNoticeDateDescIdDesc();

    Optional<Notice> findByIdAndPublishedTrue(Long id);
}
