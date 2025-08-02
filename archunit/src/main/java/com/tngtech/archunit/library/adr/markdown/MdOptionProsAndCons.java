package com.tngtech.archunit.library.adr.markdown;

import com.tngtech.archunit.library.adr.OptionProsAndCons;

import java.util.List;
import java.util.Optional;

public final class MdOptionProsAndCons implements OptionProsAndCons {
    private final String title;
    private String example;
    private String description;
    private final List<String> prosAndCons;

    public MdOptionProsAndCons(final String title, final List<String> prosAndCons) {
        this.title = title;
        this.prosAndCons = prosAndCons;
    }

    @Override
    public String title() {
        return this.title;
    }

    @Override
    public Optional<String> example() {
        return Optional.ofNullable(this.example);
    }

    @Override
    public OptionProsAndCons withExample(final String example) {
        this.example = example;
        return this;
    }

    @Override
    public Optional<String> description() {
        return Optional.ofNullable(this.description);
    }

    @Override
    public OptionProsAndCons withDescription(final String description) {
        this.description = description;
        return this;
    }

    @Override
    public List<String> prosAndCons() {
        return this.prosAndCons;
    }
}
