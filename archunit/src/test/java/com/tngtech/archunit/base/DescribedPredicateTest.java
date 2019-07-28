package com.tngtech.archunit.base;

import com.google.common.collect.ImmutableList;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.base.DescribedPredicate.allElements;
import static com.tngtech.archunit.base.DescribedPredicate.alwaysFalse;
import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.base.DescribedPredicate.anyElementThat;
import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.base.DescribedPredicate.doNot;
import static com.tngtech.archunit.base.DescribedPredicate.doesNot;
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
        assertThat(alwaysTrue()).accepts(new Object()).hasDescription("always true");
    }

    @Test
    public void alwaysFalse_works() {
        assertThat(alwaysFalse()).rejects(new Object()).hasDescription("always false");
    }

    @Test
    public void and_works() {
        assertThat(alwaysFalse().and(alwaysFalse())).rejects(new Object());
        assertThat(alwaysFalse().and(alwaysTrue())).rejects(new Object());
        assertThat(alwaysTrue().and(alwaysFalse())).rejects(new Object());
        assertThat(alwaysTrue().and(alwaysTrue())).accepts(new Object());
    }

    @Test
    public void or_works() {
        assertThat(alwaysFalse().or(alwaysFalse())).rejects(new Object());
        assertThat(alwaysFalse().or(alwaysTrue())).accepts(new Object());
        assertThat(alwaysTrue().or(alwaysFalse())).accepts(new Object());
        assertThat(alwaysTrue().or(alwaysTrue())).accepts(new Object());
    }

    @Test
    public void equalTo_works() {
        assertThat(equalTo(5))
                .rejects(4)
                .hasDescription("equal to '5'")
                .accepts(5)
                .rejects(6);

        Object object = new Object();
        assertThat(equalTo(object)).accepts(object);
    }

    @Test
    public void lessThan_works() {
        assertThat(lessThan(4))
                .accepts(3)
                .hasDescription("less than '4'")
                .rejects(4)
                .rejects(5);

        assertThat(lessThan(Foo.SECOND))
                .accepts(Foo.FIRST)
                .rejects(Foo.SECOND)
                .rejects(Foo.THIRD);
    }

    @Test
    public void greaterThan_works() {
        assertThat(greaterThan(5))
                .accepts(6)
                .hasDescription("greater than '5'")
                .rejects(5)
                .rejects(4);

        assertThat(greaterThan(Foo.SECOND))
                .rejects(Foo.FIRST)
                .rejects(Foo.SECOND)
                .accepts(Foo.THIRD);
    }

    @Test
    public void lessThanOrEqualTo_works() {
        assertThat(lessThanOrEqualTo(5))
                .accepts(4)
                .hasDescription("less than or equal to '5'")
                .accepts(5)
                .rejects(6);

        assertThat(lessThanOrEqualTo(Foo.SECOND))
                .accepts(Foo.FIRST)
                .accepts(Foo.SECOND)
                .rejects(Foo.THIRD);
    }

    @Test
    public void greaterThanOrEqualTo_works() {
        assertThat(greaterThanOrEqualTo(5))
                .accepts(6)
                .hasDescription("greater than or equal to '5'")
                .accepts(5)
                .rejects(4);

        assertThat(greaterThanOrEqualTo(Foo.SECOND))
                .rejects(Foo.FIRST)
                .accepts(Foo.SECOND)
                .accepts(Foo.THIRD);
    }

    @Test
    public void describe_works() {
        Predicate<Integer> isEven = new Predicate<Integer>() {
            @Override
            public boolean apply(Integer input) {
                return input % 2 == 0;
            }
        };

        assertThat(describe("is even", isEven))
                .accepts(8)
                .hasDescription("is even")
                .accepts(4)
                .rejects(5);
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
                $(new NotScenario("do not") {
                    @Override
                    <T> DescribedPredicate<T> apply(DescribedPredicate<T> input) {
                        return doNot(input);
                    }
                }),
                $(new NotScenario("does not") {
                    @Override
                    <T> DescribedPredicate<T> apply(DescribedPredicate<T> input) {
                        return doesNot(input);
                    }
                })
        );
    }

    @Test
    @UseDataProvider("not_scenarios")
    public void not_works(NotScenario scenario) {
        assertThat(scenario.apply(equalTo(5)))
                .accepts(4)
                .hasDescription(scenario.expectedPrefix + " equal to '5'")
                .rejects(5)
                .accepts(6);

        Object object = new Object();
        assertThat(scenario.apply(equalTo(object))).rejects(object);
    }

    @Test
    public void onResultOf_works() {
        assertThat(equalTo(5).onResultOf(constant(4))).rejects(new Object());
        assertThat(equalTo(5).onResultOf(constant(5))).accepts(new Object());
        assertThat(equalTo(5).onResultOf(constant(6))).rejects(new Object());
    }

    @Test
    public void anyElementThat_works() {
        assertThat(anyElementThat(equalTo(5)))
                .hasDescription("any element that equal to '5'")
                .accepts(ImmutableList.of(5))
                .accepts(ImmutableList.of(-1, 0, 5, 6))
                .accepts(ImmutableList.of(-1, 0, 5, 5, 6))
                .rejects(ImmutableList.of(-1, 0, 6))
                .rejects(ImmutableList.<Integer>of());
    }

    @Test
    public void allElements_works() {
        assertThat(allElements(equalTo(5)))
                .hasDescription("all elements equal to '5'")
                .accepts(ImmutableList.of(5))
                .accepts(ImmutableList.of(5, 5, 5))
                .rejects(ImmutableList.of(5, 5, 6))
                .rejects(ImmutableList.of(-1, 0, 5, 6))
                .accepts(ImmutableList.<Integer>of());
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