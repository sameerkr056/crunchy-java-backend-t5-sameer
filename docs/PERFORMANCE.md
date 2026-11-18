# Performance Testing – Full Track

The Full Track performance requirement was to test the application with 1,000 driver records and check whether the paginated Driver List API responds within 500 ms.

Performance testing was carried out in both the local environment and the Railway deployment.

## 1. Test Setup

| Item | Details |
|---|---|
| Application | Cab Booking Management System |
| Backend | Java 21, Spring Boot |
| Database | MySQL 8 |
| Dataset | 1,000 driver records |
| Local Environment | Local Spring Boot application |
| Production Environment | Railway deployment |
| Performance Target | Under 500 ms |

## 2. Dataset Details

The Railway database contains 1,000 driver records.

| Description | Records |
|---|---:|
| Original driver records | 25 |
| Generated test records | 975 |
| **Total driver records** | **1,000** |

The generated records were added for performance testing.

## 3. API Tested

The following paginated endpoint was used for testing:

```http
GET /api/v1/drivers?page=0&size=100
```

Pagination parameters:

| Parameter | Description |
|---|---|
| `page` | Page number, starting from 0 |
| `size` | Number of records returned per page |

Different page numbers were tested to check the response time of the Driver List API.

## 4. Local Performance Results

The API was tested locally with different page numbers.

| Page | Response Time |
|---|---:|
| Page 0 | 81 ms |
| Page 1 | 46 ms |
| Page 5 | 57 ms |
| Page 9 | 39 ms |
| **Average** | **55.75 ms** |

The local application returned responses within the 500 ms target during these tests.

## 5. Railway Performance Results

The same paginated Driver List API was tested using the deployed Railway application.

| Page | Response Time |
|---|---:|
| Page 0 | 408 ms |
| Page 1 | 386 ms |
| **Average** | **397 ms** |

The average was calculated as:

```text
(408 + 386) / 2 = 397 ms
```

Both measured requests completed within the 500 ms target.

## 6. Performance Comparison

| Environment | Average Response Time | Target |
|---|---:|---:|
| Local | 55.75 ms | 500 ms |
| Railway | 397 ms | 500 ms |

<badge color="success">Target achieved for tested API</badge>

The Railway response time was higher than the local response time. This test also includes the network time between the client and the deployed application.

## 7. Pagination and Database Performance

Pagination was used to retrieve driver records in smaller pages.

Example:

```http
GET /api/v1/drivers?page=0&size=100
GET /api/v1/drivers?page=1&size=100
GET /api/v1/drivers?page=5&size=100
GET /api/v1/drivers?page=9&size=100
```

This allows the application to retrieve a limited number of records instead of returning the complete dataset in a single response.

## 8. Performance Conclusion

The Driver List API was tested with 1,000 driver records in both local and Railway environments.

- Local average response time: **55.75 ms**
- Railway average response time: **397 ms**
- Performance target: **Under 500 ms**

The tested Driver List API met the Full Track response-time target in both environments.

**Note:** These results apply only to the tested paginated Driver List API. Other endpoints, p95 response time, maximum response time, and SQL EXPLAIN plans have not been included because their measurements were not provided.