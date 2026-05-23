package com.vinuni.roombooking.service;

import com.vinuni.roombooking.enums.AccessLevel;
import com.vinuni.roombooking.model.Admin;
import com.vinuni.roombooking.model.Room;
import com.vinuni.roombooking.model.Staff;
import com.vinuni.roombooking.model.Student;
import com.vinuni.roombooking.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RoomApprovalPolicyTest {

    private RoomApprovalPolicy policy;
    private User student;
    private User staff;
    private User admin;

    @BeforeEach
    void setUp() {
        policy = new RoomApprovalPolicy();
        student = new Student("s1", "alice", "pass", "alice@example.com", "S1", "CS", 2);
        staff = new Staff("st1", "bob", "pass", "bob@example.com", "ST1", "Library");
        admin = new Admin("a1", "carol", "pass", "carol@example.com", "A1");
    }

    @Test
    void classify_OpenRooms_AutoApproveAllUsers() {
        assertOpenForAll(new Room(1, "A102-Group-Discussion-Room", 8, AccessLevel.ALL_USERS));
        assertOpenForAll(new Room(2, "A218-Prayer-Room", 4, AccessLevel.ALL_USERS));
        assertOpenForAll(new Room(3, "A203-One-Button-Studio", 6, AccessLevel.ALL_USERS));
        assertOpenForAll(new Room(4, "A122-Brainstorm-Space-Meeting-Room", 10, AccessLevel.ALL_USERS));
        assertOpenForAll(new Room(5, "A123-Synergy-Station-Meeting-Room", 6, AccessLevel.ALL_USERS));
        assertOpenForAll(new Room(6, "A124-Focus-Corner-Meeting-Room", 6, AccessLevel.ALL_USERS));
        assertOpenForAll(new Room(7, "A128-Design-Thinking-Meeting-Room", 20, AccessLevel.ALL_USERS));
    }

    @Test
    void classify_MeetingRooms_AutoApproveStaffAndAdminOnly() {
        Room room = new Room(8, "D210-Meeting-Room", 8, AccessLevel.STAFF_ONLY);

        assertEquals(RoomApprovalPolicy.Category.MEETING_ROOM, policy.classify(room));
        assertFalse(policy.canAutoApprove(room, student));
        assertTrue(policy.canAutoApprove(room, staff));
        assertTrue(policy.canAutoApprove(room, admin));
    }

    @Test
    void classify_Classrooms_AutoApproveAdminOnly() {
        Room room = new Room(9, "C101-TBL-Classroom", 42, AccessLevel.ALL_USERS);

        assertEquals(RoomApprovalPolicy.Category.CLASSROOM, policy.classify(room));
        assertFalse(policy.canAutoApprove(room, student));
        assertFalse(policy.canAutoApprove(room, staff));
        assertTrue(policy.canAutoApprove(room, admin));
    }

    @Test
    void classify_OtherRooms_NonAdminsNeedApproval() {
        Room room = new Room(10, "A-ELab-Co-working-Space", 50, AccessLevel.ALL_USERS);

        assertEquals(RoomApprovalPolicy.Category.OTHER, policy.classify(room));
        assertFalse(policy.canAutoApprove(room, student));
        assertFalse(policy.canAutoApprove(room, staff));
        assertFalse(policy.canAutoApprove(room, admin));
    }

    private void assertOpenForAll(Room room) {
        assertEquals(RoomApprovalPolicy.Category.OPEN, policy.classify(room));
        assertTrue(policy.canAutoApprove(room, student));
        assertTrue(policy.canAutoApprove(room, staff));
        assertTrue(policy.canAutoApprove(room, admin));
    }
}
