package com.tngtech.archunit.core.importer.testexamples.compileraddedparameters;

import java.util.List;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

/**
 * Local classes in all contexts that matter for the parameters of their constructors: whether there is an enclosing
 * instance (instance method, instance initializer, constructor and lambda in an instance method vs. static method,
 * static initializer and lambda in a static method) and whether local variables are captured.
 */
@SuppressWarnings("unused")
public class LocalClassExamples {
    public static final Class<?> LOCAL_CLASS_IN_STATIC_INITIALIZER;
    public static final Class<?> LOCAL_CLASS_IN_STATIC_INITIALIZER_CAPTURING_VARIABLE;

    static {
        class LocalClassInStaticInitializer {
            LocalClassInStaticInitializer(@SomeParameterAnnotation String param) {
            }
        }
        LOCAL_CLASS_IN_STATIC_INITIALIZER = LocalClassInStaticInitializer.class;

        int captured = 42;
        class LocalClassInStaticInitializerCapturingVariable {
            LocalClassInStaticInitializerCapturingVariable(@SomeParameterAnnotation String param) {
            }

            int captured() {
                return captured;
            }
        }
        LOCAL_CLASS_IN_STATIC_INITIALIZER_CAPTURING_VARIABLE = LocalClassInStaticInitializerCapturingVariable.class;
    }

    public final Class<?> localClassInInstanceInitializer;
    public final Class<?> localClassInConstructor;

    {
        class LocalClassInInstanceInitializer {
            LocalClassInInstanceInitializer(String param) {
            }
        }
        localClassInInstanceInitializer = LocalClassInInstanceInitializer.class;
    }

    public LocalClassExamples() {
        class LocalClassInConstructor {
            LocalClassInConstructor(String param) {
            }
        }
        localClassInConstructor = LocalClassInConstructor.class;
    }

    public Class<?> localClassInInstanceMethod() {
        class LocalClassInInstanceMethod {
            LocalClassInInstanceMethod(@SomeParameterAnnotation String param) {
            }
        }
        return LocalClassInInstanceMethod.class;
    }

    public Class<?> localClassInInstanceMethodWithGenericParameter() {
        class LocalClassInInstanceMethodWithGenericParameter {
            LocalClassInInstanceMethodWithGenericParameter(@SomeParameterAnnotation List<String> param) {
            }
        }
        return LocalClassInInstanceMethodWithGenericParameter.class;
    }

    public Class<?> localClassInInstanceMethodCapturingVariable() {
        int captured = 42;
        class LocalClassInInstanceMethodCapturingVariable {
            LocalClassInInstanceMethodCapturingVariable(@SomeParameterAnnotation String param) {
            }

            int captured() {
                return captured;
            }
        }
        return LocalClassInInstanceMethodCapturingVariable.class;
    }

    public Class<?> localClassInInstanceMethodCapturingGenericVariable() {
        List<String> captured = emptyList();
        class LocalClassInInstanceMethodCapturingGenericVariable {
            LocalClassInInstanceMethodCapturingGenericVariable(String param) {
            }

            List<String> captured() {
                return captured;
            }
        }
        return LocalClassInInstanceMethodCapturingGenericVariable.class;
    }

    public static Class<?> localClassInStaticMethod() {
        class LocalClassInStaticMethod {
            LocalClassInStaticMethod(@SomeParameterAnnotation String param) {
            }
        }
        return LocalClassInStaticMethod.class;
    }

    public static Class<?> localClassInStaticMethodCapturingVariable() {
        int captured = 42;
        class LocalClassInStaticMethodCapturingVariable {
            LocalClassInStaticMethodCapturingVariable(@SomeParameterAnnotation String param) {
            }

            int captured() {
                return captured;
            }
        }
        return LocalClassInStaticMethodCapturingVariable.class;
    }

    public static Class<?> localClassInStaticMethodCapturingGenericVariable() {
        List<String> captured = emptyList();
        class LocalClassInStaticMethodCapturingGenericVariable {
            LocalClassInStaticMethodCapturingGenericVariable(String param) {
            }

            List<String> captured() {
                return captured;
            }
        }
        return LocalClassInStaticMethodCapturingGenericVariable.class;
    }

    public Class<?> localClassInLambdaInInstanceMethod() {
        Supplier<Class<?>> localClass = () -> {
            class LocalClassInLambdaInInstanceMethod {
                LocalClassInLambdaInInstanceMethod(String param) {
                }
            }
            return LocalClassInLambdaInInstanceMethod.class;
        };
        return localClass.get();
    }

    public static Class<?> localClassInLambdaInStaticMethod() {
        Supplier<Class<?>> localClass = () -> {
            class LocalClassInLambdaInStaticMethod {
                LocalClassInLambdaInStaticMethod(String param) {
                }
            }
            return LocalClassInLambdaInStaticMethod.class;
        };
        return localClass.get();
    }

    public static Class<?> localClassInAnonymousClassInStaticMethod() {
        return new Supplier<Class<?>>() {
            @Override
            public Class<?> get() {
                class LocalClassInAnonymousClass {
                    LocalClassInAnonymousClass(String param) {
                    }
                }
                return LocalClassInAnonymousClass.class;
            }
        }.get();
    }

    /**
     * @return the local class followed by its anonymous subclass; each of the two classes captures a different local variable
     */
    public static List<Class<?>> localClassWithAnonymousSubclassCapturingDifferentVariablesInStaticMethod() {
        int capturedByLocalClass = 1;
        int capturedByAnonymousSubclass = 2;
        class LocalClassWithAnonymousSubclass {
            LocalClassWithAnonymousSubclass(String param) {
            }

            int capturedByLocalClass() {
                return capturedByLocalClass;
            }

            Class<?> anonymousSubclass() {
                return new LocalClassWithAnonymousSubclass("param") {
                    int capturedByAnonymousSubclass() {
                        return capturedByAnonymousSubclass;
                    }
                }.getClass();
            }
        }
        return asList(LocalClassWithAnonymousSubclass.class, new LocalClassWithAnonymousSubclass("param").anonymousSubclass());
    }
}
