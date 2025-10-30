package com.netcracker.qubership.vsec.jobs.impl_act.weekly_reports;

import com.netcracker.qubership.vsec.model.team.QSMember;

import java.time.LocalDate;

public class ReportWarning {
    private final LocalDate date;
    private final QSMember person;

    public ReportWarning(LocalDate date, QSMember person) {
        this.date = date;
        this.person = person;
    }

    public LocalDate getDate() {
        return date;
    }

    public QSMember getPerson() {
        return person;
    }
}
