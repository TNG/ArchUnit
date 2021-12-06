package com.tngtech.archunit.core.importer.testexamples.methodresolution;

import java.io.Serializable;

@SuppressWarnings({"unused", "ConstantConditions"})
public class DeterminesMethodAnalogouslyToReflectionApi {

    public static class EquivalentMethodsAreChosenDepthFirst {

        public static class LeftLeftHasPrecedence {
            interface LeftLeftGrandParent {
                @ExpectedMethod
                void target();
            }

            interface LeftRightGrandParent {
                void target();
            }

            interface LeftParent extends LeftLeftGrandParent, LeftRightGrandParent {
            }

            interface RightLeftGrandParent {
                void target();
            }

            interface RightRightGrandParent {
                void target();
            }

            interface RightParent extends RightLeftGrandParent, RightRightGrandParent {
            }

            interface Child extends LeftParent, RightParent {
            }

            void scenario() {
                Child child = null;
                child.target();
            }
        }

        public static class LeftRightHasPrecedenceOverRight {
            interface LeftLeftGrandParent {
            }

            interface LeftRightGrandParent {
                @ExpectedMethod
                void target();
            }

            interface LeftParent extends LeftLeftGrandParent, LeftRightGrandParent {
            }

            interface RightLeftGrandParent {
                void target();
            }

            interface RightRightGrandParent {
                void target();
            }

            interface RightParent extends RightLeftGrandParent, RightRightGrandParent {
            }

            interface Child extends LeftParent, RightParent {
            }

            void scenario() {
                Child child = null;
                child.target();
            }
        }

        public static class RightLeftHasPrecedenceOverRightRight {
            interface LeftLeftGrandParent {
            }

            interface LeftRightGrandParent {
            }

            interface LeftParent extends LeftLeftGrandParent, LeftRightGrandParent {
            }

            interface RightLeftGrandParent {
                @ExpectedMethod
                void target();
            }

            interface RightRightGrandParent {
                void target();
            }

            interface RightParent extends RightLeftGrandParent, RightRightGrandParent {
            }

            interface Child extends LeftParent, RightParent {
            }

            void scenario() {
                Child child = null;
                child.target();
            }
        }

        public static class RightRightIsPickedIfThereIsNoAlternative {
            interface LeftLeftGrandParent {
            }

            interface LeftRightGrandParent {
            }

            interface LeftParent extends LeftLeftGrandParent, LeftRightGrandParent {
            }

            interface RightLeftGrandParent {
            }

            interface RightRightGrandParent {
                @ExpectedMethod
                void target();
            }

            interface RightParent extends RightLeftGrandParent, RightRightGrandParent {
            }

            interface Child extends LeftParent, RightParent {
            }

            void scenario() {
                Child child = null;
                child.target();
            }
        }

        public static class LeftOverriddenHasPrecedenceOverParents {
            interface LeftLeftGrandParent {
                void target();
            }

            interface LeftRightGrandParent {
                void target();
            }

            interface LeftParent extends LeftLeftGrandParent, LeftRightGrandParent {
                @Override
                @ExpectedMethod
                void target();
            }

            interface RightLeftGrandParent {
                void target();
            }

            interface RightRightGrandParent {
                void target();
            }

            interface RightParent extends RightLeftGrandParent, RightRightGrandParent {
            }

            interface Child extends LeftParent, RightParent {
            }

            void scenario() {
                Child child = null;
                child.target();
            }
        }

        public static class RightOverriddenHasPrecedenceOverParents {
            interface LeftLeftGrandParent {
            }

            interface LeftRightGrandParent {
            }

            interface LeftParent extends LeftLeftGrandParent, LeftRightGrandParent {
            }

            interface RightLeftGrandParent {
                void target();
            }

            interface RightRightGrandParent {
                void target();
            }

            interface RightParent extends RightLeftGrandParent, RightRightGrandParent {
                @Override
                @ExpectedMethod
                void target();
            }

            interface Child extends LeftParent, RightParent {
            }

            void scenario() {
                Child child = null;
                child.target();
            }
        }

        public static class LeftLeftHasPrecedenceOverOverriddenRight {
            interface LeftLeftGrandParent {
                @ExpectedMethod
                void target();
            }

            interface LeftRightGrandParent {
                void target();
            }

            interface LeftParent extends LeftLeftGrandParent, LeftRightGrandParent {
            }

            interface RightLeftGrandParent {
                void target();
            }

            interface RightRightGrandParent {
                void target();
            }

