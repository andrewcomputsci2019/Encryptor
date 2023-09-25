# Encryptor
___
A lightweight simple cross-platform desktop application that can encrypt files
using AES-256 through both password and key-based encryption and decryption
methods.

## how to use
When you launch the application, the home screen appears.
This is your starting point for encrypting or decrypting a file.
Both options will guide you through a step-by-step wizard.
Here, you can choose your preferred encryption type: password or key-based,
and specify the output directory for the encrypted or decrypted file.
For a quicker process, you can simply drag and drop files, bypassing the file selection menu.
___
## Demo

___
## How to build and run
The easiest way to build and run is
to import this project into any I.D.E that supports gradle and run the gradle run task.
Otherwise, the following shell script can be used to run from the command line.
```shell
  ##cd into project root dir with the gradlew and gradlew.bat file
  ##if on Windows run the gradlew.bat file
  ##change /path_to_jdk_directory to your jdk distribution
  ./gradlew -Dorg.gradle.java.home=/path_to_jdk17_directory
  ./gradlew run
```
### Building a binary distribution
This is similar to the above, but instead you will run the jpackage task.
The output of this task will be located in the build/jpackage folder.
Do keep in mind to build a binary distribution it may require external tools, like WIX for windows.
