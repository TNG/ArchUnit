[![Build Status](https://travis-ci.org/TNG/ArchUnit.svg?branch=master)](https://travis-ci.org/TNG/ArchUnit)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.tngtech.archunit/archunit/badge.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.tngtech.archunit%22%20)
[![License](https://img.shields.io/github/license/TNG/ArchUnit.svg)](https://github.com/TNG/ArchUnit/blob/master/LICENSE)

<img src="logo/ArchUnit-Logo.png" height="64" alt="ArchUnit">

ArchUnit is a free, simple and extensible library for checking the architecture of your Java code. That is, ArchUnit can check
dependencies between packages and classes, layers and slices, check for cyclic dependencies and more. It does so by
analyzing given Java bytecode, importing all classes into a Java code structure.
ArchUnit's main focus is to automatically test architecture and coding rules, using any plain Java unit testing
framework.

## An Example

#### Add the Maven Central dependency to your project

###### Gradle

```
testCompile 'com.tngtech.archunit:archunit:0.13.1'
```

###### Maven

```
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit</artifactId>
    <version>0.13.1</version>
    <scope>test</scope>
</dependency>
```

#### Create a test

```java
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

public class MyArchitectureTest {
    @Test
    public void some_architecture_rule() {
        JavaClasses importedClasses = new ClassFileImporter().importPackages("com.myapp");
    
        ArchRule rule = classes()... // see next section
    
        rule.check(importedClasses);
    }
}
```
#### Let the API guide you
![ArchUnit Fluent API](ArchUnit-API.gif)

## Where to look next

For further information, check out the user guide at [http://archunit.org](http://archunit.org) 
or test examples for the current release at
[ArchUnit Examples](https://github.com/TNG/ArchUnit-Examples).

## License

ArchUnit is published under the Apache License 2.0, see http://www.apache.org/licenses/LICENSE-2.0 for details.

It redistributes some third party libraries:

* ASM (http://asm.ow2.org), under BSD Licence
* Google Guava (https://github.com/google/guava), under Apache License 2.0

All licenses for ArchUnit and redistributed libraries can be found within the [licenses](licenses) folder.
