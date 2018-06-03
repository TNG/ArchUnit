package com.tngtech.archunit.testutils;

import java.util.ArrayList;
import java.util.List;

class ExpectedMessage implements MessageAssertionChain.Link {
    private final String expectedMessage;

    ExpectedMessage(String expectedMessage) {
        this.expectedMessage = expectedMessage;
    }

    @Override
    public Result filterMatching(List<String> lines) {
        List<String> rest = new ArrayList<>();
        for (String line : lines) {
            if (!expectedMessage.equals(line)) {
                rest.add(line);
            }
        }
        boolean matches = !lines.equals(rest);
        return new Result(matches, rest);
    }

    @Override
    public String getDescription() {
        return "Message: " + expectedMessage;
    }
}
