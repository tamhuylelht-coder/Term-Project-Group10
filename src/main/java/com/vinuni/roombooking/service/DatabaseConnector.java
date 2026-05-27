package com.vinuni.roombooking.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.vinuni.roombooking.enums.AccessLevel;
import com.vinuni.roombooking.enums.BookingStatus;
import com.vinuni.roombooking.enums.InvitationStatus;
import com.vinuni.roombooking.enums.RoomStatus;
import com.vinuni.roombooking.model.Admin;
import com.vinuni.roombooking.model.BookingRequest;
import com.vinuni.roombooking.model.Invitation;
import com.vinuni.roombooking.model.Room;
import com.vinuni.roombooking.model.Staff;
import com.vinuni.roombooking.model.Student;
import com.vinuni.roombooking.model.TimeSlot;
import com.vinuni.roombooking.model.User;
import com.vinuni.roombooking.repository.BookingRepository;

import jakarta.annotation.PostConstruct;


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
     * Persist a new Room. The {@code room.getRoomId()} value is ignored —
     * the rooms table uses {@code AUTO_INCREMENT}, so the DB picks the next id.
     * Callers that need the generated id should re-query via {@link #findAllRooms()}
     * or pull it from the returned value.
     *
     * @return the DB-assigned room_id, or -1 if the driver didn't return one.
     */
    public int insertRoom(Room room){
        String roomName = room.getRoomName();
        int capacity = room.getCapacity();
        String access = room.getAccess().name();
        String status = room.getStatus().name();

        try{
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO rooms (room_name, capacity, access_level, room_status) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, roomName);
            ps.setInt(2, capacity);
            ps.setString(3, access);
            ps.setString(4, status);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
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



    /**
     * Case-insensitive prefix search on user_name, used by the invitee
     * picker. Caller passes a raw query string; the wildcard suffix and
     * lowercasing are applied here. Always returns a list (never null) so
     * the UI can iterate without a null check. Self-exclusion via
     * {@code excludeUserId} stops a host from inviting themselves.
     */
    public List<User> searchUsers(String query, String excludeUserId, int limit){
        String filter = (query == null ? "" : query.trim().toLowerCase()) + "%";
        try{
            PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM users " +
                "WHERE LOWER(user_name) LIKE ? AND user_id <> ? " +
                "ORDER BY user_name LIMIT ?");
            ps.setString(1, filter);
            ps.setString(2, excludeUserId == null ? "" : excludeUserId);
            ps.setInt(3, Math.max(1, limit));
            ResultSet rs = ps.executeQuery();
            List<User> out = new ArrayList<>();
            while (rs.next()) {
                User u = userFromRow(rs);
                if (u != null) out.add(u);
            }
            return out;
        }
        catch (SQLException e){
            throw new IllegalStateException("Cannot search users: " + e);
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
     * Persist a new booking. Writes title (NOT NULL — defaults to "Untitled
     * booking" if the caller hasn't set one) and the optional description.
     */
    public void insertBooking(BookingRequest req) {
        String bookingId = req.getBookingId();
        String userId = req.getUser().getUserId();
        int roomId = req.getRoom().getRoomId();
        String title = req.getTitle();
        if (title == null || title.isBlank()) title = "Untitled booking";
        String description = req.getDescription();
        Timestamp startTime = Timestamp.valueOf(req.getTimeSlot().getStartTime());
        Timestamp endTime = Timestamp.valueOf(req.getTimeSlot().getEndTime());
        String status = req.getStatus().name();
        Timestamp createdAt = Timestamp.valueOf(req.getCreatedAt());

        try{
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO bookings (booking_id, user_id, room_id, title, description, " +
                "start_time, end_time, booking_status, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setString(1, bookingId);
            ps.setString(2, userId);
            ps.setInt(3, roomId);
            ps.setString(4, title);
            if (description == null) ps.setNull(5, Types.LONGVARCHAR);
            else                     ps.setString(5, description);
            ps.setTimestamp(6, startTime);
            ps.setTimestamp(7, endTime);
            ps.setString(8, status);
            ps.setTimestamp(9, createdAt);
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

    public List<BookingRequest> findBookingsByUser(String userId) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT b.booking_id, b.title, b.description, " +
                "       b.start_time, b.end_time, b.booking_status, b.created_at, " +
                "       u.user_id, u.user_name, u.user_password, u.user_email, u.user_role, " +
                "       u.student_id, u.student_major, u.year_of_study, " +
                "       u.staff_id, u.staff_department, u.admin_id, " +
                "       r.room_id, r.room_name, r.capacity, r.access_level, r.room_status " +
                "FROM bookings b " +
                "JOIN users u ON b.user_id = u.user_id " +
                "JOIN rooms r ON b.room_id = r.room_id " +
                "WHERE b.user_id = ?");
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            List<BookingRequest> out = new ArrayList<>();
            while (rs.next()) out.add(bookingFromRow(rs));
            return out;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot fetch bookings by user: " + e);
        }
    }

    public List<BookingRequest> findAllBookings() {
        return findBookingsWhere("");
    }

    /**
     * Single-booking lookup by id. Goes to the DB so callers can recover a
     * persisted booking after a JVM restart cleared the in-memory cache.
     * Returns null when no row matches.
     */
    public BookingRequest findBookingById(String bookingId) {
        if (bookingId == null || bookingId.isBlank()) return null;
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT b.booking_id, b.title, b.description, " +
                "       b.start_time, b.end_time, b.booking_status, b.created_at, " +
                "       u.user_id, u.user_name, u.user_password, u.user_email, u.user_role, " +
                "       u.student_id, u.student_major, u.year_of_study, " +
                "       u.staff_id, u.staff_department, u.admin_id, " +
                "       r.room_id, r.room_name, r.capacity, r.access_level, r.room_status " +
                "FROM bookings b " +
                "JOIN users u ON b.user_id = u.user_id " +
                "JOIN rooms r ON b.room_id = r.room_id " +
                "WHERE b.booking_id = ? LIMIT 1");
            ps.setString(1, bookingId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? bookingFromRow(rs) : null;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot fetch booking by id: " + e);
        }
    }

    /** Bookings whose {@code booking_status = 'PENDING'} — what AdminView needs
     *  for the approval queue. Cheaper than {@link #findAllBookings} when the
     *  history table grows. */
    public List<BookingRequest> findPendingBookings() {
        return findBookingsWhere("WHERE b.booking_status = 'PENDING'");
    }

    private List<BookingRequest> findBookingsWhere(String whereClause) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT b.booking_id, b.title, b.description, " +
                "       b.start_time, b.end_time, b.booking_status, b.created_at, " +
                "       u.user_id, u.user_name, u.user_password, u.user_email, u.user_role, " +
                "       u.student_id, u.student_major, u.year_of_study, " +
                "       u.staff_id, u.staff_department, u.admin_id, " +
                "       r.room_id, r.room_name, r.capacity, r.access_level, r.room_status " +
                "FROM bookings b " +
                "JOIN users u ON b.user_id = u.user_id " +
                "JOIN rooms r ON b.room_id = r.room_id " +
                whereClause);
            ResultSet rs = ps.executeQuery();
            List<BookingRequest> out = new ArrayList<>();
            while (rs.next()) out.add(bookingFromRow(rs));
            return out;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot fetch bookings: " + e);
        }
    }

    private BookingRequest bookingFromRow(ResultSet rs) throws SQLException {
        User user = userFromRow(rs);
        Room room = new Room(
                rs.getInt("room_id"),
                rs.getString("room_name"),
                rs.getInt("capacity"),
                AccessLevel.valueOf(rs.getString("access_level")));
        room.setStatus(RoomStatus.valueOf(rs.getString("room_status")));
        TimeSlot slot = new TimeSlot(
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime());
        BookingRequest req = new BookingRequest(
                rs.getString("booking_id"), user, room, slot,
                safeColumn(rs, "title"),
                safeColumn(rs, "description"));
        req.setStatus(BookingStatus.valueOf(rs.getString("booking_status")));
        return req;
    }

    /** Returns the string column if present, or null if the ResultSet
     *  doesn't have it. Lets us hydrate optional columns without breaking
     *  callers that fetched a narrower projection. */
    private String safeColumn(ResultSet rs, String column) {
        try { return rs.getString(column); }
        catch (SQLException ignored) { return null; }
    }

    private User userFromRow(ResultSet rs) throws SQLException {
        String role = rs.getString("user_role");
        String userId = rs.getString("user_id");
        String userName = rs.getString("user_name");
        String password = rs.getString("user_password");
        String email = rs.getString("user_email");
        if ("STUDENT".equals(role)) {
            return new Student(userId, userName, password, email,
                    rs.getString("student_id"),
                    rs.getString("student_major"),
                    rs.getInt("year_of_study"));
        } else if ("STAFF".equals(role)) {
            return new Staff(userId, userName, password, email,
                    rs.getString("staff_id"),
                    rs.getString("staff_department"));
        } else if ("ADMIN".equals(role)) {
            Admin admin = new Admin(userId, userName, password, email, rs.getString("admin_id"));
            admin.setDatabaseConnector(this);
            admin.setBookingRepository(bookingRepository);
            return admin;
        }
        return null;
    }

    public List<User> findAllUsers() {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM users");
            ResultSet rs = ps.executeQuery();
            List<User> out = new ArrayList<>();
            while (rs.next()) {
                User u = userFromRow(rs);
                if (u != null) out.add(u);
            }
            return out;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot fetch users: " + e);
        }
    }

    public void deleteRoom(int roomId) {
        try {
            PreparedStatement ps = connection.prepareStatement("DELETE FROM rooms WHERE room_id = ?");
            ps.setInt(1, roomId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot delete room: " + e);
        }
    }

    /**
     * Insert an invitation row for {@code (bookingId, userId)}.
     *
     * @return {@code true} if a new row was inserted, {@code false} if the
     *         (booking_id, user_id) pair already existed (duplicate). Callers
     *         can use this to count the actual number of distinct invitees
     *         instead of trusting the form's selected set, which may contain
     *         duplicates from a flaky picker.
     */
    public boolean insertInvitation(String bookingId, String userId){
        try{
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO invitations (booking_id, user_id, status, invited_at) " +
                "VALUES (?, ?, 'PENDING', ?)");
            ps.setString(1, bookingId);
            ps.setString(2, userId);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            return ps.executeUpdate() > 0;
        }
        catch(SQLException e){
            // Idempotent: swallow duplicate (booking_id, user_id) errors so the
            // host can re-submit the form without breaking the batch.
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if(msg.contains("unique") || msg.contains("duplicate")) return false;
            throw new IllegalStateException("Cannot insert invitation: " + e);
        }
    }

    public void updateInvitationStatus(String bookingId, String userId, InvitationStatus status){
        try{
            PreparedStatement ps = connection.prepareStatement(
                "UPDATE invitations SET status = ? WHERE booking_id = ? AND user_id = ?");
            ps.setString(1, status.name());
            ps.setString(2, bookingId);
            ps.setString(3, userId);
            ps.executeUpdate();
        }
        catch(SQLException e){
            throw new IllegalStateException("Cannot update invitation status: " + e);
        }
    }

    /**
     * All invitations addressed to {@code userId}, joined with the host
     * booking + host user + invitee user + room in a single query. Used by
     * BrowseBookingsView.
     */
    public List<Invitation> findInvitationsByUser(String userId){
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT i.status, i.invited_at, " +
                "       b.booking_id, b.title, b.description, " +
                "       b.start_time, b.end_time, b.booking_status, b.created_at, " +
                "       u.user_id   AS host_user_id,   u.user_name AS host_user_name, " +
                "       u.user_password AS host_user_password, u.user_email AS host_user_email, " +
                "       u.user_role AS host_user_role, " +
                "       u.student_id AS host_student_id, u.student_major AS host_student_major, " +
                "       u.year_of_study AS host_year_of_study, " +
                "       u.staff_id AS host_staff_id, u.staff_department AS host_staff_department, " +
                "       u.admin_id AS host_admin_id, " +
                "       r.room_id, r.room_name, r.capacity, r.access_level, r.room_status, " +
                "       inv.user_id AS invitee_user_id, inv.user_name AS invitee_user_name, " +
                "       inv.user_password AS invitee_user_password, inv.user_email AS invitee_user_email, " +
                "       inv.user_role AS invitee_user_role, " +
                "       inv.student_id AS invitee_student_id, inv.student_major AS invitee_student_major, " +
                "       inv.year_of_study AS invitee_year_of_study, " +
                "       inv.staff_id AS invitee_staff_id, inv.staff_department AS invitee_staff_department, " +
                "       inv.admin_id AS invitee_admin_id " +
                "FROM invitations i " +
                "JOIN bookings b ON i.booking_id = b.booking_id " +
                "JOIN users u    ON b.user_id    = u.user_id " +
                "JOIN users inv  ON i.user_id    = inv.user_id " +
                "JOIN rooms r    ON b.room_id    = r.room_id " +
                "WHERE i.user_id = ? " +
                "ORDER BY b.start_time ASC");
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            List<Invitation> out = new ArrayList<>();
            while (rs.next()) out.add(invitationFromRow(rs));
            return out;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot fetch invitations by user: " + e);
        }
    }

    /**
     * Invitations for a specific booking — used by hosts to see who responded.
     */
    public List<Invitation> findInvitationsByBooking(String bookingId){
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT i.status, i.invited_at, " +
                "       b.booking_id, b.title, b.description, " +
                "       b.start_time, b.end_time, b.booking_status, b.created_at, " +
                "       u.user_id   AS host_user_id,   u.user_name AS host_user_name, " +
                "       u.user_password AS host_user_password, u.user_email AS host_user_email, " +
                "       u.user_role AS host_user_role, " +
                "       u.student_id AS host_student_id, u.student_major AS host_student_major, " +
                "       u.year_of_study AS host_year_of_study, " +
                "       u.staff_id AS host_staff_id, u.staff_department AS host_staff_department, " +
                "       u.admin_id AS host_admin_id, " +
                "       r.room_id, r.room_name, r.capacity, r.access_level, r.room_status, " +
                "       inv.user_id AS invitee_user_id, inv.user_name AS invitee_user_name, " +
                "       inv.user_password AS invitee_user_password, inv.user_email AS invitee_user_email, " +
                "       inv.user_role AS invitee_user_role, " +
                "       inv.student_id AS invitee_student_id, inv.student_major AS invitee_student_major, " +
                "       inv.year_of_study AS invitee_year_of_study, " +
                "       inv.staff_id AS invitee_staff_id, inv.staff_department AS invitee_staff_department, " +
                "       inv.admin_id AS invitee_admin_id " +
                "FROM invitations i " +
                "JOIN bookings b ON i.booking_id = b.booking_id " +
                "JOIN users u    ON b.user_id    = u.user_id " +
                "JOIN users inv  ON i.user_id    = inv.user_id " +
                "JOIN rooms r    ON b.room_id    = r.room_id " +
                "WHERE i.booking_id = ?");
            ps.setString(1, bookingId);
            ResultSet rs = ps.executeQuery();
            List<Invitation> out = new ArrayList<>();
            while (rs.next()) out.add(invitationFromRow(rs));
            return out;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot fetch invitations by booking: " + e);
        }
    }

    /**
     * DB-backed overlap check for the room/time the caller wants to book.
     * Overlap predicate: {@code start_time < end AND end_time > start}. Rows
     * in CANCELLED or REJECTED states are ignored so a previously freed slot
     * is reusable.
     *
     * @return true if some live booking already covers any part of [start, end).
     */
    public boolean hasRoomConflict(int roomId, LocalDateTime start, LocalDateTime end){
        return hasRoomConflict(roomId, start, end, null);
    }

    /**
     * Like {@link #hasRoomConflict(int, LocalDateTime, LocalDateTime)} but
     * skips the row whose {@code booking_id = excludeBookingId}. Useful for
     * queue / admin re-validation, where the booking being re-evaluated is
     * already persisted and would otherwise conflict with itself.
     */
    public boolean hasRoomConflict(int roomId, LocalDateTime start, LocalDateTime end,
                                   String excludeBookingId){
        try {
            String sql = "SELECT 1 FROM bookings " +
                    "WHERE room_id = ? AND start_time < ? AND end_time > ? " +
                    "AND booking_status NOT IN ('CANCELLED', 'REJECTED') ";
            if (excludeBookingId != null && !excludeBookingId.isBlank()) {
                sql += "AND booking_id <> ? ";
            }
            sql += "LIMIT 1";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, roomId);
            ps.setTimestamp(2, Timestamp.valueOf(end));
            ps.setTimestamp(3, Timestamp.valueOf(start));
            if (excludeBookingId != null && !excludeBookingId.isBlank()) {
                ps.setString(4, excludeBookingId);
            }
            return ps.executeQuery().next();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot check room conflict: " + e);
        }
    }

    /**
     * Count of ACCEPTED invitations on a booking. The attendee count under
     * the invite-only model.
     */
    public int countAcceptedInvitees(String bookingId){
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) AS c FROM invitations " +
                "WHERE booking_id = ? AND status = 'ACCEPTED'");
            ps.setString(1, bookingId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("c") : 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot count accepted invitees: " + e);
        }
    }

    /** Total number of invitations on a booking, regardless of status. */
    public int countInvitations(String bookingId){
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) AS c FROM invitations WHERE booking_id = ?");
            ps.setString(1, bookingId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("c") : 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot count invitations: " + e);
        }
    }

    /**
     * True if the booking has at least one invitation and every invitation has
     * been ACCEPTED. A booking with zero invitations returns {@code false} —
     * the "all invitees accepted" wording doesn't apply when there are no
     * invitees, so promotion code-paths should special-case that elsewhere.
     */
    public boolean allInviteesAccepted(String bookingId){
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT " +
                "  SUM(CASE WHEN status = 'ACCEPTED' THEN 1 ELSE 0 END) AS accepted, " +
                "  COUNT(*) AS total " +
                "FROM invitations WHERE booking_id = ?");
            ps.setString(1, bookingId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return false;
            int total = rs.getInt("total");
            int accepted = rs.getInt("accepted");
            return total > 0 && accepted == total;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot check invitee acceptance: " + e);
        }
    }

    /**
     * DB-backed "host already booked something on this date" check.
     * Cancelled/rejected bookings are ignored so a freed slot doesn't block
     * the user from re-booking. The comparison is on the booking's start
     * date in the server's local time — same convention as the calendar UI.
     */
    public boolean hasActiveBookingOnDate(String userId, java.time.LocalDate date){
        return hasActiveBookingOnDate(userId, date, null);
    }

    /**
     * Same-day check with optional self-exclusion — pass the booking's own id
     * when re-validating a row that's already in the table so it doesn't fail
     * the daily limit against itself.
     */
    public boolean hasActiveBookingOnDate(String userId, java.time.LocalDate date,
                                          String excludeBookingId){
        try {
            java.time.LocalDateTime dayStart = date.atStartOfDay();
            java.time.LocalDateTime dayEnd   = date.plusDays(1).atStartOfDay();
            String sql = "SELECT 1 FROM bookings " +
                    "WHERE user_id = ? AND start_time >= ? AND start_time < ? " +
                    "AND booking_status NOT IN ('CANCELLED', 'REJECTED') ";
            if (excludeBookingId != null && !excludeBookingId.isBlank()) {
                sql += "AND booking_id <> ? ";
            }
            sql += "LIMIT 1";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, userId);
            ps.setTimestamp(2, Timestamp.valueOf(dayStart));
            ps.setTimestamp(3, Timestamp.valueOf(dayEnd));
            if (excludeBookingId != null && !excludeBookingId.isBlank()) {
                ps.setString(4, excludeBookingId);
            }
            return ps.executeQuery().next();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot check same-day bookings: " + e);
        }
    }

    private Invitation invitationFromRow(ResultSet rs) throws SQLException {
        User host = userFromAliasedRow(rs, "host_");
        Room room = new Room(
                rs.getInt("room_id"),
                rs.getString("room_name"),
                rs.getInt("capacity"),
                AccessLevel.valueOf(rs.getString("access_level")));
        room.setStatus(RoomStatus.valueOf(rs.getString("room_status")));
        TimeSlot slot = new TimeSlot(
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime());
        BookingRequest booking = new BookingRequest(
                rs.getString("booking_id"), host, room, slot,
                safeColumn(rs, "title"),
                safeColumn(rs, "description"));
        booking.setStatus(BookingStatus.valueOf(rs.getString("booking_status")));

        User invitee = userFromAliasedRow(rs, "invitee_");
        return new Invitation(
                booking,
                invitee,
                InvitationStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("invited_at").toLocalDateTime());
    }

    /**
     * Variant of {@link #userFromRow} that reads from columns prefixed with
     * {@code prefix}. Needed because findInvitationsByUser/Booking JOINs the
     * users table twice (host + invitee).
     */
    private User userFromAliasedRow(ResultSet rs, String prefix) throws SQLException {
        String role   = rs.getString(prefix + "user_role");
        String userId = rs.getString(prefix + "user_id");
        String userName = rs.getString(prefix + "user_name");
        String password = rs.getString(prefix + "user_password");
        String email    = rs.getString(prefix + "user_email");
        if ("STUDENT".equals(role)) {
            return new Student(userId, userName, password, email,
                    rs.getString(prefix + "student_id"),
                    rs.getString(prefix + "student_major"),
                    rs.getInt(prefix + "year_of_study"));
        } else if ("STAFF".equals(role)) {
            return new Staff(userId, userName, password, email,
                    rs.getString(prefix + "staff_id"),
                    rs.getString(prefix + "staff_department"));
        } else if ("ADMIN".equals(role)) {
            Admin admin = new Admin(userId, userName, password, email,
                    rs.getString(prefix + "admin_id"));
            admin.setDatabaseConnector(this);
            admin.setBookingRepository(bookingRepository);
            return admin;
        }
        return null;
    }

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
