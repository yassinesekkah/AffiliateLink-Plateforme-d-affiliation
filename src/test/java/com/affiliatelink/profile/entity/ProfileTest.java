package com.affiliatelink.profile.entity;

import com.affiliatelink.common.exception.InvalidStatusTransitionException;
import com.affiliatelink.profile.Niche;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class ProfileTest {

    @Test
    void newProfileShouldHaveStatusPending(){

        //Given/When create new Advertiser
        Advertiser advertiser = new Advertiser(
                "advertiserTest@test.com",
                "testCompany"
        );

        //Then verification de status initial
        assertEquals(AccountStatus.PENDING, advertiser.getStatus());
    }

    @Test
    void newPublisherShouldHaveStatusPending(){

        //given
        Publisher publisher = new Publisher(
                "testEmail@gmail.com",
            "firstName",
                "lastNam",
                Niche.FOOD,
                "bio",
                "country"
        );

        //then
        assertEquals(AccountStatus.PENDING, publisher.getStatus());
    }

    @ParameterizedTest
    @CsvSource(
            {
                    "PENDING, ACTIVE",
                    "ACTIVE, SUSPENDED",
                    "SUSPENDED, ACTIVE"
            }
    )
    void shouldAllowValidStatusTransition(AccountStatus initialStatus, AccountStatus newStatus){
        //given
        Publisher publisher = new Publisher(
                "testEmail@gmail.com",
                "firstName",
                "lastNam",
                Niche.FOOD,
                "bio",
                "country"
        );

        if (initialStatus == AccountStatus.ACTIVE
                || initialStatus == AccountStatus.SUSPENDED) {
            publisher.changeStatus(AccountStatus.ACTIVE);
        }

        if (initialStatus == AccountStatus.SUSPENDED) {
            publisher.changeStatus(AccountStatus.SUSPENDED);
        }

        // When
        publisher.changeStatus(newStatus);

        // Then
        assertEquals(newStatus, publisher.getStatus());

    }

    @ParameterizedTest
    @CsvSource({
            "PENDING, SUSPENDED",
            "PENDING, PENDING",
            "ACTIVE, PENDING",
            "ACTIVE, ACTIVE",
            "SUSPENDED, PENDING",
            "SUSPENDED, SUSPENDED"
    })
    void shouldRejectInvalidStatusTransition(
            AccountStatus initialStatus,
            AccountStatus newStatus
    ) {
        //given
        Publisher publisher = new Publisher(
                "email@email.com",
                "ismail",
                "sekkah",
                Niche.GAMING,
                "bio",
                "maroc"
        );

        if (initialStatus == AccountStatus.ACTIVE
                || initialStatus == AccountStatus.SUSPENDED) {
            publisher.changeStatus(AccountStatus.ACTIVE);
        }

        if (initialStatus == AccountStatus.SUSPENDED) {
            publisher.changeStatus(AccountStatus.SUSPENDED);
        }

        // When / Then
        assertThrows(
                InvalidStatusTransitionException.class,
                () -> publisher.changeStatus(newStatus)
        );

        assertEquals(initialStatus, publisher.getStatus());
    }

}
