package forge.util;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

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
            URL url = new URL(urlToDownload);
            System.out.println("Attempting to fetch: " + url);
            BufferedImage image = ImageIO.read(url);
            // First, save to a temporary file so that nothing tries to read
            // a partial download.
            File destFile = new File(destPath + ".tmp");
            // need to check directory folder for mkdir
            destFile.getParentFile().mkdirs();
            if (ImageIO.write(image, "jpg", destFile)) {
                // Now, rename it to the correct name.
                if (destFile.renameTo(new File(destPath))) {
                    System.out.println("Saved image to " + destPath);
                    SwingUtilities.invokeLater(notifyObservers);
                } else {
                    System.err.println("Failed to rename image to " + destPath);
                }
            } else {
                System.err.println("Failed to save image from " + url + " as jpeg");
                // try to save image as png instead
                if (ImageIO.write(image, "png", destFile)) {
                    String newPath = destPath.replace(".jpg", ".png");
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

        public void run() {
            for (String urlToDownload : downloadUrls) {
                try {
                    doFetch(urlToDownload);
                    break;
                } catch (IOException e) {
                    System.err.println("Failed to download card [" + destPath + "] image: " + e.getMessage());
                }
            }
        }
    }

}
