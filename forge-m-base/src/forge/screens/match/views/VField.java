package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.toolbox.FCardPanel;

public class VField extends VZoneDisplay {
    private boolean flipped;

    public VField(Player player0) {
        super(player0, ZoneType.Battlefield);
        setVisible(true); //unlike other display areas, show by default
    }

    public boolean isFlipped() {
        return flipped;
    }
    public void setFlipped(boolean flipped0) {
        flipped = flipped0;
    }

    protected void refreshCardPanels(List<Card> model) {
        super.refreshCardPanels(model);

        for (final Card card : model) {
            updateCard(card);
        }
    }

    @Override
    public CardAreaPanel addCard(final Card card) {
        CardAreaPanel cardPanel = super.addCard(card);
        //cardPanel.setVisible(false); //hide placeholder until card arrives //TODO: Uncomment when animation set up
        return cardPanel;
    }

    public void updateCard(final Card card) {
        final FCardPanel toPanel = getCardPanel(card.getUniqueNumber());
        if (toPanel == null) { return; }

        if (card.isTapped()) {
            toPanel.setTapped(true);
            toPanel.setTappedAngle(FCardPanel.TAPPED_ANGLE);
        }
        else {
            toPanel.setTapped(false);
            toPanel.setTappedAngle(0);
        }

        toPanel.getAttachedPanels().clear();
        if (card.isEnchanted()) {
            final ArrayList<Card> enchants = card.getEnchantedBy();
            for (final Card e : enchants) {
                final FCardPanel cardE = getCardPanel(e.getUniqueNumber());
                if (cardE != null) {
                    toPanel.getAttachedPanels().add(cardE);
                }
            }
        }
   
        if (card.isEquipped()) {
            final ArrayList<Card> enchants = card.getEquippedBy();
            for (final Card e : enchants) {
                final FCardPanel cardE = getCardPanel(e.getUniqueNumber());
                if (cardE != null) {
                    toPanel.getAttachedPanels().add(cardE);
                }
            }
        }

        if (card.isFortified()) {
            final ArrayList<Card> fortifications = card.getFortifiedBy();
            for (final Card e : fortifications) {
                final FCardPanel cardE = getCardPanel(e.getUniqueNumber());
                if (cardE != null) {
                    toPanel.getAttachedPanels().add(cardE);
                }
            }
        }

        if (card.isEnchantingCard()) {
            toPanel.setAttachedToPanel(getCardPanel(card.getEnchantingCard().getUniqueNumber()));
        }
        else if (card.isEquipping()) {
            toPanel.setAttachedToPanel(getCardPanel(card.getEquipping().get(0).getUniqueNumber()));
        }
        else if (card.isFortifying()) {
            toPanel.setAttachedToPanel(getCardPanel(card.getFortifying().get(0).getUniqueNumber()));
        }
        else {
            toPanel.setAttachedToPanel(null);
        }

        toPanel.setCard(toPanel.getCard());
    }

    @Override
    protected void doLayout(float width, float height) {
        startLayout();

        List<FCardPanel> creatures = new ArrayList<FCardPanel>();
        List<FCardPanel> lands = new ArrayList<FCardPanel>();
        List<FCardPanel> otherPermanents = new ArrayList<FCardPanel>();

        for (FCardPanel cardPanel : getCardPanels()) {
            if (cardPanel.getCard().isCreature()) {
                creatures.add(cardPanel);
            }
            else if (cardPanel.getCard().isLand()) {
                lands.add(cardPanel);
            }
            else {
                otherPermanents.add(cardPanel);
            }
        }

        float cardSize = height / 2;
        float x = 0;
        float y = flipped ? cardSize : 0;

        for (FCardPanel cardPanel : creatures) {
            x += layoutCardPanel(cardPanel, x, y, cardSize, cardSize);
        }

        x = 0;
        y = flipped ? 0 : cardSize;

        for (FCardPanel cardPanel : lands) {
            x += layoutCardPanel(cardPanel, x, y, cardSize, cardSize);
        }
        for (FCardPanel cardPanel : otherPermanents) {
            x += layoutCardPanel(cardPanel, x, y, cardSize, cardSize);
        }
    }
}
