package com.loyaltap.business.repository;

import com.loyaltap.business.model.Business;
import com.loyaltap.business.model.BusinessStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessRepository extends JpaRepository<Business, UUID> {

    Optional<Business> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    List<Business> findAllByStatusOrderByNameAsc(BusinessStatus status);
}
