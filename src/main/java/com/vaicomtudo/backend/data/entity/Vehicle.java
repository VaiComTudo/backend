package com.vaicomtudo.backend.data.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class Vehicle {
    private VehicleCondition condition;

    private String type;
}