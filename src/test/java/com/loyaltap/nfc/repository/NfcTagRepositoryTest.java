package com.loyaltap.nfc.repository;

import com.loyaltap.business.model.Business;
import com.loyaltap.business.repository.BusinessRepository;
import com.loyaltap.nfc.model.NfcTag;
import com.loyaltap.nfc.model.NfcTagStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:loyaltap-nfc-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.test.database.replace=none",
        "spring.liquibase.url=jdbc:h2:mem:loyaltap-nfc-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.liquibase.user=sa",
        "spring.liquibase.password=",
        "spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.xml",
        "spring.jpa.hibernate.ddl-auto=none"
})
class NfcTagRepositoryTest {

    @Autowired
    private NfcTagRepository nfcTagRepository;
    @Autowired
    private BusinessRepository businessRepository;

    @Test
    void persistsRelationshipStatusUsageAndAuditTimestamps() {
        Business business = persistBusiness("nfc-test");
        NfcTag tag = tag(business, "tag-1");
        tag.setLastUsedAt(Instant.parse("2026-01-01T00:00:00Z"));

        NfcTag saved = nfcTagRepository.saveAndFlush(tag);
        NfcTag found = nfcTagRepository.findByTagCode(saved.getTagCode()).orElseThrow();

        assertEquals(business.getId(), found.getBusiness().getId());
        assertEquals(NfcTagStatus.ACTIVE, found.getStatus());
        assertEquals(tag.getLastUsedAt(), found.getLastUsedAt());
        assertNotNull(found.getCreatedAt());
        assertNotNull(found.getUpdatedAt());
    }

    @Test
    void rejectsDuplicateTagCode() {
        Business business = persistBusiness("nfc-duplicate");
        nfcTagRepository.saveAndFlush(tag(business, "same-code"));

        assertThrows(DataIntegrityViolationException.class,
                () -> nfcTagRepository.saveAndFlush(tag(business, "same-code")));
    }

    private Business persistBusiness(String slug) {
        Business business = new Business();
        business.setName("Loyal Coffee");
        business.setSlug(slug);
        return businessRepository.saveAndFlush(business);
    }

    private NfcTag tag(Business business, String code) {
        NfcTag tag = new NfcTag();
        tag.setBusiness(business);
        tag.setTagCode(code);
        tag.setLocationName("Front desk");
        tag.setStatus(NfcTagStatus.ACTIVE);
        return tag;
    }
}
