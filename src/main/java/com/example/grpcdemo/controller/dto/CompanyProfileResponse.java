package com.example.grpcdemo.controller.dto;

import java.util.List;

/**
 * Aggregated view returned to the enterprise portal when displaying the
 * company overview screen.
 */
public class CompanyProfileResponse {

    private String companyId;
    private EnterpriseCompanyInfoDto company;
    private List<HrContactDto> hrContacts;

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public EnterpriseCompanyInfoDto getCompany() {
        return company;
    }

    public void setCompany(EnterpriseCompanyInfoDto company) {
        this.company = company;
    }

    public List<HrContactDto> getHrContacts() {
        return hrContacts;
    }

    public void setHrContacts(List<HrContactDto> hrContacts) {
        this.hrContacts = hrContacts;
    }
}
