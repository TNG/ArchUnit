[![Build Status](https://travis-ci.org/TNG/ArchUnit.png?branch=master)](https://travis-ci.org/TNG/ArchUnit)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.tngtech.archunit/archunit/badge.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.tngtech.archunit%22%20)
[![License](https://img.shields.io/github/license/TNG/ArchUnit.svg)](https://github.com/TNG/ArchUnit/blob/master/LICENSE.txt)

<img src="logo/ArchUnit-Logo.png" height="64" alt="ArchUnit">

ArchUnit is a free, simple and extensible library for checking the architecture of your Java code. That is, ArchUnit can check
dependencies between packages and classes, layers and slices, check for cyclic dependencies and more. It does so by
analyzing given Java bytecode, importing all classes into a Java code structure.
ArchUnit's main focus is to automatically test architecture and coding rules, using any plain Java unit testing
framework.

## TL;DR

If you want to dive right into the first ArchUnit test using JUnit, follow these steps

1) ArchUnit can be obtained from Maven Central
```
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit</artifactId>
    <version>0.4.0</version>
    <scope>test</scope>
</dependency>
```
2) Create a JUnit test
```java
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "com.mycompany.myapp")
class MyArchitectureTest {
    @ArchTest
    public static final ArchRule myRule = 
            classes()...
}
```
3) Let the API guide you
![ArchUnit Fluent API](ArchUnit-API.gif)

## Why test your architecture?

Most developers working in larger projects will know the story, where once upon a time somebody experienced looked at
the code and drew up some nice architecture diagrams, showing the components the system should consist of, and how
they should interact. But when the project got bigger, the use cases more complex, and new developers dropped in and
old developers dropped out, there were more and more cases where new features would just be added in any way that fit.
And suddenly everything depended on everything and every change could have an unforeseeable effect on any other
component. Of course you could have one or several experienced developers, having the role of the architect, who look
at the code once a week, identify violations and correct them. But a safer way is to just define the components in
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
several tools with different focus that can be used for this purpose as well, like [AspectJ](https://eclipse.org/aspectj/),
[Checkstyle](http://checkstyle.sourceforge.net/) or [FindBugs](http://findbugs.sourceforge.net/).
So why would you need another tool for this?

Each of these tools has a more or less convenient way to specify rules like packages matching '..service..'
may not access packages matching '..controller..', and similar. Some tools, like AspectJ, enable you to specify
more powerful rules, like subclasses of class A that are annotated with @X may only access methods annotated
with @Y. But each of those tools also has some limitations you might run into, if your rules become more complex.
This might be as simple as not being able to specify a pointcut to only apply to interfaces in AspectJ or no
back references within pointcuts. Other tools are not even able to define complex rules, like AspectJ allows, at all. 
Furthermore, you might need to learn a new language to specify the rules, or need different infrastructure to evaluate
them.

For some tests of coding rules the Java Reflection API provides a convenient way to talk about your code. For
example you can test some serialization properties of the return values of methods of classes annotated with
@Remote or similar. ArchUnit strives to bring this convenience to a level of code structures instead
of mere simple classes. ArchUnit provides simple predefined ways to test the typical standard cases, like package
dependencies. But it also is fully extensible, providing a convenient way to write custom rules where imported
classes can be accessed similarly to using the Reflection API. In fact, the imported structure provides a natural way to
use the full power of the Reflection API for your tests. But it also allows to write tests looking at field accesses,
method or constructor calls and subclasses. Furthermore, it does not need any special infrastructure, nor any new
language, it is plain Java and rules can be evaluated with any unit testing tool like [JUnit](http://junit.org/).

## Getting started

The typical _Hello World_ of architecture testing would be to specify package 'one' may not access package 'two'.
A simple ArchUnit test for this could look like the following:

```Java
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

// ...

private final ClassFileImporter importer = new ClassFileImporter();

private JavaClasses classes;

@Before
public void importClasses() {
    classes = importer.importClasspath(); // imports all classes from the classpath that are not from JARs
}

@Test
public void one_should_not_access_two() {
    ArchRule rule = noClasses().that().resideInAPackage("..one..")
        .should().accessClassesThat().resideInAPackage("..two.."); // The '..' represents a wildcard for any number of packages

    rule.check(classes);
}

// ...
```

If this rule is violated, the test will fail with an error message like

```
java.lang.AssertionError: Architecture Violation [Priority: MEDIUM] - 
Rule 'no classes that reside in a package '..one..' should access classes that reside in a package '..two..'' was violated:
Method <my.one.ClassInOne.illegalAccessToTwo()> calls method <my.two.ClassInTwo.doSomething()> in (ClassInOne.java:12)
Method <my.one.ClassInOne.illegalAccessToTwo()> calls constructor <my.two.ClassInTwo.<init>()> in (ClassInOne.java:11)
Method <my.one.ClassInOne.illegalAccessToTwo()> gets field <my.two.ClassInTwo.someField> in (ClassInOne.java:10)
```

## Writing custom rules

ArchUnit comes with many predefined syntax elements like `classes().that().are...` or 
`classes().should().accessField(..)`
for typical use cases like accessing a field, calling a method or accessing a package. However, if the predefined syntax
is missing a specific syntax element for a certain architecture or coding test, it is easy to define custom 
predicates and conditions to extend rules in the following way:

```Java
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

// assign imported classes to a variable classes

@Test
public void core_classes_should_not_access_remote_endpoints() {
    DescribedPredicate<JavaClass> belongToCore = new DescribedPredicate<JavaClass>("belong to core"){
        @Override
        public boolean apply(JavaClass input) {
            return input.getPackage().contains(".core.") || input.isAnnotatedWith(Core.class);
        }
    };

    ArchCondition<JavaClass> notCallRemoteApiEndpoints = 
        new ArchCondition<JavaClass>("not call remote api endpoints") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                for (JavaAccess<?> access : item.getAllAccessesFromSelf()) {
                    if (access.getTarget().isAnnotatedWith(Remote.class)) {
                        events.add(SimpleConditionEvent.violated(access,
                            "Target is annotated with @Remote where " + access.getDescription()));
                    }
                }
            }
        };

    classes().that(belongToCore).should(notCallRemoteApiEndpoints).check(classes);
}
```

A resulting violation could be reported for example as

```
java.lang.AssertionError: 
Architecture Violation [Priority: MEDIUM] - Rule 'classes that belong to core should not call remote api endpoints' was violated:
Target is annotated with @Remote where 
Method <com.tngtech.archunit.example.core.SomeCoreClass.accessRemote()> 
calls method <com.tngtech.archunit.example.foo.SomeRemoteEndpoint.execute()> in (SomeCoreClass.java:6)
```

## Predefined rules and elements

By convention a lot of predefined `DescribedPredicate`s can be found within various static inner classes named `Predicates`
within the ArchUnit core class that the respective predicate targets. For example there is
`JavaClass.Predicates.simpleName(String)` to match `JavaClasses` by their simple name. Likewise there is
`HasName.Predicates.name(String)` to match any class implementing `HasName` by their name. This can for example be used
on a `JavaClass` to match the fully qualified class name or on a `JavaMethod` to match the method name.

Predicates can be joined using the methods `and(..)` and `or(..)`, e.g. 

```Java
DescribedPredicate<JavaClass> nameAndAnnotationMatches = simpleName("Foo").and(annotatedWith(Bar.class))
```

Further, more complex predefined rules can be found inside of the package `com.tngtech.archunit.library`, for example
to check package slice dependencies and cycles or conveniently specify layered architectures.

## Adding a reason

While it is not strictly necessary, it is strongly encouraged to add a reason to rules that are not self-explanatory.
Not only will it raise acceptance, if people see their code as a cause of failing tests, but it will also document,
why this rule was once introduced. `ArchRule`s offer a simple way to add a `because(..)` clause to your rule:

```Java
classes().that().areAnnotatedWith(GuiComponent.class)
    .should().onlyBeAccessed().byClassesThat().areAssignableTo(GuiComponentProxyCreator.class)
    .because("our GUI components must be proxied on access to ensure platform independence");
```

The resulting failure message will now be extended

```
java.lang.AssertionError: Architecture Violation [Priority: MEDIUM] - 
Rule 'classes that are annotated with @GuiComponent should only be accessed by classes that are assignable
to some.pkg.GuiComponentProxyCreator, because our GUI components must be proxied on access to ensure
platform independence' was violated:
...
```

## Using ArchUnit with JUnit 4

The approach of the last section is inefficient, because the classes will be reimported on every run, 
which can take considerable time. To solve this, you can use a different way to declare rules, and a custom 
JUnit runner that will cache the imported classes by URLs. Thus when several tests, importing classes from the same
URLs, are run, the import will only happen once.

With JUnit 4, `ArchRule`s can be evaluated as fields using the `ArchUnitRunner`:

```Java
@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = {"my.pkg.one", "my.pkg.two"})
public class MyArchTest {
    @ArchTest
    public static final ArchRule one_shouldnt_access_two = 
        // This could of course easily come from a central library instead of being defined here
        noClasses().that().resideInAPackage("..one..")
            .should().accessClassesThat().resideInAPackage("..two..");
}
```

Additionally tests can also be specified as methods that take `JavaClasses` as input, which will result in reusing
the cached classes as well:

```Java
@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = {"my.pkg.one", "my.pkg.two"})
public class MyArchTest {
    // ...

    @ArchTest
    public static void one_shouldnt_access_two_could_also_be_specified_as_method(JavaClasses classes) {
        noClasses().that().resideInAPackage("..one..")
            .should().accessClassesThat().resideInAPackage("..two..")
            .check(classes);
    }
}
```

(Note that fields and methods used this way must always be static, to encourage a simple and unified way to specify
rules).

It is possible to define reusable rule sets as classes like:

```Java
public class MyArchRules {
    @ArchTest
    public static final ArchRule someRuleAsField = /* definition of some rule */;

    @ArchTest
    public static void anotherRuleAsMethod(JavaClasses classes) {
        /* definition of another rule */
    }
}
```

and then import those in dependent projects to easily evaluate them

```Java
@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = {"some.pkg.of.my.project"})
public class MyArchTestThatUsesMyRules {
    @ArchTest
    public static final ArchRules myArchRules = ArchRules.in(MyArchRules.class);
}
```

If this test is run, it will evaluate all rules (fields and methods) defined in `MyArchRules` against the imported
classes.

## Ignoring certain violations

In legacy projects where architecture tests are introduced, there might be too many violations to fix at the current
time. Nevertheless tests should be activated to ensure that no new violations will be introduced. To keep the focus on
those new violations, it is possible to ignore the current violations.
This is configured by putting a file named 'archunit_ignore_patterns.txt' in the root of the classpath. Each line
of this file will be interpreted as a regular expression. Violations with a message matching any of these regular
expressions are removed from the result. If no messages are left, the test will consequently pass.

## ArchUnit and the classpath

Since ArchUnit is written in plain Java, ArchUnit is compiled to Java classes, loaded by a ClassLoader and executed
within a JVM. ArchUnit's subject consists of other Java classes, which may be on the classpath of the same
ClassLoader, within the scope of your test. But this is not required; it is possible to execute

```Java
new ClassFileImporter().importPath(Paths.get("/home/someuser/workspace/someproject"));
```

or

```Java
new ClassFileImporter().importJar(new JarFile("/home/someuser/.m2/repository/my/project/my-project.jar"));
```

and evaluate rules on the result. However at times it may be more convenient to use ArchUnit with all classes
on the classpath. For example, consider some Annotation

```Java
@interface CustomAnnotation {
    String value();
}
```

If you need to access this annotation to write a custom rule against it, without this annotation on the classpath
one has to rely on

```Java
JavaAnnotation annotation = javaClass.getAnnotationOfType("some.pkg.CustomAnnotation");
Object value = annotation.get("value"); // result is untyped, since it might not be on the classpath (e.g. enums)
```

So there is neither type safety nor automatic refactoring support. If this annotation is on the classpath, however,
this can be written way more naturally, like

```Java
CustomAnnotation annotation = javaClass.getAnnotationOfType(CustomAnnotation.class);
String value = annotation.value();
```

Also, most `Java...` objects (e.g. `JavaClass`, `JavaMethod`, `JavaField`, ...) ArchUnit offers at its core API,
are not only modelled closely to the Java Reflection API, but also provide a simple way to access the 
respective API, if all necessary classes are on the classpath. For example

```Java
JavaClass javaClass = javaClasses.get(String.class);
assertEquals(String.class, javaClass.reflect());

JavaMethod javaMethod = javaClass.getMethod("length");
assertEquals(String.class.getDeclaredMethod("length"), javaMethod.reflect());
```

This allows to use the full power of the Reflection API when writing custom rules, if necessary (and the classpath
is correct). ArchUnit's own rule API never relies on the classpath, though, such that the evaluation of
default rules and syntax combinations does not depend on whether the classes were imported from the classpath
or some JAR / folder.

## Advanced configuration

Some behavior of ArchUnit can be configured within a central property file.
This file must be named `archunit.properties` and reside in the root of the classpath.
Supported configuration options are

```
# E.g. if a class calls a method, but the declaring class is not within the scope of the import,
# like in a case, where a package like 'my.app' is imported, and java.lang.String#length is called.
# Should ArchUnit try to locate the missing class on the classpath and import it as well?
#
# default = false - This has a performance impact
resolveMissingDependenciesFromClassPath=true

# Extends the customizability of 'resolveMissingDependenciesFromClassPath' by allowing to specify
# a custom implementation of ClassResolver. Such a custom implementation has full control, how
# type names should be resolved against JavaClasses. SelectedClassResolverFromClasspath is one example,
# it allows to resolve some types from the classpath (based on their package, while others are 
# just stubbed. E.g. if you want to resolve classes from your own app, but not from java.util.. 
# or similar).
#
# classResolver.args allows to configure constructor parameters, to be supplied to a constructor
# accepting a single List<String> parameter. If no arguments are configured, a default constructor
# is supported as well.
#
# default = absent - fall back to evaluating 'resolveMissingDependenciesFromClassPath'
classResolver=com.tngtech.archunit.core.importer.resolvers.SelectedClassResolverFromClasspath
classResolver.args=com.tngtech.archunit.core,com.tngtech.archunit.base

# Should ArchUnit include the MD5 sum of imported classes into the JavaClass#getSource()?
# This way failure tracking can be improved, if there are inconsistencies within the imported sources.
# 
# default = false - This has a performance impact
enableMd5InClassSources=true
```

## Extending ArchUnit

Besides extending the rule syntax itself, it's also possible, to extend the rule evaluation mechanism in a completely
dynamic way. An use case could be to draw some diagrams of failed rules, or report failures to
some custom system.

ArchUnit uses the standard Java `ServiceLoader` mechanism. I.e. to register a custom extension, one has to add a
class implementing `com.tngtech.archunit.lang.extension.ArchUnitExtension`, add a text file (in UTF-8) to the
directory `/META-INF/services`, named 'com.tngtech.archunit.lang.extension.ArchUnitExtension' and add a line
containing the fully qualified class name of the custom extension.

Extensions can be configured via `archunit.properties`, in particular, only enabled extensions will be evaluated, i.e.

```
# this must be set to true, or the extension will never be called
extension.my-custom-extension.enabled=true

# All further properties will be passed to the custom extension during evaluation
# In this case properties with a single entry 'my-prop'='someValue'
extension.my-custom-extension.my-prop=someValue
```

Note that 'my-custom-extension' refers to the String returned by `extension.getUniqueIdentifier()`.

## License

ArchUnit is published under the Apache License 2.0, see http://www.apache.org/licenses/LICENSE-2.0 for details.

Furthermore, ArchUnit redistributes some third party libraries to avoid classpath collisions:

* ASM (http://asm.ow2.org)
* Google Guava (https://github.com/google/guava)

All licenses for ArchUnit and redistributed libraries can be found within the [licenses](licenses) folder.

## Where to look next

Further examples can be found in the project `archunit-example`, including some further predefined rules
like detecting cyclic dependencies or checking for specific field accesses or method calls.
