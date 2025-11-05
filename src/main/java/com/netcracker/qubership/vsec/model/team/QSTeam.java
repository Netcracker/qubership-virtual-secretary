package com.netcracker.qubership.vsec.model.team;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.stream.Collectors;

public class QSTeam {
    @JsonProperty("qubership-members")
    private List<QSMember> members;

    public List<QSMember> getMembers() {
        return members;
    }

    public void setMembers(List<QSMember> members) {
        this.members = members;
    }

    /**
     * Returns list of unique emails of all team members
     */
    public List<String> getAllEmails() {
        return members.stream().map(QSMember::getEmail).collect(Collectors.toList());
    }
}
