package com.smartcampus.api.resources;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Context;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @Context
    private UriInfo uriInfo;

    @GET
    public Map<String, Object> getDiscovery() {
        String base = uriInfo.getBaseUri().toString();
        // base ends with "/api/v1/"; ensure consistent link building
        if (!base.endsWith("/")) {
            base = base + "/";
        }

        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("role", "Lead Backend Architect");
        contact.put("email", "admin@smartcampus.example");

        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms", base + "rooms");
        resources.put("sensors", base + "sensors");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("name", "Smart Campus Sensor & Room Management API");
        response.put("version", "v1");
        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("contact", contact);
        response.put("resources", resources);
        return response;
    }
}
