package com.tngtech.archunit.testutils;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class ExpectedMessage implements MessageAssertionChain.Link {
    private final String expectedMessage;

    ExpectedMessage(String expectedMessage) {
        this.expectedMessage = expectedMessage;
    }

    @Override
    public Result filterMatching(List<String> lines) {
        List<String> rest = lines.stream().filter(line -> !expectedMessage.equals(line)).collect(toList());
        boolean matches = !lines.equals(rest);
        return new Result(matches, rest);
    }

    @Override
    public String getDescription() {
        return "Message: " + expectedMessage;
    }

    public static ExpectedMessage violation(String message) {
        return new ExpectedMessage(message);
    }
}
