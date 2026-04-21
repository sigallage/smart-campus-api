package com.smartcampus.api;

import com.smartcampus.api.filters.ApiLoggingFilter;
import com.smartcampus.api.mappers.GlobalThrowableMapper;
import com.smartcampus.api.mappers.LinkedResourceNotFoundExceptionMapper;
import com.smartcampus.api.mappers.RoomNotEmptyExceptionMapper;
import com.smartcampus.api.mappers.SensorUnavailableExceptionMapper;
import com.smartcampus.api.resources.DiscoveryResource;
import com.smartcampus.api.resources.SensorResource;
import com.smartcampus.api.resources.SensorRoomResource;
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/api/v1")
public class SmartCampusApplication extends ResourceConfig {
    public SmartCampusApplication() {
        register(JacksonFeature.class);

        // Observability
        register(ApiLoggingFilter.class);

        // Exception mapping (Part 5)
        register(RoomNotEmptyExceptionMapper.class);
        register(LinkedResourceNotFoundExceptionMapper.class);
        register(SensorUnavailableExceptionMapper.class);
        register(GlobalThrowableMapper.class);

        register(DiscoveryResource.class);
        register(SensorRoomResource.class);
        register(SensorResource.class);
    }
}
