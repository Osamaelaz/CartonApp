package com.cartonapp.service;

/** Temporary authentication used until the database-backed login stage. */
public final class SampleAuthenticationService implements AuthenticationService {
    @Override
    public boolean authenticate(char[] password) {
        return password != null && java.util.Arrays.equals(password, "123".toCharArray());
    }
}
