package com.affiliatelink.offer.controller;

import com.affiliatelink.common.exception.BusinessConflictException;
import com.affiliatelink.common.exception.ResourceNotFoundException;
import com.affiliatelink.offer.dto.CampaignRequest;
import com.affiliatelink.offer.dto.CampaignResponse;
import com.affiliatelink.offer.entity.CampaignStatus;
import com.affiliatelink.offer.entity.PayoutModel;
import com.affiliatelink.offer.service.CampaignService;
import com.affiliatelink.offer.service.OfferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OfferController.class)
class CampaignCreationControllerIT {
    @Autowired MockMvc mockMvc;
    @MockitoBean CampaignService campaignService;
    @MockitoBean OfferService offerService;

    private static final String JSON = """
            {"name":"Campaign","startDate":"2030-01-01","endDate":"2030-02-01",
             "budget":1000,"payoutModel":"CPC","payoutValue":2}
            """;

    @Test
    void createCampaignShouldReturn201WithCreatedCampaign() throws Exception {
        // Given
        CampaignRequest request = request();
        when(campaignService.create(1L, request)).thenReturn(new CampaignResponse(2L, 1L, request.name(),
                request.startDate(), request.endDate(), request.budget(), request.payoutModel(),
                request.payoutValue(), CampaignStatus.DRAFT));
        // When / Then
        mockMvc.perform(post("/api/offres/1/campaigns").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.offerId").value(1))
                .andExpect(jsonPath("$.name").value("Campaign"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.payoutModel").value("CPC"))
                .andExpect(jsonPath("$.payoutValue").value(2));
        verify(campaignService).create(1L, request);
        verifyNoInteractions(offerService);
    }

    @Test
    void createCampaignShouldReturn400WhenBudgetIsNotPositive() throws Exception {
        // Given
        String invalidJson = JSON.replace("\"budget\":1000", "\"budget\":0");
        // When / Then
        mockMvc.perform(post("/api/offres/1/campaigns").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.instance").value("/api/offres/1/campaigns"))
                .andExpect(jsonPath("$.errors.budget").isNotEmpty());
        verifyNoInteractions(campaignService, offerService);
    }

    @Test
    void createCampaignShouldReturn404WhenOfferDoesNotExist() throws Exception {
        // Given
        when(campaignService.create(99L, request())).thenThrow(new ResourceNotFoundException("Offer not found: 99"));
        // When / Then
        mockMvc.perform(post("/api/offres/99/campaigns").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Offer not found: 99"))
                .andExpect(jsonPath("$.instance").value("/api/offres/99/campaigns"));
        verify(campaignService).create(99L, request());
    }

    @Test
    void createCampaignShouldReturn409WhenAdvertiserIsInactive() throws Exception {
        // Given
        when(campaignService.create(1L, request())).thenThrow(new BusinessConflictException("Advertiser is not active"));
        // When / Then
        mockMvc.perform(post("/api/offres/1/campaigns").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Advertiser is not active"))
                .andExpect(jsonPath("$.instance").value("/api/offres/1/campaigns"));
        verify(campaignService).create(1L, request());
    }

    private CampaignRequest request() {
        return new CampaignRequest("Campaign", LocalDate.of(2030, 1, 1), LocalDate.of(2030, 2, 1),
                new BigDecimal("1000"), PayoutModel.CPC, new BigDecimal("2"));
    }
}
