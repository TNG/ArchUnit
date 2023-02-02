# Contributing

Contributions are very welcome. The following will provide some helpful guidelines.

## How to build the project

ArchUnit requires at least JDK 15 to build.
The following is just an example input/output from a Unix command line.
Windows users should use `gradlew.bat` instead.

```
$ cd /path/to/git/clone/of/ArchUnit
$ ./gradlew showJdkVersion
Configured JDK: 15
$ ./gradlew build
```

You can configure the JDK Gradle uses according to the 
[Gradle User Guide](https://docs.gradle.org/current/userguide/build_environment.html)

## How to contribute

If you want to tackle an existing issue please add a comment to make sure the issue is sufficiently discussed
and that no two contributors collide by working on the same issue. 
To submit a contribution, please follow the following workflow:

* Fork the project
* Create a feature branch
* Add your contribution
* When you're completely done, build the project and run all tests via `./gradlew clean build -PallTests`
* Create a Pull Request

### Commits

Commit messages should be clear and fully elaborate the context and the reason of a change.
Each commit message should follow the following conventions:

* it may use markdown to improve readability on GitHub
* it must start with a title
  * less than 70 characters
  * starting lowercase
  * written in imperative style as to complete the statement "if applied this commit will" (e.g. "fix race condition when loading import plugins")
* if the commit is not trivial the title should be followed by a body
  * separated from the title by a blank line
  * explaining all necessary context and reasons for the change
* if your commit refers to an issue, please post-fix it with the issue number, e.g. `Issue: #123` or `Resolves: #123`

A full example:

```
report classes contained in multiple PlantUML components as violation

So far when checking an `ArchRule` based on `PlantUmlArchCondition` we were throwing an exception
if a class was contained in multiple diagram components. 
This causes problems for legacy code bases where some classes might violate the conventions
in such a way, which should be frozen as violations to be iteratively fixed. 
But throwing a `ComponentIntersectionException` prevents any such approach. 
We thus replace the `ComponentIntersectionException` by a regular rule violation that can be treated
like any other violation of the architecture and in particular be frozen via `FreezingArchRule`.

Resolves: #960
```

Furthermore, commits must be signed off according to the [DCO](DCO).

### Pull Requests

If your Pull Request resolves an issue, please add a respective line to the end, like

```
Resolves #123
```

### Formatting

Please adjust your code formatter to the general style of the project. To help with this, you can
use the code formatters within the [develop](develop) folder. Furthermore, imports should be
added in a consistent way, in particular lay out your imports

* java.*
* javax.*
* all other imports
* all static imports

and don't use any `*` imports at any time.
