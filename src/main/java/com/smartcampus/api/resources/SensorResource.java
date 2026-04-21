package com.smartcampus.api.resources;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.smartcampus.api.model.ApiError;
import com.smartcampus.api.model.Sensor;
import com.smartcampus.api.exceptions.LinkedResourceNotFoundException;
import com.smartcampus.api.store.InMemoryStore;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final InMemoryStore store = InMemoryStore.getInstance();

    @Context
    private UriInfo uriInfo;

    @GET
    public List<Sensor> listSensors(@QueryParam("type") String type) {
        List<Sensor> all = store.listSensors();
        if (type == null || type.trim().isEmpty()) {
            return all;
        }

        String normalized = type.trim().toLowerCase(Locale.ROOT);
        return all.stream()
                .filter(s -> s.getType() != null && s.getType().trim().toLowerCase(Locale.ROOT).equals(normalized))
                .collect(Collectors.toList());
    }

    // Sub-Resource Locator: /api/v1/sensors/{sensorId}/readings
    @Path("{sensorId}/readings")
    public SensorReadingResource readings(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId, store);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor) {
        if (sensor == null || isBlank(sensor.getId()) || isBlank(sensor.getType()) || isBlank(sensor.getRoomId())) {
            return badRequest("Invalid sensor payload. Required: id, type, roomId.");
        }

        if (isBlank(sensor.getStatus())) {
            sensor.setStatus("ACTIVE");
        }

        if (!store.roomExists(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                    "room",
                    sensor.getRoomId(),
                    "roomId '%s' does not exist.".formatted(sensor.getRoomId()));
        }

        if (store.sensorExists(sensor.getId())) {
            return conflict("Sensor with id '%s' already exists.".formatted(sensor.getId()));
        }

        Sensor created = store.createSensor(sensor);
        if (created == null) {
            // defensive fallback; should be covered by checks above
            return conflict("Unable to create sensor.");
        }

        URI location = uriInfo.getAbsolutePathBuilder().path(created.getId()).build();
        return Response.created(location).entity(created).build();
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

    private String requestPath() {
        return uriInfo == null ? null : uriInfo.getRequestUri().getPath();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
