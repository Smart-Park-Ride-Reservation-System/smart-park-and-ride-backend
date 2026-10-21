# Smart Park & Ride Reservation System

**A smart parking reservation prototype for integrated transport hubs in Sri Lanka.**

A full-stack final-year project developed at the University of Moratuwa to support parking management, advance reservations, QR-based access and IoT-assisted parking occupancy monitoring, using the Makumbura Multimodal Transport Centre (MMTC) as the reference context.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [System Architecture](#system-architecture)
- [Testing](#testing)
- [Challenges and Lessons Learned](#challenges-and-lessons-learned)
- [Getting Started](#getting-started)
- [Project Information](#project-information)
- [Author](#author)

## Overview

The Smart Park & Ride Reservation System provides a web-based approach to managing parking facilities at integrated transport hubs. It connects commuters, parking operations and supporting IoT components through a central application.

The backend provides REST APIs and business logic for authentication, parking-slot management, reservations, QR workflows and integration with external services.

## Features

- **Authentication and authorisation** — JWT-based authentication and role-based access for commuters, attendants and administrators.
- **Parking management** — Organised management of transport hubs, parking lots and parking slots.
- **Online reservations** — View slot availability and reserve parking spaces in advance.
- **QR-based access** — Support reservation verification and parking check-in/check-out workflows.
- **Payment workflow** — PayHere integration within the prototype reservation process.
- **Subscription management** — Support recurring parking plans and validity periods.
- **Real-time updates** — Firebase Realtime Database and Server-Sent Events (SSE) for parking-slot synchronisation.
- **AI-assisted analytics** — Groq API integration for parking analytics.
- **IoT integration** — ESP32-based hardware and infrared sensors for parking-occupancy monitoring.
- **Reservation lifecycle management** — Scheduled processes for handling reservation expiry and unused reservations.

## Technology Stack

| Area | Technologies |
|---|---|
| Backend | Java 21, Spring Boot 3.4.4 |
| API | REST |
| Security | Spring Security, JWT |
| Database | MySQL |
| Real-time synchronisation | Firebase Realtime Database, SSE |
| AI integration | Groq API |
| Frontend | Next.js, React, Tailwind CSS |
| Automated UI testing | Selenium WebDriver, Python, pytest |
| Hardware | ESP32, infrared sensors, servo motor |
| Deployment | Vercel (frontend), Railway (backend hosting used during development) |
| Version control | Git, GitHub |

## System Architecture

```mermaid
<img width="556" height="440" alt="image" src="https://github.com/user-attachments/assets/032158c3-538a-4b88-86a5-12c7058cd8c3" />

```

The system combines a relational database for core application data with Firebase for real-time parking-slot synchronisation. The backend also integrates with external services and IoT components as part of the prototype architecture.

## Testing

The project used both manual testing and automated UI testing.

### Manual Testing

Manual checks covered:

- Navigation and page accessibility
- Registration and login validation
- Parking-slot display and reservation workflows
- QR check-in and check-out workflows
- Responsive navigation at a 390-pixel mobile viewport

### Automated UI Testing

- **Framework:** Selenium WebDriver
- **Language:** Python
- **Test runner:** pytest
- **Coverage areas:** Navigation, authentication and related web-interface interactions

Testing helped identify and address issues involving mobile navigation, authentication-token handling, reservation expiry and synchronisation between the database and sensor-related data.

## Challenges and Lessons Learned

Key technical challenges included:

- Integrating the frontend, Spring Boot backend and external services.
- Coordinating MySQL data with Firebase real-time updates.
- Handling authentication-token expiry and renewal.
- Managing reservation timeouts and automatic cancellation.
- Integrating physical sensor readings with application state.
- Using automated tests to identify usability and navigation issues.

These challenges reinforced the importance of incremental integration, consistent state management and testing throughout development.

## Getting Started

### Prerequisites

- Java Development Kit (JDK) 21
- Maven, or the project's Maven Wrapper if available
- MySQL Server
- Git

### 1. Clone the repository

```bash
git clone https://github.com/Smart-Park-Ride-Reservation-System/smart-park-and-ride-backend.git
cd smart-park-and-ride-backend
```

### 2. Configure the database

Create the database if the application uses the default database name:

```sql
CREATE DATABASE parking_db;
```

Configure the database connection using the properties expected by the project in `application.properties` or `application.yml`.

### 3. Configure environment variables

Set the database credentials, JWT configuration and any required external-service credentials according to the application's configuration.

Do not commit real credentials, API keys, private keys or `.env` files to GitHub. Use environment variables and provide only safe example values in documentation.

### 4. Run the backend

If the Maven Wrapper is available, run on Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Or use Maven:

```bash
mvn spring-boot:run
```

The application typically starts on port `8080`, unless a different port is configured.

> **Note:** Confirm the database configuration, required environment variables and startup instructions against the current source code before running the application.

## Project Information

| Item | Details |
|---|---|
| Project type | Individual final-year undergraduate project |
| Institution | University of Moratuwa, Sri Lanka |
| Degree | Bachelor of Information Technology (External) |
| Reference context | Makumbura Multimodal Transport Centre (MMTC) |

## Author

**Chamathka Nethmini Wijesinghe**

- [GitHub](https://github.com/chamaneth)
- [LinkedIn](https://www.linkedin.com/in/chamathka-nethmini-wije/)

## Licence

Refer to the licence file in this repository for the applicable terms of use, modification and redistribution.
