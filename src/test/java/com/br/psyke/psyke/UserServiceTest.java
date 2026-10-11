package com.br.psyke.psyke;

import com.br.psyke.psyke.dto.CreateUserRequest;
import com.br.psyke.psyke.dto.UpdateRolesRequest;
import com.br.psyke.psyke.dto.UpdateUserRequest;
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
    private User existingUser;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();
        service = new UserService(users, roles, encoder);
        tenantId = UUID.randomUUID();
        rolePatient = Role.builder().id(UUID.randomUUID()).name("ROLE_PATIENT").build();
        roleAdmin = Role.builder().id(UUID.randomUUID()).name("ROLE_ADMIN").build();
        existingUser = User.builder().id(UUID.randomUUID()).tenantId(tenantId)
                .fullName("Carlos").email("carlos@test.com")
                .cpf("12345678900").phone("+5511988887777")
                .active(true).roles(Set.of(rolePatient)).build();
    }

    @Test
    void shouldCreateUserWithDefaultRole() {
        var req = new CreateUserRequest("Maria", "maria@test.com", "password123", null, null, null);
        var saved = User.builder().id(UUID.randomUUID()).tenantId(tenantId)
                .fullName("Maria").email("maria@test.com").active(true).emailVerified(false)
                .roles(Set.of(rolePatient)).build();

        when(users.existsByEmailIgnoreCaseAndTenantId(req.email(), tenantId)).thenReturn(false);
        when(roles.findByName("ROLE_PATIENT")).thenReturn(Optional.of(rolePatient));
        when(users.save(any())).thenReturn(saved);

        var resp = service.create(req, tenantId);
        assertThat(resp.fullName()).isEqualTo("Maria");
        assertThat(resp.roles()).containsExactly("ROLE_PATIENT");
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        var req = new CreateUserRequest("Joao", "joao@test.com", "password123", null, null, List.of("ROLE_PATIENT"));

        when(users.existsByEmailIgnoreCaseAndTenantId(req.email(), tenantId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(req, tenantId))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("já cadastrado");
    }

    @Test
    void shouldThrowWhenRoleNotFound() {
        var req = new CreateUserRequest("Ana", "ana@test.com", "password123", null, null, List.of("ROLE_FAKE"));

        when(users.existsByEmailIgnoreCaseAndTenantId(req.email(), tenantId)).thenReturn(false);
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

        when(users.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(roles.findByName("ROLE_ADMIN")).thenReturn(Optional.of(roleAdmin));
        when(users.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.updateRoles(userId, List.of("ROLE_ADMIN"), tenantId, UUID.randomUUID());
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

    @Test
    void shouldFindUserById() {
        when(users.findByIdAndTenantId(existingUser.getId(), tenantId))
                .thenReturn(Optional.of(existingUser));

        var resp = service.findById(existingUser.getId(), tenantId);
        assertThat(resp.fullName()).isEqualTo("Carlos");
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        var id = UUID.randomUUID();
        when(users.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id, tenantId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldUpdateUser() {
        var id = existingUser.getId();
        var req = new UpdateUserRequest("Carlos Updated", null, null, null);

        when(users.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.of(existingUser));
        when(users.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.update(id, req, tenantId, UUID.randomUUID());
        assertThat(resp.fullName()).isEqualTo("Carlos Updated");
    }

    @Test
    void shouldDeactivateUser() {
        var id = existingUser.getId();
        when(users.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.of(existingUser));

        service.delete(id, tenantId, UUID.randomUUID());
        assertThat(existingUser.isActive()).isFalse();
        verify(users).save(existingUser);
    }

    @Test
    void shouldThrowOnUpdateWhenUserNotFound() {
        var id = UUID.randomUUID();
        when(users.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, new UpdateUserRequest(null, null, null, null), tenantId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowOnDeleteWhenUserNotFound() {
        var id = UUID.randomUUID();
        when(users.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id, tenantId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldUpdateMultipleFields() {
        var id = existingUser.getId();
        var req = new UpdateUserRequest("Novo Nome", "99988877766", "+5511977776666", false);

        when(users.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.of(existingUser));
        when(users.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.update(id, req, tenantId, UUID.randomUUID());
        assertThat(resp.fullName()).isEqualTo("Novo Nome");
        assertThat(resp.phone()).isEqualTo("+5511977776666");
    }

    @Test
    void shouldNotFindUserFromAnotherTenant() {
        var id = existingUser.getId();
        var otherTenant = UUID.randomUUID();
        when(users.findByIdAndTenantId(id, otherTenant)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id, otherTenant))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldNotAllowAdminToRemoveOwnAdminRole() {
        var id = existingUser.getId();
        when(users.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> service.updateRoles(id, List.of("ROLE_PATIENT"), tenantId, id))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldNotAllowUserToDeactivateThemselves() {
        var id = existingUser.getId();
        assertThatThrownBy(() -> service.delete(id, tenantId, id))
                .isInstanceOf(IllegalArgumentException.class);
        verify(users, never()).save(any());
    }
}
