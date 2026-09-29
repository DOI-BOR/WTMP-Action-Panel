# USBR WTMP Action Panel
This repository contains the Java scripts related to the Action Window for the Bureau of Reclamation (USBR) Water Temperature Modeling Platform (WTMP) WAT. 
These files handle the actions and set up of the WTMP Action Window in the WAT.
These files are in Java.

## Dependencies
* com.fasterxml.jackson.annotation
* com.fasterxml.jackson.core
* com.fasterxml.jackson.databind
* com.fasterxml.jackson.databind.annotation
* com.fasterxml.jackson.databind.module
* com.fasterxml.jackson.dataformat.csv
* com.google.common.flogger
* com.rma.client
* com.rma.commands
* com.rma.editors
* com.rma.event
* com.rma.factories
* com.rma.io
* com.rma.message
* com.rma.model
* com.rma.swing
* com.rma.swing.excel
* com.rma.swing.tree
* com.rma.ui
* com.rma.util
* gov.usbr.wq.merlindataexchange
* gov.usbr.wq.merlindataexchange.configuration
* gov.usbr.wq.merlindataexchange.io.MerlinDataExchangeTimeSeriesReader
* gov.usbr.wq.merlindataexchange.io.wq.MerlinDataExchangeProfileReader
* gov.usbr.wq.merlindataexchange.parameters
* hec.data
* hec.dssgui
* hec.geometry
* hec.gfx2d
* hec.gui
* hec.heclib.dss
* hec.heclib.util
* hec.hecmath
* hec.hecmath.computation
* hec.hecmath.functions
* hec.io
* hec.io.impl
* hec.lang
* hec.model
* hec.ui
* hec.util
* hec2.model
* hec2.plugin.model
* hec2.plugin.util
* hec2.wat
* hec2.wat.client
* hec2.wat.command
* hec2.wat.event
* hec2.wat.factories
* hec2.wat.io
* hec2.wat.model
* hec2.wat.plugin
* hec2.wat.plugin.ceQualW2
* hec2.wat.plugin.ceQualW2.model
* hec2.wat.ui
* hec2.wat.util
* net.sf.jasperreports.engine
* net.sf.jasperreports.engine.data
* net.sf.jasperreports.engine.design
* net.sf.jasperreports.engine.export
* net.sf.jasperreports.engine.export.ooxml
* net.sf.jasperreports.engine.util
* net.sf.jasperreports.engine.xml
* net.sf.jasperreports.export
* net.sf.jasperreports.repo
* org.apache.poi.ss.usermodel
* org.apache.poi.xssf.usermodel
* org.eclipse.jgit.api
* org.eclipse.jgit.api.errors
* org.eclipse.jgit.lib
* org.eclipse.jgit.storage.file
* org.eclipse.jgit.transport
* org.gitlab4j.api
* org.gitlab4j.api.models
* org.jdesktop.swingx
* org.jdesktop.swingx.JXTree
* org.jdesktop.swingx.decorator
* org.jdesktop.swingx.treetable
* org.jdom
* org.jdom.input
* org.jfree.data.time
* org.python.core
* org.python.google.common.io
* org.python.util
* rma.lang
* rma.swing
* rma.swing.list
* rma.swing.table
* rma.swing.text
* rma.swing.tree
* rma.util

## Building
There are two options for building this package, using GitHub or a local build. All production and offical testing builds are created via the GitHub build system. This requires someone with Reclamation credentials to trigger a build and, if necessary, work through the official build progression to create artificats for every package. Local builds should be used when testing or when builds via GitHub are unavailable.

### Building on GitHub
The build process on GitHub is already preconfigured within GitHub actions. There are two actions available for building, a test build and a publishing build. These differ only in that the publishing build places an artifact on to the GitHub packaging system following the YYYY.MM.DD naming convention. This convention does prevent multiple releases from occuring within a single day. If this becomes an issue, please contact a member of the core Reclamation WTMP development team for assistance. 

The test build is available only for redundancy. Test builds should be done locally to reduce use of limited GitHub Action resources. A passing local test build is a prerequisite for triggering the publishing workflow.

### Building locally
A local build occurs outside of GitHub on a developer's own machine. This is the preferred mechanism for confirming changes are functional, and a local test build should be made and validated before making a pull request. Additionally, a local build may be utilized when GitHub is unavailable. 

The specific configuration needed for local will vary based on the developer's environment and the specific repository being built. These instructions are specified for the Bureau of Reclamation's environment on a Windows machine. This process is intended to conform with Department of Interior security practices and may differ nontrivially in other environments. Producing a local build has the following steps:

