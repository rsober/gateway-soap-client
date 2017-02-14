# gateway-soap-client

This repository contains the JAX-WS java client for invoking SOAP via the gateway

## Dependencies

The following dependencies are required to run the gateway-soap-client project. 
- Java 8 JDK

Ensure that you have the correct version of Java available from the command line.

```
[ rsnyder@Administrators-MacBook-Pro:~/platform/gateway ] $ java -version
java version "1.8.0_66"
Java(TM) SE Runtime Environment (build 1.8.0_66-b17)
Java HotSpot(TM) 64-Bit Server VM (build 25.66-b17, mixed mode)
```

The exact output may differ on your system, but make sure that the version is some flavor of 1.8.0.

Ensure that wsimport is also available on via the command line.

```
[ rsnyder@Administrators-MacBook-Pro:~/platform/gateway-soap-client ] $ wsimport -version
wsimport version "2.2.9"
```

The following are useful to have, but not required.
- SoapUI (latest version)
- Eclipse

## Getting started

Generate the Eclipse project files by running the following task from the command line.

```bash
./gradlew eclipse
```

You can now create a new Eclipse project from the generated Eclipse files.

Run the tests to make sure you have everything set up correctly.

```bash
./gradlew test
```

The tests should pass successfully.


## Proguard

Run the following to reduce the size of the jar. Only proguard 5.0 has been verified to work.

```bash
proguard.sh @proguard.conf
```