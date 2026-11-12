package com.affiliatelink.offer.controller;

import com.affiliatelink.common.exception.BusinessConflictException;
import com.affiliatelink.common.exception.ResourceNotFoundException;
import com.affiliatelink.offer.dto.OfferRequest;
import com.affiliatelink.offer.dto.OfferResponse;
import com.affiliatelink.offer.service.CampaignService;
import com.affiliatelink.offer.service.OfferService;
import com.affiliatelink.profile.Niche;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OfferController.class)
class OfferControllerIT {
    @Autowired MockMvc mockMvc;
    @MockitoBean OfferService offerService;
    @MockitoBean CampaignService campaignService;

    private static final String JSON = """
            {"advertiserId":10,"title":"Gaming offer","description":"Description",
             "landingUrl":"https://example.com","niche":"GAMING","tags":["gaming"]}
            """;

    @Test
    void createOfferShouldReturn201WithCreatedOffer() throws Exception {
        // Given
        when(offerService.create(request())).thenReturn(response());
        // When / Then
        mockMvc.perform(post("/api/offres").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.advertiserId").value(10))
                .andExpect(jsonPath("$.title").value("Gaming offer"))
                .andExpect(jsonPath("$.landingUrl").value("https://example.com"))
                .andExpect(jsonPath("$.niche").value("GAMING"))
                .andExpect(jsonPath("$.tags[0]").value("gaming"));
        verify(offerService).create(request());
        verifyNoInteractions(campaignService);
    }

    @ParameterizedTest
    @CsvSource({"advertiserId,0", "title,''", "landingUrl,invalid-url", "description,''"})
    void createOfferShouldReturn400ForInvalidFields(String field, String value) throws Exception {
        // Given
        String json = switch (field) {
            case "advertiserId" -> JSON.replace("\"advertiserId\":10", "\"advertiserId\":" + value);
            case "title" -> JSON.replace("Gaming offer", value);
            case "landingUrl" -> JSON.replace("https://example.com", value);
            default -> JSON.replace("Description", value);
        };
        // When / Then
        expectProblem(mockMvc.perform(post("/api/offres").contentType(MediaType.APPLICATION_JSON).content(json)),
                400, "Bad Request", "Request validation failed", "/api/offres")
                .andExpect(jsonPath("$.errors." + field).isNotEmpty());
        verifyNoInteractions(offerService, campaignService);
    }

    @Test
    void createOfferShouldReturn404WhenAdvertiserDoesNotExist() throws Exception {
        // Given
        when(offerService.create(request())).thenThrow(new ResourceNotFoundException("Advertiser not found"));
        // When / Then
        expectProblem(mockMvc.perform(post("/api/offres").contentType(MediaType.APPLICATION_JSON).content(JSON)),
                404, "Not Found", "Advertiser not found", "/api/offres");
        verify(offerService).create(request());
    }

    @Test
    void createOfferShouldReturn409WhenAdvertiserIsInactive() throws Exception {
        // Given
        when(offerService.create(request())).thenThrow(new BusinessConflictException("Advertiser is not active"));
        // When / Then
        expectProblem(mockMvc.perform(post("/api/offres").contentType(MediaType.APPLICATION_JSON).content(JSON)),
                409, "Conflict", "Advertiser is not active", "/api/offres");
        verify(offerService).create(request());
    }

    @Test
    void getOfferByIdShouldReturnOffer() throws Exception {
        // Given
        when(offerService.getById(1L)).thenReturn(response());
        // When / Then
        mockMvc.perform(get("/api/offres/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Gaming offer"));
        verify(offerService).getById(1L);
    }

    @Test
    void getOfferByIdShouldReturn404WhenOfferDoesNotExist() throws Exception {
        // Given
        when(offerService.getById(99L)).thenThrow(new ResourceNotFoundException("Offer not found 99"));
        // When / Then
        expectProblem(mockMvc.perform(get("/api/offres/99")),
                404, "Not Found", "Offer not found 99", "/api/offres/99");
        verify(offerService).getById(99L);
    }

    @Test
    void updateOfferShouldReturnUpdatedOffer() throws Exception {
        // Given
        when(offerService.updateOffer(1L, request())).thenReturn(response());
        // When / Then
        mockMvc.perform(put("/api/offres/1").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Gaming offer"))
                .andExpect(jsonPath("$.tags[0]").value("gaming"));
        verify(offerService).updateOffer(1L, request());
    }

    @Test
    void updateOfferShouldReturn400WhenTitleIsBlank() throws Exception {
        // Given
        String json = JSON.replace("Gaming offer", "");
        // When / Then
        expectProblem(mockMvc.perform(put("/api/offres/1").contentType(MediaType.APPLICATION_JSON).content(json)),
                400, "Bad Request", "Request validation failed", "/api/offres/1")
                .andExpect(jsonPath("$.errors.title").isNotEmpty());
        verifyNoInteractions(offerService, campaignService);
    }

    @Test
    void updateOfferShouldReturn404WhenOfferDoesNotExist() throws Exception {
        // Given
        when(offerService.updateOffer(99L, request())).thenThrow(new ResourceNotFoundException("Offer not found 99"));
        // When / Then
        expectProblem(mockMvc.perform(put("/api/offres/99").contentType(MediaType.APPLICATION_JSON).content(JSON)),
                404, "Not Found", "Offer not found 99", "/api/offres/99");
        verify(offerService).updateOffer(99L, request());
    }

    @Test
    void getOffersShouldPassFiltersPaginationAndSortToService() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "title"));
        when(offerService.getAll(pageable, Niche.GAMING, 10L, "gaming", "offer"))
                .thenReturn(new PageImpl<>(List.of(response()), pageable, 3));
        // When / Then
        mockMvc.perform(get("/api/offres").param("niche", "GAMING").param("advertiserId", "10")
                        .param("tag", "gaming").param("keyword", "offer")
                        .param("page", "1").param("size", "2").param("sort", "title,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        verify(offerService).getAll(pageable, Niche.GAMING, 10L, "gaming", "offer");
    }

    @Test
    void getOffersShouldUseDefaultPaginationWithoutFilters() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        when(offerService.getAll(pageable, null, null, null, null))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));
        // When / Then
        mockMvc.perform(get("/api/offres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
        verify(offerService).getAll(pageable, null, null, null, null);
    }

    private ResultActions expectProblem(ResultActions result, int code, String title, String detail, String instance)
            throws Exception {
        return result.andExpect(status().is(code))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.status").value(code))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.instance").value(instance));
    }

    private OfferRequest request() {
        return new OfferRequest(10L, "Gaming offer", "Description", "https://example.com", Niche.GAMING, Set.of("gaming"));
    }

    private OfferResponse response() {
        return new OfferResponse(1L, 10L, "Gaming offer", "Description", "https://example.com", Niche.GAMING,
                Set.of("gaming"), LocalDateTime.of(2030, 1, 1, 12, 0));
    }
}
