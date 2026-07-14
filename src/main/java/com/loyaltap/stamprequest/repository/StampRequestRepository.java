package com.loyaltap.stamprequest.repository;

import com.loyaltap.stamprequest.model.StampRequest;
import com.loyaltap.stamprequest.model.StampRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StampRequestRepository extends JpaRepository<StampRequest, UUID> {

    @Override
    @EntityGraph(attributePaths = {"membership.user", "business", "nfcTag", "approvedByEmployee"})
    Optional<StampRequest> findById(UUID id);

    @EntityGraph(attributePaths = {"membership.user", "business", "nfcTag", "approvedByEmployee"})
    Optional<StampRequest> findByIdempotencyKey(String idempotencyKey);

    @EntityGraph(attributePaths = {"membership.user", "business", "nfcTag", "approvedByEmployee"})
    List<StampRequest> findAllByBusinessIdAndStatusOrderByCreatedAtAsc(
            UUID businessId,
            StampRequestStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"membership.user", "business", "nfcTag", "approvedByEmployee"})
    @Query("select request from StampRequest request where request.id = :id")
    Optional<StampRequest> findByIdForUpdate(@Param("id") UUID id);

    @Query("select request.id from StampRequest request where request.status = :status and request.expiresAt <= :now")
    List<UUID> findIdsToExpire(@Param("status") StampRequestStatus status, @Param("now") Instant now);
}
