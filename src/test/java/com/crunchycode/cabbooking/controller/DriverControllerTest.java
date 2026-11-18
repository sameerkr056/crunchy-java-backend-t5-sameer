package com.crunchycode.cabbooking.controller;

import com.crunchycode.cabbooking.dto.request.DriverRequest;
import com.crunchycode.cabbooking.dto.response.DriverResponse;
import com.crunchycode.cabbooking.dto.response.DriverWithRidesResponse;
import com.crunchycode.cabbooking.dto.response.PageResponse;
import com.crunchycode.cabbooking.dto.response.VehicleTypeResponse;
import com.crunchycode.cabbooking.exception.ResourceConflictException;
import com.crunchycode.cabbooking.exception.ResourceNotFoundException;
import com.crunchycode.cabbooking.security.JwtService;
import com.crunchycode.cabbooking.service.DriverService;
import com.crunchycode.cabbooking.support.SecurityTestSetup;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.Stream;

import static com.crunchycode.cabbooking.support.RequestHelper.DRIVER_JSON;
import static com.crunchycode.cabbooking.support.RequestHelper.build;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests with MockMvc: real security + real controller + real exception handler, mocked service.
 */
@WebMvcTest(DriverController.class)
@SecurityTestSetup
class DriverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private DriverService driverService;

    private String adminToken() {
        return "Bearer " + jwtService.generateToken("admin", "ADMIN");
    }

    private String userToken() {
        return "Bearer " + jwtService.generateToken("student", "USER");
    }

    private static DriverResponse sampleDriver() {
        return new DriverResponse(1L, "Test Driver", "test.driver@example.com", "9876543210", "JH0120239999", true);
    }

    private static <T> PageResponse<T> emptyPage() {
        return new PageResponse<>(List.of(), 0, 10, 0L, 0, true, true);
    }

    // every endpoint of this controller: method, url, body, adminOnly
    private static Stream<Arguments> allEndpoints() {
        return Stream.of(
                Arguments.of("POST", "/api/v1/drivers", DRIVER_JSON, true),
                Arguments.of("GET", "/api/v1/drivers", null, false),
                Arguments.of("GET", "/api/v1/drivers/with-rides", null, false),
                Arguments.of("GET", "/api/v1/drivers/1", null, false),
                Arguments.of("PUT", "/api/v1/drivers/1", DRIVER_JSON, true),
                Arguments.of("DELETE", "/api/v1/drivers/1", null, true),
                Arguments.of("GET", "/api/v1/drivers/1/vehicle-types", null, false),
                Arguments.of("PUT", "/api/v1/drivers/1/vehicle-types/2", null, true),
                Arguments.of("DELETE", "/api/v1/drivers/1/vehicle-types/2", null, true));
    }

    private static Stream<Arguments> adminOnlyEndpoints() {
        return allEndpoints().filter(args -> (Boolean) args.get()[3]);
    }

    // ---------------- 401 and 403 ----------------

    @ParameterizedTest(name = "{0} {1} without token -> 401")
    @MethodSource("allEndpoints")
    void withoutToken_returns401(String method, String url, String body, boolean adminOnly) throws Exception {
        mockMvc.perform(build(method, url, body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value(url));
    }

    @ParameterizedTest(name = "{0} {1} with a garbage token -> 401")
    @MethodSource("allEndpoints")
    void withInvalidToken_returns401(String method, String url, String body, boolean adminOnly) throws Exception {
        mockMvc.perform(build(method, url, body).header("Authorization", "Bearer this.is.not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0} {1} as USER -> 403")
    @MethodSource("adminOnlyEndpoints")
    void adminOnlyEndpoint_asUser_returns403(String method, String url, String body, boolean adminOnly)
            throws Exception {
        mockMvc.perform(build(method, url, body).header("Authorization", userToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You do not have permission to perform this action"));
    }

    @Test
    void tokenSignedWithAnotherSecret_returns401() throws Exception {
        String forged = Jwts.builder().subject("hacker").claim("role", "ADMIN")
                .signWith(Jwts.SIG.HS256.key().build()).compact();

        mockMvc.perform(build("DELETE", "/api/v1/drivers/1", null).header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
        verify(driverService, never()).deleteDriver(any());
    }

    // ---------------- happy paths ----------------

    @Test
    void admin_createsDriver_returns201WithLocation() throws Exception {
        when(driverService.createDriver(any(DriverRequest.class))).thenReturn(sampleDriver());

        mockMvc.perform(build("POST", "/api/v1/drivers", DRIVER_JSON).header("Authorization", adminToken()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/drivers/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Driver"));
    }

    @Test
    void user_listsDrivers_returns200Page() throws Exception {
        PageResponse<DriverResponse> page = emptyPage();
        when(driverService.getAllDrivers(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(build("GET", "/api/v1/drivers?page=0&size=5&sortBy=name&direction=desc", null)
                        .header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.first").value(true));
    }

    @Test
    void user_getsDriverById_returns200() throws Exception {
        when(driverService.getDriverById(1L)).thenReturn(sampleDriver());

        mockMvc.perform(build("GET", "/api/v1/drivers/1", null).header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licenseNumber").value("JH0120239999"));
    }

    @Test
    void admin_updatesDriver_returns200() throws Exception {
        when(driverService.updateDriver(eq(1L), any(DriverRequest.class))).thenReturn(sampleDriver());

        mockMvc.perform(build("PUT", "/api/v1/drivers/1", DRIVER_JSON).header("Authorization", adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    void admin_deletesDriver_returns204() throws Exception {
        mockMvc.perform(build("DELETE", "/api/v1/drivers/1", null).header("Authorization", adminToken()))
                .andExpect(status().isNoContent());
        verify(driverService).deleteDriver(1L);
    }

    @Test
    void admin_assignsAndRemovesVehicleType_returns204() throws Exception {
        mockMvc.perform(build("PUT", "/api/v1/drivers/1/vehicle-types/2", null).header("Authorization", adminToken()))
                .andExpect(status().isNoContent());
        mockMvc.perform(build("DELETE", "/api/v1/drivers/1/vehicle-types/2", null).header("Authorization", adminToken()))
                .andExpect(status().isNoContent());
        verify(driverService).assignVehicleType(1L, 2L);
        verify(driverService).removeVehicleType(1L, 2L);
    }

    @Test
    void user_listsVehicleTypesOfDriver_returns200() throws Exception {
        PageResponse<VehicleTypeResponse> page = new PageResponse<>(
                List.of(new VehicleTypeResponse(2L, "SUV", "Big car")), 0, 10, 1L, 1, true, true);
        when(driverService.getVehicleTypesOfDriver(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(build("GET", "/api/v1/drivers/1/vehicle-types", null).header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("SUV"));
    }

    @Test
    void withRides_defaultsToOptimizedMode() throws Exception {
        PageResponse<DriverWithRidesResponse> page = emptyPage();
        when(driverService.getDriversWithRidesOptimized(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(build("GET", "/api/v1/drivers/with-rides", null).header("Authorization", userToken()))
                .andExpect(status().isOk());
        verify(driverService).getDriversWithRidesOptimized(any(Pageable.class));
        verify(driverService, never()).getDriversWithRidesNaive(any(Pageable.class));
    }

    @Test
    void withRides_naiveMode_usesTheNaiveMethod() throws Exception {
        PageResponse<DriverWithRidesResponse> page = emptyPage();
        when(driverService.getDriversWithRidesNaive(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(build("GET", "/api/v1/drivers/with-rides?mode=naive&size=20", null)
                        .header("Authorization", userToken()))
                .andExpect(status().isOk());
        verify(driverService).getDriversWithRidesNaive(any(Pageable.class));
    }

    // ---------------- global exception handler: our error shape, never Spring's default ----------------

    @Test
    void missingDriver_returns404InOurErrorShape() throws Exception {
        when(driverService.getDriverById(99L)).thenThrow(new ResourceNotFoundException("Driver not found with id: 99"));

        mockMvc.perform(build("GET", "/api/v1/drivers/99", null).header("Authorization", userToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Driver not found with id: 99"))
                .andExpect(jsonPath("$.path").value("/api/v1/drivers/99"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void businessConflict_returns409() throws Exception {
        doThrow(new ResourceConflictException("Driver has associated rides"))
                .when(driverService).deleteDriver(1L);

        mockMvc.perform(build("DELETE", "/api/v1/drivers/1", null).header("Authorization", adminToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Driver has associated rides"));
    }

    @Test
    void invalidBody_returns400WithFieldMessages() throws Exception {
        String badBody = "{\"name\":\"\",\"email\":\"not-an-email\",\"phone\":\"123\",\"licenseNumber\":\"\"}";

        mockMvc.perform(build("POST", "/api/v1/drivers", badBody).header("Authorization", adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("email")))
                .andExpect(jsonPath("$.message", containsString("name")));
        verify(driverService, never()).createDriver(any());
    }

    @Test
    void invalidPaginationSize_returns400() throws Exception {
        mockMvc.perform(build("GET", "/api/v1/drivers?size=0", null).header("Authorization", userToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("size")));
    }

    @Test
    void invalidSortField_returns400() throws Exception {
        mockMvc.perform(build("GET", "/api/v1/drivers?sortBy=password", null).header("Authorization", userToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid sortBy")));
    }

    @Test
    void malformedJson_returns400() throws Exception {
        mockMvc.perform(build("POST", "/api/v1/drivers", "{ this is not json").header("Authorization", adminToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unexpectedError_returns500WithoutLeakingDetails() throws Exception {
        when(driverService.getDriverById(1L)).thenThrow(new IllegalStateException("db password is hunter2"));

        mockMvc.perform(build("GET", "/api/v1/drivers/1", null).header("Authorization", userToken()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Something went wrong. Please try again later"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("hunter2"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }
}
