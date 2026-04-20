package com.innowise.apigateway.controller;

import com.innowise.apigateway.dto.RegistrationDto;
import com.innowise.apigateway.dto.UserResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import static com.innowise.apigateway.constant.Routes.*;
import static com.innowise.apigateway.constant.ControllerMessage.*;

@RestController
@RequestMapping(value = "/api/v1/auth/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class RegisterController {

    private final WebClient webClient;

    public RegisterController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @PostMapping
    public Mono<ResponseEntity<String>> register(@RequestBody RegistrationDto registrationDto) {
        return webClient
                .post()
                .uri(CREATE_USER_EUREKA_PATH)
                .bodyValue(registrationDto.toUserPart())
                .retrieve()
                .bodyToMono(UserResponseDto.class)
                .flatMap(userResponse -> {
                   Long userId = userResponse.getId();

                   return webClient.post()
                           .uri(SAVE_CREDENTIAL_EUREKA_PATH)
                           .bodyValue(registrationDto.toAuthPart(userId))
                           .retrieve()
                           .toBodilessEntity()
                           .thenReturn(ResponseEntity.ok(REGISTER_SUCCESS.formatted(userId)))

                           .onErrorResume(authError ->
                               webClient.delete()
                                       .uri(DELETE_USER_EUREKA_PATH, userId)
                                       .retrieve()
                                       .toBodilessEntity()
                                       .then(Mono.error(new ResponseStatusException(
                                               HttpStatus.INTERNAL_SERVER_ERROR, AUTH_SERVICE_ERROR
                                       )))
                           );
                });
    }

}
