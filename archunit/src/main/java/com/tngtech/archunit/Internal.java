package com.tngtech.archunit;

/**
 * Any element annotated with this annotation, is meant for internal use ONLY. Users of ArchUnit should never
 * directly access / extend / instantiate any object / member annotated with {@link Internal}.<br>
 * If you do so, you do at your own risk and such code might break with any (even minor) new release.
 */
public @interface Internal {
}
