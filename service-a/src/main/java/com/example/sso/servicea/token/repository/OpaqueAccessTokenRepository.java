package com.example.sso.servicea.token.repository;

import java.util.Optional;

import com.example.sso.servicea.token.entity.OpaqueAccessTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpaqueAccessTokenRepository extends JpaRepository<OpaqueAccessTokenEntity, String> {

	Optional<OpaqueAccessTokenEntity> findByTokenHashAndActiveTrue(String tokenHash);
}
