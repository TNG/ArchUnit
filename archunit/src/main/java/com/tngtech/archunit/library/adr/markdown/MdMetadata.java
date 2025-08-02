package com.tngtech.archunit.library.adr.markdown;

import com.tngtech.archunit.library.adr.Metadata;

import java.util.List;
import java.util.Optional;

public final class MdMetadata implements Metadata {

    private String status;
    private String date;
    private List<String> decisionMakers;
    private List<String> consulted;
    private List<String> informed;

    @Override
    public Optional<String> status() {
        return Optional.ofNullable(this.status);
    }

    @Override
    public Metadata withStatus(final String status) {
        this.status = status;
        return this;
    }

    @Override
    public Optional<String> date() {
        return Optional.ofNullable(this.date);
    }

    @Override
    public Metadata withDate(final String date) {
        this.date = date;
        return this;
    }

    @Override
    public Optional<List<String>> decisionMakers() {
        return Optional.ofNullable(this.decisionMakers);
    }

    @Override
    public Metadata withDecisionMakers(final List<String> decisionMakers) {
        this.decisionMakers = decisionMakers;
        return this;
    }

    @Override
    public Optional<List<String>> consulted() {
        return Optional.ofNullable(this.consulted);
    }

    @Override
    public Metadata withConsulted(final List<String> consulted) {
        this.consulted = consulted;
        return this;
    }

    @Override
    public Optional<List<String>> informed() {
        return Optional.ofNullable(this.informed);
    }

    @Override
    public Metadata withInformed(final List<String> informed) {
        this.informed = informed;
        return this;
    }
}
