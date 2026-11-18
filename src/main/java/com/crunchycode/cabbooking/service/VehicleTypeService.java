package com.crunchycode.cabbooking.service;

import com.crunchycode.cabbooking.dto.request.VehicleTypeRequest;
import com.crunchycode.cabbooking.dto.response.DriverResponse;
import com.crunchycode.cabbooking.dto.response.PageResponse;
import com.crunchycode.cabbooking.dto.response.VehicleTypeResponse;
import com.crunchycode.cabbooking.exception.ResourceConflictException;
import com.crunchycode.cabbooking.exception.ResourceNotFoundException;
import com.crunchycode.cabbooking.model.VehicleType;
import com.crunchycode.cabbooking.repository.DriverRepository;
import com.crunchycode.cabbooking.repository.VehicleTypeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VehicleTypeService {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final DriverRepository driverRepository;

    public VehicleTypeService(VehicleTypeRepository vehicleTypeRepository, DriverRepository driverRepository) {
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.driverRepository = driverRepository;
    }

    @Transactional
    public VehicleTypeResponse createVehicleType(VehicleTypeRequest request) {
        if (vehicleTypeRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ResourceConflictException("A vehicle type named '" + request.getName() + "' already exists");
        }
        VehicleType vehicleType = new VehicleType();
        vehicleType.setName(request.getName());
        vehicleType.setDescription(request.getDescription());
        return VehicleTypeResponse.from(vehicleTypeRepository.save(vehicleType));
    }

    public PageResponse<VehicleTypeResponse> getAllVehicleTypes(Pageable pageable) {
        Page<VehicleTypeResponse> page = vehicleTypeRepository.findAll(pageable).map(VehicleTypeResponse::from);
        return PageResponse.from(page);
    }

    public VehicleTypeResponse getVehicleTypeById(Long id) {
        return VehicleTypeResponse.from(findVehicleTypeOrThrow(id));
    }

    @Transactional
    public VehicleTypeResponse updateVehicleType(Long id, VehicleTypeRequest request) {
        VehicleType vehicleType = findVehicleTypeOrThrow(id);
        if (vehicleTypeRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new ResourceConflictException("A vehicle type named '" + request.getName() + "' already exists");
        }
        vehicleType.setName(request.getName());
        vehicleType.setDescription(request.getDescription());
        return VehicleTypeResponse.from(vehicleType);
    }

    @Transactional
    public void deleteVehicleType(Long id) {
        VehicleType vehicleType = findVehicleTypeOrThrow(id);
        if (driverRepository.existsByVehicleTypesId(id)) {
            throw new ResourceConflictException(
                    "Vehicle type with id " + id + " cannot be deleted because drivers are using it");
        }
        vehicleTypeRepository.delete(vehicleType);
    }

    // Many-to-many: drivers that have this vehicle type
    public PageResponse<DriverResponse> getDriversOfVehicleType(Long id, Pageable pageable) {
        findVehicleTypeOrThrow(id);
        Page<DriverResponse> page = driverRepository.findByVehicleTypesId(id, pageable).map(DriverResponse::from);
        return PageResponse.from(page);
    }

    private VehicleType findVehicleTypeOrThrow(Long id) {
        return vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found with id: " + id));
    }
}
