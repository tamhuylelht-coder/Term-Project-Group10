package com.vinuni.roombooking.service;

import com.vinuni.roombooking.model.BookingRequest;
import com.vinuni.roombooking.model.Room;
import com.vinuni.roombooking.model.User;
import com.vinuni.roombooking.enums.RoomStatus;

import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.stereotype.Service;
import java.sql.*;



/**
 * Thin wrapper around the MySQL JDBC connection.
 * Phase 2 owner: Huy Tam — implement all methods by May 15.
 * BookingService will swap its in-memory calls for these on integration day.
 */
@Service
public class DatabaseConnector {

    private Connection connection;

    @Value("${spring.datasource.username}")
    private String userName;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.url}")
    private String     connectionUrl;


    /**
     * STUB — Phase 2 (Huy Tam): open JDBC connection using connectionUrl.
     */

    /**
     * Code referenced from stackoverflow: https://stackoverflow.com/questions/2839321/connect-java-to-a-mysql-database
     * and geeksforgeeks: https://www.geeksforgeeks.org/java/java-database-connectivity-with-mysql/
     */
    public void connect() {
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(connectionUrl, userName, password);

        }
        catch (SQLException e){
            throw new IllegalStateException("Cannot connect to the database!" + e);
        }
        catch (ClassNotFoundException e){
            throw new IllegalStateException("Cannot find the driver in the classpath!" + e);
        }
        
    }

    /**
     * STUB — Phase 2 (Huy Tam): SELECT * FROM bookings WHERE room_id = roomId.
     * Returns null until implemented.
     */

    public void insertRoom(Room room){
        int roomId = room.getRoomId();
        String roomName = room.getRoomName();
        int capacity = room.getCapacity();
        String access = room.getAccess().toString();
        String status = room.getStatus().toString();

        try{
            PreparedStatement ps = connection.prepareStatement("INSERT INTO rooms (room_id, room_name, capacity, access_level, room_status) VALUES (?, ?, ?, ?,?)");
            ps.setInt(1, roomId);
            ps.setString(2, roomName);
            ps.setInt(3, capacity);
            ps.setString(4, access);
            ps.setString(5, status);
            ps.executeUpdate();
        }
        catch(SQLException e){
            throw new IllegalStateException("Cannot make query: " + e);
        }
        
    }

    public ResultSet fetchRoomSchedules(int roomId) {
        try{
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM bookings WHERE room_id = ?");
            ps.setInt(1, roomId);
            return ps.executeQuery();
        }
        catch (SQLException e){
            throw new IllegalStateException("Cannot query to fetch rooms" + e);
        }
        
    }


    /**
     * Possible new method to add new user to the database
     */
    public void insertUser(User user){
        String userId = user.getUserId();
        String userName = user.getUserName();
    }


    /**
     * STUB — Phase 2 (Huy Tam): INSERT booking into bookings table.
     */
    public void insertBooking(BookingRequest req) {
        // TODO (Huy Tam): INSERT INTO bookings (booking_id, user_id, room_id, start_time, end_time, status)
        // Extracting information from the booking request
        String bookingId = req.getBookingId();
        String userId = req.getUser().getUserId();
        int roomId = req.getRoom().getRoomId();
        Timestamp startTime = Timestamp.valueOf(req.getTimeSlot().getStartTime());
        Timestamp endTime = Timestamp.valueOf(req.getTimeSlot().getEndTime());
        String status = req.getStatus().toString();
        Timestamp createdAt = Timestamp.valueOf(req.getCreatedAt());

        try{
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO bookings (booking_id, user_id, room_id, start_time, end_time, booking_status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setString(1, bookingId);
            ps.setString(2, userId);
            ps.setInt(3, roomId);
            ps.setTimestamp(4, startTime);
            ps.setTimestamp(5, endTime);
            ps.setString(6, status);
            ps.setTimestamp(7, createdAt);
            ps.executeUpdate();
        }
        catch(SQLException e){
            throw new IllegalStateException("Cannot make query: " + e);
        }
    }

    public void updateBookingStatus(BookingRequest req){
        String bookingId = req.getBookingId();
        String newStatus = req.getStatus().toString();

        try{
            PreparedStatement ps = connection.prepareStatement("UPDATE bookings SET booking_status = ? WHERE booking_id = ?");
            ps.setString(1, newStatus);
            ps.setString(2, bookingId);

            ps.executeUpdate();
        }
        catch(SQLException e){
            throw new IllegalStateException("Cannot make query: " + e);
        }
    }

    /**
     * STUB — Phase 2 (Huy Tam): UPDATE rooms SET status = ? WHERE room_id = ?.
     */
    public void updateRoomStatus(int roomId, RoomStatus status) {
        // TODO (Huy Tam): UPDATE rooms SET status = status.name() WHERE room_id = roomId

        try{
            PreparedStatement ps = connection.prepareStatement("UPDATE rooms SET room_status = ? WHERE room_id = ?");
            ps.setString(1, status.name());
            ps.setInt(2, roomId);
            ps.executeUpdate();
        }
        catch(SQLException e){
            throw new IllegalStateException("Cannot make query: " + e);
        }
    }

    /**
     * STUB — Phase 2 (Huy Tam): close connection gracefully.
     */
    public void close() {
        try{
            if (connection != null && !connection.isClosed()){
                connection.close();
            } 
        }
        catch(SQLException e){
            throw new IllegalStateException("Cannot close the connection" + e);
        }
        
    }
}
