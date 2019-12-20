package com.tngtech.archunit.library.freeze;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class ViolationLineMatcherFactoryTest {
    @Test
    @DataProvider(splitBy = "\\|", value = {
            "" +
                    "|" +
                    "|" + true,
            "" +
                    "abc|" +
                    "abc|" + true,
            "" +
                    "abc|" +
                    "abcd|" + false,
            "" +
                    "(A.java:1)|" +
                    "(A.java:2)|" + true,
            "" +
                    "A.java:1|" +
                    "A.java:2|" + false,
            "" +
                    "(A.java:1)|" +
                    "(A.java:2|" + false,
            "" +
                    "A$1 B$2 C$4 (X.java:111)|" +
                    "A$2 B$3 C$5 (X.java:222)|" + true,
            "" +
                    "A$a|" +
                    "A$b|" + false,
            "" +
                    "A:1|" +
                    "A$2|" + false,
            "" +
                    "Method <MyClass.lambda$myFunction$2()> has a violation in (MyClass.java:123)|" +
                    "Method <MyClass.lambda$myFunction$123()> has a violation in (MyClass.java:0)|" + true,
            "" +
                    "Method <C.lambda$myFunction$2()> is bad in (C.java:123)|" +
                    "Method <C.lambda$myFunction$2()> is bad in (C.java:123), too|" + false,
            "" +
                    "A:1) B$2 C|" +  // limitation of the current implementation:
                    "A: B$ C|" + true,  // false positive
    })
    public void default_matcher(String str1, String str2, boolean expected) {
        ViolationLineMatcher defaultMatcher = ViolationLineMatcherFactory.create();
        assertThat(defaultMatcher.matches(str1, str2))
                .as(String.format("'%s' matches '%s'", str1, str2))
                .isEqualTo(expected);
    }
}
