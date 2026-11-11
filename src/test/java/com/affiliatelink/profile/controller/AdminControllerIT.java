package com.affiliatelink.profile.controller;

import com.affiliatelink.common.exception.InvalidStatusTransitionException;
import com.affiliatelink.common.exception.ResourceNotFoundException;
import com.affiliatelink.profile.dto.UpdateProfileStatusRequest;
import com.affiliatelink.profile.entity.AccountStatus;
import com.affiliatelink.profile.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
class AdminControllerIT {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProfileService profileService;

    @Test
    void updateStatusShouldReturn204() throws Exception {
        // Given
        String json = """
                {"status": "ACTIVE"}
                """;
        UpdateProfileStatusRequest request =
                new UpdateProfileStatusRequest(AccountStatus.ACTIVE);

        // When / Then
        mockMvc.perform(patch("/api/admin/profiles/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(profileService).updateStatus(1L, request);
    }

    @Test
    void updateStatusShouldReturn400WhenStatusIsMissing() throws Exception {
        // Given
        String json = "{}";

        // When / Then
        mockMvc.perform(patch("/api/admin/profiles/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.instance").value("/api/admin/profiles/1/status"))
                .andExpect(jsonPath("$.errors.status").isNotEmpty());

        verifyNoInteractions(profileService);
    }

    @Test
    void updateStatusShouldReturn404WhenProfileDoesNotExist() throws Exception {
        // Given
        String json = """
                {"status": "ACTIVE"}
                """;
        UpdateProfileStatusRequest request =
                new UpdateProfileStatusRequest(AccountStatus.ACTIVE);
        doThrow(new ResourceNotFoundException("Profile not found"))
                .when(profileService).updateStatus(99L, request);

        // When / Then
        mockMvc.perform(patch("/api/admin/profiles/99/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Profile not found"))
                .andExpect(jsonPath("$.instance").value("/api/admin/profiles/99/status"));

        verify(profileService).updateStatus(99L, request);
    }

    @Test
    void updateStatusShouldReturn409WhenTransitionIsInvalid() throws Exception {
        // Given
        String json = """
                {"status": "SUSPENDED"}
                """;
        UpdateProfileStatusRequest request =
                new UpdateProfileStatusRequest(AccountStatus.SUSPENDED);
        doThrow(new InvalidStatusTransitionException("Transition de statut non autorisee"))
                .when(profileService).updateStatus(1L, request);

        // When / Then
        mockMvc.perform(patch("/api/admin/profiles/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Transition de statut non autorisee"))
                .andExpect(jsonPath("$.instance").value("/api/admin/profiles/1/status"));

        verify(profileService).updateStatus(1L, request);
    }
}
