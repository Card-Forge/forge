package forge.screens.rogue;

import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinImage;
import forge.deck.FDeckViewer;
import forge.gamemodes.rogue.RogueRun;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.util.Localizer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A card reward after a won match: up to a number of cards, or none. Reroll answers null. The battle's other spoils head it. */
public class RogueRewardScreen extends RogueScreen {
    /** Sideways, the width of the ledger panel down the left, in units. */
    static final float PANEL = 190;

    private final String title;
    private final List<PaperCard> cards;
    private final int maxSelections;
    private final Set<Integer> picked = new LinkedHashSet<>();
    private final List<RogueSpoils.Spoil> spoils = RogueSpoils.take();
    private final Map<FSkinProp, float[]> where = new EnumMap<>(FSkinProp.class);
    private final RogueCardGrid grid;
    private final RogueButton viewDeck;
    private final RogueButton reroll;
    private final RogueButton ok;

    /** rerollLabel is null when there is no reroll. */
    public RogueRewardScreen(RogueRun run, String title, List<PaperCard> cards, int maxSelections, String rerollLabel,
                             boolean rerollEnabled, RogueQuestion<List<PaperCard>> question) {
        super(run == null ? null : () -> run);
        this.title = title;
        this.cards = cards;
        this.maxSelections = maxSelections;
        Localizer localizer = Localizer.getInstance();
        grid = add(new RogueCardGrid(cards, picked::contains, this::toggle, false));
        viewDeck = add(new RogueButton(localizer.getMessage("lblViewDeck"), null, RogueButton.State.NORMAL,
            () -> {
                if (run != null) {
                    FDeckViewer.show(run.getCurrentDeck());
                }
            }));
        reroll = rerollLabel == null ? null : add(new RogueButton(rerollLabel, "Refresh",
            rerollEnabled ? RogueButton.State.NORMAL : RogueButton.State.DISABLED, () -> question.answer(null)).after(FSkinImage.QUEST_COIN));
        ok = add(new RogueButton(localizer.getMessage("lblOK"), null, RogueButton.State.MAIN, () -> confirm(question)));
    }

    private void confirm(RogueQuestion<List<PaperCard>> question) {
        List<PaperCard> chosen = new ArrayList<>();
        for (int i : picked) {
            chosen.add(cards.get(i));
        }
        for (RogueButton button : buttonRows().get(0)) {
            button.setState(RogueButton.State.DISABLED);
        }
        grid.flyChosen(this, viewDeck, () -> question.answer(chosen));
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
        float top = header(null) + 3 * u;
        grid.setBounds(x, y + top, w, h - top);
    }

    /** Sideways the run bar and the buttons stand at the foot of the ledger panel, and the cards fill the rest. */
    @Override
    protected boolean layoutSideways(float width, float height, RogueButton menu, RogueRunBar bar) {
        float u = RogueLook.unit();
        float panel = PANEL * u;
        float bx = 16 * u;
        float bw = panel - 26 * u;
        float okY = height - 46 * u;
        ok.setBounds(bx, okY, bw, 30 * u);
        float rowY = okY - 32 * u;
        if (reroll == null) {
            viewDeck.setBounds(bx, rowY, bw, 28 * u);
        } else {
            float half = (bw - 4 * u) / 2;
            reroll.setBounds(bx, rowY, half, 28 * u);
            viewDeck.setBounds(bx + half + 4 * u, rowY, half, 28 * u);
        }
        if (menu != null) {
            menu.setBounds(panel - 34 * u, 14 * u, 24 * u, 25 * u);
        }
        if (bar != null) {
            bar.setCompact(true);
            bar.setVisible(true);
            bar.setBounds(14 * u, rowY - 46 * u, panel - 22 * u, 40 * u);
        }
        grid.setBounds(panel + 7 * u, 49 * u, width - panel - 10 * u, height - 52 * u);
        return true;
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);
        if (!Forge.isLandscapeMode()) {
            header(g);
            return;
        }
        float u = RogueLook.unit();
        float panel = PANEL * u;
        Localizer localizer = Localizer.getInstance();
        RogueSpoils.sidePanel(g, 3 * u, 3 * u, panel, getHeight() - 6 * u, null, null, spoils, where);
        // The banner over the cards is the upright one: the title, then the selection line, both centred
        float bx = panel + 7 * u;
        float bw = getWidth() - panel - 10 * u;
        RogueLook.paper(g, bx, 3 * u, bw, 44 * u);
        RogueLook.fit(g, localizer.getMessage("lblRogueVictoryReward", title), bx + 10 * u, 8 * u, 1.35f, RogueLook.INK, bw - 20 * u, Align.center);
        RogueLook.fit(g, selection(), bx + 10 * u, 26 * u, 1, RogueLook.SOFT, bw - 20 * u, Align.center);
    }

    private String selection() {
        return Localizer.getInstance().getMessage("lblRogueCardRewardSelectionCount", maxSelections, picked.size());
    }

    private float header(Graphics g) {
        Localizer localizer = Localizer.getInstance();
        return RogueSpoils.uprightHeader(g, getWidth(), localizer.getMessage("lblRogueVictoryReward", title),
            selection(), spoils, g == null ? null : where);
    }

    @Override
    protected float[] coinSource(int count) {
        return where.get(RogueSpoils.iconFor(count));
    }
}
