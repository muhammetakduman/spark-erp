package com.electrician.tracker.dto;

import java.time.LocalDate;

/** "Mustafa Akbül 29.09.2026 tarihinde 2 işe daha yazılı": other planned entries of an employee that day. */
public record EmployeeBooking(String employeeName, LocalDate date, int otherJobCount) {
}
