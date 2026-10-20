package com.affiliatelink.profile.service;

import com.affiliatelink.common.exception.EmailAlreadyExistsException;
import com.affiliatelink.profile.dto.CreatePublisherRequest;
import com.affiliatelink.profile.dto.CreatePublisherResponse;
import com.affiliatelink.profile.entity.Publisher;
import com.affiliatelink.profile.repository.ProfileRepository;
import com.affiliatelink.profile.repository.PublisherRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PublisherService {

    private final ProfileRepository profileRepository;
    private final PublisherRepository publisherRepository;

    public PublisherService(ProfileRepository profileRepository, PublisherRepository publisherRepository){
        this.profileRepository = profileRepository;
        this.publisherRepository = publisherRepository;
    }

    private CreatePublisherResponse toResponse(Publisher publisher){

        return new CreatePublisherResponse(
                publisher.getId(),
                publisher.getEmail(),
                publisher.getFirstName(),
                publisher.getLastName(),
                publisher.getNiche(),
                publisher.getBio(),
                publisher.getCountry(),
                publisher.getStatus(),
                publisher.getCreatedAt()
        );
    }

    public CreatePublisherResponse createPublisher(CreatePublisherRequest request){

        if(profileRepository.existsByEmail(request.email())){
            throw new EmailAlreadyExistsException("Email deja utilise");
        }

        Publisher publisher = new Publisher(request.email(), request.firstName(),
                request.lastName(), request.niche(), request.bio(), request.country()
        );

        Publisher savedPublisher = publisherRepository.save(publisher);

        CreatePublisherResponse response = toResponse(savedPublisher);

        return response;
    }

    public List<CreatePublisherResponse> findAll(){

        List<Publisher> publishers = publisherRepository.findAll();

        List<CreatePublisherResponse> publisherResponses = new ArrayList<>();

        for(Publisher publisher : publishers){

            CreatePublisherResponse publisherResponse = toResponse(publisher);
            publisherResponses.add(publisherResponse);
        }
        return publisherResponses;
    }
}
