package com.br.psyke.psyke.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/** File bytes kept apart from {@link Document} so listing documents never loads file contents. */
@Entity
@Table(name = "document_contents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentContent {

    @Id
    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "content", nullable = false, columnDefinition = "BYTEA")
    private byte[] content;
}
