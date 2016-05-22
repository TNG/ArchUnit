package com.tngtech.archunit.core;

@SuppressWarnings("serial")
public class ReflectionNotPossibleException extends RuntimeException {
    private final String owner;
    private final String name;
    private final String desc;

    public ReflectionNotPossibleException(String owner, String name, String desc) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }

    @Override
    public String getMessage() {
        return String.format("Couldn't determine an unique reflective target for %s.%s with descriptor %s. " +
                "This is likely the case, because the target was not uniquely determinable, e.g. because " +
                "the target was an interface method inherited from several sources. If you think, the API should " +
                "be extended to incorporate all possible reflective targets in case the target is not unique, " +
                "please file a feature request. If you think the target should be uniquely determinable, please " +
                "file a bug report.", owner, name, desc);
    }
}
