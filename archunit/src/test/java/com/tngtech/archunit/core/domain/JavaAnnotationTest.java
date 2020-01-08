package com.tngtech.archunit.core.domain;

import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import org.assertj.core.data.MapEntry;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.guava.api.Assertions.assertThat;

public class JavaAnnotationTest {
    @Test
    public void description_of_annotation_on_class() {
        JavaClass javaClass = importClasses(SomeAnnotation.class, SomeClass.class).get(SomeClass.class);

        JavaAnnotation<JavaClass> annotation = javaClass.getAnnotationOfType(SomeAnnotation.class.getName());

        assertThat(annotation.getDescription()).isEqualTo(
                "Annotation <" + SomeAnnotation.class.getName()
                        + "> on class <" + SomeClass.class.getName() + ">");
    }

    @Test
    public void description_of_annotation_on_method() {
        JavaClass javaClass = importClasses(SomeAnnotation.class, SomeClass.class).get(SomeClass.class);

        JavaAnnotation<JavaMethod> annotation = javaClass.getMethod("method").getAnnotationOfType(SomeAnnotation.class.getName());

        assertThat(annotation.getDescription()).isEqualTo(
                "Annotation <" + SomeAnnotation.class.getName()
                        + "> on method <" + SomeClass.class.getName() + ".method()>");
    }

    @Test
    public void description_of_class_annotation_parameter() {
        JavaClass javaClass = importClasses(SomeAnnotation.class, SomeClass.class).get(SomeClass.class);

        JavaAnnotation<?> annotation = ((JavaAnnotation<?>[]) javaClass.getAnnotationOfType(SomeAnnotation.class.getName()).get("sub").get())[0];

        assertThat(annotation.getDescription()).isEqualTo(
                "Annotation <" + SubAnnotation.class.getName()
                        + "> on class <" + SomeClass.class.getName() + ">");
    }

    @Test
    public void description_of_method_annotation_parameter() {
        JavaClass javaClass = importClasses(SomeAnnotation.class, SomeClass.class).get(SomeClass.class);

        JavaAnnotation<?> annotation = ((JavaAnnotation<?>[]) javaClass.getMethod("method")
                .getAnnotationOfType(SomeAnnotation.class.getName()).get("sub").get())[0];

        assertThat(annotation.getDescription()).isEqualTo(
                "Annotation <" + SubAnnotation.class.getName()
                        + "> on method <" + SomeClass.class.getName() + ".method()>");
    }

    @Test
    public void visits_first_level_parameters() {
        JavaClasses classes = importClasses(
                AnnotationWithFirstLevelParameters.class, ClassWithAnnotationWithFirstLevelParameters.class,
                Class1.class, Class2.class, SomeEnum.class);
        JavaAnnotation<JavaClass> annotation = classes.get(ClassWithAnnotationWithFirstLevelParameters.class)
                .getAnnotationOfType(AnnotationWithFirstLevelParameters.class.getName());
        Map<Enum<?>, JavaEnumConstant> enumConstants = getEnumConstants(classes.get(SomeEnum.class));
        ParametersStoringVisitor visitor = new ParametersStoringVisitor();

        annotation.accept(visitor);

        MapEntry<String, Object>[] expectedParameters = expectedParametersWithOffset1("", classes, enumConstants);
        assertThat(visitor.getVisitedParameters())
                .contains(expectedParameters)
                .hasSize(expectedParameters.length);
    }

