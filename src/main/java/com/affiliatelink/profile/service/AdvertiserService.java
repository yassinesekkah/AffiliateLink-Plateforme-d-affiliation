package com.affiliatelink.profile.service;

import com.affiliatelink.common.exception.EmailAlreadyExistsException;
import com.affiliatelink.profile.dto.AdvertierResponse;
import com.affiliatelink.profile.dto.AdvertiserRequest;
import com.affiliatelink.profile.entity.Advertiser;
import com.affiliatelink.profile.repository.AdvertiserRepository;
import com.affiliatelink.profile.repository.ProfileRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdvertiserService {

    private final AdvertiserRepository advertiserRepository;
    private final ProfileRepository profileRepository;

    public AdvertiserService(AdvertiserRepository advertiserRepository, ProfileRepository profileRepository){
        this.advertiserRepository = advertiserRepository;
        this.profileRepository = profileRepository;
    }

    private AdvertierResponse toResponse(Advertiser advertiser){
        return new AdvertierResponse(
                advertiser.getId(),
                advertiser.getEmail(),
                advertiser.getCompanyName(),
                advertiser.getSector(),
                advertiser.getWebsite(),
                advertiser.getStatus(),
                advertiser.getCreatedAt()
        );
    }

    public AdvertierResponse createAdvertiser (AdvertiserRequest request){

        if(profileRepository.existsByEmail(request.email())){
            throw new EmailAlreadyExistsException("Email deja utilise");
        }

        Advertiser advertiser = new Advertiser(
                request.email(),
                request.companyName(),
                request.sector(),
                request.website()
        );

        Advertiser savedAdvertiser = advertiserRepository.save(advertiser);

        return toResponse(savedAdvertiser);

    }

    public List<AdvertierResponse> findAll(){

        List<Advertiser> advertisers = advertiserRepository.findAll();

        List<AdvertierResponse> advertierResponses = new ArrayList<>();

        for(Advertiser advertiser : advertisers){
            AdvertierResponse advertierResponse = toResponse(advertiser);
            advertierResponses.add(advertierResponse);
        }
        return advertierResponses;
    }
}
