**[WARNING!!!]**

Page imported from the old SlightlyMagic wiki. To be integrated into other wiki pages and/or README... or deleted.

---

**WORK IN PROGRESS**

### Install Maven

<http://maven.apache.org/download.html#Installation>.

To test your installation you should execute the following command:

`mvn --version`

You should see something like like this:

`Apache Maven 3.0.3 (r1075438; 2011-02-28 09:31:09-0800)`  
`Maven home: /opt/local/share/java/maven3`  
`Java version: 1.6.0_24, vendor: Apple Inc.`  
`Java home: /System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home`  
`Default locale: en_US, platform encoding: MacRoman`  
`OS name: "mac os x", version: "10.6.7", arch: "x86_64", family: "mac"`

#### Mac OSX

Maven should already be installed. To test installation, open the
Terminal application run the test command above.

#### Windows

1.  Use the above link to download the zip file.
2.  Unzip the file into a directory.
3.  Add the directory to your "PATH" variable.
4.  Open a command window
5.  Execute the test command above.

#### Linux

1.  Open your package manager e.g. Synaptic on Debian
2.  Install the Maven2 package
3.  Open a Terminal
4.  Execute the test command above.

### Install SVN

<http://subversion.tigris.org/>

To test your installation you should execute the following command:

`svn --version`

You should see something like like this:

`svn, version 1.6.17 (r1128011)`  
`compiled Jun  2 2011, 09:40:34`

#### Mac OSX

Maven should already be installed. To test installation, open the
Terminal application run the test command above.

#### Windows

1.  Use the above link to download the zip file.
2.  Unzip the file into a directory.
3.  Add the directory to your "PATH" variable.
4.  Open a command window
5.  Execute the test command above.

#### Linux

1.  Open your package manager e.g. Synaptic on Debian
2.  Install the Subversion package
3.  Open a Terminal
4.  Execute the test command above.

### Build Forge

From a terminal window, go to the directory where forge was checked out
via GIT. Update to the latest version of the code

`mvn scm:update`

Use this command to perform a simple build of the jar file

`mvn -U -B clean install`

Use this command to do a snapshot package build

`mvn -U -B clean -P osx,windows-linux install`

Use this command to do a snapshot package build of the Windows/Linux
package only

`mvn -U -B clean -P windows-linux install`

Use this command to do a snapshot package build of the Mac OSX package
only

`mvn -U -B clean -P osx install`

Use this command to do a snapshot package build and site deployment

`mvn -U -B clean -P osx,windows-linux install site deploy site:deploy`

Use this command to do full package build and upload to GoogleCode

`mvn -U -B clean -P osx,windows-linux install site release:clean release:prepare release:perform -Dusername="`<user>`" -Dpassword="`<password>`"`

where <user> and <password> are your GoogleCode credentials (typically
something like "you@gmail.com" "w4e4sdg")

### Build System Utilities

These utilities are used in the build process. They are automatically
included in the build. The links are for reference only.

[Google Upload](http://code.google.com/p/maven-gcu-plugin/wiki/Usage)

[Jar Bundler](http://www.informagen.com/JarBundler/)

[Create DMG Script](http://www.yoursway.com/free/#createdmg)

