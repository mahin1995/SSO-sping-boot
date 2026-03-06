package com.example.sso.servicea.service.impl;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.sso.servicea.service.AppUserAccountService;
import com.example.sso.servicea.user.entity.AppRoleEntity;
import com.example.sso.servicea.user.entity.AppScopeEntity;
import com.example.sso.servicea.user.entity.AppUserEntity;
import com.example.sso.servicea.user.repository.AppRoleRepository;
import com.example.sso.servicea.user.repository.AppUserRepository;

@Service
public class AppUserAccountServiceImpl implements AppUserAccountService {

	private static final String DEFAULT_ROLE = "APP_USER";

	private final AppUserRepository appUserRepository;
	private final AppRoleRepository appRoleRepository;

	public AppUserAccountServiceImpl(
			AppUserRepository appUserRepository,
			AppRoleRepository appRoleRepository
	) {
		this.appUserRepository = appUserRepository;
		this.appRoleRepository = appRoleRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByUsername(String username) {
		return appUserRepository.existsByUsername(normalizeRequired(username, "username"));
	}

	@Override
	@Transactional
	public AppUserEntity createUser(String username, String encodedPassword, Set<String> roleNames) {
		String normalizedUsername = normalizeRequired(username, "username");
		String normalizedEncodedPassword = normalizeRequired(encodedPassword, "encodedPassword");

		Set<String> normalizedRoleNames = normalizeRoleNames(roleNames);
		Set<AppRoleEntity> roles = resolveRoles(normalizedRoleNames);

		AppUserEntity appUser = new AppUserEntity(normalizedUsername, normalizedEncodedPassword, true, roles);
		return appUserRepository.save(appUser);
	}

	@Override
	@Transactional
	public void createUserIfMissing(String username, String encodedPassword, Set<String> roleNames) {
		if (existsByUsername(username)) {
			return;
		}
		createUser(username, encodedPassword, roleNames);
	}

	@Override
	@Transactional(readOnly = true)
	public UserRoleScopeView getUserRoleScopeView(String username) {
		String normalizedUsername = normalizeRequired(username, "username");
		AppUserEntity user = appUserRepository.findByUsername(normalizedUsername)
				.orElseThrow(() -> new AppUserAccountService.UserNotFoundException("User not found: " + normalizedUsername));
		return buildRoleScopeView(user);
	}

	@Override
	@Transactional
	public UserRoleScopeView assignRoles(String username, Set<String> roleNames) {
		String normalizedUsername = normalizeRequired(username, "username");
		AppUserEntity user = appUserRepository.findByUsername(normalizedUsername)
				.orElseThrow(() -> new AppUserAccountService.UserNotFoundException("User not found: " + normalizedUsername));

		Set<String> normalizedRoleNames = normalizeRoleNames(roleNames);
		Set<AppRoleEntity> roles = resolveRoles(normalizedRoleNames);
		user.getRoles().clear();
		user.getRoles().addAll(roles);

		AppUserEntity updatedUser = appUserRepository.save(user);
		return buildRoleScopeView(updatedUser);
	}

	private UserRoleScopeView buildRoleScopeView(AppUserEntity user) {
		Set<String> roles = user.getRoles().stream()
				.map(AppRoleEntity::getName)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		Set<String> scopes = user.getRoles().stream()
				.flatMap(role -> role.getScopes().stream())
				.map(AppScopeEntity::getName)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		return new UserRoleScopeView(user.getUsername(), roles, scopes);
	}

	private Set<AppRoleEntity> resolveRoles(Set<String> roleNames) {
		Set<AppRoleEntity> roles = new LinkedHashSet<>(appRoleRepository.findByNameIn(roleNames));
		if (roles.size() != roleNames.size()) {
			Set<String> foundRoleNames = roles.stream()
					.map(AppRoleEntity::getName)
					.collect(Collectors.toCollection(LinkedHashSet::new));
			Set<String> missingRoles = new LinkedHashSet<>(roleNames);
			missingRoles.removeAll(foundRoleNames);
			throw new AppUserAccountService.RoleNotFoundException("Roles not found: " + String.join(", ", missingRoles));
		}
		return roles;
	}

	private Set<String> normalizeRoleNames(Set<String> roleNames) {
		if (roleNames == null || roleNames.isEmpty()) {
			return new LinkedHashSet<>(Set.of(DEFAULT_ROLE));
		}

		Set<String> normalized = roleNames.stream()
				.map(role -> normalizeRequired(role, "role"))
				.collect(Collectors.toCollection(LinkedHashSet::new));

		if (normalized.isEmpty()) {
			return new LinkedHashSet<>(Set.of(DEFAULT_ROLE));
		}
		return normalized;
	}

	private String normalizeRequired(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " is required");
		}
		return value.trim();
	}
}
