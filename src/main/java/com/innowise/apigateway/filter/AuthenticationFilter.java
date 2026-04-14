package com.innowise.apigateway.filter;

import com.innowise.apigateway.handler.ValidRouteHandler;
import com.innowise.apigateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthenticationFilter extends AbstractGatewayFilterFactory {

    private static final String JWT_HEADER_PREFIX = "Bearer ";


    private final JwtUtil jwtUtil;
    private final ValidRouteHandler validRouteHandler;

    @Override
    public GatewayFilter apply(Object config) {
        return ((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if(!validRouteHandler.isSecured.test(request)){
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if(authHeader == null || !authHeader.startsWith(JWT_HEADER_PREFIX)){
                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Header Format"));
            }

            String token = authHeader.substring(JWT_HEADER_PREFIX.length());



            return Mono.fromRunnable(() -> jwtUtil.validateToken(token))
                    .then(chain.filter(exchange))
                    .onErrorResume(e -> Mono.error(e));
        });
    }
}