    @Test
    public void visits_second_level_parameters() {
        JavaClasses classes = importClasses(
                AnnotationWithFirstLevelParameters.class, AnnotationWithSecondLevelParameters.class,
                ClassWithAnnotationWithSecondLevelParameters.class,
                Class1.class, Class2.class, Class3.class, Class4.class, SomeEnum.class);
        JavaAnnotation<JavaClass> annotation = classes.get(ClassWithAnnotationWithSecondLevelParameters.class)
                .getAnnotationOfType(AnnotationWithSecondLevelParameters.class.getName());
        Map<Enum<?>, JavaEnumConstant> enumConstants = getEnumConstants(classes.get(SomeEnum.class));
        ParametersStoringVisitor visitor = new ParametersStoringVisitor();

        annotation.accept(visitor);

        JavaAnnotation<?>[] annotations = (JavaAnnotation<?>[]) annotation.get("annotations").get();
        MapEntry<String, Object>[] expectedTopLevelParameters = createEntries(
                entry("annotation", annotation.get("annotation").get()),
                entry("annotations", (Object) annotations[0]), entry("annotations", (Object) annotations[1]));
        MapEntry<String, Object>[] expectedParametersOfSingleAnnotation = expectedParametersWithOffset1("annotation.", classes, enumConstants);
        MapEntry<String, Object>[] expectedParametersOfFirstAnnotationOfArray = expectedParametersWithOffset1("annotations.", classes, enumConstants);
        MapEntry<String, Object>[] expectedParametersOfSecondAnnotationOfArray = expectedParametersWithOffset3("annotations.", classes, enumConstants);

        assertThat(visitor.getVisitedParameters())
                .contains(expectedTopLevelParameters)
                .contains(expectedParametersOfSingleAnnotation)
                .contains(expectedParametersOfFirstAnnotationOfArray)
                .contains(expectedParametersOfSecondAnnotationOfArray)
                .hasSize(expectedTopLevelParameters.length + expectedParametersOfSingleAnnotation.length
                        + expectedParametersOfFirstAnnotationOfArray.length + expectedParametersOfSecondAnnotationOfArray.length);
    }

    private MapEntry<String, Object>[] expectedParametersWithOffset1(String prefix, JavaClasses classes, Map<Enum<?>, JavaEnumConstant> enumConstants) {
        return createEntries(
                entry(prefix + "aBoolean", (Object) false), entry(prefix + "booleans", (Object) true),
                entry(prefix + "aByte", (Object) (byte) 1), entry(prefix + "bytes", (Object) (byte) 1), entry(prefix + "bytes", (Object) (byte) 2),
                entry(prefix + "aChar", (Object) 'a'), entry(prefix + "chars", (Object) 'a'), entry(prefix + "chars", (Object) 'b'),
                entry(prefix + "aDouble", (Object) 1.0), entry(prefix + "doubles", (Object) 1.0), entry(prefix + "doubles", (Object) 2.0),
                entry(prefix + "aFloat", (Object) 1.0F), entry(prefix + "floats", (Object) 1.0F), entry(prefix + "floats", (Object) 2.0F),
                entry(prefix + "anInt", (Object) 1), entry(prefix + "ints", (Object) 1), entry(prefix + "ints", (Object) 2),
                entry(prefix + "aLong", (Object) 1L), entry(prefix + "longs", (Object) 1L), entry(prefix + "longs", (Object) 2L),
                entry(prefix + "aShort", (Object) (short) 1), entry(prefix + "shorts", (Object) (short) 1), entry(prefix + "shorts", (Object) (short) 2),
                entry(prefix + "aString", (Object) "string1"), entry(prefix + "strings", (Object) "string1"), entry(prefix + "strings", (Object) "string2"),
                entry(prefix + "aClass", (Object) classes.get(Class1.class)),
                entry(prefix + "classes", (Object) classes.get(Class1.class)), entry(prefix + "classes", (Object) classes.get(Class2.class)),
                entry(prefix + "anEnum", (Object) enumConstants.get(SomeEnum.FIRST)),
                entry(prefix + "enums", (Object) enumConstants.get(SomeEnum.FIRST)), entry(prefix + "enums", (Object) enumConstants.get(SomeEnum.SECOND)));
    }

