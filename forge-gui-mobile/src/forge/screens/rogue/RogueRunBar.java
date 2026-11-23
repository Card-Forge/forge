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
    private static WeakReference<RogueRun> shownRun = new WeakReference<>(null);
    private static final int[] now = new int[COUNTS];
    private static final float[] shown = new float[COUNTS];
    private static int counting;

    private final RogueScreen screen;
    private final Supplier<RogueRun> run;
    private final RogueButton menu;
    private final float[] iconX = new float[COUNTS];

    RogueRunBar(RogueScreen screen, Supplier<RogueRun> run, Runnable openMenu) {
        this.screen = screen;
        this.run = run;
        this.menu = add(new RogueButton("", "Menu", RogueButton.State.NORMAL, openMenu));
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
        float[] mid = {screen.getWidth() / 2 - size, screen.getHeight() * 0.4f - size, size * 2, size * 2};
        float[] bar = {getLeft() + iconX[i], getTop() + 21 * u, size, size};
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
        RogueLook.avatar(g, r.getSelectedRogueDeck().getAvatarIndex(), 9 * u, 8 * u, 28 * u);
        RogueLook.fit(g, r.getCurrentCommanderName(), 44 * u, 4 * u, 1.15f, RogueLook.INK, w - 76 * u, Align.left);

        float x = 44 * u;
        float y = 21 * u;
        float lifeGap = Math.max((w - 78 * u) / 4, 50 * u);
        for (int i = 0; i < COUNTS; i++) {
            int value = Math.round(shown[i]);
            String text = i == LIFE ? value + "/" + r.getMaxLife() : String.valueOf(value);
            float size = 14 * u;
            iconX[i] = x;
            float bump = isCounting(i) ? 2 * u : 0;
            icon(g, i, x - bump / 2, y - bump / 2, size + bump);
            RogueLook.text(g, text, x + 16 * u, y + u, 1, RogueLook.INK);
            x += i == LIFE ? lifeGap : (16 + RogueLook.textWidth(text, 1) / u + 10) * u;
        }
        if (Forge.getCurrentScreen() == screen) {
            follow(r, counts(r));
        }
    }
}
