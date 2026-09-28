# BloodLink — Blood Donor Registry and Search

BloodLink is a Spring Boot 4 / Java 17 full-stack college project. It contains a REST API backed by MySQL and a responsive HTML, CSS, and JavaScript frontend served by Spring Boot at `http://localhost:8080/`.

## Requirements

- JDK 17
- MySQL 8.x (or compatible MySQL server)
- IntelliJ IDEA (optional)

## MySQL setup

Start MySQL, then edit `src/main/resources/application.properties` if necessary:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/bloodlink?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=update
```

Change `spring.datasource.username` and `spring.datasource.password` to your local MySQL credentials. The JDBC URL asks MySQL to create the `bloodlink` database if it does not exist. The application creates/updates its tables with Hibernate when it starts; do not use the test database settings for a production database.

## Run the application

From the project root, run:

```powershell
.\mvnw.cmd spring-boot:run
```

Or package and run:

```powershell
.\mvnw.cmd clean package
java -jar target\demo-0.0.1-SNAPSHOT.jar
```

Visit [http://localhost:8080/](http://localhost:8080/) for the dashboard. The UI is served from `src/main/resources/static/`; its JavaScript uses the live API and does not include demo donor data.

## IntelliJ IDEA

1. Install/configure JDK 17 in **File → Project Structure → Project SDK**.
2. Open the existing project folder (`demo`) and let IntelliJ import the Maven project from `pom.xml`.
3. Start MySQL and check the credentials in `src/main/resources/application.properties`.
4. Run `com.example.demo.DemoApplication` from the editor, or use the Maven tool window's `spring-boot:run` goal.
5. Open `http://localhost:8080/`.

## API endpoints

All JSON request bodies use `Content-Type: application/json`. Blood group labels are `A+`, `A-`, `B+`, `B-`, `AB+`, `AB-`, `O+`, and `O-`.

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/donors` | Register a donor (returns `201 Created`) |
| `GET` | `/api/donors` | List all donors, including availability |
| `GET` | `/api/donors/{id}` | Get a donor |
| `PUT` | `/api/donors/{id}` | Update a donor |
| `DELETE` | `/api/donors/{id}` | Delete a donor and their donation history (`204 No Content`) |
| `GET` | `/api/donors/search?bloodGroup=O%2B&city=Chennai` | Search currently available donors; either filter is optional |
| `POST` | `/api/donations` | Record a donation and start cooldown |
| `GET` | `/api/dashboard/stats` | Get total, available, and unavailable donor counts |
| `GET` | `/api/dashboard/blood-groups` | Get total and available counts for every blood group |

Donor request:

```json
{
  "name": "Priya Kumar",
  "phone": "+91 98765 43210",
  "email": "priya@example.com",
  "bloodGroup": "O+",
  "city": "Chennai",
  "lastDonationDate": null
}
```

To update, send the same fields to `PUT /api/donors/{id}`. `lastDonationDate` may be `null` for a donor without donation history.

Donation request:

```json
{
  "donorId": 1,
  "donationDate": "2026-09-28"
}
```

Invalid fields return a JSON error with a `fieldErrors` map; not-found donors return `404`, and cooldown/business-rule conflicts return `409`.

## Postman examples

Set the base URL to `http://localhost:8080`.

**Register a donor** — `POST /api/donors`, Body → raw → JSON:

```json
{
  "name": "Priya Kumar",
  "phone": "+91 98765 43210",
  "email": "priya@example.com",
  "bloodGroup": "O+",
  "city": "Chennai",
  "lastDonationDate": null
}
```

**Search available O-positive donors in Chennai** — `GET /api/donors/search?bloodGroup=O%2B&city=Chennai`.

**Record a donation** — `POST /api/donations`, Body → raw → JSON:

```json
{
  "donorId": 1,
  "donationDate": "2026-09-28"
}
```

**View dashboard totals** — `GET /api/dashboard/stats`; **view blood group counts** — `GET /api/dashboard/blood-groups`.

## 90-day cooldown

Availability is computed dynamically in the service layer from `lastDonationDate`; a donor is unavailable until 90 days have elapsed. On day 90 (`lastDonationDate + 90 days`), they become available automatically without a scheduled database update. Search results and dashboard counts use the same calculation. Recording a donation creates a `DonationRecord`, updates the donor's `lastDonationDate`, and rejects a donor still inside the cooldown with a clear `409 Conflict` response. Donation dates in the future and dates older than the donor's most recent recorded donation are rejected.
