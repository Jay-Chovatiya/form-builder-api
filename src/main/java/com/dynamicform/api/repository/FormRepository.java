package com.dynamicform.api.repository;

import com.dynamicform.api.entity.FormEntity;
import com.dynamicform.api.entity.FormStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormRepository extends JpaRepository<FormEntity, Long> {

    List<FormEntity> findAllByOrderByIdDesc();

    Optional<FormEntity> findByCode(String code);

    Optional<FormEntity> findByCodeAndStatus(String code, FormStatus status);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
