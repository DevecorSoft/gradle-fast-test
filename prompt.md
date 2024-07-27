I'm designing a intellij plugin that to build and run single kotlin test suite, which is named gradle-necessary-build.
Just like intellij bundled gradle tool, but I want to make it compile related source code only instead of the whole project code in test and src.
Basically, I think the plugin should do the following steps:
1. when I want to run from single file src/test/kotlin/example/ExampleTest.kt, to generate a temp gradle.build.kts file and assign src dirs with `[src/main/kotlin/example, src/test/kotlin/example]`
2. run gradle build with the temp gradle.build.kts file

the temp gradle.build.kts should be only the test related, should not be included by version control system. where I can place it?
