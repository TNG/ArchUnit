package com.tngtech.archunit.exampletest.junit5;

import java.lang.annotation.Annotation;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.example.onionarchitecture.domain.model.OrderItem;
import com.tngtech.archunit.example.onionarchitecture.domain.service.OrderQuantity;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.annotations.Adapter;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.annotations.Application;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.annotations.DomainModel;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.annotations.DomainService;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongTo;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

@ArchTag("example")
@AnalyzeClasses(packages = {
        "com.tngtech.archunit.example.onionarchitecture",
        "com.tngtech.archunit.example.onionarchitecture_by_annotations"
})
public class OnionArchitectureTest {

    @ArchTest
    static final ArchRule onion_architecture_is_respected = onionArchitecture()
            .domainModels("..domain.model..")
            .domainServices("..domain.service..")
            .applicationServices("..application..")
            .adapter("cli", "..adapter.cli..")
            .adapter("persistence", "..adapter.persistence..")
            .adapter("rest", "..adapter.rest..");

    @ArchTest
    static final ArchRule onion_architecture_is_respected_with_exception = onionArchitecture()
            .domainModels("..domain.model..")
            .domainServices("..domain.service..")
            .applicationServices("..application..")
            .adapter("cli", "..adapter.cli..")
            .adapter("persistence", "..adapter.persistence..")
            .adapter("rest", "..adapter.rest..")

            .ignoreDependency(OrderItem.class, OrderQuantity.class);

    @ArchTest
    static final ArchRule onion_architecture_defined_by_annotations = onionArchitecture()
            .domainModels(byAnnotation(DomainModel.class))
            .domainServices(byAnnotation(DomainService.class))
            .applicationServices(byAnnotation(Application.class))
            .adapter("cli", byAnnotation(adapter("cli")))
            .adapter("persistence", byAnnotation(adapter("persistence")))
            .adapter("rest", byAnnotation(adapter("rest")));

    private static DescribedPredicate<JavaClass> byAnnotation(Class<? extends Annotation> annotationType) {
        DescribedPredicate<CanBeAnnotated> annotatedWith = annotatedWith(annotationType);
        return belongTo(annotatedWith).as(annotatedWith.getDescription());
    }

    private static DescribedPredicate<JavaClass> byAnnotation(DescribedPredicate<? super JavaAnnotation<?>> annotationType) {
        DescribedPredicate<CanBeAnnotated> annotatedWith = annotatedWith(annotationType);
        return belongTo(annotatedWith).as(annotatedWith.getDescription());
    }

    private static DescribedPredicate<JavaAnnotation<?>> adapter(String adapterName) {
        return describe(
                String.format("@%s(\"%s\")", Adapter.class.getSimpleName(), adapterName),
                a -> a.getRawType().isEquivalentTo(Adapter.class) && a.as(Adapter.class).value().equals(adapterName)
        );
    }
}
