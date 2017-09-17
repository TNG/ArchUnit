# Contributing

Contributions are very welcome. The following will provide some helpful guidelines.

## ArchUnit Contributor License Agreement

* You will only submit contributions where you have authored 100% of the content.
* You will only submit contributions to which you have the necessary rights. 
This means in particular, that if you are employed you have received the necessary permissions 
from your employer to make the contributions.
* Whatever content you contribute will be provided under the project license(s) (see !["LICENSE.txt"](LICENSE.txt))

## How to contribute

If you want to submit a contribution, please follow the following workflow:

* Fork the project
* Create a feature branch
* Add your contribution
* When you're completely done, build the project and run all tests via `./gradlew clean build -PallTests`
* Create a Pull Request

### Commit messages

Commit messages should be clear and fully elaborate the context and the reason of a change.
If your commit refers to an issue, please post-fix it with the issue number, e.g.

```
Issue: #123
```

### Pull Requests

If your Pull Request resolves an issue, please add a respective line to the end, like

```
Resolves #123
```

Furthermore, please add the following line to your Pull Request description:

```
I hereby agree to the terms of the ArchUnit Contributor License Agreement.
```

### Formatting

Please adjust your code formatter to the general style of the project. To help with this, you can
use the code formatters within the ![develop](develop) folder. Furthermore, imports should be
added in a consistent way, in particular lay out your imports

* java.*
* javax.*
* all other imports
* all static imports

and don't use any `*` imports at any time.