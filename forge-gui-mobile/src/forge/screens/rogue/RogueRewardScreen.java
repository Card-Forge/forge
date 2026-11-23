package forge.screens.rogue;

import com.badlogic.gdx.utils.Align;
import forge.Graphics;
import forge.deck.FDeckViewer;
import forge.gamemodes.rogue.RogueRun;
import forge.item.PaperCard;
import forge.util.Localizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** A card reward after a won match: up to a number of cards, or none. Reroll answers null. */
public class RogueRewardScreen extends RogueScreen {
    private final String title;
    private final List<PaperCard> cards;
    private final int maxSelections;
    private final int gold;
    private final Set<Integer> picked = new LinkedHashSet<>();
    private final RogueCardGrid grid;
    private final RogueButton viewDeck;
    private final RogueButton reroll;
    private final RogueButton ok;

    /** rerollLabel is null when there is no reroll. */
    public RogueRewardScreen(RogueRun run, String title, List<PaperCard> cards, int maxSelections, String rerollLabel,
                             boolean rerollEnabled, int gold, RogueQuestion<List<PaperCard>> question) {
        super(run == null ? null : () -> run);
        this.title = title;
        this.cards = cards;
        this.maxSelections = maxSelections;
        this.gold = gold;
        Localizer localizer = Localizer.getInstance();
        grid = add(new RogueCardGrid(cards, picked::contains, this::toggle, false));
        viewDeck = add(new RogueButton(localizer.getMessage("lblViewDeck"), null, RogueButton.State.NORMAL,
            () -> {
                if (run != null) {
                    FDeckViewer.show(run.getCurrentDeck());
                }
            }));
        reroll = rerollLabel == null ? null : add(new RogueButton(rerollLabel, "Refresh",
            rerollEnabled ? RogueButton.State.NORMAL : RogueButton.State.DISABLED, () -> question.answer(null)));
        ok = add(new RogueButton(localizer.getMessage("lblOK"), null, RogueButton.State.MAIN, () -> {
            List<PaperCard> chosen = new ArrayList<>();
            for (int i : picked) {
                chosen.add(cards.get(i));
            }
            question.answer(chosen);
        }));
    }

    private void toggle(int index) {
        if (!picked.remove(index) && picked.size() < maxSelections) {
            picked.add(index);
        }
    }

    @Override
    protected String background() {
        return "reward";
    }

    @Override
    protected float backgroundDim() {
        return 0.45f;
    }

    @Override
    protected List<List<RogueButton>> buttonRows() {
        return List.of(reroll == null ? List.of(viewDeck, ok) : List.of(viewDeck, reroll, ok));
    }

    @Override
    protected void layoutBody(float x, float y, float w, float h) {
        float u = RogueLook.unit();
        grid.setBounds(x, y + 45 * u, w, h - 45 * u);
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);
        float u = RogueLook.unit();
        float w = getWidth();
        Localizer localizer = Localizer.getInstance();
        RogueLook.paper(g, 3 * u, 3 * u, w - 6 * u, 42 * u);
        RogueLook.fit(g, localizer.getMessage("lblRogueVictoryReward", title), 10 * u, 8 * u, 1.35f, RogueLook.INK, w - 90 * u, Align.left);
        RogueLook.fit(g, localizer.getMessage("lblRogueGoldTotal", gold), w - 80 * u, 10 * u, 1, RogueLook.SOFT, 70 * u, Align.right);
        RogueLook.fit(g, localizer.getMessage("lblRogueCardRewardSelectionCount", maxSelections, picked.size()), 10 * u, 26 * u, 1,
            RogueLook.SOFT, w - 20 * u, Align.center);
    }
}
