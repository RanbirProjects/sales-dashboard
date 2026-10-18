# 📊 Sales Data Dashboard — Java Full Stack

Spring Boot + JPA/Hibernate + H2 / MySQL + vanilla JS with animated Chart.js dashboards.

## Features
- KPI cards with count-up animation (revenue, orders, units, average order value)
- Animated charts: monthly revenue, revenue by category / region, top products, top salespeople
- Filters by region, category and date range
- Add, edit and delete sales (with validation), live search, CSV export
- **Data storage**: file-based H2 by default (zero setup, persists in `./data/`), MySQL via a profile
- 400 demo sales are seeded automatically on first run

## Run in VS Code
1. Install **JDK 17+** and **Maven** (macOS: `brew install openjdk@17 maven`).
2. In VS Code install the *Extension Pack for Java* and *Spring Boot Extension Pack* (VS Code will suggest them).
3. Open this folder (`File → Open Folder…`).
4. Run it either way:
   - **Terminal:** `mvn spring-boot:run`
   - **Run/Debug panel:** choose *Run Sales Dashboard (H2)* and press ▶
5. Open **http://localhost:8080**

## Storage options
**H2 (default)** — nothing to install. Data lives in `data/salesdb.mv.db`.
Browse it at http://localhost:8080/h2-console → JDBC URL `jdbc:h2:file:./data/salesdb`, user `sa`, empty password.

**MySQL** — edit the password in `src/main/resources/application-mysql.properties`, make sure MySQL is running, then:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```
(or pick *Run Sales Dashboard (MySQL)* in VS Code). The `salesdb` database is created automatically.

## REST API
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/sales?region=&category=&from=&to=` | List sales (all filters optional, dates `YYYY-MM-DD`) |
| POST | `/api/sales` | Create a sale |
| PUT | `/api/sales/{id}` | Update a sale |
| DELETE | `/api/sales/{id}` | Delete a sale |
| GET | `/api/dashboard?…` | KPIs + chart series (same filters) |
| GET | `/api/filters` | Distinct regions, categories, products, salespeople |
| GET | `/api/sales/export?…` | Download CSV |

Example body for POST/PUT:
```json
{ "orderDate": "2026-10-18", "product": "Laptop", "category": "Electronics",
  "region": "South", "salesperson": "Priya", "quantity": 2, "unitPrice": 62000 }
```

## Project structure
```
sales-dashboard/
├── pom.xml
├── .vscode/                     # launch + recommended extensions
└── src/main/
    ├── java/com/ranbir/salesdashboard/
    │   ├── SalesDashboardApplication.java
    │   ├── model/Sale.java              # JPA entity + validation
    │   ├── repository/SaleRepository.java
    │   ├── service/SaleService.java     # business logic + aggregations
    │   ├── controller/                  # REST API + error handler
    │   ├── dto/                         # Dashboard, LabelValue, Filters
    │   └── config/DataSeeder.java       # demo data
    └── resources/
        ├── application.properties       # H2 (default)
        ├── application-mysql.properties
        └── static/                      # index.html, css/style.css, js/app.js, js/chart.umd.js
```

## Reset the data
Stop the app and delete the `data/` folder — demo data is re-created on the next start.
