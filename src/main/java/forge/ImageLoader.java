package forge;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;

import com.google.common.cache.CacheLoader;

import forge.error.BugReporter;
import forge.properties.NewConstants;

final class ImageLoader extends CacheLoader<String, BufferedImage> {
    // image file extensions for various formats in order of likelihood
    // the last, empty, string is for keys that come in with an extension already in place
    private static final String[] _FILE_EXTENSIONS = { ".jpg", ".png", "" };
    
    @Override
    public BufferedImage load(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        
        final String path;
        final String filename;
        if (key.startsWith(ImageCache.TOKEN_PREFIX)) {
            filename = key.substring(ImageCache.TOKEN_PREFIX.length());
            path = NewConstants.CACHE_TOKEN_PICS_DIR;
        } else if (key.startsWith(ImageCache.ICON_PREFIX)) {
            filename = key.substring(ImageCache.ICON_PREFIX.length());
            path = NewConstants.CACHE_ICON_PICS_DIR;
        } else if (key.startsWith(ImageCache.BOOSTER_PREFIX)) {
            filename = key.substring(ImageCache.BOOSTER_PREFIX.length());
            path = NewConstants.CACHE_BOOSTER_PICS_DIR;
        } else if (key.startsWith(ImageCache.FATPACK_PREFIX)) {
            filename = key.substring(ImageCache.FATPACK_PREFIX.length());
            path = NewConstants.CACHE_FATPACK_PICS_DIR;
        } else if (key.startsWith(ImageCache.PRECON_PREFIX)) {
            filename = key.substring(ImageCache.PRECON_PREFIX.length());
            path = NewConstants.CACHE_PRECON_PICS_DIR;
        } else if (key.startsWith(ImageCache.TOURNAMENTPACK_PREFIX)) {
            filename = key.substring(ImageCache.TOURNAMENTPACK_PREFIX.length());
            path = NewConstants.CACHE_TOURNAMENTPACK_PICS_DIR;
        } else {
            filename = key;
            path = NewConstants.CACHE_CARD_PICS_DIR;
        }

        BufferedImage ret = _findFile(key, path, filename);
        
        // some S00 cards are really part of 6ED
        if (null == ret ) {
            String s2kAlias = ImageCache.getSetFolder("S00");
            if ( filename.startsWith(s2kAlias) ) {
                ret = _findFile(key, path, filename.replace(s2kAlias, ImageCache.getSetFolder("6ED")));
            }
        }

        // try without set prefix
        String setlessFilename = null;
        if (null == ret && filename.contains("/")) {
            setlessFilename = filename.substring(filename.indexOf('/') + 1);
            ret = _findFile(key, path, setlessFilename);
        }
        
        // try lowering the art index to the minimum for regular cards
        if (null == ret && null != setlessFilename && setlessFilename.contains(".full")) {
            ret = _findFile(key, path, setlessFilename.replaceAll("[0-9]*[.]full", "1.full"));
        }
        
        if (null == ret) {
            System.out.println("File not found, no image created: " + key);
        }
        
        return ret;
    }

    private static BufferedImage _findFile(String key, String path, String filename) {
        for (String ext : _FILE_EXTENSIONS) {
            File file = new File(path, filename + ext);
            //System.out.println(String.format("Searching for %s at: %s", key, file.getAbsolutePath()));
            if (file.exists()) {
                //System.out.println(String.format("Found %s at: %s", key, file.getAbsolutePath()));
                try {
                    return ImageIO.read(file);
                } catch (IOException ex) {
                    BugReporter.reportException(ex, "Could not read image file " + file.getAbsolutePath() + " ");
                    break;
                }
            }
        }
        
        return null;
    }
}
