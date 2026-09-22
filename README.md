Social App Backend API

A RESTful API built with Spring Boot 3 for a social media application. It provides secure authentication, media uploads, user interactions, and real-time messaging.

Tech Stack

Framework: Java 21, Spring Boot 3 (Web, Data JPA, Security, Mail, Validation)
Database: MySQL
Security: Spring Security & JWT (JSON Web Tokens)
Storage: Cloudinary API
Real-Time: WebSockets & STOMP
Documentation: Swagger (Springdoc OpenAPI)

Core Features

Authentication: User registration with SMTP email verification and JWT-based login.
Feed & Interactions: Direct-to-cloud image uploads (Cloudinary), paginated chronological feed, unique toggle likes, and comments.
Real-Time Chat: REST-based inbox/history combined with live WebSocket messaging, secured via a custom STOMP JWT interceptor.
Interactive Docs: Swagger UI integration with Bearer Token support.

Local Setup
To run this project locally, create a MySQL database named social_app and configure the following environment variables in your IDE or .env file:

DB_USERNAME=root
DB_PASSWORD=your_mysql_password
JWT_SECRET=your_super_secret_jwt_key_here
CLOUDINARY_URL=cloudinary://API_KEY:API_SECRET@CLOUD_NAME
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_google_app_password
