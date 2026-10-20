package com.affiliatelink.profile.service;

import com.affiliatelink.common.exception.EmailAlreadyExistsException;
import com.affiliatelink.profile.dto.CreatePublisherRequest;
import com.affiliatelink.profile.dto.CreatePublisherResponse;
import com.affiliatelink.profile.entity.Profile;
import com.affiliatelink.profile.entity.Publisher;
import com.affiliatelink.profile.repository.ProfileRepository;
import com.affiliatelink.profile.repository.PublisherRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
public class PublisherService {

    private final ProfileRepository profileRepository;
    private final PublisherRepository publisherRepository;

    public PublisherService(ProfileRepository profileRepository, PublisherRepository publisherRepository){
        this.profileRepository = profileRepository;
        this.publisherRepository = publisherRepository;
    }

    public CreatePublisherResponse createPublisher(CreatePublisherRequest request){

        Publisher publisher = new Publisher(request.email(), request.firstName(),
                request.lastName(), request.niche(), request.bio(), request.country()
        );

        if(profileRepository.existsByEmail(request.email())){
            throw new EmailAlreadyExistsException("Email deja utilise");
        }
        Publisher savedPublisher = publisherRepository.save(publisher);

        CreatePublisherResponse response = new CreatePublisherResponse(
                publisher.getId(),
                publisher.getEmail(),
                publisher.getFirstName(),
                publisher.getLastName(),
                publisher.getNiche(),
                publisher.getNiche().toString(),
                publisher.getCountry(),
                publisher.getStatus(),
                publisher.getCreatedAt()

        );

        return response;
    }
}
