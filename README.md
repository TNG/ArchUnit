# ArchUnit

ArchUnit is a free, simple and extensible tool for checking the architecture of your code. I.e. ArchUnit can check
dependencies between packages and classes, layers and slices, check for cyclic dependencies and more. It does so by
using the ASM library to analyse given Java Byte Code, and importing all classes into a Java code structure.
ArchUnit's sole focus is to automatically test architecture and coding rules, instead of providing ways to visualise
dependencies in a fancy graphic tool.

## Why test your architecture?

Most developers working in bigger projects will know the story, where once upon a time somebody experienced looked at
the product and drew up some nice architecture diagrams, showing the components the system should consist of, and how 
they should interact. But when the project got bigger, the use cases more complex, and new developers dropped in and
old developers dropped out, there were more and more cases where new features would just be added in any way that fit.
And suddenly everything depended on everything and every change could have an unforeseeable effect on any other
component. Of course you could have one or several experienced developers, having the role of the architect, who look
at the code once a week, identify violations and correct them. But a safer way is, to just define the components in
code and rules for these components that can be automatically tested, for example as part of your continuous integration
build.

Especially in an agile project, where the role of the architect might even be distributed, developers should all
have a common language and understanding of the components and their relations. When the project evolves, the
components you talk about, have to evolve, too. Otherwise strange constructs will suddenly appear, trying to
force use cases into a component structure that is not at all fitting. If you have automatic architecture tests,
you can evolve the rules, see where old components need to change, and ensure that new components comply to the
common understanding of the developers/architects. Altogether this will contribute to the quality of the code base and 
prevent a decline in development speed. Furthermore new developers will have a much easier time to get acquainted with
the code and get up to speed with their development.

## Why use ArchUnit?

There are several free tools out there to automatically test for dependencies between packages and classes, and 
several tools with different focus that can be used for this purpose as well, like AspectJ, Checkstyle or Findbugs.
So why would you need another tool for this?

Each of these tools has a more or less convenient way to specify rules like all packages matching '..service..' 
may not access packages matching '..controller..', and similar. Some tools, like AspectJ, enable you to specify
more powerful rules, like subclasses of class A that are annotated with @X may only access methods annotated
with @Y. But each of those tools also has some limitations you might run into, if your rules become more complex.
This might be as simple as not being able to specify a pointcut to only apply to interfaces in AspectJ or no
back references within pointcuts. Other tools aren't even able to define complex rules, like AspectJ allows, at all. 
Furthermore you might need to learn a new language to specify the rules, or need different infrastructure to evaluate
them.

For some tests of coding rules the Java Reflection API provides a convenient way to talk about your code. For
example you can test some serialization properties of the return values of methods of classes annotated with
@Remote or similar. What ArchUnit does, it striving to bring this convenience to a level of code structures instead
of mere simple classes. ArchUnit provides simple predefined ways to test the typical standard cases, like package
dependencies. But it also is fully extensible, providing a convenient way to write custom rules where imported
classes can be accessed similar to using the reflection API. In fact, the imported structure provides a natural way to
use the full power of the Reflection API for your tests. But it also allows to write tests looking at field accesses,
method or constructor calls and subclasses. Furthermore it doesn't need any special infrastructure, nor any new
language, it is plain Java and rules can be evaluated with any unit testing tool like JUnit.

## Getting started

The typical Hello World of architecture testing would be to specify package 'one' may not access package 'two'. 
A simple ArchUnit test for this could look like the following:

```Java

import static com.tngtech.archunit.lang.ArchRule.*;
import static com.tngtech.archunit.lang.conditions.ArchConditions.*;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.*;

// ...

private final ClassFileImporter importer = new ClassFileImporter();

private JavaClasses classes;

@Before
public void setUp() {
    classes = importer.importClasspath(); // imports all classes from the classpath, that are not from JARs
}

@Test
public void one_should_not_access_two() {
    all(classes.that(resideIn("..one..")))
            .should("not access classes that reside in '..two..'")
            .assertedBy(never(classAccessesPackage("..two..")));
}

// ...
```

If this rule is violated, the test will fail with an error message like

```
com.tngtech.archunit.lang.ArchAssertionError: 
Architecture Violation [Priority: MEDIUM] - 
Rule 'classes that reside in '..one..' should not access classes that reside in '..two..'' was violated:
Method <my.one.ClassInOne.illegalAccessToTwo()> calls method <my.two.ClassInTwo.doSomething()> in (ClassInOne.java:12)
Method <my.one.ClassInOne.illegalAccessToTwo()> calls constructor <my.two.ClassInTwo.<init>()> in (ClassInOne.java:11)
Method <my.one.ClassInOne.illegalAccessToTwo()> gets field <my.two.ClassInTwo.someField> in (ClassInOne.java:10)
```

The syntax to specify a rule this way, is 

```
all(<<objectsToTest>>)
    .should(<<ruleText>>)
    .assertedBy(<<conditionThatNeedsToHoldForAllObjects>>)
```

