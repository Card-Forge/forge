package forge.screens.rogue;

import com.badlogic.gdx.utils.Align;
import forge.Graphics;
import forge.assets.FSkinImage;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RogueRun;
import forge.toolbox.FContainer;
import java.util.function.Supplier;

/** The commander, life, gold, Echoes and removal credits, with the run menu button. */
public class RogueRunBar extends FContainer {
    private final Supplier<RogueRun> run;
    private final RogueButton menu;

    public RogueRunBar(Supplier<RogueRun> run, Runnable openMenu) {
        this.run = run;
        this.menu = add(new RogueButton("", "Menu", RogueButton.State.NORMAL, openMenu));
    }

    @Override
    protected void doLayout(float width, float height) {
        float u = RogueLook.unit();
        menu.setBounds(width - 30 * u, 10 * u, 24 * u, 25 * u);
    }

    @Override
    protected void drawBackground(Graphics g) {
        RogueRun r = run.get();
        if (r == null) {
            return;
        }
        float u = RogueLook.unit();
        float w = getWidth();
        RogueLook.paper(g, 0, 0, w, getHeight());
        RogueLook.avatar(g, r.getSelectedRogueDeck().getAvatarIndex(), 9 * u, 8 * u, 28 * u);
        RogueLook.fit(g, r.getCurrentCommanderName(), 44 * u, 4 * u, 1.15f, RogueLook.INK, w - 76 * u, Align.left);

        String[] values = {r.getCurrentLife() + "/" + r.getMaxLife(), String.valueOf(r.getCurrentGold()),
            String.valueOf(RogueMetaProgress.getInstance().getTotalEchoes()), String.valueOf(r.getRemovalCredits())};
        float x = 44 * u;
        float y = 21 * u;
        float lifeGap = Math.max((w - 78 * u) / 4, 50 * u);
        for (int i = 0; i < values.length; i++) {
            float size = 14 * u;
            switch (i) {
                case 0 -> RogueLook.item(g, "Life", x, y, size);
                case 1 -> RogueLook.skin(g, FSkinImage.QUEST_COIN, x, y, size);
                case 2 -> RogueLook.skin(g, FSkinImage.QUEST_GOLD, x, y, size);
                default -> RogueLook.skin(g, FSkinImage.CARD_IMAGE, x, y, size);
            }
            RogueLook.text(g, values[i], x + 16 * u, y + u, 1, RogueLook.INK);
            x += i == 0 ? lifeGap : (16 + RogueLook.textWidth(values[i], 1) / u + 10) * u;
        }
    }
}
