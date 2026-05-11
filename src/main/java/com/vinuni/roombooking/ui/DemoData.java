package com.vinuni.roombooking.ui;

import com.vinuni.roombooking.enums.AccessLevel;
import com.vinuni.roombooking.model.Admin;
import com.vinuni.roombooking.model.Room;
import com.vinuni.roombooking.model.Staff;
import com.vinuni.roombooking.model.Student;
import com.vinuni.roombooking.model.User;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * FRONTEND-ONLY placeholder source of users and rooms.
 *
 * The backend does not yet expose a UserRepository or RoomRepository, so the
 * UI reads these in-memory lists to render the Login, Rooms, and Admin pages.
 * When the data layer lands, replace lookupUser() / getRooms() with real
 * repository calls and delete this class.
 */
@Component
public class DemoData {

    private final List<User> users = List.of(
            new Student("u-s1", "alice", "pass", "alice@vinuni.edu.vn",
                    "S2024001", "Computer Science", 3),
            new Staff("u-st1", "bob", "pass", "bob@vinuni.edu.vn",
                    "ST101", "IT Department"),
            new Admin("u-a1", "carol", "pass", "carol@vinuni.edu.vn", "A001")
    );

    private final List<Room> rooms = List.of(
            new Room(101, "Library Study Room A", 4, AccessLevel.STUDENT_ONLY),
            new Room(102, "Library Study Room B", 6, AccessLevel.STUDENT_ONLY),
            new Room(201, "Meeting Room 2F", 12, AccessLevel.STAFF_ONLY),
            new Room(301, "Conference Hall", 40, AccessLevel.ALL_USERS),
            new Room(302, "Innovation Lab", 20, AccessLevel.ALL_USERS)
    );

    public List<User> getUsers() {
        return users;
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public User lookupUser(String userName) {
        return users.stream()
                .filter(u -> u.getUserName().equalsIgnoreCase(userName))
                .findFirst()
                .orElse(null);
    }

    public Room lookupRoom(int roomId) {
        return rooms.stream()
                .filter(r -> r.getRoomId() == roomId)
                .findFirst()
                .orElse(null);
    }
}
