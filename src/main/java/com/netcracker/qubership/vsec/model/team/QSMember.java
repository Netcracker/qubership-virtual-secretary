package com.netcracker.qubership.vsec.model.team;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QSMember {
    @JsonProperty("email")
    private String email;

    @JsonProperty("full-name")
    private String fullName;

    @JsonProperty("lead-full-name")
    private String leadFullName;

    @JsonProperty("lead-email")
    private String leadEmail;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getLeadFullName() {
        return leadFullName;
    }

    public void setLeadFullName(String leadFullName) {
        this.leadFullName = leadFullName;
    }

    public String getLeadEmail() {
        return leadEmail;
    }

    public void setLeadEmail(String leadEmail) {
        this.leadEmail = leadEmail;
    }
}
