package com.affiliatelink.profile.service;

import com.affiliatelink.common.exception.EmailAlreadyExistsException;
import com.affiliatelink.common.exception.ResourceNotFoundException;
import com.affiliatelink.profile.dto.AdvertiserRequest;
import com.affiliatelink.profile.entity.Advertiser;
import com.affiliatelink.profile.repository.AdvertiserRepository;
import com.affiliatelink.profile.repository.ProfileRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


public class AdvertiserServiceTest {

    @Test
    void createAdvertiserShouldRejectExistingEmail() {
        // Given
        ProfileRepository profileRepository = mock(ProfileRepository.class);
        AdvertiserRepository advertiserRepository = mock(AdvertiserRepository.class);

        AdvertiserService advertiserService = new AdvertiserService(advertiserRepository, profileRepository);

        AdvertiserRequest request = new AdvertiserRequest(
                "test@gmail.com",
                "Test Company",
                "Tech",
                "https://example.com"
        );

        when(profileRepository.existsByEmail("test@gmail.com"))
                .thenReturn(true);
        // When Then
        assertThrows(EmailAlreadyExistsException.class,
                () -> advertiserService.createAdvertiser(request));

        verify(advertiserRepository, never()).save(any(Advertiser.class));
    }

    @Test
    void findByIdShouldThrowWhenAdvertiserDoesNotExist(){
        //given
        AdvertiserRepository advertiserRepository = mock(AdvertiserRepository.class);
        ProfileRepository profileRepository = mock(ProfileRepository.class);
        AdvertiserService advertiserService = new AdvertiserService(advertiserRepository, profileRepository);

        when(advertiserRepository.findById(4L)).thenReturn(Optional.empty());

        //when then
        assertThrows(ResourceNotFoundException.class,
                () -> advertiserService.findById(4L));

    }
}
