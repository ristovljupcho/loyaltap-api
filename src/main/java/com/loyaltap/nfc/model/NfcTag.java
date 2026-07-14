package com.loyaltap.nfc.model;

import com.loyaltap.business.model.Business;
import com.loyaltap.common.auditing.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "nfc_tags")
@Getter
@Setter
public class NfcTag extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(name = "tag_code", nullable = false, unique = true, updatable = false, length = 150)
    private String tagCode;

    @Column(name = "location_name", length = 150)
    private String locationName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private NfcTagStatus status = NfcTagStatus.ACTIVE;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;
}
