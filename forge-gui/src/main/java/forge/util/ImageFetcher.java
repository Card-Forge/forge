package forge.util;

import forge.ImageKeys;
import forge.StaticData;
import forge.card.CardEdition;
import forge.gui.FThreads;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

public abstract class ImageFetcher {
    // see https://scryfall.com/docs/api/languages and
    // https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes
    private static final HashMap<String, String> langCodeMap = new HashMap<>();

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

    private String getScryfallDownloadURL(PaperCard c, String face, boolean useArtCrop, boolean hasSetLookup, String imagePath, ArrayList<String> downloadUrls) {
        StaticData data = StaticData.instance();
        CardEdition edition = data.getEditions().get(c.getEdition());
        if (edition == null) // edition does not exist - some error occurred with card data
            return null;
        if (hasSetLookup) {
            List<PaperCard> clones = StaticData.instance().getCommonCards().getAllCards(c.getName());
            for (PaperCard pc : clones) {
                if (clones.size() > 1) {//clones only
                    if (!c.getEdition().equalsIgnoreCase(pc.getEdition())) {
                        CardEdition ed = data.getEditions().get(pc.getEdition());
                        if (ed != null) {
                            String setCode =ed.getScryfallCode();
                            String langCode = ed.getCardsLangCode();
                            downloadUrls.add(ForgeConstants.URL_PIC_SCRYFALL_DOWNLOAD + ImageUtil.getScryfallDownloadUrl(pc, face, setCode, langCode, useArtCrop));
                        }
                    }
                } else {// original from set
                    CardEdition ed = data.getEditions().get(pc.getEdition());
                    if (ed != null) {
                        String setCode =ed.getScryfallCode();
                        String langCode = ed.getCardsLangCode();
                        downloadUrls.add(ForgeConstants.URL_PIC_SCRYFALL_DOWNLOAD + ImageUtil.getScryfallDownloadUrl(pc, face, setCode, langCode, useArtCrop));
                    }
                }
            }
        } else {
            String setCode = edition.getScryfallCode();
            String langCode = edition.getCardsLangCode();
            String primaryUrl = ImageUtil.getScryfallDownloadUrl(c, face, setCode, langCode, useArtCrop);
            downloadUrls.add(ForgeConstants.URL_PIC_SCRYFALL_DOWNLOAD + primaryUrl);

            String alternateUrl = ImageUtil.getScryfallDownloadUrl(c, face, setCode, langCode, useArtCrop, true);
            if (alternateUrl != null && !alternateUrl.equals(primaryUrl)) {
                downloadUrls.add(ForgeConstants.URL_PIC_SCRYFALL_DOWNLOAD + alternateUrl);
            }
        }
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

        //planechaseBG file...
        if (imageKey.startsWith("PLANECHASEBG:")) {
            final ArrayList<String> downloadUrls = new ArrayList<>();
            final String filename = imageKey.substring("PLANECHASEBG:".length());
            downloadUrls.add("https://downloads.cardforge.org/images/planes/" + filename);
            FileUtil.ensureDirectoryExists(ForgeConstants.CACHE_PLANECHASE_PICS_DIR);
            File destFile = new File(ForgeConstants.CACHE_PLANECHASE_PICS_DIR, filename);
            if (destFile.exists())
                return;

            setupObserver(destFile.getAbsolutePath(), callback, downloadUrls);
            return;
        }

        boolean useArtCrop = "Crop".equals(FModel.getPreferences().getPref(ForgePreferences.FPref.UI_CARD_ART_FORMAT));
        final String prefix = imageKey.substring(0, 2);
        final ArrayList<String> downloadUrls = new ArrayList<>();
        File destFile = null;
        if (prefix.equals(ImageKeys.CARD_PREFIX)) {
            PaperCard paperCard = ImageUtil.getPaperCardFromImageKey(imageKey);
            if (paperCard == null) {
                System.err.println("Paper card not found for: " + imageKey);
                return;
            }
            //Skip fetching if it's a custom user card.
            if (paperCard.getRules().isCustom()) {
                return;
            }
            // Skip fetching if artist info is not available for art crop
            if (useArtCrop && paperCard.getArtist().isEmpty())
                return;
            String imagePath = ImageUtil.getImageRelativePath(paperCard, "", true, false);
            final boolean hasSetLookup = ImageKeys.hasSetLookup(imagePath);
            String face = "";
            if (imageKey.endsWith(ImageKeys.BACKFACE_POSTFIX)) {
                face = "back";
            } else if (imageKey.endsWith(ImageKeys.SPECFACE_W)) {
                face = "white";
            } else if (imageKey.endsWith(ImageKeys.SPECFACE_U)) {
                face = "blue";
            } else if (imageKey.endsWith(ImageKeys.SPECFACE_B)) {
                face = "black";
            } else if (imageKey.endsWith(ImageKeys.SPECFACE_R)) {
                face = "red";
            } else if (imageKey.endsWith(ImageKeys.SPECFACE_G)) {
                face = "green";
            }
            String filename = "";
            switch (face) {
                case "back":
                    filename = paperCard.getCardAltImageKey();
                    break;
                case "white":
                    filename = paperCard.getCardWSpecImageKey();
                    break;
                case "blue":
                    filename = paperCard.getCardUSpecImageKey();
                    break;
                case "black":
                    filename = paperCard.getCardBSpecImageKey();
                    break;
                case "red":
                    filename = paperCard.getCardRSpecImageKey();
                    break;
                case "green":
                    filename = paperCard.getCardGSpecImageKey();
                    break;
                default:
                    filename = paperCard.getCardImageKey();
                    break;
            }
            if (useArtCrop) {
                filename = TextUtil.fastReplace(filename, ".full", ".artcrop");
            }
            boolean updateLink = false;
            if ("back".equals(face)) {// seems getimage relative path don't process variants for back faces.
                try {
                    filename = TextUtil.fastReplace(filename, "1.full", imageKey.substring(imageKey.lastIndexOf('|') + 1, imageKey.indexOf('$')) + ".full");
                    updateLink = true;
                } catch (Exception e) {
                    filename = paperCard.getCardAltImageKey();
                    updateLink = false;
                }
            }
            destFile = new File(ForgeConstants.CACHE_CARD_PICS_DIR, filename + ".jpg");

            //skip ftp if using art crop
            if (!useArtCrop) {
                //move priority of ftp image here
                StringBuilder setDownload = new StringBuilder(ForgeConstants.URL_PIC_DOWNLOAD);
                if (!hasSetLookup) {
                    if (!updateLink) {
                        setDownload.append(ImageUtil.getDownloadUrl(paperCard, face));
                        downloadUrls.add(setDownload.toString());
                    } else {
                        String url = ImageUtil.getDownloadUrl(paperCard, face);
                        setDownload.append(TextUtil.fastReplace(url, "1.full", imageKey.substring(imageKey.lastIndexOf('|') + 1, imageKey.indexOf('$')) + ".full"));
                        downloadUrls.add(setDownload.toString());
                    }
                } else {
                    List<PaperCard> clones = StaticData.instance().getCommonCards().getAllCards(paperCard.getName());
                    for (PaperCard pc : clones) {
                        if (clones.size() > 1) {//clones only
                            if (!paperCard.getEdition().equalsIgnoreCase(pc.getEdition())) {
                                StringBuilder set = new StringBuilder(ForgeConstants.URL_PIC_DOWNLOAD);
                                set.append(ImageUtil.getDownloadUrl(pc, face));
                                downloadUrls.add(set.toString());
                            }
                        } else {// original from set
                            StringBuilder set = new StringBuilder(ForgeConstants.URL_PIC_DOWNLOAD);
                            set.append(ImageUtil.getDownloadUrl(pc, face));
                            downloadUrls.add(set.toString());
                        }
                    }
                }
            }
            final String cardCollectorNumber = paperCard.getCollectorNumber();
            if (!cardCollectorNumber.equals(IPaperCard.NO_COLLECTOR_NUMBER)) {
                // This function adds to downloadUrls for us
                this.getScryfallDownloadURL(paperCard, face, useArtCrop, hasSetLookup, filename, downloadUrls);
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
                String[] tempdata = imageKey.split("[_](?=[^_]*$)"); //We want to check the edition first.
                if (tempdata.length == 2) {
                    CardEdition E = StaticData.instance().getEditions().get(tempdata[1]);
                    if (E != null && E.getType() == CardEdition.Type.CUSTOM_SET) return; //Custom set token, skip fetching.
                }
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
            // TODO: Figure out why this codepath gets reached.
            //  Ideally, fetchImage() wouldn't be called if we already have the image.
            if (prefix.equals(ImageKeys.CARD_PREFIX)) {
                PaperCard paperCard = ImageUtil.getPaperCardFromImageKey(imageKey);
                if (paperCard != null)
                    paperCard.hasImage(true);
            }
            return;
        }

        setupObserver(destFile.getAbsolutePath(), callback, downloadUrls);
    }
    private void setupObserver(final String destPath, final Callback callback, final ArrayList<String> downloadUrls) {
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

        final Runnable notifyObservers = () -> {
            FThreads.assertExecutedByEdt(true);

            for (Callback o : currentFetches.get(destPath)) {
                if (o != null)
                    o.onImageFetched();
            }
            currentFetches.remove(destPath);
        };
        try {
            ThreadUtil.getServicePool().submit(getDownloadTask(downloadUrls.toArray(new String[0]), destPath, notifyObservers));
        } catch (RejectedExecutionException re) {
            re.printStackTrace();
        }
    }

    protected abstract Runnable getDownloadTask(String[] toArray, String destPath, Runnable notifyObservers);

    public interface Callback {
        void onImageFetched();
    }
}