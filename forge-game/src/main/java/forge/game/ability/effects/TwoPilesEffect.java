package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.collect.FCollectionView;

public class TwoPilesEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final String valid = sa.getParamOrDefault("ValidCards", "");

        sb.append("Separate all ").append(valid).append(" cards ");

        sb.append(Lang.joinHomogenous(getTargetPlayers(sa)));
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

        final String valid = sa.getParamOrDefault("ValidCards", "Card");

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        Player separator = card.getController();
        if (sa.hasParam("Separator")) {
            final FCollectionView<Player> choosers = AbilityUtils.getDefinedPlayers(card, sa.getParam("Separator"), sa);
            if (!choosers.isEmpty()) {
                separator = sa.getActivatingPlayer().getController().chooseSingleEntityForEffect(choosers, null, sa, Localizer.getInstance().getMessage("lblChooser") + ":", false, null, null);
            }
        }

        Player chooser = tgtPlayers.get(0);
        if (sa.hasParam("Chooser")) {
            final FCollectionView<Player> choosers = AbilityUtils.getDefinedPlayers(card, sa.getParam("Chooser"), sa);
            if (!choosers.isEmpty()) {
                chooser = sa.getActivatingPlayer().getController().chooseSingleEntityForEffect(choosers, null, sa, Localizer.getInstance().getMessage("lblChooser") + ":", false, null, null);
            }
        }

        for (final Player p : tgtPlayers) {
            if (!p.isInGame()) {
                continue;
            }

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
            if ("One".equals(sa.getParamOrDefault("FaceDown", "False"))) {
                title = Localizer.getInstance().getMessage("lblSelectCardForFaceDownPile");
            } else if (isLeftRightPile) {
                title = Localizer.getInstance().getMessage("lblSelectCardForLeftPile");
            } else {
                title = Localizer.getInstance().getMessage("lblDivideCardIntoTwoPiles");
            }

            // first, separate the cards into piles
            final CardCollectionView pile1;
            final CardCollection pile2;
            if (sa.hasParam("DefinedPiles")) {
                final String[] def = sa.getParam("DefinedPiles").split(",", 2);
                pile1 = AbilityUtils.getDefinedCards(card, def[0], sa);
                pile2 = AbilityUtils.getDefinedCards(card, def[1], sa);
            } else {
                pile1 = separator.getController().chooseCardsForEffect(pool, sa, title, 0, size, false, null);
                pile2 = new CardCollection(pool);
                pile2.removeAll(pile1);
            }

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


            if (sa.hasParam("RememberChosen")) {
                card.addRemembered(chosenPile);
            }

            // take action on the chosen pile
            if (sa.hasParam("ChosenPile")) {
                List<Object> tempRemembered = Lists.newArrayList(card.getRemembered());
                card.removeRemembered(tempRemembered);
                card.addRemembered(chosenPile);

                SpellAbility sub = sa.getAdditionalAbility("ChosenPile");
                if (sub != null) {
                    AbilityUtils.resolve(sub);
                }
                card.removeRemembered(chosenPile);
                card.addRemembered(tempRemembered);
            }

            // take action on the unchosen pile
            if (sa.hasParam("UnchosenPile")) {
                List<Object> tempRemembered = Lists.newArrayList(card.getRemembered());
                card.removeRemembered(tempRemembered);
                card.addRemembered(unchosenPile);

                SpellAbility sub = sa.getAdditionalAbility("UnchosenPile");
                if (sub != null) {
                    AbilityUtils.resolve(sub);
                }
                card.removeRemembered(unchosenPile);
                card.addRemembered(tempRemembered);
            }
        }

        if (!sa.hasParam("KeepRemembered") && !sa.hasParam("RememberChosen")) {
            // prior to addition of "DefinedPiles" param, TwoPilesEffect cleared remembered objects in the
            // Chosen/Unchosen subability resolutions, so this preserves that
            card.clearRemembered();
        }
    }
}
