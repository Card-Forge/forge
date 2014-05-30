package forge.assets;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.esotericsoftware.minlog.Log;

import forge.FThreads;
import forge.Forge;
import forge.properties.ForgeConstants;
import forge.screens.SplashScreen;
import forge.toolbox.FProgressBar;
import forge.util.FileUtil;
import forge.util.TextUtil;

public class AssetsDownloader {
    //if not forge-gui-mobile-dev, check whether assets are up to date
    public static void checkForUpdates(final SplashScreen splashScreen) {
        if (Gdx.app.getType() == ApplicationType.Desktop) { return; }

        //TODO see if app needs updating
        //progressBar.setDescription("Checking for updates");

        //set if assets need updating
        File versionFile = new File(ForgeConstants.ASSETS_DIR + "version.txt");
        if (!versionFile.exists()) {
            try {
                versionFile.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
                Gdx.app.exit(); //can't continue if this fails
            }
        }
        else if (Forge.CURRENT_VERSION.equals(TextUtil.join(FileUtil.readFile(versionFile), "\n")) && !FSkin.assetsDownloadNeeded()) {
            return; //if version matches what had been previously saved and FSkin isn't requesting assets download, no need to download assets
        }

        downloadAssets(splashScreen.getProgressBar());

        //reload light version of skin of assets updated
        FThreads.invokeInEdtAndWait(new Runnable() {
            @Override
            public void run() {
                FSkin.reloadAfterAssetsDownload(splashScreen);
            }
        });

        //save version string to file once assets finish downloading
        //so they don't need to be re-downloaded until you upgrade again
        FileUtil.writeFile(versionFile, Forge.CURRENT_VERSION);
    }

    private static void downloadAssets(final FProgressBar progressBar) {
        FThreads.invokeInEdtLater(new Runnable() {
            @Override
            public void run() {
                progressBar.setShowProgressTrail(true);
                progressBar.setDescription("Downloading resource files...");
            }
        });

        String url = "http://cardforge.org/android/releases/forge/forge-gui-android/" + Forge.CURRENT_VERSION + "/assets.zip";
        final File destDir = new File(ForgeConstants.ASSETS_DIR);
        final File fileDest = new File(destDir.getAbsolutePath() + "/assets.zip");
        final File resDir = new File(ForgeConstants.RES_DIR);
        try {
            if (resDir.exists()) {
                resDir.delete(); //attempt to delete previous res directory if to be rebuilt
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        ReadableByteChannel rbc = null;
        FileOutputStream    fos = null;
        try {
            // test for folder existence
            if (!destDir.exists() && !destDir.mkdir()) { // create folder if not found
                System.out.println("Can't create folder" + destDir.getAbsolutePath());
            }

            URL imageUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection(Proxy.NO_PROXY);
            // don't allow redirections here -- they indicate 'file not found' on the server
            conn.setInstanceFollowRedirects(false);
            conn.connect();

            if (conn.getResponseCode() != 200) {
                conn.disconnect();
                System.out.println("Could not download assets.zip");
                return;
            }

            rbc = Channels.newChannel(conn.getInputStream());
            fos = new FileOutputStream(fileDest);
            fos.getChannel().transferFrom(rbc, 0, 1 << 27);
        }
        catch (final Exception ex) {
            Log.error("Assets", "Error downloading assets", ex);
        }
        finally {
            if (rbc != null) {
                try {
                    rbc.close();
                }
                catch (IOException e) {
                    System.out.println("error closing input stream");
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                }
                catch (IOException e) {
                    System.out.println("error closing output stream");
                }
            }
        }

        //if assets.zip downloaded successfully, unzip into destination folder
        FThreads.invokeInEdtLater(new Runnable() {
            @Override
            public void run() {
                progressBar.setDescription("Unzipping resource files...");
            }
        });

        try {
            ZipFile zipFile = new ZipFile(fileDest);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry)entries.nextElement();

                String path = ForgeConstants.ASSETS_DIR + entry.getName();
                if (entry.isDirectory()) {
                    new File(path).mkdir();
                    continue;
                }
                copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(path)));
            }

            zipFile.close();
            fileDest.delete();
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static final void copyInputStream(InputStream in, OutputStream out) throws IOException{
        byte[] buffer = new byte[1024];
        int len;

        while((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
        }

        in.close();
        out.close();
    }
}
