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
import java.sql.SQLException;

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

    /**
     * Testing method insertUser() in DatabaseConnector.java
     * Result: Insert user in database successfully
     */
    // @Test
    // public void testInsertUser(){
    //     databaseConnector.insertUser(testStudent);
    //     databaseConnector.insertUser(testStaff);
    //     databaseConnector.insertUser(testAdmin);

    //     System.out.println("Users inserted successfully");
    // }

    /**
     * Testing method findUserByName()
     * Result: Successful
     */
    // @Test
    // public void testFindUserByName(){
    //     String name = "Test Student";
    //     String name2 = "Test Student 2";

    //     try{
    //         ResultSet rs = databaseConnector.findUserByName(name);
    //         ResultSet rs2 = databaseConnector.findUserByName(name2);

    //         // Print the result of the first name
    //         if(rs.next()){
    //             System.out.println("User found, user has id: " + rs.getString("user_id"));
    //         }
    //         else{System.out.println("No user found");}

    //         // Print the result 
    //         if(rs2.next()){
    //             System.out.println("User found, user has id: " + rs2.getString("user_id"));
    //         }
    //         else{System.out.println("No user found");}
    //     }
    //     catch(SQLException e){
    //         throw new IllegalStateException("Cannot make query: " + e);
    //     }
        

    // }

    /**
     * Testing method findUserAuthByName() from DatabaseConnector.java
     * Result: Successful
     */
    // @Test
    // public void testFindUserAuthByName(){
    //     String name = "Test Staff";
    //     String name2 = "Test Staff 2";

    //     try{
    //         ResultSet rs = databaseConnector.findUserAuthByName(name);
    //         ResultSet rs2 = databaseConnector.findUserAuthByName(name2);

    //         //Print the output of the first name
    //         if(rs.next()){
    //             System.out.println("User found, user has password: " + rs.getString("user_password"));
    //         }
    //         else{System.out.println("User not found");}

    //         //Print the output of the second name
    //         if(rs2.next()){
    //             System.out.println("User found, user has password: " + rs2.getString("user_password"));   
    //         }
    //         else{System.out.println("User not found");}
    //     }
    //     catch(SQLException e){
    //         throw new IllegalStateException("Cannot make query: " + e);
    //     }
    // }
    
    /**
     * Testing findAllRooms() from DatabaseConnector.java
     * Result: Successful
     */
    @Test
    public void testFindAllRooms(){
        try{
            ResultSet rs = databaseConnector.findAllRooms();
            while(rs.next()){
                System.out.println("---------------------------------------------------------------");
                System.out.println("This is room " + rs.getInt("room_id"));
                System.out.println("Room " + rs.getInt("room_id") + " can store " + rs.getInt("capacity") + " people");
                System.out.println("This room is currently " + rs.getString("room_status"));
                if(rs.getString("access_level").equals("STUDENT_ONLY")){
                    System.out.println("Only student can book this room");
                }
                else if(rs.getString("access_level").equals("STAFF_ONLY")){
                    System.out.println("Only staff can book this room");
                }
                else if(rs.getString("access_level").equals("ALL_USERS")){
                    System.out.println("Both student and staff can book this room");
                }
                System.out.println("--------------------------------------------------------------");
            }
        }
        catch(SQLException e){
            throw new IllegalStateException("Cannot make query: " + e);
        }
    }
    
}
