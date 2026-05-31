package com.br.psyke.psyke;

import com.br.psyke.psyke.dto.UpdateDocumentRequest;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.Document;
import com.br.psyke.psyke.repository.DocumentRepository;
import com.br.psyke.psyke.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private DocumentRepository documents;
    private DocumentService service;
    private UUID clinicId;
    private Document existingDoc;

    @BeforeEach
    void setUp() {
        service = new DocumentService(documents);
        var tmpDir = System.getProperty("java.io.tmpdir") + "/psyke-test";
        ReflectionTestUtils.setField(service, "uploadPath", tmpDir);
        ReflectionTestUtils.invokeMethod(service, "init");
        clinicId = UUID.randomUUID();
        existingDoc = Document.builder()
                .id(UUID.randomUUID()).clinicId(clinicId)
                .originalName("relatorio.pdf").fileType("application/pdf")
                .fileSize(1024).storagePath(clinicId + "/test-uuid.pdf")
                .build();
    }

    @Test
    void shouldFindById() {
        when(documents.findByIdAndClinicId(existingDoc.getId(), clinicId))
                .thenReturn(Optional.of(existingDoc));

        var resp = service.findById(existingDoc.getId(), clinicId);
        assertThat(resp.originalName()).isEqualTo("relatorio.pdf");
    }

    @Test
    void shouldThrowWhenNotFound() {
        var id = UUID.randomUUID();
        when(documents.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id, clinicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldFindAllWithPagination() {
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(existingDoc));

        when(documents.findByClinicId(clinicId, pageable)).thenReturn(page);

        var resp = service.findAll(clinicId, null, pageable);
        assertThat(resp).hasSize(1);
    }

    @Test
    void shouldDeleteDocument() {
        when(documents.findByIdAndClinicId(existingDoc.getId(), clinicId))
                .thenReturn(Optional.of(existingDoc));

        service.delete(existingDoc.getId(), clinicId);
        verify(documents).delete(existingDoc);
    }

    @Test
    void shouldThrowOnDeleteWhenNotFound() {
        var id = UUID.randomUUID();
        when(documents.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id, clinicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
