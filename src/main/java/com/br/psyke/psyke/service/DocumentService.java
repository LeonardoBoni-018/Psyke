package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.DocumentResponse;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.Document;
import com.br.psyke.psyke.model.DocumentContent;
import com.br.psyke.psyke.repository.DocumentContentRepository;
import com.br.psyke.psyke.repository.DocumentRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Files are stored in PostgreSQL ({@code document_contents}) so they survive redeploys on hosts with
 * ephemeral disks. Documents uploaded by older versions (on disk) can still be downloaded.
 */
@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final long MAX_SIZE = 10L * 1024 * 1024;

    /** Allow-list: extension → content type served on download. */
    private static final Map<String, String> ALLOWED = Map.of(
            "pdf", "application/pdf",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "webp", "image/webp",
            "txt", "text/plain",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final DocumentRepository documents;
    private final DocumentContentRepository contents;

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

    public Download download(UUID id, UUID clinicId) {
        var doc = documents.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        var stored = contents.findById(doc.getId());
        if (stored.isPresent())
            return new Download(new ByteArrayResource(stored.get().getContent()), doc.getOriginalName(), contentType(doc));
        return new Download(legacyFile(doc), doc.getOriginalName(), contentType(doc));
    }

    /** @deprecated use {@link #download(UUID, UUID)}, which also returns name and type. */
    @Deprecated
    public String getOriginalFilename(UUID id, UUID clinicId) {
        return findById(id, clinicId).originalName();
    }

    @Transactional
    public DocumentResponse upload(MultipartFile file, UUID clinicId, UUID patientId, UUID appointmentId, UUID professionalId, String notes) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("Arquivo vazio");
        if (file.getSize() > MAX_SIZE) throw new IllegalArgumentException("Arquivo maior que 10 MB");

        var originalName = sanitizeName(file.getOriginalFilename());
        var extension = extensionOf(originalName);
        if (!ALLOWED.containsKey(extension))
            throw new IllegalArgumentException("Tipo de arquivo não permitido. Use PDF, imagem, TXT ou Word.");

        var doc = documents.save(Document.builder()
                .clinicId(clinicId)
                .patientId(patientId)
                .appointmentId(appointmentId)
                .professionalId(professionalId)
                .originalName(originalName)
                .fileType(ALLOWED.get(extension))
                .fileSize(file.getSize())
                .notes(notes)
                .build());
        contents.save(DocumentContent.builder().documentId(doc.getId()).content(file.getBytes()).build());
        return toResponse(doc);
    }

    @Transactional
    public void delete(UUID id, UUID clinicId) {
        var doc = documents.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        contents.findById(doc.getId()).ifPresent(contents::delete);
        if (doc.getStoragePath() != null) {
            try {
                Files.deleteIfExists(resolveLegacy(doc.getStoragePath()));
            } catch (IOException | IllegalArgumentException ignored) {
                // best effort for legacy files
            }
        }
        documents.delete(doc);
    }

    private Resource legacyFile(Document doc) {
        if (doc.getStoragePath() == null) throw new ResourceNotFoundException("File not found");
        try {
            var resource = new UrlResource(resolveLegacy(doc.getStoragePath()).toUri());
            if (!resource.exists() || !resource.isReadable())
                throw new ResourceNotFoundException("File not found");
            return resource;
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File not found");
        }
    }

    private Path resolveLegacy(String storagePath) {
        var file = root.resolve(storagePath).normalize();
        if (!file.startsWith(root)) throw new IllegalArgumentException("Invalid storage path");
        return file;
    }

    private static String contentType(Document doc) {
        var type = ALLOWED.get(extensionOf(doc.getOriginalName()));
        return type != null ? type : "application/octet-stream";
    }

    private static String extensionOf(String name) {
        var dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** Drops any path, control characters and quotes; keeps at most 200 characters. */
    static String sanitizeName(String original) {
        if (original == null || original.isBlank()) return "arquivo";
        var name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[\\p{Cntrl}\"<>|:*?]", "_").trim();
        if (name.isEmpty()) name = "arquivo";
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }

    private DocumentResponse toResponse(Document d) {
        return new DocumentResponse(
                d.getId().toString(), d.getClinicId(),
                d.getPatientId(), d.getAppointmentId(), d.getProfessionalId(),
                d.getOriginalName(), d.getFileType(), d.getFileSize(),
                d.getNotes(), d.getCreatedAt(), d.getUpdatedAt());
    }

    public record Download(Resource resource, String filename, String contentType) {}
}
