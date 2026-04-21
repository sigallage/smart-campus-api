package com.smartcampus.api.mappers;

import com.smartcampus.api.exceptions.RoomNotEmptyException;
import com.smartcampus.api.model.ApiError;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(RoomNotEmptyException exception) {
        int status = Response.Status.CONFLICT.getStatusCode();
        ApiError body = new ApiError(status, "Conflict", exception.getMessage(), path());
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(body).build();
    }

    private String path() {
        return uriInfo == null ? null : uriInfo.getRequestUri().getPath();
    }
}
