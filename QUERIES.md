# QUERIES.md – Database Queries & Performance Testing

**Internship:** CrunchyCode – Task 5 Full Track  
**Project:** `crunchy-java-backend-t5-sameer`

## 1. Project Overview

The Cab Booking Management System is a REST API developed using Java 21 and Spring Boot.

It manages:
- Drivers
- Rides
- Vehicle Types
- Driver and Ride relationships
- JWT-based authentication and authorization

**Database:** MySQL 8  
**Migration Tool:** Flyway  
**Deployment:** Railway

## 2. Database Records

The Railway MySQL database contains 1,000 driver records.

| Record Type | Count |
|---|---:|
| Original records | 25 |
| Generated testing records | 975 |
| **Total** | **1,000** |

The additional records were generated for pagination and performance testing. The original 25 records were preserved.

## 3. Pagination API

**Endpoint:**

```http
GET /api/v1/drivers?page=0&size=100
```

| Parameter | Value | Description |
|---|---:|---|
| page | 0 | First page |
| size | 100 | Records per page |
| sortBy | id | Sorting field |
| direction | asc | Ascending order |

**Expected pagination information:**

| Field | Expected Value |
|---|---:|
| totalElements | 1000 |
| totalPages | 10 |
| size | 100 |

## 4. Pagination Testing

Pagination was tested using the Swagger UI.

**Live Swagger URL:**

https://crunchy-java-backend-t5-sameer-production.up.railway.app/swagger-ui/index.html

| Page | Page Size | HTTP Status |
|---:|---:|---|
| 0 | 100 | 200 OK |
| 1 | 100 | 200 OK |
| 5 | 100 | 200 OK |
| 9 | 100 | 200 OK |

All four tested requests returned successful responses.

## 5. Local Performance Testing

Performance was measured using Chrome Developer Tools Network tab.

**Test Configuration:**
- Endpoint: `GET /api/v1/drivers`
- Total records: 1,000
- Page size: 100
- Tool: Chrome Developer Tools

### Results

| Page | Response Time |
|---:|---:|
| 0 | 81 ms |
| 1 | 46 ms |
| 5 | 57 ms |
| 9 | 39 ms |
| **Average** | **55.75 ms** |

Average calculation:

```text
(81 + 46 + 57 + 39) / 4 = 55.75 ms
```

All four measured local requests completed within 500 ms.

## 6. Railway Performance Testing

The deployed Railway API was also tested with 1,000 driver records.

| Page | Response Time |
|---:|---:|
| 0 | 408 ms |
| 1 | 386 ms |
| **Average** | **397 ms** |

Both requests returned HTTP 200 OK.

Average calculation:

```text
(408 + 386) / 2 = 397 ms
```

Both measured Railway requests completed within the 500 ms target.

## 7. Local vs Railway Comparison

| Metric | Local | Railway |
|---|---:|---:|
| Total records | 1,000 | 1,000 |
| Page size | 100 | 100 |
| Page 0 | 81 ms | 408 ms |
| Page 1 | 46 ms | 386 ms |
| Average | 55.75 ms | 397 ms |
| Target | Below 500 ms | Below 500 ms |
| Result | Within target | Within target |

The local environment showed faster response times than the Railway deployment during these tests.

However, both environments completed all measured requests within the 500 ms target.

## 8. Conclusion

The Cab Booking Management System was successfully deployed on Railway with MySQL.

A dataset of 1,000 driver records was used to test pagination.

The local API achieved an average response time of 55.75 ms across four measured requests.

The Railway API achieved an average response time of 397 ms across two measured requests.

Both local and Railway performance results were within the 500 ms target.

These results demonstrate successful pagination functionality and acceptable API response times in both environments.

**Note:** Performance results represent the measurements recorded during testing. Response times may vary depending on system load and deployment resources.