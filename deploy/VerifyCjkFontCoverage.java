import java.awt.Font;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Release-time audit for the bundled Android CJK font.
 *
 * Run from the repository root with:
 *   java deploy/VerifyCjkFontCoverage.java
 */
public final class VerifyCjkFontCoverage {
    private static final Path FONT = Path.of(
            "forge-gui-android", "assets", "bundled-font", "SourceHanSansCN.ttf");
    private static final List<Path> REQUIRED_TEXT = List.of(
            Path.of("forge-gui", "res", "languages", "cardnames-zh-CN.txt"),
            Path.of("forge-gui", "res", "languages", "zh-CN.properties"),
            Path.of("forge-gui", "src", "main", "resources", "forge-community-release-notes-zh-CN.txt"));
    private static final Set<Integer> RUNTIME_NORMALIZED = Set.of(
            0x0160, 0x02E3, 0x2075, 0x2610, 0xA789, 0x2B689, 0x2B812);

    private VerifyCjkFontCoverage() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        Font font = Font.createFont(Font.TRUETYPE_FONT, root.resolve(FONT).toFile());
        Set<Integer> required = new TreeSet<>();
        for (Path relativePath : REQUIRED_TEXT) {
            Path file = root.resolve(relativePath);
            if (!Files.isRegularFile(file)) {
                throw new IOException("Required character source is missing: " + file);
            }
            Files.readString(file, StandardCharsets.UTF_8).codePoints()
                    .filter(VerifyCjkFontCoverage::isVisibleCodePoint)
                    .forEach(required::add);
        }
        required.add(0x25A1); // runtime replacement for U+2610 BALLOT BOX

        List<Integer> missing = new ArrayList<>();
        for (int codePoint : required) {
            // libGDX BitmapFont uses UTF-16 chars internally and cannot represent supplementary
            // planes, so fail the release audit for those even if the TTF has a glyph.
            if (!RUNTIME_NORMALIZED.contains(codePoint)
                    && (codePoint > Character.MAX_VALUE || !font.canDisplay(codePoint))) {
                missing.add(codePoint);
            }
        }

        System.out.printf("CJK coverage: %,d required code points, %,d unsupported.%n",
                required.size(), missing.size());
        if (!missing.isEmpty()) {
            for (int codePoint : missing) {
                System.err.printf("U+%04X %s%n", codePoint,
                        new String(Character.toChars(codePoint)));
            }
            System.exit(1);
        }
    }

    private static boolean isVisibleCodePoint(int codePoint) {
        return !Character.isISOControl(codePoint)
                && !Character.isWhitespace(codePoint)
                && codePoint != 0xFEFF;
    }
}
