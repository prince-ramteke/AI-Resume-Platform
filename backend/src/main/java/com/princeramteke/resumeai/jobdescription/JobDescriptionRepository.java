package com.princeramteke.resumeai.jobdescription;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobDescriptionRepository extends JpaRepository<JobDescription, Long> {

    Optional<JobDescription> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);

    Optional<JobDescription> findByIdAndDeletedFalse(Long id);

    Page<JobDescription> findAllByUserIdAndDeletedFalse(Long userId, Pageable pageable);

    Page<JobDescription> findAllByUserIdAndDeletedFalseAndTitleContainingIgnoreCase(
            Long userId, String title, Pageable pageable);
}
