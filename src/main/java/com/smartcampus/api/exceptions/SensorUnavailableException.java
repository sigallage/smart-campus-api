package com.smartcampus.api.exceptions;

public class SensorUnavailableException extends RuntimeException {
    private final String sensorId;

    public SensorUnavailableException(String sensorId) {
        super("Sensor is unavailable and cannot accept new readings while in MAINTENANCE.");
        this.sensorId = sensorId;
    }

    public String getSensorId() {
        return sensorId;
    }
}
