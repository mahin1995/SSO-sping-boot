package com.example.sso.servicea.service.impl;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.sso.servicea.service.RoleScopeService;
import com.example.sso.servicea.user.entity.AppRoleEntity;
import com.example.sso.servicea.user.entity.AppScopeEntity;
import com.example.sso.servicea.user.repository.AppRoleRepository;
import com.example.sso.servicea.user.repository.AppScopeRepository;

@Service
public class RoleScopeServiceImpl implements RoleScopeService {

	private final AppRoleRepository appRoleRepository;
	private final AppScopeRepository appScopeRepository;

	public RoleScopeServiceImpl(
			AppRoleRepository appRoleRepository,
			AppScopeRepository appScopeRepository
	) {
		this.appRoleRepository = appRoleRepository;
		this.appScopeRepository = appScopeRepository;
	}

	@Override
	@Transactional
	public RoleScopeView createOrUpdateRole(String roleName, String description, Set<String> scopes) {
		String normalizedRoleName = normalizeRequired(roleName, "roleName");
		Set<String> normalizedScopes = normalizeScopes(scopes);

		AppRoleEntity role = appRoleRepository.findByName(normalizedRoleName)
				.orElseGet(() -> new AppRoleEntity(normalizedRoleName, null, Set.of()));

		Set<AppScopeEntity> scopeEntities = resolveScopes(normalizedScopes);
		role.setDescription(description == null ? null : description.trim());
		role.setScopes(scopeEntities);
		AppRoleEntity savedRole = appRoleRepository.save(role);

		return toView(savedRole);
	}

	@Override
	@Transactional(readOnly = true)
	public RoleScopeView getRole(String roleName) {
		String normalizedRoleName = normalizeRequired(roleName, "roleName");
		AppRoleEntity role = appRoleRepository.findByName(normalizedRoleName)
				.orElseThrow(() -> new RoleScopeService.RoleNotFoundException("Role not found: " + normalizedRoleName));
		return toView(role);
	}

	private RoleScopeView toView(AppRoleEntity role) {
		Set<String> scopeNames = role.getScopes().stream()
				.map(AppScopeEntity::getName)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		return new RoleScopeView(role.getName(), role.getDescription(), scopeNames);
	}

	private Set<AppScopeEntity> resolveScopes(Set<String> scopeNames) {
		List<AppScopeEntity> existingScopes = appScopeRepository.findByNameIn(scopeNames);
		Set<String> existingScopeNames = existingScopes.stream()
				.map(AppScopeEntity::getName)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		Set<String> missingScopeNames = new LinkedHashSet<>(scopeNames);
		missingScopeNames.removeAll(existingScopeNames);

		Set<AppScopeEntity> scopes = new LinkedHashSet<>(existingScopes);
		for (String missingScopeName : missingScopeNames) {
			scopes.add(appScopeRepository.save(new AppScopeEntity(missingScopeName, null)));
		}
		return scopes;
	}

	private Set<String> normalizeScopes(Set<String> scopes) {
		if (scopes == null || scopes.isEmpty()) {
			throw new RoleScopeService.InvalidRoleScopeException("At least one scope is required");
		}

		Set<String> normalized = scopes.stream()
				.map(scope -> normalizeRequired(scope, "scope"))
				.collect(Collectors.toCollection(LinkedHashSet::new));
		if (normalized.isEmpty()) {
			throw new RoleScopeService.InvalidRoleScopeException("At least one valid scope is required");
		}
		return normalized;
	}

	private String normalizeRequired(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new RoleScopeService.InvalidRoleScopeException(fieldName + " is required");
		}
		return value.trim();
	}
}
