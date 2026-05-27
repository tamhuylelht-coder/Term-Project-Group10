# Room Booking Application

## Project Overview

This is a Vaadin-based room booking app built with Spring Boot. It provides a browser UI for students, staff, and admins to browse rooms, submit meeting bookings, review invitations, and manage booking approval.

Target users:
- Students who need group or project room reservations.
- Staff who manage meeting rooms and staff-only spaces.
- Admins who review pending requests and inspect bookings.

**Recommended database for testing**: For public testing and grading we recommend using the embedded H2 database (default configuration). H2 requires no external services, starts quickly, auto-runs the project's schema and seed scripts, and enables the web console for inspection at `/h2-console`.

## Prerequisites

| Tool | Minimum Version | Notes |
|---|---|---|
| JDK | 21 | `pom.xml` sets Java 21. |
| Maven | any wrapper-compatible version | Use `./mvnw` or `mvnw.cmd`. |
| H2 | bundled runtime | The app uses embedded H2 for public testing. |

## Project Structure

```text
src/main/java/com/vinuni/roombooking/
  RoomBookingApplication.java    - Spring Boot entry point
  config/                       - Spring profile and local dev seeder
  enums/                        - Room, booking, invitation, access and status enums
  model/                        - Domain objects: User, Student, Staff, Admin, Room, BookingRequest, Invitation, TimeSlot
  repository/                   - In-memory booking cache and pending queue
  service/                      - Business logic and JDBC persistence layer
  ui/                           - Vaadin frontend views, layout, and session helpers
  validator/                    - Booking validation rules
src/main/resources/
  application.properties        - Active H2 datasource configuration for public testing
  application-local.properties  - Optional local overrides (not required for H2 mode)
  schema.sql                    - Database schema for users, rooms, bookings, RSVP, invitations
  addroom.sql                   - Seed data for rooms
  add_demo_users.sql            - Seed data for demo user accounts
```

## Database Setup

No external database is required for public testing. The app uses embedded H2 and initializes itself from schema and seed SQL.

### Run the app with embedded H2

Just start the application. The database schema and seed data are loaded automatically from:

```text
src/main/resources/schema.sql
src/main/resources/addroom.sql
src/main/resources/add_demo_users.sql
```

### Optional: inspect the H2 console

After startup, you can open:

```text
http://localhost:8080/h2-console
```

Use these values:

```text
JDBC URL: jdbc:h2:mem:roombooking
User Name: sa
Password: 
```

## Configuration

Edit `src/main/resources/application.properties` only if you want to change the embedded datasource or enable a different database.

Active fields in `application.properties`:

```properties
spring.datasource.url
spring.datasource.driver-class-name
spring.datasource.username
spring.datasource.password
spring.sql.init.mode
spring.sql.init.schema-locations
spring.sql.init.data-locations
spring.sql.init.continue-on-error
spring.jpa.hibernate.ddl-auto
spring.jpa.database-platform
spring.jpa.open-in-view
spring.h2.console.enabled
```

Current `application.properties` is configured for embedded H2:

```properties
spring.datasource.url=jdbc:h2:mem:roombooking;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER,ROLE;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:schema.sql
spring.sql.init.data-locations=classpath:addroom.sql,classpath:add_demo_users.sql
spring.sql.init.continue-on-error=true

spring.jpa.hibernate.ddl-auto=none
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.open-in-view=false
spring.h2.console.enabled=true
```

## Build and Run

### macOS / Linux

```bash
chmod +x mvnw
./mvnw clean package
./mvnw spring-boot:run
```

### Windows (Command Prompt)

```cmd
mvnw.cmd clean package
mvnw.cmd spring-boot:run
```

## Accessing the App

Open this URL in your browser after the server starts:

```text
http://localhost:8080
```

## Sample Accounts for Testing

The app seeds demo accounts from `src/main/java/com/vinuni/roombooking/config/LocalDevSeeder.java` and `src/main/resources/add_demo_users.sql`.

| Role | Username | Password |
|---|---|---|
| Student | dung | pass |
| Student | tam | pass |
| Student | mien | pass |
| Staff | an | pass |
| Staff | qdung | pass |
| Admin | carol | pass |

Additional seeded users from `add_demo_users.sql` include several student and staff accounts with the same bcrypt password hash.

## Optional: Setting up MySQL from scratch

If you prefer to run against a standalone MySQL server (not required for public testing), follow these steps to create the database and load the project's SQL scripts.

### macOS / Linux

1. Start MySQL server.
2. Create the database and run the schema and seed scripts:

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS roombooking_db;"
mysql -u root -p roombooking_db < src/main/resources/schema.sql
mysql -u root -p roombooking_db < src/main/resources/addroom.sql
mysql -u root -p roombooking_db < src/main/resources/add_demo_users.sql
```

3. Edit `src/main/resources/application.properties` to point to MySQL (replace username/password):

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/roombooking_db
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
spring.jpa.hibernate.ddl-auto=update
```

### Windows (Command Prompt)

1. Start MySQL server.
2. Run these commands:

```cmd
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS roombooking_db;"
mysql -u root -p roombooking_db < src\main\resources\schema.sql
mysql -u root -p roombooking_db < src\main\resources\addroom.sql
mysql -u root -p roombooking_db < src\main\resources\add_demo_users.sql
```

3. Update `src/main/resources/application.properties` with your MySQL credentials (see example above).

When MySQL is configured and available the application will connect to it instead of H2.

## Key Features to Test

- Log in with a seeded user and open the calendar.
- Create a booking within the next 7 days for an available room.
- Verify room access restrictions for student-only, staff-only, and all-users rooms.
- View hosted bookings and invited meetings on the calendar interface.
- Use the admin panel to review pending approvals, all bookings, rooms, and users.
- Force-cancel a booking by ID from the admin `Force cancel` section.

## Design Decisions

- The frontend uses Vaadin server-side views, so UI pages are implemented as Java components.
- Domain models are in `model/`; `User` is abstract and specialized by `Student`, `Staff`, and `Admin`.
- Persistence happens through `service/DatabaseConnector.java` using JDBC and the `users`, `rooms`, `bookings`, `rsvp`, and `invitations` tables.
- `BookingRepository` uses a cache with `LinkedHashMap` and `LinkedList` to store bookings and a pending queue, while the DB is the source of truth for persisted data.
- `BookingValidator` enforces access control, future date, 7-day advance window, 3-hour duration, minimum participant count, and one booking per day.
- `RoomApprovalPolicy` classifies rooms by access level and room name, then decides whether a booking can be auto-approved.
- Conflict detection uses `DatabaseConnector.hasRoomConflict(...)` with this SQL predicate:

```sql
SELECT 1 FROM bookings
WHERE room_id = ?
  AND start_time < ?
  AND end_time > ?
  AND booking_status NOT IN ('CANCELLED', 'REJECTED')
LIMIT 1
```

This rejects overlap when any live booking covers part of the requested interval.

## Troubleshooting

- If `localhost:8080` is already in use, start with a different port:

```bash
./mvnw spring-boot:run -Dserver.port=8081
```

- If `./mvnw` is not executable on macOS:

```bash
chmod +x mvnw
```

- If the app fails to start, verify that `application.properties` still contains the H2 configuration and not leftover MySQL settings.

- If you want to switch back to MySQL later, replace the datasource fields in `application.properties` with MySQL credentials and disable H2 console settings.

