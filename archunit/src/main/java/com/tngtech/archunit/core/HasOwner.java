package com.tngtech.archunit.core;

public interface HasOwner<T> {
    T getOwner();

    interface IsOwnedByClass extends HasOwner<JavaClass> {
    }

    interface IsOwnedByMethod extends HasOwner<JavaMethodLike<?, ?>> {
    }
}
