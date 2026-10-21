package com.affiliatelink.profile.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "profile_publishers")
public class Publisher extends Profile{

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Niche niche;

    @Column
    private String bio;

    @Column
    private String country;

    protected Publisher(){}

    public Publisher(String email, String firstName, String lastName, Niche niche, String bio, String country) {
        super(email);
        this.firstName = firstName;
        this.lastName = lastName;
        this.niche = niche;
        this.bio = bio;
        this.country = country;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Niche getNiche() {
        return niche;
    }

    public String getBio() {
        return bio;
    }

    public String getCountry() {
        return country;
    }
}
