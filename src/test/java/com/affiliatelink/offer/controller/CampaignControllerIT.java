package com.affiliatelink.offer.controller;

import com.affiliatelink.common.exception.BusinessConflictException;
import com.affiliatelink.common.exception.ResourceNotFoundException;
import com.affiliatelink.offer.dto.CampaignResponse;
import com.affiliatelink.offer.entity.CampaignStatus;
import com.affiliatelink.offer.entity.PayoutModel;
import com.affiliatelink.offer.service.CampaignService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CampaignController.class)
class CampaignControllerIT {
    @Autowired MockMvc mockMvc;
    @MockitoBean CampaignService campaignService;

    @Test
    void updateStatusShouldReturn204() throws Exception {
        // Given
        String json = """
                {"status":"ACTIVE"}
                """;
        // When / Then
        mockMvc.perform(patch("/api/campaigns/2/status").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(campaignService).updateStatus(2L, CampaignStatus.ACTIVE);
    }

    @Test
    void updateStatusShouldReturn400WhenStatusIsMissing() throws Exception {
        // Given
        String json = "{}";
        // When / Then
        mockMvc.perform(patch("/api/campaigns/2/status").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.instance").value("/api/campaigns/2/status"))
                .andExpect(jsonPath("$.errors.status").isNotEmpty());
        verifyNoInteractions(campaignService);
    }

    @Test
    void updateStatusShouldReturn404WhenCampaignDoesNotExist() throws Exception {
        // Given
        doThrow(new ResourceNotFoundException("Campaign not found: 99"))
                .when(campaignService).updateStatus(99L, CampaignStatus.ACTIVE);
        // When / Then
        mockMvc.perform(patch("/api/campaigns/99/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Campaign not found: 99"))
                .andExpect(jsonPath("$.instance").value("/api/campaigns/99/status"));
        verify(campaignService).updateStatus(99L, CampaignStatus.ACTIVE);
    }

    @Test
    void updateStatusShouldReturn409WhenTransitionIsForbidden() throws Exception {
        // Given
        String detail = "Invalid campaign status transition: DRAFT -> ENDED";
        doThrow(new BusinessConflictException(detail)).when(campaignService).updateStatus(2L, CampaignStatus.ENDED);
        // When / Then
        mockMvc.perform(patch("/api/campaigns/2/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ENDED\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.instance").value("/api/campaigns/2/status"));
        verify(campaignService).updateStatus(2L, CampaignStatus.ENDED);
    }

    @Test
    void getCampaignsShouldReturnAllCampaignsWithoutFilter() throws Exception {
        // Given
        when(campaignService.getAll(null)).thenReturn(List.of(response()));
        // When / Then
        mockMvc.perform(get("/api/campaigns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].offerId").value(1))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
        verify(campaignService).getAll(null);
    }

    @Test
    void getCampaignsShouldPassStatusFilterToService() throws Exception {
        // Given
        when(campaignService.getAll(CampaignStatus.ACTIVE)).thenReturn(List.of(response()));
        // When / Then
        mockMvc.perform(get("/api/campaigns").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
        verify(campaignService).getAll(CampaignStatus.ACTIVE);
    }

    @Test
    void getCampaignsShouldReturnEmptyArrayWhenNoCampaignsExist() throws Exception {
        // Given
        when(campaignService.getAll(null)).thenReturn(List.of());
        // When / Then
        mockMvc.perform(get("/api/campaigns"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(campaignService).getAll(null);
    }

    private CampaignResponse response() {
        return new CampaignResponse(2L, 1L, "Campaign", LocalDate.of(2030, 1, 1),
                LocalDate.of(2030, 2, 1), new BigDecimal("1000"), PayoutModel.CPC,
                new BigDecimal("2"), CampaignStatus.ACTIVE);
    }
}
