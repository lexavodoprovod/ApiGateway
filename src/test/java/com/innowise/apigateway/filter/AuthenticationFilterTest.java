package com.innowise.apigateway.filter;

import com.innowise.apigateway.handler.ValidRouteHandler;
import com.innowise.apigateway.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ValidRouteHandler routeHandler;

    @Mock
    private GatewayFilterChain chain;

    @InjectMocks
    private AuthenticationFilter filter;

    private static final String VALID_TOKEN = "valid.jwt.token";

    @BeforeEach
    void setup() {
        when(routeHandler.isSecured(any())).thenReturn(true);
    }

    @Test
    void shouldSkipFilter_WhenRouteIsNotSecured() {
        when(routeHandler.isSecured(any())).thenReturn(false);
        when(chain.filter(any())).thenReturn(Mono.empty());


        MockServerHttpRequest request = MockServerHttpRequest.get("/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = filter.apply(new Object()).filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(exchange);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void shouldReturn401_WhenAuthHeaderIsMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/users/1").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = filter.apply(new Object()).filter(exchange, chain);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException
                        && ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.UNAUTHORIZED)
                .verify();
    }

    @Test
    void shouldReturn401_WhenHeaderFormatIsInvalid() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/users/1")
                .header(HttpHeaders.AUTHORIZATION, "Basic user:pass") // Не Bearer
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = filter.apply(new Object()).filter(exchange, chain);

        StepVerifier.create(result)
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    void shouldPass_WhenTokenIsValid() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/users/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(any())).thenReturn(Mono.empty());


        doNothing().when(jwtUtil).validateToken(VALID_TOKEN);

        Mono<Void> result = filter.apply(new Object()).filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        verify(jwtUtil).validateToken(VALID_TOKEN);
        verify(chain).filter(exchange);
    }

    @Test
    void shouldReturnError_WhenTokenIsInvalid() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/users/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(any())).thenReturn(Mono.empty());


        doThrow(new JwtException("Expired")).when(jwtUtil).validateToken(VALID_TOKEN);

        Mono<Void> result = filter.apply(new Object()).filter(exchange, chain);

        StepVerifier.create(result)
                .expectError(JwtException.class)
                .verify();
    }
}