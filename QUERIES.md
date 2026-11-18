# QUERIES.md - The N+1 Query Problem

CrunchyCode Internship, Task 3 (Full Track). Project: `crunchy-java-backend-t3-sameer`.

> **Task 5 note:** SQL printing and Hibernate statistics are ON for local development only. The Docker image starts with the `prod` profile, which turns them off, so to repeat the N+1 experiment run the app locally from IntelliJ (no profile). The endpoint is still `GET /api/v1/drivers/with-rides?mode=naive|optimized` and still needs a token.

> **Task 4 note:** since the API is now secured, every request in this file needs a token. Log in first (`POST /api/v1/auth/login`) and send `Authorization: Bearer <token>` (in Postman: Authorization tab -> Bearer Token). Any logged-in role can call `GET /drivers/with-rides`.

> **How to read the numbers in this file.** The project was written without a MySQL server available, so the
> "observed" columns are **empty on purpose**. Section 3 explains what the code is *designed* to produce
> (a prediction), and Section 8 is the table you fill in with the numbers you actually see in your own console.
> Do not submit predicted numbers as observed results.

---

## 1. What is the N+1 problem?

You load a list of **N** parents with **1** query, and then, while using each parent, Hibernate silently runs
**one more query per parent** to load a lazy relationship. That is `1 + N` queries instead of 1 or 2.

In this project:

- `Driver` has `@OneToMany List<Ride> rides` (lazy, which is the right default).
- Loading a page of 20 drivers is 1 query.
- Building each driver's JSON calls `driver.getRides()`. The rides were never loaded, so Hibernate runs
  `select ... from rides where driver_id = ?` once **per driver**: 20 extra queries.

It works and returns correct data, so nobody notices in development. In production, with thousands of rows,
it becomes slow and hammers the database.

## 2. Endpoint used for testing

```
GET /api/v1/drivers/with-rides?mode=naive&page=0&size=20&sortBy=id&direction=asc        <- BEFORE (N+1)
GET /api/v1/drivers/with-rides?mode=optimized&page=0&size=20&sortBy=id&direction=asc    <- AFTER (fixed)
```

- Same endpoint, same page, same JSON. Only the loading strategy differs.
- `mode=naive` keeps the original, unoptimized code path in `DriverService.getDriversWithRidesNaive(...)`
  so the problem stays reproducible. `mode=optimized` (the default) uses
  `DriverService.getDriversWithRidesOptimized(...)`.
- Test data: Flyway migrations `V5` to `V8` insert **25 drivers**, **75 rides** (2 to 4 per driver),
  7 vehicle types and 52 driver/vehicle-type links. With `size=20` the page holds 20 drivers.

## 3. Expected query count (from reading the code, NOT observed)

| | Queries in the code | For `size=20` |
|---|---|---|
| Before (`mode=naive`) | 1 (page of drivers) + 1 (count for page info) + **N** (rides per driver) | 1 + 1 + 20 = **22** |
| After (`mode=optimized`) | 1 (page of drivers) + 1 (count) + 1 (`left join fetch` of all rides) | 1 + 1 + 1 = **3** |

Notes:
- The count query only runs when Spring Data cannot tell the total from the page itself. With `size=20` and
  25 drivers it does run. If you use `size=30`, the count is skipped and each number drops by 1.
- The "after" number does not grow with N. Ask for 100 drivers per page and it stays at 3 queries.

## 4. How to capture the numbers (step by step)

1. Start MySQL, then run `CabBookingApplication` in IntelliJ (see README). Flyway creates the tables and sample data.
2. Wait for `Started CabBookingApplication`. **Clear the console** (trash-can icon on the Run tab).
3. In Postman or the browser call the BEFORE endpoint:
   `http://localhost:8080/api/v1/drivers/with-rides?mode=naive&page=0&size=20`
4. In the console you will see lines starting with `Hibernate:`. Count them in one of two ways:
   - **Quick way:** find the `Session Metrics { ... }` block printed right after the request and read the line
     `... nanoseconds spent preparing N JDBC statements;`. That `N` is the query count.
   - **Visual way:** select the `Hibernate:` lines and count them, or use IntelliJ's console search
     (`Ctrl+F`) for `from rides` and read the match count.
5. Take the **BEFORE screenshot** (repeated `select ... from rides r1_0 where r1_0.driver_id=?` lines plus the
   Session Metrics block) and save it as `docs/n-plus-one-before.png`.
6. Clear the console again and call the AFTER endpoint:
   `http://localhost:8080/api/v1/drivers/with-rides?mode=optimized&page=0&size=20`
7. Count again the same way and save the **AFTER screenshot** as `docs/n-plus-one-after.png`.
8. Write both numbers into the table in Section 8.

Tip: call each endpoint once as a warm-up before the real measurement. The very first request after startup can
log a few extra statements (connection and metadata checks) that are not part of the N+1 problem. Always clear
the console right before the request you measure.

## 5. Example of the repeated SQL (shape of what you should see BEFORE)

