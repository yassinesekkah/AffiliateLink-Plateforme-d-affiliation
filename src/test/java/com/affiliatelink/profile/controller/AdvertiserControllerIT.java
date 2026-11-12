package com.affiliatelink.profile.controller;

import com.affiliatelink.common.exception.EmailAlreadyExistsException;
import com.affiliatelink.common.exception.ResourceNotFoundException;
import com.affiliatelink.profile.dto.AdvertierResponse;
import com.affiliatelink.profile.dto.AdvertiserRequest;
import com.affiliatelink.profile.entity.AccountStatus;
import com.affiliatelink.profile.service.AdvertiserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdvertiserController.class)
class AdvertiserControllerIT {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AdvertiserService advertiserService;

    @Test
    void createAdvertiserShouldReturn201() throws Exception {
        // Given
        String json = """
                {
                    "email": "test@gmail.com",
                    "companyName": "Test Company",
                    "sector": "Tech",
                    "website": "https://example.com"
                }
                """;

        AdvertiserRequest request = new AdvertiserRequest(
                "test@gmail.com", "Test Company", "Tech", "https://example.com"
        );

        AdvertierResponse response = new AdvertierResponse(
                1L, "test@gmail.com", "Test Company", "Tech",
                "https://example.com", AccountStatus.PENDING,
                LocalDateTime.of(2026, 11, 10, 10, 0)
        );

        when(advertiserService.createAdvertiser(request)).thenReturn(response);

        // When / Then
        mockMvc.perform(post("/api/advertisers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("test@gmail.com"))
                .andExpect(jsonPath("$.companyName").value("Test Company"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(advertiserService).createAdvertiser(request);
    }

    @Test
    void createAdvertiserShouldReturn400WhenEmailIsInvalid() throws Exception {
        // Given
        String json = """
                {
                    "email": "email-invalide",
                    "companyName": "Test Company",
                    "sector": "Tech",
                    "website": "https://example.com"
                }
                """;

        // When / Then
        mockMvc.perform(post("/api/advertisers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.instance").value("/api/advertisers"))
                .andExpect(jsonPath("$.errors.email").isNotEmpty());

        verifyNoInteractions(advertiserService);
    }

    @Test
    void createAdvertiserShouldReturn409WhenEmailExists() throws Exception {
        // Given
        String json = """
                {
                    "email": "test@gmail.com",
                    "companyName": "Test Company",
                    "sector": "Tech",
                    "website": "https://example.com"
                }
                """;
        AdvertiserRequest request = new AdvertiserRequest(
                "test@gmail.com", "Test Company", "Tech", "https://example.com"
        );
        when(advertiserService.createAdvertiser(request))
                .thenThrow(new EmailAlreadyExistsException("Email deja utilise"));

        // When / Then
        mockMvc.perform(post("/api/advertisers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Email deja utilise"))
                .andExpect(jsonPath("$.instance").value("/api/advertisers"));

        verify(advertiserService).createAdvertiser(request);
    }

    @Test
    void findByIdShouldReturn404WhenAdvertiserDoesNotExist() throws Exception {
        // Given
        when(advertiserService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Advertiser not found"));

        // When / Then
        mockMvc.perform(get("/api/advertisers/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Advertiser not found"))
                .andExpect(jsonPath("$.instance").value("/api/advertisers/99"));

        verify(advertiserService).findById(99L);
    }

    @Test
    void findByIdShouldReturnAdvertiser() throws Exception {
        // Given
        AdvertierResponse response = new AdvertierResponse(
                1L, "test@gmail.com", "Test Company", "Tech",
                "https://example.com", AccountStatus.PENDING,
                LocalDateTime.of(2026, 11, 10, 10, 0)
        );
        when(advertiserService.findById(1L)).thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/advertisers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("test@gmail.com"))
                .andExpect(jsonPath("$.companyName").value("Test Company"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(advertiserService).findById(1L);
    }

    @Test
    void findAllShouldReturnAdvertisers() throws Exception {
        // Given
        AdvertierResponse response = new AdvertierResponse(
                1L, "test@gmail.com", "Test Company", "Tech",
                "https://example.com", AccountStatus.PENDING,
                LocalDateTime.of(2026, 11, 10, 10, 0)
        );
        when(advertiserService.findAll()).thenReturn(List.of(response));

        // When / Then
        mockMvc.perform(get("/api/advertisers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].email").value("test@gmail.com"));

        verify(advertiserService).findAll();
    }

    @Test
    void findAllShouldReturnEmptyListWhenNoAdvertisersExist() throws Exception {
        // Given
        when(advertiserService.findAll()).thenReturn(List.of());

        // When / Then
        mockMvc.perform(get("/api/advertisers"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(advertiserService).findAll();
    }
}
