package com.vinuni.roombooking.model;

import java.time.LocalDateTime;

import com.vinuni.roombooking.enums.InvitationStatus;

/**
 * One row of the invitations table — joined back with the host's booking and
 * the invitee's user record. Carried around as a value object; persistence is
 * handled by DatabaseConnector.
 */
public class Invitation {

    private final BookingRequest booking;
    private final User           invitee;
    private final InvitationStatus status;
    private final LocalDateTime  invitedAt;

    public Invitation(BookingRequest booking, User invitee,
                      InvitationStatus status, LocalDateTime invitedAt) {
        this.booking   = booking;
        this.invitee   = invitee;
        this.status    = status;
        this.invitedAt = invitedAt;
    }

    public BookingRequest    getBooking()   { return booking; }
    public User              getInvitee()   { return invitee; }
    public InvitationStatus  getStatus()    { return status; }
    public LocalDateTime     getInvitedAt() { return invitedAt; }
}
