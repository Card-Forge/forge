package forge.download;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;

import forge.gui.GuiBase;
import forge.gui.download.GuiDownloadZipService;
import forge.gui.util.SOptionPane;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.BuildInfo;
import forge.util.FileUtil;
import forge.util.Localizer;
import forge.util.TextUtil;
import forge.util.WaitCallback;

import static forge.localinstance.properties.ForgeConstants.DAILY_SNAPSHOT_URL;
import static forge.localinstance.properties.ForgeConstants.RELEASE_URL;

public class AutoUpdater {
    private static final boolean VERSION_FROM_METADATA = true;
    private static final Localizer localizer = Localizer.getInstance();

    public static String[] updateChannels = new String[]{ "none", "snapshot", "release"};

    private final boolean isLoading;
    private String updateChannel;
    private String version;
    private final String buildVersion;
    private String versionUrlString;
    private String packageUrl;
    private String packagePath;
    private String buildDate = "";
    private String snapsBuildDate = "";

    public AutoUpdater(boolean loading) {
        // What do I need? Preferences? Splashscreen? UI? Skins?
        isLoading = loading;
        updateChannel = FModel.getPreferences().getPref(ForgePreferences.FPref.AUTO_UPDATE);
        buildVersion = BuildInfo.getVersionString();
    }

    public boolean updateAvailable() {
        // TODO Check if an update is available, and add a UI element to notify the user.
        return verifyUpdateable();
    }

    public boolean attemptToUpdate(CompletableFuture<String> cf) {
        if (!verifyUpdateable()) {
            return false;
        }
        try {
            if (downloadUpdate(cf)) {
                extractAndRestart();
            }
        } catch(IOException | URISyntaxException | ExecutionException | InterruptedException e) {
            return false;
        }
        return true;
    }

    private void extractAndRestart() {
        extractUpdate();
        restartForge();
    }

    private boolean verifyUpdateable() {
        if (buildVersion.contains("GIT")) {
            //return false;
        }

        if (isLoading) {
            // TODO This doesn't work yet, because FSkin isn't loaded at the time.
            return false;
        } else if (updateChannel.equals("none")) {
            String message = localizer.getMessage("lblYouHaventSetUpdateChannel");
            List<String> options = ImmutableList.of(localizer.getMessageorUseDefault("lblCancel", "Cancel"), localizer.getMessageorUseDefault("lblRelease", "Release"), localizer.getMessageorUseDefault("lblSnapshot", "Snapshot"));
            int option = SOptionPane.showOptionDialog(message, localizer.getMessage("lblManualCheck"), null, options, 0);
            if (option < 1) {
                return false;
            }
            updateChannel = options.get(option);
        }

        if (buildVersion.contains("SNAPSHOT")) {
            if (!updateChannel.equalsIgnoreCase(localizer.getMessageorUseDefault("lblSnapshot", "Snapshot"))) {
                System.out.println("Snapshot build versions must use snapshot update channel to work");
                return false;
            }

            versionUrlString = DAILY_SNAPSHOT_URL + "version.txt";
        } else {
            if (!updateChannel.equalsIgnoreCase(localizer.getMessageorUseDefault("lblRelease", "Release"))) {
                System.out.println("Release build versions must use release update channel to work");
                return false;
            }
            versionUrlString = RELEASE_URL + "forge/forge-gui-desktop/version.txt";
        }

        // Check the internet connection
        if (!testNetConnection()) {
            return false;
        }

        // Download appropriate version file
        return compareBuildWithLatestChannelVersion();
    }

    private boolean testNetConnection() {
        try (Socket socket = new Socket()) {
            InetSocketAddress address = new InetSocketAddress("releases.cardforge.org", 443);
            socket.connect(address, 1000);
            return true;
        } catch (IOException e) {
            return false; // Either timeout or unreachable or failed DNS lookup.
        }
    }

    private boolean compareBuildWithLatestChannelVersion() {
        try {
            retrieveVersion();
            if (buildVersion.contains("SNAPSHOT")) {
                URL url = new URL(DAILY_SNAPSHOT_URL + "build.txt");
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date snapsTimestamp = simpleDateFormat.parse(FileUtil.readFileToString(url));
                snapsBuildDate = snapsTimestamp.toString();
                buildDate = BuildInfo.getTimestamp().toString();
                return BuildInfo.verifyTimestamp(snapsTimestamp);
            }
            if (StringUtils.isEmpty(version) ) {
                return false;
            }

            if (buildVersion.equals(version)) {
                return false;
            }
        }
        catch (Exception e) {
            SOptionPane.showOptionDialog(e.getMessage(), localizer.getMessage("lblError"), null, ImmutableList.of("Ok"));
            return false;
        }
        // If version doesn't match, it's assummably newer.
        return true;
    }

