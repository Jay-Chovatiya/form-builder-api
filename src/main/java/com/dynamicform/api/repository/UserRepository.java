package com.dynamicform.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dynamicform.api.entity.UserEntity;

@Repository 
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @Query("SELECT u FROM UserEntity u WHERE u.username = :input OR u.email = :input")
    Optional<UserEntity> findByUserNameOrEmail(@Param("input") String userNameOrEmail);
    
}
