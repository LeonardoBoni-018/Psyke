package com.br.psyke.psyke.service.prontuario;

import com.br.psyke.psyke.dto.prontuario.*;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.Anamnese;
import com.br.psyke.psyke.model.EvolucaoClinica;
import com.br.psyke.psyke.model.Prontuario;
import com.br.psyke.psyke.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProntuarioService {

    private final ProntuarioRepository prontuarioRepo;
    private final EvolucaoClinicaRepository evolucaoRepo;
    private final AnamneseRepository anamneseRepo;
    private final UserRepository userRepo;

    @Transactional
    public ProntuarioResponse create(ProntuarioRequest req, UUID userId, boolean isAdmin) {
        if (!isAdmin) {
            userRepo.findById(req.patientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        }

        if (prontuarioRepo.existsByPatientId(req.patientId())) {
            throw new IllegalArgumentException("Patient already has a prontuário");
        }

        var prontuario = Prontuario.builder()
                .patientId(req.patientId())
                .professionalId(userId)
                .status(Prontuario.ProntuarioStatus.ACTIVE)
                .allergies(req.allergies())
                .chronicConditions(req.chronicConditions())
                .medications(req.medications())
                .notes(req.notes())
                .build();

        return ProntuarioResponse.from(prontuarioRepo.save(prontuario));
    }

    @Transactional(readOnly = true)
    public ProntuarioResponse findById(UUID id, UUID userId, boolean isAdmin, boolean isPatient, boolean isReceptionist) {
        var prontuario = prontuarioRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prontuário not found"));

        checkAccess(prontuario.getPatientId(), userId, isAdmin, isPatient, isReceptionist);
        return ProntuarioResponse.from(prontuario);
    }

    @Transactional(readOnly = true)
    public ProntuarioResponse findByPatient(UUID patientId, UUID userId, boolean isAdmin, boolean isPatient, boolean isReceptionist) {
        checkAccess(patientId, userId, isAdmin, isPatient, isReceptionist);

        return prontuarioRepo.findByPatientId(patientId)
                .map(ProntuarioResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Prontuário not found for this patient"));
    }

    @Transactional
    public ProntuarioResponse update(UUID id, ProntuarioRequest req, UUID userId, boolean isAdmin) {
        var prontuario = prontuarioRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prontuário not found"));

        if (!isAdmin && !prontuario.getProfessionalId().equals(userId)) {
            throw new AccessDeniedException("Only the responsible professional can edit this prontuário");
        }

        if (req.allergies() != null) prontuario.setAllergies(req.allergies());
        if (req.chronicConditions() != null) prontuario.setChronicConditions(req.chronicConditions());
        if (req.medications() != null) prontuario.setMedications(req.medications());
        if (req.notes() != null) prontuario.setNotes(req.notes());

        return ProntuarioResponse.from(prontuarioRepo.save(prontuario));
    }

    @Transactional
    public ProntuarioResponse archive(UUID id, boolean isAdmin) {
        if (!isAdmin) throw new AccessDeniedException("Only admins can archive prontuários");

        var prontuario = prontuarioRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prontuário not found"));

        prontuario.setStatus(Prontuario.ProntuarioStatus.ARCHIVED);
        return ProntuarioResponse.from(prontuarioRepo.save(prontuario));
    }

    @Transactional
    public EvolucaoClinicaResponse addEvolucao(UUID prontuarioId, EvolucaoClinicaRequest req, UUID userId, boolean isAdmin) {
        var prontuario = prontuarioRepo.findById(prontuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Prontuário not found"));

        if (!isAdmin && !prontuario.getProfessionalId().equals(userId)) {
            throw new AccessDeniedException("Only the responsible professional can add evolutions");
        }

        if (evolucaoRepo.existsByAppointmentId(req.appointmentId())) {
            throw new IllegalArgumentException("Evolution already registered for this appointment");
        }

        var evolucao = EvolucaoClinica.builder()
                .prontuarioId(prontuarioId)
                .appointmentId(req.appointmentId())
                .professionalId(userId)
                .subjective(req.subjective())
                .objective(req.objective())
                .assessment(req.assessment())
                .plan(req.plan())
                .techniques(req.techniques())
                .build();

        return EvolucaoClinicaResponse.from(evolucaoRepo.save(evolucao));
    }

    @Transactional(readOnly = true)
    public List<EvolucaoClinicaResponse> listEvolucoes(UUID prontuarioId, UUID userId, boolean isAdmin, boolean isPatient, boolean isReceptionist) {
        var prontuario = prontuarioRepo.findById(prontuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Prontuário not found"));

        checkAccess(prontuario.getPatientId(), userId, isAdmin, isPatient, isReceptionist);

        return evolucaoRepo.findByProntuarioIdOrderByCreatedAtDesc(prontuarioId)
                .stream().map(EvolucaoClinicaResponse::from).toList();
    }

    @Transactional
    public AnamneseResponse createAnamnese(AnamneseRequest req, UUID userId, boolean isAdmin) {
        var prontuario = prontuarioRepo.findById(req.prontuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Prontuário not found"));

        if (!isAdmin && !prontuario.getProfessionalId().equals(userId)) {
            throw new AccessDeniedException("Only the responsible professional can create anamnesis");
        }

        if (anamneseRepo.findByProntuarioId(req.prontuarioId()).isPresent()) {
            throw new IllegalArgumentException("Anamnesis already exists for this prontuário");
        }

        var anamnese = Anamnese.builder()
                .prontuarioId(req.prontuarioId())
                .professionalId(userId)
                .chiefComplaint(req.chiefComplaint())
                .historyIllness(req.historyIllness())
                .personalHistory(req.personalHistory())
                .familyHistory(req.familyHistory())
                .socialHistory(req.socialHistory())
                .medications(req.medications())
                .build();

        return AnamneseResponse.from(anamneseRepo.save(anamnese));
    }

    @Transactional(readOnly = true)
    public AnamneseResponse findAnamnese(UUID prontuarioId, UUID userId, boolean isAdmin, boolean isPatient, boolean isReceptionist) {
        var prontuario = prontuarioRepo.findById(prontuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Prontuário not found"));

        checkAccess(prontuario.getPatientId(), userId, isAdmin, isPatient, isReceptionist);

        return anamneseRepo.findByProntuarioId(prontuarioId)
                .map(AnamneseResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Anamnesis not found for this prontuário"));
    }

    @Transactional
    public AnamneseResponse updateAnamnese(UUID prontuarioId, AnamneseRequest req, UUID userId, boolean isAdmin) {
        var prontuario = prontuarioRepo.findById(prontuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Prontuário not found"));

        if (!isAdmin && !prontuario.getProfessionalId().equals(userId)) {
            throw new AccessDeniedException("Only the responsible professional can edit anamnesis");
        }

        var anamnese = anamneseRepo.findByProntuarioId(prontuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Anamnesis not found"));

        if (req.chiefComplaint() != null) anamnese.setChiefComplaint(req.chiefComplaint());
        if (req.historyIllness() != null) anamnese.setHistoryIllness(req.historyIllness());
        if (req.personalHistory() != null) anamnese.setPersonalHistory(req.personalHistory());
        if (req.familyHistory() != null) anamnese.setFamilyHistory(req.familyHistory());
        if (req.socialHistory() != null) anamnese.setSocialHistory(req.socialHistory());
        if (req.medications() != null) anamnese.setMedications(req.medications());

        return AnamneseResponse.from(anamneseRepo.save(anamnese));
    }

    /**
     * Psychological records are confidential (CFP Resolution 01/2009, LGPD art. 11): only admins and
     * clinicians may read them. Receptionists manage the schedule, not clinical content.
     */
    private void checkAccess(UUID patientId, UUID userId, boolean isAdmin, boolean isPatient, boolean isReceptionist) {
        if (isAdmin) return;
        if (isReceptionist && !isPatient)
            throw new AccessDeniedException("Receptionists cannot access clinical records");
        if (isPatient && !userId.equals(patientId))
            throw new AccessDeniedException("Patients can only access their own records");
    }
}
