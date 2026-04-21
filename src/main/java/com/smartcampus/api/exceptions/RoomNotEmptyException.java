package com.smartcampus.api.exceptions;

public class RoomNotEmptyException extends RuntimeException {
    private final String roomId;

    public RoomNotEmptyException(String roomId) {
        super("Room cannot be deleted because it has ACTIVE sensors assigned.");
        this.roomId = roomId;
    }

    public String getRoomId() {
        return roomId;
    }
}
