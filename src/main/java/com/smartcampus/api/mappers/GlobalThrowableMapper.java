package com.smartcampus.api.mappers;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.smartcampus.api.model.ApiError;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GlobalThrowableMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalThrowableMapper.class.getName());

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        // Never leak stack traces to clients; log internally instead.
        LOGGER.log(Level.SEVERE, "Unhandled exception", exception);

        int status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        ApiError body = new ApiError(status, "Internal Server Error", "An unexpected error occurred.", path());
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(body).build();
    }

    private String path() {
        return uriInfo == null ? null : uriInfo.getRequestUri().getPath();
    }
}
