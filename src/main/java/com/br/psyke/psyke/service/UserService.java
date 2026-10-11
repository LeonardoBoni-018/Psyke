package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.CreateUserRequest;
import com.br.psyke.psyke.dto.UpdateUserRequest;
import com.br.psyke.psyke.dto.UserResponse;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.exception.UserAlreadyExistsException;
import com.br.psyke.psyke.model.Role;
import com.br.psyke.psyke.model.User;
import com.br.psyke.psyke.repository.RoleRepository;
import com.br.psyke.psyke.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Users live in the shared master schema, so every lookup is scoped by tenant to prevent
 * cross-tenant access by id (OWASP API1).
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String ADMIN = "ROLE_ADMIN";

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    public Page<UserResponse> findAll(UUID tenantId, Pageable pageable) {
        return users.findByTenantId(tenantId, pageable).map(this::toResponse);
    }

    public UserResponse findById(UUID id, UUID tenantId) {
        return toResponse(load(id, tenantId));
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest req, UUID tenantId, UUID actorId) {
        var user = load(id, tenantId);
        if (req.fullName() != null) user.setFullName(req.fullName());
        if (req.cpf() != null) user.setCpf(req.cpf());
        if (req.phone() != null) user.setPhone(req.phone());
        if (req.active() != null) {
            if (!req.active() && id.equals(actorId))
                throw new IllegalArgumentException("Você não pode desativar o próprio usuário");
            user.setActive(req.active());
        }
        return toResponse(users.save(user));
    }

    @Transactional
    public void delete(UUID id, UUID tenantId, UUID actorId) {
        if (id.equals(actorId))
            throw new IllegalArgumentException("Você não pode desativar o próprio usuário");
        var user = load(id, tenantId);
        user.setActive(false);
        users.save(user);
    }

    @Transactional
    public UserResponse create(CreateUserRequest req, UUID tenantId) {
        var email = req.email().trim();
        if (users.existsByEmailIgnoreCaseAndTenantId(email, tenantId))
            throw new UserAlreadyExistsException("E-mail já cadastrado nesta clínica");

        var roleSet = req.roles() != null && !req.roles().isEmpty()
            ? resolveRoles(req.roles())
            : Set.of(roles.findByName("ROLE_PATIENT")
                    .orElseThrow(() -> new ResourceNotFoundException("Default role ROLE_PATIENT not found")));

        var user = User.builder()
                .tenantId(tenantId)
                .fullName(req.fullName())
                .email(email)
                .passwordHash(encoder.encode(req.password()))
                .cpf(req.cpf())
                .phone(req.phone())
                .active(true)
                .emailVerified(false)
                .roles(roleSet)
                .build();
        return toResponse(users.save(user));
    }

    @Transactional
    public UserResponse updateRoles(UUID id, List<String> roleNames, UUID tenantId, UUID actorId) {
        var user = load(id, tenantId);
        if (id.equals(actorId) && !roleNames.contains(ADMIN))
            throw new IllegalArgumentException("Você não pode remover o próprio acesso de administrador");
        user.setRoles(resolveRoles(roleNames));
        return toResponse(users.save(user));
    }

    private User load(UUID id, UUID tenantId) {
        return users.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Set<Role> resolveRoles(List<String> names) {
        return names.stream().distinct().map(name -> roles.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Role '" + name + "' not found")))
                .collect(Collectors.toSet());
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId().toString(), u.getFullName(), u.getEmail(),
                u.getCpf(), u.getPhone(), u.isActive(), u.isEmailVerified(),
                u.getRoles().stream().map(Role::getName).sorted().toList());
    }
}
