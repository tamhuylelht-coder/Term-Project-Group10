package com.vinuni.roombooking.model;
import com.vinuni.roombooking.enums.InvitationStatus;

import java.time.LocalDateTime;

public class Invitation {
    private final BookingRequest booking;
    private final User invitee;
    private final InvitationStatus status;
    private final LocalDateTime invitedAt;

    public Invitation(BookingRequest booking, User invitee, InvitationStatus status, LocalDateTime invitedAt){
        this.booking = booking;
        this.invitee = invitee;
        this.status = status;
        this.invitedAt = invitedAt;
    }

    public BookingRequest getBookingRequest(){return booking;}

    public User getUser(){return invitee;}

    public InvitationStatus getStatus(){return status;}

    public LocalDateTime getInvitedAt(){return invitedAt;}

}
