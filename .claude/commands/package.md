---
description: Package the application as a JAR
---

Build and package SieveEditor as an executable JAR with dependencies.

Execute: `cd app && mvn clean package`

After successful packaging:
1. Confirm JAR location: `app/target/SieveEditor-jar-with-dependencies.jar`
2. Show file size
3. Provide run command: `java -jar app/target/SieveEditor-jar-with-dependencies.jar`
