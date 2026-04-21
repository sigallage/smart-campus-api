package com.smartcampus.api.resources;

import java.net.URI;
import java.util.List;

import com.smartcampus.api.model.ApiError;
import com.smartcampus.api.model.Room;
import com.smartcampus.api.exceptions.RoomNotEmptyException;
import com.smartcampus.api.store.InMemoryStore;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("rooms")
@Produces(MediaType.APPLICATION_JSON)
public class SensorRoomResource {

    private final InMemoryStore store = InMemoryStore.getInstance();

    @Context
    private UriInfo uriInfo;

    @GET
    public List<Room> listRooms() {
        return store.listRooms();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRoom(Room room) {
        if (room == null || isBlank(room.getId()) || isBlank(room.getName()) || room.getCapacity() <= 0) {
            return badRequest("Invalid room payload. Required: id, name, capacity>0.");
        }

        Room created = store.createRoom(room);
        if (created == null) {
            return conflict("Room with id '%s' already exists.".formatted(room.getId()));
        }

        URI location = uriInfo.getAbsolutePathBuilder().path(created.getId()).build();
        return Response.created(location).entity(created).build();
    }

    @GET
    @Path("{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            return notFound("Room '%s' not found.".formatted(roomId));
        }
        return Response.ok(room).build();
    }

    @DELETE
    @Path("{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room existing = store.getRoom(roomId);
        if (existing == null) {
            return notFound("Room '%s' not found.".formatted(roomId));
        }

        if (store.roomHasActiveSensors(roomId)) {
            throw new RoomNotEmptyException(roomId);
        }

        store.deleteRoom(roomId);
        return Response.noContent().build();
    }

    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ApiError(
                        Response.Status.BAD_REQUEST.getStatusCode(),
                        Response.Status.BAD_REQUEST.getReasonPhrase(),
                        message,
                        requestPath()))
                .build();
    }

    private Response conflict(String message) {
        return Response.status(Response.Status.CONFLICT)
                .entity(new ApiError(
                        Response.Status.CONFLICT.getStatusCode(),
                        Response.Status.CONFLICT.getReasonPhrase(),
                        message,
                        requestPath()))
                .build();
    }

    private Response notFound(String message) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ApiError(
                        Response.Status.NOT_FOUND.getStatusCode(),
                        Response.Status.NOT_FOUND.getReasonPhrase(),
                        message,
                        requestPath()))
                .build();
    }

    private String requestPath() {
        return uriInfo == null ? null : uriInfo.getRequestUri().getPath();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
