# Three-minute demo script

> **Status: script only. No video has been recorded or uploaded.** Record it yourself, upload it (YouTube "unlisted",
> Google Drive with link sharing, or Loom) and put the link in the README and in the CrunchyCode submission form.

Target length: 3:00 (a few seconds either way is fine). Speak slowly, one idea per sentence.

## Before you press record (5 minutes)

- [ ] The Railway app is **deployed and healthy**. Open the health URL once so it is warm.
- [ ] Your ADMIN account exists (you set `ADMIN_USERNAME` / `ADMIN_PASSWORD` on Railway).
- [ ] 1,000 test records are loaded (`scripts/perf/seed-1000.sql`) and `docs/PERFORMANCE.md` is filled in.
- [ ] Browser tabs ready: (1) Swagger UI on the **public HTTPS Railway URL**, (2) README on GitHub, (3) Railway dashboard (Deployments / Logs). Do **not** open the Variables tab.
- [ ] A second browser tab or Postman ready? Not needed. Everything works from Swagger.
- [ ] Close notifications. Zoom the browser to 125% so text is readable.
- [ ] Have two accounts ready: a USER you will register live (pick a fresh username) and your ADMIN.
- [ ] Recording tool: Windows **Win + G** (Xbox Game Bar) or OBS Studio. Record the browser window with your microphone.

Never show passwords, the JWT secret, database passwords or the Railway "Variables" tab. If something sensitive appears, re-record.

## Timeline

| Time | What you show | What you say (example wording) |
|------|---------------|--------------------------------|
| **0:00 - 0:15** Introduction | README top section on GitHub | "Hi, I am Sameer Kumar. This is my Cab Booking Management API for the CrunchyCode internship. It manages drivers, rides and vehicle types, with JWT security, a MySQL database and Swagger documentation." |
| **0:15 - 0:35** Live deployment | Browser address bar with the **https://...up.railway.app** URL, then `/actuator/health` showing `"status":"UP"`. Quick look at the Railway Deployments tab (green, "Active") | "It is deployed on Railway from a Docker image and reachable on a public HTTPS address, not localhost. The health check shows the app and its Railway MySQL database are up." |
| **0:35 - 0:55** Swagger UI | `/swagger-ui/index.html`. Scroll the groups: Authentication, Drivers, Rides, Vehicle types | "This is the Swagger documentation generated from the code. Every endpoint has a description, example request and the possible response codes." |
| **0:55 - 1:20** Register and login | `POST /api/v1/auth/register` -> Try it out -> Execute (201). Then `POST /api/v1/auth/login` -> Execute, show the `token` | "I register a new user. Passwords are stored with BCrypt. Everyone who registers is a USER. Now I log in and get a JWT token." |
| **1:20 - 1:40** JWT in Swagger | Click **Authorize**, paste the token, Close. Run `GET /api/v1/drivers` (200) | "I paste the token into Authorize. Now protected endpoints work. Without the token the same call returns 401." (optionally click Logout in the dialog and show 401) |
| **1:40 - 2:05** USER versus ADMIN | As USER run `DELETE /api/v1/drivers/{id}` (use a high id like 9999) -> **403**. Re-Authorize with the ADMIN token and run `POST /api/v1/vehicle-types` (201) | "A USER can read data and book rides, but deleting is for ADMIN only, so the user gets 403 Forbidden. As ADMIN I can create a vehicle type." |
| **2:05 - 2:25** Database operations | In the ADMIN session: `GET /api/v1/vehicle-types` shows the new type (persistent). Optionally show MySQL Workbench with the row, or the Railway database "Data" tab | "The data is stored in Railway MySQL. Flyway created the tables automatically when the app started, and the data stays after redeploys." |
| **2:25 - 2:45** Pagination | `GET /api/v1/drivers?page=0&size=5&sortBy=name&direction=asc`, point at `totalElements` (1,025 if you loaded the test data and kept the sample data) and `totalPages`. Change to `page=1` | "Every list endpoint is paginated and sortable. Here are 5 drivers per page out of more than a thousand." |
| **2:45 - 3:00** Performance and close | Show `docs/PERFORMANCE.md` table **with your real measured numbers** (or run `GET /api/v1/rides?size=20` and say the time Swagger shows). Finish on the README | "I measured response times with 1,000 records, and the results are in docs/PERFORMANCE.md. The code, tests and documentation are on GitHub. Thank you." |

## If something goes wrong while recording

- 401 on every call: the token expired (60 minutes by default) or you forgot Authorize. Log in again.
- First request is slow: the app was cold. Warm it up before recording.
- A username already exists (409): use a new one.

## After recording

- [ ] Watch the video once: is the URL in the address bar visible and **not localhost**?
- [ ] Is it under about 3 minutes and without any secret on screen?
- [ ] Paste the link into README ("Demo video" line) and into the CrunchyCode submission form.
