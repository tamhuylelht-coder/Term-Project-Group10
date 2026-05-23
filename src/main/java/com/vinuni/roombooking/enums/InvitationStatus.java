package com.vinuni.roombooking.enums;

/**
 * Response state of an invitation in the invitations table. Mirrors Outlook
 * meeting-response semantics:
 *   PENDING  - host invited the user; they haven't clicked Accept or Decline yet.
 *   ACCEPTED - user clicked Accept; they're attending.
 *   DECLINED - user clicked Decline; they aren't attending. Row is kept so the
 *              host can see they were asked and said no.
 */
public enum InvitationStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}
