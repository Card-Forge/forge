package forge.download;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.esotericsoftware.minlog.Log;
import com.google.common.io.Files;

import forge.FThreads;
import forge.GuiBase;
import forge.interfaces.IProgressBar;
import forge.util.FileUtil;

public class GuiDownloadZipService extends GuiDownloadService {
    private final String name, desc, sourceUrl, destFolder, deleteFolder;
    private int filesExtracted;

    public GuiDownloadZipService(final String name0, final String desc0, final String sourceUrl0, final String destFolder0, final String deleteFolder0, final IProgressBar progressBar0) {
        name = name0;
        desc = desc0;
        sourceUrl = sourceUrl0;
        destFolder = destFolder0;
        deleteFolder = deleteFolder0;
        progressBar = progressBar0;
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
        final Map<String, String> files = new HashMap<String, String>();
        files.put("_", "_");
        return files; //not needed by zip service, so just return map of size 1
    }

    @Override
    public final void run() {
        downloadAndUnzip();
        if (!cancel) {
            FThreads.invokeInEdtNowOrLater(new Runnable() {
                @Override
                public void run() {
                    progressBar.setDescription(filesExtracted + " " + desc + " extracted");
                    finish();
                }
            });
        }
    }

    public void downloadAndUnzip() {
        filesExtracted = 0;
        String zipFilename = download("temp.zip");
        if (zipFilename == null) { return; }

        //if assets.zip downloaded successfully, unzip into destination folder
        try {
            GuiBase.getInterface().preventSystemSleep(true); //prevent system from going into sleep mode while unzipping

            if (deleteFolder != null) {
                final File deleteDir = new File(deleteFolder);
                if (deleteDir.exists()) {
                    //attempt to delete previous res directory if to be rebuilt
                    progressBar.reset();
                    progressBar.setDescription("Deleting old " + desc + "...");
                    if (deleteFolder.equals(destFolder)) { //move zip file to prevent deleting it
                        final String oldZipFilename = zipFilename;
                        zipFilename = deleteDir.getParentFile().getAbsolutePath() + File.separator + "temp.zip";
                        Files.move(new File(oldZipFilename), new File(zipFilename));
                    }
                    FileUtil.deleteDirectory(deleteDir);
                }
            }

            final ZipFile zipFile = new ZipFile(zipFilename);
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();

            progressBar.reset();
            progressBar.setPercentMode(true);
            progressBar.setDescription("Extracting " + desc);
            progressBar.setMaximum(zipFile.size());

            FileUtil.ensureDirectoryExists(destFolder);

            int count = 0;
            int failedCount = 0;
            while (entries.hasMoreElements()) {
                if (cancel) { break; }

                try {
                    final ZipEntry entry = entries.nextElement();

                    final String path = destFolder + entry.getName();
                    if (entry.isDirectory()) {
                        new File(path).mkdir();
                        progressBar.setValue(++count);
                        continue;
                    }
                    copyInputStream(zipFile.getInputStream(entry), path);
                    progressBar.setValue(++count);
                    filesExtracted++;
                }
                catch (final Exception e) { //don't quit out completely if an entry is not UTF-8
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

    public String download(final String filename) {
        GuiBase.getInterface().preventSystemSleep(true); //prevent system from going into sleep mode while downloading

        progressBar.reset();
        progressBar.setPercentMode(true);
        progressBar.setDescription("Downloading " + desc);

        try {
            final URL url = new URL(sourceUrl);
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection(getProxy());

            if (url.getPath().endsWith(".php")) {
                //ensure file can be downloaded if returned from PHP script
                conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 4.01; Windows NT)");
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

            // input stream to read file - with 8k buffer
            final InputStream input = new BufferedInputStream(conn.getInputStream(), 8192);

            FileUtil.ensureDirectoryExists(destFolder);

            // output stream to write file
            final String destFile = destFolder + filename;
            final OutputStream output = new FileOutputStream(destFile);

            int count;
            long total = 0;
            final byte data[] = new byte[1024];

            while ((count = input.read(data)) != -1) {
                if (cancel) { break; }

                total += count;
                progressBar.setValue((int)(100 * total / contentLength));
                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            input.close();

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

    protected void copyInputStream(final InputStream in, final String outPath) throws IOException{
        final byte[] buffer = new byte[1024];
        int len;
        final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outPath));

        while((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
        }

        in.close();
        out.close();
    }
}
