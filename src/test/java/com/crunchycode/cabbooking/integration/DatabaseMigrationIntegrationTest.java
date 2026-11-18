package com.crunchycode.cabbooking.integration;

import com.crunchycode.cabbooking.model.Driver;
import com.crunchycode.cabbooking.repository.DriverRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The real thing: the whole application against a real MySQL 8 started by Testcontainers.
 * It proves that
 *  - all Flyway migrations (V1 to V10) run on MySQL 8,
 *  - Hibernate's schema validation accepts the entities (ddl-auto=validate),
 *  - Swagger, the health check, login, roles and pagination work together.
 *
 * It needs Docker (Docker Desktop on Windows). Without Docker the whole class is SKIPPED, not failed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseMigrationIntegrationTest {

    private static final String ADMIN_USER = "it_admin";
    private static final String ADMIN_PASSWORD = "ItAdminPass123";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        // throw-away secret that only exists during this test run
        registry.add("app.jwt.secret", () -> Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded()));
        registry.add("app.admin.username", () -> ADMIN_USER);
        registry.add("app.admin.password", () -> ADMIN_PASSWORD);
    }

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private DriverRepository driverRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private int count(String sql) {
        Integer value = jdbc.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    private String login(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("username", username, "password", password));
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    // ---------------- database and Flyway ----------------

    @Test
    @Order(1)
    void everyFlywayMigrationRanSuccessfully() {
        assertThat(count("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0")).isZero();
        assertThat(count("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1")).isGreaterThanOrEqualTo(10);
    }

    @Test
    @Order(2)
    void sampleDataFromTheMigrationsIsPresent() {
        assertThat(count("SELECT COUNT(*) FROM drivers")).isEqualTo(25);
        assertThat(count("SELECT COUNT(*) FROM rides")).isEqualTo(75);
        assertThat(count("SELECT COUNT(*) FROM vehicle_types")).isEqualTo(7);
        assertThat(count("SELECT COUNT(*) FROM driver_vehicle_types")).isEqualTo(52);
    }

    @Test
    @Order(3)
    void performanceIndexesFromV10Exist() {
        List<String> indexes = jdbc.queryForList(
                "SELECT DISTINCT index_name FROM information_schema.statistics WHERE table_schema = DATABASE()",
                String.class);

        assertThat(indexes).contains("idx_drivers_name", "idx_drivers_available_id", "idx_rides_driver_status",
                "idx_rides_booking_time", "idx_rides_status_booking_time", "idx_rides_driver_id", "idx_rides_status");
    }

    @Test
    @Order(4)
    void adminAccountIsCreatedWithABCryptHash() {
        String hash = jdbc.queryForObject("SELECT password FROM users WHERE username = ?", String.class, ADMIN_USER);
        String role = jdbc.queryForObject("SELECT role FROM users WHERE username = ?", String.class, ADMIN_USER);

        assertThat(role).isEqualTo("ADMIN");
        assertThat(hash).startsWith("$2").isNotEqualTo(ADMIN_PASSWORD);
    }

    @Test
    @Order(5)
    void paginationAndTheN1FixWorkOnRealMysql() {
        Page<Driver> page = driverRepository.search(null, PageRequest.of(0, 10, Sort.by("name")));
        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotalElements()).isEqualTo(25);

        List<Long> ids = page.getContent().stream().map(Driver::getId).toList();
        List<Driver> withRides = driverRepository.findAllWithRidesByIdIn(ids);
        assertThat(withRides).hasSize(10);
    }

    // ---------------- Swagger and health ----------------

    @Test
    @Order(6)
    void openApiJsonIsPublicAndDescribesTheApi() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        String json = result.getResponse().getContentAsString();

        assertThat(json).contains("/api/v1/drivers", "/api/v1/rides", "/api/v1/vehicle-types", "/api/v1/auth/login",
                "bearerAuth", "ErrorResponse");
    }

    @Test
    @Order(7)
    void swaggerUiIsPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    @Order(8)
    void healthCheckIsPublicAndReportsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // ---------------- security end to end ----------------

    @Test
    @Order(9)
    void registerLoginAndRolesWorkEndToEnd() throws Exception {
        // register a normal user and log in
        String registerBody = "{\"username\":\"it_user\",\"email\":\"it_user@example.com\",\"password\":\"ItUserPass123\"}";
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
        String userToken = login("it_user", "ItUserPass123");
        String adminToken = login(ADMIN_USER, ADMIN_PASSWORD);

        // no token -> 401
        mockMvc.perform(get("/api/v1/drivers")).andExpect(status().isUnauthorized());

        // USER can read (paginated, real data) ...
        mockMvc.perform(get("/api/v1/drivers?page=0&size=5&sortBy=name").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(25));

        // ... but cannot delete -> 403
        mockMvc.perform(delete("/api/v1/drivers/1").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // ADMIN can use the N+1 demo endpoint and manage vehicle types
        mockMvc.perform(get("/api/v1/drivers/with-rides?size=5").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rides").isArray());

        String vehicleType = "{\"name\":\"Integration Van\",\"description\":\"created by the test\"}";
        mockMvc.perform(post("/api/v1/vehicle-types").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(vehicleType))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/vehicle-types").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(vehicleType))
                .andExpect(status().isConflict());

        // the driver with rides cannot be deleted even by an ADMIN (database rule + service rule)
        mockMvc.perform(delete("/api/v1/drivers/1").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }
}
