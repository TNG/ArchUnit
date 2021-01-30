package com.tngtech.archunit.testutil.assertion;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;

import static com.google.common.collect.Iterables.cycle;
import static com.google.common.collect.Iterables.limit;

class DescriptionContext {
    private static final String MARKER = "##MARKER##";
    private static final String PLACEHOLDER = "_";

    private final String context;
    private final String description;
    private final String currentElement;
    private final String joinString;

    DescriptionContext(String context) {
        this(context + MARKER, "assertion", "", ", ");
    }

    private DescriptionContext(String context, String description, String currentElement, String joinString) {
        this.context = context;
        this.description = description;
        this.currentElement = currentElement;
        this.joinString = joinString;
    }

    public DescriptionContext describe(String part) {
        return new DescriptionContext(context.replace(MARKER, part + MARKER), description, part, joinString);
    }

    public DescriptionContext describeUpperBounds() {
        String newContext = context.replace(MARKER, " extends " + MARKER);
        return new DescriptionContext(newContext, description, currentElement, " & ");
    }

    public DescriptionContext describeLowerBounds() {
        String newContext = context.replace(MARKER, " super " + MARKER);
        return new DescriptionContext(newContext, description, currentElement, " & ");
    }

    public DescriptionContext describeElements(int number) {
        String elementsPlaceHolder = number > 0 ? joinedPlaceHolders(number) : "[]";
        return new DescriptionContext(context.replace(MARKER, elementsPlaceHolder), description, currentElement, joinString);
    }

    public DescriptionContext describeElement(int index, int totalSize) {
        int maxIndex = totalSize - 1;
        String prefix = index > 0 ? joinedPlaceHolders(index) + joinString : "";
        String suffix = index < maxIndex ? joinString + joinedPlaceHolders(maxIndex - index) : "";
        String newContext = context.replace(MARKER, prefix + MARKER + suffix);
        String newCurrentElement = this.currentElement + "[" + index + "]";
        return new DescriptionContext(newContext, description, newCurrentElement, joinString);
    }

    private String joinedPlaceHolders(int number) {
        return FluentIterable.from(limit(cycle(PLACEHOLDER), number)).join(Joiner.on(joinString));
    }

    public DescriptionContext step(String description) {
        return new DescriptionContext(context, description, currentElement, joinString);
    }

    public DescriptionContext metaInfo() {
        String newContext = context.replace(MARKER, "{" + MARKER + "}");
        return new DescriptionContext(newContext, description, currentElement, joinString);
    }

    public DescriptionContext describeTypeParameters() {
        String newContext = context.replace(MARKER, "<" + MARKER + ">");
        String newJoinString = ", ";
        return new DescriptionContext(newContext, description, currentElement, newJoinString);
    }

    @Override
    public String toString() {
        String currentElementInfix = currentElement.isEmpty() ? "" : "[" + currentElement + "]";
        return "\"" + description + "\"" + currentElementInfix + " -> " + context.replace(MARKER, "");
    }
}
