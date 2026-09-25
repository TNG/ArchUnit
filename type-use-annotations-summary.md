# Making TYPE_USE annotations visible as dependencies

## Problem

Annotations declared with `@Target(TYPE_USE)` (or `TYPE_PARAMETER`) only — the most
prominent example being Checker Framework's `@Nullable` — are invisible to ArchUnit.

Because such an annotation has no `FIELD`/`METHOD`/`PARAMETER` target, `javac` does **not**
write it into the normal declaration-annotation tables (`RuntimeVisible/InvisibleAnnotations`).
It writes it into the separate type-annotation table (`RuntimeVisibleTypeAnnotations`,
JVMS §4.7.20). ArchUnit's importer never read that table, so:

- `javaClass.getDirectDependenciesFromSelf()` never contained the annotation type, and
- `dependOnClassesThat().resideInAPackage("org.checkerframework.checker.nullness.qual")`
  found nothing.

## Scope of this change ("the moderate path")

Make TYPE_USE annotations surface as **dependencies of the enclosing class**. This is the
smallest, self-contained increment that fixes the reported problem.

It deliberately does **not** attach the annotation to a specific type-usage position
(e.g. `@Nullable` on _this field's_ type vs. _that type argument_). Full positional modeling
would require decoding ASM's `TypeReference`/`TypePath` and introducing annotated-type nodes
across ~17 positions — a much larger feature, out of scope here.

## How it works

The data flow mirrors how declaration annotations are already handled:

1. **ASM capture** — `JavaClassProcessor` now overrides
   `visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible)` on the
   class visitor, `FieldProcessor`, and `MethodProcessor`. Each reuses the existing
   `AnnotationProcessor`, so annotation **values** (e.g. `@Foo(Bar.class)`) are parsed too.
   This covers field types, method return / formal-parameter / throws / type-parameter
   positions, and nested positions such as `List<@X SomeType>`.
2. **Storage / wiring** — a new `DeclarationHandler.onDeclaredTypeAnnotations(...)` callback,
   implemented by `ClassFileProcessor.ClassDetailsRecorder` (attributes to the class currently
   being processed and registers the annotation types for resolution), stores the builders
   per class in `ClassFileImportRecord` (`typeAnnotationsByOwner`).
3. **Model build** — a new `ImportContext.createTypeAnnotations(JavaClass)` method, implemented
   in `ClassGraphCreator`, builds real `JavaAnnotation<JavaClass>` objects (owner = the class)
   into a `Set`, populated on `JavaClass` during `completeAnnotations`.
4. **Dependency derivation** — `JavaClassDependencies` gains a `typeAnnotationDependenciesFromSelf()`
   stream that reuses the existing annotation-dependency logic (`Dependency.tryCreateFromAnnotation`
   plus the member visitor). So both the annotation type itself and any class/enum members it
   references become dependencies, with origin = the annotated class.

## Files changed

Production (`archunit/src/main/java`):

| File | Change |
|------|--------|
| `core/importer/JavaClassProcessor.java` | Override `visitTypeAnnotation` on class/field/method visitors; collect per-processor and emit at `visitEnd`. Import `org.objectweb.asm.TypePath`. |
| `core/importer/DeclarationHandler.java` | New `onDeclaredTypeAnnotations(Set<JavaAnnotationBuilder>)`. |
| `core/importer/ClassFileProcessor.java` | Implement `onDeclaredTypeAnnotations` (store + register types to resolve). |
| `core/importer/ClassFileImportRecord.java` | `typeAnnotationsByOwner` storage + `addTypeAnnotations` / `getTypeAnnotationsFor`. |
| `core/domain/ImportContext.java` | New `createTypeAnnotations(JavaClass)`. |
| `core/importer/ClassGraphCreator.java` | Implement `createTypeAnnotations` (build a `Set<JavaAnnotation<JavaClass>>`). |
| `core/domain/JavaClass.java` | `typeAnnotations` field, populate in `completeAnnotations`, package-private `getTypeAnnotations()`. |
| `core/domain/JavaClassDependencies.java` | Refactor shared `dependenciesOfAnnotations(...)`; add `typeAnnotationDependenciesFromSelf()` to the dependency set. |

Tests (`archunit/src/test/java`):

| File | Change |
|------|--------|
| `core/domain/JavaClassTest.java` | New test `direct_dependencies_from_self_by_type_annotation` + TYPE_USE example classes/annotations. |
| `core/importer/ImportTestUtils.java` | Implement `createTypeAnnotations` on the `ImportContextStub`. |

## Design decisions

- **Class-level granularity.** Every type-use annotation is attributed to its enclosing class,
  not to an exact type node. `TypePath`/`TypeReference` are intentionally ignored.
- **No new public API.** `JavaClass.getTypeAnnotations()` is package-private (used only for
  dependency derivation), so there is no `@PublicAPI` surface change. Exposing a public
  accessor to *query* type-use annotations would be a natural follow-up.
- **`Set`, not `Map`.** Several type-use annotations of the same type can attach to one class
  (e.g. two `@Nullable` fields), so a `Set` is built directly rather than the
  map-keyed-by-type produced by `buildAnnotations`.
- **Both retention kinds captured.** Like the existing `visitAnnotation`, the `visible` flag is
  ignored, so both `RuntimeVisible-` and `RuntimeInvisibleTypeAnnotations` are read. (Checker's
  `@Nullable` is `RUNTIME`.)

## Verification

- New test `JavaClassTest.direct_dependencies_from_self_by_type_annotation` passes. It uses
  `@Target(TYPE_USE)`-only annotations on a field type, a type argument, a method return type,
  a parameter type, and an annotation-with-class-member, and asserts each appears in
  `getDirectDependenciesFromSelf()` (annotation type + annotation-member dependencies).
- Full regression: `JavaClassTest` (110 tests) and `ClassFileImporterAnnotationsTest` (26 tests)
  both pass with 0 failures / 0 errors.
- `./gradlew :archunit:compileTestJava` compiles cleanly.

## Not covered (possible follow-ups)

- Type-use annotations inside **method bodies** (local variables, casts, `instanceof`, `new`) via
  `visitInsnAnnotation` / `visitLocalVariableAnnotation` / `visitTryCatchAnnotation`.
- **Record components** (`visitRecordComponent().visitTypeAnnotation`) — currently covered only
  indirectly via the generated field/accessor.
- A **public** `getTypeAnnotations()` accessor and/or **positional** modeling of type-use
  annotations on the type nodes themselves.