package forge.util;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import forge.card.CardEdition;
import forge.item.IPaperCard;
import org.apache.commons.lang3.tuple.Pair;

import forge.ImageKeys;
import forge.StaticData;
import forge.gui.FThreads;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;

public abstract class ImageFetcher {
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();
    // see https://scryfall.com/docs/api/languages and
    // https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes
    private static final HashMap<String, String> langCodeMap = new HashMap<>();
    private static final Map<String, String> scryfallSetCodes = new HashMap<>();
    private static final String INVALID_SCRYFALL_SET_CODE = "NotFound";

    static {
        langCodeMap.put("en-US", "en");
        langCodeMap.put("es-ES", "es");
        langCodeMap.put("fr-FR", "fr");
        langCodeMap.put("de-DE", "de");
        langCodeMap.put("it-IT", "it");
        langCodeMap.put("pt-BR", "pt");
        langCodeMap.put("ja-JP", "ja");
        langCodeMap.put("ko-KR", "ko");
        langCodeMap.put("ru-RU", "ru");
        langCodeMap.put("zh-CN", "zhs");
        langCodeMap.put("zh-HK", "zht");
    };
    private HashMap<String, HashSet<Callback>> currentFetches = new HashMap<>();
    private HashMap<String, String> tokenImages;

    private static boolean isValidScryfallURL(final String urlString){
        try {
            URL u = new URL(urlString);
            HttpURLConnection huc = (HttpURLConnection) u.openConnection();
            huc.setInstanceFollowRedirects(true);
            huc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9.1.2) " +
                    "Gecko/20090729 Firefox/3.5.2 (.NET CLR 3.5.30729)");
            huc.setRequestMethod("HEAD");
            return (huc.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (IOException e) {
            return false;
        }

    }

    private String getScryfallDownloadURL(PaperCard c, boolean backFace, String langCode){
        String setCode = scryfallSetCodes.getOrDefault(c.getEdition(), null);
        if ((setCode != null) && (!setCode.equals(INVALID_SCRYFALL_SET_CODE))){
                return ForgeConstants.URL_PIC_SCRYFALL_DOWNLOAD +
                        ImageUtil.getScryfallDownloadUrl(c, backFace, setCode, langCode);
        }

        // No entry matched yet for edition
        StaticData data = StaticData.instance();
        CardEdition edition = data.getEditions().get(c.getEdition());
        if (edition == null) // edition does not exist - some error occurred with card data
            return null;
        // 1. Try MCI code first, as it original.
        String mciCode = edition.getMciCode().toLowerCase();
        String url = ForgeConstants.URL_PIC_SCRYFALL_DOWNLOAD +
                ImageUtil.getScryfallDownloadUrl(c, backFace, mciCode, langCode);
        if (isValidScryfallURL(url)) {
            scryfallSetCodes.put(c.getEdition(), setCode);
            return url;
        }
        // 2. MCI didn't work, so now try all other codes available in edition, alias included.
        // skipping dups with set, and returning as soon as one will work.
        Set<String> cardSetCodes = new HashSet<>();
        // all set-codes should be lower case
        cardSetCodes.add(mciCode); // add MCI
        cardSetCodes.add(edition.getCode().toLowerCase());
        cardSetCodes.add(edition.getCode2().toLowerCase());
        if (edition.getAlias() != null)
            cardSetCodes.add(edition.getAlias().toLowerCase());
        for (String code : cardSetCodes) {
            if (code.equals(mciCode))
                continue;  // Already checked, SKIP
            url = ForgeConstants.URL_PIC_SCRYFALL_DOWNLOAD +
                    ImageUtil.getScryfallDownloadUrl(c, backFace, code, langCode);
            if (isValidScryfallURL(url)) {
                scryfallSetCodes.put(c.getEdition(), setCode);
                return url;
            }
        }
        // If we're here, no valid URL has been found. Record this for the future
        scryfallSetCodes.put(c.getEdition(), INVALID_SCRYFALL_SET_CODE);
        return null;
    }

    public void fetchImage(final String imageKey, final Callback callback) {
        FThreads.assertExecutedByEdt(true);

        if (FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_DISABLE_CARD_IMAGES))
            return;

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
            final boolean backFace = ImageUtil.hasBackFacePicture(paperCard);
            final String filename = ImageUtil.getImageKey(paperCard, backFace, true);
            destFile = new File(ForgeConstants.CACHE_CARD_PICS_DIR, filename + ".jpg");

            //move priority of ftp image here
            StringBuilder setDownload = new StringBuilder(ForgeConstants.URL_PIC_DOWNLOAD);
            setDownload.append(ImageUtil.getDownloadUrl(paperCard, backFace));
            downloadUrls.add(setDownload.toString());
            final String cardCollectorNumber = paperCard.getCollectorNumber();
            if (!cardCollectorNumber.equals(IPaperCard.NO_COLLECTOR_NUMBER)){
                String langCode = "en";
                String UILang = FModel.getPreferences().getPref(ForgePreferences.FPref.UI_LANGUAGE);
                if (langCodeMap.containsKey(UILang))
                    langCode = langCodeMap.get(UILang);
                final String scryfallURL = this.getScryfallDownloadURL(paperCard, backFace, langCode);
                if (scryfallURL == null)
                    return;  // Non existing card, or Card's set not found in Scryfall
                downloadUrls.add(scryfallURL);
            }

        } else if (prefix.equals(ImageKeys.TOKEN_PREFIX)) {
            if (tokenImages == null) {
                tokenImages = new HashMap<>();
                for (Pair<String, String> nameUrlPair : FileUtil
                        .readNameUrlFile(ForgeConstants.IMAGE_LIST_TOKENS_FILE)) {
                    tokenImages.put(nameUrlPair.getLeft(), nameUrlPair.getRight());
                }
            }
            final String filename = imageKey.substring(2) + ".jpg";
            String tokenUrl = tokenImages.get(filename);
            if (tokenUrl == null) {
                System.err
                        .println("No specified file for '" + filename + "'.. Attempting to download from default Url");
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
            // TODO: Figure out why this codepath gets reached.
            //  Ideally, fetchImage() wouldn't be called if we already have the image.
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