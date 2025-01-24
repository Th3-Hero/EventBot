# Academic Event Management System (EventBot)

EventBot is a Discord-integrated bot designed to help students stay organized by tracking deadlines and milestones for assignments, labs, quizzes, exams, and more. With robust features for event management, filtering, and notifications, EventBot simplifies academic organization for small groups of students.

## Features
### Event Management
- Create Events: Add academic events with details like title, description, due date, event type, and associated courses.
- Edit/Delete Events: Admins can update or delete events as needed. All changes are logged publicly for transparency. 
- Mark Events Completed: Stop receiving notifications for events that are completed.

### Event Tracking
Filter events by:
- Number: View the next X upcoming events (e.g., 3 events). 
- Time Period: Get events within a specific timeframe (e.g., the next 3 days). 
- Course: Focus on events related to a specific course. 
- Type: Filter by event type (e.g., assignment, quiz, lab, exam).

### Notifications
- Customizable reminders:
  - Set reminders for specific offsets (e.g., 24, 48, or 100 hours before the event).
- Users are notified only for events in their selected courses.
- Notifications stop automatically once an event is marked as completed.

### Discord Integration
- Slash commands, buttons, modals, and selections make interaction intuitive and accessible directly through Discord.
- Courses can be individually selected, ensuring users only receive notifications for events relevant to their classes.

### User Roles
- Admins: Edit, and delete events (also anything a user can do).
    - When an event is edited chances are publicly displayed for transparency.
    - Deleted events can be recovered for a configurable time period.
- Users: Create and manage events, select their courses, and mark events as completed. \


## Tech Stack
- Spring Framework:
    - Spring Boot for application development.
    - Spring MVC for REST API development.
    - Jakarta Validation for input validation.
- JDA (Java Discord API) for seamless integration with Discord.
- Hibernate:
    - Flyway for database migrations.
- Quartz for scheduled tasks.
- PostgreSQL for efficient data storage and retrieval.
- Lombok for reducing boilerplate code.

### Deployment:
Hosted on a local server. Deployment is semi-automated using custom scripts.

## Pre-requisites:

- Java Development Kit (JDK) 21 found [here](https://adoptium.net/temurin/releases/?os=windows)
- Maven (build tool) found [here](https://maven.apache.org/download.cgi)
- PostgresSQL (database) found [here](https://www.postgresql.org/download/)

## Furture plans
- Expand functionality with a dedicated web application:
    - Separate main server, auth server, frontend website, and Discord bot for notifications.
- Enhance user experience with smoother interaction flows outside Discord.