package com.loyaltap.membership.repository;

import com.loyaltap.membership.model.Membership;
import com.loyaltap.membership.model.MembershipStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    boolean existsByUserIdAndBusinessId(String userId, UUID businessId);

    @EntityGraph(attributePaths = {"user", "business"})
    List<Membership> findAllByUserIdAndStatusOrderByCreatedAtAsc(String userId, MembershipStatus status);

    @Override
    @EntityGraph(attributePaths = {"user", "business"})
    Optional<Membership> findById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select membership from Membership membership where membership.id = :id")
    Optional<Membership> findByIdForUpdate(@Param("id") UUID id);
}
