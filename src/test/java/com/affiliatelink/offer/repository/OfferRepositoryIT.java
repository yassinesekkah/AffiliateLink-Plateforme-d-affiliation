package com.affiliatelink.offer.repository;

import com.affiliatelink.offer.dto.OfferResponse;
import com.affiliatelink.offer.entity.Offer;
import com.affiliatelink.offer.entity.Tag;
import com.affiliatelink.offer.mapper.OfferMapperImpl;
import com.affiliatelink.offer.service.OfferService;
import com.affiliatelink.profile.Niche;
import com.affiliatelink.profile.service.ProfileApiImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.docker.compose.enabled=false", "spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
// Exercise the actual Specifications built by OfferService, with real dependencies.
@Import({OfferService.class, OfferMapperImpl.class, ProfileApiImpl.class})
@Testcontainers
class OfferRepositoryIT {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("affiliatelink_offer_test").withUsername("test").withPassword("test");

    @Autowired EntityManager entityManager;
    @Autowired OfferRepository offerRepository;
    @Autowired OfferService offerService;

    @BeforeEach
    void setUp() {
        Tag gaming = new Tag("gaming");
        Tag popular = new Tag("popular");
        Tag food = new Tag("food");
        entityManager.persist(gaming);
        entityManager.persist(popular);
        entityManager.persist(food);
        persistOffer(10L, "Alpha Gaming", Niche.GAMING, gaming, popular);
        persistOffer(20L, "Beta Gaming", Niche.GAMING, gaming);
        persistOffer(10L, "Gamma Food", Niche.FOOD, food);
        persistOffer(30L, "Delta Untagged", Niche.FOOD);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void searchShouldFilterByNicheOnly() {
        // Given / When
        Page<OfferResponse> result = search(Niche.GAMING, null, null, null);
        // Then
        assertThat(result.getContent()).extracting(OfferResponse::title)
                .containsExactlyInAnyOrder("Alpha Gaming", "Beta Gaming");
    }

    @Test
    void searchShouldFilterByAdvertiserOnly() {
        // Given / When
        Page<OfferResponse> result = search(null, 10L, null, null);
        // Then
        assertThat(result.getContent()).extracting(OfferResponse::title)
                .containsExactlyInAnyOrder("Alpha Gaming", "Gamma Food");
    }

    @ParameterizedTest
    @ValueSource(strings = {"gaming", " GAMING "})
    void searchShouldFilterByNormalizedTagOnly(String tag) {
        // Given / When
        Page<OfferResponse> result = search(null, null, tag, null);
        // Then
        assertThat(result.getContent()).extracting(OfferResponse::title)
                .containsExactlyInAnyOrder("Alpha Gaming", "Beta Gaming");
    }

    @ParameterizedTest
    @ValueSource(strings = {"gaming", " GaMiNg "})
    void searchShouldMatchKeywordWithinTitleIgnoringCaseAndOuterSpaces(String keyword) {
        // Given / When
        Page<OfferResponse> result = search(null, null, null, keyword);
        // Then
        assertThat(result.getContent()).extracting(OfferResponse::title)
                .containsExactlyInAnyOrder("Alpha Gaming", "Beta Gaming");
    }

    @Test
    void searchShouldApplyAllCriteriaTogether() {
        // Given / When
        Page<OfferResponse> result = search(Niche.GAMING, 10L, "gaming", "alpha");
        // Then
        assertThat(result.getContent()).extracting(OfferResponse::title).containsExactly("Alpha Gaming");
    }

    @Test
    void searchShouldReturnEmptyWhenCriteriaMatchDifferentOffers() {
        // Given / When: advertiser 20 has gaming offers, but no food offers.
        Page<OfferResponse> result = search(Niche.FOOD, 20L, null, null);
        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void searchShouldReturnEmptyForUnknownTag() {
        // Given / When
        Page<OfferResponse> result = search(null, null, "unknown", null);
        // Then
        assertThat(result.getContent()).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void searchShouldReturnAllOffersIncludingUntaggedWhenNoCriteriaAreProvided(String ignoredFilter) {
        // Given / When
        Page<OfferResponse> result = search(null, null, ignoredFilter, ignoredFilter);
        // Then
        assertThat(result.getContent()).extracting(OfferResponse::title)
                .containsExactlyInAnyOrder("Alpha Gaming", "Beta Gaming", "Gamma Food", "Delta Untagged");
        assertThat(result.getTotalElements()).isEqualTo(4);
    }

    @ParameterizedTest
    @EnumSource(Sort.Direction.class)
    void searchShouldSortByTitle(Sort.Direction direction) {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(direction, "title"));
        // When
        Page<OfferResponse> result = offerService.getAll(pageable, null, null, null, null);
        // Then
        String[] ascending = {"Alpha Gaming", "Beta Gaming", "Delta Untagged", "Gamma Food"};
        String[] descending = {"Gamma Food", "Delta Untagged", "Beta Gaming", "Alpha Gaming"};
        assertThat(result.getContent()).extracting(OfferResponse::title)
                .containsExactly(direction == Sort.Direction.ASC ? ascending : descending);
    }

    @Test
    void searchShouldPaginateWithoutDuplicatesAndPreserveTotalCount() {
        // Given
        Sort sort = Sort.by("title");
        // When
        Page<OfferResponse> first = offerService.getAll(PageRequest.of(0, 2, sort), null, null, null, null);
        Page<OfferResponse> second = offerService.getAll(PageRequest.of(1, 2, sort), null, null, null, null);
        Page<OfferResponse> beyondLast = offerService.getAll(PageRequest.of(2, 2, sort), null, null, null, null);
        // Then
        assertThat(first.getContent()).extracting(OfferResponse::title).containsExactly("Alpha Gaming", "Beta Gaming");
        assertThat(second.getContent()).extracting(OfferResponse::title).containsExactly("Delta Untagged", "Gamma Food");
        assertThat(first.getTotalElements()).isEqualTo(4);
        assertThat(second.getTotalElements()).isEqualTo(4);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(beyondLast.getContent()).isEmpty();
        assertThat(beyondLast.getTotalElements()).isEqualTo(4);
    }

    @Test
    void searchShouldPaginateFilteredOffersWithoutDuplicatingOffersWithMultipleTags() {
        // Given / When
        Page<OfferResponse> result = offerService.getAll(PageRequest.of(0, 1, Sort.by("title")),
                null, null, "gaming", null);
        // Then
        assertThat(result.getContent()).extracting(OfferResponse::title).containsExactly("Alpha Gaming");
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    void persistedOffersShouldShareTheSameTagRow() {
        // Given / When
        var offers = offerRepository.findAll(Sort.by("title"));
        Tag firstGamingTag = offers.get(0).getTags().stream()
                .filter(tag -> tag.getName().equals("gaming")).findFirst().orElseThrow();
        Tag secondGamingTag = offers.get(1).getTags().iterator().next();
        // Then
        assertThat(firstGamingTag.getId()).isEqualTo(secondGamingTag.getId());
        assertThat(offers.get(0).getTags()).extracting(Tag::getName).containsExactlyInAnyOrder("gaming", "popular");
    }

    private Page<OfferResponse> search(Niche niche, Long advertiserId, String tag, String keyword) {
        return offerService.getAll(PageRequest.of(0, 20), niche, advertiserId, tag, keyword);
    }

    private void persistOffer(Long advertiserId, String title, Niche niche, Tag... tags) {
        Offer offer = new Offer(advertiserId, title, "Description", "https://example.com", niche);
        offer.getTags().addAll(java.util.List.of(tags));
        entityManager.persist(offer);
    }
}
