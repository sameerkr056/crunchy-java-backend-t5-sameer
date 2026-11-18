# Performance (Full Track: under 500 ms with 1,000 records)

> **Status: NOT MEASURED YET.** The code, indexes, test data script and measuring script are ready, but the
> person who wrote this file could not run MySQL, Docker or the deployed app. Every cell marked `_fill in_` must be filled
> from your own run. **Do not submit the 500 ms claim without these numbers.**

## 1. What was prepared

| Item | File |
|------|------|
| Indexes (migration) | `src/main/resources/db/migration/V10__add_performance_indexes.sql` |
| 1,000 test records (drivers, rides, vehicle types, links) | `scripts/perf/seed-1000.sql` |
| Index usage check (`EXPLAIN`) | `scripts/perf/explain-queries.sql` |
| Timing script (PowerShell) | `scripts/measure-performance.ps1` |
| Remove the test data again | `scripts/perf/cleanup-perf-data.sql` |

## 2. Collection endpoints and their pagination

Every endpoint that returns a list is paginated with `page`, `size` (1 to 100, default 10), `sortBy` (only allowed fields) and `direction`. MySQL does the `LIMIT`, never Java.

| Endpoint | Sortable fields | Query pattern |
|----------|-----------------|---------------|
| `GET /api/v1/drivers` | id, name, email, phone, licenseNumber, available | `WHERE (available = ?) ORDER BY ... LIMIT` + count |
| `GET /api/v1/drivers/with-rides` | same as drivers | page of drivers, then ONE `LEFT JOIN FETCH` for their rides |
| `GET /api/v1/rides` | id, passengerName, pickupLocation, dropLocation, fare, status, bookingTime | `WHERE status = ? AND driver_id = ? ORDER BY ... LIMIT` + count (driver loaded in the same query) |
| `GET /api/v1/vehicle-types` | id, name, description | `ORDER BY ... LIMIT` + count |
| `GET /api/v1/drivers/{id}/vehicle-types` | id, name, description | join table lookup by `driver_id` (primary key) |
| `GET /api/v1/vehicle-types/{id}/drivers` | driver fields | join table lookup by `vehicle_type_id` (index) |

## 3. Indexes

| Index | Table (columns) | Why | From |
|-------|-----------------|-----|------|
| `uk_drivers_email`, `uk_drivers_license_number` | drivers | unique values, duplicate checks | V1 |
| `idx_rides_driver_id` | rides (driver_id) | foreign key, rides of a driver, N+1 fix join | V2 |
| `idx_rides_status` | rides (status) | filter by status | V2 |
| `uk_vehicle_types_name` | vehicle_types (name) | unique name | V3 |
| primary key + `idx_dvt_vehicle_type_id` | driver_vehicle_types | both directions of the many-to-many | V4 |
| `uk_users_username`, `uk_users_email` | users | login lookup | V9 |
| `idx_drivers_name` | drivers (name) | sort drivers by name | **V10** |
| `idx_drivers_available_id` | drivers (available, id) | filter by availability, stable order | **V10** |
| `idx_rides_driver_status` | rides (driver_id, status) | "does this driver have an ACCEPTED ride?" and `driverId` + `status` filters | **V10** |
| `idx_rides_booking_time` | rides (booking_time) | newest-first listing | **V10** |
| `idx_rides_status_booking_time` | rides (status, booking_time) | filter by status and sort by date | **V10** |

Honest note: with only 1,000 rows MySQL can answer many of these queries quickly even without an index, and it may choose a
full table scan when that is cheaper. The `EXPLAIN` output (section 6) shows what it really does. Indexes matter more as the data grows.

## 4. Optimizations that were already in place (kept)

- Pagination everywhere (no query returns "all rows").
- `GET /drivers/with-rides`: two-step loading (page of drivers, then one `JOIN FETCH` for their rides), so the number of SQL queries
  does not grow with the page size. See `QUERIES.md`.
