-- Schema for the room booking app. Authored for MySQL; the H2 in-memory dev datasource
-- runs it via MySQL compatibility mode (see spring.datasource.url in application.properties).
-- Original `USE roombooking_db;` removed because in-memory H2 has no database to switch to,
-- and missing `;` between CREATE TABLE statements have been added so the script parses.

CREATE TABLE users (
    user_id VARCHAR(255) PRIMARY KEY,
    user_name VARCHAR(255) NOT NULL,
    user_password VARCHAR(255) NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    user_role ENUM('STUDENT', 'STAFF', 'ADMIN') NOT NULL,
    student_id VARCHAR(255),
    student_major VARCHAR(255),
    year_of_study INT,
    staff_id VARCHAR(255),
    staff_department VARCHAR(255),
    admin_id VARCHAR(255)
);

CREATE TABLE rooms (
    room_id INT PRIMARY KEY AUTO_INCREMENT,
    room_name VARCHAR(255) NOT NULL,
    capacity INT NOT NULL,
    access_level ENUM('STUDENT_ONLY', 'STAFF_ONLY', 'ALL_USERS') NOT NULL,
    room_status ENUM('AVAILABLE', 'OCCUPIED', 'MAINTENANCE') NOT NULL
);

CREATE TABLE bookings (
    booking_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    room_id INT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    booking_status ENUM('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED') NOT NULL,
    created_at DATETIME NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (room_id) REFERENCES rooms(room_id)
);

CREATE TABLE rsvp (
    rsvp_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,

    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- Outlook-style invitations. Distinct from rsvp:
--   - rsvp        = "I self-added myself to attend this open booking"
--   - invitations = "The host asked me; I've not yet responded / accepted / declined"
-- The unique (booking_id, user_id) constraint stops the host from inviting the
-- same person twice.
CREATE TABLE invitations (
    invitation_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    status ENUM('PENDING', 'ACCEPTED', 'DECLINED') NOT NULL DEFAULT 'PENDING',
    invited_at DATETIME NOT NULL,

    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    UNIQUE KEY uq_invitation (booking_id, user_id)
);

