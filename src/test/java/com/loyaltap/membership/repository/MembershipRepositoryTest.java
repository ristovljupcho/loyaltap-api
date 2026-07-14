package com.loyaltap.membership.repository;

import com.loyaltap.business.model.Business;
import com.loyaltap.business.repository.BusinessRepository;
import com.loyaltap.membership.model.Membership;
import com.loyaltap.membership.model.MembershipStatus;
import com.loyaltap.user.model.User;
import com.loyaltap.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:loyaltap-membership-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.test.database.replace=none",
        "spring.liquibase.url=jdbc:h2:mem:loyaltap-membership-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.liquibase.user=sa",
        "spring.liquibase.password=",
        "spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.xml",
        "spring.jpa.hibernate.ddl-auto=none"
})
class MembershipRepositoryTest {

    @Autowired
    private MembershipRepository membershipRepository;
    @Autowired
    private BusinessRepository businessRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistsRelationshipsDefaultsEnumsAuditingAndSupportsLockedLookup() {
        User user = persistUser("user-1");
        Business business = persistBusiness("membership-test");

        Membership saved = membershipRepository.saveAndFlush(membership(user, business));
        Membership found = membershipRepository.findByIdForUpdate(saved.getId()).orElseThrow();

        assertEquals(user.getId(), found.getUser().getId());
        assertEquals(business.getId(), found.getBusiness().getId());
        assertEquals(MembershipStatus.ACTIVE, found.getStatus());
        assertEquals(0, found.getPointsBalance());
        assertEquals(0, found.getReservedPoints());
        assertNotNull(found.getCreatedAt());
        assertNotNull(found.getUpdatedAt());
    }

    @Test
    void listsOnlyActiveMembershipsForUser() {
        User user = persistUser("user-2");
        Membership active = membership(user, persistBusiness("active-membership"));
        Membership blocked = membership(user, persistBusiness("blocked-membership"));
        blocked.setStatus(MembershipStatus.BLOCKED);
        membershipRepository.saveAllAndFlush(List.of(active, blocked));

        List<Membership> found = membershipRepository.findAllByUserIdAndStatusOrderByCreatedAtAsc(
                user.getId(), MembershipStatus.ACTIVE
        );

        assertEquals(1, found.size());
        assertEquals(active.getId(), found.getFirst().getId());
    }

    @Test
    void rejectsDuplicateUserBusinessMembership() {
        User user = persistUser("user-3");
        Business business = persistBusiness("duplicate-membership");
        membershipRepository.saveAndFlush(membership(user, business));

        assertThrows(DataIntegrityViolationException.class,
                () -> membershipRepository.saveAndFlush(membership(user, business)));
    }

    @Test
    void rejectsReservedPointsAboveBalance() {
        Membership membership = membership(
                persistUser("user-4"),
                persistBusiness("invalid-balance")
        );
        membership.setPointsBalance(1);
        membership.setReservedPoints(2);

        assertThrows(DataIntegrityViolationException.class,
                () -> membershipRepository.saveAndFlush(membership));
    }

    @Test
    void rejectsNegativeCounters() {
        Membership membership = membership(
                persistUser("user-5"),
                persistBusiness("invalid-counter")
        );
        membership.setTotalPointsEarned(-1);

        assertThrows(DataIntegrityViolationException.class,
                () -> membershipRepository.saveAndFlush(membership));
    }

    @Test
    void createsUserStatusLookupIndex() {
        Long indexCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.INDEXES
                WHERE LOWER(TABLE_NAME) = 'memberships'
                  AND LOWER(INDEX_NAME) = 'idx_memberships_user_status'
                """, Long.class);

        assertNotNull(indexCount);
        assertTrue(indexCount > 0);
    }

    private User persistUser(String id) {
        User user = new User();
        user.setId(id);
        return userRepository.saveAndFlush(user);
    }

    private Business persistBusiness(String slug) {
        Business business = new Business();
        business.setName("Loyal Coffee");
        business.setSlug(slug);
        return businessRepository.saveAndFlush(business);
    }

    private Membership membership(User user, Business business) {
        Membership membership = new Membership();
        membership.setUser(user);
        membership.setBusiness(business);
        return membership;
    }
}
