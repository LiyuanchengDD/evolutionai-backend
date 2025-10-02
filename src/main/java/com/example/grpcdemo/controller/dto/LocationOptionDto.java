package com.example.grpcdemo.controller.dto;

/**
 * Simple key/label pair used for country and city dropdowns.
 */
public class LocationOptionDto {

    private String code;
    private String name;

    public LocationOptionDto() {
    }

    public LocationOptionDto(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
