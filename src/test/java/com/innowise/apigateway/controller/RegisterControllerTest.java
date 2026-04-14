package com.innowise.apigateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.apigateway.dto.RegistrationDto;
import com.innowise.apigateway.dto.UserResponseDto;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.innowise.apigateway.constant.ControllerMessage.*;

class RegisterControllerTest {

    private static MockWebServer mockBackEnd;
    private WebTestClient webTestClient;

    private final ObjectMapper objectMapper = new ObjectMapper();



    @BeforeEach
    void initialize() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();

        String baseUrl = String.format("http://localhost:%s", mockBackEnd.getPort());

        WebClient.Builder webClientBuilder = WebClient.builder()
                .baseUrl(baseUrl)
                .filter((request, next) -> {
                    java.net.URI uri = request.url();
                    java.net.URI newUri = java.net.URI.create(baseUrl + uri.getPath());
                    return next.exchange(ClientRequest.from(request)
                            .url(newUri)
                            .build());
                });

        RegisterController registerController = new RegisterController(webClientBuilder);
        this.webTestClient = WebTestClient.bindToController(registerController).build();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @Test
    void shouldRegisterSuccessfully() throws Exception {

        RegistrationDto dto = RegistrationDto.builder()
                .id(1L)
                .name("Ivan")
                .surname("Ivanov")
                .birthDate(LocalDate.of(1980, 1, 1))
                .email("test@gmail.com")
                .username("username")
                .password("password")
                .build();

        Long responseId = 100L;
        UserResponseDto userResponse = new UserResponseDto(responseId);


        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(userResponse))
                .addHeader("Content-Type", "application/json"));

        mockBackEnd.enqueue(new MockResponse().setResponseCode(200));

        webTestClient.post()
                .uri("/register")
                .bodyValue(dto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo(REGISTER_SUCCESS.formatted(responseId));

        assertEquals("/users", mockBackEnd.takeRequest().getPath());
        assertEquals("/auth/save", mockBackEnd.takeRequest().getPath());
    }

    @Test
    void shouldRollback_WhenAuthServiceFails() throws Exception {
        RegistrationDto dto = RegistrationDto.builder()
                .id(1L)
                .name("Ivan")
                .surname("Ivanov")
                .birthDate(LocalDate.of(1980, 1, 1))
                .email("test@gmail.com")
                .username("username")
                .password("password")
                .build();
        UserResponseDto userResponse = new UserResponseDto(100L);


        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(userResponse))
                .addHeader("Content-Type", "application/json"));

        mockBackEnd.enqueue(new MockResponse().setResponseCode(500));

        mockBackEnd.enqueue(new MockResponse().setResponseCode(200));

        webTestClient.post()
                .uri("/register")
                .bodyValue(dto)
                .exchange()
                .expectStatus().is5xxServerError();

        mockBackEnd.takeRequest();
        mockBackEnd.takeRequest();

        RecordedRequest deleteRequest = mockBackEnd.takeRequest();
        assertEquals("DELETE", deleteRequest.getMethod());
        assertEquals("/users/100", deleteRequest.getPath());
    }

    @Test
    void shouldFailImmediately_WhenUserServiceFails() throws Exception {
        RegistrationDto dto = RegistrationDto.builder()
                .id(1L)
                .name("Ivan")
                .surname("Ivanov")
                .birthDate(LocalDate.of(1980, 1, 1))
                .email("test@gmail.com")
                .username("username")
                .password("password")
                .build();

        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("{\"error\": \"User already exists\"}")
                .addHeader("Content-Type", "application/json"));

        webTestClient.post()
                .uri("/register")
                .bodyValue(dto)
                .exchange()
                .expectStatus().is5xxServerError();



        assertEquals(1, mockBackEnd.getRequestCount() );
        assertEquals("/users", mockBackEnd.takeRequest().getPath());
    }
}