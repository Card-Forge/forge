package forge;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.google.common.cache.CacheLoader;

import forge.error.BugReporter;
import forge.properties.NewConstants;

final class ImageLoader extends CacheLoader<String, BufferedImage> {
    // image file extensions for various formats in order of likelihood
    // the last, empty, string is for keys that come in with an extension already in place
    private static final String[] _FILE_EXTENSIONS = { ".jpg", ".png", "" };
    
    @Override
    public BufferedImage load(String key) {
        final String path;
        final String filename;
        if (key.startsWith(ImageCache.TOKEN_PREFIX)) {
            filename = key.substring(ImageCache.TOKEN_PREFIX.length());
            path = NewConstants.CACHE_TOKEN_PICS_DIR;
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
        
        // try without set prefix and/or '.full' suffix
        if (null == ret && (filename.contains("/") || filename.contains(".full"))) {
            String bareFilename = filename;
            if (bareFilename.contains("/")) {
                bareFilename = filename.substring(filename.indexOf('/') + 1);
            }
            if (bareFilename.endsWith(".full")) {
                bareFilename = bareFilename.substring(0, bareFilename.length() - 5);
            }

            ret = _findFile(key, path, bareFilename);
        }
        
        if (null == ret) {
            System.out.println("File not found, no image created: " + key);
        }
        
        return ret;
    }

    public static BufferedImage getImage(final File file) {
        BufferedImage image = null;;
        try {
            image = ImageIO.read(file);
        } catch (IOException ex) {
            BugReporter.reportException(ex, "Could not read image file " + file.getAbsolutePath() + " ");
        }
        return image;
    }
    
    private static BufferedImage _findFile(String key, String path, String filename) {
        for (String ext : _FILE_EXTENSIONS) {
            File file = new File(path, filename + ext);
            //System.out.println(String.format("Searching for %s at: %s", key, file.getAbsolutePath()));
            if (file.exists()) {
                //System.out.println(String.format("Found %s at: %s", key, file.getAbsolutePath()));
                return getImage(file);
            }
        }
        
        return null;

    }
}