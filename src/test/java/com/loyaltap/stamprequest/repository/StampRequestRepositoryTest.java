package com.loyaltap.stamprequest.repository;

import com.loyaltap.business.model.Business;
import com.loyaltap.business.repository.BusinessRepository;
import com.loyaltap.employee.model.Employee;
import com.loyaltap.employee.model.EmployeeRole;
import com.loyaltap.employee.repository.EmployeeRepository;
import com.loyaltap.membership.model.Membership;
import com.loyaltap.membership.repository.MembershipRepository;
import com.loyaltap.nfc.model.NfcTag;
import com.loyaltap.nfc.repository.NfcTagRepository;
import com.loyaltap.stamprequest.model.StampRequest;
import com.loyaltap.stamprequest.model.StampRequestStatus;
import com.loyaltap.user.model.User;
import com.loyaltap.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:loyaltap-stamp-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.test.database.replace=none",
        "spring.liquibase.url=jdbc:h2:mem:loyaltap-stamp-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.liquibase.user=sa",
        "spring.liquibase.password=",
        "spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.xml",
        "spring.jpa.hibernate.ddl-auto=none"
})
class StampRequestRepositoryTest {

    @Autowired StampRequestRepository stampRequestRepository;
    @Autowired MembershipRepository membershipRepository;
    @Autowired NfcTagRepository nfcTagRepository;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired BusinessRepository businessRepository;
    @Autowired UserRepository userRepository;

    @Test
    void persistsRelationshipsEnumAndTimes() {
        Fixture fixture = fixture("stamp-1");
        StampRequest request = request(fixture, "key-1");
        request.setApprovedByEmployee(fixture.employee());

        StampRequest saved = stampRequestRepository.saveAndFlush(request);
        StampRequest found = stampRequestRepository.findById(saved.getId()).orElseThrow();

        assertEquals(fixture.membership().getId(), found.getMembership().getId());
        assertEquals(fixture.business().getId(), found.getBusiness().getId());
        assertEquals(fixture.tag().getId(), found.getNfcTag().getId());
        assertEquals(fixture.employee().getId(), found.getApprovedByEmployee().getId());
        assertEquals(StampRequestStatus.PENDING, found.getStatus());
    }

    @Test
    void rejectsDuplicateIdempotencyKey() {
        Fixture fixture = fixture("stamp-2");
        stampRequestRepository.saveAndFlush(request(fixture, "same-key"));
        assertThrows(DataIntegrityViolationException.class,
                () -> stampRequestRepository.saveAndFlush(request(fixture, "same-key")));
    }

    private Fixture fixture(String suffix) {
        User user = new User();
        user.setId("user-" + suffix);
        user = userRepository.saveAndFlush(user);
        Business business = new Business();
        business.setName("Loyal Coffee");
        business.setSlug(suffix);
        business = businessRepository.saveAndFlush(business);
        Membership membership = new Membership();
        membership.setUser(user);
        membership.setBusiness(business);
        membership = membershipRepository.saveAndFlush(membership);
        NfcTag tag = new NfcTag();
        tag.setBusiness(business);
        tag.setTagCode("tag-" + suffix);
        tag = nfcTagRepository.saveAndFlush(tag);
        Employee employee = new Employee();
        employee.setBusiness(business);
        employee.setUser(user);
        employee.setRole(EmployeeRole.EMPLOYEE);
        employee = employeeRepository.saveAndFlush(employee);
        return new Fixture(business, membership, tag, employee);
    }

    private StampRequest request(Fixture fixture, String key) {
        Instant now = Instant.parse("2026-01-01T12:00:00Z");
        StampRequest request = new StampRequest();
        request.setMembership(fixture.membership());
        request.setBusiness(fixture.business());
        request.setNfcTag(fixture.tag());
        request.setPointsToAdd(1);
        request.setStatus(StampRequestStatus.PENDING);
        request.setIdempotencyKey(key);
        request.setCreatedAt(now);
        request.setExpiresAt(now.plusSeconds(300));
        return request;
    }

    private record Fixture(Business business, Membership membership, NfcTag tag, Employee employee) {
    }
}
