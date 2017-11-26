---
title: Getting Started
layout: splash
permalink: /getting-started
---

If you want to dive right into the first ArchUnit test using JUnit, follow these steps

## ArchUnit can be obtained from Maven Central
```
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit</artifactId>
    <version>0.5.0</version>
</dependency>
```
## Create a JUnit test
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
## Let the API guide you
![ArchUnit Fluent API](assets/ArchUnit-API.gif)
