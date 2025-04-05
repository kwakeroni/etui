# Exiftool Template UI

This application provides a GUI for [ExifTool](https://exiftool.org/),
a utility to add and modify metadata in image files.

In contrast to [ExifToolGUI](https://exiftool.org/gui/),
which is batch-oriented (applying the same metadata),
this application is template-oriented
(applying a uniform structure of metadata).

This application was created as a private tool, based on my own needs.
So don't expect new features to be added frequently.

This project is not associated with ExifTool or its creator.

## Running
There are currently no binary distributions of Etui.
The application must be built locally.

### Requirements
* [Java 22](https://openjdk.org/projects/jdk/22/)
 * The `JAVA_HOME` environment variable needs to be set and point towards the Java installation
* [Exiftool](https://exiftool.org/)


### Building and running
The application can be run using the build-and-run scripts:

#### _Linux (bash)_

```console
./etui.sh
```

Environment variables, including `JAVA_HOME` can be set by creating a local `setEnv.sh` script:
```bash
export JAVA_HOME=/c/Programs/jdk-22.0.1
```

#### _Windows (cmd)_
```console
etui.cmd
```

Environment variables, including `JAVA_HOME` can be set by creating a local `setEnv.cmd` script:
```bat
SET JAVA_HOME=C:\Programs\jdk-22.0.1
```

#### Taking control of the build process
Etui is a [Maven](https://maven.apache.org/) project 
and can be built using the standard build procedure.
The project provides a [Maven wrapper](https://maven.apache.org/wrapper/)
to provide the right version of Maven when it is not installed.

```console
./mvnw verify
```

To start the application, run the `Etui` main class from the root package.
Using maven this can be done using the `local-exec` profile with the
[exec-maven-plugin](https://www.mojohaus.org/exec-maven-plugin/)
```console
./mvnw exec:exec -Plocal-exec
```



