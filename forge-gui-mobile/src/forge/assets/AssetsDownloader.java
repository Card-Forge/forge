package forge.assets;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.badlogic.gdx.files.FileHandle;
import forge.gui.GuiBase;
import forge.util.BuildInfo;
import forge.util.DateUtil;
import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Gdx;
import com.google.common.collect.ImmutableList;

import forge.Forge;
import forge.gui.FThreads;
import forge.gui.download.GuiDownloadZipService;
import forge.gui.util.SOptionPane;
import forge.util.FileUtil;

import static forge.localinstance.properties.ForgeConstants.ADV_TEXTURE_BG_FILE;
import static forge.localinstance.properties.ForgeConstants.ASSETS_DIR;
import static forge.localinstance.properties.ForgeConstants.DAILY_SNAPSHOT_URL;
import static forge.localinstance.properties.ForgeConstants.DEFAULT_SKINS_DIR;
import static forge.localinstance.properties.ForgeConstants.GITHUB_COMMITS_ATOM;
import static forge.localinstance.properties.ForgeConstants.GITHUB_FORGE_URL;
import static forge.localinstance.properties.ForgeConstants.GITHUB_RELEASES_ATOM;
import static forge.localinstance.properties.ForgeConstants.RELEASE_URL;
import static forge.localinstance.properties.ForgeConstants.RES_DIR;

public class AssetsDownloader {
    private final static ImmutableList<String> downloadIgnoreExit = ImmutableList.of("Download", "Ignore", "Exit");
    private final static ImmutableList<String> downloadExit = ImmutableList.of("Download", "Exit");

