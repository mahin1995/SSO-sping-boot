package com.example.sso.servicea.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.sso.servicea.user.entity.AppUserEntity;

public interface AppUserRepository extends JpaRepository<AppUserEntity, Long> {

	@EntityGraph(attributePaths = {"roles", "roles.scopes"})
	Optional<AppUserEntity> findByUsername(String username);

	boolean existsByUsername(String username);
}
