package com.affiliatelink.offer.service;

import com.affiliatelink.common.exception.BusinessConflictException;
import com.affiliatelink.common.exception.ResourceNotFoundException;
import com.affiliatelink.offer.dto.CampaignRequest;
import com.affiliatelink.offer.dto.CampaignResponse;
import com.affiliatelink.offer.entity.Campaign;
import com.affiliatelink.offer.entity.CampaignStatus;
import com.affiliatelink.offer.entity.Offer;
import com.affiliatelink.offer.entity.PayoutModel;
import com.affiliatelink.offer.mapper.CampaignMapper;
import com.affiliatelink.offer.repository.CampaignRepository;
import com.affiliatelink.offer.repository.OfferRepository;
import com.affiliatelink.profile.AdvertiserVerification;
import com.affiliatelink.profile.ProfileApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class CampaignService {

    private final OfferRepository offerRepository;
    private final CampaignRepository campaignRepository;
    private final ProfileApi profileApi;
    private final CampaignMapper campaignMapper;
    private final Logger log = LoggerFactory.getLogger(CampaignService.class);

    public CampaignService(OfferRepository offerRepository, CampaignRepository campaignRepository, ProfileApi profileApi, CampaignMapper campaignMapper) {
        this.offerRepository = offerRepository;
        this.campaignRepository = campaignRepository;
        this.profileApi = profileApi;
        this.campaignMapper = campaignMapper;
    }

    @Transactional
    public CampaignResponse create(Long offerId, CampaignRequest request){

        //fetch offer with offerId
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(()->
                        new ResourceNotFoundException("Offer not found: " + offerId)
                );

        //verifie l'advertiser
        AdvertiserVerification result = profileApi.verifyAdvertiser(offer.getAdvertiserId());

        if (result == AdvertiserVerification.NOT_FOUND) {
            throw new ResourceNotFoundException("Advertiser not found");
        }

        if (result == AdvertiserVerification.INACTIVE) {
            throw new BusinessConflictException("Advertiser is not active");
        }

        //verifie les dates
        if (!request.endDate().isAfter(request.startDate())) {
            throw new IllegalArgumentException(
                    "End date must be after start date"
            );
        }

        // 4. Validate payout
        BigDecimal payoutValue = request.payoutValue();

        if (request.payoutModel() == PayoutModel.CPA_PERCENT) {

            if (payoutValue.compareTo(BigDecimal.ZERO) < 0
                    || payoutValue.compareTo(new BigDecimal("100")) > 0) {

                throw new IllegalArgumentException(
                        "CPA_PERCENT payout must be between 0 and 100"
                );
            }

        } else {

            if (payoutValue.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Payout value must be greater than 0"
                );
            }
        }

        // 5. Convert DTO to Entity
        Campaign campaign = new Campaign(
                request.name(),
                request.startDate(),
                request.endDate(),
                request.budget(),
                request.payoutModel(),
                request.payoutValue(),
                offer
        );

        Campaign savedCampaign = campaignRepository.save(campaign);

        log.info("Campaign saved: campaignId={}, offerId={}",
                savedCampaign.getId(), offer.getId());

        return campaignMapper.toResponse(savedCampaign);

        }
    }

