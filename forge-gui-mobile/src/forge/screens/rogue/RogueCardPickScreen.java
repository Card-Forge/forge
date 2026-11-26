package forge.screens.rogue;

import com.badlogic.gdx.utils.Align;
import forge.Graphics;
import forge.deck.FDeckViewer;
import forge.gamemodes.rogue.flow.RogueDecisions.CardChoice;
import forge.gamemodes.rogue.flow.RogueDecisions.CardChoiceKind;
import forge.item.PaperCard;
import forge.util.Localizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Choosing some of a list of cards: a starting deck, cards to add, or cards to remove. */
public class RogueCardPickScreen extends RogueScreen {
    private final CardChoice choice;
    private final RogueQuestion<List<PaperCard>> question;
    private final List<PaperCard> cards;
    private final Set<Integer> picked = new LinkedHashSet<>();
    private final RogueCardGrid grid;
    private final RogueButton viewDeck;
    private final RogueButton clear;
    private final RogueButton ok;

    public RogueCardPickScreen(CardChoice choice, RogueQuestion<List<PaperCard>> question) {
        super(choice.run() == null || choice.kind() == CardChoiceKind.STARTING_DECK ? null : choice::run);
        this.choice = choice;
        this.question = question;
        this.cards = choice.shown();
        Localizer localizer = Localizer.getInstance();
        grid = add(new RogueCardGrid(cards, picked::contains, this::toggle, choice.kind() == CardChoiceKind.REMOVE));
        viewDeck = add(new RogueButton(localizer.getMessage("lblViewDeck"), null, RogueButton.State.NORMAL, this::viewDeck));
        clear = add(new RogueButton(localizer.getMessage("lblClearSelection"), null, RogueButton.State.NORMAL, () -> {
            picked.clear();
            update();
        }));
        ok = add(new RogueButton("", null, RogueButton.State.DISABLED, this::confirm));
        update();
    }

    private void toggle(int index) {
        if (!picked.remove(index)) {
            if (choice.max() == 1) {
                picked.clear();
            }
            if (picked.size() < choice.max()) {
                picked.add(index);
            }
        }
        update();
    }

    private List<PaperCard> pickedCards() {
        List<PaperCard> list = new ArrayList<>();
        for (int i : picked) {
            list.add(cards.get(i));
        }
        return list;
    }

    private void update() {
        Localizer localizer = Localizer.getInstance();
        int missing = choice.min() - picked.size();
        ok.setText(missing > 0 ? localizer.getMessage("lblRogueChooseMore", missing) : localizer.getMessage("lblOK"));
        ok.setState(choice.accepts(pickedCards()) ? RogueButton.State.MAIN : RogueButton.State.DISABLED);
    }

    private void confirm() {
        List<PaperCard> list = pickedCards();
        if (choice.accepts(list)) {
            for (List<RogueButton> row : buttonRows()) {
                for (RogueButton button : row) {
                    button.setState(RogueButton.State.DISABLED);
                }
            }
            grid.flyChosen(this, ok, () -> question.answer(list));
        }
    }

    private void viewDeck() {
        if (choice.preview() != null) {
            FDeckViewer.show(choice.preview().apply(pickedCards()));
        } else if (choice.run() != null) {
            FDeckViewer.show(choice.run().getCurrentDeck());
        }
    }

    private String countText() {
        Localizer localizer = Localizer.getInstance();
        return choice.min() == choice.max()
            ? localizer.getMessage("lblRogueFixedCardSelectionCount", choice.subtitle(), picked.size(), choice.max())
            : localizer.getMessage("lblRogueVariableCardSelectionCount", choice.subtitle(), picked.size(), choice.min(), choice.max());
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
        return List.of(List.of(viewDeck, clear, ok));
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
        RogueLook.paper(g, 3 * u, 3 * u, w - 6 * u, 42 * u);
        RogueLook.fit(g, choice.title(), 10 * u, 8 * u, 1.35f, RogueLook.INK, w - 20 * u, Align.center);
        RogueLook.fit(g, countText(), 10 * u, 26 * u, 1, RogueLook.SOFT, w - 20 * u, Align.center);
    }
}
