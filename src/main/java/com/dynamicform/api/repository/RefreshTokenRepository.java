package com.dynamicform.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dynamicform.api.entity.RefreshTokenEntity;

@Repository 
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {
    
    @Query("SELECT r FROM RefreshTokenEntity r JOIN FETCH r.user WHERE r.tokenHash = :tokenHash")
    Optional<RefreshTokenEntity> findByTokenHash(@Param("tokenHash") String tokenHash);
}