Thus `classAccessesPackage("..")` is a condition of a JavaClass, matching classes that access the package.
Consequently `never(classAccessesPackage(".."))` is again a condition matching the negative case, and the rule fails
if any element of `<<objectsToTest>>` violates the condition.

## Writing custom rules

ArchUnit comes with some predefined rules and conditions for typical use cases like accessing a field, calling a
method or accessing a package. Predefined rules can be found inside of the package `com.tngtech.archunit.library`,
while reusable conditions and predicates can be found inside of the classes 
`com.tngtech.archunit.lang.conditions.ArchConditions` and `com.tngtech.archunit.lang.conditions.ArchPredicates`
respectively. However, if the set of predefined rules is missing a specific condition necessary for a certain
architecture or code style test, it's easy to define custom conditions and rules the following way:

```Java
// assign imported classes to a variable classes

@Test
public void core_classes_shouldnt_access_remote_endpoints() {
    DescribedPredicate<JavaClass> areCore = DescribedPredicate.of(withAnnotation(Core.class))
                    .onResultOf(REFLECT).as("Classes annotated with @Core");
    
    ArchCondition<JavaClass> classAccessesTargetAnnotatedWithRemote = new ArchCondition<JavaClass>() {
        @Override
        public void check(JavaClass item, ConditionEvents events) {
            for (JavaAccess<?> access : item.getAllAccesses()) {
                if (access.getTarget().isAnnotationPresent(Remote.class)) {
                    events.add(ConditionEvent.violated(
                            "Target is annotated with @Remote where " + access.getDescription()));
                }
            }
        }
    };

    all(classes.that(areCore)).should("not call remote api endpoints")
            .assertedBy(classAccessesTargetAnnotatedWithRemote);
}
```

A resulting violation could be reported for example as

```
com.tngtech.archunit.lang.ArchAssertionError: 
Architecture Violation [Priority: MEDIUM] - Rule 'Classes annotated with @Core should not call remote api endpoints' was violated:
Target is annotated with @Remote where 
Method <com.tngtech.archunit.example.foo.SomeCoreClass.accessRemote()> 
calls method <com.tngtech.archunit.example.foo.SomeRemoteEndpoint.execute()> in (SomeCoreClass.java:6)
```

## Using ArchUnit with JUnit

The approach of the last section is inefficient in some ways. First of all, sharing rules is not as convenient as
it could be, second, and worse, the classes will be reimported on every run, which can take considerable time. To
solve this, you can use a different way to specify rules, and a custom JUnit-Runner, that will cache the imported
classes by URLs. Thus when several tests, importing classes from the same URLs, are run, the import will only happen once.

The format to specify a rule without yet passing the classes to test in, is the following:

```Java
rule(all(JavaClass.class).that(resideIn("..one..")))
        .should("not access classes that reside in '..two..'")
        .assertedBy(never(classAccessesPackage("..two..")));
```

This way the rule can easily be reused and it can be evaluated using the `ArchUnitRunner`:

```Java
@RunWith(ArchUnitRunner.class)
@AnalyseClasses(packages = {"my.pkg.one", "my.pkg.two"})
public class MyArchTest {
    @ArchTest
    public static final ArchRule<JavaClass> oneShouldntAccessTwo = 
            // This could of course easily come from a central library instead of being defined here
            rule(all(JavaClass.class).that(resideIn("..one..")))
                    .should("not access classes that reside in '..two..'")
                    .assertedBy(never(classAccessesPackage("..two..")));
}
```

Additional tests can also be specified as methods that take `JavaClasses` as input, which will result in reusing
the cached classes as well:


```Java
@RunWith(ArchUnitRunner.class)
@AnalyseClasses(packages = {"my.pkg.one", "my.pkg.two"})
public class MyArchTest {
    // ...

    @ArchTest
    public static void oneShouldntAccessTwoCouldAlsoBeSpecifiedAsMethod(JavaClasses classes) {
        all(classes).that(resideIn("..one.."))
                .should("not access classes that reside in '..two..'")
                .assertedBy(never(classAccessesPackage("..two..")));
    }
}
```

It is possible to define reusable rule sets as classes like:

```Java
public class MyArchRules {
    @ArchTest
    public static final ArchRule<JavaClass> someRuleAsField = /* definition of some rule */;
                    
    @ArchTest
    public static void anotherRuleAsMethod(JavaClasses classes) {
        /* definition of another rule */
    }
}
```

and then import those in dependent projects to easily evaluate them

```Java
@RunWith(ArchUnitRunner.class)
@AnalyseClasses(packages = {"some.pkg.of.my.project"})
public class MyArchTestThatUsesMyRules {
    @ArchTest
    public static final ArchRules<JavaClass> myArchRules = ArchRules.in(MyArchRules.class);
}
```

If this test is run, it will evaluate all rules (fields and methods) defined in `MyArchRules` against the imported
classes.

## Where to look next

Further examples can be found inside of the project `archunit-example`, including some further predefined rules
like detecting cyclic dependencies or checking for specific field accesses or method calls.