package com.example.grpcdemo.security;

import java.io.Serializable;

public record AuthenticatedUser(String userId, String email, boolean devToken) implements Serializable {
}

