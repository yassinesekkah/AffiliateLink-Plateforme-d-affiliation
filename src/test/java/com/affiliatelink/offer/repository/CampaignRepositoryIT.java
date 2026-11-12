package com.affiliatelink.offer.repository;

import com.affiliatelink.offer.entity.Campaign;
import com.affiliatelink.offer.entity.CampaignStatus;
import com.affiliatelink.offer.entity.Offer;
import com.affiliatelink.offer.entity.PayoutModel;
import com.affiliatelink.profile.Niche;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.docker.compose.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers
class CampaignRepositoryIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("affiliatelink_repository_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired CampaignRepository campaignRepository;
    @Autowired EntityManager entityManager;

    @ParameterizedTest
    @EnumSource(CampaignStatus.class)
    void findByStatusShouldReturnOnlyMatchingCampaignsAcrossOffers(CampaignStatus status) {
        // Given
        Offer first = persistOffer(10L, "First offer");
        Offer second = persistOffer(20L, "Second offer");
        List<Long> expectedIds = new ArrayList<>();
        for (CampaignStatus candidate : CampaignStatus.values()) {
            Campaign one = persistCampaign(first, candidate);
            Campaign two = persistCampaign(second, candidate);
            if (candidate == status) {
                expectedIds.add(one.getId());
                expectedIds.add(two.getId());
            }
        }
        flushAndClear();

        // When
        List<Campaign> result = campaignRepository.findByStatus(status);

        // Then
        assertThat(result).extracting(Campaign::getId).containsExactlyInAnyOrderElementsOf(expectedIds);
        assertThat(result).allSatisfy(campaign -> assertThat(campaign.getStatus()).isEqualTo(status));
    }

    @ParameterizedTest
    @EnumSource(CampaignStatus.class)
    void findByOfferIdAndStatusShouldApplyBothFilters(CampaignStatus status) {
        // Given: even another offer from the same advertiser must be excluded.
        Offer target = persistOffer(10L, "Target offer");
        Offer other = persistOffer(10L, "Other offer");
        Long offerId = target.getId();
        Campaign expected = persistCampaign(target, status);
        for (CampaignStatus candidate : CampaignStatus.values()) {
            persistCampaign(other, candidate);
            if (candidate != status) {
                persistCampaign(target, candidate);
            }
        }
        Long expectedId = expected.getId();
        flushAndClear();

        // When
        List<Campaign> result = campaignRepository.findByOfferIdAndStatus(offerId, status);

        // Then
        assertThat(result).extracting(Campaign::getId).containsExactly(expectedId);
    }

    @ParameterizedTest
    @EnumSource(CampaignStatus.class)
    void findByOfferAdvertiserIdAndStatusShouldIncludeAllAdvertiserOffersAndExcludeOthers(CampaignStatus status) {
        // Given
        Offer first = persistOffer(10L, "First advertiser offer");
        Offer second = persistOffer(10L, "Second advertiser offer");
        Offer otherAdvertiser = persistOffer(20L, "Other advertiser offer");
        List<Long> expectedIds = new ArrayList<>();
        for (CampaignStatus candidate : CampaignStatus.values()) {
            Campaign one = persistCampaign(first, candidate);
            Campaign two = persistCampaign(second, candidate);
            persistCampaign(otherAdvertiser, candidate);
            if (candidate == status) {
                expectedIds.add(one.getId());
                expectedIds.add(two.getId());
            }
        }
        flushAndClear();

        // When
        List<Campaign> result = campaignRepository.findByOfferAdvertiserIdAndStatus(10L, status);

        // Then
        assertThat(result).extracting(Campaign::getId).containsExactlyInAnyOrderElementsOf(expectedIds);
    }

    @Test
    void findByStatusShouldReturnEmptyListWhenNoCampaignHasRequestedStatus() {
        // Given
        persistCampaign(persistOffer(10L, "Offer"), CampaignStatus.DRAFT);
        flushAndClear();

        // When
        List<Campaign> result = campaignRepository.findByStatus(CampaignStatus.ACTIVE);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findByOfferIdAndStatusShouldReturnEmptyListWhenNoCampaignMatchesBothFilters() {
        // Given
        Offer target = persistOffer(10L, "Target offer");
        Long offerId = target.getId();
        persistCampaign(target, CampaignStatus.PAUSED);
        persistCampaign(persistOffer(10L, "Other offer"), CampaignStatus.ACTIVE);
        flushAndClear();

        // When
        List<Campaign> result = campaignRepository.findByOfferIdAndStatus(offerId, CampaignStatus.ACTIVE);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findByOfferAdvertiserIdAndStatusShouldReturnEmptyListWhenNoCampaignMatchesBothFilters() {
        // Given
        persistCampaign(persistOffer(10L, "Target advertiser offer"), CampaignStatus.PAUSED);
        persistCampaign(persistOffer(20L, "Other advertiser offer"), CampaignStatus.ACTIVE);
        flushAndClear();

        // When
        List<Campaign> result = campaignRepository.findByOfferAdvertiserIdAndStatus(10L, CampaignStatus.ACTIVE);

        // Then
        assertThat(result).isEmpty();
    }

    private Offer persistOffer(Long advertiserId, String title) {
        Offer offer = new Offer(advertiserId, title, "Description", "https://example.com", Niche.GAMING);
        entityManager.persist(offer);
        return offer;
    }

    private Campaign persistCampaign(Offer offer, CampaignStatus status) {
        Campaign campaign = new Campaign("Campaign " + status, LocalDate.of(2030, 1, 1),
                LocalDate.of(2030, 2, 1), new BigDecimal("1000.00"), PayoutModel.CPC,
                new BigDecimal("2.50"), offer);
        campaign.changeStatus(status);
        entityManager.persist(campaign);
        return campaign;
    }

    private void flushAndClear() {
        // Execute pending SQL and force queries to reload entities from PostgreSQL.
        entityManager.flush();
        entityManager.clear();
    }
}
