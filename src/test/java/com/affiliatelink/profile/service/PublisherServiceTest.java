package com.affiliatelink.profile.service;

import com.affiliatelink.common.exception.EmailAlreadyExistsException;
import com.affiliatelink.common.exception.ResourceNotFoundException;
import com.affiliatelink.profile.Niche;
import com.affiliatelink.profile.dto.AdvertiserRequest;
import com.affiliatelink.profile.dto.CreatePublisherRequest;
import com.affiliatelink.profile.entity.Publisher;
import com.affiliatelink.profile.repository.ProfileRepository;
import com.affiliatelink.profile.repository.PublisherRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class PublisherServiceTest {

    @Test
    void createPublisherShouldRejectExistingEmail() {
        // Given
        ProfileRepository profileRepository = mock(ProfileRepository.class);
        PublisherRepository publisherRepository = mock(PublisherRepository.class);

        PublisherService publisherService = new PublisherService(profileRepository, publisherRepository);

         CreatePublisherRequest request = new CreatePublisherRequest(
                "test@gmail.com",
                "firstName",
                "lastNamt",
                 Niche.GAMING,
                 "bio",
                 "country"
        );

        when(profileRepository.existsByEmail("test@gmail.com"))
                .thenReturn(true);
        // When Then
        assertThrows(EmailAlreadyExistsException.class,
                () -> publisherService.createPublisher(request));

        verify(publisherRepository, never()).save(any(Publisher.class));

    }

    @Test
    void findByIdShouldThrowWhenPublisherDoesNotExist(){
        //given
        ProfileRepository profileRepository = mock(ProfileRepository.class);
        PublisherRepository publisherRepository = mock(PublisherRepository.class);

        PublisherService publisherService = new PublisherService(profileRepository, publisherRepository);

        when(publisherRepository.findById(2L)).thenReturn(Optional.empty());


        //when then
        assertThrows(ResourceNotFoundException.class,
                () -> publisherService.findById(2L));
    }
}
