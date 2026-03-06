package com.example.sso.servicea.user;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.sso.servicea.user.entity.AppRoleEntity;
import com.example.sso.servicea.user.entity.AppScopeEntity;
import com.example.sso.servicea.user.entity.AppUserEntity;

public class AuthUserDetails implements UserDetails {

	private final Long userId;
	private final String username;
	private final String passwordHash;
	private final boolean enabled;
	private final Set<String> roleNames;
	private final Set<String> scopes;

	public AuthUserDetails(AppUserEntity appUser) {
		this.userId = appUser.getId();
		this.username = appUser.getUsername();
		this.passwordHash = appUser.getPasswordHash();
		this.enabled = appUser.isEnabled();
		this.roleNames = appUser.getRoles().stream()
				.map(AppRoleEntity::getName)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		this.scopes = appUser.getRoles().stream()
				.flatMap(role -> role.getScopes().stream())
				.map(AppScopeEntity::getName)
				.collect(Collectors.toCollection(LinkedHashSet::new));
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
