package com.cartonapp.service;

/** Provides the application login check. */
public interface AuthenticationService {
    boolean authenticate(char[] password);
}
