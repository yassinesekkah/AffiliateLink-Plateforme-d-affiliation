package com.affiliatelink.offer;

import com.affiliatelink.offer.entity.Campaign;
import com.affiliatelink.offer.entity.CampaignStatus;
import com.affiliatelink.offer.entity.Offer;
import com.affiliatelink.offer.entity.PayoutModel;
import com.affiliatelink.offer.repository.CampaignRepository;
import com.affiliatelink.offer.repository.OfferRepository;
import com.affiliatelink.profile.Niche;
import com.affiliatelink.profile.ProfileSuspended;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.ALL_DEPENDENCIES)
@TestPropertySource(properties = "spring.docker.compose.enabled=false")
@Testcontainers
class CampaignSuspensionModuleIT {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("affiliatelink_module_test").withUsername("test").withPassword("test");

    @Autowired CampaignRepository campaignRepository;
    @Autowired OfferRepository offerRepository;
    @Autowired TransactionTemplate transactions;

    @AfterEach
    void cleanUp() {
        // Scenario commits its transactions; clean up explicitly between independent tests.
        transactions.executeWithoutResult(status -> {
            campaignRepository.deleteAllInBatch();
            offerRepository.deleteAllInBatch();
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3})
    void profileSuspendedShouldPauseOnlyAdvertiserActiveCampaigns(int activeCount, Scenario scenario) {
        // Given: committed fixtures are visible to the asynchronous listener.
        List<Long> activeIds = new ArrayList<>();
        Map<Long, CampaignStatus> expectedStatuses = transactions.execute(status -> {
            Map<Long, CampaignStatus> expected = new LinkedHashMap<>();
            for (int i = 0; i < activeCount; i++) {
                Offer offer = saveOffer(10L, "Target offer " + i);
                Long id = saveCampaign(offer, CampaignStatus.ACTIVE).getId();
                activeIds.add(id);
                expected.put(id, CampaignStatus.PAUSED);
            }
            Offer targetOffer = saveOffer(10L, "Other target statuses");
            for (CampaignStatus initial : List.of(CampaignStatus.DRAFT, CampaignStatus.PAUSED, CampaignStatus.ENDED)) {
                expected.put(saveCampaign(targetOffer, initial).getId(), initial);
            }
            Offer otherAdvertiser = saveOffer(20L, "Other advertiser");
            for (CampaignStatus initial : CampaignStatus.values()) {
                expected.put(saveCampaign(otherAdvertiser, initial).getId(), initial);
            }
            return expected;
        });

        // When: publish through Spring, without calling the listener directly.
        scenario.publish(new ProfileSuspended(10L))
                .customize(wait -> wait.atMost(Duration.ofSeconds(10)))
                .andWaitForStateChange(this::readStatuses,
                        statuses -> activeIds.stream().allMatch(id -> statuses.get(id) == CampaignStatus.PAUSED))
                // Then: all target active campaigns paused; all other statuses preserved.
                .andVerify(statuses -> assertThat(statuses).containsExactlyInAnyOrderEntriesOf(expectedStatuses));
    }

    private Map<Long, CampaignStatus> readStatuses() {
        return transactions.execute(status -> {
            Map<Long, CampaignStatus> statuses = new LinkedHashMap<>();
            campaignRepository.findAll().forEach(campaign -> statuses.put(campaign.getId(), campaign.getStatus()));
            return statuses;
        });
    }

    private Offer saveOffer(Long advertiserId, String title) {
        return offerRepository.save(new Offer(advertiserId, title, "Description", "https://example.com", Niche.GAMING));
    }

    private Campaign saveCampaign(Offer offer, CampaignStatus status) {
        Campaign campaign = new Campaign("Campaign " + status, LocalDate.of(2030, 1, 1),
                LocalDate.of(2030, 2, 1), new BigDecimal("1000"), PayoutModel.CPC, BigDecimal.ONE, offer);
        campaign.changeStatus(status);
        return campaignRepository.save(campaign);
    }
}
