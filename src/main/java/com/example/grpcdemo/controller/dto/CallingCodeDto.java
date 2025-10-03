package com.example.grpcdemo.controller.dto;

/**
 * DTO exposing international calling code options.
 */
public class CallingCodeDto {

    private String countryCode;
    private String countryName;
    private String callingCode;

    public CallingCodeDto() {
    }

    public CallingCodeDto(String countryCode, String countryName, String callingCode) {
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.callingCode = callingCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getCallingCode() {
        return callingCode;
    }

    public void setCallingCode(String callingCode) {
        this.callingCode = callingCode;
    }
}
