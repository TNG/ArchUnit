/*
 * Copyright 2014-2026 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.core.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static java.util.stream.Collectors.toList;

/**
 * Matches packages with a syntax similar to AspectJ. In particular
 * <ul>
 *     <li><b>{@code *}</b> stands for any non-empty sequence of characters but not the dot '.', while</li>
 *     <li><b>{@code ..}</b> stands for any sequence of packages, including zero packages.</li>
 * </ul>
 * For example
 * <ul>
 *    <li>{@code "..pack.."} matches {@code a.pack}, {@code a.pack.b} or {@code a.b.pack.c.d},
 *        but not {@code a.packa.b}</li>
 *    <li>{@code "*.pack.*"} matches {@code a.pack.b},
 *        but not {@code a.b.pack.c}</li>
 *    <li>{@code "..*pack*.."} matches {@code a.prepackfix.b},
 *        but not {@code a.prepack.b}</li>
 *    <li>{@code "*.*.pack*.."} matches {@code a.b.packfix.c.d},
 *        but neither {@code a.packfix.b} nor {@code a.b.prepack.d}</li>
 * </ul>
 * <p>
 * You can also use alternations with the <b>{@code |}</b> operator within brackets. For example
 * <ul>
 *    <li>{@code "pack.[a.c|b*].d"} matches {@code pack.a.c.d} or {@code pack.bar.d},
 *        but neither {@code pack.a.d} nor {@code pack.b.c.d}</li>
 * </ul>
 * <p>
 * Furthermore, the use of capturing groups is supported:
 * <ul>
 *     <li><b>{@code (*)}</b> matches any sequence of characters, * but not the dot '.', while</li>
 *     <li><b>{@code (**)}</b> matches any sequence including the dot.</li>
 * </ul>
 * For example
 * <ul>
 *     <li>{@code "..service.(*).."} matches {@code a.service.hello.b},
 *         and group 1 would be {@code "hello"}</li>
 *     <li>{@code "..service.(**)"} matches {@code a.service.hello.more},
 *         and group 1 would be {@code "hello.more"}</li>
 *     <li>{@code "my.(*)..service.(**)"} matches {@code my.company.some.service.hello.more},
 *         group 1 would be {@code "company"}, and group 2 would be {@code "hello.more"}</li>
 *     <li>{@code "..service.(a|b*).."} matches {@code a.service.bar.more},
 *         and group 1 would be {@code "bar"}</li>
 * </ul>
 *The segments matched by capturing groups can be retrieved from the result of {@link #match(String)}
 * via {@link Result#getGroup(int)}.
 *
 * @see PackageMatcher#of(String) PackageMatcher.of(packageIdentifier)
 */
@PublicAPI(usage = ACCESS)
public final class PackageMatcher {
    private static final String OPT_LETTERS_AT_START = "(?:^\\w*)?";
    private static final String OPT_LETTERS_AT_END = "(?:\\w*$)?";
    private static final String ARBITRARY_PACKAGES = "\\.(?:\\w+\\.)*";
    private static final String TWO_DOTS_REGEX = String.format("(?:%s%s%s)?", OPT_LETTERS_AT_START, ARBITRARY_PACKAGES, OPT_LETTERS_AT_END);

    private static final String TWO_STAR_CAPTURE_LITERAL = "(**)";
    private static final String TWO_STAR_CAPTURE_REGEX = "(\\w+(?:\\.\\w+)*)";
    static final String TWO_STAR_REGEX_MARKER = "#%#%#";

    private static final Pattern ILLEGAL_ALTERNATION_PATTERN = Pattern.compile("\\[[^|]*]");
    private static final Pattern ILLEGAL_NESTED_GROUP_PATTERN = Pattern.compile(
            nestedGroupRegex('(', ')', '(')
                    + "|" + nestedGroupRegex('(', ')', '[')
                    + "|" + nestedGroupRegex('[', ']', '(')
                    + "|" + nestedGroupRegex('[', ']', '['));

    private static final Set<Character> PACKAGE_CONTROL_SYMBOLS = ImmutableSet.of('*', '(', ')', '.', '|', '[', ']');

    private final String packageIdentifier;
    private final Pattern packagePattern;

    private PackageMatcher(String packageIdentifier) {
        validate(packageIdentifier);

        this.packageIdentifier = packageIdentifier;
        this.packagePattern = Pattern.compile(convertToRegex(packageIdentifier));
    }

