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
import static java.util.Collections.singletonList;

@Internal
public class MessageAssertionChain {
    private final List<Link> links = new ArrayList<>();

    public void add(Link link) {
        links.add(link);
    }

    @Override
    public String toString() {
        List<String> descriptions = new ArrayList<>();
        for (Link link : links) {
            descriptions.add(link.getDescription());
        }
        return Joiner.on(System.lineSeparator()).join(descriptions);
    }

    public static Link matchesLine(final String pattern) {
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

    public static Link containsLine(String text, Object... args) {
        final String expectedLine = String.format(text, args);
        return new Link() {
            @Override
            public Result filterMatching(List<String> lines) {
                List<String> result = new ArrayList<>(lines);
                boolean matches = result.remove(expectedLine);
                return new Result(matches, result, "Lines were: " + lines);
            }

            @Override
            public String getDescription() {
                return String.format("Message contains line '%s'", expectedLine);
            }
        };
    }

    public static Link containsConsecutiveLines(final List<String> lines) {
        checkArgument(!lines.isEmpty(), "Asserting zero consecutive lines makes no sense");
        final String linesDesription = Joiner.on(System.lineSeparator()).join(lines);
        final String description = "Message contains consecutive lines " + System.lineSeparator() + linesDesription;

        return new Link() {
            @Override
            public Result filterMatching(List<String> allLines) {
                int indexOfFirstLine = allLines.indexOf(lines.get(0));
                if (indexOfFirstLine < 0) {
                    return new Result(false, allLines);
                }
                if (!lines.equals(allLines.subList(indexOfFirstLine, indexOfFirstLine + lines.size()))) {
                    return new Result(false, allLines);
                }
                List<String> remainingLines = ImmutableList.copyOf(Iterables.concat(
                        allLines.subList(0, indexOfFirstLine),
                        allLines.subList(indexOfFirstLine + lines.size(), allLines.size())));
                return new Result(true, remainingLines);
            }

            @Override
            public String getDescription() {
                return description;
            }
        };
    }

    public void evaluate(AssertionError error) {
        List<String> remainingLines = Splitter.on(System.lineSeparator()).splitToList(error.getMessage());
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
        if (result.mismatchDescription.isPresent()) {
            message += System.lineSeparator() + "But: " + result.mismatchDescription.get();
        }
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

            public static Result success(List<String> remainingLines) {
                return new Result(true, remainingLines);
            }

            public static Result failure(List<String> lines) {
                return failure(lines, "Lines were " + Joiner.on(System.lineSeparator()).join(lines));
            }

            public static Result failure(List<String> lines, String mismatchDescription, Object... args) {
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

            public static List<String> difference(List<String> list, String toSubtract) {
                return difference(list, singletonList(toSubtract));
            }

            public static List<String> difference(List<String> list, List<String> toSubtract) {
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

                public Builder containsConsecutiveLines(List<String> lines) {
                    subLinks.add(MessageAssertionChain.containsConsecutiveLines(lines));
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
                    return new Result(matches, remainingLines);
                }

                private Optional<String> append(Optional<String> description, Optional<String> part) {
                    if (!description.isPresent() || !part.isPresent()) {
                        return description.or(part);
                    }
                    return Optional.of(description.get() + System.lineSeparator() + part.get());
                }
            }
        }
    }

}
