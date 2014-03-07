package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;

import forge.Forge.Graphics;
import forge.assets.FSkinTexture;
import forge.game.card.Card;
import forge.model.FModel;
import forge.toolbox.FCardPanel;
import forge.toolbox.FContainer;
import forge.toolbox.FScrollPane;

public class VField extends FScrollPane {
    private boolean flipped;

    private final List<FCardPanel> creatures = new ArrayList<FCardPanel>();
    private final List<FCardPanel> lands = new ArrayList<FCardPanel>();
    private final List<FCardPanel> otherPermanents = new ArrayList<FCardPanel>();

    public VField() {
        Card creature = Card.getCardForUi(FModel.getMagicDb().getCommonCards().getCard("Llanowar Elves"));
        Card land = Card.getCardForUi(FModel.getMagicDb().getCommonCards().getCard("Forest"));
        Card artifact = Card.getCardForUi(FModel.getMagicDb().getCommonCards().getCard("Coat of Arms"));

        for (int i = 0; i < 6; i++) {
            creatures.add(add(new FCardPanel(creature)));
        }
        for (int i = 0; i < 3; i++) {
            lands.add(add(new FCardPanel(land)));
        }
        for (int i = 0; i < 2; i++) {
            otherPermanents.add(add(new FCardPanel(artifact)));
        }
    }

    public boolean isFlipped() {
        return flipped;
    }
    public void setFlipped(boolean flipped0) {
        flipped = flipped0;
    }

    @Override
    protected void doLayout(float width, float height) {
        float x = 0;
        float y = 0;
        float cardSize = height / 2;

        for (FCardPanel cardPanel : creatures) {
            cardPanel.setBounds(x, y, cardSize, cardSize);
            x += cardSize;
        }
        x = 0;
        y += cardSize;
        for (FCardPanel cardPanel : lands) {
            cardPanel.setBounds(x, y, cardSize, cardSize);
            x += cardSize;
        }
        for (FCardPanel cardPanel : otherPermanents) {
            cardPanel.setBounds(x, y, cardSize, cardSize);
            x += cardSize;
        }
    }
}
