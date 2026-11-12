package com.affiliatelink.offer.service;

import com.affiliatelink.common.exception.BusinessConflictException;
import com.affiliatelink.common.exception.ResourceNotFoundException;
import com.affiliatelink.offer.dto.CampaignRequest;
import com.affiliatelink.offer.dto.CampaignResponse;
import com.affiliatelink.offer.entity.*;
import com.affiliatelink.offer.mapper.CampaignMapper;
import com.affiliatelink.offer.repository.CampaignRepository;
import com.affiliatelink.offer.repository.OfferRepository;
import com.affiliatelink.profile.AdvertiserVerification;
import com.affiliatelink.profile.Niche;
import com.affiliatelink.profile.ProfileApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {
    @Mock OfferRepository offerRepository;
    @Mock CampaignRepository campaignRepository;
    @Mock ProfileApi profileApi;
    @Mock CampaignMapper campaignMapper;

    CampaignService service;
    Offer offer;
    final LocalDate start = LocalDate.of(2030, 1, 1);

    @BeforeEach
    void setUp() {
        service = new CampaignService(offerRepository, campaignRepository, profileApi, campaignMapper);
        offer = new Offer(10L, "Offer", "Description", "https://example.com", Niche.GAMING);
        ReflectionTestUtils.setField(offer, "id", 1L);
    }

    @ParameterizedTest
    @CsvSource({"CPC,0.01", "CPL,1", "CPA_FIXED,10", "CPA_PERCENT,0", "CPA_PERCENT,50", "CPA_PERCENT,100"})
    void createShouldSaveDraftForValidPayout(PayoutModel model, BigDecimal payout) {
        // Given
        CampaignRequest request = request(model, payout, start.plusDays(10));
        allowCreation();
        Campaign saved = campaign(2L, CampaignStatus.DRAFT, request.endDate());
        CampaignResponse response = response(saved);
        when(campaignRepository.save(any(Campaign.class))).thenReturn(saved);
        when(campaignMapper.toResponse(saved)).thenReturn(response);

        // When
        CampaignResponse result = service.create(1L, request);

        // Then
        assertThat(result).isSameAs(response);
        ArgumentCaptor<Campaign> captor = ArgumentCaptor.forClass(Campaign.class);
        verify(campaignRepository).save(captor.capture());
        Campaign created = captor.getValue();
        assertThat(created.getStatus()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(created.getOffer()).isSameAs(offer);
        assertThat(created.getName()).isEqualTo(request.name());
        assertThat(created.getStartDate()).isEqualTo(request.startDate());
        assertThat(created.getEndDate()).isEqualTo(request.endDate());
        assertThat(created.getBudget()).isEqualByComparingTo(request.budget());
        assertThat(created.getPayoutModel()).isEqualTo(model);
        assertThat(created.getPayoutValue()).isEqualByComparingTo(payout);
    }

    @ParameterizedTest
    @CsvSource({"CPC,0", "CPC,-1", "CPL,0", "CPL,-1", "CPA_FIXED,0", "CPA_FIXED,-1",
            "CPA_PERCENT,-0.01", "CPA_PERCENT,100.01"})
    void createShouldRejectInvalidPayoutWithoutSaving(PayoutModel model, BigDecimal payout) {
        // Given
        allowCreation();
        CampaignRequest request = request(model, payout, start.plusDays(1));
        // When / Then
        assertThatIllegalArgumentException().isThrownBy(() -> service.create(1L, request));
        verifyNoInteractions(campaignRepository, campaignMapper);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void createShouldRejectEndDateNotAfterStartDate(int days) {
        // Given
        allowCreation();
        CampaignRequest request = request(PayoutModel.CPC, BigDecimal.ONE, start.plusDays(days));
        // When / Then
        assertThatIllegalArgumentException().isThrownBy(() -> service.create(1L, request))
                .withMessage("End date must be after start date");
        verifyNoInteractions(campaignRepository, campaignMapper);
    }

    @Test
    void createShouldRejectMissingOffer() {
        // Given
        when(offerRepository.findById(1L)).thenReturn(Optional.empty());
        // When / Then
        assertThatThrownBy(() -> service.create(1L, request(PayoutModel.CPC, BigDecimal.ONE, start.plusDays(1))))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(profileApi, campaignRepository, campaignMapper);
    }

    @ParameterizedTest
    @EnumSource(value = AdvertiserVerification.class, names = {"NOT_FOUND", "INACTIVE"})
    void createShouldRejectUnavailableAdvertiser(AdvertiserVerification verification) {
        // Given
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(profileApi.verifyAdvertiser(10L)).thenReturn(verification);
        // When / Then
        assertThatThrownBy(() -> service.create(1L, request(PayoutModel.CPC, BigDecimal.ONE, start.plusDays(1))))
                .isInstanceOf(verification == AdvertiserVerification.NOT_FOUND
                        ? ResourceNotFoundException.class : BusinessConflictException.class);
        verifyNoInteractions(campaignRepository, campaignMapper);
    }

    @ParameterizedTest
    @CsvSource({"DRAFT,ACTIVE", "ACTIVE,PAUSED", "ACTIVE,ENDED", "PAUSED,ACTIVE", "PAUSED,ENDED"})
    void updateStatusShouldAllowValidTransition(CampaignStatus initial, CampaignStatus next) {
        // Given
        Campaign campaign = campaign(2L, initial, LocalDate.now().plusYears(1));
        when(campaignRepository.findById(2L)).thenReturn(Optional.of(campaign));
        if (next == CampaignStatus.ACTIVE) {
            when(profileApi.verifyAdvertiser(10L)).thenReturn(AdvertiserVerification.ACTIVE);
            when(campaignRepository.findByOfferIdAndStatus(1L, CampaignStatus.ACTIVE)).thenReturn(List.of());
        }
        // When
        service.updateStatus(2L, next);
        // Then
        assertThat(campaign.getStatus()).isEqualTo(next);
        if (next != CampaignStatus.ACTIVE) {
            verifyNoInteractions(profileApi);
            verify(campaignRepository, never()).findByOfferIdAndStatus(anyLong(), any());
        }
    }

    @ParameterizedTest
    @CsvSource({"DRAFT,DRAFT", "DRAFT,PAUSED", "DRAFT,ENDED", "ACTIVE,DRAFT", "ACTIVE,ACTIVE",
            "PAUSED,DRAFT", "PAUSED,PAUSED", "ENDED,DRAFT", "ENDED,ACTIVE", "ENDED,PAUSED", "ENDED,ENDED"})
    void updateStatusShouldRejectInvalidTransition(CampaignStatus initial, CampaignStatus next) {
        // Given
        Campaign campaign = campaign(2L, initial, LocalDate.now().plusYears(1));
        when(campaignRepository.findById(2L)).thenReturn(Optional.of(campaign));
        // When / Then
        assertThatThrownBy(() -> service.updateStatus(2L, next)).isInstanceOf(BusinessConflictException.class);
        assertThat(campaign.getStatus()).isEqualTo(initial);
        verifyNoInteractions(profileApi);
        verify(campaignRepository, never()).findByOfferIdAndStatus(anyLong(), any());
    }

    @ParameterizedTest
    @EnumSource(value = CampaignStatus.class, names = {"DRAFT", "PAUSED"})
    void activationShouldRejectExpiredCampaignWithoutPausingOtherCampaigns(CampaignStatus initial) {
        // Given
        Campaign campaign = campaign(2L, initial, LocalDate.now().minusDays(1));
        when(campaignRepository.findById(2L)).thenReturn(Optional.of(campaign));
        // When / Then
        assertThatThrownBy(() -> service.updateStatus(2L, CampaignStatus.ACTIVE))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessage("Cannot activate campaign: end date has passed");
        assertThat(campaign.getStatus()).isEqualTo(initial);
        verify(campaignRepository, never()).findByOfferIdAndStatus(anyLong(), any());
    }

    @Test
    void activationShouldAllowEndDateTodayAndPausePreviousActiveCampaign() {
        // Given
        Campaign campaign = campaign(2L, CampaignStatus.DRAFT, LocalDate.now());
        Campaign previous = campaign(3L, CampaignStatus.ACTIVE, LocalDate.now().plusDays(10));
        when(campaignRepository.findById(2L)).thenReturn(Optional.of(campaign));
        when(profileApi.verifyAdvertiser(10L)).thenReturn(AdvertiserVerification.ACTIVE);
        when(campaignRepository.findByOfferIdAndStatus(1L, CampaignStatus.ACTIVE)).thenReturn(List.of(previous));
        // When
        service.updateStatus(2L, CampaignStatus.ACTIVE);
        // Then
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
        assertThat(previous.getStatus()).isEqualTo(CampaignStatus.PAUSED);
        verify(campaignRepository).findByOfferIdAndStatus(1L, CampaignStatus.ACTIVE);
    }

    @ParameterizedTest
    @EnumSource(value = AdvertiserVerification.class, names = {"NOT_FOUND", "INACTIVE"})
    void activationShouldRejectUnavailableAdvertiser(AdvertiserVerification verification) {
        // Given
        Campaign campaign = campaign(2L, CampaignStatus.PAUSED, LocalDate.now().plusYears(1));
        when(campaignRepository.findById(2L)).thenReturn(Optional.of(campaign));
        when(profileApi.verifyAdvertiser(10L)).thenReturn(verification);
        // When / Then
        assertThatThrownBy(() -> service.updateStatus(2L, CampaignStatus.ACTIVE)).isInstanceOf(BusinessConflictException.class);
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.PAUSED);
        verify(campaignRepository, never()).findByOfferIdAndStatus(anyLong(), any());
    }

    @Test
    void updateStatusShouldRejectMissingCampaign() {
        // Given
        when(campaignRepository.findById(2L)).thenReturn(Optional.empty());
        // When / Then
        assertThatThrownBy(() -> service.updateStatus(2L, CampaignStatus.ACTIVE)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(profileApi);
    }

    private void allowCreation() {
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(profileApi.verifyAdvertiser(10L)).thenReturn(AdvertiserVerification.ACTIVE);
    }

    private CampaignRequest request(PayoutModel model, BigDecimal payout, LocalDate end) {
        return new CampaignRequest("Campaign", start, end, new BigDecimal("1000"), model, payout);
    }

    private Campaign campaign(Long id, CampaignStatus status, LocalDate end) {
        Campaign campaign = new Campaign("Campaign", end.minusDays(10), end, new BigDecimal("1000"),
                PayoutModel.CPC, BigDecimal.ONE, offer);
        ReflectionTestUtils.setField(campaign, "id", id);
        campaign.changeStatus(status);
        return campaign;
    }

    private CampaignResponse response(Campaign campaign) {
        return new CampaignResponse(campaign.getId(), offer.getId(), campaign.getName(), campaign.getStartDate(),
                campaign.getEndDate(), campaign.getBudget(), campaign.getPayoutModel(), campaign.getPayoutValue(), campaign.getStatus());
    }
}
