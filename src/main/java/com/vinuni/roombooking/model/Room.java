    package com.vinuni.roombooking.model;

import com.vinuni.roombooking.enums.AccessLevel;
import com.vinuni.roombooking.enums.RoomStatus;

public class Room {

    protected int         roomId;
    protected String      roomName;
    protected int         capacity;
    protected AccessLevel access;
    public    RoomStatus  status;

    public Room(int roomId, String roomName, int capacity, AccessLevel access) {
        this.roomId   = roomId;
        this.roomName = roomName;
        this.capacity = capacity;
        this.access   = access;
        this.status   = RoomStatus.AVAILABLE;
    }

    public int getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public int getCapacity() {
        return capacity;
    }

    public AccessLevel getAccess() {
        return access;
    }

    public RoomStatus getStatus(){
        return status;
    }

    /**
     * STUB — Phase 2: check status == AVAILABLE (and optionally query DB for live occupancy).
     */
    public boolean isAvailable() {
        // TODO (Huy Dung): return status == RoomStatus.AVAILABLE;
        return true;
    }
}
