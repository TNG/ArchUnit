package com.tngtech.archunit.base;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysFalse;
import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.base.DescribedPredicate.doesnt;
import static com.tngtech.archunit.base.DescribedPredicate.dont;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.base.DescribedPredicate.greaterThan;
import static com.tngtech.archunit.base.DescribedPredicate.greaterThanOrEqualTo;
import static com.tngtech.archunit.base.DescribedPredicate.lessThan;
import static com.tngtech.archunit.base.DescribedPredicate.lessThanOrEqualTo;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;

@RunWith(DataProviderRunner.class)
public class DescribedPredicateTest {

    @Test
    public void alwaysTrue_works() {
        assertThat(alwaysTrue().apply(new Object())).isTrue();
        assertThat(alwaysTrue().getDescription()).contains("always true");
    }

    @Test
    public void alwaysFalse_works() {
        assertThat(alwaysFalse().apply(new Object())).isFalse();
        assertThat(alwaysFalse().getDescription()).contains("always false");
    }

    @Test
    public void and_works() {
        assertThat(alwaysFalse().and(alwaysFalse()).apply(new Object())).isFalse();
        assertThat(alwaysFalse().and(alwaysTrue()).apply(new Object())).isFalse();
        assertThat(alwaysTrue().and(alwaysFalse()).apply(new Object())).isFalse();
        assertThat(alwaysTrue().and(alwaysTrue()).apply(new Object())).isTrue();
    }

    @Test
    public void or_works() {
        assertThat(alwaysFalse().or(alwaysFalse()).apply(new Object())).isFalse();
        assertThat(alwaysFalse().or(alwaysTrue()).apply(new Object())).isTrue();
        assertThat(alwaysTrue().or(alwaysFalse()).apply(new Object())).isTrue();
        assertThat(alwaysTrue().or(alwaysTrue()).apply(new Object())).isTrue();
    }

    @Test
    public void equalTo_works() {
        assertThat(equalTo(5).apply(4)).isFalse();
        assertThat(equalTo(5).getDescription()).contains("equal to '5'");
        assertThat(equalTo(5).apply(5)).isTrue();
        assertThat(equalTo(5).apply(6)).isFalse();

        Object object = new Object();
        assertThat(equalTo(object).apply(object)).isTrue();
    }

    @Test
    public void lessThan_works() {
        assertThat(lessThan(4).apply(3)).isTrue();
        assertThat(lessThan(4).getDescription()).contains("less than '4'");
        assertThat(lessThan(4).apply(4)).isFalse();
        assertThat(lessThan(4).apply(5)).isFalse();

        assertThat(lessThan(Foo.SECOND).apply(Foo.FIRST)).isTrue();
        assertThat(lessThan(Foo.SECOND).apply(Foo.SECOND)).isFalse();
        assertThat(lessThan(Foo.SECOND).apply(Foo.THIRD)).isFalse();
    }

    @Test
    public void greaterThan_works() {
        assertThat(greaterThan(5).apply(6)).isTrue();
        assertThat(greaterThan(5).getDescription()).contains("greater than '5'");
        assertThat(greaterThan(5).apply(5)).isFalse();
        assertThat(greaterThan(5).apply(4)).isFalse();

        assertThat(greaterThan(Foo.SECOND).apply(Foo.FIRST)).isFalse();
        assertThat(greaterThan(Foo.SECOND).apply(Foo.SECOND)).isFalse();
        assertThat(greaterThan(Foo.SECOND).apply(Foo.THIRD)).isTrue();
    }

    @Test
    public void lessThanOrEqualTo_works() {
        assertThat(lessThanOrEqualTo(5).apply(4)).isTrue();
        assertThat(lessThanOrEqualTo(5).getDescription()).contains("less than or equal to '5'");
        assertThat(lessThanOrEqualTo(5).apply(5)).isTrue();
        assertThat(lessThanOrEqualTo(5).apply(6)).isFalse();

        assertThat(lessThanOrEqualTo(Foo.SECOND).apply(Foo.FIRST)).isTrue();
        assertThat(lessThanOrEqualTo(Foo.SECOND).apply(Foo.SECOND)).isTrue();
        assertThat(lessThanOrEqualTo(Foo.SECOND).apply(Foo.THIRD)).isFalse();
    }

    @Test
    public void greaterThanOrEqualTo_works() {
        assertThat(greaterThanOrEqualTo(5).apply(6)).isTrue();
        assertThat(greaterThanOrEqualTo(5).getDescription()).contains("greater than or equal to '5'");
        assertThat(greaterThanOrEqualTo(5).apply(5)).isTrue();
        assertThat(greaterThanOrEqualTo(5).apply(4)).isFalse();

        assertThat(greaterThanOrEqualTo(Foo.SECOND).apply(Foo.FIRST)).isFalse();
        assertThat(greaterThanOrEqualTo(Foo.SECOND).apply(Foo.SECOND)).isTrue();
        assertThat(greaterThanOrEqualTo(Foo.SECOND).apply(Foo.THIRD)).isTrue();
    }

    @DataProvider
    public static Object[][] not_scenarios() {
        return $$(
                $(new NotScenario("not") {
                    @Override
                    <T> DescribedPredicate<T> apply(DescribedPredicate<T> input) {
                        return not(input);
                    }
                }),
                $(new NotScenario("don't") {
                    @Override
                    <T> DescribedPredicate<T> apply(DescribedPredicate<T> input) {
                        return dont(input);
                    }
                }),
                $(new NotScenario("doesn't") {
                    @Override
                    <T> DescribedPredicate<T> apply(DescribedPredicate<T> input) {
                        return doesnt(input);
                    }
                })
        );
    }

    @Test
    @UseDataProvider("not_scenarios")
    public void not_works(NotScenario scenario) {
        assertThat(scenario.apply(equalTo(5)).apply(4)).isTrue();
        assertThat(scenario.apply(equalTo(5)).getDescription()).contains(scenario.expectedPrefix + " equal to '5'");
        assertThat(scenario.apply(equalTo(5)).apply(5)).isFalse();
        assertThat(scenario.apply(equalTo(5)).apply(6)).isTrue();

        Object object = new Object();
        assertThat(scenario.apply(equalTo(object)).apply(object)).isFalse();
    }

    @Test
    public void onResultOf_works() {
        assertThat(equalTo(5).onResultOf(constant(4)).apply(new Object())).isFalse();
        assertThat(equalTo(5).onResultOf(constant(5)).apply(new Object())).isTrue();
        assertThat(equalTo(5).onResultOf(constant(6)).apply(new Object())).isFalse();
    }

    private Function<Object, Integer> constant(final int integer) {
        return new Function<Object, Integer>() {
            @Override
            public Integer apply(Object input) {
                return integer;
            }
        };
    }

    private abstract static class NotScenario {
        private final String expectedPrefix;

        NotScenario(String expectedPrefix) {
            this.expectedPrefix = expectedPrefix;
        }

        abstract <T> DescribedPredicate<T> apply(DescribedPredicate<T> input);

        @Override
        public String toString() {
            return expectedPrefix;
        }
    }

    enum Foo {
        FIRST, SECOND, THIRD
    }
}