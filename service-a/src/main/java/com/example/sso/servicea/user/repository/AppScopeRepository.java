package com.example.sso.servicea.user.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.sso.servicea.user.entity.AppScopeEntity;

public interface AppScopeRepository extends JpaRepository<AppScopeEntity, Long> {

	Optional<AppScopeEntity> findByName(String name);

	List<AppScopeEntity> findByNameIn(Collection<String> names);

	boolean existsByName(String name);
}
