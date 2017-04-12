package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface GivenConjunction<OBJECTS> {
    @PublicAPI(usage = ACCESS)
    ArchRule should(ArchCondition<OBJECTS> condition);

    /**
     * Combines the current predicate (e.g. {@link JavaClass.Predicates#simpleName(String) simpleName} == 'SomeClass') with
     * another predicate (e.g. {@link JavaClass.Predicates#resideInAPackage(String) resideInAPackage}  'foo.bar')
     * using AND (i.e. both predicates must be satisfied).<br><br>
     * <p>
     * NOTE: {@link #and(DescribedPredicate)} and {@link #or(DescribedPredicate)} combine predicates in the
     * sequence they are declared, without any "operator precedence". I.e.
     * <br><br>
     * <pre><code>
     * all(objects()).that(predicateA).or(predicateB).and(predicateC)...
     * </code></pre>
     * <p>
     * will filter on predicate (predicateA || predicateB) && predicateC, and
     * <br><br>
     * <pre><code>
     * all(objects()).that(predicateA).and(predicateB).or(predicateC)...
     * </code></pre>
     * <p>
     * will filter on predicate (predicateA && predicateB) || predicateC. If you need more control over the
     * precedence, how predicates are joined, you have to join these predicates separately, i.e.
     * <br><br>
     * <pre><code>
     * all(objects()).that(predicateA.or(predicateB.and(predicateC)))...
     * </code></pre>
     * <br>
     *
     * @param predicate The predicate to be ANDed on the current object filter predicate
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    GivenConjunction<OBJECTS> and(DescribedPredicate<? super OBJECTS> predicate);

    /**
     * Combines the current predicate (e.g. {@link JavaClass.Predicates#simpleName(String) simpleName} == 'SomeClass')
     * with another predicate (e.g. {@link JavaClass.Predicates#resideInAPackage(String) resideInAPackage} 'foo.bar')
     * using OR (i.e. at least one of the predicates must be satisfied).<br><br>
     * <p>
     * NOTE: For considerations about precedence, when joining predicates, consider note at
     * {@link #and(DescribedPredicate)}
     *
     * @param predicate The predicate to be ORed on the current object filter predicate
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    GivenConjunction<OBJECTS> or(DescribedPredicate<? super OBJECTS> predicate);
}