            interface RightParent extends RightLeftGrandParent, RightRightGrandParent {
                @Override
                void target();
            }

            interface Child extends LeftParent, RightParent {
            }

            void scenario() {
                Child child = null;
                child.target();
            }
        }

        public static class LeftRightHasPrecedenceOverOverriddenRight {
            interface LeftLeftGrandParent {
            }

            interface LeftRightGrandParent {
                @ExpectedMethod
                void target();
            }

            interface LeftParent extends LeftLeftGrandParent, LeftRightGrandParent {
            }

            interface RightLeftGrandParent {
                void target();
            }

            interface RightRightGrandParent {
                void target();
            }

            interface RightParent extends RightLeftGrandParent, RightRightGrandParent {
                @Override
                void target();
            }

            interface Child extends LeftParent, RightParent {
            }

            void scenario() {
                Child child = null;
                child.target();
            }
        }

        public static class LeftOverriddenHasPrecedenceOverRightOverridden {
            interface LeftLeftGrandParent {
                void target();
            }

            interface LeftRightGrandParent {
                void target();
            }

            interface LeftParent extends LeftLeftGrandParent, LeftRightGrandParent {
                @Override
                @ExpectedMethod
                void target();
            }

            interface RightLeftGrandParent {
                void target();
            }

            interface RightRightGrandParent {
                void target();
            }

            interface RightParent extends RightLeftGrandParent, RightRightGrandParent {
                @Override
                void target();
            }

            interface Child extends LeftParent, RightParent {
            }

            void scenario() {
                Child child = null;
                child.target();
            }
        }
    }

    public static class ClassHasPrecedenceOverInterface {

        public static class ParentClassHasPrecedenceOverChildInterfaces {
            interface LeftLeftGrandParent {
                void target();
            }

            interface LeftRightGrandParent {
                void target();
            }

            interface LeftParent extends LeftLeftGrandParent, LeftRightGrandParent {
                @Override
                void target();
            }

            interface RightLeftGrandParent {
                void target();
            }

            interface RightRightGrandParent {
                void target();
            }

            interface RightParent extends RightLeftGrandParent, RightRightGrandParent {
                @Override
                void target();
            }

            abstract static class ParentClass {
                @ExpectedMethod
                public abstract void target();
            }

            abstract static class Child extends ParentClass implements LeftParent, RightParent {
            }

            void scenario() {
                Child child = null;
                child.target();
            }
        }

        public static class GrandParentClassHasPrecedenceOverChildInterfaces {
            interface LeftLeftGrandParent {
                void target();
            }

            interface LeftRightGrandParent {
                void target();
            }

            interface LeftParent extends LeftLeftGrandParent, LeftRightGrandParent {
                @Override
                void target();
            }

            interface RightLeftGrandParent {
                void target();
            }

            interface RightRightGrandParent {
                void target();
            }

            interface RightParent extends RightLeftGrandParent, RightRightGrandParent {
                @Override
                void target();
            }

            abstract static class GrandParentClass {
                @ExpectedMethod
                public abstract void target();
            }

            abstract static class ParentClass extends GrandParentClass {
            }

            abstract static class Child extends ParentClass implements LeftParent, RightParent {
            }

            void scenario() {
                Child child = null;
                child.target();
            }
        }
    }

    public static class InterfaceOnParentHasPrecedenceOverInterfaceOnChild {

        public static class LeftLeftOnGrandparentHasPrecedenceOverAllOthers {
            interface LeftLeftGrandParent {
                @ExpectedMethod
                void target();
            }

            interface LeftRightGrandParent {
                void target();
            }

            interface LeftParent {
                void target();
            }

            interface RightLeftGrandParent {
                void target();
            }

            interface RightRightGrandParent {
                void target();
            }

            interface RightParent {
                void target();
            }

            abstract static class GrandParentClass implements LeftLeftGrandParent, LeftRightGrandParent, RightLeftGrandParent, RightRightGrandParent {
            }

            abstract static class ParentClass extends GrandParentClass implements LeftParent, RightParent {
            }

            abstract static class Child extends ParentClass {
            }

            void scenario() {
                Child child = null;
                child.target();
            }
        }

        public static class LeftRightOnGrandparentHasPrecedenceOverLeftOnParent {
            interface LeftLeftGrandParent {
            }

            interface LeftRightGrandParent {
                @ExpectedMethod
                void target();
            }

            interface LeftParent {
                void target();
            }

            interface RightLeftGrandParent {
                void target();
            }

