package com.affiliatelink.profile.service;

import com.affiliatelink.common.exception.ResourceNotFoundException;
import com.affiliatelink.profile.ProfileSuspended;
import com.affiliatelink.profile.dto.UpdateProfileStatusRequest;
import com.affiliatelink.profile.entity.AccountStatus;
import com.affiliatelink.profile.entity.Advertiser;
import com.affiliatelink.profile.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.Optional;

public class ProfileServiceTest {

    @Test
    void suspendingProfileShouldPublishProfileSuspendedEvent(){
        //given
        ProfileRepository profileRepository = mock(ProfileRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        Advertiser advertiser = new Advertiser(
                "email@test.com",
                "companyName"
        );

        Long profileId = 1L;
        ReflectionTestUtils.setField(advertiser, "id", profileId);

        advertiser.changeStatus(AccountStatus.ACTIVE);

        ProfileService profileService = new ProfileService(profileRepository, eventPublisher);

        UpdateProfileStatusRequest request = new UpdateProfileStatusRequest(AccountStatus.SUSPENDED);

        when(profileRepository.findById(profileId))
                .thenReturn(Optional.of(advertiser));

        //when
        profileService.updateStatus(profileId, request);

        //then
        assertEquals(AccountStatus.SUSPENDED, advertiser.getStatus());

        verify(eventPublisher).publishEvent(new ProfileSuspended(profileId));

    }

    @Test
    void activatingProfileShouldNotPublishEvent(){

        //given
        ProfileRepository profileRepository = mock(ProfileRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        Advertiser advertiser = new Advertiser(
                "email@test.com",
                "companyName"
        );

        Long profileId = 1L;
        ReflectionTestUtils.setField(advertiser, "id", profileId);

        ProfileService profileService = new ProfileService(profileRepository, eventPublisher);

        when(profileRepository.findById(profileId))
                .thenReturn(Optional.of(advertiser));

        UpdateProfileStatusRequest request =
                new UpdateProfileStatusRequest(AccountStatus.ACTIVE);

        //when
        profileService.updateStatus(profileId, request);

        //Then
        assertEquals(AccountStatus.ACTIVE, advertiser.getStatus());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void updateStatusShouldThrowWhenProfileDoesNotExist(){
        //given
        ProfileRepository profileRepository = mock(ProfileRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        ProfileService profileService = new ProfileService(profileRepository, eventPublisher);

        when(profileRepository.findById(2L)).thenReturn(Optional.empty());

        UpdateProfileStatusRequest statusRequest = new UpdateProfileStatusRequest(AccountStatus.ACTIVE);

        //when then
        assertThrows(ResourceNotFoundException.class,
                () -> profileService.updateStatus(2L, statusRequest));

    }

}
