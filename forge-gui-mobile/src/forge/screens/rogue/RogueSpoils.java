package forge.screens.rogue;

import com.badlogic.gdx.utils.Align;
import forge.Graphics;
import forge.assets.FSkin;
import forge.assets.FSkinImageInterface;
import forge.localinstance.skin.FSkinProp;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * What a won or lost battle gave, gathered on the flow thread while its rewards are worked out. The reward screens list
 * it as a ledger, each entry under the title desktop gives it, in place of a message for each.
 */
public final class RogueSpoils {
    public record Spoil(String text, String title, FSkinProp icon) { }

    private static final List<Spoil> spoils = new CopyOnWriteArrayList<>();
    private static volatile boolean seen;

    private RogueSpoils() {
    }

    /** Forgets the last battle's spoils. */
    public static void start() {
        spoils.clear();
        seen = false;
    }

    public static void add(String text, String title, FSkinProp icon) {
        spoils.add(new Spoil(text, title, icon));
    }

    /** The spoils so far, for a screen that shows them; they then count as seen. */
    static List<Spoil> take() {
        seen = true;
        return List.copyOf(spoils);
    }

    /** Whether some spoils have not been on any screen yet. */
    public static boolean unseen() {
        return !seen && !spoils.isEmpty();
    }

    /** The icon of the spoil whose coins fly to a run bar count: life, gold, Echoes; credits have none. */
    static FSkinProp iconFor(int count) {
        return switch (count) {
            case 0 -> FSkinProp.ICO_QUEST_CHARM;
            case 1 -> FSkinProp.ICO_QUEST_COIN;
            case 2 -> FSkinProp.ICO_QUEST_GOLD;
            default -> null;
        };
    }

    /** The height of the ledger in cols columns across w. */
    static float ledgerHeight(List<Spoil> list, float w, int cols) {
        return ledger(null, list, 0, 0, w, cols, null);
    }

    /**
     * Draws the ledger in cols columns across w and returns its height. Measures only when g is null. Where each icon
     * was drawn goes into where, by icon, when it is given.
     */
    static float ledger(Graphics g, List<Spoil> list, float x, float y, float w, int cols, Map<FSkinProp, float[]> where) {
        float u = RogueLook.unit();
        float gap = 10 * u;
        float colWidth = (w - gap * (cols - 1)) / cols;
        float textWidth = colWidth - 19 * u;
        float rowY = y;
        for (int start = 0; start < list.size(); start += cols) {
            float rowHeight = 0;
            for (int i = start; i < Math.min(list.size(), start + cols); i++) {
                Spoil spoil = list.get(i);
                float ex = x + (i - start) * (colWidth + gap);
                float h = 11 * u + RogueLook.wrappedHeight(spoil.text(), textWidth, 0.85f);
                rowHeight = Math.max(rowHeight, h);
                if (g == null) {
                    continue;
                }
                FSkinImageInterface icon = spoil.icon() == null ? null : FSkin.getImages().get(spoil.icon());
                if (icon != null) {
                    g.drawImage(icon, ex, rowY + 2 * u, 15 * u, 15 * u);
                    if (where != null) {
                        where.put(spoil.icon(), new float[]{ex, rowY + 2 * u, 15 * u, 15 * u});
                    }
                }
                RogueLook.fit(g, spoil.title().toUpperCase(), ex + 19 * u, rowY, 0.7f, RogueLook.DEEP, textWidth, Align.left);
                boolean lost = spoil.icon() == FSkinProp.ICO_QUEST_MINUS;
                RogueLook.wrap(g, spoil.text(), ex + 19 * u, rowY + 10 * u, textWidth, 0.85f, lost ? RogueLook.SOFT : RogueLook.INK, Align.left);
            }
            rowY += rowHeight + 6 * u;
        }
        return list.isEmpty() ? 0 : rowY - 6 * u - y;
    }

    /**
     * Upright: a parchment banner with the title and a line of guidance, both centred, then the ledger in its
     * own gilded panel under it. Returns the height of both; measures only when g is null.
     */
    static float uprightHeader(Graphics g, float w, String title, String line, List<Spoil> list, Map<FSkinProp, float[]> where) {
        float u = RogueLook.unit();
        float banner = line.isEmpty() ? 30 * u : 44 * u;
        float h = banner;
        if (!list.isEmpty()) {
            h += 2 * u + ledgerHeight(list, w - 36 * u, 2) + 28 * u;
        }
        if (g == null) {
            return h;
        }
        RogueLook.paper(g, 3 * u, 3 * u, w - 6 * u, banner);
        RogueLook.fit(g, title, 10 * u, 8 * u, 1.35f, RogueLook.INK, w - 20 * u, Align.center);
        if (!line.isEmpty()) {
            RogueLook.fit(g, line, 10 * u, 26 * u, 1, RogueLook.SOFT, w - 20 * u, Align.center);
        }
        if (!list.isEmpty()) {
            float py = 3 * u + banner + 2 * u;
            RogueLook.gilded(g, 3 * u, py, w - 6 * u, h - banner - 2 * u);
            ledger(g, list, 18 * u, py + 13 * u, w - 36 * u, 2, where);
        }
        return h;
    }

    /**
     * Sideways: a gilded panel down the left with the title, a note, a gold rule and the ledger. Without a title the
     * ledger starts at the top. The run bar and the buttons go at its foot. Returns where the ledger ends.
     */
    static float sidePanel(Graphics g, float x, float y, float w, float h, String title, String note, List<Spoil> list,
                           Map<FSkinProp, float[]> where) {
        float u = RogueLook.unit();
        RogueLook.gilded(g, x, y, w, h);
        float ix = x + 13 * u;
        float iw = w - 26 * u;
        float ty = y + 13 * u;
        if (title == null) {
            // The menu button sits in the panel's top right corner, beside the first line of the ledger
            return ty + ledger(g, list, ix, ty, iw - 28 * u, 1, where);
        }
        // The menu button sits in the panel's top right corner, beside the title and the note
        ty += RogueLook.wrap(g, title, ix, ty, iw - 28 * u, 1.2f, RogueLook.INK, Align.left);
        if (note != null && !note.isEmpty()) {
            ty += RogueLook.wrap(g, note, ix, ty, iw - 28 * u, 0.9f, RogueLook.SOFT, Align.left);
        }
        if (list.isEmpty()) {
            return ty;
        }
        ty += 3 * u;
        RogueLook.line(g, ix, ty, ix + iw, ty, 1.2f, RogueLook.RULE);
        ty += 6 * u;
        return ty + ledger(g, list, ix, ty, iw, 1, where);
    }
}
