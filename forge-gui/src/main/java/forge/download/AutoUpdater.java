package forge.download;

import forge.gui.GuiBase;
import forge.gui.download.GuiDownloadZipService;
import forge.gui.util.SOptionPane;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.*;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static forge.localinstance.properties.ForgeConstants.GITHUB_FORGE_URL;
import static forge.localinstance.properties.ForgeConstants.GITHUB_RELEASES_ATOM;
import static forge.localinstance.properties.ForgeConstants.GITHUB_SNAPSHOT_URL;

public class AutoUpdater {
    private static final Pattern RELEASE_TAG = Pattern.compile("releases/tag/forge-([0-9]+(?:[.][0-9]+)*)");
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
    private Date snapsBuildDate;

    public AutoUpdater(boolean loading) {
        isLoading = loading;
        updateChannel = FModel.getPreferences().getPref(ForgePreferences.FPref.AUTO_UPDATE);
        buildVersion = BuildInfo.getVersionString();
    }

    public Date getSnapsBuildDate() {
        return snapsBuildDate;
    }

    public boolean attemptToUpdate(CompletableFuture<String> cf) {
        if (!verifyUpdateable()) {
            return false;
        }
        try {
            if (downloadUpdate(cf)) {
                extractAndRestart();
            }
        } catch (IOException | URISyntaxException | ExecutionException | InterruptedException e) {
            return false;
        }
        return true;
    }

    private void extractAndRestart() {
        extractUpdate();
        restartForge();
    }

    public boolean verifyUpdateable() {
        if (buildVersion.contains("GIT")) {
            //return false;
        }

        if (isLoading) {
            // TODO This doesn't work yet, because FSkin isn't loaded at the time.
            return false;
        } else if (updateChannel.equals("none")) {
            String message = localizer.getMessage("lblYouHaventSetUpdateChannel");
            List<String> options = List.of(localizer.getMessageorUseDefault("lblCancel", "Cancel"), localizer.getMessageorUseDefault("lblRelease", "Release"), localizer.getMessageorUseDefault("lblSnapshot", "Snapshot"));
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

            versionUrlString = GITHUB_SNAPSHOT_URL + "version.txt";
        } else {
            if (!isReleaseChannel()) {
                System.out.println("Release build versions must use release update channel to work");
                return false;
            }
            versionUrlString = GITHUB_RELEASES_ATOM;
        }

        if (!testNetConnection()) {
            return false;
        }

        // Download appropriate version file
        return compareBuildWithLatestChannelVersion();
    }

    private boolean testNetConnection() {
        // test against the host updates are actually fetched from;
        // releases.cardforge.org is no longer reachable and blocked all updates
        String host;
        try {
            host = new URL(versionUrlString).getHost();
        } catch (MalformedURLException e) {
            host = "github.com";
        }
        try (Socket socket = new Socket()) {
            InetSocketAddress address = new InetSocketAddress(host, 443);
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
                URL url = new URL(GITHUB_SNAPSHOT_URL + "build.txt");
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                snapsBuildDate = simpleDateFormat.parse(FileUtil.readFileToString(url));
                buildDate = BuildInfo.getTimestamp().toString();
                return BuildInfo.verifyTimestamp(snapsBuildDate);
            }
            if (StringUtils.isEmpty(version) ) {
                return false;
            }
            if (buildVersion.equals(version)) {
                return false;
            }
        }
        catch (Exception e) {
            SOptionPane.showOptionDialog(e.getMessage(), localizer.getMessage("lblError"), null, List.of("Ok"));
            return false;
        }
        // If version doesn't match, it's assummably newer.
        return true;
    }

    private void retrieveVersion() throws MalformedURLException {
        if (isReleaseChannel()) {
            extractVersionFromLatestRelease();
            packageUrl = GITHUB_FORGE_URL + "releases/download/forge-" + version + "/forge-installer-" + version + ".jar";
        } else {
            URL versionUrl = new URL(versionUrlString);
            version = FileUtil.readFileToString(versionUrl);
            packageUrl = GITHUB_SNAPSHOT_URL + "forge-installer-" + version + ".jar";
        }
    }

    /**
     * Latest numbered release, from the releases feed. Entries are newest first, and the rolling
     * daily-snapshots pre-release is in there too, so only forge-&lt;version&gt; tags are considered.
     */
    private void extractVersionFromLatestRelease() throws MalformedURLException {
        String feed = FileUtil.readFileToString(new URL(GITHUB_RELEASES_ATOM));
        Matcher m = RELEASE_TAG.matcher(feed);
        if (m.find()) {
            version = m.group(1);
        }
    }

    private boolean isReleaseChannel() {
        return updateChannel.equalsIgnoreCase(localizer.getMessageorUseDefault("lblRelease", "Release"));
    }

    private boolean downloadUpdate(CompletableFuture<String> cf) throws URISyntaxException, IOException, ExecutionException, InterruptedException {
        // TODO Change the "auto" to be more auto.
        if (isLoading) {
            // We need to preload enough of a Skins to show a dialog and a button if we're in loading
            // splashScreen.prepareForDialogs();
            return downloadFromBrowser();
        }
        String logs = snapsBuildDate == null ? "" : cf.get();
        String v = snapsBuildDate == null ? version : version + TextUtil.enclosedParen(snapsBuildDate.toString());
        String b = buildDate.isEmpty() ? buildVersion : buildVersion + TextUtil.enclosedParen(buildDate);
        String message = localizer.getMessage("lblNewVersionForgeAvailableUpdateConfirm", v, b) + logs;
        final List<String> options = List.of(localizer.getMessage("lblUpdateNow"), localizer.getMessage("lblUpdateLater"));
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
        File downloaded = new File(packagePath);
        if (!downloaded.exists()) {
            return;
        }

        // Install it in place if we can tell where this copy of Forge lives, so the user doesn't
        // have to point the installer at their own installation by hand.
        if (UpdateInstaller.isSupported(downloaded)) {
            String installDir = UpdateInstaller.getInstallDir().getAbsolutePath();
            List<String> options = List.of(localizer.getMessage("lblUpdateNow"), localizer.getMessage("lblUpdateLater"));
            if (SOptionPane.showOptionDialog(localizer.getMessage("lblForgeUpdateInstallConfirm", installDir),
                    localizer.getMessage("lblRestart"), null, options, 0) != 0) {
                return; // the package stays where it was downloaded, they can run it whenever
            }
            if (UpdateInstaller.install(downloaded)) {
                System.exit(0);
            }
            // handing it over failed, fall through to the installer's own UI
        }

        if (SOptionPane.showOptionDialog(localizer.getMessage("lblForgeUpdateMessage", packagePath), localizer.getMessage("lblRestart"), null, List.of(localizer.getMessage("lblOK")), 0) == 0) {
            // still point the installer at this installation, so clicking through it updates
            // Forge instead of installing a second copy somewhere else
            if (UpdateInstaller.openInstaller(downloaded)) {
                System.exit(0);
            }
            final Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null) {
                try {
                    if (packagePath.endsWith(".jar")) {
                        downloaded.setExecutable(true, false);
                        desktop.open(downloaded);
                    } else {
                        desktop.open(downloaded.getParentFile());
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
