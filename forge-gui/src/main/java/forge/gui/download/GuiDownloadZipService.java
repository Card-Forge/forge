package forge.gui.download;

import com.esotericsoftware.minlog.Log;
import com.google.common.io.Files;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.interfaces.IProgressBar;
import forge.util.BuildInfo;
import forge.util.FileUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class GuiDownloadZipService extends GuiDownloadService {
    private final String name, desc, sourceUrl, destFolder, deleteFolder;
    private int filesExtracted;
    private boolean allowDeletion;

    public GuiDownloadZipService(final String name0, final String desc0, final String sourceUrl0, final String destFolder0, final String deleteFolder0, final IProgressBar progressBar0) {
        this(name0, desc0, sourceUrl0, destFolder0, deleteFolder0, progressBar0,true);
    }
    public GuiDownloadZipService(final String name0, final String desc0, final String sourceUrl0, final String destFolder0, final String deleteFolder0, final IProgressBar progressBar0, final boolean allowDeletion0) {
        name = name0;
        desc = desc0;
        sourceUrl = sourceUrl0;
        destFolder = destFolder0;
        deleteFolder = deleteFolder0;
        progressBar = progressBar0;
        allowDeletion = allowDeletion0;
    }

    @Override
    public String getTitle() {
        return "Download " + name;
    }

    @Override
    protected String getStartOverrideDesc() {
        return desc;
    }

    @Override
    protected final Map<String, String> getNeededFiles() {
        final Map<String, String> files = new HashMap<>();
        files.put("_", "_");
        return files; //not needed by zip service, so just return map of size 1
    }

    @Override
    public final void run() {
        downloadAndUnzip();
        if (!cancel) {
            FThreads.invokeInEdtNowOrLater(() -> {
                if (progressBar != null)
                    progressBar.setDescription(filesExtracted + " " + desc + " extracted");
                finish();
            });
        }
    }

    public void downloadAndUnzip() {
        filesExtracted = 0;

        String zipFilename = download("temp.zip");
        if (zipFilename == null) { return; }

        extract(zipFilename);
    }

    public String download(final String filename) {
        GuiBase.getInterface().preventSystemSleep(true); //prevent system from going into sleep mode while downloading

        if (progressBar == null)
            return "";
        progressBar.reset();
        progressBar.setPercentMode(true);
        progressBar.setDescription("Downloading " + desc);

        try {
            final URL url = new URL(sourceUrl);
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection(getProxy());

            if (url.getPath().endsWith(".php")) {
                //ensure file can be downloaded if returned from PHP script
                conn.setRequestProperty("User-Agent", BuildInfo.getUserAgent());
            }

            conn.connect();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            final long contentLength = conn.getContentLength();
            if (contentLength == 0) {
                return null;
            }

            progressBar.setMaximum(100);

            FileUtil.ensureDirectoryExists(destFolder);
            final String destFile = destFolder + filename;

            // input stream to read file - with 8k buffer
            // output stream to write file
            try(InputStream input = new BufferedInputStream(conn.getInputStream(), 8192);
                OutputStream output = java.nio.file.Files.newOutputStream(Paths.get(destFile))) {

                int count;
                long total = 0;
                final byte[] data = new byte[1024];

                while ((count = input.read(data)) != -1) {
                    if (cancel) {
                        break;
                    }

                    total += count;
                    if (progressBar != null)
                        progressBar.setValue((int) (100 * total / contentLength));
                    output.write(data, 0, count);
                }

                output.flush();
            }

            if (cancel) {
                new File(destFile).delete();
                return null;
            }

            return destFile;
        }
        catch (final Exception ex) {
            Log.error("Downloading " + desc, "Error downloading " + desc, ex);
            return null;
        }
        finally {
            GuiBase.getInterface().preventSystemSleep(false);
        }
    }

    public void extract(String zipFilename) {
        //if assets.zip downloaded successfully, unzip into destination folder
        try {
            GuiBase.getInterface().preventSystemSleep(true); //prevent system from going into sleep mode while unzipping

            if (deleteFolder != null && allowDeletion) {
                final File deleteDir = new File(deleteFolder);
                if (deleteDir.exists()) {
                    //attempt to delete previous res directory if to be rebuilt
                    if (progressBar != null) {
                        progressBar.reset();
                        progressBar.setDescription("Deleting old " + desc + "...");
                    }
                    if (deleteFolder.equals(destFolder)) { //move zip file to prevent deleting it
                        final String oldZipFilename = zipFilename;
                        zipFilename = deleteDir.getParentFile().getAbsolutePath() + File.separator + "temp.zip";
                        Files.move(new File(oldZipFilename), new File(zipFilename));
                    }
                    FileUtil.deleteDirectory(deleteDir);
                }
            }

            final Charset charset = Charset.forName("IBM437");
            ZipFile zipFile;
            try {
                zipFile = new ZipFile(zipFilename, charset);
            } catch (Throwable e) { //some older Android versions need the old method
                zipFile = new ZipFile(zipFilename);
            }
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();

            if (progressBar != null) {
                progressBar.reset();
                progressBar.setPercentMode(true);
                progressBar.setDescription("Extracting " + desc);
                progressBar.setMaximum(zipFile.size());
            }
            FileUtil.ensureDirectoryExists(destFolder);

            int count = 0;
            int failedCount = 0;
            while (entries.hasMoreElements()) {
                if (cancel) { break; }

                try {
                    final ZipEntry entry = entries.nextElement();

                    final String path = destFolder + File.separator + entry.getName();
                    if (entry.isDirectory()) {
                        new File(path).mkdir();
                        if (progressBar != null)
                            progressBar.setValue(++count);
                        continue;
                    }
                    copyInputStream(zipFile.getInputStream(entry), path);
                    if (progressBar != null)
                        progressBar.setValue(++count);
                    filesExtracted++;
                }
                catch (final Exception e) { //don't quit out completely if an entry is not UTF-8
                    if (progressBar != null)
                        progressBar.setValue(++count);
                    failedCount++;
                }
            }

            if (failedCount > 0) {
                Log.error("Downloading " + desc, failedCount + " " + desc + " could not be extracted");
            }

            zipFile.close();
            new File(zipFilename).delete();
        }
        catch (final Exception e) {
            e.printStackTrace();
        }
        finally {
            GuiBase.getInterface().preventSystemSleep(false);
        }
    }

    protected void copyInputStream(final InputStream in, final String outPath) throws IOException {
        final byte[] buffer = new byte[1024];
        int len;

        try (BufferedOutputStream out = new BufferedOutputStream(java.nio.file.Files.newOutputStream(Paths.get(outPath)))) {
            while ((len = in.read(buffer)) >= 0) {
                out.write(buffer, 0, len);
            }
        }

        in.close();
    }
}
