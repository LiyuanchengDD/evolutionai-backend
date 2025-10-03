package com.example.grpcdemo.controller.dto;

/**
 * Response wrapper returned after creating or updating an HR contact. It
 * optionally surfaces a freshly generated password so the UI can display it
 * once.
 */
public class HrContactResponse {

    private HrContactDto contact;
    private String password;

    public HrContactDto getContact() {
        return contact;
    }

    public void setContact(HrContactDto contact) {
        this.contact = contact;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
