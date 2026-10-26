package com.affiliatelink.profile.service;

import com.affiliatelink.profile.ProfileApi;
import com.affiliatelink.profile.entity.AccountStatus;
import com.affiliatelink.profile.entity.Advertiser;
import com.affiliatelink.profile.repository.AdvertiserRepository;

import java.util.Optional;

public class ProfileApiImpl implements ProfileApi {

    private final AdvertiserRepository advertiserRepository;

    public ProfileApiImpl(AdvertiserRepository advertiserRepository) {
        this.advertiserRepository = advertiserRepository;
    }

    @Override
    public boolean isActiveAdvertiser(Long advertiserId) {

        return advertiserRepository.findById(advertiserId)
                .map(advertiser -> advertiser.getStatus() == AccountStatus.ACTIVE)
                .orElse(false);

    }
}
