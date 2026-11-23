package forge.web.protocol;

import com.google.gson.JsonObject;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.IHasForgeLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** The page's text in Forge's language. It is sent as patterns that the page formats itself (text.ts), by the rules of {@link forge.util.Localizer}. */
public final class PageText implements IHasForgeLog {
    /** The keys the page uses. {@link PageTextKeys} writes this list when it reads the keys from the TypeScript. */
    static final String KEYS_RESOURCE = "/forge/web/page-text.keys";

    private static byte[] json;

    private PageText() {
    }

    /** The page's text in the form the page asks for: {lang, text: {key: pattern}}. It is built once, on first use. */
    public static synchronized byte[] json() {
        if (json == null) {
            final String lang = FModel.getPreferences().getPref(FPref.UI_LANGUAGE);
            final Properties english = load("en-US");
            final Properties chosen = "en-US".equals(lang) ? english : load(lang);
            final JsonObject text = new JsonObject();
            for (final String key : keys(english)) {
                final String pattern = chosen.getProperty(key, english.getProperty(key));
                if (pattern != null) {
                    text.addProperty(key, pattern);
                }
            }
            final JsonObject out = new JsonObject();
            out.addProperty("lang", lang);
            out.add("text", text);
            json = MessageJson.WRITER.toJson(out).getBytes(StandardCharsets.UTF_8);
        }
        return json;
    }

    /** The keys the page uses. If the list was never written, as when run without Maven, it is every English key. */
    private static List<String> keys(final Properties english) {
        try (InputStream in = PageText.class.getResourceAsStream(KEYS_RESOURCE)) {
            if (in != null) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().filter(k -> !k.isBlank()).toList();
            }
        } catch (final IOException e) {
            netLog.warn(e, "Could not read the page's keys");
        }
        return new ArrayList<>(english.stringPropertyNames());
    }

    static Properties load(final String lang) {
        final Properties p = new Properties();
        final Path file = Path.of(ForgeConstants.LANG_DIR, lang + ".properties");
        try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(in);
        } catch (final IOException e) {
            netLog.warn(e, "Could not read the language file " + file);
        }
        return p;
    }
}
