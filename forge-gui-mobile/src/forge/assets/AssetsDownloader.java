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
import forge.util.ForgeUpdateConfig;
import forge.util.UpdateManifest;
import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Gdx;
import com.google.common.collect.ImmutableList;

import forge.Forge;
import forge.gui.FThreads;
import forge.gui.download.GuiDownloadZipService;
import forge.gui.util.SOptionPane;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.FileUtil;

import static forge.localinstance.properties.ForgeConstants.ADV_TEXTURE_BG_FILE;
import static forge.localinstance.properties.ForgeConstants.ASSETS_DIR;
import static forge.localinstance.properties.ForgeConstants.GITHUB_SNAPSHOT_URL;
import static forge.localinstance.properties.ForgeConstants.DEFAULT_SKINS_DIR;
import static forge.localinstance.properties.ForgeConstants.GITHUB_COMMITS_ATOM;
import static forge.localinstance.properties.ForgeConstants.GITHUB_FORGE_URL;
import static forge.localinstance.properties.ForgeConstants.GITHUB_RELEASES_ATOM;
import static forge.localinstance.properties.ForgeConstants.FONTS_DIR;
import static forge.localinstance.properties.ForgeConstants.LANG_DIR;
import static forge.localinstance.properties.ForgeConstants.RELEASE_URL;
import static forge.localinstance.properties.ForgeConstants.RES_DIR;

public class AssetsDownloader {
    private static ImmutableList<String> getDownloadIgnoreExitOptions() {
        return ImmutableList.of(Forge.getLocalizer().getMessage("lblDownload"), Forge.getLocalizer().getMessage("lblIgnore"), Forge.getLocalizer().getMessage("lblExit"));
    }

    private static ImmutableList<String> getDownloadExitOptions() {
        return ImmutableList.of(Forge.getLocalizer().getMessage("lblDownload"), Forge.getLocalizer().getMessage("lblExit"));
    }

