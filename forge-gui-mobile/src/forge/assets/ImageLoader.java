package forge.assets;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.google.common.cache.CacheLoader;

import forge.Forge;
import forge.ImageKeys;
import forge.card.CardImageRenderer;
import forge.game.card.Card;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.properties.ForgeConstants;

import org.apache.commons.lang3.StringUtils;

import java.io.File;

final class ImageLoader extends CacheLoader<String, Texture> {
    // image file extensions for various formats in order of likelihood
    // the last, empty, string is for keys that come in with an extension already in place
    private static final String[] _FILE_EXTENSIONS = { ".jpg", ".png", "" };

    @Override
    public Texture load(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }

        final String path;
        final String filename;
        boolean isCard = false;
        if (key.startsWith(ImageKeys.TOKEN_PREFIX)) {
            filename = key.substring(ImageKeys.TOKEN_PREFIX.length());
            path = ForgeConstants.CACHE_TOKEN_PICS_DIR;
            isCard = true;
        }
        else if (key.startsWith(ImageKeys.ICON_PREFIX)) {
            filename = key.substring(ImageKeys.ICON_PREFIX.length());
            path = ForgeConstants.CACHE_ICON_PICS_DIR;
        }
        else if (key.startsWith(ImageKeys.BOOSTER_PREFIX)) {
            filename = key.substring(ImageKeys.BOOSTER_PREFIX.length());
            path = ForgeConstants.CACHE_BOOSTER_PICS_DIR;
        }
        else if (key.startsWith(ImageKeys.FATPACK_PREFIX)) {
            filename = key.substring(ImageKeys.FATPACK_PREFIX.length());
            path = ForgeConstants.CACHE_FATPACK_PICS_DIR;
        }
        else if (key.startsWith(ImageKeys.PRECON_PREFIX)) {
            filename = key.substring(ImageKeys.PRECON_PREFIX.length());
            path = ForgeConstants.CACHE_PRECON_PICS_DIR;
        }
        else if (key.startsWith(ImageKeys.TOURNAMENTPACK_PREFIX)) {
            filename = key.substring(ImageKeys.TOURNAMENTPACK_PREFIX.length());
            path = ForgeConstants.CACHE_TOURNAMENTPACK_PICS_DIR;
        }
        else {
            filename = key;
            path = ForgeConstants.CACHE_CARD_PICS_DIR;
            isCard = true;
        }

        Texture ret = findFile(key, path, filename);
        if (ret != null) { return ret; }

        // some S00 cards are really part of 6ED
        String s2kAlias = ImageUtil.getSetFolder("S00");
        if (filename.startsWith(s2kAlias)) {
            ret = findFile(key, path, filename.replace(s2kAlias, ImageUtil.getSetFolder("6ED")));
            if (ret != null) { return ret; }
        }

        // try without set prefix
        String setCode;
        String setlessFilename;
        int idx = filename.indexOf('/');
        if (idx != -1) {
            setCode = filename.substring(0, idx);
            setlessFilename = filename.substring(idx + 1);
            ret = findFile(key, path, setlessFilename);
            if (ret != null) { return ret; }

            // try lowering the art index to the minimum for regular cards
            if (setlessFilename.contains(".full")) {
                ret = findFile(key, path, setlessFilename.replaceAll("[0-9]*[.]full", "1.full"));
                if (ret != null) { return ret; }
            }
        }
        else {
            setCode = null;
            setlessFilename = filename;
        }

        if (isCard) { //if image is for card, attempt to create image for it
            int artIndex = 0;
            String cardName = setlessFilename;
            idx = cardName.indexOf('.');
            if (idx != -1) {
                int dotIdx = idx;
                //trim art index
                while (idx > 0 && Character.isDigit(cardName.charAt(idx - 1))) {
                    idx--;
                }
                if (dotIdx > idx) {
                    artIndex = Integer.parseInt(cardName.substring(dotIdx, idx));
                }
                cardName = cardName.substring(0, idx);
            }
            PaperCard pc = FModel.getMagicDb().getCommonCards().getCard(cardName, setCode, artIndex);
            if (pc == null) {
                pc = FModel.getMagicDb().getVariantCards().getCard(cardName, setCode, artIndex);
            }
            if (pc != null) {
                ret = CardImageRenderer.createCardImage(Card.getCardForUi(pc));
                if (ret != null) { return ret; }
            }
        }

        System.out.println("File not found, no image created: " + key);
        return null;
    }

    private static Texture findFile(String key, String path, String filename) {
        for (String ext : _FILE_EXTENSIONS) {
            File file = new File(path, filename + ext);
            //System.out.println(String.format("Searching for %s at: %s", key, file.getAbsolutePath()));
            if (file.exists()) {
                //System.out.println(String.format("Found %s at: %s", key, file.getAbsolutePath()));
                try {
                    return new Texture(new FileHandle(file));
                }
                catch (Exception ex) {
                    Forge.log("Could not read image file " + file.getAbsolutePath() + "\n\nException:\n" + ex.toString());
                    break;
                }
            }
        }

        return null;
    }
}
