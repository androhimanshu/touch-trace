# Gradle wrapper

The Gradle wrapper requires a binary `gradle/wrapper/gradle-wrapper.jar`,
which cannot be created as a text file. Generate it locally once and
commit the result so everyone builds with a pinned Gradle version.

## Generate locally

With Gradle 8.7+ installed:

```bash
gradle wrapper --gradle-version 8.7 --distribution-type bin
```

This creates:

```
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
```

Commit all four. After that you can build with `./gradlew assembleDebug`
instead of a system Gradle install.

## CI

`.gitlab-ci.yml` uses the `gradle:8.7-jdk17` Docker image and calls
`gradle` directly, so CI does not depend on the committed wrapper. Once
you commit the wrapper you may switch the CI scripts to `./gradlew` for
fully reproducible builds.
