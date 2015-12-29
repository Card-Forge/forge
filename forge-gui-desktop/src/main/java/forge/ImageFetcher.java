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
    private static HashMap<String, HashSet<Callback>> currentFetches = new HashMap<>();
    private static HashMap<String, String> tokenImages;

    public static void fetchImage(CardView card, final String imageKey, Callback cachedImage) {
        FThreads.assertExecutedByEdt(true);

        final String prefix = imageKey.substring(0, 2);
        final File destFile;
        final String urlToDownload;
        if (prefix.equals(ImageKeys.CARD_PREFIX)) {
            PaperCard paperCard = ImageUtil.getPaperCardFromImageKey(imageKey);
            if (paperCard == null) {
                System.err.println("Paper card not found for: " + imageKey);
                return;
            }
            final boolean backFace = imageKey.endsWith(ImageKeys.BACKFACE_POSTFIX);
            final String filename = ImageUtil.getImageKey(paperCard, backFace, false);
            final StaticData data = StaticData.instance();
            final String editionCode2 = data.getEditions().getCode2ByCode(paperCard.getEdition());
            destFile = new File(ForgeConstants.CACHE_CARD_PICS_DIR + "/" + editionCode2 + "/" + filename + ".jpg");

            // First, try to fetch from magiccards.info, if we have the collector's number to generate a URL.
            final int cardNum = data.getCommonCards().getCardCollectorNumber(paperCard.getName(), paperCard.getEdition());
            if (cardNum != -1)  {
                String suffix = "";
                if (paperCard.getRules().getOtherPart() != null) {
                    suffix = (backFace ? "b" : "a");
                }
                urlToDownload = String.format("http://magiccards.info/scans/en/%s/%d%s.jpg", editionCode2.toLowerCase(), cardNum, suffix);
            } else {
                // Fall back to using Forge's LQ card downloaded from Wizards' website. This currently only works for older cards.
                String[] result = ImageUtil.getDownloadUrlAndDestination(ForgeConstants.CACHE_CARD_PICS_DIR, paperCard, backFace);
                if (result == null) {
                    return;
                }
                urlToDownload = result[0];
            }
        } else if (prefix.equals(ImageKeys.TOKEN_PREFIX)) {
            if (tokenImages == null) {
                tokenImages = new HashMap<>();
                for (Pair<String, String> nameUrlPair : FileUtil.readNameUrlFile(ForgeConstants.IMAGE_LIST_TOKENS_FILE)) {
                    tokenImages.put(nameUrlPair.getLeft(), nameUrlPair.getRight());
                }
            }
            final String filename = imageKey.substring(2) + ".jpg";
            urlToDownload = tokenImages.get(filename);
            if (urlToDownload == null) {
                System.err.println("Token " + imageKey + " not found in: " + ForgeConstants.IMAGE_LIST_TOKENS_FILE);
                return;
            }
            destFile = new File(ForgeConstants.CACHE_TOKEN_PICS_DIR, filename);
        } else {
            System.err.println("Cannot fetch image for: " + imageKey);
            return;
        }
        if (destFile.exists()) {
            // TODO: Figure out why this codepath gets reached. Ideally, fetchImage() wouldn't
            // be called if we already have the image.
            return;
        }
        final String destPath = destFile.getAbsolutePath();

        // Note: No synchronization is needed here because this is executed on
        // EDT thread (see assert on top) and so is the notification of observers.
        HashSet<Callback> observers = currentFetches.get(destPath);
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

                for (Callback o : currentFetches.get(destPath)) {
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
                    // First, save to a temporary file so that nothing tries to read
                    // a partial download.
                    File destFile = new File(destPath + ".tmp");
                    destFile.mkdirs();
                    ImageIO.write(image, "jpg", destFile);
                    // Now, rename it to the correct name.
                    destFile.renameTo(new File(destPath));
                    System.out.println("Saved image to " + destPath);
                    SwingUtilities.invokeLater(notifyObservers);
                } catch (IOException e) {
                    System.err.println("Failed to download card image: " + e.getMessage());
                }
            }
        });
    }
    
    public static interface Callback {
        public void onImageFetched();
    }
 }