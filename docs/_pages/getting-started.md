---
title: Getting Started
layout: splash
permalink: /getting-started
---

If you want to dive right into the first ArchUnit test, follow these steps

## Add ArchUnit as dependency

ArchUnit can be obtained from Maven Central.

#### Maven
```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit</artifactId>
    <version>0.13.1</version>
    <scope>test</scope>
</dependency>
```

#### Gradle
```groovy
dependencies {
    testCompile 'com.tngtech.archunit:archunit:0.13.1'
}
```

## Create a test
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
## Let the API guide you
![ArchUnit Fluent API](assets/ArchUnit-API.gif)

## How to continue
For further information, for example how to use the extended JUnit 4 support supplying caching and 
more, check out the [User Guide](userguide/html/000_Index.html) or examples for the current 
release at [ArchUnit Examples](https://github.com/TNG/ArchUnit-Examples).
