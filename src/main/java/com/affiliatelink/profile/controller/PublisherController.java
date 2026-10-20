package com.affiliatelink.profile.controller;

import com.affiliatelink.profile.dto.CreatePublisherRequest;
import com.affiliatelink.profile.dto.CreatePublisherResponse;
import com.affiliatelink.profile.entity.Publisher;
import com.affiliatelink.profile.service.PublisherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PublisherController {

    private final PublisherService publisherService;

    public PublisherController(PublisherService publisherService){
        this.publisherService = publisherService;
    }

    @PostMapping("/api/publishers")
    public ResponseEntity<CreatePublisherResponse> createPublisher(@RequestBody @Valid CreatePublisherRequest request){

        CreatePublisherResponse response = publisherService.createPublisher(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/api/publishers")
    public ResponseEntity<List<CreatePublisherResponse>> findAll(){

        List<CreatePublisherResponse> publishers = publisherService.findAll();

        return ResponseEntity.ok(publishers);
    }
}
