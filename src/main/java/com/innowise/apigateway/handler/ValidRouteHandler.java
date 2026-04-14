package com.innowise.apigateway.handler;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

import static com.innowise.apigateway.constant.Routes.*;

@Component
public class ValidRouteHandler {

    public static final List<String> openApiEndpoints = List.of(
            REGISTER_PATH, LOGIN_PATH
    );


    public Predicate<ServerHttpRequest> isSecured = request -> openApiEndpoints.stream()
            .noneMatch(uri -> request.getURI().getPath().contains(uri));

}
