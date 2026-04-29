package com.innowise.apigateway.filter;

import com.innowise.apigateway.config.AuthenticationConfig;
import com.innowise.apigateway.handler.ValidRouteHandler;
import com.innowise.apigateway.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;


import static com.innowise.apigateway.constant.TokenInfo.*;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationConfig> {



    private final JwtUtil jwtUtil;
    private final ValidRouteHandler validRouteHandler;

    public AuthenticationFilter(JwtUtil jwtUtil, ValidRouteHandler validRouteHandler) {
        super(AuthenticationConfig.class);
        this.jwtUtil = jwtUtil;
        this.validRouteHandler = validRouteHandler;
    }


    @Override
    public GatewayFilter apply(AuthenticationConfig config) {
        return ((exchange, chain) -> {

            if(!config.isEnabled()) {
                return chain.filter(exchange);
            }

            ServerHttpRequest request = exchange.getRequest();

            if(!validRouteHandler.isSecured(request)) {
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst(JWT_HEADER_NAME);

            if(authHeader == null || !authHeader.startsWith(JWT_HEADER_PREFIX)){
                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Header Format"));
            }

            String token = authHeader.substring(JWT_HEADER_PREFIX.length());



            return Mono.fromRunnable(() -> jwtUtil.validateToken(token))
                    .then(chain.filter(exchange))
                    .onErrorResume(Mono::error);
        });
    }
}
