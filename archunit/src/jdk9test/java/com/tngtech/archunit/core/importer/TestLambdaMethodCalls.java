/*
 * Copyright 2019 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.core.importer;

import java.util.function.Supplier;

import org.junit.Test;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

public class TestLambdaMethodCalls {
  @Test
  public void checkAllMethodsOfPersonInUse(){
    JavaClasses importedClass = new ClassFileImporter()
            .importClasses(DummyClassWithLambdas.class);

    ArchRule rule = ArchRuleDefinition.classes().should(haveOnlyNecessaryMethods);
    rule.check(importedClass);
  }

  ArchCondition<JavaClass> haveOnlyNecessaryMethods = new ArchCondition<>(
      "Classes must only have methods that are called at least once") {
    @Override
    public void check(final JavaClass item, final ConditionEvents events) {
      for (JavaMethod method : item.getMethods()) {
        if (method.getCallsOfSelf().isEmpty()) {
          String message = String.format("Method %s is never accessed", method.getFullName());
          events.add(SimpleConditionEvent.violated(method, message));
        }
      }
    }
  };

  class DummyClassWithLambdas {

    public void useMethodsByLambda() {
      Person testPerson = new Person();
      dummyMethodString(testPerson::getName);
      dummyMethodInt(() -> testPerson.getAge());

    }

    private void dummyMethodString(Supplier<String> x) {
        useMethodsByLambda();
    }

    private void dummyMethodInt(Supplier<Integer> y) {
    }

    public class Person {
      String name;
      int age;

      public String getName() {
        return name;
      }

      public int getAge() {
        return age;
      }

    }
  }

}