package forge.assets;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.google.common.collect.ImmutableList;
import forge.FThreads;
import forge.Forge;
import forge.download.GuiDownloadZipService;
import forge.properties.ForgeConstants;
import forge.screens.SplashScreen;
import forge.util.FileUtil;
import forge.util.gui.SOptionPane;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class AssetsDownloader {
    public static final boolean SHARE_DESKTOP_ASSETS = true; //change to false to test downloading separate assets for desktop version

    private final static ImmutableList<String> downloadIgnoreExit = ImmutableList.of("Download", "Ignore", "Exit");
    private final static ImmutableList<String> downloadExit = ImmutableList.of("Download", "Exit");

    //if not sharing desktop assets, check whether assets are up to date
    public static void checkForUpdates(final SplashScreen splashScreen) {
        if (Gdx.app.getType() == ApplicationType.Desktop && SHARE_DESKTOP_ASSETS) { return; }

        splashScreen.getProgressBar().setDescription("Checking for updates...");

        String message;
        boolean connectedToInternet = Forge.getDeviceAdapter().isConnectedToInternet();
        if (connectedToInternet) {
            try {
                URL versionUrl = new URL("https://releases.cardforge.org/forge/forge-gui-android/version.txt");
                String version = FileUtil.readFileToString(versionUrl);
                if (!StringUtils.isEmpty(version) && !Forge.CURRENT_VERSION.equals(version)) {
                    splashScreen.prepareForDialogs();

                    message = "A new version of Forge is available (" + version + ").\n" +
                            "You are currently on an older version (" + Forge.CURRENT_VERSION + ").\n\n" +
                            "Would you like to update to the new version now?";
                    if (!Forge.getDeviceAdapter().isConnectedToWifi()) {
                        message += " If so, you may want to connect to wifi first. The download is around 6.5MB.";
                    }
                    if (SOptionPane.showConfirmDialog(message, "New Version Available", "Update Now", "Update Later", true, true)) {
                        String filename = "forge-android-" + version + "-signed-aligned.apk";
                        String apkFile = new GuiDownloadZipService("", "update",
                                "https://releases.cardforge.org/forge/forge-gui-android/" + version + "/" + filename,
                                Forge.getDeviceAdapter().getDownloadsDir(), null, splashScreen.getProgressBar()).download(filename);
                        if (apkFile != null) {
                            if (Forge.androidVersion < 29) { //Android 9 and below...
                                Forge.getDeviceAdapter().openFile(apkFile);
                                Forge.exit(true);
                                return;
                            }
                            //Android 10 and newer manual apk installation
                            switch (SOptionPane.showOptionDialog("Download Successful. Go to your downloads folder and install " + filename +" to update Forge. Forge will now exit.", "", null, ImmutableList.of("Ok"))) {
                                default:
                                    Forge.exit(true);
                            }
                            return;
                        }
                        SOptionPane.showMessageDialog("Could not download update. " +
                                "Press OK to proceed without update.", "Update Failed");
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        //see if assets need updating
        File versionFile = new File(ForgeConstants.ASSETS_DIR + "version.txt");
        if (!versionFile.exists()) {
            try {
                versionFile.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
                Forge.exit(true); //can't continue if this fails
                return;
            }
        }
        else if (Forge.CURRENT_VERSION.equals(FileUtil.readFileToString(versionFile)) && FSkin.getSkinDir() != null) {
            return; //if version matches what had been previously saved and FSkin isn't requesting assets download, no need to download assets
        }

        splashScreen.prepareForDialogs(); //ensure colors set up for showing message dialogs

        boolean canIgnoreDownload = FSkin.getAllSkins() != null; //don't allow ignoring download if resource files haven't been previously loaded

        if (!connectedToInternet) {
            message = "Updated resource files cannot be downloaded due to lack of internet connection.\n\n";
            if (canIgnoreDownload) {
                message += "You can continue without this download, but you may miss out on card fixes or experience other problems.";
            }
            else {
                message += "You cannot start the app since you haven't previously downloaded these files.";
            }
            SOptionPane.showMessageDialog(message, "No Internet Connection");
            if (!canIgnoreDownload) {
                Forge.exit(true); //exit if can't ignore download
            }
            return;
        }

        //prompt user whether they wish to download the updated resource files
        message = "There are updated resource files to download. " +
                "This download is around 100MB, ";
        if (Forge.getDeviceAdapter().isConnectedToWifi()) {
            message += "which shouldn't take long if your wifi connection is good.";
        }
        else {
            message += "so it's highly recommended that you connect to wifi first.";
        }
        final List<String> options;
        message += "\n\n";
        if (canIgnoreDownload) {
            message += "If you choose to ignore this download, you may miss out on card fixes or experience other problems.";
            options = downloadIgnoreExit;
        } else {
            message += "This download is mandatory to start the app since you haven't previously downloaded these files.";
            options = downloadExit;
        }

        switch (SOptionPane.showOptionDialog(message, "", null, options)) {
        case 1:
            if (!canIgnoreDownload) {
                Forge.exit(true); //exit if can't ignore download
            }
            return;
        case 2:
            Forge.exit(true);
            return;
        }

        new GuiDownloadZipService("", "resource files",
                "https://releases.cardforge.org/forge/forge-gui-android/" + Forge.CURRENT_VERSION + "/" + "assets.zip",
                ForgeConstants.ASSETS_DIR, ForgeConstants.RES_DIR, splashScreen.getProgressBar()).downloadAndUnzip();

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
    }
}
