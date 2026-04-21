package com.smartcampus.api.mappers;

import com.smartcampus.api.exceptions.LinkedResourceNotFoundException;
import com.smartcampus.api.model.ApiError;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        int status = 422;
        ApiError body = new ApiError(status, "Unprocessable Entity", exception.getMessage(), path());
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(body).build();
    }

    private String path() {
        return uriInfo == null ? null : uriInfo.getRequestUri().getPath();
    }
}
