# gradle fast test

Run your unit test with a very fast approach. 

## lifecycles

```mermaid
flowchart TB
    start[start test for exampleTest.java] -->
    read[load .fastTest.config.yaml] -->
    import[source files scanning] -->
    generate[generate gradle task: fastTest] -->
    run[./gradlew fastTest -x compileJava -x compileKotlinJava] -->
    `end`[clean up]
```

## roadmap

- [] generate config to enable manually adding source files for a test file
- [] classpath resolver
- [] advanced scanning inside same package
- [] json-schema for config file
