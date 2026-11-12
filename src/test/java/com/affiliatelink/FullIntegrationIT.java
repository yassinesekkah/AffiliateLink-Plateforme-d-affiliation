package com.affiliatelink;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureMockMvc
@Testcontainers
class FullIntegrationIT {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("affiliatelink_full_test").withUsername("test").withPassword("test");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        // All HTTP requests commit normally; reset only the disposable test database.
        jdbc.execute("TRUNCATE TABLE offer_offer_tags, offer_tags, offer_campaigns, offer_offers, "
                + "profile_publishers, profile_advertisers, profile_profiles RESTART IDENTITY CASCADE");
    }

    @Test
    void advertiserRegistrationShouldPersistPendingProfileAndAllowConsultation() throws Exception {
        // Given / When
        long id = createAdvertiser("register@example.com");
        // Then
        mockMvc.perform(get("/api/advertisers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.email").value("register@example.com"))
                .andExpect(jsonPath("$.companyName").value("Test Company"))
                .andExpect(jsonPath("$.status").value("PENDING"));
        assertThat(jdbc.queryForObject("SELECT status FROM profile_profiles WHERE id = ?", String.class, id))
                .isEqualTo("PENDING");
    }

    @Test
    void duplicateRegistrationShouldReturn409WithoutCreatingAnotherProfile() throws Exception {
        // Given
        createAdvertiser("duplicate@example.com");
        // When / Then
        mockMvc.perform(post("/api/advertisers").contentType(MediaType.APPLICATION_JSON)
                        .content(advertiserJson("duplicate@example.com")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.instance").value("/api/advertisers"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM profile_profiles", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM profile_advertisers", Long.class)).isEqualTo(1);
    }

    @Test
    void successiveCampaignActivationsShouldPausePreviouslyActiveCampaign() throws Exception {
        // Given
        long advertiserId = activeAdvertiser("campaigns@example.com");
        long offerId = createOffer(advertiserId, "Campaign offer", "GAMING");
        long first = createCampaign(offerId, "First");
        long second = createCampaign(offerId, "Second");
        assertThat(campaignStatus(first)).isEqualTo("DRAFT");
        assertThat(campaignStatus(second)).isEqualTo("DRAFT");

        // When / Then
        changeCampaignStatus(first, "ACTIVE");
        assertThat(campaignStatus(first)).isEqualTo("ACTIVE");
        assertThat(campaignStatus(second)).isEqualTo("DRAFT");
        changeCampaignStatus(second, "ACTIVE");
        assertThat(campaignStatus(first)).isEqualTo("PAUSED");
        assertThat(campaignStatus(second)).isEqualTo("ACTIVE");
        mockMvc.perform(get("/api/campaigns").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(second));
    }

    @Test
    void offerSearchShouldFilterSortAndPaginatePersistedOffers() throws Exception {
        // Given
        long advertiserId = activeAdvertiser("search@example.com");
        long alpha = createOffer(advertiserId, "Alpha Gaming", "GAMING");
        long beta = createOffer(advertiserId, "Beta Gaming", "GAMING");
        createOffer(advertiserId, "Gamma Food", "FOOD");

        // When / Then
        searchPage(advertiserId, 0)
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(alpha))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
        searchPage(advertiserId, 1)
                .andExpect(jsonPath("$.content[0].id").value(beta))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void suspendingAdvertiserShouldPauseTheirActiveCampaignsAndPreserveOtherCampaigns() throws Exception {
        // Given
        long target = activeAdvertiser("suspended@example.com");
        long other = activeAdvertiser("other@example.com");
        long targetOffer = createOffer(target, "Target offer", "GAMING");
        long secondOffer = createOffer(target, "Second target offer", "GAMING");
        long otherOffer = createOffer(other, "Other advertiser offer", "GAMING");
        long first = createCampaign(targetOffer, "First active");
        long second = createCampaign(secondOffer, "Second active");
        long draft = createCampaign(targetOffer, "Draft");
        long otherActive = createCampaign(otherOffer, "Other active");
        changeCampaignStatus(first, "ACTIVE");
        changeCampaignStatus(second, "ACTIVE");
        changeCampaignStatus(otherActive, "ACTIVE");

        // When: suspension publishes the real event after the HTTP transaction commits.
        changeProfileStatus(target, "SUSPENDED");

        // Then: wait for the asynchronous listener to persist both changes.
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(campaignStatus(first)).isEqualTo("PAUSED");
            assertThat(campaignStatus(second)).isEqualTo("PAUSED");
        });
        assertThat(campaignStatus(draft)).isEqualTo("DRAFT");
        assertThat(campaignStatus(otherActive)).isEqualTo("ACTIVE");
        mockMvc.perform(get("/api/advertisers/{id}", target))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    private long createAdvertiser(String email) throws Exception {
        return id(mockMvc.perform(post("/api/advertisers").contentType(MediaType.APPLICATION_JSON)
                .content(advertiserJson(email))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING")));
    }

    private String advertiserJson(String email) {
        return """
                {"email":"%s","companyName":"Test Company","sector":"Tech","website":"https://example.com"}
                """.formatted(email);
    }

    private long activeAdvertiser(String email) throws Exception {
        long id = createAdvertiser(email);
        changeProfileStatus(id, "ACTIVE");
        return id;
    }

    private void changeProfileStatus(long id, String newStatus) throws Exception {
        mockMvc.perform(patch("/api/admin/profiles/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + newStatus + "\"}")).andExpect(status().isNoContent());
    }

    private long createOffer(long advertiserId, String title, String niche) throws Exception {
        return id(mockMvc.perform(post("/api/offres").contentType(MediaType.APPLICATION_JSON).content("""
                {"advertiserId":%d,"title":"%s","description":"Description",
                 "landingUrl":"https://example.com","niche":"%s","tags":[" Featured "]}
                """.formatted(advertiserId, title, niche))).andExpect(status().isCreated()));
    }

    private long createCampaign(long offerId, String name) throws Exception {
        return id(mockMvc.perform(post("/api/offres/{id}/campaigns", offerId).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"%s","startDate":"2030-01-01","endDate":"2030-02-01",
                         "budget":1000,"payoutModel":"CPC","payoutValue":2}
                        """.formatted(name))).andExpect(status().isCreated()));
    }

    private void changeCampaignStatus(long id, String newStatus) throws Exception {
        mockMvc.perform(patch("/api/campaigns/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + newStatus + "\"}")).andExpect(status().isNoContent());
    }

    private String campaignStatus(long id) {
        return jdbc.queryForObject("SELECT status FROM offer_campaigns WHERE id = ?", String.class, id);
    }

    private ResultActions searchPage(long advertiserId, int page) throws Exception {
        return mockMvc.perform(get("/api/offres").param("advertiserId", Long.toString(advertiserId))
                        .param("niche", "GAMING").param("tag", " FEATURED ").param("keyword", " gaming ")
                        .param("page", Integer.toString(page)).param("size", "1").param("sort", "title,asc"))
                .andExpect(status().isOk());
    }

    private long id(ResultActions result) throws Exception {
        Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
