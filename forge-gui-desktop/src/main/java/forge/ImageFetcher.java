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

import org.apache.commons.lang3.tuple.Pair;

import forge.game.card.CardView;
import forge.item.PaperCard;
import forge.properties.ForgeConstants;
import forge.util.FileUtil;
import forge.util.ImageUtil;

public class ImageFetcher {
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();
    private static HashMap<String, HashSet<CachedCardImage>> currentFetches = new HashMap<>();
    private static HashMap<String, String> tokenImages;

    public static void fetchImage(CardView card, final String imageKey, CachedCardImage cachedImage) {
        FThreads.assertExecutedByEdt(true);

        final String prefix = imageKey.substring(0, 2);
        String[] result = null;
        if (prefix.equals(ImageKeys.CARD_PREFIX)) {
            PaperCard paperCard = ImageUtil.getPaperCardFromImageKey(imageKey);
            if (paperCard == null) {
                System.err.println("Paper card not found for: " + imageKey);
                return;
            }
            boolean backFace = imageKey.endsWith(ImageKeys.BACKFACE_POSTFIX);
            result = ImageUtil.getDownloadUrlAndDestination(ForgeConstants.CACHE_CARD_PICS_DIR, paperCard, backFace);
        } else if (prefix.equals(ImageKeys.TOKEN_PREFIX)) {
            if (tokenImages == null) {
                tokenImages = new HashMap<>();
                for (Pair<String, String> nameUrlPair : FileUtil.readNameUrlFile(ForgeConstants.IMAGE_LIST_TOKENS_FILE)) {
                    tokenImages.put(nameUrlPair.getLeft(), nameUrlPair.getRight());
                }
            }
            String filename = imageKey.substring(2) + ".jpg";
            String url = tokenImages.get(filename);
            if (url == null) {
                System.err.println("Token " + imageKey + " not found in: " + ForgeConstants.IMAGE_LIST_TOKENS_FILE);
                return;
            }
            result = new String[] { url, new File(ForgeConstants.CACHE_TOKEN_PICS_DIR, filename).getAbsolutePath() };
        } else {
            System.err.println("Cannot fetch image for: " + imageKey);
            return;
        }

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