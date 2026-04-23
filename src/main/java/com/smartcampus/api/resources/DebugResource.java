package com.smartcampus.api.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("debug")
@Produces(MediaType.APPLICATION_JSON)
public class DebugResource {

    @GET
    @Path("boom")
    public String boom() {
        throw new IllegalStateException("Simulated server error (debug endpoint)");
    }
}
