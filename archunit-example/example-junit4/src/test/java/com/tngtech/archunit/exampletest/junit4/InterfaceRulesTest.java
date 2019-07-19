package com.tngtech.archunit.exampletest.junit4;

import java.util.HashSet;
import java.util.Set;

import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.core.importer.Locations;
import com.tngtech.archunit.example.layers.SomeBusinessInterface;
import com.tngtech.archunit.example.layers.persistence.first.dao.SomeDao;
import com.tngtech.archunit.example.layers.service.impl.SomeInterfacePlacedInTheWrongPackage;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.junit.LocationProvider;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@Category(Example.class)
@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(locations = InterfaceRulesTest.RelevantExampleClasses.class)
public class InterfaceRulesTest {

    @ArchTest
    public static final ArchRule interfaces_should_not_have_names_ending_with_the_word_interface =
            noClasses().that().areInterfaces().should().haveNameMatching(".*Interface");

    @ArchTest
    public static final ArchRule interfaces_should_not_have_simple_class_names_containing_the_word_interface =
            noClasses().that().areInterfaces().should().haveSimpleNameContaining("Interface");

    @ArchTest
    public static final ArchRule interfaces_must_not_be_placed_in_implementation_packages =
            noClasses().that().resideInAPackage("..impl..").should().beInterfaces();

    public static class RelevantExampleClasses implements LocationProvider {
        @Override
        public Set<Location> get(Class<?> testClass) {
            Set<Location> result = new HashSet<>();
            result.addAll(Locations.ofClass(SomeBusinessInterface.class));
            result.addAll(Locations.ofClass(SomeDao.class));
            result.addAll(Locations.ofClass(SomeInterfacePlacedInTheWrongPackage.class));
            return result;
        }
    }
}
