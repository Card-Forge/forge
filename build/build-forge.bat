@REM The lines starting with "REM" are comments and give hints about debugging possibilities

@REM delete the following line to reenable the output of the commands executed
@echo off
echo.
echo Forge SVN Builder V1.3
echo.
echo By UnderFlow
echo This script is for the most part a modified version of existing Forge build scripts. The original creators of those scripts or script parts are (in chronological order):
echo Snacko
echo Fnoed
echo Chris H.
echo.
echo We would also like to thank all Forge contributors.
echo.
echo For more information visit http://www.slightlymagic.net/forum/viewtopic.php?f=26^&t=2767
echo.

if "%~1"=="" (echo Please start this file with a command line parameter containing the desired output path in double quotes
	goto :end
)
echo Target directory: %~1
echo.


echo.
echo ---------- 1: Updating SVN ---------------------------
echo.
if exist forge-svn (
	build\svn.exe update forge-svn
) else (
	build\svn.exe checkout http://cardforge.googlecode.com/svn/src/ forge-svn
)


echo.
echo ---------- 2: Creating jar file ----------------------
echo.
REM remove the "2>NUL" to reenable the error output stream of the following command

java -jar build\ecj-3.5.2.jar forge-svn\src -1.6 -classpath forge-svn\src;forge-svn\res\lib\google-collections-1.0.jar;forge-svn\res\lib\java-image-scaling-0.8.4.jar;forge-svn\res\lib\miglayout-3.7.3.1-swing.jar;forge-svn\res\lib\jl1.0.1.jar;forge-svn\res\lib\napkinlaf-1.2.jar;forge-svn\res\lib\nimrodlf.jar;forge-svn\res\lib\substance.jar;forge-svn\res\lib\java-yield-1.0-SNAPSHOT-jar-with-dependencies.jar;forge-svn\res\lib\xstream-1.3.1.jar;forge-svn\res\lib\xpp3_min-1.1.4c.jar;forge-svn\res\lib\minlog-1.2.jar;forge-svn\res\lib\swingx-1.6.1.jar;forge-svn\res\lib\testng-6.0.1.jar -g:none -d output 2>NUL

mkdir output\META-INF
copy forge-svn\build\manifest.forge output\META-INF\MANIFEST.MF /Y
cd output
REM remove the ">NUL" to reenable the standard output stream of the following command
..\build\7z.exe a -tzip -r -mx=9 ..\run-forge.jar *.class META-INF/MANIFEST.MF > NUL


echo.
echo ---------- 3: Copying Forge to target directory ------
echo.
cd ..
REM If you you want more feedback from the robocopies, remove some of the parameters:
REM /NJH -> no job header
REM /NJS -> no job summary
REM /NDL /NFL -> no directory list / no file list, respectively
robocopy .\forge-svn %1 forge.properties /NDL /NFL /NJH /NJS
robocopy . %1 run-forge.jar /NDL /NFL /NJH /NJS
robocopy .\build %1 forge.exe /NDL /NFL /NJH /NJS
robocopy .\forge-svn\res "%~1\res" /E /XF forge.preferences /XF display_new_layout.xml /XD .svn /NDL /NFL /NJH /NJS


echo.
echo ---------- 4: Cleaning up ----------------------------
echo.
rmdir output /s /q
del run-forge.jar

:end
pause