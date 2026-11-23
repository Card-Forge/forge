package forge.web.limited;

import forge.card.CardRules;
import forge.gamemodes.limited.CardRanker;
import forge.item.PaperCard;
import forge.web.deck.CardCatalog;
import forge.web.images.Foil;
import forge.web.protocol.PropertyJson;
import forge.web.protocol.ToBrowser.DraftCard;
import forge.web.protocol.ToBrowser.DraftLogKind;
import forge.web.protocol.ToBrowser.DraftLogLine;
import forge.web.protocol.ToBrowser.DraftState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/** The draft as the browser sees it. A step number identifies the pack in hand, so each pick names the exact pack it was made from. */
final class DraftView {
    private int step;
    private volatile DraftState latest;
    private final List<DraftLogLine> log = new ArrayList<>();

    /** Packs go to the next seat (1) in odd rounds and to the previous seat (-1) in even rounds, as in BoosterDraft.passPacks. round counts from 1, and 0 means before the first round. */
    static int passDirection(final int round) {
        return round % 2 == 0 && round > 0 ? -1 : 1;
    }

    /** The seat whose pack was passed on. It is empty when the counts show anything other than one pass, so the dial is redrawn instead of animated. */
    static List<Integer> moved(final int[] before, final int[] after, final int pickingSeat, final int direction) {
        if (before == null || after == null || before.length != after.length || pickingSeat < 0 || pickingSeat >= after.length) {
            return List.of();
        }
        final int next = Math.floorMod(pickingSeat + direction, after.length);
        for (int i = 0; i < after.length; i++) {
            final int expected = i == pickingSeat ? -1 : i == next ? 1 : 0;
            if (after[i] - before[i] != expected) {
                return List.of();
            }
        }
        return List.of(pickingSeat);
    }

    static DraftCard moved(final DraftCard c, final boolean sideboard) {
        return new DraftCard(c.name(), c.image(), c.cost(), c.mv(), c.colors(), c.type(), c.text(), c.pt(), c.rarity(), c.rank(), c.pack(),
                c.pick(), sideboard);
    }

    static DraftCard card(final PaperCard card, final int packNumber, final int pickNumber, final boolean sideboard) {
        final CardRules rules = card.getRules();
        // A ranked card scores at least 1. Desktop's rounding can show 0
        final double score = CardRanker.getRawScore(card);
        return new DraftCard(card.getName(), Foil.key(card), PropertyJson.manaCost(rules.getManaCost()),
                rules.getManaCost().getCMC(), CardCatalog.letters(rules.getColor()), rules.getType().toString(),
                rules.getOracleText().replace("\\n", "\n").replace("\r\n", "\n"), CardCatalog.pt(rules),
                card.getRarity().toString(), score <= 0 ? null : (int) Math.max(1, Math.round(Math.min(99, score))), packNumber, pickNumber, sideboard);
    }

    /** Builds and stores the next state. Its step goes up only when newPack says the pack in hand changed. */
    DraftState state(final boolean newPack, final IntFunction<DraftState> build) {
        if (newPack) {
            step++;
        }
        latest = build.apply(step);
        return latest;
    }

    /** Adds a line to the draft's log, worded as in desktop's draft log. */
    void log(final DraftLogKind kind, final String line) {
        log.add(new DraftLogLine(kind, line));
    }

    List<DraftLogLine> lines() {
        return List.copyOf(log);
    }

    /** The state last built, to send again to a browser that reconnects. Null before the first pack. */
    DraftState latest() {
        return latest;
    }
}
