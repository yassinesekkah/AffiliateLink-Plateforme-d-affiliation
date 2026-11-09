package com.affiliatelink.offer.listener;

import com.affiliatelink.offer.entity.Campaign;
import com.affiliatelink.offer.entity.CampaignStatus;
import com.affiliatelink.offer.repository.CampaignRepository;
import com.affiliatelink.profile.ProfileSuspended;
import org.springframework.stereotype.Component;
import org.springframework.modulith.events.ApplicationModuleListener;

import java.util.List;

@Component
public class CampaignSuspensionListener {

    private final CampaignRepository campaignRepository;

    public CampaignSuspensionListener(
            CampaignRepository campaignRepository
    ) {
        this.campaignRepository = campaignRepository;
    }

    @ApplicationModuleListener
    public void onProfileSuspended(ProfileSuspended event) {

        List<Campaign> campaigns =
                campaignRepository.findByOfferAdvertiserIdAndStatus(
                        event.profileId(),
                        CampaignStatus.ACTIVE
                );

        for (Campaign campaign : campaigns) {
            campaign.changeStatus(CampaignStatus.PAUSED);
        }
    }
}
