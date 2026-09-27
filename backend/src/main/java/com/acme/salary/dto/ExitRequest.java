package com.acme.salary.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ExitRequest(@NotNull LocalDate lastWorkingDay) {
}
