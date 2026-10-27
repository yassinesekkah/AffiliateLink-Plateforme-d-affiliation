package com.affiliatelink.profile.service;

import com.affiliatelink.profile.AdvertiserVerification;
import com.affiliatelink.profile.ProfileApi;
import com.affiliatelink.profile.entity.AccountStatus;
import com.affiliatelink.profile.entity.Advertiser;
import com.affiliatelink.profile.repository.AdvertiserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProfileApiImpl implements ProfileApi {

    private final AdvertiserRepository advertiserRepository;

    public ProfileApiImpl(AdvertiserRepository advertiserRepository) {
        this.advertiserRepository = advertiserRepository;
    }

    @Override
    public AdvertiserVerification verifyAdvertiser(Long advertiserId) {

        Optional<Advertiser> advertiserOpt = advertiserRepository.findById(advertiserId);

        if(advertiserOpt.isEmpty()){
            return AdvertiserVerification.NOT_FOUND;
        }
        if(advertiserOpt.get().getStatus() != AccountStatus.ACTIVE){
            return AdvertiserVerification.INACTIVE;
        }

        return AdvertiserVerification.ACTIVE;
    }
}
