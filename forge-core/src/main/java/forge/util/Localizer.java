package forge.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.*;

public class Localizer {
	
	private static Localizer instance;

	private List<LocalizationChangeObserver> observers = new ArrayList<>();
	
	private Locale locale;
	private ResourceBundle resourceBundle;

	public static Localizer getInstance() {
		if (instance == null) {
			synchronized (Localizer.class) {
				instance = new Localizer();
			}
		}
		return instance;
	}
	
	private Localizer() {
	}
	
	public void initialize(String localeID, String languagesDirectory) {
		setLanguage(localeID, languagesDirectory);
	}

	public String getMessage(final String key, final Object... messageArguments) {
		MessageFormat formatter = null;
		
		try {
			formatter = new MessageFormat(resourceBundle.getString(key.toLowerCase()), locale);
		} catch (final IllegalArgumentException | MissingResourceException e) {
			e.printStackTrace();
		}
		
		if (formatter == null) {
			System.err.println("INVALID PROPERTY: '" + key + "' -- Translation Needed?");
			return "INVALID PROPERTY: '" + key + "' -- Translation Needed?";
		}
		
		formatter.setLocale(locale);
		
		String formattedMessage = "CHAR ENCODING ERROR";
		try {
			//Support non-English-standard characters
			formattedMessage = new String(formatter.format(messageArguments).getBytes("ISO-8859-1"), "UTF-8");
		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
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
			} catch (NullPointerException | MissingResourceException e) {
				//If the language can't be loaded, default to US English
				resourceBundle = ResourceBundle.getBundle("en-US", new Locale("en", "US"), loader);
				e.printStackTrace();
			}

			System.out.println("Language '" + resourceBundle.toString() + "' loaded successfully.");
			
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
		public String langaugeID; 
	} 
	
}
