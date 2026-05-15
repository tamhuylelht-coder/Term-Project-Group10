package com.vinuni.roombooking.service;
import com.vinuni.roombooking.enums.AccessLevel;
import com.vinuni.roombooking.enums.BookingStatus;
import com.vinuni.roombooking.model.BookingRequest;
import com.vinuni.roombooking.model.Room;
import com.vinuni.roombooking.model.TimeSlot;
import com.vinuni.roombooking.model.User;
import com.vinuni.roombooking.model.Staff;
import com.vinuni.roombooking.model.Student;
import com.vinuni.roombooking.model.Admin;
import java.sql.ResultSet;
import com.vinuni.roombooking.repository.BookingRepository;
import com.vinuni.roombooking.validator.BookingValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@SpringBootTest
public class DatabaseConnectorTest {
    
    // private BookingService bookingService;

    @Autowired
    private DatabaseConnector databaseConnector;

    // Connect to database for testing
    @BeforeEach
    public void setUp(){
        databaseConnector.connect();
        System.out.println("Connected to database");
    }



    /**
     * Create test users to add to database for testing
     */
    private User testStudent = new Student("U001", "Test Student", "123", "S001",
        "student1@gmail.com", "Business", 3
    );
    // Test Staff
    private User testStaff = new Staff("U002", "Test Staff", "321",
        "staff1@gmail.com", "St001", "Mathematics"
    );
    // Test Admin
    private User testAdmin = new Admin("U003", "Test Staff", "456",
        "admin1@gmail.com", "A001"
    );
    

    /**
     * Create test rooms and add to database for testing
     * insertRoom() and fetchRoomSchedules()
     */
    private Room testRoom1 = new Room(1, "Room 1", 10, AccessLevel.STUDENT_ONLY);
    private Room testRoom2 = new Room(2, "Room 2", 20, AccessLevel.STAFF_ONLY);
    private Room testRoom3 = new Room(3, "Room 3", 30, AccessLevel.ALL_USERS);

    
    

    /**
     * Testing method insertRoom() in DatabaseConnector.java
     * Result: Run succesfully
     */
    // @Test
    // public void testInsertRoom(){
    //     databaseConnector.insertRoom(testRoom1);
    //     databaseConnector.insertRoom(testRoom2);
    //     databaseConnector.insertRoom(testRoom3);
    //     System.out.println("Rooms inserted successfully");
    // }


    /**
     * Testing method fetchRoomSchedules() from DatabaseConnector.java
     * Result: Run succesfully
     */
    // @Test
    // public void testFetchRooms(){
    //     ResultSet result1 = databaseConnector.fetchRoomSchedules(1);
    //     ResultSet result2 = databaseConnector.fetchRoomSchedules(2);
    //     ResultSet result3 = databaseConnector.fetchRoomSchedules(3);

    //     System.out.println("Fetch schedules for room succesfully");
    //     System.out.println(result1);
    //     System.out.println(result2);
    //     System.out.println(result3);
    // }
    
    
}
