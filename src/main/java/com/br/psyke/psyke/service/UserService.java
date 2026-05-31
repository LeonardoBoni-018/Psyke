package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.CreateUserRequest;
import com.br.psyke.psyke.dto.UpdateRolesRequest;
import com.br.psyke.psyke.dto.UpdateUserRequest;
import com.br.psyke.psyke.dto.UserResponse;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.exception.UserAlreadyExistsException;
import com.br.psyke.psyke.model.Role;
import com.br.psyke.psyke.model.User;
import com.br.psyke.psyke.repository.RoleRepository;
import com.br.psyke.psyke.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    public Page<UserResponse> findAll(UUID tenantId, Pageable pageable) {
        return users.findByTenantId(tenantId, pageable).map(this::toResponse);
    }

    public UserResponse findById(UUID id) {
        return users.findByIdWithRoles(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest req) {
        var user = users.findByIdWithRoles(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (req.fullName() != null) user.setFullName(req.fullName());
        if (req.cpf() != null) user.setCpf(req.cpf());
        if (req.phone() != null) user.setPhone(req.phone());
        if (req.active() != null) user.setActive(req.active());

        user = users.save(user);
        return toResponse(user);
    }

    @Transactional
    public void delete(UUID id) {
        var user = users.findByIdWithRoles(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(false);
        users.save(user);
    }

    @Transactional
    public UserResponse create(CreateUserRequest req, UUID tenantId) {
        if (users.existsByEmailAndTenantId(req.email(), tenantId))
            throw new UserAlreadyExistsException("Email '" + req.email() + "' already in use in this tenant");

        var roleSet = req.roles() != null && !req.roles().isEmpty()
            ? req.roles().stream().map(name -> roles.findByName(name)
                    .orElseThrow(() -> new ResourceNotFoundException("Role '" + name + "' not found")))
                    .collect(Collectors.toSet())
            : java.util.Set.of(roles.findByName("ROLE_PATIENT")
                    .orElseThrow(() -> new ResourceNotFoundException("Default role ROLE_PATIENT not found")));

        var user = User.builder()
                .tenantId(tenantId)
                .fullName(req.fullName())
                .email(req.email())
                .passwordHash(encoder.encode(req.password()))
                .cpf(req.cpf())
                .phone(req.phone())
                .active(true)
                .emailVerified(false)
                .roles(roleSet)
                .build();
        user = users.save(user);
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateRoles(UUID id, List<String> roleNames) {
        var user = users.findByIdWithRoles(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var roleSet = roleNames.stream().map(name -> roles.findByName(name)
                    .orElseThrow(() -> new ResourceNotFoundException("Role '" + name + "' not found")))
                    .collect(Collectors.toSet());
        user.setRoles(roleSet);
        user = users.save(user);
        return toResponse(user);
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId().toString(), u.getFullName(), u.getEmail(),
                u.getCpf(), u.getPhone(), u.isActive(), u.isEmailVerified(),
                u.getRoles().stream().map(Role::getName).toList());
    }
}
