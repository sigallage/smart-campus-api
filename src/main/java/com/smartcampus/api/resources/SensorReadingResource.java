package com.smartcampus.api.resources;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.smartcampus.api.model.ApiError;
import com.smartcampus.api.model.Sensor;
import com.smartcampus.api.model.SensorReading;
import com.smartcampus.api.exceptions.SensorUnavailableException;
import com.smartcampus.api.store.InMemoryStore;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Produces(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final InMemoryStore store;

    @Context
    private UriInfo uriInfo;

    public SensorReadingResource(String sensorId, InMemoryStore store) {
        this.sensorId = sensorId;
        this.store = store;
    }

    @GET
    public Response listReadings() {
        if (!store.sensorExists(sensorId)) {
            return notFound("Sensor '%s' not found.".formatted(sensorId));
        }
        List<SensorReading> readings = store.listReadings(sensorId);
        return Response.ok(readings).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response appendReading(SensorReading reading) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            return notFound("Sensor '%s' not found.".formatted(sensorId));
        }

        if (sensor.getStatus() != null && "MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId);
        }

        if (reading == null) {
            return badRequest("Invalid reading payload.");
        }

        if (reading.getId() == null || reading.getId().trim().isEmpty()) {
            reading.setId(UUID.randomUUID().toString());
        }

        if (reading.getTimestamp() <= 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        if (Double.isNaN(reading.getValue()) || Double.isInfinite(reading.getValue())) {
            return badRequest("Reading value must be a finite number.");
        }

        SensorReading created = store.appendReading(sensorId, reading);

        URI location = uriInfo == null
                ? null
                : uriInfo.getAbsolutePathBuilder().path(created.getId()).build();

        if (location == null) {
            return Response.status(Response.Status.CREATED).entity(created).build();
        }
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
}
