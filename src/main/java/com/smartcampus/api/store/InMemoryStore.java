package com.smartcampus.api.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.smartcampus.api.model.Room;
import com.smartcampus.api.model.Sensor;
import com.smartcampus.api.model.SensorReading;

public final class InMemoryStore {
    private static final InMemoryStore INSTANCE = new InMemoryStore();

    private final ConcurrentMap<String, Room> rooms = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CopyOnWriteArrayList<SensorReading>> readingsBySensorId = new ConcurrentHashMap<>();

    private InMemoryStore() {
    }

    public static InMemoryStore getInstance() {
        return INSTANCE;
    }

    public List<Room> listRooms() {
        return new ArrayList<>(rooms.values());
    }

    public Room getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public boolean roomExists(String roomId) {
        return rooms.containsKey(roomId);
    }

    public Room createRoom(Room room) {
        Objects.requireNonNull(room, "room");
        Room previous = rooms.putIfAbsent(room.getId(), room);
        return previous == null ? room : null;
    }

    public Room deleteRoom(String roomId) {
        return rooms.remove(roomId);
    }

    public boolean roomHasActiveSensors(String roomId) {
        for (Sensor sensor : sensors.values()) {
            if (roomId.equals(sensor.getRoomId()) && isActive(sensor)) {
                return true;
            }
        }
        return false;
    }

    private boolean isActive(Sensor sensor) {
        if (sensor == null || sensor.getStatus() == null) {
            return false;
        }
        return "ACTIVE".equalsIgnoreCase(sensor.getStatus());
    }

    public Sensor createSensor(Sensor sensor) {
        Objects.requireNonNull(sensor, "sensor");

        Room room = rooms.get(sensor.getRoomId());
        if (room == null) {
            return null;
        }

        Sensor previous = sensors.putIfAbsent(sensor.getId(), sensor);
        if (previous != null) {
            // duplicate sensor id
            return null;
        }

        // Link sensor to room (best-effort, but keep it consistent)
        synchronized (room) {
            if (room.getSensorIds() == null) {
                room.setSensorIds(new ArrayList<>());
            }
            if (!room.getSensorIds().contains(sensor.getId())) {
                room.getSensorIds().add(sensor.getId());
            }
        }

        return sensor;
    }

    public boolean sensorExists(String sensorId) {
        return sensors.containsKey(sensorId);
    }

    public Sensor getSensor(String sensorId) {
        return sensors.get(sensorId);
    }

    public List<Sensor> listSensors() {
        return new ArrayList<>(sensors.values());
    }

    public List<SensorReading> listReadings(String sensorId) {
        CopyOnWriteArrayList<SensorReading> list = readingsBySensorId.get(sensorId);
        if (list == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(list);
    }

    public SensorReading appendReading(String sensorId, SensorReading reading) {
        Objects.requireNonNull(sensorId, "sensorId");
        Objects.requireNonNull(reading, "reading");

        Sensor sensor = sensors.get(sensorId);
        if (sensor == null) {
            return null;
        }

        readingsBySensorId.computeIfAbsent(sensorId, k -> new CopyOnWriteArrayList<>()).add(reading);

        // Side-effect required by spec: update currentValue on the parent Sensor
        sensor.setCurrentValue(reading.getValue());
        return reading;
    }
}
