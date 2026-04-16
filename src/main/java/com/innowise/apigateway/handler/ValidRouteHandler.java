package com.innowise.apigateway.handler;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

import static com.innowise.apigateway.constant.Routes.*;

@Component
@Getter
@Setter
public class ValidRouteHandler {

    public static final List<String> OPEN_API_ENDPOINTS = List.of(
            REGISTER_PATH, LOGIN_PATH
    );


//    private Predicate<ServerHttpRequest> isSecured = request -> openApiEndpoints.stream()
//            .noneMatch(uri -> request.getURI().getPath().contains(uri));

    public boolean isSecured(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        return OPEN_API_ENDPOINTS.stream()
                .noneMatch(path::contains);
    }

}
