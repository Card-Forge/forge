package forge.game.ability.effects;

import java.util.List;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;

public class TwoPilesEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        String valid = "";
        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        sb.append("Separate all ").append(valid).append(" cards ");

        for (final Player p : tgtPlayers) {
            sb.append(p).append(" ");
        }
        sb.append("controls into two piles.");
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        ZoneType zone = null;
        boolean pile1WasChosen = true;
        boolean isLeftRightPile = sa.hasParam("LeftRightPile");

        if (sa.hasParam("Zone")) {
            zone = ZoneType.smartValueOf(sa.getParam("Zone"));
        }

        String valid = "Card";
        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final List<Player> tgtPlayers = getTargetPlayers(sa);

        Player separator = card.getController();
        if (sa.hasParam("Separator")) {
            separator = AbilityUtils.getDefinedPlayers(card, sa.getParam("Separator"), sa).get(0);
        }

        Player chooser = tgtPlayers.get(0);
        if (sa.hasParam("Chooser")) {
            chooser = AbilityUtils.getDefinedPlayers(card, sa.getParam("Chooser"), sa).get(0);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                CardCollectionView pool0;
                if (sa.hasParam("DefinedCards")) {
                    pool0 = AbilityUtils.getDefinedCards(card, sa.getParam("DefinedCards"), sa);
                } else {
                    pool0 = p.getCardsIn(zone);
                }
                CardCollection pool = CardLists.getValidCards(pool0, valid, card.getController(), card, sa);
                int size = pool.size();
                if (size == 0) {
                    return;
                }

                String title;
                if("One".equals(sa.getParamOrDefault("FaceDown", "False"))) {
                    title = Localizer.getInstance().getMessage("lblSelectCardForFaceDownPile");
                } else if (isLeftRightPile) {
                    title = Localizer.getInstance().getMessage("lblSelectCardForLeftPile");
                } else {
                    title = Localizer.getInstance().getMessage("lblDivideCardIntoTwoPiles");
                }

                card.clearRemembered();

                // first, separate the cards into piles
                final CardCollectionView pile1 = separator.getController().chooseCardsForEffect(pool, sa, title, 0, size, false, null);
                final CardCollection pile2 = new CardCollection(pool);
                pile2.removeAll(pile1);

                if (isLeftRightPile) {
                    pile1WasChosen = true;
                } else {
                    pile1WasChosen = chooser.getController().chooseCardsPile(sa, pile1, pile2, sa.getParamOrDefault("FaceDown", "False"));
                }
                CardCollectionView chosenPile = pile1WasChosen ? pile1 : pile2;
                CardCollectionView unchosenPile = !pile1WasChosen ? pile1 : pile2;
                
                StringBuilder notification = new StringBuilder();
                if (isLeftRightPile) {
                    notification.append("\n");
                    notification.append(Lang.getInstance().getPossessedObject(separator.getName(), Localizer.getInstance().getMessage("lblLeftPile")));
                    notification.append("\n--------------------\n");
                    if (!chosenPile.isEmpty()) {
                        for (Card c : chosenPile) {
                            notification.append(c.getName()).append("\n");
                        }
                    } else {
                        notification.append("(" + Localizer.getInstance().getMessage("lblEmptyPile") + ")\n");
                    }
                    notification.append("\n");
                    notification.append(Lang.getInstance().getPossessedObject(separator.getName(), Localizer.getInstance().getMessage("lblRightPile")));
                    notification.append("\n--------------------\n");
                    if (!unchosenPile.isEmpty()) {
                        for (Card c : unchosenPile) {
                            notification.append(c.getName()).append("\n");
                        }
                    } else {
                        notification.append("(" + Localizer.getInstance().getMessage("lblEmptyPile") + ")\n");
                    }
                    p.getGame().getAction().notifyOfValue(sa, separator, notification.toString(), separator);
                } else {
                    notification.append(chooser + " " + Localizer.getInstance().getMessage("lblChoosesPile") + " " + (pile1WasChosen ? "1" : "2") + ":\n");
                    if (!chosenPile.isEmpty()) {
                        for (Card c : chosenPile) {
                            notification.append(c.getName()).append("\n");
                        }
                    } else {
                        notification.append("(" + Localizer.getInstance().getMessage("lblEmptyPile") + ")");
                    }
                    p.getGame().getAction().notifyOfValue(sa, chooser, notification.toString(), chooser);
                }


                // take action on the chosen pile
                if (sa.hasParam("ChosenPile")) {
                    for (final Card z : chosenPile) {
                        card.addRemembered(z);
                    }

                    SpellAbility sub = sa.getAdditionalAbility("ChosenPile");
                    if (sub != null) {
                        AbilityUtils.resolve(sub);
                    }
                }

                // take action on the unchosen pile
                if (sa.hasParam("UnchosenPile")) {
                    card.clearRemembered();
                    for (final Card z : unchosenPile) {
                        card.addRemembered(z);
                    }
                    
                    SpellAbility sub = sa.getAdditionalAbility("UnchosenPile");
                    if (sub != null) {
                        AbilityUtils.resolve(sub);
                    }
                }
            }
        }
    } // end twoPiles resolve
}
