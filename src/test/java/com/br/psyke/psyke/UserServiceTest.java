package com.br.psyke.psyke;

import com.br.psyke.psyke.dto.CreateUserRequest;
import com.br.psyke.psyke.dto.UpdateRolesRequest;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.exception.UserAlreadyExistsException;
import com.br.psyke.psyke.model.Role;
import com.br.psyke.psyke.model.User;
import com.br.psyke.psyke.repository.RoleRepository;
import com.br.psyke.psyke.repository.UserRepository;
import com.br.psyke.psyke.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository users;
    @Mock private RoleRepository roles;
    private PasswordEncoder encoder;
    private UserService service;
    private UUID tenantId;
    private Role rolePatient;
    private Role roleAdmin;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();
        service = new UserService(users, roles, encoder);
        tenantId = UUID.randomUUID();
        rolePatient = Role.builder().id(UUID.randomUUID()).name("ROLE_PATIENT").build();
        roleAdmin = Role.builder().id(UUID.randomUUID()).name("ROLE_ADMIN").build();
    }

    @Test
    void shouldCreateUserWithDefaultRole() {
        var req = new CreateUserRequest("Maria", "maria@test.com", "password123", null, null, null);
        var saved = User.builder().id(UUID.randomUUID()).tenantId(tenantId)
                .fullName("Maria").email("maria@test.com").active(true).emailVerified(false)
                .roles(Set.of(rolePatient)).build();

        when(users.existsByEmailAndTenantId(req.email(), tenantId)).thenReturn(false);
        when(roles.findByName("ROLE_PATIENT")).thenReturn(Optional.of(rolePatient));
        when(users.save(any())).thenReturn(saved);

        var resp = service.create(req, tenantId);
        assertThat(resp.fullName()).isEqualTo("Maria");
        assertThat(resp.roles()).containsExactly("ROLE_PATIENT");
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        var req = new CreateUserRequest("Joao", "joao@test.com", "password123", null, null, List.of("ROLE_PATIENT"));

        when(users.existsByEmailAndTenantId(req.email(), tenantId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(req, tenantId))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("already in use");
    }

    @Test
    void shouldThrowWhenRoleNotFound() {
        var req = new CreateUserRequest("Ana", "ana@test.com", "password123", null, null, List.of("ROLE_FAKE"));

        when(users.existsByEmailAndTenantId(req.email(), tenantId)).thenReturn(false);
        when(roles.findByName("ROLE_FAKE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req, tenantId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void shouldUpdateRoles() {
        var userId = UUID.randomUUID();
        var user = User.builder().id(userId).tenantId(tenantId).fullName("Carlos")
                .email("carlos@test.com").active(true).roles(Set.of(rolePatient)).build();

        when(users.findByIdWithRoles(userId)).thenReturn(Optional.of(user));
        when(roles.findByName("ROLE_ADMIN")).thenReturn(Optional.of(roleAdmin));
        when(users.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.updateRoles(userId, List.of("ROLE_ADMIN"));
        assertThat(resp.roles()).containsExactly("ROLE_ADMIN");
    }

    @Test
    void shouldFindAllUsersByTenant() {
        var user1 = User.builder().id(UUID.randomUUID()).tenantId(tenantId).fullName("User 1")
                .email("u1@test.com").active(true).roles(Set.of(rolePatient)).build();
        var user2 = User.builder().id(UUID.randomUUID()).tenantId(tenantId).fullName("User 2")
                .email("u2@test.com").active(true).roles(Set.of(rolePatient)).build();
        var pageable = PageRequest.of(0, 10);

        when(users.findByTenantId(tenantId, pageable)).thenReturn(new PageImpl<>(List.of(user1, user2)));

        var resp = service.findAll(tenantId, pageable);
        assertThat(resp).hasSize(2);
    }
}
