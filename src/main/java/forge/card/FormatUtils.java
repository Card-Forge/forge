package forge.card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.slightlymagic.maxmtg.Predicate;

import org.apache.commons.lang3.StringUtils;

import forge.Singletons;
import forge.game.GameFormat;
import forge.util.FileUtil;

public final class FormatUtils {

    private final Map<String, GameFormat> formats = new TreeMap<String, GameFormat>(String.CASE_INSENSITIVE_ORDER);

    /**
     * Gets the standard.
     * 
     * @return the standard
     */
    public GameFormat getStandard() {
        return formats.get("Standard");
    }

    /**
     * Gets the extended.
     * 
     * @return the extended
     */
    public GameFormat getExtended() {
        return formats.get("Extended");
    }

    /**
     * Gets the modern.
     * 
     * @return the modern
     */
    public GameFormat getModern() {
        return formats.get("Modern");
    }

    // list are immutable, no worries
    /**
     * Gets the formats.
     * 
     * @return the formats
     */
    public Collection<GameFormat> getFormats() {
        return formats.values();
    }

    public FormatUtils() {
        final List<String> fData = FileUtil.readFile("res/blockdata/formats.txt");

        for (final String s : fData) {
            if (StringUtils.isBlank(s)) {
                continue;
            }

            String name = null;
            final List<String> sets = new ArrayList<String>(); // default: all
                                                               // sets
            // allowed
            final List<String> bannedCards = new ArrayList<String>(); // default:
            // nothing
            // banned

            final String[] sParts = s.trim().split("\\|");
            for (final String sPart : sParts) {
                final String[] kv = sPart.split(":", 2);
                final String key = kv[0].toLowerCase();
                if ("name".equals(key)) {
                    name = kv[1];
                } else if ("sets".equals(key)) {
                    sets.addAll(Arrays.asList(kv[1].split(", ")));
                } else if ("banned".equals(key)) {
                    bannedCards.addAll(Arrays.asList(kv[1].split("; ")));
                }
            }
            if (name == null) {
                throw new RuntimeException("Format must have a name! Check formats.txt file");
            }
            final GameFormat thisFormat = new GameFormat(name, sets, bannedCards);

            formats.put(name, thisFormat);
        }
    }

    public abstract static class Predicates {

        public static final Predicate<CardEdition> isLegalInFormat(final GameFormat format) {
            return new LegalInFormat(format);
        }

        private static class LegalInFormat extends Predicate<CardEdition> {
            private final GameFormat format;

            public LegalInFormat(final GameFormat fmt) {
                this.format = fmt;
            }

            @Override
            public boolean isTrue(final CardEdition subject) {
                return this.format.isSetLegal(subject.getCode());
            }
        }

        /** The Constant setsInT2. */
        public static final Predicate<CardEdition> SETS_IN_STANDARD = Predicates
                .isLegalInFormat(Singletons.getModel().getFormats().getStandard());

        /** The Constant setsInExt. */
        public static final Predicate<CardEdition> SETS_IN_EXT = Predicates.isLegalInFormat(Singletons.getModel().getFormats().getExtended());

        /** The Constant setsInModern. */
        public static final Predicate<CardEdition> SET_IN_MODERN = Predicates.isLegalInFormat(Singletons.getModel().getFormats().getModern());

    }
}

/** 
 * TODO: Write javadoc for this type.
 *
 */
