# cyprolink
CyProAssist server application based on [eniLINK](http://github.com/enilink/enilink).

## Building
* This is a plain Maven project
* a full build can be executed via `mvn package`

## Running
* change to the folder `launch`
* run `mvn test -Pconfigure -DskipTests` to initialize or update a launch configuration
* run `mvn test` to (re-)start the eniLINK platform
* The application should now be available at: [http://localhost:8080/cyprolink/](http://localhost:8080/cyprolink/)

## Developing
* The project can be developed with any IDE supporting Java and Scala projects
* **IDEA:** `File > Project from existing sources...`
* **Eclipse:** `File > Import > Maven > Existing Maven Projects`