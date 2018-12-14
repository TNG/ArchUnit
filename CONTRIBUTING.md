# Contributing

Contributions are very welcome. The following will provide some helpful guidelines.

## How to build the project

### Building locally on an OSX machine

Assumptions

* Java 9
* Gradle

The following is just an example input/output from the command line

```
$ java --version
openjdk 9.0.4
OpenJDK Runtime Environment (build 9.0.4+11)
OpenJDK 64-Bit Server VM (build 9.0.4+11, mixed mode)
$ cd /path/to/git/clone/of/ArchUnit
$ ./gradlew clean build
```


### Building locally on a Linux machine

TBD

### Building locally on a Windows machine

TBD


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
