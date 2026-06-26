package com.loyaltap.business.service;

import com.loyaltap.business.dto.CreateBusinessRequest;
import com.loyaltap.business.dto.UpdateBusinessRequest;
import com.loyaltap.business.mapper.BusinessMapper;
import com.loyaltap.business.model.Business;
import com.loyaltap.business.model.BusinessStatus;
import com.loyaltap.business.repository.BusinessRepository;
import com.loyaltap.common.error.DuplicateResourceException;
import com.loyaltap.common.error.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessServiceTest {

    private static final UUID BUSINESS_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T10:00:00Z");

    @Mock
    private BusinessRepository businessRepository;

    private BusinessService businessService;

    @BeforeEach
    void setUp() {
        businessService = new BusinessService(businessRepository, new BusinessMapper());
    }

    @Test
    void createBusinessGeneratesSlugAndSavesActiveBusiness() {
        CreateBusinessRequest request = createRequest(" Cafe Aroma ", null);
        when(businessRepository.existsBySlug("cafe-aroma")).thenReturn(false);
        when(businessRepository.save(any(Business.class))).thenAnswer(invocation -> {
            Business business = invocation.getArgument(0);
            setPersistenceFields(business);
            return business;
        });

        var response = businessService.createBusiness(request);

        assertThat(response.id()).isEqualTo(BUSINESS_ID);
        assertThat(response.name()).isEqualTo("Cafe Aroma");
        assertThat(response.slug()).isEqualTo("cafe-aroma");
        assertThat(response.status()).isEqualTo(BusinessStatus.ACTIVE);

        ArgumentCaptor<Business> captor = ArgumentCaptor.forClass(Business.class);
        verify(businessRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Cafe Aroma");
        assertThat(captor.getValue().getSlug()).isEqualTo("cafe-aroma");
    }

    @Test
    void createBusinessRejectsDuplicateSlug() {
        CreateBusinessRequest request = createRequest("Cafe Aroma", "Cafe Aroma");
        when(businessRepository.existsBySlug("cafe-aroma")).thenReturn(true);

        assertThatThrownBy(() -> businessService.createBusiness(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("cafe-aroma");

        verify(businessRepository, never()).save(any());
    }

    @Test
    void listActiveBusinessesReturnsActiveBusinessesOrderedByName() {
        Business business = business("Cafe Aroma", "cafe-aroma", BusinessStatus.ACTIVE);
        when(businessRepository.findAllByStatusOrderByNameAsc(BusinessStatus.ACTIVE))
                .thenReturn(List.of(business));

        var responses = businessService.listActiveBusinesses();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().slug()).isEqualTo("cafe-aroma");
    }

    @Test
    void getBusinessThrowsWhenBusinessIsMissing() {
        when(businessRepository.findById(BUSINESS_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> businessService.getBusiness(BUSINESS_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(BUSINESS_ID.toString());
    }

    @Test
    void updateBusinessUpdatesEditableFields() {
        Business business = business("Cafe Aroma", "cafe-aroma", BusinessStatus.ACTIVE);
        when(businessRepository.findById(BUSINESS_ID)).thenReturn(Optional.of(business));
        when(businessRepository.existsBySlugAndIdNot("new-cafe", BUSINESS_ID)).thenReturn(false);

        var response = businessService.updateBusiness(BUSINESS_ID, new UpdateBusinessRequest(
                " New Cafe ",
                "New Cafe",
                "Updated description",
                null,
                "owner@example.com",
                null,
                "Main Street 1",
                null,
                "Skopje",
                null,
                null,
                "MK"
        ));

        assertThat(response.name()).isEqualTo("New Cafe");
        assertThat(response.slug()).isEqualTo("new-cafe");
        assertThat(response.description()).isEqualTo("Updated description");
        assertThat(response.email()).isEqualTo("owner@example.com");
        assertThat(response.city()).isEqualTo("Skopje");
    }

    @Test
    void updateBusinessRejectsDuplicateSlug() {
        Business business = business("Cafe Aroma", "cafe-aroma", BusinessStatus.ACTIVE);
        when(businessRepository.findById(BUSINESS_ID)).thenReturn(Optional.of(business));
        when(businessRepository.existsBySlugAndIdNot("new-cafe", BUSINESS_ID)).thenReturn(true);

        assertThatThrownBy(() -> businessService.updateBusiness(BUSINESS_ID, new UpdateBusinessRequest(
                null,
                "New Cafe",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ))).isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("new-cafe");
    }

    @Test
    void deactivateBusinessMarksBusinessInactive() {
        Business business = business("Cafe Aroma", "cafe-aroma", BusinessStatus.ACTIVE);
        when(businessRepository.findById(BUSINESS_ID)).thenReturn(Optional.of(business));

        var response = businessService.deactivateBusiness(BUSINESS_ID);

        assertThat(response.status()).isEqualTo(BusinessStatus.INACTIVE);
        assertThat(business.getStatus()).isEqualTo(BusinessStatus.INACTIVE);
    }

    @Test
    void deactivateBusinessIsIdempotentWhenAlreadyInactive() {
        Business business = business("Cafe Aroma", "cafe-aroma", BusinessStatus.INACTIVE);
        when(businessRepository.findById(BUSINESS_ID)).thenReturn(Optional.of(business));

        var response = businessService.deactivateBusiness(BUSINESS_ID);

        assertThat(response.status()).isEqualTo(BusinessStatus.INACTIVE);
        assertThat(business.getStatus()).isEqualTo(BusinessStatus.INACTIVE);
    }

    private CreateBusinessRequest createRequest(String name, String slug) {
        return new CreateBusinessRequest(
                name,
                slug,
                "A neighborhood cafe",
                "+38970000000",
                "owner@example.com",
                "https://example.com",
                "Main Street 1",
                null,
                "Skopje",
                null,
                "1000",
                "MK"
        );
    }

    private Business business(String name, String slug, BusinessStatus status) {
        Business business = new Business();
        business.setName(name);
        business.setSlug(slug);
        business.setStatus(status);
        business.setDescription("A neighborhood cafe");
        business.setEmail("owner@example.com");
        business.setCity("Skopje");
        setPersistenceFields(business);
        return business;
    }

    private void setPersistenceFields(Business business) {
        ReflectionTestUtils.setField(business, "id", BUSINESS_ID);
        ReflectionTestUtils.setField(business, "createdAt", CREATED_AT);
        ReflectionTestUtils.setField(business, "updatedAt", UPDATED_AT);
    }
}