    private void validate(String packageIdentifier) {
        if (packageIdentifier.contains("...")) {
            throw new IllegalArgumentException("Package Identifier may not contain more than two '.' in a row");
        }
        if (packageIdentifier.replace("(**)", "").contains("**")) {
            throw new IllegalArgumentException("Package Identifier may not contain more than one '*' in a row");
        }
        if (packageIdentifier.contains("(..)")) {
            throw new IllegalArgumentException("Package Identifier does not support capturing via (..), use (**) instead");
        }
        if (ILLEGAL_ALTERNATION_PATTERN.matcher(packageIdentifier).find()) {
            throw new IllegalArgumentException(
                    "Package Identifier does not allow alternation brackets '[]' without specifying any alternative via '|' inside");
        }
        if (containsToplevelAlternation(packageIdentifier)) {
            throw new IllegalArgumentException(
                    "Package Identifier only supports '|' inside of '[]' or '()'");
        }
        if (ILLEGAL_NESTED_GROUP_PATTERN.matcher(packageIdentifier).find()) {
            throw new IllegalArgumentException("Package Identifier does not support nesting '()' or '[]' within other '()' or '[]'");
        }
        validateCharacters(packageIdentifier);
    }

    private boolean containsToplevelAlternation(String packageIdentifier) {
        Stream<String> prefixesBeforeAlternation = IntStream.range(0, packageIdentifier.length())
                .filter(i -> packageIdentifier.charAt(i) == '|')
                .mapToObj(alternationIndex -> packageIdentifier.substring(0, alternationIndex));

        return prefixesBeforeAlternation.anyMatch(beforeAlternation ->
                sameNumberOfOccurrences(beforeAlternation, '(', ')')
                        && sameNumberOfOccurrences(beforeAlternation, '[', ']'));
    }

    private boolean sameNumberOfOccurrences(String string, char first, char second) {
        return CharMatcher.is(first).countIn(string) == CharMatcher.is(second).countIn(string);
    }

    private void validateCharacters(String packageIdentifier) {
        for (int i = 0; i < packageIdentifier.length(); i++) {
            char c = packageIdentifier.charAt(i);
            if (!Character.isJavaIdentifierPart(c) && !PACKAGE_CONTROL_SYMBOLS.contains(c)) {
                throw new IllegalArgumentException(
                        String.format("Package Identifier '%s' may only consist of valid java identifier parts or the symbols '.)(*'",
                                packageIdentifier));
            }
        }
    }

    private String convertToRegex(String packageIdentifier) {
        return packageIdentifier
                .replaceAll("\\[(.*?)]", "(?:$1)") // replacing all '[..|..]' with '(?:..|..)'
                .replace(TWO_STAR_CAPTURE_LITERAL, TWO_STAR_REGEX_MARKER)
                .replace("*", "\\w+")
                .replace(".", "\\.")
                .replace(TWO_STAR_REGEX_MARKER, TWO_STAR_CAPTURE_REGEX)
                .replace("\\.\\.", TWO_DOTS_REGEX);
    }

    /**
     * Creates a new {@link PackageMatcher}
     *
     * @param packageIdentifier The package literal to match against (e.g. {@code 'some*..pk*'} --&gt; {@code 'somewhere.in.some.pkg'})
     * @return {@link PackageMatcher} to match packages against the supplied literal
     * supporting AspectJ syntax
     */
    @PublicAPI(usage = ACCESS)
    public static PackageMatcher of(String packageIdentifier) {
        return new PackageMatcher(packageIdentifier);
    }

    @PublicAPI(usage = ACCESS)
    public boolean matches(String aPackage) {
        return packagePattern.matcher(aPackage).matches();
    }

    /**
     * Returns a matching {@link PackageMatcher.Result Result}
     * against the provided package name. If the package identifier of this {@link PackageMatcher} does not match the
     * given package name, then {@link Optional#empty()} is returned.
     *
     * @param aPackage The package name to match against
     * @return A {@link PackageMatcher.Result Result} if the package name matches,
     * otherwise {@link Optional#empty()}
     */
    @PublicAPI(usage = ACCESS)
    public Optional<Result> match(String aPackage) {
        Matcher matcher = packagePattern.matcher(aPackage);
        return matcher.matches() ? Optional.of(new Result(matcher)) : Optional.empty();
    }

    @Override
    public String toString() {
        return "PackageMatcher{" + packageIdentifier + '}';
    }

    private static String nestedGroupRegex(char outerOpeningChar, char outerClosingChar, char nestedOpeningChar) {
        return "\\" + outerOpeningChar + "[^" + outerClosingChar + "]*\\" + nestedOpeningChar;
    }

    @PublicAPI(usage = ACCESS)
    public static final class Result {
        private final Matcher matcher;

        private Result(Matcher matcher) {
            this.matcher = matcher;
        }

        @PublicAPI(usage = ACCESS)
        public int getNumberOfGroups() {
            return matcher.groupCount();
        }

        /**
         * @param number 1-based number of the capturing group
         * @return The subsequence captured by the given group during the previous match operation.
         * @throws IndexOutOfBoundsException if there is no capturing group in the pattern with the given number.
         */
        @PublicAPI(usage = ACCESS)
        public String getGroup(int number) {
            return matcher.group(number);
        }
    }

    @PublicAPI(usage = ACCESS)
    public static final Function<Result, List<String>> TO_GROUPS = input ->
            IntStream.rangeClosed(1, input.getNumberOfGroups())
                    .mapToObj(input::getGroup)
                    .collect(toList());
}
