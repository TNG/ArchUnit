package com.tngtech.archunit.lang;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.collect.ImmutableSet;

public class FailureMessages implements Iterable<String> {
    private final Set<String> failureMessages = new TreeSet<>();

    public boolean isEmpty() {
        return failureMessages.isEmpty();
    }

    public void add(String message) {
        failureMessages.add(message);
    }

    @Override
    public Iterator<String> iterator() {
        return ImmutableSet.copyOf(failureMessages).iterator();
    }
}
