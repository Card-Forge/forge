package forge;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import forge.game.card.CardView;
import forge.item.PaperCard;
import forge.properties.ForgeConstants;
import forge.util.ImageUtil;

public class ImageFetcher {
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();
    private static HashMap<String, HashSet<CachedCardImage>> currentFetches = new HashMap<>();

    public static void fetchImage(CardView card, final String imageKey, CachedCardImage cachedImage) {
        FThreads.assertExecutedByEdt(true);

        final String prefix = imageKey.substring(0, 2);
        if (!prefix.equals(ImageKeys.CARD_PREFIX)) {
            return;
        }
        PaperCard paperCard = ImageUtil.getPaperCardFromImageKey(imageKey);
        if (paperCard == null) {
            return;
        }

        boolean backFace = imageKey.endsWith(ImageKeys.BACKFACE_POSTFIX);
        String[] result = ImageUtil.getDownloadUrlAndDestination(ForgeConstants.CACHE_CARD_PICS_DIR, paperCard, backFace);
        if (result == null) {
            return;
        }
        final String urlToDownload = result[0];
        final String destPath = result[1];

        // Note: No synchronization is needed here because this is executed on
        // EDT thread (see assert on top) and so is the notification of observers.
        HashSet<CachedCardImage> observers = currentFetches.get(destPath);
        if (observers != null) {
            // Already in the queue, simply add the new observer.
            observers.add(cachedImage);
            return;
        }

        observers = new HashSet<>();
        observers.add(cachedImage);
        currentFetches.put(destPath, observers);

        final Runnable notifyObservers = new Runnable() {
            public void run() {
                FThreads.assertExecutedByEdt(true);

                for (CachedCardImage o : currentFetches.get(destPath)) {
                    o.onImageFetched();
                }
                currentFetches.remove(destPath);
            }
        };

        threadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("Attempting to fetch: " + urlToDownload);
                    URL url = new URL(urlToDownload);
                    BufferedImage image = ImageIO.read(url.openStream());
                    File destFile = new File(destPath);
                    destFile.mkdirs();
                    ImageIO.write(image, "jpg", destFile);
                    System.out.println("Saved image to " + destFile);

                    SwingUtilities.invokeLater(notifyObservers);
                } catch (IOException e) {
                    System.err.println("Failed to download card image: " + e.getMessage());
                }
            }
        });
    }
}