package forge.util;

import com.badlogic.gdx.files.FileHandle;
import forge.Forge;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgeConstants;
import io.sentry.Sentry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class LibGDXImageFetcher extends ImageFetcher {
    @Override
    protected Runnable getDownloadTask(String[] downloadUrls, String destPath, Runnable notifyObservers) {
        return new LibGDXDownloadTask(downloadUrls, destPath, notifyObservers);
    }

    private static class LibGDXDownloadTask implements Runnable {
        private final String[] downloadUrls;
        private final String destPath;
        private final Runnable notifyObservers;

        LibGDXDownloadTask(String[] downloadUrls, String destPath, Runnable notifyObservers) {
            this.downloadUrls = downloadUrls;
            this.destPath = destPath;
            this.notifyObservers = notifyObservers;
        }

        private boolean doFetch(String urlToDownload) throws IOException {
            if (disableHostedDownload && urlToDownload.startsWith(ForgeConstants.URL_CARDFORGE)) {
                // Don't try to download card images from cardforge servers
                return false;
            }

            if (scryfallCooldownTime != null && urlToDownload.startsWith(ForgeConstants.URL_PIC_SCRYFALL_DOWNLOAD)) {
                // Don't try to download card images from scryfall if we've been rate limited
                if (scryfallCooldownTime.after(new Date())) {
                    System.err.println("Currently in cooldown period for scryfall downloads. Skipping download attempt for: " + urlToDownload);
                    return false;
                } else {
                    // Cooldown period has expired, reset the cooldown time
                    scryfallCooldownTime = null;
                }
            }

            String newdespath = urlToDownload.contains(".fullborder.") || urlToDownload.startsWith(ForgeConstants.URL_PIC_SCRYFALL_DOWNLOAD) ?
                    TextUtil.fastReplace(destPath, ".full.", ".fullborder.") : destPath;
            if (!newdespath.contains(".full") && urlToDownload.startsWith(ForgeConstants.URL_PIC_SCRYFALL_DOWNLOAD) &&
                    !destPath.startsWith(ForgeConstants.CACHE_TOKEN_PICS_DIR) && !destPath.startsWith(ForgeConstants.CACHE_PLANECHASE_PICS_DIR))
                newdespath = newdespath.replace(".jpg", ".fullborder.jpg"); //fix planes/phenomenon for round border options
            URL url = new URL(urlToDownload);
            System.out.println("Attempting to fetch: " + url);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestProperty("Accept", "*/*");
            c.setRequestProperty("User-Agent", BuildInfo.getUserAgent());

            int responseCode = c.getResponseCode();
            String responseMessage = c.getResponseMessage();
            System.out.println("HTTP Response: " + responseCode + " " + responseMessage + " for URL: " + urlToDownload);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Failed to fetch image. HTTP code: " + responseCode + " (" + responseMessage + ") for URL: " + urlToDownload);
                c.disconnect();

                if (responseCode == 429) {
                    System.err.println("Device has been rate limited. Adding reduction of download attempts for this device.");
                    Sentry.captureMessage("Device has been rate limited. Adding reduction of download attempts for this device. " + urlToDownload);
                    // Don't try to download from scryfall for 5 minutes
                    scryfallCooldownTime = new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5));
                }

                return false;
            }

            InputStream is = c.getInputStream();
            // First, save to a temporary file so that nothing tries to read
            // a partial download.
            FileHandle destFile = new FileHandle(newdespath + ".tmp");
            System.out.println(newdespath);
            destFile.parent().mkdirs();
            try(OutputStream out = Files.newOutputStream(destFile.file().toPath())) {
                // Conversion to JPEG will be handled differently depending on the platform
                Forge.getDeviceAdapter().convertToJPEG(is, out);
                is.close();
            }
            destFile.moveTo(new FileHandle(newdespath));
            c.disconnect();

            System.out.println("Saved image to " + newdespath);
            GuiBase.getInterface().invokeInEdtLater(notifyObservers);
            return true;
        }

        private String tofullBorder(String imageurl) {
            if (!imageurl.contains(".full.jpg"))
                return imageurl;
            try {
                URL url = new URL(imageurl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                //connection.setConnectTimeout(1000 * 5); //wait 5 seconds the most
                //connection.setReadTimeout(1000 * 5);
                conn.setRequestProperty("User-Agent", BuildInfo.getUserAgent());
                if(conn.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND)
                    imageurl = TextUtil.fastReplace(imageurl, ".full.jpg", ".fullborder.jpg");
                conn.disconnect();
                return imageurl;
            } catch (IOException ex) {
                return imageurl;
            }
        }

        public void run() {
            boolean success = false;
            for (String urlToDownload : downloadUrls) {
                boolean isPlanechaseBG = urlToDownload.startsWith("PLANECHASEBG:");
                try {
                    success = doFetch(urlToDownload.replace("PLANECHASEBG:", ""));
                    if (success) {
                        break;
                    }
                } catch (IOException e) {
                    if (isPlanechaseBG) {
                        System.err.println("Failed to download planechase background [" + destPath + "] image: " + e.getMessage());
                    } else {
                        System.err.println("Failed to download card [" + destPath + "] image: " + e.getMessage());
                        if (urlToDownload.contains("tokens")) {
                            int setIndex = urlToDownload.lastIndexOf('_');
                            int typeIndex = urlToDownload.lastIndexOf('.');
                            String setlessFilename = urlToDownload.substring(0, setIndex);
                            String extension = urlToDownload.substring(typeIndex);
                            urlToDownload = setlessFilename + extension;
                            try {
                                try {
                                    TimeUnit.MILLISECONDS.sleep(100);
                                } catch (InterruptedException ex) {
                                    throw new RuntimeException(ex);
                                }
                                success = doFetch(urlToDownload);
                                if (success) {
                                    break;
                                }
                            } catch (IOException t) {
                                System.out.println("Failed to download setless token [" + destPath + "]: " + e.getMessage());
                            }
                        }
                    }
                } finally {
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
    }

}
