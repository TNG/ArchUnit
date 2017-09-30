---
layout: splash
permalink: /motivation
---

# Why test your architecture?

Most developers working in larger projects will know the story, where once upon a time somebody experienced looked at
the code and drew up some nice architecture diagrams, showing the components the system should consist of, and how
they should interact. But when the project got bigger, the use cases more complex, and new developers dropped in and
old developers dropped out, there were more and more cases where new features would just be added in any way that fit.
And suddenly everything depended on everything and every change could have an unforeseeable effect on any other
component. Of course you could have one or several experienced developers, having the role of the architect, who look
at the code once a week, identify violations and correct them. But a safer way is to just define the components in
code and rules for these components that can be automatically tested, for example as part of your continuous integration
build.

Especially in an agile project, where the role of the architect might even be distributed, developers should all
have a common language and understanding of the components and their relations. When the project evolves, the
components you talk about, have to evolve, too. Otherwise strange constructs will suddenly appear, trying to
force use cases into a component structure that is not at all fitting. If you have automatic architecture tests,
you can evolve the rules, see where old components need to change, and ensure that new components comply to the
common understanding of the developers/architects. Altogether this will contribute to the quality of the code base and 
prevent a decline in development speed. Furthermore new developers will have a much easier time to get acquainted with
the code and get up to speed with their development.

# Why use ArchUnit?

There are several free tools out there to automatically test for dependencies between packages and classes, and 
several tools with different focus that can be used for this purpose as well, like [AspectJ](https://eclipse.org/aspectj/),
[Checkstyle](http://checkstyle.sourceforge.net/) or [FindBugs](http://findbugs.sourceforge.net/).
So why would you need another tool for this?

Each of these tools has a more or less convenient way to specify rules like packages matching '..service..'
may not access packages matching '..controller..', and similar. Some tools, like AspectJ, enable you to specify
more powerful rules, like subclasses of class A that are annotated with @X may only access methods annotated
with @Y. But each of those tools also has some limitations you might run into, if your rules become more complex.
This might be as simple as not being able to specify a pointcut to only apply to interfaces in AspectJ or no
back references within pointcuts. Other tools are not even able to define complex rules, like AspectJ allows, at all. 
Furthermore, you might need to learn a new language to specify the rules, or need different infrastructure to evaluate
them.

For some tests of coding rules the Java Reflection API provides a convenient way to talk about your code. For
example you can test some serialization properties of the return values of methods of classes annotated with
@Remote or similar. ArchUnit strives to bring this convenience to a level of code structures instead
of mere simple classes. ArchUnit provides simple predefined ways to test the typical standard cases, like package
dependencies. But it also is fully extensible, providing a convenient way to write custom rules where imported
classes can be accessed similarly to using the Reflection API. In fact, the imported structure provides a natural way to
use the full power of the Reflection API for your tests. But it also allows to write tests looking at field accesses,
method or constructor calls and subclasses. Furthermore, it does not need any special infrastructure, nor any new
language, it is plain Java and rules can be evaluated with any unit testing tool like [JUnit](http://junit.org/).

