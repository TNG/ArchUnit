package com.tngtech.archunit.integration;

import java.util.UUID;

import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.AdministrationPort;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.administration.AdministrationCLI;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.order.OrderItem;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.order.OrderQuantity;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.product.Product;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.product.ProductId;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.product.ProductName;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.product.ProductRepository;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.shopping.ShoppingCart;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.shopping.ShoppingCartId;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.shopping.ShoppingCartRepository;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.shopping.ShoppingController;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.shopping.ShoppingService;
import com.tngtech.archunit.testutils.ExpectedTestFailures;

import static com.tngtech.archunit.testutils.ExpectedAccess.callFromMethod;
import static com.tngtech.archunit.testutils.ExpectedDependency.constructor;
import static com.tngtech.archunit.testutils.ExpectedDependency.field;
import static com.tngtech.archunit.testutils.ExpectedDependency.method;
import static java.lang.System.lineSeparator;

class ExpectedOnionArchitectureByAnnotationFailures {
    // This is only extracted to avoid the import clashes.
    // Otherwise, it would be really bloated two write down with fully qualified class names everywhere
    static void addTo(ExpectedTestFailures expectedTestFailures) {
        expectedTestFailures
                .ofRule("onion_architecture_defined_by_annotations",
                        "Onion architecture consisting of" + lineSeparator() +
                                "domain models (annotated with @DomainModel)" + lineSeparator() +
                                "domain services (annotated with @DomainService)" + lineSeparator() +
                                "application services (annotated with @Application)" + lineSeparator() +
                                "adapter 'cli' (annotated with @Adapter(\"cli\"))" + lineSeparator() +
                                "adapter 'persistence' (annotated with @Adapter(\"persistence\"))" + lineSeparator() +
                                "adapter 'rest' (annotated with @Adapter(\"rest\"))")
                .by(constructor(Product.class).withParameter(ProductId.class))
                .by(constructor(Product.class).withParameter(ProductName.class))
                .by(constructor(ShoppingCart.class).withParameter(ShoppingCartId.class))
                .by(constructor(ShoppingService.class).withParameter(ProductRepository.class))
                .by(constructor(ShoppingService.class).withParameter(ShoppingCartRepository.class))

                .by(field(Product.class, "id").ofType(ProductId.class))
                .by(field(Product.class, "name").ofType(ProductName.class))
                .by(field(ShoppingCart.class, "id").ofType(ShoppingCartId.class))
                .by(field(ShoppingService.class, "productRepository").ofType(ProductRepository.class))
                .by(field(ShoppingService.class, "shoppingCartRepository").ofType(ShoppingCartRepository.class))

                .by(method(AdministrationCLI.class, "handle")
                        .referencingClassObject(ProductRepository.class)
                        .inLine(18))
                .by(callFromMethod(AdministrationCLI.class, "handle", String[].class, AdministrationPort.class)
                        .toMethod(ProductRepository.class, "getTotalCount")
                        .inLine(19))
                .by(callFromMethod(ShoppingController.class, "addToShoppingCart", UUID.class, UUID.class, int.class)
                        .toConstructor(ProductId.class, UUID.class)
                        .inLine(20))
                .by(callFromMethod(ShoppingController.class, "addToShoppingCart", UUID.class, UUID.class, int.class)
                        .toConstructor(ShoppingCartId.class, UUID.class)
                        .inLine(20))
                .by(method(ShoppingService.class, "addToShoppingCart").withParameter(ProductId.class))
                .by(method(ShoppingService.class, "addToShoppingCart").withParameter(ShoppingCartId.class))
                .by(callFromMethod(ShoppingService.class, "addToShoppingCart", ShoppingCartId.class, ProductId.class, OrderQuantity.class)
                        .toMethod(ShoppingCartRepository.class, "read", ShoppingCartId.class)
                        .inLine(21))
                .by(callFromMethod(ShoppingService.class, "addToShoppingCart", ShoppingCartId.class, ProductId.class, OrderQuantity.class)
                        .toMethod(ProductRepository.class, "read", ProductId.class)
                        .inLine(22))
                .by(callFromMethod(ShoppingService.class, "addToShoppingCart", ShoppingCartId.class, ProductId.class, OrderQuantity.class)
                        .toMethod(ShoppingCartRepository.class, "save", ShoppingCart.class)
                        .inLine(25))
                .by(constructor(OrderItem.class).withParameter(OrderQuantity.class))
                .by(field(OrderItem.class, "quantity").ofType(OrderQuantity.class));
    }
}
