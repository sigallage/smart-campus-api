package com.smartcampus.api;

import java.io.IOException;
import java.net.URI;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

public final class Main {
    private static final URI BASE_URI = URI.create("http://0.0.0.0:8080/");

    private Main() {
    }

    public static HttpServer startServer() {
        return GrizzlyHttpServerFactory.createHttpServer(BASE_URI, new SmartCampusApplication());
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = startServer();
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));
        System.out.println("Smart Campus API started: " + BASE_URI + "api/v1");
        System.out.println("Press Ctrl+C to stop.");

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
