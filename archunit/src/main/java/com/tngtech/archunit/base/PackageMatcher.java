/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.base;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Matches packages with a syntax similar to AspectJ. In particular '*' stands for any sequence of
 * characters, '..' stands for any sequence of packages, including zero packages.<br>
 * For example
 * <ul>
 * <li><b>{@code '..pack..'}</b> matches <b>{@code 'a.pack'}</b>, <b>{@code 'a.pack.b'}</b> or <b>{@code 'a.b.pack.c.d'}</b>,
 * but not <b>{@code 'a.packa.b'}</b></li>
 * <li><b>{@code '*.pack.*'}</b> matches <b>{@code 'a.pack.b'}</b>, but not <b>{@code 'a.b.pack.c'}</b></li>
 * <li><b>{@code '..*pack*..'}</b> matches <b>{@code 'a.prepackfix.b'}</b></li>
 * <li><b>{@code '*.*.pack*..'}</b> matches <b>{@code 'a.b.packfix.c.d'}</b>,
 * but neither <b>{@code 'a.packfix.b'}</b> nor <b>{@code 'a.b.prepack.d'}</b></li>
 * </ul>
 * Furthermore the use of capturing groups is supported. In this case '(*)' matches any sequence of characters,
 * but not the dot '.', while '(**)' matches any sequence including the dot. <br>
 * For example
 * <ul>
 * <li><b>{@code '..service.(*)..'}</b> matches <b>{@code 'a.service.hello.b'}</b> and group 1 would be <b>{@code 'hello'}</b></li>
 * <li><b>{@code '..service.(**)'}</b> matches <b>{@code 'a.service.hello.more'}</b> and group 1 would be <b>{@code 'hello.more'}</b></li>
 * <li><b>{@code 'my.(*)..service.(**)'}</b> matches <b>{@code 'my.company.some.service.hello.more'}</b>
 * and group 1 would be <b>{@code 'company'}</b>, while group 2 would be <b>{@code 'hello.more'}</b></li>
 * </ul>
 * Create via {@link PackageMatcher#of(String) PackageMatcher.of(packageIdentifier)}
 */
public final class PackageMatcher {
    private static final String OPT_LETTERS_AT_START = "(?:^\\w*)?";
    private static final String OPT_LETTERS_AT_END = "(?:\\w*$)?";
    private static final String ARBITRARY_PACKAGES = "\\.(?:\\w+\\.)*";
    private static final String TWO_DOTS_REGEX = String.format("(?:%s%s%s)?", OPT_LETTERS_AT_START, ARBITRARY_PACKAGES, OPT_LETTERS_AT_END);

    private static final String TWO_STAR_CAPTURE_LITERAL = "(**)";
    private static final String TWO_STAR_CAPTURE_REGEX = "(\\w+(?:\\.\\w+)*)";
    static final String TWO_STAR_REGEX_MARKER = "#%#%#";

    private static final Set<Character> PACKAGE_CONTROL_SYMBOLS = ImmutableSet.of('*', '(', ')', '.');

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
        validateCharacters(packageIdentifier);
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
        return packageIdentifier.
                replace(TWO_STAR_CAPTURE_LITERAL, TWO_STAR_REGEX_MARKER).
                replace("*", "\\w+").
                replace(".", "\\.").
                replace(TWO_STAR_REGEX_MARKER, TWO_STAR_CAPTURE_REGEX).
                replace("\\.\\.", TWO_DOTS_REGEX);
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
     * given package name, then {@link Optional#absent()} is returned.
     *
     * @param aPackage The package name to match against
     * @return A {@link PackageMatcher.Result Result} if the package name matches,
     * otherwise {@link Optional#absent()}
     */
    @PublicAPI(usage = ACCESS)
    public Optional<Result> match(String aPackage) {
        Matcher matcher = packagePattern.matcher(aPackage);
        return matcher.matches() ? Optional.of(new Result(matcher)) : Optional.<Result>absent();
    }

    @Override
    public String toString() {
        return "PackageMatcher{" + packageIdentifier + '}';
    }

    public static final class Result {
        private final Matcher matcher;

        private Result(Matcher matcher) {
            this.matcher = matcher;
        }

        @PublicAPI(usage = ACCESS)
        public int getNumberOfGroups() {
            return matcher.groupCount();
        }

        @PublicAPI(usage = ACCESS)
        public String getGroup(int number) {
            return matcher.group(number);
        }
    }

    @PublicAPI(usage = ACCESS)
    public static final Function<Result, List<String>> TO_GROUPS = new Function<Result, List<String>>() {
        @Override
        public List<String> apply(Result input) {
            List<String> result = new ArrayList<>();
            for (int i = 0; i < input.getNumberOfGroups(); i++) {
                result.add(input.getGroup(i + 1));
            }
            return result;
        }
    };
}
