/*
 * Copyright 2017 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.junit;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.tngtech.archunit.Internal;

import static com.google.common.base.Preconditions.checkArgument;
import static com.tngtech.archunit.junit.MessageAssertionChain.Link.Result.difference;
import static java.lang.System.lineSeparator;
import static java.util.Collections.singletonList;

public class MessageAssertionChain {
    private final List<Link> links = new ArrayList<>();

    void add(Link link) {
        links.add(link);
    }

    @Override
    public String toString() {
        List<String> descriptions = new ArrayList<>();
        for (Link link : links) {
            descriptions.add(link.getDescription());
        }
        return Joiner.on(lineSeparator()).join(descriptions);
    }

    static Link matchesLine(final String pattern) {
        final Pattern p = Pattern.compile(pattern);
        return new Link() {
            @Override
            public Result filterMatching(List<String> lines) {
                for (String line : lines) {
                    if (p.matcher(line).matches()) {
                        return Result.success(difference(lines, line));
                    }
                }
                return Result.failure(lines);
            }

            @Override
            public String getDescription() {
                return String.format("Message has line matching '%s'", pattern);
            }
        };
    }

    static Link containsLine(String text, Object... args) {
        final String expectedLine = String.format(text, args);
        return new Link() {
            @Override
            public Result filterMatching(List<String> lines) {
                List<String> result = new ArrayList<>(lines);
                boolean matches = result.remove(expectedLine);
                return new Result(matches, result, describeLines(lines));
            }

            @Override
            public String getDescription() {
                return String.format("Message contains line '%s'", expectedLine);
            }
        };
    }

    private static String describeLines(List<String> lines) {
        return "Lines were >>>>>>>>" + lineSeparator() + Joiner.on(lineSeparator()).join(lines) + lineSeparator() + "<<<<<<<<";
    }

    static Link containsConsecutiveLines(final List<String> expectedLines) {
        checkArgument(!expectedLines.isEmpty(), "Asserting zero consecutive lines makes no sense");
        final String linesDescription = Joiner.on(lineSeparator()).join(expectedLines);
        final String description = "Message contains consecutive lines " + lineSeparator() + linesDescription;

        return new Link() {
            @Override
            public Result filterMatching(List<String> allLines) {
                int indexOfFirstLine = allLines.indexOf(expectedLines.get(0));
                if (indexOfFirstLine < 0) {
                    return new Result(false, allLines, String.format("Couldn't find line '%s'", expectedLines.get(0)));
                }

                List<String> linesToAnalyze = allLines.subList(indexOfFirstLine, indexOfFirstLine + expectedLines.size());
                Optional<String> lineMismatch = findMismatch(expectedLines, linesToAnalyze);
                if (lineMismatch.isPresent()) {
                    return new Result(false, allLines, lineMismatch.get());
                }
                List<String> remainingLines = ImmutableList.copyOf(Iterables.concat(
                        allLines.subList(0, indexOfFirstLine),
                        allLines.subList(indexOfFirstLine + expectedLines.size(), allLines.size())));
                return new Result(true, remainingLines);
            }

            private Optional<String> findMismatch(List<String> expectedLines, List<String> linesToAnalyze) {
                for (int i = 0; i < expectedLines.size(); i++) {
                    String checkLine = linesToAnalyze.size() > i ? linesToAnalyze.get(i) : null;
                    if (!expectedLines.get(i).equals(checkLine)) {
                        return Optional.of(String.format("Expected line '%s', but was %s",
                                expectedLines.get(i), checkLine != null ? "'" + checkLine + "'" : "<empty>"));
                    }
                }
                return Optional.absent();
            }

            @Override
            public String getDescription() {
                return description;
            }
        };
    }

    void evaluate(AssertionError error) {
        List<String> remainingLines = Splitter.on(lineSeparator()).splitToList(error.getMessage());
        for (Link link : links) {
            Link.Result result = link.filterMatching(remainingLines);
            if (!result.matches) {
                throw new AssertionError(createErrorMessage(link, result));
            }
            remainingLines = result.remainingLines;
        }
        if (!remainingLines.isEmpty()) {
            throw new AssertionError("Unexpected message lines: " + remainingLines);
        }
    }

    private String createErrorMessage(Link link, Link.Result result) {
        String message = "Expected: " + link.getDescription();
        String mismatchDescription = result.mismatchDescription
                .or("The following lines were unexpected: " + result.remainingLines);
        message += lineSeparator() + "But: " + mismatchDescription;
        return message;
    }

    @Internal
    public interface Link {
        Result filterMatching(List<String> lines);

        String getDescription();

        @Internal
        class Result {
            private final boolean matches;
            private final List<String> remainingLines;
            private final Optional<String> mismatchDescription;

            static Result success(List<String> remainingLines) {
                return new Result(true, remainingLines);
            }

            static Result failure(List<String> lines) {
                return failure(lines, describeLines(lines));
            }

            static Result failure(List<String> lines, String mismatchDescription, Object... args) {
                return new Result(false, lines, String.format(mismatchDescription, args));
            }

            public Result(boolean matches, List<String> remainingLines) {
                this(matches, remainingLines, Optional.<String>absent());
            }

            public Result(boolean matches, List<String> remainingLines, String mismatchDescription) {
                this(matches, remainingLines, Optional.of(mismatchDescription));
            }

            private Result(boolean matches, List<String> remainingLines, Optional<String> mismatchDescription) {
                this.mismatchDescription = mismatchDescription;
                this.matches = matches;
                this.remainingLines = remainingLines;
            }

            static List<String> difference(List<String> list, String toSubtract) {
                return difference(list, singletonList(toSubtract));
            }

            static List<String> difference(List<String> list, List<String> toSubtract) {
                List<String> result = new ArrayList<>(list);
                result.removeAll(toSubtract);
                return result;
            }

            @Internal
            public static class Builder {
                private final List<Link> subLinks = new ArrayList<>();

                public Builder containsLine(String line) {
                    subLinks.add(MessageAssertionChain.containsLine(line));
                    return this;
                }

                public Builder matchesLine(String pattern) {
                    subLinks.add(MessageAssertionChain.matchesLine(pattern));
                    return this;
                }

                public Result build(List<String> lines) {
                    boolean matches = true;
                    List<String> remainingLines = new ArrayList<>(lines);
                    Optional<String> mismatchDescription = Optional.absent();
                    for (Link link : subLinks) {
                        Result result = link.filterMatching(remainingLines);
                        matches = matches && result.matches;
                        remainingLines = result.remainingLines;
                        mismatchDescription = append(mismatchDescription, result.mismatchDescription);
                    }
                    return new Result(matches, remainingLines, mismatchDescription);
                }

                private Optional<String> append(Optional<String> description, Optional<String> part) {
                    if (!description.isPresent() || !part.isPresent()) {
                        return description.or(part);
                    }
                    return Optional.of(description.get() + lineSeparator() + part.get());
                }
            }
        }
    }
}
