# ArchUnit Examples

This module houses some examples
* to illustrate how to use ArchUnit, and
* providing input for the `archunit-integration-test` at the same time.

The [example rules within `example/test`](src/test/java/com/tngtech/archunit/exampletest)
are applied to classes from [`example/main`](src/main/java/com/tngtech/archunit/example/),
which are designed to break the architectural concepts (like layer dependencies, etc.).
This demonstrates how ArchUnit detects such violations.

In order to execute those tests (marked with `@Category(Example.class)`, excluded from the regular build),
simply add the property `example` to the Gradle build:
```
../gradlew clean build -P example
```

Alternatively, the tests can also be run directly from any IDE, of course.

Happy exploring!