    private void retrieveVersion() throws MalformedURLException {
        if (VERSION_FROM_METADATA && updateChannel.equalsIgnoreCase(localizer.getMessageorUseDefault("lblRelease", "Release"))) {
            extractVersionFromMavenRelease();
        } else {
            URL versionUrl = new URL(versionUrlString);
            version = FileUtil.readFileToString(versionUrl);
        }
        if (updateChannel.equalsIgnoreCase(localizer.getMessageorUseDefault("lblRelease", "Release"))) {
            packageUrl = RELEASE_URL + "forge/forge-gui-desktop/" + version + "/forge-gui-desktop-" + version + ".tar.bz2";
        } else {
            packageUrl = DAILY_SNAPSHOT_URL + "forge-installer-" + version + ".jar";
        }
    }

    private void extractVersionFromMavenRelease() throws MalformedURLException {
        String RELEASE_MAVEN_METADATA = RELEASE_URL + "forge/forge-gui-desktop/maven-metadata.xml";
        URL metadataUrl = new URL(RELEASE_MAVEN_METADATA);
        String xml = FileUtil.readFileToString(metadataUrl);

        Pattern p = Pattern.compile("<release>(.*)</release>");
        Matcher m = p.matcher(xml);
        while (m.find()) {
            version = m.group(1);
        }
    }

    private boolean downloadUpdate(CompletableFuture<String> cf) throws URISyntaxException, IOException, ExecutionException, InterruptedException {
        // TODO Change the "auto" to be more auto.
        if (isLoading) {
            // We need to preload enough of a Skins to show a dialog and a button if we're in loading
            // splashScreen.prepareForDialogs();
            return downloadFromBrowser();
        }
        String logs = snapsBuildDate.isEmpty() ? "" : cf.get();
        String v = snapsBuildDate.isEmpty() ? version : version + TextUtil.enclosedParen(snapsBuildDate);
        String b = buildDate.isEmpty() ? buildVersion : buildVersion + TextUtil.enclosedParen(buildDate);
        String message = localizer.getMessage("lblNewVersionForgeAvailableUpdateConfirm", v, b) + logs;
        final List<String> options = ImmutableList.of(localizer.getMessage("lblUpdateNow"), localizer.getMessage("lblUpdateLater"));
        if (SOptionPane.showOptionDialog(message, localizer.getMessage("lblNewVersionAvailable"), null, options, 0) == 0) {
            return downloadFromForge();
        }

        return false;
    }

    private boolean downloadFromBrowser() throws URISyntaxException, IOException {
        final Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            // Linking directly there will auto download, but won't auto-update
            desktop.browse(new URI(packageUrl));
            return true;
        } else {
            System.out.println("Download latest version: " + packageUrl);
            return false;
        }
    }

    private boolean downloadFromForge() {
        System.out.println("Downloading update from " + packageUrl + " to Downloads folder");
        WaitCallback<Boolean> callback = new WaitCallback<Boolean>() {
            @Override
            public void run() {
                GuiBase.getInterface().download(new GuiDownloadZipService("Auto Updater", localizer.getMessage("lblNewVersionDownloading"), packageUrl, System.getProperty("user.home") + "/Downloads/", null, null) {
                    @Override
                    public void downloadAndUnzip() {
                        packagePath = download(version + "-upgrade.jar");
                        if (packagePath != null) {
                            restartAndUpdate(packagePath);
                        }
                    }
                }, this);
            }
        };

        SwingUtilities.invokeLater(callback);

        return false;
    }
    private void restartAndUpdate(String packagePath) {
        if (SOptionPane.showOptionDialog(localizer.getMessage("lblForgeUpdateMessage", packagePath), localizer.getMessage("lblRestart"), null, ImmutableList.of(localizer.getMessage("lblOK")), 0) == 0) {
            final Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null) {
                try {
                    File installer = new File(packagePath);
                    if (installer.exists()) {
                        if (packagePath.endsWith(".jar")) {
                            installer.setExecutable(true, false);
                            desktop.open(installer);
                        } else {
                            desktop.open(installer.getParentFile());
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println(packagePath);
            }
            System.exit(0);
        }
    }
    private void extractUpdate() {
        // TODO Something like https://stackoverflow.com/questions/315618/how-do-i-extract-a-tar-file-in-java
        final Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null) {
            try {
                desktop.open(new File(packagePath).getParentFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println(packagePath);
        }
    }

    private void restartForge() {
        if (isLoading || SOptionPane.showConfirmDialog(localizer.getMessage("lblForgeHasBeenUpdateRestartForgeToUseNewVersion"), localizer.getMessage("lblExitNowConfirm"))) {
            System.exit(0);
        }
    }
}
