package com.loyaltap.business.service;

import com.loyaltap.business.dto.BusinessRequest;
import com.loyaltap.business.dto.BusinessResponse;
import com.loyaltap.business.mapper.BusinessMapper;
import com.loyaltap.business.model.Business;
import com.loyaltap.business.model.BusinessStatus;
import com.loyaltap.business.repository.BusinessRepository;
import com.loyaltap.common.error.DuplicateResourceException;
import com.loyaltap.common.error.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessServiceImplTest {

    @Mock
    private BusinessRepository businessRepository;

    private BusinessServiceImpl businessService;

    @BeforeEach
    void setUp() {
        businessService = new BusinessServiceImpl(businessRepository, new BusinessMapper());
    }

    @Test
    void updateBusinessReplacesFieldsAndNormalizesSlug() {
        UUID businessId = UUID.randomUUID();
        Business business = new Business();
        business.setId(businessId);
        business.setName("Old name");
        business.setSlug("old-slug");
        business.setDescription("Keep this");
        business.setStatus(BusinessStatus.ACTIVE);
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));

        BusinessRequest request = validBusinessRequest("Loyal Coffee!");

        BusinessResponse response = businessService.updateBusiness(businessId, request);

        assertEquals(request.name(), response.name());
        assertEquals("loyal-coffee", response.slug());
        assertEquals(request.description(), response.description());
        assertEquals(request.email(), response.email());
    }

    @Test
    void updateBusinessRejectsMissingBusiness() {
        UUID businessId = UUID.randomUUID();
        when(businessRepository.findById(businessId)).thenReturn(Optional.empty());

        BusinessRequest request = validBusinessRequest("loyal-coffee");

        assertThrows(ResourceNotFoundException.class,
                () -> businessService.updateBusiness(businessId, request));
    }

    @Test
    void updateBusinessRejectsDuplicateNormalizedSlug() {
        UUID businessId = UUID.randomUUID();
        Business business = new Business();
        business.setId(businessId);
        business.setSlug("old-slug");
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(businessRepository.existsBySlugAndIdNot("taken-slug", businessId)).thenReturn(true);

        BusinessRequest request = validBusinessRequest("Taken Slug");

        assertThrows(DuplicateResourceException.class,
                () -> businessService.updateBusiness(businessId, request));
    }

    @Test
    void createBusinessNormalizesSlug() {
        when(businessRepository.save(org.mockito.ArgumentMatchers.any(Business.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BusinessResponse response = businessService.createBusiness(validBusinessRequest("Loyal Coffee!"));

        assertEquals("loyal-coffee", response.slug());
    }

    private BusinessRequest validBusinessRequest(String slug) {
        return new BusinessRequest(
                "Loyal Coffee",
                slug,
                "Coffee and rewards",
                "+38970123456",
                "hello@loyal.coffee",
                "https://loyal.coffee",
                "Main Street 1",
                "Skopje"
        );
    }
}
