package forge.screens.rogue;

import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinFont;
import forge.card.CardRenderer;
import forge.card.CardRenderer.CardStackPosition;
import forge.gamemodes.rogue.RogueDeck;
import forge.gamemodes.rogue.flow.RogueStart;
import forge.screens.LaunchScreen;
import forge.screens.home.NewGameMenu;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FLabel;
import forge.toolbox.FTextArea;
import forge.util.Utils;
import java.util.List;

/** New Game's page for Rogue Commander: the mode's line and three commanders, then Start. */
public class RogueLaunchScreen extends LaunchScreen {
    private final FTextArea description = add(new FTextArea(false, Forge.getLocalizer().getMessage("lblRogueCommanderDesc")));
    private final FDisplayObject art = add(new FDisplayObject() {
        @Override
        public void draw(Graphics g) {
            List<RogueDeck> roster = RogueStart.roster(false);
            if (roster.size() < 3) {
                return;
            }
            float w = getWidth();
            float h = getHeight();
            float cw = Math.min(w / 3.2f, h / RogueLook.CARD_RATIO * 0.8f);
            float y = (h - cw * RogueLook.CARD_RATIO) / 2;
            float side = cw * 0.9f;
            CardRenderer.drawCard(g, roster.get(2).getStartDeck().getCommanders().get(0), w / 2 - cw * 1.35f, y + cw * 0.12f, side,
                side * RogueLook.CARD_RATIO, CardStackPosition.Top);
            CardRenderer.drawCard(g, roster.get(1).getStartDeck().getCommanders().get(0), w / 2 + cw * 0.45f, y + cw * 0.12f, side,
                side * RogueLook.CARD_RATIO, CardStackPosition.Top);
            CardRenderer.drawCard(g, roster.get(0).getStartDeck().getCommanders().get(0), w / 2 - cw / 2, y, cw,
                cw * RogueLook.CARD_RATIO, CardStackPosition.Top);
        }
    });

    public RogueLaunchScreen() {
        super(null, NewGameMenu.getMenu());
        description.setFont(FSkinFont.get(14));
        description.setAlignment(Align.center);
        description.setTextColor(FLabel.getInlineLabelColor());
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        float padding = Utils.scale(10);
        float descriptionHeight = description.getPreferredHeight(width - 2 * padding);
        description.setBounds(padding, startY + padding, width - 2 * padding, descriptionHeight);
        art.setBounds(padding, startY + 2 * padding + descriptionHeight, width - 2 * padding, height - startY - 3 * padding - descriptionHeight);
    }

    @Override
    protected void startMatch() {
        RogueMenu.launch();
    }
}
