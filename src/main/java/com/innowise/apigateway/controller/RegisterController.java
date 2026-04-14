package com.innowise.apigateway.controller;

import com.innowise.apigateway.dto.RegistrationDto;
import com.innowise.apigateway.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RegisterController {


    private final WebClient.Builder webClientBuilder;

    @PostMapping
    public Mono<ResponseEntity<String>> register(@RequestBody RegistrationDto registrationDto) {

        WebClient webClient = webClientBuilder.build();

        return webClient
                .post()
                .uri("lb://user-service/users")
                .bodyValue(registrationDto.toUserPart())
                .retrieve()
                .bodyToMono(UserResponseDto.class)
                .flatMap(userResponse -> {
                    System.out.println(userResponse.toString());
                   Long userId = userResponse.getId();

                   return webClient.post()
                           .uri("lb://auth-service/auth/save")
                           .bodyValue(registrationDto.toAuthPart(userId))
                           .retrieve()
                           .toBodilessEntity()
                           .thenReturn(ResponseEntity.ok("Registered Successfully!"))

                           .onErrorResume(authError -> {
                               System.err.println("AuthService error: " + authError.getMessage());
                               return webClient.delete()
                                       .uri("lb://user-service/users/{id}", userId)
                                       .retrieve()
                                       .toBodilessEntity()
                                       .then(Mono.error(new ResponseStatusException(
                                               HttpStatus.INTERNAL_SERVER_ERROR, "Error in AuthService, all data was deleted from UserService"
                                       )));
                           });
                });
    }

}
