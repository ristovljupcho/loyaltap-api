package com.loyaltap.employee.repository;

import com.loyaltap.business.model.Business;
import com.loyaltap.business.repository.BusinessRepository;
import com.loyaltap.employee.model.Employee;
import com.loyaltap.employee.model.EmployeeRole;
import com.loyaltap.employee.model.EmployeeStatus;
import com.loyaltap.user.model.User;
import com.loyaltap.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:loyaltap-employee-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.test.database.replace=none",
        "spring.liquibase.url=jdbc:h2:mem:loyaltap-employee-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.liquibase.user=sa",
        "spring.liquibase.password=",
        "spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.xml",
        "spring.jpa.hibernate.ddl-auto=none"
})
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private BusinessRepository businessRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void persistsRelationshipsEnumsAndAuditTimestamps() {
        Business business = persistBusiness("employee-test");
        User user = persistUser("user-1");

        Employee saved = employeeRepository.saveAndFlush(employee(business, user));
        Employee found = employeeRepository.findById(saved.getId()).orElseThrow();

        assertEquals(business.getId(), found.getBusiness().getId());
        assertEquals(user.getId(), found.getUser().getId());
        assertEquals(EmployeeRole.MANAGER, found.getRole());
        assertEquals(EmployeeStatus.ACTIVE, found.getStatus());
        assertNotNull(found.getCreatedAt());
        assertNotNull(found.getUpdatedAt());
    }

    @Test
    void rejectsDuplicateBusinessUserAssignment() {
        Business business = persistBusiness("duplicate-test");
        User user = persistUser("user-2");
        employeeRepository.saveAndFlush(employee(business, user));

        assertThrows(DataIntegrityViolationException.class,
                () -> employeeRepository.saveAndFlush(employee(business, user)));
    }

    private Business persistBusiness(String slug) {
        Business business = new Business();
        business.setName("Loyal Coffee");
        business.setSlug(slug);
        return businessRepository.saveAndFlush(business);
    }

    private User persistUser(String id) {
        User user = new User();
        user.setId(id);
        return userRepository.saveAndFlush(user);
    }

    private Employee employee(Business business, User user) {
        Employee employee = new Employee();
        employee.setBusiness(business);
        employee.setUser(user);
        employee.setRole(EmployeeRole.MANAGER);
        return employee;
    }
}
