package com.crunchycode.cabbooking.controller;

import com.crunchycode.cabbooking.dto.request.RideRequest;
import com.crunchycode.cabbooking.dto.response.PageResponse;
import com.crunchycode.cabbooking.dto.response.RideResponse;
import com.crunchycode.cabbooking.exception.ResourceConflictException;
import com.crunchycode.cabbooking.model.RideStatus;
import com.crunchycode.cabbooking.security.JwtService;
import com.crunchycode.cabbooking.service.RideService;
import com.crunchycode.cabbooking.support.SecurityTestSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static com.crunchycode.cabbooking.support.RequestHelper.RIDE_JSON;
import static com.crunchycode.cabbooking.support.RequestHelper.STATUS_JSON;
import static com.crunchycode.cabbooking.support.RequestHelper.build;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RideController.class)
@SecurityTestSetup
class RideControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private RideService rideService;

    private String adminToken() {
        return "Bearer " + jwtService.generateToken("admin", "ADMIN");
    }

    private String userToken() {
        return "Bearer " + jwtService.generateToken("student", "USER");
    }

    private static RideResponse sampleRide(RideStatus status) {
        return new RideResponse(1L, "Test Passenger", "Station", "Airport", new BigDecimal("100.00"),
                status, 1L, LocalDateTime.of(2026, 11, 1, 10, 0));
    }

    private static Stream<Arguments> allEndpoints() {
        return Stream.of(
                Arguments.of("POST", "/api/v1/rides", RIDE_JSON, false),
                Arguments.of("GET", "/api/v1/rides", null, false),
                Arguments.of("GET", "/api/v1/rides/1", null, false),
                Arguments.of("PUT", "/api/v1/rides/1", RIDE_JSON, false),
                Arguments.of("DELETE", "/api/v1/rides/1", null, true),
                Arguments.of("PATCH", "/api/v1/rides/1/accept", null, true),
                Arguments.of("PATCH", "/api/v1/rides/1/status", STATUS_JSON, true));
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
                .andExpect(jsonPath("$.status").value(401));
    }

    @ParameterizedTest(name = "{0} {1} with a garbage token -> 401")
    @MethodSource("allEndpoints")
    void withInvalidToken_returns401(String method, String url, String body, boolean adminOnly) throws Exception {
        mockMvc.perform(build(method, url, body).header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0} {1} as USER -> 403")
    @MethodSource("adminOnlyEndpoints")
    void adminOnlyEndpoint_asUser_returns403(String method, String url, String body, boolean adminOnly)
            throws Exception {
        mockMvc.perform(build(method, url, body).header("Authorization", userToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // ---------------- happy paths ----------------

    @Test
    void user_createsRide_returns201WithLocation() throws Exception {
        when(rideService.createRide(any(RideRequest.class))).thenReturn(sampleRide(RideStatus.REQUESTED));

        mockMvc.perform(build("POST", "/api/v1/rides", RIDE_JSON).header("Authorization", userToken()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/rides/1")))
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    @Test
    void user_listsRides_returns200Page() throws Exception {
        PageResponse<RideResponse> page = new PageResponse<>(List.of(sampleRide(RideStatus.REQUESTED)),
                0, 10, 1L, 1, true, true);
        when(rideService.getAllRides(any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(build("GET", "/api/v1/rides?status=REQUESTED&page=0&size=10&sortBy=fare", null)
                        .header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].passengerName").value("Test Passenger"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void user_getsAndUpdatesRide_returns200() throws Exception {
        when(rideService.getRideById(1L)).thenReturn(sampleRide(RideStatus.REQUESTED));
        when(rideService.updateRide(eq(1L), any(RideRequest.class))).thenReturn(sampleRide(RideStatus.REQUESTED));

        mockMvc.perform(build("GET", "/api/v1/rides/1", null).header("Authorization", userToken()))
                .andExpect(status().isOk());
        mockMvc.perform(build("PUT", "/api/v1/rides/1", RIDE_JSON).header("Authorization", userToken()))
                .andExpect(status().isOk());
    }

    @Test
    void admin_acceptsRide_returns200() throws Exception {
        when(rideService.acceptRide(1L)).thenReturn(sampleRide(RideStatus.ACCEPTED));

        mockMvc.perform(build("PATCH", "/api/v1/rides/1/accept", null).header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void admin_completesRide_returns200() throws Exception {
        when(rideService.updateRideStatus(1L, RideStatus.COMPLETED)).thenReturn(sampleRide(RideStatus.COMPLETED));

        mockMvc.perform(build("PATCH", "/api/v1/rides/1/status", STATUS_JSON).header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void admin_deletesRide_returns204() throws Exception {
        mockMvc.perform(build("DELETE", "/api/v1/rides/1", null).header("Authorization", adminToken()))
                .andExpect(status().isNoContent());
        verify(rideService).deleteRide(1L);
    }

    // ---------------- errors ----------------

    @Test
    void invalidStatusTransition_returns409() throws Exception {
        when(rideService.updateRideStatus(1L, RideStatus.COMPLETED))
                .thenThrow(new ResourceConflictException("Invalid status transition from REQUESTED to COMPLETED"));

        mockMvc.perform(build("PATCH", "/api/v1/rides/1/status", STATUS_JSON).header("Authorization", adminToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Invalid status transition")));
    }

    @Test
    void invalidRideBody_returns400() throws Exception {
        String badBody = "{\"passengerName\":\"\",\"pickupLocation\":\"A\",\"dropLocation\":\"B\",\"fare\":-5,\"driverId\":1}";

        mockMvc.perform(build("POST", "/api/v1/rides", badBody).header("Authorization", userToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("fare")))
                .andExpect(jsonPath("$.message", containsString("passengerName")));
    }

    @Test
    void unknownStatusValue_returns400() throws Exception {
        mockMvc.perform(build("PATCH", "/api/v1/rides/1/status", "{\"status\":\"FLYING\"}")
                        .header("Authorization", adminToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidStatusFilter_returns400() throws Exception {
        mockMvc.perform(build("GET", "/api/v1/rides?status=FLYING", null).header("Authorization", userToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("status")));
    }
}
