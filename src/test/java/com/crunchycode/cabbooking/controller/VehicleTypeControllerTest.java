package com.crunchycode.cabbooking.controller;

import com.crunchycode.cabbooking.dto.request.VehicleTypeRequest;
import com.crunchycode.cabbooking.dto.response.DriverResponse;
import com.crunchycode.cabbooking.dto.response.PageResponse;
import com.crunchycode.cabbooking.dto.response.VehicleTypeResponse;
import com.crunchycode.cabbooking.exception.ResourceConflictException;
import com.crunchycode.cabbooking.exception.ResourceNotFoundException;
import com.crunchycode.cabbooking.security.JwtService;
import com.crunchycode.cabbooking.service.VehicleTypeService;
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

import java.util.List;
import java.util.stream.Stream;

import static com.crunchycode.cabbooking.support.RequestHelper.VEHICLE_TYPE_JSON;
import static com.crunchycode.cabbooking.support.RequestHelper.build;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VehicleTypeController.class)
@SecurityTestSetup
class VehicleTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private VehicleTypeService vehicleTypeService;

    private String adminToken() {
        return "Bearer " + jwtService.generateToken("admin", "ADMIN");
    }

    private String userToken() {
        return "Bearer " + jwtService.generateToken("student", "USER");
    }

    private static Stream<Arguments> allEndpoints() {
        return Stream.of(
                Arguments.of("POST", "/api/v1/vehicle-types", VEHICLE_TYPE_JSON, true),
                Arguments.of("GET", "/api/v1/vehicle-types", null, false),
                Arguments.of("GET", "/api/v1/vehicle-types/1", null, false),
                Arguments.of("PUT", "/api/v1/vehicle-types/1", VEHICLE_TYPE_JSON, true),
                Arguments.of("DELETE", "/api/v1/vehicle-types/1", null, true),
                Arguments.of("GET", "/api/v1/vehicle-types/1/drivers", null, false));
    }

    private static Stream<Arguments> adminOnlyEndpoints() {
        return allEndpoints().filter(args -> (Boolean) args.get()[3]);
    }

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

    @Test
    void admin_createsVehicleType_returns201WithLocation() throws Exception {
        when(vehicleTypeService.createVehicleType(any(VehicleTypeRequest.class)))
                .thenReturn(new VehicleTypeResponse(8L, "Test Van", "A van for tests"));

        mockMvc.perform(build("POST", "/api/v1/vehicle-types", VEHICLE_TYPE_JSON).header("Authorization", adminToken()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/vehicle-types/8")))
                .andExpect(jsonPath("$.name").value("Test Van"));
    }

    @Test
    void user_listsVehicleTypes_returns200Page() throws Exception {
        PageResponse<VehicleTypeResponse> page = new PageResponse<>(
                List.of(new VehicleTypeResponse(1L, "Sedan", "Comfortable car")), 0, 10, 1L, 1, true, true);
        when(vehicleTypeService.getAllVehicleTypes(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(build("GET", "/api/v1/vehicle-types?page=0&size=10&sortBy=name", null)
                        .header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Sedan"));
    }

    @Test
    void user_getsVehicleType_returns200() throws Exception {
        when(vehicleTypeService.getVehicleTypeById(1L)).thenReturn(new VehicleTypeResponse(1L, "Sedan", null));

        mockMvc.perform(build("GET", "/api/v1/vehicle-types/1", null).header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void admin_updatesVehicleType_returns200() throws Exception {
        when(vehicleTypeService.updateVehicleType(eq(1L), any(VehicleTypeRequest.class)))
                .thenReturn(new VehicleTypeResponse(1L, "Test Van", "A van for tests"));

        mockMvc.perform(build("PUT", "/api/v1/vehicle-types/1", VEHICLE_TYPE_JSON).header("Authorization", adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    void admin_deletesVehicleType_returns204() throws Exception {
        mockMvc.perform(build("DELETE", "/api/v1/vehicle-types/1", null).header("Authorization", adminToken()))
                .andExpect(status().isNoContent());
        verify(vehicleTypeService).deleteVehicleType(1L);
    }

    @Test
    void user_listsDriversOfVehicleType_returns200() throws Exception {
        PageResponse<DriverResponse> page = new PageResponse<>(
                List.of(new DriverResponse(1L, "Ravi", "ravi@example.com", "9876543210", "JH01", true)),
                0, 10, 1L, 1, true, true);
        when(vehicleTypeService.getDriversOfVehicleType(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(build("GET", "/api/v1/vehicle-types/1/drivers", null).header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Ravi"));
    }

    @Test
    void deleteVehicleTypeInUse_returns409() throws Exception {
        doThrow(new ResourceConflictException("Vehicle type with id 1 cannot be deleted because drivers are using it"))
                .when(vehicleTypeService).deleteVehicleType(1L);

        mockMvc.perform(build("DELETE", "/api/v1/vehicle-types/1", null).header("Authorization", adminToken()))
                .andExpect(status().isConflict());
    }

    @Test
    void missingVehicleType_returns404() throws Exception {
        when(vehicleTypeService.getVehicleTypeById(99L))
                .thenThrow(new ResourceNotFoundException("Vehicle type not found with id: 99"));

        mockMvc.perform(build("GET", "/api/v1/vehicle-types/99", null).header("Authorization", userToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Vehicle type not found with id: 99"));
    }

    @Test
    void blankName_returns400() throws Exception {
        mockMvc.perform(build("POST", "/api/v1/vehicle-types", "{\"name\":\" \"}").header("Authorization", adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("name")));
    }
}
