package com.smartcampus.api.filters;

import java.io.IOException;
import java.util.logging.Logger;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ApiLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(ApiLoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        UriInfo uriInfo = requestContext.getUriInfo();
        String method = requestContext.getMethod();
        String url = uriInfo == null ? "" : uriInfo.getRequestUri().toString();
        LOGGER.info(() -> "REQUEST " + method + " " + url);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        UriInfo uriInfo = requestContext.getUriInfo();
        String method = requestContext.getMethod();
        String url = uriInfo == null ? "" : uriInfo.getRequestUri().toString();
        int status = responseContext.getStatus();
        LOGGER.info(() -> "RESPONSE " + status + " " + method + " " + url);
    }
}
