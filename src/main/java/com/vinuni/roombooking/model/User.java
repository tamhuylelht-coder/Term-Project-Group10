package com.vinuni.roombooking.model;

import java.util.ArrayList;
import java.util.List;

public abstract class User {

    protected String userId;
    protected String userName;
    private   String password;
    protected String email;

    public User(String userId, String userName, String password, String email) {
        this.userId   = userId;
        this.userName = userName;
        this.password = password;
        this.email    = email;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public abstract String getUserType();

    /**
     * STUB — Phase 2: hash pwd and compare against stored hash.
     */
    public boolean authenticate(String pwd) {
        // TODO (Huy Dung): return BCrypt.checkpw(pwd, this.password);
        return true;
    }

    /**
     * STUB — Phase 2: delegate to BookingRepository.findByUser(userId).
     */
    public List<BookingRequest> viewMyBookings() {
        // TODO (Khanh An): wire to BookingRepository
        return new ArrayList<>();
    }
}
