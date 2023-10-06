package com.tngtech.archunit.testutils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.tngtech.archunit.Internal;
import com.tngtech.archunit.testutils.ExpectedRelation.LineAssociation;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class MessageAssertionChain {
    private final List<Link> links;

    MessageAssertionChain() {
        this(new ArrayList<>());
    }

    private MessageAssertionChain(List<Link> links) {
        this.links = links;
    }

    void add(Link link) {
        links.add(link);
    }

    @Override
    public String toString() {
        return links.stream().map(Link::getDescription).collect(joining(lineSeparator()));
    }

    static Link matchesLine(String pattern) {
        Pattern p = Pattern.compile(pattern);
        return new Link() {
            @Override
            public Result filterMatching(List<String> lines) {
                for (String line : lines) {
                    if (p.matcher(line).matches()) {
                        List<String> result = new ArrayList<>(lines);
                        result.remove(line);
                        return Result.success(result);
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
        String expectedLine = String.format(text, args);
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

    static Link containsText(String text, Object... args) {
        String expectedText = String.format(text, args);
        List<String> expectedLines = Splitter.on(lineSeparator()).splitToList(expectedText);
        return new Link() {
            @Override
            public Result filterMatching(List<String> lines) {
                List<String> result = new ArrayList<>(lines);
                List<Integer> matchingIndexes = findElementIndexes(result, expectedLines.get(0));
                boolean matches = false;
                for (int index : matchingIndexes) {
                    List<String> candidate = result.subList(index, index + expectedLines.size());
                    if (candidate.equals(expectedLines)) {
                        matches = true;
                        candidate.clear();
                        break;
                    }
                }
                return new Result(matches, result, describeLines(lines));
            }

            private <T> List<Integer> findElementIndexes(List<T> list, T element) {
                return IntStream.range(0, list.size())
                        .filter(i -> element.equals(list.get(i)))
                        .boxed()
                        .collect(toList());
            }

            @Override
            public String getDescription() {
                return String.format("Message contains text '%s'", expectedText);
            }
        };
    }

    private static String describeLines(List<String> lines) {
        return "Lines were >>>>>>>>" + lineSeparator() + Joiner.on(lineSeparator()).join(lines) + lineSeparator() + "<<<<<<<<";
    }

    static Link containsConsecutiveLines(List<String> expectedLines) {
        checkArgument(!expectedLines.isEmpty(), "Asserting zero consecutive lines makes no sense");
        String linesDescription = Joiner.on(lineSeparator()).join(expectedLines);
        String description = "Message contains consecutive lines " + lineSeparator() + linesDescription;

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
                return Optional.empty();
            }

            @Override
            public String getDescription() {
                return description;
            }
        };
    }

    ViolationComparisonResult evaluate(AssertionError error) {
        List<String> remainingLines = Splitter.on(lineSeparator()).splitToList(error.getMessage());
        for (Link link : links) {
            Link.Result result = link.filterMatching(remainingLines);
            if (!result.matches) {
                return ViolationComparisonResult.failure(createErrorMessage(link, result));
            }
            remainingLines = result.remainingLines;
        }
        return !remainingLines.isEmpty()
                ? ViolationComparisonResult.failure("Unexpected message lines: " + remainingLines)
                : ViolationComparisonResult.success();
    }

    private String createErrorMessage(Link link, Link.Result result) {
        String message = "Expected: " + link.getDescription();
        String mismatchDescription = result.getMismatchDescription()
                .orElse("The following lines were unexpected: " + result.remainingLines);
        message += lineSeparator() + "But: " + mismatchDescription;
        return message;
    }

    MessageAssertionChain copy() {
        return new MessageAssertionChain(links);
    }

    @Internal
    public interface Link {
        Result filterMatching(List<String> lines);

        String getDescription();

        @Internal
        class Result {
            private final boolean matches;
            private final List<String> remainingLines;
            private final String mismatchDescription;

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
                this(matches, remainingLines, null);
            }

            public Result(boolean matches, List<String> remainingLines, String mismatchDescription) {
                this.matches = matches;
                this.remainingLines = remainingLines;
                this.mismatchDescription = mismatchDescription;
            }

            Optional<String> getMismatchDescription() {
                return Optional.ofNullable(mismatchDescription);
            }

            @Internal
            public static class Builder {
                private final List<Link> subLinks = new ArrayList<>();

                Builder containsLine(String line) {
                    subLinks.add(MessageAssertionChain.containsLine(line));
                    return this;
                }

                Builder containsText(String text) {
                    subLinks.add(MessageAssertionChain.containsText(text));
                    return this;
                }

                Builder matchesLine(String pattern) {
                    subLinks.add(MessageAssertionChain.matchesLine(pattern));
                    return this;
                }

                Builder contains(Iterable<ExpectedRelation> relations) {
                    for (ExpectedRelation relation : relations) {
                        relation.associateLines(new LineAssociation() {
                            @Override
                            public void associateIfPatternMatches(String pattern) {
                                matchesLine(pattern);
                            }

                            @Override
                            public void associateIfStringIsContained(String string) {
                                containsLine(string);
                            }
                        });
                    }
                    return this;
                }

                Result build(List<String> lines) {
                    boolean matches = true;
                    List<String> remainingLines = new ArrayList<>(lines);
                    StringBuilder mismatchDescription = new StringBuilder();
                    for (Link link : subLinks) {
                        Result result = link.filterMatching(remainingLines);
                        matches = matches && result.matches;
                        remainingLines = result.remainingLines;
                        mismatchDescription.append(result.getMismatchDescription()
                                .map(d -> lineSeparator() + d)
                                .orElse(""));
                    }
                    return new Result(matches, remainingLines, mismatchDescription.toString());
                }
            }
        }
    }
}
