package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.DocumentContent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DocumentContentRepository extends JpaRepository<DocumentContent, UUID> {}
