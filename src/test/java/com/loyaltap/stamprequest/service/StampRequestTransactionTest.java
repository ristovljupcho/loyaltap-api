package com.loyaltap.stamprequest.service;

import com.loyaltap.business.model.Business;
import com.loyaltap.business.repository.BusinessRepository;
import com.loyaltap.employee.model.Employee;
import com.loyaltap.employee.model.EmployeeRole;
import com.loyaltap.employee.repository.EmployeeRepository;
import com.loyaltap.membership.model.Membership;
import com.loyaltap.membership.repository.MembershipRepository;
import com.loyaltap.nfc.model.NfcTag;
import com.loyaltap.nfc.repository.NfcTagRepository;
import com.loyaltap.stamprequest.dto.CreateStampRequest;
import com.loyaltap.stamprequest.dto.EmployeeStampRequestAction;
import com.loyaltap.stamprequest.model.StampRequestStatus;
import com.loyaltap.user.model.User;
import com.loyaltap.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class StampRequestTransactionTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.liquibase.url", POSTGRES::getJdbcUrl);
        registry.add("spring.liquibase.user", POSTGRES::getUsername);
        registry.add("spring.liquibase.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired StampRequestService stampRequestService;
    @Autowired UserRepository userRepository;
    @Autowired BusinessRepository businessRepository;
    @Autowired MembershipRepository membershipRepository;
    @Autowired NfcTagRepository nfcTagRepository;
    @Autowired EmployeeRepository employeeRepository;

    @Test
    void approvalCommitsRequestAndMembershipTogether() {
        User user = new User();
        user.setId("stamp-transaction-user");
        user = userRepository.save(user);
        Business business = new Business();
        business.setName("Transaction Coffee");
        business.setSlug("stamp-transaction");
        business = businessRepository.save(business);
        Membership membership = new Membership();
        membership.setUser(user);
        membership.setBusiness(business);
        membership = membershipRepository.save(membership);
        NfcTag tag = new NfcTag();
        tag.setBusiness(business);
        tag.setTagCode("stamp-transaction-tag");
        tag = nfcTagRepository.save(tag);
        Employee employee = new Employee();
        employee.setBusiness(business);
        employee.setUser(user);
        employee.setRole(EmployeeRole.EMPLOYEE);
        employee = employeeRepository.save(employee);

        var created = stampRequestService.createRequest(
                tag.getTagCode(),
                new CreateStampRequest(membership.getId(), user.getId(), UUID.randomUUID().toString())
        );
        var approved = stampRequestService.approveRequest(
                created.id(), new EmployeeStampRequestAction(employee.getId()));

        Membership reloaded = membershipRepository.findById(membership.getId()).orElseThrow();
        assertEquals(StampRequestStatus.APPROVED, approved.status());
        assertEquals(1, reloaded.getPointsBalance());
        assertEquals(1, reloaded.getTotalPointsEarned());
    }
}