1) Installing Java 17.0.x

	The version of Java necessary for gradle is currently misaligned with the version that's being utilized within the WAT itself. This requires downloading a separate version of Java to be able to support the gradle build process. It is recommended that users download and extract OpenJDK 17.0.2 from https://jdk.java.net/archive/. This can be place anywhere on the development machine where the user has read/write/execute permissions outside of the target repository folder.


2) Modifying the user gradle environment

	Modifications to gradle properties is necessary to retarget against the previous Java version and to utilize system certificates. This will require editing a gradle properties file. Gradle will, by default, attempt to utilize whatever Java version is on the machine system path. This version is unlikely to be compatible with either HEC-WAT or Gradle. Additionally, the Reclamation environment automatically blocks the SSL connections needed to obtain upstream dependencies unless SSL certificates are made available to gradle. 

	<br>Within the Windows user folder (e.g. C:/Users/dloney), there should be a .gradle folder (e.g. C:/Users/dloney/.gradle) that contains gradle configuration information. The folder is hidden by default and will require enabling hidden folders/files within Windows Explorer. If the folder does not exist, create it. Similarly, within the .gradle folder, there should be a gradle.properties file (C:/Users/dloney/.gradle/gradle.properties). If one doesn't exist, create it. The .properties should be the extension for the file rather than .txt or another extension.
	
	Open the gradle.properties file and add the following two lines:
	
	```
	org.gradle.java.home=/Path/to/Java 17.0.x
	org.gradle.jvmargs=-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT
	```
	
	The path in the first line should be modified to point to the top level jdk-17.0.x folder at the location from the previous step. The second line does not require any modification.  


3) Requesting an USACE-HEC Nexus access token

	A local build requires access to the USACE-HEC Nexus package environment. Through an interagency partnership, USACE has granted Reclamation access to the HEC build system to support development of the WTMP. This access is programmatic, requiring both a username and API token to authenticate into the Nexus system. If you intend to build the WTMP, please reach out to both Reclamation and USACE-HEC to discuss receiving programmatic access.


4) Requesting an Reclamation GitHub access token 

	A local build also requires access to pull WTMP packages published by Reclamation on GitHub. This requires programmatic access into GitHub through an access token. The production builds of all Reclamation packages are made available via the GitHub packaging system. For users to access Reclamation packages programmatically, they must create a classic personal access token with write:packages permissions. This will allow download of packages from the GitHub packaging system. A user may require different token permissions if they are trying to retarget off packages published by Reclamation.


5) Modifying the local build script 

	The usernames and access tokens obtained in the previous two steps need to be added to the build_local.bat script located in the repository folder. There are four lines at the top of the batch script that must be updated with usernames and access tokens. This step places these into the environment variable for the gradle build process at run time. Care should be taken that the tokens are not committed into the Git history to comprise the token credentials. 


6) Running the local build script

	The build_local.bat file should be ran within the command line. This will start the gradle build daemon and begin the build process. If gradle is not currently running in the environment, it may take some time for the script to initialize the daemon. Gradle initialization time should be reduced when building the target or downstream repositories subsequently. 
	
	<br>The end of the build process will either result in a green "BUILD SUCCESSFUL" notification or a red "BUILD FAILED" notification. The build is configured to enable a stack trace on failure to aid in debugging. Resolve any errors and repeat the build until it successfully completes.
	
	<br>It is important to note that the build will execute for whatever branch the repository is currently on. This is also important for downstream repositories that will potentially key off of local packages named by branch, specifically Merlin-to-HEC and WTMP-Git-Library. 


7) Publishing the gradle package

	When the gradle build completes, the user may access the build/libs folder within the target repository to obtain the complied usbr-actionpanel-plugin-YYYY.MM.DD.jar file. This may be sufficient to replace the existing Action Panel jar within the WAT if one is testing only the Action Panel. However, downstream repositories may require access through gradle to meet their build dependencies lest they default back to GitHub. To do this, one needs to publish the local upstream dependency for gradle to access. 
	
	<br>If building downstream repositories, make the Action Panel local build available to them via the following command from the command prompt located in the target repository:
	
	```
	./gradlew publishToMavenLocal
	```
	
	This will make the package available for downstream packages to access.


8) Stopping gradle 

	By default, gradle will leave the daemon running. This is not necessarily an issue if one is testing in rapid succession or building subsequent repositories. However, this might not be desired in all cases as the daemon consumes a significant amount of memory. To terminate the gradle daemon, open the task manager, find the OpenJDK platform library, and end the task. Force ending the task is not recommended if multiple instances are in use as it is challenging to assign a Java instance to a specific program without additional effort. 
