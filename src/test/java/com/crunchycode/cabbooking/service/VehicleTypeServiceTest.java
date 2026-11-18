package com.crunchycode.cabbooking.service;

import com.crunchycode.cabbooking.dto.request.VehicleTypeRequest;
import com.crunchycode.cabbooking.dto.response.DriverResponse;
import com.crunchycode.cabbooking.dto.response.PageResponse;
import com.crunchycode.cabbooking.dto.response.VehicleTypeResponse;
import com.crunchycode.cabbooking.exception.ResourceConflictException;
import com.crunchycode.cabbooking.exception.ResourceNotFoundException;
import com.crunchycode.cabbooking.model.Driver;
import com.crunchycode.cabbooking.model.VehicleType;
import com.crunchycode.cabbooking.repository.DriverRepository;
import com.crunchycode.cabbooking.repository.VehicleTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleTypeServiceTest {

    @Mock
    private VehicleTypeRepository vehicleTypeRepository;
    @Mock
    private DriverRepository driverRepository;

    @InjectMocks
    private VehicleTypeService vehicleTypeService;

    private static VehicleType vehicleType(Long id, String name) {
        VehicleType vehicleType = new VehicleType();
        vehicleType.setId(id);
        vehicleType.setName(name);
        vehicleType.setDescription("desc");
        return vehicleType;
    }

    private static VehicleTypeRequest request(String name) {
        VehicleTypeRequest request = new VehicleTypeRequest();
        request.setName(name);
        request.setDescription("A description");
        return request;
    }

    @Test
    void createVehicleType_savesIt() {
        when(vehicleTypeRepository.existsByNameIgnoreCase("Tuk Tuk")).thenReturn(false);
        when(vehicleTypeRepository.save(any(VehicleType.class))).thenAnswer(invocation -> {
            VehicleType saved = invocation.getArgument(0);
            saved.setId(9L);
            return saved;
        });

        VehicleTypeResponse response = vehicleTypeService.createVehicleType(request("Tuk Tuk"));

        assertThat(response.getId()).isEqualTo(9L);
        assertThat(response.getName()).isEqualTo("Tuk Tuk");
    }

    @Test
    void createVehicleType_duplicateName_throwsConflict() {
        when(vehicleTypeRepository.existsByNameIgnoreCase("SUV")).thenReturn(true);

        assertThatThrownBy(() -> vehicleTypeService.createVehicleType(request("SUV")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("SUV");
        verify(vehicleTypeRepository, never()).save(any());
    }

    @Test
    void getAllVehicleTypes_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(vehicleTypeRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(vehicleType(1L, "Sedan"), vehicleType(2L, "SUV")), pageable, 2));

        PageResponse<VehicleTypeResponse> result = vehicleTypeService.getAllVehicleTypes(pageable);

        assertThat(result.getContent()).extracting(VehicleTypeResponse::getName).containsExactly("Sedan", "SUV");
    }

    @Test
    void getVehicleTypeById_missing_throwsNotFound() {
        when(vehicleTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleTypeService.getVehicleTypeById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getVehicleTypeById_returnsIt() {
        when(vehicleTypeRepository.findById(1L)).thenReturn(Optional.of(vehicleType(1L, "Sedan")));

        assertThat(vehicleTypeService.getVehicleTypeById(1L).getName()).isEqualTo("Sedan");
    }

    @Test
    void updateVehicleType_changesNameAndDescription() {
        when(vehicleTypeRepository.findById(1L)).thenReturn(Optional.of(vehicleType(1L, "Sedan")));
        when(vehicleTypeRepository.existsByNameIgnoreCaseAndIdNot("Sedan XL", 1L)).thenReturn(false);

        VehicleTypeResponse response = vehicleTypeService.updateVehicleType(1L, request("Sedan XL"));

        assertThat(response.getName()).isEqualTo("Sedan XL");
        assertThat(response.getDescription()).isEqualTo("A description");
    }

    @Test
    void updateVehicleType_nameUsedByAnother_throwsConflict() {
        when(vehicleTypeRepository.findById(1L)).thenReturn(Optional.of(vehicleType(1L, "Sedan")));
        when(vehicleTypeRepository.existsByNameIgnoreCaseAndIdNot("SUV", 1L)).thenReturn(true);

        assertThatThrownBy(() -> vehicleTypeService.updateVehicleType(1L, request("SUV")))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void deleteVehicleType_inUse_throwsConflict() {
        when(vehicleTypeRepository.findById(1L)).thenReturn(Optional.of(vehicleType(1L, "Sedan")));
        when(driverRepository.existsByVehicleTypesId(1L)).thenReturn(true);

        assertThatThrownBy(() -> vehicleTypeService.deleteVehicleType(1L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("drivers are using it");
        verify(vehicleTypeRepository, never()).delete(any());
    }

    @Test
    void deleteVehicleType_notInUse_deletesIt() {
        VehicleType existing = vehicleType(1L, "Sedan");
        when(vehicleTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(driverRepository.existsByVehicleTypesId(1L)).thenReturn(false);

        vehicleTypeService.deleteVehicleType(1L);

        verify(vehicleTypeRepository).delete(existing);
    }

    @Test
    void getDriversOfVehicleType_returnsDriverPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Driver driver = new Driver();
        driver.setId(3L);
        driver.setName("Ravi Kumar");
        when(vehicleTypeRepository.findById(1L)).thenReturn(Optional.of(vehicleType(1L, "Sedan")));
        when(driverRepository.findByVehicleTypesId(1L, pageable)).thenReturn(new PageImpl<>(List.of(driver), pageable, 1));

        PageResponse<DriverResponse> result = vehicleTypeService.getDriversOfVehicleType(1L, pageable);

        assertThat(result.getContent()).extracting(DriverResponse::getName).containsExactly("Ravi Kumar");
    }

    @Test
    void getDriversOfVehicleType_missingType_throwsNotFound() {
        when(vehicleTypeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleTypeService.getDriversOfVehicleType(1L, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
