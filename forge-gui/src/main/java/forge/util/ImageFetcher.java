package forge.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import forge.FThreads;
import forge.ImageKeys;
import forge.StaticData;
import org.apache.commons.lang3.tuple.Pair;

import forge.item.PaperCard;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences;

public abstract class ImageFetcher {
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();
    private HashMap<String, HashSet<Callback>> currentFetches = new HashMap<>();
    private HashMap<String, String> tokenImages;

    public void fetchImage(final String imageKey, final Callback callback) {
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

            // First try to download the LQ Set URL, then fetch from scryfall
            StringBuilder setDownload = new StringBuilder(ForgeConstants.URL_PIC_DOWNLOAD);
            setDownload.append(ImageUtil.getDownloadUrl(paperCard, backFace));
            downloadUrls.add(setDownload.toString());

            int artIndex = 1;
            final Pattern pattern = Pattern.compile(
                    "^.:([^|]*\\|){2}(\\d+).*$"
            );
            Matcher matcher = pattern.matcher(imageKey);
            if (matcher.matches()) {
                artIndex = Integer.parseInt(matcher.group(2));
            }
            final StaticData data = StaticData.instance();
            final String cardNum = data.getCommonCards().getCardCollectorNumber(paperCard.getName(), paperCard.getEdition(), artIndex);
            if (cardNum != null)  {
                String suffix = "";
                if (paperCard.getRules().getOtherPart() != null) {
                    suffix = (backFace ? "b" : "a");
                }
                final String editionMciCode = data.getEditions().getMciCodeByCode(paperCard.getEdition());
                downloadUrls.add(String.format("https://img.scryfall.com/cards/normal/en/%s/%s%s.jpg", editionMciCode, cardNum, suffix));
            }
        } else if (prefix.equals(ImageKeys.TOKEN_PREFIX)) {
            if (tokenImages == null) {
                tokenImages = new HashMap<>();
                for (Pair<String, String> nameUrlPair : FileUtil.readNameUrlFile(ForgeConstants.IMAGE_LIST_TOKENS_FILE)) {
                    tokenImages.put(nameUrlPair.getLeft(), nameUrlPair.getRight());
                }
            }
            final String filename = imageKey.substring(2) + ".jpg";
            String tokenUrl = tokenImages.get(filename);
            if (tokenUrl == null) {
                System.err.println("No specified file for '" + filename + "'.. Attempting to download from default Url");
                tokenUrl = String.format("%s%s", ForgeConstants.URL_TOKEN_DOWNLOAD, filename);
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
        threadPool.submit(getDownloadTask(downloadUrls.toArray(new String[0]), destPath, notifyObservers));
    }

    protected abstract Runnable getDownloadTask(String[] toArray, String destPath, Runnable notifyObservers);

    public interface Callback {
        void onImageFetched();
    }
}