
In the past, some people noticed java heap space errors and lengthy pauses. The memory requirements for Forge have increased over time. The default setting on your computer for the java heap space may not be enough to prevent the above problems.

The technically proficient can launch the forge jar with an argument from the CLI. The argument "-Xmx512m" may work for computers with 1 Gig of memory. Computers with 2 Gigs or more of memory should be able to use "-Xmx1024m" as an argument.

We have created several scripts that will launch the Forge jar with "-Xmx1024m" as an argument. People using Windows OS should double click the "forge.exe" file. People using Apple's Mac OS X should double click the "forge.command" file. People using one of the other *nix OS should double click the "forge.sh" file.

The script file must be located in the same folder as the "run-forge.jar" file and the "run-forge.jar" file name can not be changed. Otherwise, the scripts will not work.

If you have a low end machine you may find that the scripts above will prevent java heap space errors but will find that forge still runs very slowly at times.

In this case you can try the following. Remove the background jpg picture from /res/images/ui/ folder. You can try using LQ pictures rather than the high quality pictures. Or you can try removing all of the jpg pictures from the pics folder. You can also try using the old style battlefield UI rather than the newer battlefield UI.

We have changed the archiving format to ".tar.bz2" since this may help to store the file permissions for the Mac OS X launcher. The Windows launcher in this version does not require you to rename the forge JAR file to work properly. Please keep the forge JAR file name set to "run-forge.jar".