    public static void checkForUpdates(boolean exited, Runnable runnable) {
        if (exited)
            return;
        final String versionString = Forge.getDeviceAdapter().getVersionString();
        Forge.getSplashScreen().getProgressBar().setDescription("Checking for updates...");
        if (versionString.contains("GIT")) {
            if (!GuiBase.isAndroid()) {
                run(runnable);
                return;
            }
        }

        final String packageSize = GuiBase.isAndroid() ? "160MB" : "270MB";
        final String apkSize = "12MB";

        final boolean isSnapshots = versionString.contains("SNAPSHOT");
        final String snapsURL = DAILY_SNAPSHOT_URL;
        // desktop and mobile-dev share the same package
        final String guiChannel = GuiBase.isAndroid() ? "forge/forge-gui-android/" : "forge/forge-gui-desktop/";
        final String releaseURL = RELEASE_URL +  guiChannel;
        // desktop and mobile-dev uses maven-metadata.xml on earlier releases
        final String versionText = isSnapshots ? snapsURL + "version.txt" : releaseURL + "maven-metadata.xml";
        FileHandle assetsDir = Gdx.files.absolute(ASSETS_DIR);
        FileHandle resDir = Gdx.files.absolute(RES_DIR);
        FileHandle buildTxtFileHandle = GuiBase.isAndroid() ? Gdx.files.internal("build.txt") : Gdx.files.classpath("build.txt");
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        boolean verifyUpdatable = false;
        boolean mandatory = false;
        Date snapsTimestamp = null, buildTimeStamp = null;

        String message;
        boolean connectedToInternet = Forge.getDeviceAdapter().isConnectedToInternet();
        if (connectedToInternet) {
            //currently for desktop/mobile-dev release on github
            final String releaseTag = Forge.getDeviceAdapter().getReleaseTag(GITHUB_RELEASES_ATOM);
            try {
                URL versionUrl = new URL(versionText);
                String version = isSnapshots ? FileUtil.readFileToString(versionUrl) : releaseTag.replace("forge-", "");
                String filename = "";
                String installerURL = "";
                if (GuiBase.isAndroid()) {
                    filename = "forge-android-" + version + "-signed-aligned.apk";
                    installerURL = isSnapshots ? snapsURL + filename : releaseURL + version + "/" + filename;
                } else {
                    //current release on github is tar.bz2, update this to jar installer in the future...
                    filename = isSnapshots ? "forge-installer-" + version + ".jar" : releaseTag.replace("forge-", "forge-gui-desktop-") + ".tar.bz2";
                    String releaseBZ2URL = GITHUB_FORGE_URL + "releases/download/" + releaseTag + "/" + filename;
                    String snapsBZ2URL = DAILY_SNAPSHOT_URL + filename;
                    installerURL = isSnapshots ? snapsBZ2URL : releaseBZ2URL;
                }
                String snapsBuildDate = "", buildDate = "";
                if (isSnapshots) {
                    URL url = new URL(snapsURL + "build.txt");
                    snapsTimestamp = format.parse(FileUtil.readFileToString(url));
                    snapsBuildDate = snapsTimestamp.toString();
                    if (!GuiBase.isAndroid()) {
                        buildDate = BuildInfo.getTimestamp().toString();
                        verifyUpdatable = BuildInfo.verifyTimestamp(snapsTimestamp);
                    } else {
                        if (buildTxtFileHandle.exists()) {
                            buildTimeStamp = format.parse(buildTxtFileHandle.readString());
                            buildDate = buildTimeStamp.toString();
                            // if morethan 23 hours the difference, then allow to update..
                            verifyUpdatable = DateUtil.getElapsedHours(buildTimeStamp, snapsTimestamp) > 23;
                        } else {
                            //fallback to old version comparison
                            verifyUpdatable = !StringUtils.isEmpty(version) && !versionString.equals(version);
                        }
                    }
                } else {
                    verifyUpdatable = !StringUtils.isEmpty(version) && !versionString.equals(version);
                }

                if (verifyUpdatable) {
                    Forge.getSplashScreen().prepareForDialogs();

                    message = "A new version of Forge is available.\n(v." + version + " | " + snapsBuildDate + ")\n" +
                            "You are currently on an older version.\n(v." + versionString + " | " + buildDate + ")\n" +
                            "Would you like to update to the new version now?";
                    if (!Forge.getDeviceAdapter().isConnectedToWifi()) {
                        message += " If so, you may want to connect to wifi first. The download is around " + (GuiBase.isAndroid() ? apkSize : packageSize) + ".";
                    }
                    if (isSnapshots) // this is for snaps initial info
                        message += Forge.getDeviceAdapter().getLatestChanges(GITHUB_COMMITS_ATOM, buildTimeStamp, snapsTimestamp);
                    //failed to grab latest github tag
                    if (!isSnapshots && releaseTag.isEmpty()) {
                        if (!GuiBase.isAndroid())
                            run(runnable);
                    } else if (SOptionPane.showConfirmDialog(message, "New Version Available", "Update Now", "Update Later", true, true)) {
                        String installer = new GuiDownloadZipService("", "update", installerURL,
                                Forge.getDeviceAdapter().getDownloadsDir(), null, Forge.getSplashScreen().getProgressBar()).download(filename);
                        if (installer != null) {
                            Forge.getDeviceAdapter().openFile(installer);
                            Forge.isMobileAdventureMode = Forge.advStartup;
                            Forge.exitAnimation(false);
                            return;
                        }
                        switch (SOptionPane.showOptionDialog("Could not download update. " +
                                "Press OK to proceed without update.", "Update Failed", null, ImmutableList.of("Ok"))) {
                            default:
                                if (!GuiBase.isAndroid()) {
                                    run(runnable);
                                    return;
                                }
                                break;
                        }
                    }
                } else {
                    if (!GuiBase.isAndroid()) {
                        run(runnable);
                        return;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (!GuiBase.isAndroid()) {
                    run(runnable);
                    return;
                }
            }
        } else {
            if (!GuiBase.isAndroid()) {
                run(runnable);
                return;
            }
        }
        // non android don't have seperate package to check
        if (!GuiBase.isAndroid()) {
            run(runnable);
            return;
        }
        // Android assets fallback
        String build = "";

        //see if assets need updating
        FileHandle advBG = Gdx.files.absolute(DEFAULT_SKINS_DIR).child(ADV_TEXTURE_BG_FILE);
        if (!advBG.exists()) {
            FileHandle deleteVersion = assetsDir.child("version.txt");
            if (deleteVersion.exists())
                deleteVersion.delete();
            FileHandle deleteBuild = resDir.child("build.txt");
            if (deleteBuild.exists())
                deleteBuild.delete();
        }

        FileHandle versionFile = assetsDir.child("version.txt");
        if (!versionFile.exists()) {
            try {
                versionFile.file().createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                Forge.isMobileAdventureMode = Forge.advStartup;
                Forge.exitAnimation(false); //can't continue if this fails
                return;
            }
        } else if (versionString.equals(FileUtil.readFileToString(versionFile.file())) && FSkin.getSkinDir() != null) {
            run(runnable);
            return; //if version matches what had been previously saved and FSkin isn't requesting assets download, no need to download assets
        }

        FileHandle resBuildDate = resDir.child("build.txt");
        if (buildTxtFileHandle.exists() && resBuildDate.exists()) {
            String buildString = buildTxtFileHandle.readString();
            String target = resBuildDate.readString();
            try {
                Date buildDate = format.parse(buildString);
                Date targetDate = format.parse(target);
                // if res folder has same build date then continue loading assets
                if (buildDate.equals(targetDate) && versionString.equals(FileUtil.readFileToString(versionFile.file()))) {
                    run(runnable);
                    return;
                }
                mandatory = true;
                build += "\nInstalled resources date: " + target + "\n";
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Forge.getSplashScreen().prepareForDialogs(); //ensure colors set up for showing message dialogs

        boolean canIgnoreDownload = resDir.exists() && FSkin.getAllSkins() != null && !FileUtil.readFileToString(versionFile.file()).isEmpty(); //don't allow ignoring download if resource files haven't been previously loaded
        if (mandatory && connectedToInternet)
            canIgnoreDownload = false;

        if (!connectedToInternet) {
            message = "Updated resource files cannot be downloaded due to lack of internet connection.\n\n";
            if (canIgnoreDownload) {
                message += "You can continue without this download, but you may miss out on card fixes or experience other problems.";
            } else {
                message += "You cannot start the app since you haven't previously downloaded these files.";
            }
            switch (SOptionPane.showOptionDialog(message, "No Internet Connection", null, ImmutableList.of("Ok"))) {
                default: {
                    if (!canIgnoreDownload) {
                        Forge.isMobileAdventureMode = Forge.advStartup;
                        Forge.exitAnimation(false); //exit if can't ignore download
                    }
                }
            }
            return;
        }

        //prompt user whether they wish to download the updated resource files
        message = "There are updated resource files to download. " +
                "This download is around " + packageSize + ", ";
        if (Forge.getDeviceAdapter().isConnectedToWifi()) {
            message += "which shouldn't take long if your wifi connection is good.";
        } else {
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

        switch (SOptionPane.showOptionDialog(message + build, "", null, options)) {
            case 1:
                if (!canIgnoreDownload) {
                    Forge.isMobileAdventureMode = Forge.advStartup;
                    Forge.exitAnimation(false); //exit if can't ignore download
                    return;
                } else {
                    run(runnable);
                    return;
                }
            case 2:
                Forge.isMobileAdventureMode = Forge.advStartup;
                Forge.exitAnimation(false);
                return;
        }

        //allow deletion on Android 10 or if using app-specific directory
        boolean allowDeletion = Forge.androidVersion < 30 || GuiBase.isUsingAppDirectory();
        String assetURL = isSnapshots ? snapsURL + "assets.zip" : releaseURL + versionString + "/" + "assets.zip";
        new GuiDownloadZipService("", "resource files", assetURL,
                ASSETS_DIR, RES_DIR, Forge.getSplashScreen().getProgressBar(), allowDeletion).downloadAndUnzip();

        if (allowDeletion)
            FSkinFont.deleteCachedFiles(); //delete cached font files in case any skin's .ttf file changed

        //reload light version of skin after assets updated
        FThreads.invokeInEdtAndWait(() -> {
            FSkinFont.updateAll(); //update all fonts used by splash screen
            FSkin.loadLight(FSkin.getName(), Forge.getSplashScreen());
        });

        //save version string to file once assets finish downloading
        //so they don't need to be re-downloaded until you upgrade again
        if (connectedToInternet) {
            if (versionFile.exists())
                FileUtil.writeFile(versionFile.file(), versionString);
        }
        //final check if temp.zip exists then extraction is not complete...
        FileHandle check = assetsDir.child("temp.zip");
        if (check.exists()) {
            if (versionFile.exists())
                versionFile.delete();
            check.delete();
        }
        // auto restart after update
        Forge.isMobileAdventureMode = Forge.advStartup;
        Forge.exitAnimation(true);
    }

    private static void run(Runnable toRun) {
        if (toRun != null) {
            if (!GuiBase.isAndroid()) {
                Forge.getSplashScreen().getProgressBar().setDescription("Loading game resources...");
            }
            FThreads.invokeInBackgroundThread(toRun);
            return;
        }
        if (!GuiBase.isAndroid()) {
            Forge.isMobileAdventureMode = Forge.advStartup;
            Forge.exitAnimation(false);
        }
    }
}