Exact column aliases differ slightly, but the pattern is: one drivers query, one count, then the **same rides
query repeated, once per driver, with a different id**:

```
Hibernate: select d1_0.id,d1_0.available,d1_0.email,d1_0.license_number,d1_0.name,d1_0.phone from drivers d1_0 where (? is null or d1_0.available=?) order by d1_0.id limit ?
Hibernate: select count(d1_0.id) from drivers d1_0 where (? is null or d1_0.available=?)
Hibernate: select r1_0.driver_id,r1_0.id,r1_0.booking_time,r1_0.drop_location,r1_0.fare,... from rides r1_0 where r1_0.driver_id=? order by r1_0.id     <- driver 1
Hibernate: select r1_0.driver_id,r1_0.id,r1_0.booking_time,r1_0.drop_location,r1_0.fare,... from rides r1_0 where r1_0.driver_id=? order by r1_0.id     <- driver 2
Hibernate: select r1_0.driver_id,r1_0.id,r1_0.booking_time,r1_0.drop_location,r1_0.fare,... from rides r1_0 where r1_0.driver_id=? order by r1_0.id     <- driver 3
... (the same statement keeps repeating, 20 times in total)
```

That repeated `where r1_0.driver_id=?` line is the signature of an N+1 problem.

AFTER the fix the console should show only a few statements, and the rides query appears **once** with an
`in (...)` list:

```
Hibernate: select d1_0.id, ... from drivers d1_0 where (? is null or d1_0.available=?) order by d1_0.id limit ?
Hibernate: select count(d1_0.id) from drivers d1_0 where (? is null or d1_0.available=?)
Hibernate: select d1_0.id, ..., r1_0.driver_id, r1_0.id, ... from drivers d1_0 left join rides r1_0 on d1_0.id=r1_0.driver_id where d1_0.id in (?,?,?, ... ) order by r1_0.id
```

(The snippets above show the expected *shape* of the SQL for explanation. Replace them with your own screenshots.)

## 6. Optimization technique used

**Two-step loading with `JOIN FETCH`** (see `DriverService.getDriversWithRidesOptimized` and
`DriverRepository.findAllWithRidesByIdIn`):

1. **Step 1:** load one page of drivers with normal Spring Data pagination (`LIMIT` is done by MySQL).
2. **Step 2:** take the ids of those drivers and load them again with
   `select d from Driver d left join fetch d.rides where d.id in :ids`. This brings all their rides in a single query.
3. Build the response in the order of step 1, so the requested sorting is kept.

What was **not** done: `FetchType.EAGER`. The mapping is still lazy. EAGER on `rides` would load rides for every
driver everywhere (even in `GET /drivers/{id}`), and for list queries it usually still runs the extra per-driver
queries. It moves the problem instead of solving it.

## 7. Why the optimization works

- The N extra queries were caused by Hibernate loading each `rides` collection **separately, on first access**.
  `JOIN FETCH` tells Hibernate to load the drivers and their rides **together in one SQL join**, so the
  collections are already filled when `getRides()` is called and no further query is needed.
- Why not put `JOIN FETCH` into the first (paged) query? Fetching a collection together with `LIMIT` would make
  Hibernate load **all** rows and paginate **in memory** (it logs warning `HHH90003004`). That is wrong and slow for
  big tables. Paging the drivers first (a cheap, correct `LIMIT`) and fetching the rides for only those ids
  afterwards is the standard fix, and it keeps pagination compatible with the optimization.
- The number of queries is now **constant** (about 3), independent of the page size.

Other valid options (not used here): `@EntityGraph(attributePaths = "rides")` on a repository method (same SQL idea,
same pagination caveat) and `@BatchSize(size = 20)` / `hibernate.default_batch_fetch_size` on the collection
(turns N queries into N/20 queries using `where driver_id in (...)`).

## 8. Before and after comparison (fill in with YOUR observed results)

Environment: MySQL 8, `GET /api/v1/drivers/with-rides?page=0&size=20&sortBy=id&direction=asc`, 25 drivers and
75 rides in the database.

| | Before (`mode=naive`) | After (`mode=optimized`) |
|---|---|---|
| Expected from the code (prediction) | 22 | 3 |
| **Observed query count (JDBC statements)** | _fill in_ | _fill in_ |
| Observed number of `from rides` queries | _fill in_ | _fill in_ |
| Response time (optional, ms, from Postman) | _fill in_ | _fill in_ |
| Screenshot | `docs/n-plus-one-before.png` | `docs/n-plus-one-after.png` |

Queries saved = observed before - observed after = _fill in_

Optional experiment: repeat with `size=5`, `size=20` and `size=100` for both modes and add the rows. You should see
the naive count grow with the page size and the optimized count stay flat. That is the clearest proof.

| size | Observed BEFORE | Observed AFTER |
|---|---|---|
| 5 | _fill in_ | _fill in_ |
| 20 | _fill in_ | _fill in_ |
| 25 | _fill in_ | _fill in_ |
