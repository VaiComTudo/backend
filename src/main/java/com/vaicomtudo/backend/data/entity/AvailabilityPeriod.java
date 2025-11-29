package com.vaicomtudo.backend.data;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.EqualsAndHashCode;

@Embeddable
@EqualsAndHashCode
public class AvailabilityPeriod {

    @Enumerated(EnumType.STRING)
    private DayOfWeek startDay;

    @Enumerated(EnumType.STRING)
    private DayOfWeek endDay;

    private LocalTime startTime;
    private LocalTime endTime;
}
