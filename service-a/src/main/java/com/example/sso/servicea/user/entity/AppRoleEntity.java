package com.example.sso.servicea.user.entity;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_roles")
public class AppRoleEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false, unique = true, length = 100)
	private String name;

	@Column(name = "description", length = 255)
	private String description;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
			name = "app_role_scopes",
			joinColumns = @JoinColumn(name = "role_id"),
			inverseJoinColumns = @JoinColumn(name = "scope_id")
	)
	private Set<AppScopeEntity> scopes = new LinkedHashSet<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected AppRoleEntity() {
	}

	public AppRoleEntity(String name, String description, Set<AppScopeEntity> scopes) {
		this.name = name;
		this.description = description;
		this.scopes = new LinkedHashSet<>(scopes);
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Set<AppScopeEntity> getScopes() {
		return scopes;
	}

	public void setScopes(Set<AppScopeEntity> scopes) {
		this.scopes = new LinkedHashSet<>(scopes);
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
