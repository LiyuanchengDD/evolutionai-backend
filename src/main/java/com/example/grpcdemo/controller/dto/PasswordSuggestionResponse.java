package com.example.grpcdemo.controller.dto;

/**
 * Simple wrapper returning a generated strong password.
 */
public class PasswordSuggestionResponse {

    private String password;

    public PasswordSuggestionResponse() {
    }

    public PasswordSuggestionResponse(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
