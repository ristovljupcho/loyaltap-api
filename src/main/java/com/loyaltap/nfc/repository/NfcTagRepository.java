package com.loyaltap.nfc.repository;

import com.loyaltap.nfc.model.NfcTag;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NfcTagRepository extends JpaRepository<NfcTag, UUID> {

    @EntityGraph(attributePaths = "business")
    List<NfcTag> findAllByBusinessIdOrderByCreatedAtAsc(UUID businessId);

    @EntityGraph(attributePaths = "business")
    Optional<NfcTag> findByIdAndBusinessId(UUID id, UUID businessId);

    @EntityGraph(attributePaths = "business")
    Optional<NfcTag> findByTagCode(String tagCode);
}
