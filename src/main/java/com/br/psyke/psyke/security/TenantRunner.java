package com.br.psyke.psyke.security;

import com.br.psyke.psyke.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.function.Consumer;

/** Runs a background task once per active tenant, with that tenant bound (for {@code @Scheduled} jobs). */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantRunner {

    private final TenantRepository tenants;

    public void forEachActiveTenant(String taskName, Consumer<String> task) {
        var active = tenants.findAll().stream().filter(t -> t.isActive()).toList();
        for (var tenant : active) {
            var tenantId = tenant.getId().toString();
            try {
                TenantScope.run(tenantId, null, () -> task.accept(tenantId));
            } catch (RuntimeException e) {
                log.error("Task '{}' failed for tenant {}: {}", taskName, tenant.getSlug(), e.getMessage(), e);
            }
        }
    }
}
