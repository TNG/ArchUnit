# ArchUnit Examples

This module houses some examples to illustrate how to use ArchUnit. All example
rules you can find within `example/test` refer to classes from `example/main`.
These tests are all designed to fail, to demonstrate how production code could violate
typical architectural constraints (like layer dependencies).

All tests are marked with `@Category(Example.class)`, to run them with the regular
Gradle build, add the property `example`, e.g.

```
../gradlew clean build -P example
```

Otherwise the tests can be run directly from any IDE.

Note that the example rules within this module also serve as input to
`archunit-integration-test`, to continuously ensure the expected behavior of the example
rules.