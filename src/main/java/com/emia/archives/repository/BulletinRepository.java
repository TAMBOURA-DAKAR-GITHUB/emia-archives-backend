package com.emia.archives.repository;

import com.emia.archives.entity.Bulletin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BulletinRepository extends JpaRepository<Bulletin, Long> {

    Optional<Bulletin> findByStagiaireId(Long stagiaireId);
}