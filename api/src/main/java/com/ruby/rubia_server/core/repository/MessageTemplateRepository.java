package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.MessageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, UUID> {
}