            interface RightRightGrandParent {
                void target();
            }

            interface RightParent {
                void target();
            }

            abstract static class GrandParentClass implements LeftLeftGrandParent, LeftRightGrandParent, RightLeftGrandParent, RightRightGrandParent {
            }

            abstract static class ParentClass extends GrandParentClass implements LeftParent, RightParent {
            }

            abstract static class Child extends ParentClass {
            }

            void scenario() {
                Child child = null;
                child.target();
            }
        }

        public static class RightLeftOnGrandparentHasPrecedenceOverLeftAndRightOnParent {
            interface LeftLeftGrandParent {
            }

            interface LeftRightGrandParent {
            }

            interface LeftParent {
                void target();
            }

            interface RightLeftGrandParent {
                @ExpectedMethod
                void target();
            }

            interface RightRightGrandParent {
                void target();
            }

            interface RightParent {
                void target();
            }

            abstract static class GrandParentClass implements LeftLeftGrandParent, LeftRightGrandParent, RightLeftGrandParent, RightRightGrandParent {
            }

            abstract static class ParentClass extends GrandParentClass implements LeftParent, RightParent {
            }

            abstract static class Child extends ParentClass {
            }

            void scenario() {
                Child child = null;
                child.target();
            }
        }

        public static class RightRightOnGrandparentHasPrecedenceOverLeftAndRightOnParent {
            interface LeftLeftGrandParent {
            }

            interface LeftRightGrandParent {
            }

            interface LeftParent {
                void target();
            }

            interface RightLeftGrandParent {
            }

            interface RightRightGrandParent {
                @ExpectedMethod
                void target();
            }

            interface RightParent {
                void target();
            }

            abstract static class GrandParentClass implements LeftLeftGrandParent, LeftRightGrandParent, RightLeftGrandParent, RightRightGrandParent {
            }

            abstract static class ParentClass extends GrandParentClass implements LeftParent, RightParent {
            }

            abstract static class Child extends ParentClass {
            }

            void scenario() {
                Child child = null;
                child.target();
            }
        }
    }

    public static class MoreSpecificReturnTypeHasPrecedence {

        public static class RightWithMoreSpecificReturnOnParentTypeHasPrecedenceOverAllOthers {
            interface LeftLeftGrandParent {
                Serializable target();
            }

            interface LeftRightGrandParent {
                Serializable target();
            }

            interface LeftParent {
            }

            interface RightLeftGrandParent {
                Serializable target();
            }

            interface RightRightGrandParent {
                Serializable target();
            }

            interface RightParent {
                @ExpectedMethod
                String target();
            }

            abstract static class GrandParentClass implements LeftLeftGrandParent, LeftRightGrandParent, RightLeftGrandParent, RightRightGrandParent {
            }

            abstract static class ParentClass extends GrandParentClass implements LeftParent, RightParent {
            }

            abstract static class Child extends ParentClass {
            }

            void scenario() {
                Child child = null;
                child.target();
            }
        }

        public static class LeftWithMoreSpecificReturnTypeHasPrecedenceRightWithMoreSpecificReturnType {
            interface LeftLeftGrandParent {
                Serializable target();
            }

            interface LeftRightGrandParent {
                Serializable target();
            }

            interface LeftParent {
                @ExpectedMethod
                String target();
            }

            interface RightLeftGrandParent {
                Serializable target();
            }

            interface RightRightGrandParent {
                Serializable target();
            }

            interface RightParent {
                String target();
            }

            abstract static class GrandParentClass implements LeftLeftGrandParent, LeftRightGrandParent, RightLeftGrandParent, RightRightGrandParent {
            }

            abstract static class ParentClass extends GrandParentClass implements LeftParent, RightParent {
            }

            abstract static class Child extends ParentClass {
            }

            void scenario() {
                Child child = null;
                child.target();
            }
        }

        public static class ParentInterfaceWithMoreSpecificReturnTypeHasPrecedenceOverAllOthers {
            interface FirstParentInterface {
                Serializable target();
            }

            interface SecondParentInterface {
                Serializable target();
            }

            interface ThirdParentInterface {
                @ExpectedMethod
                String target();
            }

            abstract static class ParentClass implements FirstParentInterface, SecondParentInterface, ThirdParentInterface {
            }

            abstract static class Child extends ParentClass {
            }

            void scenario() {
                Child child = null;
                child.target();
            }
        }

        public static class InterfaceWithMoreSpecificReturnTypeHasPrecedenceOverGrandParentClass {
            interface FirstParentInterface {
                Serializable target();
            }

