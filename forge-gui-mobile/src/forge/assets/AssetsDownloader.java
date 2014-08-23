package forge.assets;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
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
import forge.util.gui.SOptionPane;

public class AssetsDownloader {
    //if not forge-gui-mobile-dev, check whether assets are up to date
    public static boolean checkForUpdates(final SplashScreen splashScreen) {
        if (Gdx.app.getType() == ApplicationType.Desktop) { return true; }

        boolean connectedToInternet = Forge.getNetworkConnection().isConnected();
        if (connectedToInternet) {
            /*splashScreen.getProgressBar().setDescription("Checking for updates...");
            try {
                URL versionUrl = new URL("http://cardforge.org/android/releases/forge/forge-gui-android/version.txt");
                String version = FileUtil.readFileToString(versionUrl);
                if (!StringUtils.isEmpty(version) && !Forge.CURRENT_VERSION.equals(version)) {
                    splashScreen.prepareForDialogs();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }*/
        }

        //see if assets need updating
        File versionFile = new File(ForgeConstants.ASSETS_DIR + "version.txt");
        if (!versionFile.exists()) {
            try {
                versionFile.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
                return false; //can't continue if this fails
            }
        }
        else if (Forge.CURRENT_VERSION.equals(FileUtil.readFileToString(versionFile)) && FSkin.getSkinDir() != null) {
            return true; //if version matches what had been previously saved and FSkin isn't requesting assets download, no need to download assets
        }

        splashScreen.prepareForDialogs(); //ensure colors set up for showing message dialogs

        if (!connectedToInternet) {
            SOptionPane.showMessageDialog("Updated assets files could not be downloaded due to lack of internet connection.",
                    "No Internet Connection");
            return true;
        }

        //prompt user whether they wish to download the updated resource files
        String message = "There are updated resource files to download. " + 
                "This download is around 80MB, ";
        if (Forge.getNetworkConnection().isConnectedToWifi()) {
            message += "which shouldn't take long if your wifi connection is good.";
        }
        else {
            message += "so it's highly recommended that you connect to wifi first.";
        }
        String[] options;
        message += "\n\n";
        if (FSkin.getAllSkins() == null) {
            message += "This download is mandatory to start the app since you haven't previously downloaded these files.";
            options = new String[] { "Download", "Exit" };
        }
        else {
            message += "If you choose to ignore this download, you may miss out on card fixes or experience other problems.";
            options = new String[] { "Download", "Ignore", "Exit" };
        }
        switch (SOptionPane.showOptionDialog(message, "Download Resource Files?",
                null, options)) {
        case 1:
            return options.length == 3; //return true or false based on whether second option is Ignore vs. Exit
        case 2:
            return false; //return false to indicate to exit application
        }

        downloadAssets(splashScreen.getProgressBar());

        FSkinFont.deleteCachedFiles(); //delete cached font files in case any skin's .ttf file changed

        //reload light version of skin after assets updated
        FThreads.invokeInEdtAndWait(new Runnable() {
            @Override
            public void run() {
                FSkinFont.updateAll(); //update all fonts used by splash screen
                FSkin.loadLight(FSkin.getName(), splashScreen);
            }
        });

        //save version string to file once assets finish downloading
        //so they don't need to be re-downloaded until you upgrade again
        FileUtil.writeFile(versionFile, Forge.CURRENT_VERSION);
        return true;
    }

    private static void downloadAssets(final FProgressBar progressBar) {
        final String destFile = ForgeConstants.ASSETS_DIR + "assets.zip";

        progressBar.reset();
        progressBar.setPercentMode(true);
        progressBar.setDescription("Downloading resource files");

        try {
            URL url = new URL("http://cardforge.org/android/releases/forge/forge-gui-android/" + Forge.CURRENT_VERSION + "/assets.zip");
            URLConnection conn = url.openConnection();
            conn.connect();

            long contentLength = conn.getContentLength();
            progressBar.setMaximum(100);

            // input stream to read file - with 8k buffer
            InputStream input = new BufferedInputStream(url.openStream(), 8192);
 
            // output stream to write file
            OutputStream output = new FileOutputStream(destFile);
 
            int count;
            long total = 0;
            byte data[] = new byte[1024];
 
            while ((count = input.read(data)) != -1) {
                total += count;
                progressBar.setValue((int)(100 * total / contentLength));
                output.write(data, 0, count);
            }
 
            output.flush();
            output.close();
            input.close();
        }
        catch (final Exception ex) {
            Log.error("Assets", "Error downloading assets", ex);
        }

        //if assets.zip downloaded successfully, unzip into destination folder
        try {
            File resDir = new File(ForgeConstants.RES_DIR);
            if (resDir.exists()) {
                //attempt to delete previous res directory if to be rebuilt
                progressBar.reset();
                progressBar.setDescription("Deleting old resource files...");
                FileUtil.deleteDirectory(resDir);
            }

            ZipFile zipFile = new ZipFile(destFile);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            progressBar.reset();
            progressBar.setPercentMode(true);
            progressBar.setDescription("Unzipping resource files");
            progressBar.setMaximum(zipFile.size());

            int count = 0;
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry)entries.nextElement();

                String path = ForgeConstants.ASSETS_DIR + entry.getName();
                if (entry.isDirectory()) {
                    new File(path).mkdir();
                    progressBar.setValue(++count);
                    continue;
                }
                copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(path)));
                progressBar.setValue(++count);
            }

            zipFile.close();
            new File(destFile).delete();
        }
        catch (Exception e) {
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
