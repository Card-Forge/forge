package forge.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;

public class Localizer {

    private static Localizer instance;

    private List<LocalizationChangeObserver> observers = new ArrayList<>();

    private Locale locale;
    private ResourceBundle resourceBundle;
    private ResourceBundle englishBundle;
    private ResourceBundle adventureBundle;
    private String currentLanguageRegionID;
    private boolean silent = false;
    private boolean english = false;
    //Keys already reported, so a missing translation is logged once instead of on every lookup
    private final Set<String> reportedKeys = Collections.synchronizedSet(new HashSet<>());

    public static Localizer getInstance() {
        if (instance == null) {
            synchronized (Localizer.class) {
                instance = new Localizer();
            }
        }
        return instance;
    }

    public void setEnglish(boolean value) {
        english = value;
    }

    private Localizer() {
    }

    public void initialize(String localeID, String languagesDirectory) {
        setLanguage(localeID, languagesDirectory);
    }

    public String convert(String value, String fromEncoding, String toEncoding) throws UnsupportedEncodingException {
        return new String(value.getBytes(fromEncoding), toEncoding);
    }

    public String charset(String value, String charsets[]) {
        String probe = StandardCharsets.UTF_8.name();
        for(String c : charsets) {
            Charset charset = Charset.forName(c);
            if(charset != null) {
                try {
                    if (value.equals(convert(convert(value, charset.name(), probe), probe, charset.name()))) {
                        return c;
                    }
                } catch(UnsupportedEncodingException ignored) {}
            }
        }
        return StandardCharsets.UTF_8.name();
    }

    public String getMessageorUseDefault(final String key, final String defaultValue, final Object... messageArguments) {
        try {
            silent = true;
            String value = getMessage(key, messageArguments);
            if (value.contains("INVALID PROPERTY:"))
                return defaultValue;
            return value;
        } catch (Exception e) {
            return defaultValue;
        }
    }
    public String getEnglishMessage(final String key, final Object... messageArguments) {
        return getMessage(true, key, messageArguments);
    }
    //FIXME: localizer should return default value from english locale or it will crash some GUI element like the NewGameMenu->NewGameScreen Popup when returned null...
    public String getMessage(final String key, final Object... messageArguments) {
        return getMessage(false, key, messageArguments);
    }
    public String getMessage(boolean forcedEnglish, final String key, final Object... messageArguments) {
        //The flag covers one lookup. Clear it here, or an early return below leaves it set
        //and silences every later caller.
        final boolean quiet = silent;
        silent = false;

        MessageFormat formatter = null;
        String rawValue = null;

        try {
            //formatter = new MessageFormat(resourceBundle.getString(key.toLowerCase()), locale);
            rawValue = lookup(key, english || forcedEnglish);
            formatter = new MessageFormat(rawValue, english || forcedEnglish ? Locale.ENGLISH : locale);
        } catch (final MissingResourceException e) {
            warnOnce(quiet, key, "is not translated in " + locale + ", using English");
        } catch (final IllegalArgumentException e) {
            warnOnce(quiet, key, "is not a valid message pattern in " + locale + ", using English: " + e.getMessage());
        }

        if (formatter == null) {
            if (english || forcedEnglish) {
                return "INVALID PROPERTY: '" + key + "' -- Translation missing from English?";
            }
            try {
                rawValue = englishBundle.getString(key);
                formatter = new MessageFormat(rawValue, Locale.ENGLISH);
                forcedEnglish = true;
            } catch (final IllegalArgumentException | MissingResourceException e) {
                warnOnce(quiet, key, "is missing from en-US as well: " + e.getMessage());
                return "INVALID PROPERTY: '" + key + "' -- Translation missing from English locale?";
            }
        }

        formatter.setLocale(english || forcedEnglish ? Locale.ENGLISH : locale);

        String formattedMessage = "CHAR ENCODING ERROR";
        final String[] charsets = { "ISO-8859-1", "UTF-8" };
        //Support non-English-standard characters
        String detectedCharset = charset(rawValue, charsets);

        final int argLength = messageArguments.length;
        Object[] syncEncodingMessageArguments = new Object[argLength];
        //when messageArguments encoding not equal resourceBundle.getString(key),convert to equal
        //avoid convert to a have two encoding content formattedMessage string.
        for (int i = 0; i < argLength; i++) {
            String objCharset = charset(messageArguments[i].toString(), charsets);
            try {
                syncEncodingMessageArguments[i] = convert(messageArguments[i].toString(), objCharset, detectedCharset);
            } catch (UnsupportedEncodingException ignored) {
                System.err.println("Cannot Convert '" + messageArguments[i].toString() + "' from '" + objCharset + "' To '" + detectedCharset + "'");
                return "encoding '" + key + "' translate string failure";
            }
        }

        try {
            formattedMessage = new String(formatter.format(syncEncodingMessageArguments).getBytes(detectedCharset), StandardCharsets.UTF_8);
        } catch(UnsupportedEncodingException ignored) {}

        return formattedMessage;
    }

    /** Report a bad key once per language. The English fallback keeps the text on screen either way. */
    private void warnOnce(final boolean quiet, final String key, final String problem) {
        if (quiet || !reportedKeys.add(locale + "/" + key + "/" + problem)) {
            return;
        }
        System.err.println("Localization: '" + key + "' " + problem);
    }

    public void setLanguage(final String languageRegionID, final String languagesDirectory) {
        String[] splitLocale = languageRegionID.split("-");

        Locale oldLocale = locale;
        locale = new Locale(splitLocale[0], splitLocale[1]);

        //Don't reload the language if nothing changed
        if (oldLocale == null || !oldLocale.equals(locale)) {
            File file = new File(languagesDirectory);
            URL[] urls = null;

            try {
                urls = new URL[] { file.toURI().toURL() };
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            ClassLoader loader = new URLClassLoader(urls);
            currentLanguageRegionID = languageRegionID;

            try {
                englishBundle = ResourceBundle.getBundle("en-US", new Locale("en", "US"), loader);
                resourceBundle = ResourceBundle.getBundle(languageRegionID, new Locale(splitLocale[0], splitLocale[1]), loader);
            } catch (NullPointerException | MissingResourceException e) {
                //If the language can't be loaded, default to US English
                resourceBundle = englishBundle;
                e.printStackTrace();
            }

            adventureBundle = null;
            reportedKeys.clear();

            System.out.println("Language '" + resourceBundle.getBaseBundleName() + "' loaded successfully.");

            notifyObservers();
        }
    }

    public void loadAdventureBundle(final String languagesDirectory) {
        if (currentLanguageRegionID == null || languagesDirectory == null || languagesDirectory.isEmpty()) {
            adventureBundle = null;
            return;
        }
        try {
            URL[] urls = { new File(languagesDirectory).toURI().toURL() };
            ClassLoader adventureLoader = new URLClassLoader(urls);
            adventureBundle = ResourceBundle.getBundle("adventure-" + currentLanguageRegionID, locale, adventureLoader);
        } catch (final MalformedURLException | NullPointerException | MissingResourceException e) {
            adventureBundle = null;
        }
    }

    private String lookup(final String key, final boolean forceEnglish) {
        if (!forceEnglish && adventureBundle != null) {
            try {
                return adventureBundle.getString(key);
            } catch (final MissingResourceException ignored) {}
        }
        return (forceEnglish ? englishBundle : resourceBundle).getString(key);
    }

    public void registerObserver(LocalizationChangeObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers() {
        for (LocalizationChangeObserver observer : observers) {
            observer.localizationChanged();
        }
    }

}
