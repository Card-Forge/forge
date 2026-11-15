package forge.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/** The page's files, read from the jar or from a folder, and the page itself with its stylesheets joined and its files versioned. */
final class PageBundle {
    /** A stylesheet or script the page names. page() adds a hash of the file's contents to its address. */
    private static final Pattern PAGE_FILE = Pattern.compile("(href|src)=\"((?:css|js)/[^\"?]+)\"");
    /** A stylesheet link in the page. The page's stylesheets are served joined into one file, STYLES. */
    private static final Pattern STYLESHEET = Pattern.compile("<link rel=\"stylesheet\" href=\"(css/[^\"?]+)\">\\R?");
    static final String STYLES = "css/page.css";
    /** A chunk the page's script imports. */
    private static final Pattern CHUNK = Pattern.compile("\\./(chunks/[^\"']+\\.js)");
    // -Dforge.web.pageDir=<the web resource folder> serves the page from disk, so an edit needs only a reload
    private static final String PAGE_DIR = System.getProperty("forge.web.pageDir");
    /** The page as served. It is built once, unless the page is read from a folder that may change. */
    private volatile byte[] page;
    private volatile byte[] styles;

    /**
     * The page, with a hash of each stylesheet's and script's contents in its address, so the browser keeps them until they change.
     * Its stylesheets are joined into one file, and its script's chunks are preloaded so the browser asks for them all at once.
     * Returns null when there is no index.html.
     */
    byte[] page() throws IOException {
        final byte[] built = page;
        if (built != null && PAGE_DIR == null) {
            return built;
        }
        final byte[] html = readResource("index.html");
        if (html == null) {
            return null;
        }
        // Joined in the page's order, so the cascade is unchanged and relative addresses inside css/ still work
        final ByteArrayOutputStream joined = new ByteArrayOutputStream();
        final StringBuilder linked = new StringBuilder();
        final Matcher sheet = STYLESHEET.matcher(new String(html, StandardCharsets.UTF_8));
        boolean first = true;
        while (sheet.find()) {
            final byte[] css = readResource(sheet.group(1));
            if (css != null) {
                joined.write(css);
                joined.write('\n');
            }
            sheet.appendReplacement(linked, first ? Matcher.quoteReplacement("<link rel=\"stylesheet\" href=\"" + STYLES + "\">\n") : "");
            first = false;
        }
        sheet.appendTail(linked);
        styles = joined.toByteArray();
        final StringBuilder out = new StringBuilder();
        final Matcher file = PAGE_FILE.matcher(linked);
        while (file.find()) {
            final byte[] named = STYLES.equals(file.group(2)) ? styles : readResource(file.group(2));
            final CRC32 crc = new CRC32();
            if (named != null) {
                crc.update(named);
            }
            file.appendReplacement(out, Matcher.quoteReplacement(named == null ? file.group()
                    : file.group(1) + "=\"" + file.group(2) + "?v=" + Long.toHexString(crc.getValue()) + "\""));
        }
        file.appendTail(out);
        final byte[] script = readResource("js/app.js");
        final StringBuilder preloads = new StringBuilder();
        final Matcher chunk = CHUNK.matcher(script == null ? "" : new String(script, StandardCharsets.UTF_8));
        while (chunk.find()) {
            preloads.append("<link rel=\"modulepreload\" href=\"js/").append(chunk.group(1)).append("\">\n");
        }
        final int head = out.indexOf("</head>");
        if (head >= 0) {
            out.insert(head, preloads);
        }
        return page = out.toString().getBytes(StandardCharsets.UTF_8);
    }

    byte[] styles() throws IOException {
        return page() == null ? null : styles;
    }

    static byte[] readResource(final String resource) throws IOException {
        if (PAGE_DIR != null) {
            final Path root = Path.of(PAGE_DIR).toAbsolutePath().normalize();
            final Path file = root.resolve(resource).normalize();
            return file.startsWith(root) && Files.isRegularFile(file) ? Files.readAllBytes(file) : null;
        }
        try (InputStream in = PageBundle.class.getResourceAsStream("/web/" + resource)) {
            return in == null ? null : in.readAllBytes();
        }
    }
}
