package com.netcracker.qubership.vsec.model.team;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class QSMember {
    @JsonProperty("email")
    private String email;

    @JsonProperty("full-name")
    private String fullName;

    @JsonProperty("lead-full-name")
    private String leadFullName;

    @JsonProperty("lead-email")
    private String leadEmail;

    @JsonProperty(value = "alternative-email", required = false)
    private String altEmail;
}
