package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.DocumentResponse;
import com.br.psyke.psyke.dto.UpdateDocumentRequest;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.Document;
import com.br.psyke.psyke.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documents;

    @Value("${app.upload.path}")
    private String uploadPath;

    private Path root;

    @PostConstruct
    void init() throws IOException {
        root = Paths.get(uploadPath).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public Page<DocumentResponse> findAll(UUID clinicId, UUID patientId, Pageable pageable) {
        if (patientId != null)
            return documents.findByClinicIdAndPatientId(clinicId, patientId, pageable).map(this::toResponse);
        return documents.findByClinicId(clinicId, pageable).map(this::toResponse);
    }

    public DocumentResponse findById(UUID id, UUID clinicId) {
        return documents.findByIdAndClinicId(id, clinicId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
    }

    public Resource download(UUID id, UUID clinicId) {
        var doc = documents.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        try {
            var file = root.resolve(doc.getStoragePath()).normalize();
            var resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable())
                throw new ResourceNotFoundException("File not found on disk");
            return resource;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error reading file", e);
        }
    }

    public String getOriginalFilename(UUID id, UUID clinicId) {
        var doc = documents.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        return doc.getOriginalName();
    }

    @Transactional
    public DocumentResponse upload(MultipartFile file, UUID clinicId, UUID patientId, UUID appointmentId, UUID professionalId, String notes) throws IOException {
        var originalName = file.getOriginalFilename();
        var extension = "";
        if (originalName != null && originalName.contains("."))
            extension = originalName.substring(originalName.lastIndexOf("."));

        var storageName = UUID.randomUUID().toString() + extension;
        var clinicDir = root.resolve(clinicId.toString());
        Files.createDirectories(clinicDir);
        var targetPath = clinicDir.resolve(storageName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        var doc = Document.builder()
                .clinicId(clinicId)
                .patientId(patientId)
                .appointmentId(appointmentId)
                .professionalId(professionalId)
                .originalName(originalName != null ? originalName : "unknown")
                .storagePath(clinicId + "/" + storageName)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .notes(notes)
                .build();
        doc = documents.save(doc);
        return toResponse(doc);
    }

    @Transactional
    public void delete(UUID id, UUID clinicId) {
        var doc = documents.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        try {
            var file = root.resolve(doc.getStoragePath()).normalize();
            Files.deleteIfExists(file);
        } catch (IOException ignored) {}
        documents.delete(doc);
    }

    private DocumentResponse toResponse(Document d) {
        return new DocumentResponse(
                d.getId().toString(), d.getClinicId(),
                d.getPatientId(), d.getAppointmentId(), d.getProfessionalId(),
                d.getOriginalName(), d.getFileType(), d.getFileSize(),
                d.getNotes(), d.getCreatedAt(), d.getUpdatedAt());
    }
}
