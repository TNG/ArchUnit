package com.tngtech.archunit.core.domain;

import java.util.Optional;

import com.tngtech.archunit.core.domain.PackageMatcher.Result;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.core.domain.PackageMatcher.TO_GROUPS;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;

@RunWith(DataProviderRunner.class)
public class PackageMatcherTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    @DataProvider(value = {
            "some.arbitrary.pkg , some.arbitrary.pkg             , true",
            "some.arbitrary.pkg , some.thing.different           , false",
            "some..pkg          , some.arbitrary.pkg             , true",
            "some..middle..pkg  , some.arbitrary.middle.more.pkg , true",
            "*..pkg             , some.arbitrary.pkg             , true",
            "some..*            , some.arbitrary.pkg             , true",
            "*..pkg             , some.arbitrary.pkg.toomuch     , false",
            "toomuch.some..*    , some.arbitrary.pkg             , false",
            "*..wrong           , some.arbitrary.pkg             , false",
            "some..*            , wrong.arbitrary.pkg            , false",
            "..some             , some                           , true",
            "some..             , some                           , true",
            "*..some            , some                           , false",
            "some..*            , some                           , false",
            "..some             , asome                          , false",
            "some..             , somea                          , false",
            "*.*.*              , wrong.arbitrary.pkg            , true",
            "*.*.*              , wrong.arbitrary.pkg.toomuch    , false",
            "some.arbi*.pk*..   , some.arbitrary.pkg.whatever    , true",
            "some.arbi*..       , some.brbitrary.pkg             , false",
            "some.*rary.*kg..   , some.arbitrary.pkg.whatever    , true",
            "some.*rary..       , some.arbitrarz.pkg             , false",
            "some.pkg           , someepkg                       , false",
            "..pkg..            , some.random.pkg.maybe.anywhere , true",
            "..p..              , s.r.p.m.a                      , true",
            "*..pkg..*          , some.random.pkg.maybe.anywhere , true",
            "*..p..*            , s.r.p.m.a                      , true",
            "..[a|b|c].pk*..    , some.a.pkg.whatever            , true",
            "..[b|c].pk*..      , some.a.pkg.whatever            , false",
            "..[a|b*].pk*..     , some.bitrary.pkg.whatever      , true",
            "..[a|b*].pk*..     , some.a.pkg.whatever            , true",
            "..[a|b*].pk*..     , some.arbitrary.pkg.whatever    , false",
            "..[*c*|*d*].pk*..  , some.anydinside.pkg.whatever   , true",
            "..[*c*|*d*].pk*..  , some.nofit.pkg.whatever        , false",
    })
    public void match(String matcher, String target, boolean matches) {
        assertThat(PackageMatcher.of(matcher).matches(target))
                .as("package matches")
                .isEqualTo(matches);
    }

    @Test
    @DataProvider(value = {
            "some.(*).pkg , some.arbitrary.pkg , arbitrary",
            "some.arb(*)ry.pkg , some.arbitrary.pkg , itra",
            "some.arb(*)ry.pkg , some.arbit.rary.pkg , null",
            "some.(*).matches.(*).pkg , some.first.matches.second.pkg , first:second",
            "(*).matches.(*) , start.matches.end , start:end",
            "(*).(*).(*).(*) , a.b.c.d , a:b:c:d",
            "(*) , some , some",
            "some.(*).pkg , some.in.between.pkg , null",
            "some.(**).pkg , some.in.between.pkg , in.between",
            "some.(**).pkg.(*) , some.in.between.pkg.addon , in.between:addon",
            "some(**)pkg , somerandom.in.between.longpkg , random.in.between.long",
            "some.(**).pkg , somer.in.between.pkg , null",
            "some.(**).pkg , some.in.between.gpkg , null",
            "so(*)me.(**)pkg.an(*).more , soinfme.in.between.gpkg.and.more , inf:in.between.g:d",
            "so(*)me.(**)pkg.an(*).more , soinfme.in.between.gpkg.an.more , null",
            "so(**)me , some , null",
            "so(*)me , some , null",
            "(**)so , awe.some.aso , awe.some.a",
            "so(**) , soan.some.we , an.some.we",
            "..(a|b).pk*.(c|d).. , some.a.pkg.d.whatever , a:d",
            "..(a|b).[p|*g].(c|d).. , some.a.pkg.d.whatever , a:d",
            "..[a|b|c].pk*.(c|d).. , some.c.pkg.d.whatever , d",
            "..(a|b*|cd).pk*.(**).end , some.bitrary.pkg.in.between.end, bitrary:in.between",
            "..(a.b|c.d).pk* , some.a.b.pkg , a.b",
            "..[application|domain.*|infrastructure].(*).. , com.example.application.a.http , a",
            "..[application|domain.*|infrastructure].(*).. , com.example.domain.api.a , a",
            "..[application|domain.*|infrastructure].(*).. , com.example.domain.logic.a , a",
            "..[application|domain.*|infrastructure].(*).. , com.example.infrastructure.a.file , a"
    })
    public void capture_groups(String matcher, String target, String groupString) {
        assertThat(PackageMatcher.of(matcher).match(target).isPresent())
                .as("'%s' matching '%s'", matcher, target)
                .isEqualTo(groupString != null);

        String[] groups = groupString != null ? groupString.split(":") : new String[0];
        for (int i = 0; i < groups.length; i++) {
            assertThat(PackageMatcher.of(matcher).match(target).get().getGroup(i + 1))
                    .as("group number %d matches when matching '%s' against '%s'", i + 1, matcher, target)
                    .isEqualTo(groups[i]);
        }
    }

    @Test
    public void should_reject_more_than_two_dots_in_a_row() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Package Identifier may not contain more than two '.' in a row");

        PackageMatcher.of("some...pkg");
    }

    @Test
    public void should_reject_more_than_one_star_in_a_row() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Package Identifier may not contain more than one '*' in a row");

        PackageMatcher.of("some**package");
    }

    @Test
    public void should_reject_capturing_with_two_dots() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Package Identifier does not support capturing via (..), use (**) instead");

        PackageMatcher.of("some.(..).package");
    }

    @Test
    public void should_reject_non_alternating_alternatives() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Package Identifier does not allow alternation brackets '[]' without specifying any alternative via '|' inside");

        PackageMatcher.of("some.[nonalternating].package");
    }

    @Test
    public void should_reject_toplevel_alternations() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Package Identifier only supports '|' inside of '[]' or '()'");

        PackageMatcher.of("some.pkg|other.pkg");
    }

    @DataProvider
    public static Object[][] data_reject_nesting_of_groups() {
        return testForEach(
                "some.(pkg.(other).pkg)..",
                "some.(pkg.[a|b].pkg)..",
                "some.[inside.(pkg).it|other.(pkg).it].pkg",
                "some.[inside.[a|b].it|other].pkg"
        );
    }

    @Test
    @UseDataProvider
    public void test_reject_nesting_of_groups(String packageIdentifier) {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Package Identifier does not support nesting '()' or '[]' within other '()' or '[]'");

        PackageMatcher.of(packageIdentifier);
    }

    @Test
    public void should_reject_illegal_characters() {
        String illegalPackageIdentifier = "some" + PackageMatcher.TWO_STAR_REGEX_MARKER + "package";

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(String.format(
                "Package Identifier '%s' may only consist of valid java identifier parts or the symbols '.)(*'", illegalPackageIdentifier));

        PackageMatcher.of(illegalPackageIdentifier);
    }

    @Test
    public void transform_match_to_groups() {
        Optional<Result> result = PackageMatcher.of("com.(*)..service.(**)")
                .match("com.mycompany.some.service.special.name");

        assertThat(result.map(TO_GROUPS).get()).contains("mycompany", "special.name");
    }

    @Test
    public void transform_mismatch_to_absent() {
        Optional<Result> result = PackageMatcher.of("com.(*)..").match("mycompany");

        assertThat(result).isEmpty();
    }
}
