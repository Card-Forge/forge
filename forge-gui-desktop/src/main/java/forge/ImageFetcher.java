package forge;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.tuple.Pair;

import forge.game.card.CardView;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences;
import forge.util.FileUtil;
import forge.util.ImageUtil;

public class ImageFetcher {
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();
    private static HashMap<String, HashSet<Callback>> currentFetches = new HashMap<>();
    private static HashMap<String, String> tokenImages;
    private static boolean FETCH = FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_ENABLE_ONLINE_IMAGE_FETCHER);

    public static void fetchImage(final CardView card, final String imageKey, final Callback callback) {
        FThreads.assertExecutedByEdt(true);

        if (!FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_ENABLE_ONLINE_IMAGE_FETCHER))
            return;

        // Fake card (like the ante prompt) trying to be "fetched"
        if (imageKey.length() < 2)
            return;

        final String prefix = imageKey.substring(0, 2);
        final ArrayList<String> downloadUrls = new ArrayList<>();
        File destFile = null;
        if (prefix.equals(ImageKeys.CARD_PREFIX)) {
            PaperCard paperCard = ImageUtil.getPaperCardFromImageKey(imageKey);
            if (paperCard == null) {
                System.err.println("Paper card not found for: " + imageKey);
                return;
            }
            final boolean backFace = imageKey.endsWith(ImageKeys.BACKFACE_POSTFIX);
            final String filename = ImageUtil.getImageKey(paperCard, backFace, true);
            destFile = new File(ForgeConstants.CACHE_CARD_PICS_DIR + "/" + filename + ".jpg");

            // First, try to fetch from magiccards.info, if we have the collector's number to generate a URL.
            final StaticData data = StaticData.instance();
            final int cardNum = data.getCommonCards().getCardCollectorNumber(paperCard.getName(), paperCard.getEdition());
            if (cardNum != -1)  {
                String suffix = "";
                if (paperCard.getRules().getOtherPart() != null) {
                    suffix = (backFace ? "b" : "a");
                }
                final String editionMciCode = data.getEditions().getMciCodeByCode(paperCard.getEdition());
                downloadUrls.add(String.format("http://magiccards.info/scans/en/%s/%d%s.jpg", editionMciCode, cardNum, suffix));
            }

            // Otherwise, try the LQ image URL.
            final String[] fallbackUrlParts = ImageUtil.getDownloadUrlAndDestination(ForgeConstants.CACHE_CARD_PICS_DIR, paperCard, backFace);
            if (fallbackUrlParts != null) {
                downloadUrls.add(fallbackUrlParts[0]);
            }
        } else if (prefix.equals(ImageKeys.TOKEN_PREFIX)) {
            if (tokenImages == null) {
                tokenImages = new HashMap<>();
                for (Pair<String, String> nameUrlPair : FileUtil.readNameUrlFile(ForgeConstants.IMAGE_LIST_TOKENS_FILE)) {
                    tokenImages.put(nameUrlPair.getLeft(), nameUrlPair.getRight());
                }
            }
            final String filename = imageKey.substring(2) + ".jpg";
            final String tokenUrl = tokenImages.get(filename);
            if (tokenUrl == null) {
                System.err.println("Token " + imageKey + " not found in: " + ForgeConstants.IMAGE_LIST_TOKENS_FILE);
                return;
            }
            destFile = new File(ForgeConstants.CACHE_TOKEN_PICS_DIR, filename);
            downloadUrls.add(tokenUrl);
        }

        if (downloadUrls.isEmpty()) {
            System.err.println("No download URLs for: " + imageKey);
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
            observers.add(callback);
            return;
        }

        observers = new HashSet<>();
        observers.add(callback);
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
        threadPool.submit(new DownloadTask(downloadUrls.toArray(new String[0]), destPath, notifyObservers));
    }
    
    public static interface Callback {
        public void onImageFetched();
    }
    
    private static class DownloadTask implements Runnable {
        private final String[] downloadUrls;
        private final String destPath;
        private final Runnable notifyObservers;

        public DownloadTask(String[] downloadUrls, String destPath, Runnable notifyObservers) {
            this.downloadUrls = downloadUrls;
            this.destPath = destPath;
            this.notifyObservers = notifyObservers;
        }
        
        private void doFetch(String urlToDownload) throws IOException {
            URL url = new URL(urlToDownload);
            System.out.println("Attempting to fetch: " + url);
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
        }

        public void run() {
            for (String urlToDownload : downloadUrls) {
                try {
                    doFetch(urlToDownload);
                    break;
                } catch (IOException e) {
                    System.out.println("Failed to download card [" + destPath + "] image: " + e.getMessage());
                }
            }
        }
    }
 }