- `GET /rides` loads the driver in the same query (`@EntityGraph`), so no extra query per ride.
- Only DTOs are built from the data; `spring.jpa.open-in-view=false` avoids lazy loading in the controller.
- Production profile: no SQL printing and no Hibernate statistics (both cost time).

## 5. Testing method (do this yourself)

1. **Load 1,000 records.** In MySQL Workbench connect to the database you want to test (local or Railway) and run
   `scripts/perf/seed-1000.sql`. The last query shows the table counts.
2. **Start the app** (locally from IntelliJ, or use the Railway deployment).
3. **Measure** with PowerShell (the script logs in, warms up, then times each request):

```powershell
# local
.\scripts\measure-performance.ps1 -BaseUrl http://localhost:8080 -Username YOUR_ADMIN -Runs 20 -OutFile docs\perf-local.md

# Railway (your real URL)
.\scripts\measure-performance.ps1 -BaseUrl https://YOUR-APP.up.railway.app -Username YOUR_ADMIN -Runs 20 -OutFile docs\perf-railway.md
```

4. **Paste the tables** into section 7 below.
5. Run `scripts/perf/explain-queries.sql` and paste the important `EXPLAIN` rows into section 6.
6. If any "Max ms" is 500 or more, look at the slow endpoint, check its `EXPLAIN`, fix it (index, smaller page, query), run again and describe the change in section 8.
7. Optional: run `scripts/perf/cleanup-perf-data.sql` when you are done.

What the times mean: they are measured from your computer, so they include the internet trip to Railway. Choose the Railway region
closest to you (for example Singapore for India) before measuring. Also note whether the first request after a deploy was slow (warm-up).

## 6. EXPLAIN results (fill in)

| Query | `key` used | `rows` estimated | Comment |
|-------|-----------|------------------|---------|
| drivers, available = 1, order by name | _fill in_ | _fill in_ | |
| rides, order by booking_time desc | _fill in_ | _fill in_ | |
| rides, status = COMPLETED, order by booking_time desc | _fill in_ | _fill in_ | |
| rides, driver_id + status | _fill in_ | _fill in_ | |
| N+1 fix: drivers + rides for 20 ids | _fill in_ | _fill in_ | |

## 7. Measured response times (fill in)

Environment: _local MySQL 8 / Railway MySQL 8, app region, your location, date_

Records in the database: drivers = _fill in_, rides = _fill in_, vehicle types = _fill in_ (must be at least 1,000 in the table you test)

| Endpoint | Page size | Runs | Min ms | Avg ms | p95 ms | Max ms | Under 500 ms? |
|----------|-----------|------|--------|--------|--------|--------|---------------|
| GET /drivers | 20 | _fill in_ | _fill in_ | _fill in_ | _fill in_ | _fill in_ | _fill in_ |
| GET /drivers (sort name) | 100 | _fill in_ | _fill in_ | _fill in_ | _fill in_ | _fill in_ | _fill in_ |
| GET /rides (newest first) | 20 | _fill in_ | _fill in_ | _fill in_ | _fill in_ | _fill in_ | _fill in_ |
| GET /rides?status=COMPLETED | 20 | _fill in_ | _fill in_ | _fill in_ | _fill in_ | _fill in_ | _fill in_ |
| GET /vehicle-types | 20 | _fill in_ | _fill in_ | _fill in_ | _fill in_ | _fill in_ | _fill in_ |
| GET /drivers/with-rides (optimized) | 20 | _fill in_ | _fill in_ | _fill in_ | _fill in_ | _fill in_ | _fill in_ |
| GET /drivers/with-rides (naive N+1, for comparison) | 20 | _fill in_ | _fill in_ | _fill in_ | _fill in_ | _fill in_ | _fill in_ |

**Conclusion (write it only after measuring):** _fill in, for example "all endpoints stayed under 500 ms at 1,000 records" or what you fixed._

## 8. Optimization log (fill in if you had to change anything)

| What was slow | Evidence (time / EXPLAIN) | Change | Time after |
|---------------|---------------------------|--------|-----------|
| _fill in_ | _fill in_ | _fill in_ | _fill in_ |
