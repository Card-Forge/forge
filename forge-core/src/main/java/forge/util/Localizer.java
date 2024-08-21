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
    private boolean silent = false;
    private boolean english = false;

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
        MessageFormat formatter = null;

        try {
            //formatter = new MessageFormat(resourceBundle.getString(key.toLowerCase()), locale);
            formatter = new MessageFormat(english || forcedEnglish ? englishBundle.getString(key) : resourceBundle.getString(key), english || forcedEnglish ? Locale.ENGLISH : locale);
        } catch (final IllegalArgumentException | MissingResourceException e) {
            if (!silent)
                e.printStackTrace();
        }

        if (formatter == null) {
            if (!silent) {
                System.err.println("INVALID PROPERTY: '" + key + "' -- Translation missing from " + locale);
            }

            if (english || forcedEnglish) {
                return "INVALID PROPERTY: '" + key + "' -- Translation missing from English?";
            }
            try {
                formatter = new MessageFormat(englishBundle.getString(key), Locale.ENGLISH);
                forcedEnglish = true;
            } catch (final IllegalArgumentException | MissingResourceException e) {
                if (!silent) {
                    e.printStackTrace();
                }
                return "INVALID PROPERTY: '" + key + "' -- Translation missing from English locale?";
            }
        }

        silent = false;

        formatter.setLocale(english || forcedEnglish ? Locale.ENGLISH : locale);

        String formattedMessage = "CHAR ENCODING ERROR";
        final String[] charsets = { "ISO-8859-1", "UTF-8" };
        //Support non-English-standard characters
        String detectedCharset = charset(english || forcedEnglish ? englishBundle.getString(key) : resourceBundle.getString(key), charsets);

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

            try {
                resourceBundle = ResourceBundle.getBundle(languageRegionID, new Locale(splitLocale[0], splitLocale[1]), loader);
                englishBundle = ResourceBundle.getBundle("en-US", new Locale("en", "US"), loader);
            } catch (NullPointerException | MissingResourceException e) {
                //If the language can't be loaded, default to US English
                resourceBundle = ResourceBundle.getBundle("en-US", new Locale("en_US"), loader);
                e.printStackTrace();
            }

            System.out.println("Language '" + resourceBundle.getBaseBundleName() + "' loaded successfully.");

            notifyObservers();

        }

    }

    public List<Language> getLanguages() {
        //TODO List all languages by getting their files
        return null;
    }

    public void registerObserver(LocalizationChangeObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers() {
        for (LocalizationChangeObserver observer : observers) {
            observer.localizationChanged();
        }
    }

    public static class Language {
        public String languageName;
        public String languageID;
    }

}