    public static void checkForUpdates(boolean exited, Runnable runnable) {
        if (exited)
            return;
        installBundledCjkFont();
        installBundledLocalizationOverrides();
        if (GuiBase.isAndroid()) {
            Forge.getLocalizer().initialize(Forge.locale, LANG_DIR);
        }
        final String versionString = Forge.getDeviceAdapter().getVersionString();
        Forge.getSplashScreen().getProgressBar().setDescription(Forge.getLocalizer().getMessage("lblCheckingForUpdates"));
        if (versionString.contains("GIT")) {
            if (!GuiBase.isAndroid()) {
                run(runnable);
                return;
            }
        }

        final String packageSize = GuiBase.isAndroid() ? "160MB" : "270MB";
        final String apkSize = "12MB";

        final boolean isSnapshots = versionString.contains("SNAPSHOT");
        boolean connectedToInternet = Forge.getDeviceAdapter().isConnectedToInternet();
        UpdateManifest mirrorManifest = null;
        if (connectedToInternet) {
            if (!ForgeUpdateConfig.isMirrorEnabled()) {
                // Localized builds use the China mirror exclusively. Never fall back
                // to the upstream release or snapshot servers.
                connectedToInternet = false;
            } else {
                try {
                    mirrorManifest = UpdateManifest.load(ForgeUpdateConfig.getManifestUrl());
                } catch (Exception e) {
                    e.printStackTrace();
                    connectedToInternet = false;
                }
            }
        }
        final String snapsURL = GITHUB_SNAPSHOT_URL;
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
        if (connectedToInternet) {
            //currently for desktop/mobile-dev release on github
            final String releaseTag = mirrorManifest == null
                    ? Forge.getDeviceAdapter().getReleaseTag(GITHUB_RELEASES_ATOM)
                    : "forge-" + mirrorManifest.version();
            final UpdateManifest.Artifact mirrorInstaller = mirrorManifest == null ? null
                    : (GuiBase.isAndroid() ? mirrorManifest.android() : mirrorManifest.desktop());
            // A mirror manifest may intentionally publish only assets. In that case Android must
            // continue to the resource update below without trying to construct an empty APK URL.
            // Desktop has no separate resource package, so it can finish startup immediately.
            if (mirrorManifest != null && (mirrorInstaller == null || !mirrorInstaller.isPresent())) {
                if (!GuiBase.isAndroid()) {
                    run(runnable);
                    return;
                }
            } else {
            try {
                String version = mirrorManifest == null
                        ? (isSnapshots ? FileUtil.readFileToString(new URL(versionText)) : releaseTag.replace("forge-", ""))
                        : mirrorManifest.version();
                String filename = "";
                String installerURL = "";
                long installerSize = 0;
                String installerSha256 = "";
                if (GuiBase.isAndroid()) {
                    if (mirrorManifest != null) {
                        final UpdateManifest.Artifact artifact = mirrorManifest.android();
                        installerURL = mirrorManifest.resolveUrl(artifact);
                        filename = new FileHandle(new URL(installerURL).getPath()).name();
                        installerSize = artifact.size();
                        installerSha256 = artifact.sha256();
                    } else {
                        filename = "forge-android-" + version + "-signed-aligned.apk";
                        installerURL = isSnapshots ? snapsURL + filename : releaseURL + version + "/" + filename;
                    }
                } else {
                    if (mirrorManifest != null) {
                        final UpdateManifest.Artifact artifact = mirrorManifest.desktop();
                        installerURL = mirrorManifest.resolveUrl(artifact);
                        filename = new FileHandle(new URL(installerURL).getPath()).name();
                        installerSize = artifact.size();
                        installerSha256 = artifact.sha256();
                    } else {
                        //current release on github is tar.bz2, update this to jar installer in the future...
                        filename = isSnapshots ? "forge-installer-" + version + ".jar" : releaseTag.replace("forge-", "forge-gui-desktop-") + ".tar.bz2";
                        String releaseBZ2URL = GITHUB_FORGE_URL + "releases/download/" + releaseTag + "/" + filename;
                        String snapsBZ2URL = GITHUB_SNAPSHOT_URL + filename;
                        installerURL = isSnapshots ? snapsBZ2URL : releaseBZ2URL;
                    }
                }
                String snapsBuildDate = "", buildDate = "";
                if (mirrorManifest == null && isSnapshots) {
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

                    message = Forge.getLocalizer().getMessage("lblNewVersionForgeAvailableDetailed", version, snapsBuildDate, versionString, buildDate);
                    if (!Forge.getDeviceAdapter().isConnectedToWifi()) {
                        message += " " + Forge.getLocalizer().getMessage("lblConnectWifiForDownload", GuiBase.isAndroid() ? apkSize : packageSize);
                    }
                    if (mirrorManifest == null && isSnapshots) // this is for snaps initial info
                        message += Forge.getDeviceAdapter().getLatestChanges(GITHUB_COMMITS_ATOM, buildTimeStamp, snapsTimestamp);
                    //failed to grab latest github tag
                    if (!isSnapshots && releaseTag.isEmpty()) {
                        if (!GuiBase.isAndroid())
                            run(runnable);
                    } else if (SOptionPane.showConfirmDialog(message, Forge.getLocalizer().getMessage("lblNewVersionAvailable"), Forge.getLocalizer().getMessage("lblUpdateNow"), Forge.getLocalizer().getMessage("lblUpdateLater"), true, true)) {
                        String installer = new GuiDownloadZipService("", "update", installerURL,
                                Forge.getDeviceAdapter().getDownloadsDir(), null, Forge.getSplashScreen().getProgressBar(),
                                true, installerSize, installerSha256).download(filename);
                        if (installer != null) {
                            Forge.getDeviceAdapter().openFile(installer);
                            Forge.isMobileAdventureMode = Forge.advStartup;
                            Forge.exitAnimation(false);
                            return;
                        }
                        switch (SOptionPane.showOptionDialog(Forge.getLocalizer().getMessage("lblCouldNotDownloadUpdate"),
                                Forge.getLocalizer().getMessage("lblUpdateFailed"), null, ImmutableList.of(Forge.getLocalizer().getMessage("lblOK")))) {
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
        }
        final UpdateManifest.Artifact mirrorAssets = mirrorManifest == null ? null : mirrorManifest.assets();
        final String resourceVersion = mirrorAssets != null && mirrorAssets.isPresent()
                ? mirrorAssets.version() : versionString;
        if (versionFile.exists() && resourceVersion.equals(FileUtil.readFileToString(versionFile.file())) && FSkin.getSkinDir() != null) {
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
                if (buildDate.equals(targetDate) && resourceVersion.equals(FileUtil.readFileToString(versionFile.file()))) {
                    run(runnable);
                    return;
                }
                mandatory = true;
                build += "\n" + Forge.getLocalizer().getMessage("lblInstalledResourcesDate", target) + "\n";
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Forge.getSplashScreen().prepareForDialogs(); //ensure colors set up for showing message dialogs

        boolean canIgnoreDownload = resDir.exists() && FSkin.getAllSkins() != null && !FileUtil.readFileToString(versionFile.file()).isEmpty(); //don't allow ignoring download if resource files haven't been previously loaded
        if (mandatory && connectedToInternet)
            canIgnoreDownload = false;

        if (!connectedToInternet) {
            message = Forge.getLocalizer().getMessage("lblUpdatedResourcesUnavailable") + "\n\n";
            if (canIgnoreDownload) {
                message += Forge.getLocalizer().getMessage("lblContinueWithoutResourceUpdate");
            } else {
                message += Forge.getLocalizer().getMessage("lblCannotStartWithoutResources");
            }
            switch (SOptionPane.showOptionDialog(message, Forge.getLocalizer().getMessage("lblNoInternetConnection"), null, ImmutableList.of(Forge.getLocalizer().getMessage("lblOK")))) {
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
        message = Forge.getLocalizer().getMessage("lblUpdatedResourcesDownload", packageSize) + " ";
        if (Forge.getDeviceAdapter().isConnectedToWifi()) {
            message += Forge.getLocalizer().getMessage("lblWifiDownloadShouldBeQuick");
        } else {
            message += Forge.getLocalizer().getMessage("lblWifiDownloadRecommended");
        }
        final List<String> options;
        message += "\n\n";
        if (canIgnoreDownload) {
            message += Forge.getLocalizer().getMessage("lblIgnoreResourceUpdateWarning");
            options = getDownloadIgnoreExitOptions();
        } else {
            message += Forge.getLocalizer().getMessage("lblResourceUpdateMandatory");
            options = getDownloadExitOptions();
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
        final String assetURL;
        final long assetSize;
        final String assetSha256;
        if (mirrorAssets != null && mirrorAssets.isPresent()) {
            try {
                assetURL = mirrorManifest.resolveUrl(mirrorAssets);
            } catch (Exception e) {
                e.printStackTrace();
                Forge.isMobileAdventureMode = Forge.advStartup;
                Forge.exitAnimation(false);
                return;
            }
            assetSize = mirrorAssets.size();
            assetSha256 = mirrorAssets.sha256();
        } else {
            assetURL = isSnapshots ? snapsURL + "assets.zip" : releaseURL + versionString + "/" + "assets.zip";
            assetSize = 0;
            assetSha256 = "";
        }
        new GuiDownloadZipService("", Forge.getLocalizer().getMessage("lblResourceFiles"), assetURL,
                ASSETS_DIR, RES_DIR, Forge.getSplashScreen().getProgressBar(), allowDeletion,
                assetSize, assetSha256).downloadAndUnzip();

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
                FileUtil.writeFile(versionFile.file(), resourceVersion);
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
            installBundledCjkFont();
            installBundledLocalizationOverrides();
            if (!GuiBase.isAndroid()) {
                Forge.getSplashScreen().getProgressBar().setDescription(Forge.getLocalizer().getMessage("lblLoadingGameResources"));
            }
            FThreads.invokeInBackgroundThread(toRun);
            return;
        }
        if (!GuiBase.isAndroid()) {
            Forge.isMobileAdventureMode = Forge.advStartup;
            Forge.exitAnimation(false);
        }
    }

    private static void installBundledLocalizationOverrides() {
        if (!GuiBase.isAndroid()) {
            return;
        }
        FileHandle destination = Gdx.files.absolute(LANG_DIR);
        destination.mkdirs();
        for (String fileName : ImmutableList.of("en-US.properties", "zh-CN.properties", "cardnames-zh-CN.txt")) {
            FileHandle bundledFile = Gdx.files.internal("localization/" + fileName);
            if (bundledFile.exists()) {
                bundledFile.copyTo(destination.child(fileName));
            }
        }
    }

    private static void installBundledCjkFont() {
        if (!GuiBase.isAndroid()) {
            return;
        }
        final String fontName = "SourceHanSansCN";
        FileHandle bundledFont = Gdx.files.internal("bundled-font/" + fontName + ".ttf");
        if (!bundledFont.exists()) {
            return;
        }
        FileHandle fontDirectory = Gdx.files.absolute(FONTS_DIR);
        fontDirectory.mkdirs();
        FileHandle installedFont = fontDirectory.child(fontName + ".ttf");
        if (!installedFont.exists() || installedFont.length() != bundledFont.length()) {
            bundledFont.copyTo(installedFont);
        }
        FileHandle bundledLicense = Gdx.files.internal("bundled-font/OFL.txt");
        if (bundledLicense.exists()) {
            bundledLicense.copyTo(fontDirectory.child("SourceHanSansCN-OFL.txt"));
        }
        if (FModel.getPreferences().getPref(FPref.UI_CJK_FONT).isEmpty()) {
            FModel.getPreferences().setPref(FPref.UI_CJK_FONT, fontName);
            FModel.getPreferences().save();
            Forge.CJK_Font = fontName;
        }
    }
}
