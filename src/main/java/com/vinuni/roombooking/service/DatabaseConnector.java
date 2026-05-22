package com.vinuni.roombooking.service;

import com.vinuni.roombooking.model.BookingRequest;
import com.vinuni.roombooking.model.Room;
import com.vinuni.roombooking.model.User;
import com.vinuni.roombooking.model.Student;
import com.vinuni.roombooking.model.Staff;
import com.vinuni.roombooking.model.Admin;
import com.vinuni.roombooking.model.TimeSlot;
import com.vinuni.roombooking.enums.RoomStatus;
import com.vinuni.roombooking.enums.AccessLevel;
import com.vinuni.roombooking.enums.BookingStatus;
import com.vinuni.roombooking.repository.BookingRepository;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.sql.*;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;


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

    /** Used to wire Admin's deps when reconstructing one from a DB row. */
    @Autowired
    private BookingRepository bookingRepository;

    /** Hash user passwords before storing so SecurityConfig's BCrypt check succeeds at login. */
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Code referenced from stackoverflow: https://stackoverflow.com/questions/2839321/connect-java-to-a-mysql-database
     * and geeksforgeeks: https://www.geeksforgeeks.org/java/java-database-connectivity-with-mysql/
     */
    @PostConstruct
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
        String access = room.getAccess().name();
        String status = room.getStatus().name();

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
     * Persist a new User. The raw password from the User object is BCrypt-hashed
     * before storage so that SecurityConfig's BCrypt-based AuthenticationManager
     * can match it on login.
     */
    public void insertUser(User user){
        String userId = user.getUserId();
        String userName = user.getUserName();
        String password = passwordEncoder.encode(user.getPassword());
        String email = user.getEmail();
        if(user instanceof Student){
            String role = "STUDENT";
            String studentId = ((Student)user).getStudentId();
            String major = ((Student)user).getMajor();
            int yearOfStudy = ((Student)user).getYearOfStudy();
            try{
                PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (user_id, user_name, user_password, user_email, user_role, student_id, student_major, year_of_study) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                );
                ps.setString(1, userId);
                ps.setString(2, userName);
                ps.setString(3, password);
                ps.setString(4, email);
                ps.setString(5, role);
                ps.setString(6, studentId);
                ps.setString(7, major);
                ps.setInt(8, yearOfStudy);
                ps.executeUpdate();
            }
            catch(SQLException e){
                throw new IllegalStateException("Cannot make query:" + e);
            }
        }
        else if(user instanceof Staff){
            String role = "STAFF";
            String staffId = ((Staff)user).getStaffId();
            String department = ((Staff)user).getDepartment();
            try{
                PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (user_id, user_name, user_password, user_email, user_role, staff_id, staff_department) VALUES (?, ?, ?, ?, ?, ?, ?)"
                );
                ps.setString(1, userId);
                ps.setString(2, userName);
                ps.setString(3, password);
                ps.setString(4, email);
                ps.setString(5, role);
                ps.setString(6, staffId);
                ps.setString(7, department);
                ps.executeUpdate();
            }
            catch(SQLException e){
                throw new IllegalStateException("Cannot make query:" + e);
            }
        }
        else if(user instanceof Admin){
            String role = "ADMIN";
            String adminId = ((Admin)user).getAdminId();
            try{
                PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (user_id, user_name, user_password, user_email, user_role, admin_id) VALUES (?, ?, ?, ?, ?, ?)"
                );
                ps.setString(1, userId);
                ps.setString(2, userName);
                ps.setString(3, password);
                ps.setString(4, email);
                ps.setString(5, role);
                ps.setString(6, adminId);
                ps.executeUpdate();
            }
            catch(SQLException e){
                throw new IllegalStateException("Cannot make query:" + e);
            }
        }
        
    }

    public User findUserByName(String name){
        try{
            PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM users WHERE user_name = ?");
            ps.setString(1,name);
            ResultSet rs =  ps.executeQuery();

            if(!rs.next()){
                return null;
            }

            String userId = rs.getString("user_id");
            String userName = rs.getString("user_name");
            String password = rs.getString("user_password");
            String email = rs.getString("user_email");
            String role = rs.getString("user_role");
            if(role.equals("STUDENT")){
                String studentId = rs.getString("student_id");
                String major = rs.getString("student_major");
                int yearOfStudy = rs.getInt("year_of_study");

                return new Student(userId, userName, password, email, studentId, major, yearOfStudy);
            }
            else if(role.equals("STAFF")){
                String staffId = rs.getString("staff_id");
                String department=  rs.getString("staff_department");

                return new Staff(userId, userName, password, email, staffId, department);
            }
            else if(role.equals("ADMIN")){
                String adminId = rs.getString("admin_id");
                Admin admin = new Admin(userId, userName, password, email, adminId);
                // Wire deps so AdminView's forceCancel / overrideRequest work.
                admin.setDatabaseConnector(this);
                admin.setBookingRepository(bookingRepository);
                return admin;
            }
            else{return null;}
        }
        catch(SQLException e){
            throw new IllegalStateException("Cannot make query: "+ e);
        }
    }


    public User findUserById(String userId){
        try{
            PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM users WHERE user_id = ?");
            ps.setString(1,userId);
            ResultSet rs =  ps.executeQuery();

            if(!rs.next()){
                return null;
            }

            String userName = rs.getString("user_name");
            String password = rs.getString("user_password");
            String email = rs.getString("user_email");
            String role = rs.getString("user_role");
            if(role.equals("STUDENT")){
                String studentId = rs.getString("student_id");
                String major = rs.getString("student_major");
                int yearOfStudy = rs.getInt("year_of_study");

                return new Student(userId, userName, password, email, studentId, major, yearOfStudy);
            }
            else if(role.equals("STAFF")){
                String staffId = rs.getString("staff_id");
                String department=  rs.getString("staff_department");

                return new Staff(userId, userName, password, email, staffId, department);
            }
            else if(role.equals("ADMIN")){
                String adminId = rs.getString("admin_id");
                Admin admin = new Admin(userId, userName, password, email, adminId);
                // Wire deps so AdminView's forceCancel / overrideRequest work.
                admin.setDatabaseConnector(this);
                admin.setBookingRepository(bookingRepository);
                return admin;
            }
            else{return null;}
        }
        catch(SQLException e){
            throw new IllegalStateException("Cannot make query: "+ e);
        }
    }


    public List<User> searchUsers(String userName, String excludeUserId, int limit){
        try{
            String query = """
                        SELECT * FROM users
                        WHERE user_name LIKE ? AND user_id <> ?
                        ORDER BY user_name LIMIT ?
                        """;
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, userName);
            ps.setString(2, excludeUserId);
            ps.setInt(3, limit);

            ResultSet rs = ps.executeQuery();
            if(!rs.next()){return null;}

            List<User> userList = new ArrayList<>();
            while(rs.next()){
                String userId = rs.getString("user_id");
                User user = findUserById(userId);
                userList.add(user);
            }
            return userList;
        }
        catch (SQLException e){
            throw new IllegalStateException("Cannot make query: " + e);
        }
    }

    public List<String> findUserAuthByName(String name){
        try{
            PreparedStatement ps = connection.prepareStatement(
                "SELECT user_name, user_password, user_role FROM users WHERE user_name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();

            if(!rs.next()){return null;}

            String userName = rs.getString("user_name");
            String password = rs.getString("user_password");
            String role = rs.getString("user_role");
            return new ArrayList<>(List.of(userName, password, role));
        }
        catch(SQLException e){
            throw new IllegalStateException("Cannot make query: " + e);
        }
    }

    public List<Room> findAllRooms(){
        try{
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM rooms");
            ResultSet rs = ps.executeQuery();

            List<Room> roomList = new ArrayList<>();
            while(rs.next()){
                int roomId = rs.getInt("room_id");
                String roomName = rs.getString("room_name");
                int capacity = rs.getInt("capacity");
                AccessLevel access = AccessLevel.valueOf(rs.getString("access_level"));
                RoomStatus status = RoomStatus.valueOf(rs.getString("room_status"));

                Room returnRoom = new Room(roomId, roomName, capacity, access);
                returnRoom.setStatus(status);

                roomList.add(returnRoom);
            }
            return roomList;
        }
        catch(SQLException e){
            throw new IllegalStateException("Cannot make query: " + e);
        }
    }

    public Room findRoomById(int roomId){
        try{
            PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM rooms WHERE room_id = ?");
            ps.setInt(1, roomId);
            ResultSet rs = ps.executeQuery();

            if(!rs.next()){return null;}

            String roomName = rs.getString("room_name");
            int capacity = rs.getInt("capacity");
            AccessLevel access = AccessLevel.valueOf(rs.getString("access_level"));
            RoomStatus status = RoomStatus.valueOf(rs.getString("room_status"));
            
            Room returnRoom = new Room(roomId, roomName, capacity, access);
            returnRoom.setStatus(status);
            return returnRoom;
        }
        catch(SQLException e){
            throw new IllegalStateException("Cannot make query: " + e);
        }
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
        String status = req.getStatus().name();
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
        String newStatus = req.getStatus().name();

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
     * Methods for managing RSVP
     */
    public void insertRsvp(){

    }

    /**
     * Fetch RsvpList(bookingId) method: return HashSet
     * of user name in the rsvp list
     * @param bookingId
     * @return
     */
    public HashSet<String> fetchRsvpList(String bookingId){
        try{
            PreparedStatement ps = connection.prepareStatement("SELECT user_id FROM rsvp WHERE booking_id = ?");
            ps.setString(1, bookingId);
            ResultSet rs = ps.executeQuery();
            HashSet<String> rsvpList = new HashSet<>();

            while(rs.next()){
                String userId = rs.getString("user_id");
                rsvpList.add(userId);
            }

            return rsvpList;
        }
        catch(SQLException e){
            throw new IllegalStateException("Cannot make query: " + e);
        }

    }


    public BookingRequest findBookingById(String bookingId){
        try{
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM bookings where booking_id = ?");
            ps.setString(1, bookingId);
            ResultSet rs = ps.executeQuery();

            if(!rs.next()){return null;} // No booking found

            String userId = rs.getString("user_id");
            int roomId = rs.getInt("room_id");
            LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
            LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();
            TimeSlot timeSlot = new TimeSlot(startTime, endTime);
            BookingStatus bookingStatus = BookingStatus.valueOf(rs.getString("booking_status"));
            LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();

            User hostUser = findUserById(userId);
            Room roomBooked = findRoomById(roomId);
            HashSet<String> rsvplist = fetchRsvpList(bookingId);

            BookingRequest req = new BookingRequest(bookingId, hostUser, roomBooked, timeSlot, rsvplist, bookingStatus, createdAt);
            return req;
        }
        catch(SQLException e){
            throw new IllegalStateException("Cannot make query: " + e);
        }
    }


    public List<BookingRequest> fetchBookingsByUser(User user){
        String userId =  user.getUserId();

        try{
            PreparedStatement ps = connection.prepareStatement("SELECT booking_id FROM bookings WHERE user_id = ?");
            ps.setString(1, userId);

            ResultSet rs = ps.executeQuery();

            List<BookingRequest> bookingList = new ArrayList<>();

            while(rs.next()){
                String bookingId = rs.getString("booking_id");
                BookingRequest req = findBookingById(bookingId);
                bookingList.add(req);
            }
            return bookingList;
        }
        catch (SQLException e){
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
