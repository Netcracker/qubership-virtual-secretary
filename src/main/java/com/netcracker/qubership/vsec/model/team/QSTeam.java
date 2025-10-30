package com.netcracker.qubership.vsec.model.team;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class QSTeam {
    @JsonProperty("qubership-members")
    private List<QSMember> members;

    public List<QSMember> getMembers() {
        return members;
    }

    public void setMembers(List<QSMember> members) {
        this.members = members;
    }
}
