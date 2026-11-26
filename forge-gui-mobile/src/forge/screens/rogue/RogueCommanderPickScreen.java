package forge.screens.rogue;

import com.badlogic.gdx.utils.Align;
import forge.Graphics;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.util.Localizer;
import java.util.List;

/** Choosing any eligible commander, or a partner for the one chosen, from a searchable list. */
public class RogueCommanderPickScreen extends RogueScreen {
    private final String title;
    private final CardManager list = add(new CardManager(false));
    private final RogueButton cancel;
    private final RogueButton skip;
    private final RogueButton choose;

    /** partnerOf is null for the first commander; then Skip keeps that one alone. */
    public RogueCommanderPickScreen(List<PaperCard> commanders, PaperCard partnerOf, RogueQuestion<List<PaperCard>> question) {
        super(null);
        Localizer localizer = Localizer.getInstance();
        title = partnerOf == null ? localizer.getMessage("lblRogueChooseAnyCommander")
            : localizer.getMessage("lblChoosePartnerFor", partnerOf.getName());
        list.setup(ItemManagerConfig.CARD_CATALOG);
        list.setPool(commanders);
        list.setItemActivateHandler(e -> answerSelected(question, partnerOf));
        cancel = add(new RogueButton(localizer.getMessage("lblCancel"), null, RogueButton.State.NORMAL,
            () -> question.answer(List.of())));
        skip = partnerOf == null ? null : add(new RogueButton(localizer.getMessage("lblSkip"), null, RogueButton.State.NORMAL,
            () -> question.answer(List.of(partnerOf))));
        choose = add(new RogueButton(localizer.getMessage("lblRogueChooseCommander"), null, RogueButton.State.MAIN,
            () -> answerSelected(question, partnerOf)));
    }

    private void answerSelected(RogueQuestion<List<PaperCard>> question, PaperCard partnerOf) {
        PaperCard selected = list.getSelectedItem();
        if (selected != null) {
            question.answer(partnerOf == null ? List.of(selected) : List.of(partnerOf, selected));
        }
    }

    @Override
    protected float backgroundDim() {
        return 0.25f;
    }

    @Override
    protected List<List<RogueButton>> buttonRows() {
        return List.of(skip == null ? List.of(cancel, choose) : List.of(cancel, skip, choose));
    }

    @Override
    protected void layoutBody(float x, float y, float w, float h) {
        float u = RogueLook.unit();
        list.setBounds(x + 12 * u, y + 32 * u, w - 24 * u, h - 44 * u);
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);
        float u = RogueLook.unit();
        float w = getWidth();
        float h = list.getBottom() + 12 * u;
        RogueLook.paper(g, 3 * u, 3 * u, w - 6 * u, h - 3 * u);
        RogueLook.fit(g, title, 15 * u, 12 * u, 1.3f, RogueLook.INK, w - 30 * u, Align.left);
    }
}
