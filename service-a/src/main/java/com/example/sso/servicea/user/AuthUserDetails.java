package com.example.sso.servicea.user;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.sso.servicea.user.entity.AppRoleEntity;
import com.example.sso.servicea.user.entity.AppScopeEntity;
import com.example.sso.servicea.user.entity.AppUserEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthUserDetails implements UserDetails {

	private final Long userId;
	private final String username;
	private final String passwordHash;
	private final boolean enabled;
	private final Set<String> roleNames;
	private final Set<String> scopes;

	public AuthUserDetails(AppUserEntity appUser) {
		this(
				appUser.getId(),
				appUser.getUsername(),
				appUser.getPasswordHash(),
				appUser.isEnabled(),
				appUser.getRoles().stream()
						.map(AppRoleEntity::getName)
						.collect(Collectors.toCollection(LinkedHashSet::new)),
				appUser.getRoles().stream()
						.flatMap(role -> role.getScopes().stream())
						.map(AppScopeEntity::getName)
						.collect(Collectors.toCollection(LinkedHashSet::new))
		);
	}

	@JsonCreator
	public AuthUserDetails(
			@JsonProperty("userId") Long userId,
			@JsonProperty("username") String username,
			@JsonProperty("password") String passwordHash,
			@JsonProperty("enabled") boolean enabled,
			@JsonProperty("roleNames") Set<String> roleNames,
			@JsonProperty("scopes") Set<String> scopes
	) {
		this.userId = userId;
		this.username = username;
		this.passwordHash = passwordHash;
		this.enabled = enabled;
		this.roleNames = roleNames == null ? new LinkedHashSet<>() : new LinkedHashSet<>(roleNames);
		this.scopes = scopes == null ? new LinkedHashSet<>() : new LinkedHashSet<>(scopes);
	}

	public Long getUserId() {
		return userId;
	}

	public Set<String> getScopes() {
		return new LinkedHashSet<>(scopes);
	}

	public Set<String> getRoleNames() {
		return new LinkedHashSet<>(roleNames);
	}

	@Override
	@JsonIgnore
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Stream.concat(
						roleNames.stream().map(roleName -> new SimpleGrantedAuthority("ROLE_" + roleName)),
						scopes.stream().map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
				)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
}
