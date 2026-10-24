# Smart Park & Ride Reservation System for Integrated Transport Hubs in Sri Lanka 

A full-stack Smart Park & Ride prototype designed to support digital parking management at integrated transport hubs in Sri Lanka, using the Makumbura Multimodal Transport Centre (MMTC) as the reference context.

This project was developed as an individual final-year project for the Bachelor of Information Technology (External) degree at the University of Moratuwa, Sri Lanka.

## Overview

Traditional parking management can make it difficult for commuters to identify available parking spaces, reserve them in advance and transition conveniently to public transport.

The Smart Park & Ride Reservation System addresses this problem through a web-based platform that brings together parking-slot management, reservations, QR-based access, payment workflows and IoT-based occupancy monitoring.

The backend provides REST APIs and business logic to support the web application, reservation lifecycle, authentication, parking management and integration with external services.

## Key Features

- **User authentication and role-based access:** JWT-based authentication for commuters, attendants and administrators.
- **Parking management:** Manage transport hubs, parking lots and individual parking slots.
- **Reservations:** View slot availability and make parking reservations in advance.
- **QR-based access:** Generate reservation QR codes and support check-in and check-out workflows.
- **Payment workflow:** Integrate the PayHere payment gateway into the prototype reservation process.
- **Subscription management:** Support subscription plans and validity periods for recurring parking users.
- **Administrative dashboard:** Support parking operations, reservation monitoring and revenue-related information.
- **Real-time synchronisation:** Use Firebase Realtime Database and Server-Sent Events (SSE) to communicate parking-slot updates.
- **AI-assisted analytics:** Integrate the Groq API for AI-assisted parking analytics.
- **IoT integration:** Connect the software architecture with ESP32-based hardware and infrared sensors for parking-occupancy monitoring.

## Technology Stack

| Component | Technologies |
|---|---|
| Backend | Java 21, Spring Boot 3.4.4 |
| API | REST APIs |
| Security | Spring Security, JWT |
| Database | MySQL |
| Real-time data | Firebase Realtime Database, SSE |
| AI integration | Groq API |
| Hardware | ESP32, infrared sensors, servo motor |
| Frontend | Next.js, React, Tailwind CSS |
| UI testing | Selenium WebDriver, Python, pytest |
| Version control | Git, GitHub |
| Deployment | Vercel for the frontend; Railway was used for backend hosting |

## System Architecture
<img width="556" height="440" alt="image" src="https://github.com/user-attachments/assets/4dc3e959-6437-429c-8c6b-34d6eb44e40a" />


MySQL manages the application's relational data, while Firebase supports real-time parking-slot data synchronisation. The system was designed to coordinate the web application, backend services, database, external integrations and hardware components.

## Main Modules

### 1. User Management
- User registration and login.
- JWT-based authentication.
- Role-based access for commuters, attendants and administrators.

### 2. Parking and Slot Management
- Organise transport hubs, parking lots and parking slots.
- Display slot availability through the web application.
- Support occupancy updates through the system's real-time and IoT integration.

### 3. Reservation Management
- Select an available parking slot and reservation time.
- Create and manage parking reservations.
- Generate reservation QR codes.
- Handle incomplete reservations and reservation expiry through scheduled background processes.

### 4. QR Check-In and Check-Out
- Validate reservation QR codes through the application's access workflow.
- Support recording parking entry and exit.
- Update the associated parking-slot state.

### 5. Payment and Subscriptions
- Integrate the PayHere payment workflow into the prototype.
- Provide a payment-completion countdown for temporary reservation holds.
- Support subscription plans and validity periods for recurring parking users.

### 6. Administration and Analytics
- Provide administrative parking and reservation management.
- Support monitoring of reservations and revenue-related information.
- Integrate AI-assisted analytics using the Groq API.

## Database Design

The MySQL database contains eight related tables organised around the system's transport-hub, parking-lot, parking-slot, reservation and supporting application data.

The design includes relationships between parking slots and their associated sensors, as well as reservation records used to manage the booking lifecycle.

## Testing and Evaluation

Testing combined manual functional testing with automated UI testing to assess application behaviour and usability.

**Manual testing**
- Navigation and page accessibility.
- Registration and login validation.
- Reservation and slot-display workflows.
- QR check-in and check-out workflows.
- Responsive navigation at a 390-pixel mobile viewport.

**Automated UI testing**
- Selenium WebDriver with Python and pytest.
- Five test classes covering different areas of the web interface.
- Repeatable checks for navigation, authentication and related user interactions.

Testing also helped identify integration challenges involving authentication-token handling, reservation expiry, sensor-to-database synchronisation and mobile navigation.

## Challenges and Lessons Learned

Developing the system highlighted the challenges of integrating independently operating components into one application.

Key lessons included:

- Designing and connecting REST APIs across frontend and backend services.
- Coordinating relational database state with Firebase real-time data.
- Handling authentication-token expiry and renewal.
- Managing reservation timeouts and automatic cancellation.
- Testing hardware, backend and frontend interactions together.
- Improving responsive navigation based on automated test findings.

These challenges reinforced the importance of incremental integration, consistent state management and testing throughout development.

## Future Improvements

Potential improvements include:

- Strengthening real-time synchronisation and handling delayed sensor updates.
- Improving authentication and session lifecycle management.
- Expanding automated API, integration and hardware tests.
- Improving deployment reliability and production monitoring.
- Extending payment handling and transaction verification.
- Evaluating the system at a physical transport hub and extending it to additional locations.

## Project Information

**Project:** Smart Park & Ride Reservation System for Integrated Transport Hubs  
**Institution:** University of Moratuwa, Sri Lanka  
**Degree:** Bachelor of Information Technology (External)  
**Project type:** Individual final-year project

## Author

**Chamathka Nethmini Wijesinghe**

- GitHub: [@chamaneth](https://github.com/chamaneth)
- LinkedIn: [Chamathka Nethmini Wijesinghe](https://www.linkedin.com/in/chamathka-nethmini-wije/)

## Licence

Refer to the licence file in this repository for the applicable terms of use, modification and redistribution.
