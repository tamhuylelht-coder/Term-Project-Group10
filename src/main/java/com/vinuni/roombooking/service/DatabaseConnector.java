package com.vinuni.roombooking.service;

import com.vinuni.roombooking.model.BookingRequest;
import com.vinuni.roombooking.enums.RoomStatus;
import java.sql.Connection;
import java.sql.ResultSet;

/**
 * Thin wrapper around the MySQL JDBC connection.
 * Phase 2 owner: Huy Tam — implement all methods by May 15.
 * BookingService will swap its in-memory calls for these on integration day.
 */
public class DatabaseConnector {

    private Connection connection;
    private String     connectionUrl;

    public DatabaseConnector(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    /**
     * STUB — Phase 2 (Huy Tam): open JDBC connection using connectionUrl.
     */
    public void connect() {
        // TODO (Huy Tam): Class.forName("com.mysql.cj.jdbc.Driver");
        //   connection = DriverManager.getConnection(connectionUrl, user, pass);
    }

    /**
     * STUB — Phase 2 (Huy Tam): SELECT * FROM bookings WHERE room_id = roomId.
     * Returns null until implemented.
     */
    public ResultSet fetchRoomSchedules(int roomId) {
        // TODO (Huy Tam): PreparedStatement ps = connection.prepareStatement(
        //   "SELECT * FROM bookings WHERE room_id = ?");
        //   ps.setInt(1, roomId); return ps.executeQuery();
        return null;
    }

    /**
     * STUB — Phase 2 (Huy Tam): INSERT booking into bookings table.
     */
    public void insertBooking(BookingRequest req) {
        // TODO (Huy Tam): INSERT INTO bookings (booking_id, user_id, room_id, start_time, end_time, status)
    }

    /**
     * STUB — Phase 2 (Huy Tam): UPDATE rooms SET status = ? WHERE room_id = ?.
     */
    public void updateRoomStatus(int roomId, RoomStatus status) {
        // TODO (Huy Tam): UPDATE rooms SET status = status.name() WHERE room_id = roomId
    }

    /**
     * STUB — Phase 2 (Huy Tam): close connection gracefully.
     */
    public void close() {
        // TODO (Huy Tam): if (connection != null && !connection.isClosed()) connection.close();
    }
}
