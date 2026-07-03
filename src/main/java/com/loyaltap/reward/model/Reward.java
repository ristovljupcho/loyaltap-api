package com.loyaltap.reward.model;

import com.loyaltap.business.model.Business;
import com.loyaltap.common.auditing.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "rewards")
@Getter
@Setter
public class Reward extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rewards_business"))
    private Business business;

    // TODO: Replace this temporary String ID with a User relationship when the User entity is implemented.
    @Column(name = "created_by_user_id", nullable = false, length = 150)
    private String userId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "required_points", nullable = false)
    private int requiredPoints;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "stock_reserved", nullable = false)
    private int stockReserved;

    @Column(name = "stock_redeemed", nullable = false)
    private int stockRedeemed;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private RewardStatus status = RewardStatus.ACTIVE;
}
