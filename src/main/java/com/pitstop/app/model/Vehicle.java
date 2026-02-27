package com.pitstop.app.model;

import com.pitstop.app.constants.VehicleType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "vehicles")
@Getter
@Setter
@Builder
@AllArgsConstructor
public class Vehicle {
    @Id
    private String id;

    private VehicleType vehicleType;
    private String brand;
    private String model;
    private int engineCapacity;
    private boolean deleted = false;
    private LocalDateTime createdAt = LocalDateTime.now();

    // constructors, getters, setters
    public Vehicle() {}
    public Vehicle(VehicleType vehicleType, String brand, String model, int engineCapacity) {
        this.vehicleType = vehicleType;
        this.brand = brand;
        this.model = model;
        this.engineCapacity = engineCapacity;
        this.createdAt = LocalDateTime.now();
    }
}