    private MapEntry<String, Object>[] expectedParametersWithOffset3(String prefix, JavaClasses classes, Map<Enum<?>, JavaEnumConstant> enumConstants) {
        return createEntries(
                entry(prefix + "aBoolean", (Object) true), entry(prefix + "booleans", (Object) false),
                entry(prefix + "aByte", (Object) (byte) 3), entry(prefix + "bytes", (Object) (byte) 3), entry(prefix + "bytes", (Object) (byte) 4),
                entry(prefix + "aChar", (Object) 'c'), entry(prefix + "chars", (Object) 'c'), entry(prefix + "chars", (Object) 'd'),
                entry(prefix + "aDouble", (Object) 3.0), entry(prefix + "doubles", (Object) 3.0), entry(prefix + "doubles", (Object) 4.0),
                entry(prefix + "aFloat", (Object) 3.0F), entry(prefix + "floats", (Object) 3.0F), entry(prefix + "floats", (Object) 4.0F),
                entry(prefix + "anInt", (Object) 3), entry(prefix + "ints", (Object) 3), entry(prefix + "ints", (Object) 4),
                entry(prefix + "aLong", (Object) 3L), entry(prefix + "longs", (Object) 3L), entry(prefix + "longs", (Object) 4L),
                entry(prefix + "aShort", (Object) (short) 3), entry(prefix + "shorts", (Object) (short) 3), entry(prefix + "shorts", (Object) (short) 4),
                entry(prefix + "aString", (Object) "string3"), entry(prefix + "strings", (Object) "string3"), entry(prefix + "strings", (Object) "string4"),
                entry(prefix + "aClass", (Object) classes.get(Class3.class)),
                entry(prefix + "classes", (Object) classes.get(Class3.class)), entry(prefix + "classes", (Object) classes.get(Class4.class)),
                entry(prefix + "anEnum", (Object) enumConstants.get(SomeEnum.THIRD)),
                entry(prefix + "enums", (Object) enumConstants.get(SomeEnum.THIRD)), entry(prefix + "enums", (Object) enumConstants.get(SomeEnum.FOURTH)));
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    private final MapEntry<String, Object>[] createEntries(MapEntry<String, ?>... entries) {
        return (MapEntry<String, Object>[]) entries;
    }

    private Map<Enum<?>, JavaEnumConstant> getEnumConstants(JavaClass enumType) {
        ImmutableMap.Builder<Enum<?>, JavaEnumConstant> result = ImmutableMap.builder();
        for (Object enumConstant : enumType.reflect().getEnumConstants()) {
            Enum<?> reflectedConstant = (Enum<?>) enumConstant;
            result.put(reflectedConstant, enumType.getEnumConstant(reflectedConstant.name()));
        }
        return result.build();
    }

    private @interface SomeAnnotation {
        SubAnnotation[] sub() default {};
    }

    private @interface SubAnnotation {
    }

    @SuppressWarnings("unused")
    @SomeAnnotation(sub = @SubAnnotation)
    private static class SomeClass {
        @SomeAnnotation(sub = @SubAnnotation)
        void method() {
        }
    }

    private enum SomeEnum {
        FIRST, SECOND, THIRD, FOURTH
    }

    private @interface AnnotationWithFirstLevelParameters {
        boolean aBoolean();

        boolean[] booleans();

        byte aByte();

        byte[] bytes();

        char aChar();

        char[] chars();

        double aDouble();

        double[] doubles();

        float aFloat();

        float[] floats();

        int anInt();

        int[] ints();

        long aLong();

        long[] longs();

        short aShort();

        short[] shorts();

        String aString();

        String[] strings();

        Class<?> aClass();

        Class<?>[] classes();

        SomeEnum anEnum();

        SomeEnum[] enums();
    }

    @AnnotationWithFirstLevelParameters(
            aBoolean = false,
            booleans = {true},
            aByte = 1,
            bytes = {1, 2},
            aChar = 'a',
            chars = {'a', 'b'},
            aDouble = 1.0,
            doubles = {1.0, 2.0},
            aFloat = 1.0F,
            floats = {1.0F, 2.0F},
            anInt = 1,
            ints = {1, 2},
            aLong = 1,
            longs = {1, 2},
            aShort = 1,
            shorts = {1, 2},
            aString = "string1",
            strings = {"string1", "string2"},
            aClass = Class1.class,
            classes = {Class1.class, Class2.class},
            anEnum = SomeEnum.FIRST,
            enums = {SomeEnum.FIRST, SomeEnum.SECOND}
    )
    private static class ClassWithAnnotationWithFirstLevelParameters {
    }

    private @interface AnnotationWithSecondLevelParameters {
        AnnotationWithFirstLevelParameters annotation();

        AnnotationWithFirstLevelParameters[] annotations();
    }

    @AnnotationWithSecondLevelParameters(
            annotation = @AnnotationWithFirstLevelParameters(
                    aBoolean = false,
                    booleans = {true},
                    aByte = 1,
                    bytes = {1, 2},
                    aChar = 'a',
                    chars = {'a', 'b'},
                    aDouble = 1.0,
                    doubles = {1.0, 2.0},
                    aFloat = 1.0F,
                    floats = {1.0F, 2.0F},
                    anInt = 1,
                    ints = {1, 2},
                    aLong = 1,
                    longs = {1, 2},
                    aShort = 1,
                    shorts = {1, 2},
                    aString = "string1",
                    strings = {"string1", "string2"},
                    aClass = Class1.class,
                    classes = {Class1.class, Class2.class},
                    anEnum = SomeEnum.FIRST,
                    enums = {SomeEnum.FIRST, SomeEnum.SECOND}
            ),
            annotations = {
                    @AnnotationWithFirstLevelParameters(
                            aBoolean = false,
                            booleans = {true},
                            aByte = 1,
                            bytes = {1, 2},
                            aChar = 'a',
                            chars = {'a', 'b'},
                            aDouble = 1.0,
                            doubles = {1.0, 2.0},
                            aFloat = 1.0F,
                            floats = {1.0F, 2.0F},
                            anInt = 1,
                            ints = {1, 2},
                            aLong = 1,
                            longs = {1, 2},
                            aShort = 1,
                            shorts = {1, 2},
                            aString = "string1",
                            strings = {"string1", "string2"},
                            aClass = Class1.class,
                            classes = {Class1.class, Class2.class},
                            anEnum = SomeEnum.FIRST,
                            enums = {SomeEnum.FIRST, SomeEnum.SECOND}
                    ),
                    @AnnotationWithFirstLevelParameters(
                            aBoolean = true,
                            booleans = {false},
                            aByte = 3,
                            bytes = {3, 4},
                            aChar = 'c',
                            chars = {'c', 'd'},
                            aDouble = 3.0,
                            doubles = {3.0, 4.0},
                            aFloat = 3.0F,
                            floats = {3.0F, 4.0F},
                            anInt = 3,
                            ints = {3, 4},
                            aLong = 3,
                            longs = {3, 4},
                            aShort = 3,
                            shorts = {3, 4},
                            aString = "string3",
                            strings = {"string3", "string4"},
                            aClass = Class3.class,
                            classes = {Class3.class, Class4.class},
                            anEnum = SomeEnum.THIRD,
                            enums = {SomeEnum.THIRD, SomeEnum.FOURTH}
                    )
            })
    private static class ClassWithAnnotationWithSecondLevelParameters {
    }

    private static class Class1 {
    }

    private static class Class2 {
    }

    private static class Class3 {
    }

    private static class Class4 {
    }

    private static class ParametersStoringVisitor implements JavaAnnotation.ParameterVisitor {
        private final String prefix;
        private final Multimap<String, Object> visitedParameters;

        ParametersStoringVisitor() {
            this("", HashMultimap.<String, Object>create());
        }

        private ParametersStoringVisitor(String prefix, Multimap<String, Object> visitedParameters) {
            this.prefix = prefix;
            this.visitedParameters = visitedParameters;
        }

        public Multimap<String, Object> getVisitedParameters() {
            return visitedParameters;
        }

        @Override
        public void visitBoolean(String propertyName, boolean propertyValue) {
            visitedParameters.put(prefix + propertyName, propertyValue);
        }

        @Override
        public void visitByte(String propertyName, byte propertyValue) {
            visitedParameters.put(prefix + propertyName, propertyValue);
        }

        @Override
        public void visitCharacter(String propertyName, Character propertyValue) {
            visitedParameters.put(prefix + propertyName, propertyValue);
        }

        @Override
        public void visitDouble(String propertyName, Double propertyValue) {
            visitedParameters.put(prefix + propertyName, propertyValue);
        }

        @Override
        public void visitFloat(String propertyName, Float propertyValue) {
            visitedParameters.put(prefix + propertyName, propertyValue);
        }

        @Override
        public void visitInteger(String propertyName, int propertyValue) {
            visitedParameters.put(prefix + propertyName, propertyValue);
        }

        @Override
        public void visitLong(String propertyName, Long propertyValue) {
            visitedParameters.put(prefix + propertyName, propertyValue);
        }

        @Override
        public void visitShort(String propertyName, Short propertyValue) {
            visitedParameters.put(prefix + propertyName, propertyValue);
        }

        @Override
        public void visitString(String propertyName, String propertyValue) {
            visitedParameters.put(prefix + propertyName, propertyValue);
        }

        @Override
        public void visitClass(String propertyName, JavaClass propertyValue) {
            visitedParameters.put(prefix + propertyName, propertyValue);
        }

        @Override
        public void visitEnumConstant(String propertyName, JavaEnumConstant propertyValue) {
            visitedParameters.put(prefix + propertyName, propertyValue);
        }

        @Override
        public void visitAnnotation(String propertyName, JavaAnnotation<?> propertyValue) {
            visitedParameters.put(prefix + propertyName, propertyValue);
            propertyValue.accept(new ParametersStoringVisitor(prefix + propertyName + ".", visitedParameters));
        }
    }
}