            interface SecondParentInterface {
                @ExpectedMethod
                String target();
            }

            abstract static class GrandParentClass {
                public abstract Serializable target();
            }

            abstract static class ParentClass extends GrandParentClass implements FirstParentInterface, SecondParentInterface {
            }

            abstract static class Child extends ParentClass {
            }

            void scenario() {
                Child child = null;
                child.target();
            }
        }

        public static class ParentInterfaceOnParentClassWithMoreSpecificReturnTypeHasPrecedenceOverGrandParentClass {
            interface SecondParentInterfaceParentInterface1 {
                Serializable target();
            }

            interface SecondParentInterfaceParentInterface2 {
                @ExpectedMethod
                String target();
            }

            interface FirstParentInterface {
                Serializable target();
            }

            interface SecondParentInterface extends SecondParentInterfaceParentInterface1, SecondParentInterfaceParentInterface2 {
            }

            abstract static class GrandParentClass {
                public abstract Serializable target();
            }

            abstract static class ParentClass extends GrandParentClass implements FirstParentInterface, SecondParentInterface {
            }

            abstract static class Child extends ParentClass {
            }

            void scenario() {
                Child child = null;
                child.target();
            }
        }

        public static class GrandParentInterfaceWithMoreSpecificReturnTypeHasPrecedenceOverAllOthers {
            interface FirstGrandParentInterface {
                Serializable target();
            }

            interface SecondGrandParentInterface {
                Serializable target();
            }

            interface ThirdGrandParentInterface {
                @ExpectedMethod
                String target();
            }

            interface FirstParentInterface {
                Serializable target();
            }

            interface SecondParentInterface {
                Serializable target();
            }

            abstract static class GrandParentClass implements FirstGrandParentInterface, SecondGrandParentInterface, ThirdGrandParentInterface {
            }

            abstract static class ParentClass extends GrandParentClass implements FirstParentInterface, SecondParentInterface {
            }

            abstract static class Child extends ParentClass {
            }

            void scenario() {
                Child child = null;
                child.target();
            }
        }

        public static class GrandParentInterfaceWithMoreSpecificReturnTypeHasPrecedenceOverFirstParentInterfaceWithMoreSpecificReturnType {
            interface FirstGrandParentInterface {
                Serializable target();
            }

            interface SecondGrandParentInterface {
                Serializable target();
            }

            interface ThirdGrandParentInterface {
                @ExpectedMethod
                String target();
            }

            interface FirstParentInterface {
                String target();
            }

            interface SecondParentInterface {
                Serializable target();
            }

            abstract static class GrandParentClass implements FirstGrandParentInterface, SecondGrandParentInterface, ThirdGrandParentInterface {
            }

            abstract static class ParentClass extends GrandParentClass implements FirstParentInterface, SecondParentInterface {
            }

            abstract static class Child extends ParentClass {
            }

            void scenario() {
                Child child = null;
                child.target();
            }
        }

        public static class GrandParentInterfaceWithMoreSpecificReturnTypeHasPrecedenceOverAllParentInterfacesWithMoreSpecificReturnType {
            interface FirstGrandParentInterface {
                Serializable target();
            }

            interface SecondGrandParentInterface {
                Serializable target();
            }

            interface ThirdGrandParentInterface {
                @ExpectedMethod
                String target();
            }

            interface FirstParentInterfaceParentInterface1 {
                String target();
            }

            interface FirstParentInterfaceParentInterface2 {
                String target();
            }

            interface FirstParentInterface extends FirstParentInterfaceParentInterface1, FirstParentInterfaceParentInterface2 {
            }

            interface SecondParentInterface {
                String target();
            }

            abstract static class GrandParentClass implements FirstGrandParentInterface, SecondGrandParentInterface, ThirdGrandParentInterface {
            }

            abstract static class ParentClass extends GrandParentClass implements FirstParentInterface, SecondParentInterface {
            }

            abstract static class Child extends ParentClass {
            }

            void scenario() {
                Child child = null;
                child.target();
            }
        }
    }

    public static class StaticMethodsInInterfacesAreIgnored {

        interface FirstInterface {
            static String target() {
                return null;
            }
        }

        interface SecondInterface {
            static String target() {
                return null;
            }
        }

        interface ThirdInterface {
            @ExpectedMethod
            String target();
        }

        abstract static class Child implements FirstInterface, SecondInterface, ThirdInterface {
        }

        void scenario() {
            Child child = null;
            child.target();
        }
    }
}
