package com.loyaltap.reward.repository;

import com.loyaltap.business.model.Business;
import com.loyaltap.business.repository.BusinessRepository;
import com.loyaltap.reward.model.Reward;
import com.loyaltap.user.model.User;
import com.loyaltap.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:loyaltap-repository-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.test.database.replace=none",
        "spring.liquibase.url=jdbc:h2:mem:loyaltap-repository-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.liquibase.user=sa",
        "spring.liquibase.password=",
        "spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.xml",
        "spring.jpa.hibernate.ddl-auto=none"
})
class RewardRepositoryTest {

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void persistsUserRelationship() {
        Business business = new Business();
        business.setName("Loyal Coffee");
        business.setSlug("loyal-coffee");
        business = businessRepository.save(business);

        User user = new User();
        user.setId("user-1");
        user = userRepository.save(user);

        Reward reward = new Reward();
        reward.setBusiness(business);
        reward.setUser(user);
        reward.setName("Free coffee");
        reward.setRequiredPoints(10);

        Reward savedReward = rewardRepository.saveAndFlush(reward);

        assertEquals(user.getId(), savedReward.getUser().getId());
    }
}
