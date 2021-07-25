package forge.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

public class SwingImageFetcher extends ImageFetcher {

    @Override
    protected Runnable getDownloadTask(String[] downloadUrls, String destPath, Runnable notifyObservers) {
        return new SwingDownloadTask(downloadUrls, destPath, notifyObservers);
    }

    private static class SwingDownloadTask implements Runnable {
        private final String[] downloadUrls;
        private final String destPath;
        private final Runnable notifyObservers;

        public SwingDownloadTask(String[] downloadUrls, String destPath, Runnable notifyObservers) {
            this.downloadUrls = downloadUrls;
            this.destPath = destPath;
            this.notifyObservers = notifyObservers;
        }

        private void doFetch(String urlToDownload) throws IOException {
            String newdespath = urlToDownload.contains(".fullborder.jpg") ?
                    TextUtil.fastReplace(destPath, ".full.jpg", ".fullborder.jpg") : destPath;
            URL url = new URL(urlToDownload);
            System.out.println("Attempting to fetch: " + url);
            BufferedImage image = ImageIO.read(url);
            // First, save to a temporary file so that nothing tries to read
            // a partial download.
            File destFile = new File(newdespath + ".tmp");
            // need to check directory folder for mkdir
            destFile.getParentFile().mkdirs();
            if (ImageIO.write(image, "jpg", destFile)) {
                // Now, rename it to the correct name.
                if (destFile.renameTo(new File(newdespath))) {
                    System.out.println("Saved image to " + newdespath);
                    SwingUtilities.invokeLater(notifyObservers);
                } else {
                    System.err.println("Failed to rename image to " + newdespath);
                }
            } else {
                System.err.println("Failed to save image from " + url + " as jpeg");
                // try to save image as png instead
                if (ImageIO.write(image, "png", destFile)) {
                    String newPath = newdespath.replace(".jpg", ".png");
                    if (destFile.renameTo(new File(newPath))) {
                        System.out.println("Saved image to " + newPath);
                        SwingUtilities.invokeLater(notifyObservers);
                    } else {
                        System.err.println("Failed to rename image to " + newPath);
                    }
                } else {
                    System.err.println("Failed to save image from " + url + " as png");
                }
            }
        }

        private String tofullBorder(String imageurl) {
            if (!imageurl.contains(".full.jpg"))
                return imageurl;
            try {
                URL url = new URL(imageurl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                //connection.setConnectTimeout(1000 * 5); //wait 5 seconds the most
                //connection.setReadTimeout(1000 * 5);
                conn.setRequestProperty("User-Agent", "");
                if(conn.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND)
                    imageurl = TextUtil.fastReplace(imageurl, ".full.jpg", ".fullborder.jpg");
                conn.disconnect();
                return imageurl;
            } catch (IOException ex) {
                return imageurl;
            }
        }

        public void run() {
            for (String urlToDownload : downloadUrls) {
                try {
                    doFetch(tofullBorder(urlToDownload));
                    break;
                } catch (IOException e) {
                    System.err.println("Failed to download card [" + destPath + "] image: " + e.getMessage());
                    if (urlToDownload.contains("tokens")) {
                        int setIndex = urlToDownload.lastIndexOf('_');
                        int typeIndex = urlToDownload.lastIndexOf('.');
                        String setlessFilename = urlToDownload.substring(0, setIndex);
                        String extension = urlToDownload.substring(typeIndex);
                        urlToDownload = setlessFilename+extension;
                        try {
                            doFetch(tofullBorder(urlToDownload));
                            break;
                        } catch (IOException t) {
                            System.err.println("Failed to download setless token [" + destPath + "]: " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

}
