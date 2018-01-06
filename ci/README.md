# CI configurations

This is a very simple and INSECURE docker configuration to quickly set up a Jenkins build against
a remote Windows slave for ArchUnit master.

## Preconditions

On the windows slave, install and then copy

* JDK 8 (http://www.oracle.com/technetwork/java/javase/downloads/index.html)
  * -copy-> c:\jenkins\jdk
* Git (https://git-scm.com/download/win)
  * -copy-> c:\jenkins\Git

## How to run it

Start the Jenkins master via

```
docker-compose up
```

Then login (admin:admin) and either access

```
localhost:8080/computer/WindowsSlave
```

and execute the displayed command at the windows slave, or
on the windows slave access the Jenkins node

```
$JENKINS_URL:8080/computer/WindowsSlave
```

and click the Java Webstart button.