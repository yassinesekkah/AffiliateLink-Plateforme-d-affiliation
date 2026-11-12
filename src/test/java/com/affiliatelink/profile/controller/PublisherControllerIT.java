package com.affiliatelink.profile.controller;

import com.affiliatelink.common.exception.EmailAlreadyExistsException;
import com.affiliatelink.common.exception.ResourceNotFoundException;
import com.affiliatelink.profile.Niche;
import com.affiliatelink.profile.dto.CreatePublisherRequest;
import com.affiliatelink.profile.dto.CreatePublisherResponse;
import com.affiliatelink.profile.entity.AccountStatus;
import com.affiliatelink.profile.service.PublisherService;
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

@WebMvcTest(PublisherController.class)
class PublisherControllerIT {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PublisherService publisherService;

    @Test
    void createPublisherShouldReturn201() throws Exception {
        // Given
        String json = """
                {
                    "email": "publisher@test.com",
                    "firstName": "First",
                    "lastName": "Last",
                    "niche": "GAMING",
                    "bio": "Gaming creator",
                    "country": "Morocco"
                }
                """;
        CreatePublisherRequest request = new CreatePublisherRequest(
                "publisher@test.com", "First", "Last", Niche.GAMING,
                "Gaming creator", "Morocco"
        );
        CreatePublisherResponse response = new CreatePublisherResponse(
                1L, "publisher@test.com", "First", "Last", Niche.GAMING,
                "Gaming creator", "Morocco", AccountStatus.PENDING,
                LocalDateTime.of(2026, 11, 11, 10, 0)
        );
        when(publisherService.createPublisher(request)).thenReturn(response);

        // When / Then
        mockMvc.perform(post("/api/publishers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("publisher@test.com"))
                .andExpect(jsonPath("$.firstName").value("First"))
                .andExpect(jsonPath("$.lastName").value("Last"))
                .andExpect(jsonPath("$.niche").value("GAMING"))
                .andExpect(jsonPath("$.bio").value("Gaming creator"))
                .andExpect(jsonPath("$.country").value("Morocco"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(publisherService).createPublisher(request);
    }

    @Test
    void createPublisherShouldReturn400WhenEmailIsInvalid() throws Exception {
        // Given
        String json = """
                {
                    "email": "email-invalide",
                    "firstName": "First",
                    "lastName": "Last",
                    "niche": "GAMING"
                }
                """;

        // When / Then
        mockMvc.perform(post("/api/publishers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.instance").value("/api/publishers"))
                .andExpect(jsonPath("$.errors.email").isNotEmpty());

        verifyNoInteractions(publisherService);
    }

    @Test
    void createPublisherShouldReturn400WhenNicheIsMissing() throws Exception {
        // Given
        String json = """
                {
                    "email": "publisher@test.com",
                    "firstName": "First",
                    "lastName": "Last"
                }
                """;

        // When / Then
        mockMvc.perform(post("/api/publishers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.instance").value("/api/publishers"))
                .andExpect(jsonPath("$.errors.niche").isNotEmpty());

        verifyNoInteractions(publisherService);
    }

    @Test
    void createPublisherShouldReturn409WhenEmailExists() throws Exception {
        // Given
        String json = """
                {
                    "email": "publisher@test.com",
                    "firstName": "First",
                    "lastName": "Last",
                    "niche": "GAMING"
                }
                """;
        CreatePublisherRequest request = new CreatePublisherRequest(
                "publisher@test.com", "First", "Last", Niche.GAMING, null, null
        );
        when(publisherService.createPublisher(request))
                .thenThrow(new EmailAlreadyExistsException("Email deja utilise"));

        // When / Then
        mockMvc.perform(post("/api/publishers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Email deja utilise"))
                .andExpect(jsonPath("$.instance").value("/api/publishers"));

        verify(publisherService).createPublisher(request);
    }

    @Test
    void findByIdShouldReturn404WhenPublisherDoesNotExist() throws Exception {
        // Given
        when(publisherService.findById(99L))
                .thenThrow(new ResourceNotFoundException("publisher not found"));

        // When / Then
        mockMvc.perform(get("/api/publishers/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("publisher not found"))
                .andExpect(jsonPath("$.instance").value("/api/publishers/99"));

        verify(publisherService).findById(99L);
    }

    @Test
    void findByIdShouldReturnPublisher() throws Exception {
        // Given
        CreatePublisherResponse response = new CreatePublisherResponse(
                1L, "publisher@test.com", "First", "Last", Niche.GAMING,
                "Gaming creator", "Morocco", AccountStatus.PENDING,
                LocalDateTime.of(2026, 11, 11, 10, 0)
        );
        when(publisherService.findById(1L)).thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/publishers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("publisher@test.com"))
                .andExpect(jsonPath("$.firstName").value("First"))
                .andExpect(jsonPath("$.niche").value("GAMING"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(publisherService).findById(1L);
    }

    @Test
    void findAllShouldReturnPublishers() throws Exception {
        // Given
        CreatePublisherResponse response = new CreatePublisherResponse(
                1L, "publisher@test.com", "First", "Last", Niche.GAMING,
                "Gaming creator", "Morocco", AccountStatus.PENDING,
                LocalDateTime.of(2026, 11, 11, 10, 0)
        );
        when(publisherService.findAll()).thenReturn(List.of(response));

        // When / Then
        mockMvc.perform(get("/api/publishers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].email").value("publisher@test.com"));

        verify(publisherService).findAll();
    }

    @Test
    void findAllShouldReturnEmptyListWhenNoPublishersExist() throws Exception {
        // Given
        when(publisherService.findAll()).thenReturn(List.of());

        // When / Then
        mockMvc.perform(get("/api/publishers"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(publisherService).findAll();
    }
}
