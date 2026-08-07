package com.princeramteke.resumeai.resume;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Optional<Resume> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);

    Optional<Resume> findByIdAndDeletedFalse(Long id);

    Page<Resume> findAllByUserIdAndDeletedFalse(Long userId, Pageable pageable);
}
