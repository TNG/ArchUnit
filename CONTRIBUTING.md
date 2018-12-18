# Contributing

Contributions are very welcome. The following will provide some helpful guidelines.

## How to build the project

ArchUnit requires at least JDK 9 to build. The following is just an example input/output from a 
Unix command line. Windows users should use `gradlew.bat` instead.

```
$ cd /path/to/git/clone/of/ArchUnit
$ ./gradlew showJdkVersion
Configured JDK: 1.9
$ ./gradlew build
```

You can configure the JDK Gradle uses according to the 
[Gradle User Guide](https://docs.gradle.org/current/userguide/build_environment.html)

## How to contribute

If you want to submit a contribution, please follow the following workflow:

* Fork the project
* Create a feature branch
* Add your contribution
* When you're completely done, build the project and run all tests via `./gradlew clean build -PallTests`
* Create a Pull Request

### Commits

Commit messages should be clear and fully elaborate the context and the reason of a change.
If your commit refers to an issue, please post-fix it with the issue number, e.g.

```
Issue: #123
```

Furthermore, commits should be signed off according to the [DCO](DCO).

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
