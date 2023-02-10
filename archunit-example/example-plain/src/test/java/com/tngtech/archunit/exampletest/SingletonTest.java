package com.tngtech.archunit.exampletest;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.singleton.SingletonClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * @author Per Lundberg
 */
@Category(Example.class)
public class SingletonTest {
    private static final JavaClasses classes = new ClassFileImporter().importPackagesOf( SingletonClass.class);

    @Test
    public void getInstance_is_not_used_from_inside_methods() {
        methods()
                .that().haveName( "getInstance" )
                .and().areStatic()

                // Note: this is a convoluted way to say "no parameters".
                .and().haveRawParameterTypes( new String[0] )

                .should( onlyBeCalledFromWhitelistedOrigins(
                        // The class below will trigger a violation unless present as a parameter here.
                        "com.tngtech.archunit.example.singleton.SingletonClassWhitelistedInvalidConsumer"
                ) )
                .because( "" +
                        "getInstance() calls should not be spread out through the methods of a class. This makes it hard/impossible " +
                        "to override the dependencies for tests, and also means the dependencies are much harder to identify when " +
                        "quickly looking at the code. Instead, move all getInstance() calls to the INSTANCE supplier and pass the " +
                        "dependency to the constructor that way. If this is impossible for a particular case, add the class name to " +
                        "the whitelist in " + getClass().getName() )
                .check( classes );
    }

    private ArchCondition<JavaMethod> onlyBeCalledFromWhitelistedOrigins( String... whitelistedOrigins ) {
        return new ArchCondition<JavaMethod>( "only be called by whitelisted methods" ) {
            @Override
            public void check( JavaMethod method, ConditionEvents events ) {
                method.getCallsOfSelf().stream()
                        // TODO: Add your own exceptions as needed here, if you have particular
                        // TODO: use cases where getInstance() calls are permissible.
                        // Static getInstance() methods are always allowed to call getInstance. This
                        // does not break dependency injection and does not come with significant
                        // design flaws.
                        .filter( call -> !( Objects.equals( call.getOrigin().getName(), "getInstance" ) && call.getOrigin().getModifiers().contains( JavaModifier.STATIC ) ) )

                        // Anything not handled by the exceptions above must be explicitly listed in
                        // the whitelistedOrigins parameter.
                        .filter( call -> {
                            Optional<String> result = Arrays.stream( whitelistedOrigins )
                                    .filter( o -> call.getOrigin().getFullName().startsWith( o ) )
                                    .findFirst();

                            return !result.isPresent();
                        } )
                        .forEach( call -> events.add( SimpleConditionEvent.violated( method, call.getDescription() ) ) );
            }
        };
    }
}
