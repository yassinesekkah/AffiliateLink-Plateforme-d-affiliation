package com.affiliatelink.profile.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="profile_profiles")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    // Tout nouveau compte est créé avec le statut PENDING.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status = AccountStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Profile(){
    }

    protected Profile(String email){
        this.email = email;
    }

    public Long getId(){
        return id;
    }

    public String getEmail(){
        return this.email;
    }

    public AccountStatus getStatus(){
        return this.status;
    }

    public LocalDateTime getCreatedAt(){
        return this.createdAt;
    }

    public void changeStatus(AccountStatus newStatus){

        if(this.status == AccountStatus.PENDING && newStatus == AccountStatus.ACTIVE){
            this.status = newStatus;
        }

        else if(this.status == AccountStatus.ACTIVE && newStatus == AccountStatus.SUSPENDED){
            this.status = newStatus;
        }

        else if(this.status == AccountStatus.SUSPENDED && newStatus == AccountStatus.ACTIVE){
            this.status = newStatus;
        }

        else{
            throw new IllegalStateException(
                    "Transition de statut non autorisée"
            );
        }
    }
}
