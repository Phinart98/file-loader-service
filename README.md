# USSD Event Processor - File Loader Service

This application monitors a directory for USSD event files and loads them into a PostgreSQL database.

## Prerequisites

- Java 21
- Docker and Docker Compose (Optional)
- IntelliJ IDEA or VS Code with Spring Boot plugins/extensions

## Quick Start

### 1. Clone the repository

```bash
git clone https://github.com/Phinart98/file-loader-service.git
cd file-loader-service
```

### 2. Start the database

```bash
docker-compose up -d
```

This starts:
- PostgreSQL database on port 5432
- pgAdmin web interface on http://localhost:5050

### 3. Run the application

Open the project in your IDE (IntelliJ IDEA or VS Code) and run the application directly.

The application will:
- Connect to the PostgreSQL database
- Monitor the configured directory for new files
- Process files and load records into the database

### 4. Access pgAdmin

- Open http://localhost:5050 in your browser
- Login with:
    - Email: admin@admin.com
    - Password: admin
- Create a new server connection:
    - Name: Any name you want
    - Host: localhost (or postgres if inside Docker network)
    - Port: 5432
    - Username: postgres
    - Password: postgres

### 5. Process files

Place USSD log files in the directory configured in `application.properties` (default: C:/ussd/input).

### 6. Stop the database

When finished:

```bash
docker-compose down
```

## Running without Docker

If you prefer to run the application directly:

1. Start a PostgreSQL database.
2. Update `application.properties` with your database connection details
3. Run the application directly from your IDE:
    - In IntelliJ: Click the Run button
    - In VS Code: Use the Spring Boot Dashboard to run the application


## Configuration

Key settings in `application.properties`:
- `file.monitoring.directory`: Directory to monitor for new files
- `file.monitoring.processed-directory`: Directory for processed files
- `file.monitoring.interval`: Polling interval in milliseconds (default: 60000)