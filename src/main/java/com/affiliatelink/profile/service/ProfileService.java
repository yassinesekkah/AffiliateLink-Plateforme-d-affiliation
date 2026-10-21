package com.affiliatelink.profile.service;

import com.affiliatelink.common.exception.ResourceNotFoundException;
import com.affiliatelink.profile.dto.UpdateProfileStatusRequest;
import com.affiliatelink.profile.entity.Profile;
import com.affiliatelink.profile.repository.ProfileRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository){
        this.profileRepository = profileRepository;
    }

    @Transactional
    public void updateStatus(Long id, UpdateProfileStatusRequest request){

        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        profile.changeStatus(request.status());
    }
}
