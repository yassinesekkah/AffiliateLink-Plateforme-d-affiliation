package com.affiliatelink.offer.service;

import com.affiliatelink.common.exception.BusinessConflictException;
import com.affiliatelink.common.exception.ResourceNotFoundException;
import com.affiliatelink.offer.dto.OfferRequest;
import com.affiliatelink.offer.dto.OfferResponse;
import com.affiliatelink.offer.entity.Offer;
import com.affiliatelink.offer.entity.Tag;
import com.affiliatelink.offer.mapper.OfferMapper;
import com.affiliatelink.offer.repository.OfferRepository;
import com.affiliatelink.offer.repository.TagRepository;
import com.affiliatelink.profile.AdvertiserVerification;
import com.affiliatelink.profile.Niche;
import com.affiliatelink.profile.ProfileApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferServiceTest {
    @Mock OfferRepository offerRepository;
    @Mock ProfileApi profileApi;
    @Mock OfferMapper offerMapper;
    @Mock TagRepository tagRepository;

    OfferService service;

    @BeforeEach
    void setUp() {
        service = new OfferService(offerRepository, profileApi, offerMapper, tagRepository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void createShouldSaveOfferForActiveAdvertiserWithoutTags(Set<String> tags) {
        // Given
        OfferRequest request = request(tags);
        Offer offer = offer();
        OfferResponse response = response(Set.of());
        allowCreation(request, offer, response);

        // When
        OfferResponse result = service.create(request);

        // Then
        assertThat(result).isSameAs(response);
        assertThat(offer.getTags()).isEmpty();
        verify(profileApi).verifyAdvertiser(10L);
        verify(offerMapper).toEntity(request);
        verify(offerRepository).save(offer);
        verify(offerMapper).toResponse(offer);
        verifyNoInteractions(tagRepository);
    }

    @ParameterizedTest
    @EnumSource(value = AdvertiserVerification.class, names = {"NOT_FOUND", "INACTIVE"})
    void createShouldRejectUnavailableAdvertiserWithoutSaving(AdvertiserVerification verification) {
        // Given
        OfferRequest request = request(Set.of("gaming"));
        when(profileApi.verifyAdvertiser(10L)).thenReturn(verification);

        // When / Then
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(verification == AdvertiserVerification.NOT_FOUND
                        ? ResourceNotFoundException.class : BusinessConflictException.class)
                .hasMessage(verification == AdvertiserVerification.NOT_FOUND
                        ? "Advertiser not found" : "Advertiser is not active");
        verifyNoInteractions(offerRepository, tagRepository, offerMapper);
    }

    @Test
    void createShouldTrimAndLowercaseNewTagBeforeSaving() {
        // Given
        OfferRequest request = request(Set.of("  GAMING  "));
        Offer offer = offer();
        OfferResponse response = response(Set.of("gaming"));
        allowCreation(request, offer, response);
        when(tagRepository.findByName("gaming")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        OfferResponse result = service.create(request);

        // Then
        ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).save(tagCaptor.capture());
        assertThat(tagCaptor.getValue().getName()).isEqualTo("gaming");
        assertThat(offer.getTags()).containsExactly(tagCaptor.getValue());
        assertThat(result).isSameAs(response);
        verify(tagRepository).findByName("gaming");
    }

    @Test
    void createShouldReuseExistingTagWithoutDuplicatesAfterNormalization() {
        // Given
        OfferRequest request = request(Set.of("GAMING", " gaming ", "gaming"));
        Offer offer = offer();
        Tag existing = new Tag("gaming");
        allowCreation(request, offer, response(Set.of("gaming")));
        when(tagRepository.findByName("gaming")).thenReturn(Optional.of(existing));

        // When
        service.create(request);

        // Then
        assertThat(offer.getTags()).containsExactly(existing);
        verify(tagRepository, never()).save(any());
        verify(offerRepository).save(offer);
    }

    @Test
    void createShouldSaveNewTagOnlyOnceWhenNamesNormalizeToSameValue() {
        // Given
        OfferRequest request = request(Set.of("GAMING", " gaming "));
        Offer offer = offer();
        Tag saved = new Tag("gaming");
        allowCreation(request, offer, response(Set.of("gaming")));
        // The second lookup sees the tag saved by the first lookup.
        when(tagRepository.findByName("gaming")).thenReturn(Optional.empty(), Optional.of(saved));
        when(tagRepository.save(any(Tag.class))).thenReturn(saved);

        // When
        service.create(request);

        // Then
        assertThat(offer.getTags()).containsExactly(saved);
        ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).save(tagCaptor.capture());
        assertThat(tagCaptor.getValue().getName()).isEqualTo("gaming");
    }

    @Test
    void createShouldShareExistingTagBetweenOffers() {
        // Given
        OfferRequest firstRequest = request(Set.of("gaming"));
        OfferRequest secondRequest = new OfferRequest(20L, "Other offer", "Other description",
                "https://example.org", Niche.GAMING, Set.of(" GAMING "));
        Offer first = offer();
        Offer second = new Offer(20L, "Other offer", "Other description", "https://example.org", Niche.GAMING);
        Tag shared = new Tag("gaming");
        when(profileApi.verifyAdvertiser(10L)).thenReturn(AdvertiserVerification.ACTIVE);
        when(profileApi.verifyAdvertiser(20L)).thenReturn(AdvertiserVerification.ACTIVE);
        when(offerMapper.toEntity(firstRequest)).thenReturn(first);
        when(offerMapper.toEntity(secondRequest)).thenReturn(second);
        when(tagRepository.findByName("gaming")).thenReturn(Optional.of(shared));
        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        service.create(firstRequest);
        service.create(secondRequest);

        // Then
        assertThat(first.getTags()).containsExactly(shared);
        assertThat(second.getTags()).containsExactly(shared);
        verify(tagRepository, never()).save(any());
        verify(offerRepository).save(first);
        verify(offerRepository).save(second);
    }

    @Test
    void getByIdShouldReturnMappedOffer() {
        // Given
        Offer offer = offer();
        OfferResponse response = response(Set.of());
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(offerMapper.toResponse(offer)).thenReturn(response);

        // When
        OfferResponse result = service.getById(1L);

        // Then
        assertThat(result).isSameAs(response);
        verify(offerMapper).toResponse(offer);
    }

    @Test
    void getByIdShouldRejectMissingOffer() {
        // Given
        when(offerRepository.findById(99L)).thenReturn(Optional.empty());
        // When / Then
        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Offer not found 99");
        verifyNoInteractions(offerMapper, tagRepository, profileApi);
    }

    @Test
    void updateOfferShouldReplaceFieldsAndTagsAndReuseExistingTags() {
        // Given
        Offer offer = offer();
        Tag old = new Tag("old");
        Tag existing = new Tag("food");
        offer.getTags().add(old);
        OfferRequest request = new OfferRequest(10L, "Updated title", "Updated description",
                "https://updated.example.com", Niche.FOOD, Set.of(" FOOD ", "food", " NEW "));
        OfferResponse response = response(Set.of("food", "new"));
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(tagRepository.findByName("food")).thenReturn(Optional.of(existing));
        when(tagRepository.findByName("new")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(offerMapper.toResponse(offer)).thenReturn(response);

        // When
        OfferResponse result = service.updateOffer(1L, request);

        // Then
        assertThat(result).isSameAs(response);
        assertThat(offer.getTitle()).isEqualTo(request.title());
        assertThat(offer.getDescription()).isEqualTo(request.description());
        assertThat(offer.getLandingUrl()).isEqualTo(request.landingUrl());
        assertThat(offer.getNiche()).isEqualTo(Niche.FOOD);
        assertThat(offer.getTags()).extracting(Tag::getName).containsExactlyInAnyOrder("food", "new");
        assertThat(offer.getTags()).contains(existing).doesNotContain(old);
        ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).save(tagCaptor.capture());
        assertThat(tagCaptor.getValue().getName()).isEqualTo("new");
        verify(tagRepository, never()).delete(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void updateOfferShouldClearOldTagsWhenNoTagsAreProvided(Set<String> tags) {
        // Given
        Offer offer = offer();
        offer.getTags().add(new Tag("old"));
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));

        // When
        service.updateOffer(1L, request(tags));

        // Then
        assertThat(offer.getTags()).isEmpty();
        verifyNoInteractions(tagRepository);
        verify(offerMapper).toResponse(offer);
    }

    @Test
    void updateOfferShouldRejectMissingOfferWithoutModifyingTags() {
        // Given
        when(offerRepository.findById(99L)).thenReturn(Optional.empty());
        // When / Then
        assertThatThrownBy(() -> service.updateOffer(99L, request(Set.of("gaming"))))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Offer not found 99");
        verifyNoInteractions(tagRepository, offerMapper, profileApi);
        verify(offerRepository, never()).save(any());
    }

    private OfferRequest request(Set<String> tags) {
        return new OfferRequest(10L, "Offer", "Description", "https://example.com", Niche.GAMING, tags);
    }

    private Offer offer() {
        return new Offer(10L, "Offer", "Description", "https://example.com", Niche.GAMING);
    }

    private OfferResponse response(Set<String> tags) {
        return new OfferResponse(1L, 10L, "Offer", "Description", "https://example.com", Niche.GAMING,
                tags, LocalDateTime.of(2030, 1, 1, 12, 0));
    }

    private void allowCreation(OfferRequest request, Offer offer, OfferResponse response) {
        when(profileApi.verifyAdvertiser(request.advertiserId())).thenReturn(AdvertiserVerification.ACTIVE);
        when(offerMapper.toEntity(request)).thenReturn(offer);
        when(offerRepository.save(offer)).thenReturn(offer);
        when(offerMapper.toResponse(offer)).thenReturn(response);
    }
}
