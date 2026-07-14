package com.loyaltap.membership.model;

import com.loyaltap.business.model.Business;
import com.loyaltap.common.auditing.AuditableEntity;
import com.loyaltap.user.model.User;
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

import java.util.UUID;

@Entity
@Table(name = "memberships")
@Getter
@Setter
public class Membership extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(name = "points_balance", nullable = false)
    private int pointsBalance;

    @Column(name = "reserved_points", nullable = false)
    private int reservedPoints;

    @Column(name = "total_points_earned", nullable = false)
    private int totalPointsEarned;

    @Column(name = "total_points_spent", nullable = false)
    private int totalPointsSpent;

    @Column(name = "total_rewards_redeemed", nullable = false)
    private int totalRewardsRedeemed;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MembershipStatus status = MembershipStatus.ACTIVE;
}
