package com.example.sso.servicea.user.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.sso.servicea.user.entity.AppRoleEntity;

public interface AppRoleRepository extends JpaRepository<AppRoleEntity, Long> {

	@EntityGraph(attributePaths = {"scopes"})
	Optional<AppRoleEntity> findByName(String name);

	@EntityGraph(attributePaths = {"scopes"})
	List<AppRoleEntity> findByNameIn(Collection<String> names);

	boolean existsByName(String name);
}
