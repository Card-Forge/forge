package forge.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.slightlymagic.braids.util.ClumsyRunnable;
import net.slightlymagic.braids.util.testng.BraidsAssertFunctions;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test the BuildInfo class.
 */
public class BuildInfoTest {

    /** System property name for the class path. */
    private static final String JAVA_CLASS_PATH = "java.class.path";
    /** Manifest attribute name for the Build ID. */
    private static final String MF_ATTR_NAME_BUILD = "Implementation-Build"; // NOPMD
                                                                             // by
                                                                             // Braids
                                                                             // on
                                                                             // 8/12/11
                                                                             // 10:29
                                                                             // AM
    /** Manifest attribute name for the version string. */
    private static final String MF_ATTR_NAME_VERSION = "Implementation-Version"; // NOPMD
                                                                                 // by
                                                                                 // Braids
                                                                                 // on
                                                                                 // 8/12/11
                                                                                 // 10:29
                                                                                 // AM

    /**
     * Test BuildInfo using a mock jar file.
     * 
     * @throws IOException
     *             rarely
     */
    @Test
    public final void test_BuildInfo_mockJar() throws IOException { // NOPMD by
                                                                    // Braids on
                                                                    // 8/12/11
                                                                    // 10:26 AM
        File jarAsFile = null;
        try {
            jarAsFile = this.makeTmpJarWithManifest("BuildInfoTest-", ".jar", new String[] {
                    BuildInfoTest.MF_ATTR_NAME_VERSION, "1.2.42", BuildInfoTest.MF_ATTR_NAME_BUILD, "4200" });

            final BuildInfo info = new BuildInfo(jarAsFile.getAbsolutePath());

            final String actualVersion = info.getVersion();
            Assert.assertEquals(actualVersion, "1.2.42", "versions match");

            final String actualBuildID = info.getBuildID();
            Assert.assertEquals(actualBuildID, "4200", "build IDs match");

        } finally {
            Assert.assertTrue(jarAsFile.delete(), "attempting to delete temporary jar file");
        }
    }

    /**
     * Test BuildInfo with one mock forge jar placed in the class path.
     * 
     * @throws IOException
     *             indirectly
     */
    @Test
    public final void test_BuildInfo_oneJarInCP() throws IOException { // NOPMD
                                                                       // by
                                                                       // Braids
                                                                       // on
                                                                       // 8/12/11
                                                                       // 10:26
                                                                       // AM
        final String origClassPath = System.getProperty(BuildInfoTest.JAVA_CLASS_PATH);
        File jarAsFile = null;

        try {
            jarAsFile = this.makeTmpJarWithManifest("forge-BuildInfoTest-", "-with-dependencies.jar", new String[] {
                    BuildInfoTest.MF_ATTR_NAME_VERSION, "1.2.43", BuildInfoTest.MF_ATTR_NAME_BUILD, "4201" });

            System.setProperty(BuildInfoTest.JAVA_CLASS_PATH,
                    jarAsFile.getAbsolutePath() + System.getProperty("path.separator") + origClassPath);

            final BuildInfo info = new BuildInfo();

            final String actualVersion = info.getVersion();
            Assert.assertEquals(actualVersion, "1.2.43", "versions match");

            final String actualBuildID = info.getBuildID();
            Assert.assertEquals(actualBuildID, "4201", "build IDs match");

        } finally {
            Assert.assertTrue(jarAsFile.delete(), "attempting to delete temporary jar file");
            System.setProperty(BuildInfoTest.JAVA_CLASS_PATH, origClassPath);
        }
    }

    /**
     * Test BuildInfo with two mock forge jars placed in the class path; this is
     * an error condition.
     * 
     * @throws IOException
     *             indirectly
     */
    @Test
    public final void test_BuildInfo_twoJarsInCP() throws IOException { // NOPMD
                                                                        // by
                                                                        // Braids
                                                                        // on
                                                                        // 8/12/11
                                                                        // 10:26
                                                                        // AM
        final String origClassPath = System.getProperty(BuildInfoTest.JAVA_CLASS_PATH);
        File jarAsFile1 = null;
        File jarAsFile2 = null;

        try {
            jarAsFile1 = this.makeTmpJarWithManifest("forge-BuildInfoTest-1-", "-with-dependencies.jar", new String[] {
                    BuildInfoTest.MF_ATTR_NAME_VERSION, "1.1.1", BuildInfoTest.MF_ATTR_NAME_BUILD, "1111" });

            jarAsFile2 = this.makeTmpJarWithManifest("forge-BuildInfoTest-2-", "-with-dependencies.jar", new String[] {
                    BuildInfoTest.MF_ATTR_NAME_VERSION, "2.2.2", BuildInfoTest.MF_ATTR_NAME_BUILD, "2222" });

            final String pathSep = System.getProperty("path.separator");

            System.setProperty(BuildInfoTest.JAVA_CLASS_PATH,
                    jarAsFile1.getAbsolutePath() + pathSep + jarAsFile2.getAbsolutePath() + pathSep + origClassPath);

            final BuildInfo info = new BuildInfo();

            BraidsAssertFunctions.assertThrowsException(MultipleForgeJarsFoundError.class, new ClumsyRunnable() {
                @Override
                public void run() throws Exception { // NOPMD by Braids on
                                                     // 8/12/11 10:29 AM
                    info.getBuildID();
                }
            });

        } finally {
            Assert.assertTrue(jarAsFile1.delete(), "attempting to delete 1st temporary jar");
            Assert.assertTrue(jarAsFile2.delete(), "attempting to delete 2nd temporary jar");
            System.setProperty(BuildInfoTest.JAVA_CLASS_PATH, origClassPath);
        }
    }

    /**
     * Helper method to create jar files at specific locations with specific
     * name-value pairs in their manifests.
     * 
     * @param jarLocation
     *            where to create the jar file
     * @param nameValuePairs
     *            has the form {"name1", "value1", "name2", "value2", ...}
     */
    private File makeTmpJarWithManifest(final String fileNamePrefix, final String fileNameSuffix,
            final String[] nameValuePairs) throws IOException {
        if ((nameValuePairs.length % 2) != 0) {
            throw new IllegalArgumentException("nameValuePairs must contain an even number of elements.");
        }

        File result = null;
        FileOutputStream fileOut = null;
        ZipOutputStream zipOut = null;
        try {
            result = File.createTempFile(fileNamePrefix, fileNameSuffix);
            fileOut = new FileOutputStream(result);
            zipOut = new ZipOutputStream(fileOut);

            final ZipEntry zipEntry = new ZipEntry("META-INF/MANIFEST.MF");
            zipOut.putNextEntry(zipEntry);

            zipOut.write(this.toASCII("Manifest-Version: 1.3\n"));

            for (int ix = 0; ix < nameValuePairs.length; ix += 2) {
                zipOut.write(this.toASCII(nameValuePairs[ix]));
                zipOut.write(this.toASCII(": "));
                zipOut.write(this.toASCII(nameValuePairs[ix + 1]));
                zipOut.write(this.toASCII("\n"));
            }

            zipOut.write(this.toASCII("\n"));

            zipOut.closeEntry();
        } finally {
            if (zipOut != null) {
                zipOut.close();
            }
            if (fileOut != null) {
                fileOut.close();
            }
        }

        return result;
    }

    private byte[] toASCII(final String str) {
        return str.getBytes(BuildInfo.US_ASCII_CHARSET);
    }
}
