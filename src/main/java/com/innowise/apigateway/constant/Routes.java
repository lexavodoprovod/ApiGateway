package com.innowise.apigateway.constant;

public class Routes {
    public static final String REGISTER_PATH = "/auth/registration";
    public static final String LOGIN_PATH = "/auth/login";


    public static final String CREATE_USER_EUREKA_PATH = "lb://user-service/users";
    public static final String SAVE_CREDENTIAL_EUREKA_PATH = "lb://auth-service/auth/save";
    public static final String DELETE_USER_EUREKA_PATH = "lb://user-service/users/{id}";

}
