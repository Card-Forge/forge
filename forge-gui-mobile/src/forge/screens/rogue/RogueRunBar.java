package forge.screens.rogue;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinImage;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RogueRun;
import forge.toolbox.FContainer;
import java.lang.ref.WeakReference;
import java.util.function.Supplier;

/**
 * The commander, life, gold, Echoes and removal credits, with the run menu button. A count that changed since any run
 * bar last showed it ticks to its new value as coins fly in from the middle of the page, or out when it fell. Only
 * the current screen's bar follows the changes, so a screen moving away does not take the coins with it.
 */
public class RogueRunBar extends FContainer {
    private static final int LIFE = 0;
    private static final int COUNTS = 4;
    /** Compact, where each count starts across the width; life takes the most room, as it shows its maximum too. */
    private static final float[] COMPACT_SLOTS = {0, 0.36f, 0.62f, 0.84f};
    private static WeakReference<RogueRun> shownRun = new WeakReference<>(null);
    private static final int[] now = new int[COUNTS];
    private static final float[] shown = new float[COUNTS];
    private static int counting;

    private final RogueScreen screen;
    private final Supplier<RogueRun> run;
    private final RogueButton menu;
    private final float[] iconX = new float[COUNTS];
    private float iconY;
    private boolean compact;

    RogueRunBar(RogueScreen screen, Supplier<RogueRun> run, Runnable openMenu) {
        this.screen = screen;
        this.run = run;
        this.menu = add(new RogueButton("", "Menu", RogueButton.State.NORMAL, openMenu));
    }

    /** Compact: no avatar and no menu button, the counts spread evenly under the name, for a narrow panel. */
    void setCompact(boolean compact) {
        this.compact = compact;
        menu.setVisible(!compact);
    }

    @Override
    protected void doLayout(float width, float height) {
        float u = RogueLook.unit();
        menu.setBounds(width - 30 * u, 10 * u, 24 * u, 25 * u);
    }

    private static int[] counts(RogueRun r) {
        now[LIFE] = r.getCurrentLife();
        now[1] = r.getCurrentGold();
        now[2] = RogueMetaProgress.getInstance().getTotalEchoes();
        now[3] = r.getRemovalCredits();
        return now;
    }

    private static void icon(Graphics g, int i, float x, float y, float size) {
        switch (i) {
            case LIFE -> RogueLook.item(g, "Life", x, y, size);
            case 1 -> RogueLook.skin(g, FSkinImage.QUEST_COIN, x, y, size);
            case 2 -> RogueLook.skin(g, FSkinImage.QUEST_GOLD, x, y, size);
            default -> RogueLook.skin(g, FSkinImage.CARD_IMAGE, x, y, size);
        }
    }

    /**
     * For a screen that shows the counts without a run bar, as the map does sideways: they count as shown, so the next
     * run bar ticks only what changes after.
     */
    static void shownElsewhere(RogueRun r) {
        shownRun = new WeakReference<>(r);
        int[] now = counts(r);
        for (int i = 0; i < COUNTS; i++) {
            if (!isCounting(i)) {
                shown[i] = now[i];
            }
        }
    }

    /** Starts the changes since the counts were last shown; the first look at a run shows it as it is. */
    private void follow(RogueRun r, int[] now) {
        if (shownRun.get() != r) {
            shownRun = new WeakReference<>(r);
            for (int i = 0; i < COUNTS; i++) {
                shown[i] = now[i];
            }
            return;
        }
        for (int i = 0; i < COUNTS; i++) {
            int from = Math.round(shown[i]);
            if (from == now[i] || isCounting(i)) {
                continue;
            }
            int index = i;
            int to = now[i];
            counting |= 1 << i;
            RogueMotion.play(0.7f, t -> shown[index] = from + (to - from) * Interpolation.pow3Out.apply(t), () -> {
                shown[index] = to;
                counting &= ~(1 << index);
            });
            launchCoins(i, to - from);
        }
    }

    private static boolean isCounting(int i) {
        return (counting & (1 << i)) != 0;
    }

    private void launchCoins(int i, int change) {
        float u = RogueLook.unit();
        float size = 14 * u;
        float[] source = screen.coinSource(i);
        float[] mid = source != null ? source : new float[]{screen.getWidth() / 2 - size, screen.getHeight() * 0.4f - size, size * 2, size * 2};
        float[] bar = {getLeft() + iconX[i], getTop() + iconY, size, size};
        int coins = Math.max(3, Math.min(8, Math.abs(change)));
        for (int c = 0; c < coins; c++) {
            float spread = (c - coins / 2f) * 12 * u;
            float[] a = change > 0 ? new float[]{mid[0] + spread, mid[1], mid[2], mid[3]} : bar;
            float[] b = change > 0 ? bar : new float[]{mid[0] + spread, mid[1], mid[2], mid[3]};
            screen.fly((g, x, y, w, h) -> icon(g, i, x, y, w), a, b, c * 0.05f, 0.5f, 40 * u, null);
        }
    }

    @Override
    protected void drawBackground(Graphics g) {
        RogueRun r = run.get();
        if (r == null) {
            return;
        }
        if (shownRun.get() != r) {
            follow(r, counts(r));
        }
        float u = RogueLook.unit();
        float w = getWidth();
        RogueLook.paper(g, 0, 0, w, getHeight());
        float x;
        float y;
        float lifeGap;
        if (compact) {
            RogueLook.fit(g, r.getCurrentCommanderName(), 8 * u, 5 * u, 0.9f, RogueLook.SOFT, w - 16 * u, Align.left);
            x = 8 * u;
            y = 20 * u;
            lifeGap = 0;
        } else {
            RogueLook.avatar(g, r.getSelectedRogueDeck().getAvatarIndex(), 9 * u, 8 * u, 28 * u);
            RogueLook.fit(g, r.getCurrentCommanderName(), 44 * u, 4 * u, 1.15f, RogueLook.INK, w - 76 * u, Align.left);
            x = 44 * u;
            y = 21 * u;
            lifeGap = Math.max((w - 78 * u) / 4, 50 * u);
        }
        iconY = y;
        for (int i = 0; i < COUNTS; i++) {
            if (compact) {
                x = 8 * u + COMPACT_SLOTS[i] * (w - 16 * u);
            }
            int value = Math.round(shown[i]);
            String text = i == LIFE ? value + "/" + r.getMaxLife() : String.valueOf(value);
            float size = 14 * u;
            iconX[i] = x;
            float bump = isCounting(i) ? 2 * u : 0;
            icon(g, i, x - bump / 2, y - bump / 2, size + bump);
            RogueLook.text(g, text, x + 16 * u, y + u, 1, RogueLook.INK);
            x += i == LIFE ? lifeGap : (16 + RogueLook.textWidth(text, 1) / u + 10) * u;
        }
        // While the flow works, the screen showing may be a moment from replaced by the one that asks, such as the rewards
        // after a match, and the change should rise there, where its coins fly from
        if (Forge.getCurrentScreen() == screen && !RogueController.get().working()) {
            follow(r, counts(r));
        }
    }
}
