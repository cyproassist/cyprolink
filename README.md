# cyprolink
CyProAssist server application based on [eniLINK](http://github.com/enilink/enilink).

## Prerequisites
You need the [Eclipse IDE](http://www.eclipse.org) with the following plug-ins installed:
- Plug-in Development Environment (PDE)
  - PDE is a set of modules for the development of OSGi applications in Eclipse.
  - It can be installed through the Mars or Neon update sites.
- [Scala IDE](http://scala-ide.org/)
  - The Scala IDE is required for the editing and compilation of Scala code.
- M2E - Maven integration for Eclipse
  - M2E is required for building Maven projects within Eclipse.
- M2E Scala integration (m2eclipse-scala)
  - Can be installed from the following update site: http://alchim31.free.fr/m2e-scala/update-site
  - Allows M2E to compile Scala code.

## Set-up the build

### Import the project into Eclipse
- Git clone the project into your workspace.
- File > Import ... > Existing Maven Projects and select the root directory of the beginner project.
- Let M2E search for missing connectors (if any) and install them.

### Set-up the target platfrom
PDE has the concept of a target platform that specifies the runtime environment of a OSGi application.
- Open the eniLINK.target file with the "Target Editor"
- Use the buttons "Update" and "Reload" if required.
- Select "Set as Target Platform" on the top-right corner.
- Maybe "Maven > Update Project ..." (Alt+F5) is required on the projects to build them successfully.

### Run the application
- Open the "Run Configurations" dialog by invoking Run > Run Configutations ...
- Select Eclipse Application > eniLINK-web (Duplicate entries are due to a bug in Eclipse.)
- Use "Add Required Plug-ins" on the Plug-ins tab (Only the first time if required.)
- Press "Run" to start the application.
- The application should now be available at: [http://localhost:10080/cyprolink/](http://localhost:10080/cyprolink/)

