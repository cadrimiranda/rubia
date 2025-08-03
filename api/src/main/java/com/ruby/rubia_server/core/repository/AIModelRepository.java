package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.AIModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AIModelRepository extends JpaRepository<AIModel, UUID> {

    List<AIModel> findByIsActiveTrueOrderBySortOrderAscNameAsc();

    List<AIModel> findAllByOrderBySortOrderAscNameAsc();

    List<AIModel> findByProviderOrderBySortOrderAscNameAsc(String provider);

    Optional<AIModel> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT COUNT(m) FROM AIModel m WHERE m.isActive = true")
    long countActiveModels();
}