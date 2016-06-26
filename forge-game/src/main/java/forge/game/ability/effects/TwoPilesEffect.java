package forge.game.ability.effects;

import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

import java.util.List;

public class TwoPilesEffect extends SpellAbilityEffect {

    // *************************************************************************
    // ***************************** TwoPiles **********************************
    // *************************************************************************

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
            separator = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Separator"), sa).get(0);
        }

        Player chooser = tgtPlayers.get(0);
        if (sa.hasParam("Chooser")) {
            chooser = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Chooser"), sa).get(0);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                CardCollectionView pool0;
                if (sa.hasParam("DefinedCards")) {
                    pool0 = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("DefinedCards"), sa);
                } else {
                    pool0 = p.getCardsIn(zone);
                }
                CardCollection pool = CardLists.getValidCards(pool0, valid, card.getController(), card);
                int size = pool.size();
                if (size == 0) {
                    return;
                }

                // first, separate the cards into piles
                final CardCollectionView pile1 = separator.getController().chooseCardsForEffect(pool, sa, "Divide cards into two piles", 0, size, false);
                final CardCollection pile2 = new CardCollection(pool);
                pile2.removeAll(pile1);

                System.out.println("Pile 1:" + pile1);
                System.out.println("Pile 2:" + pile2);
                card.clearRemembered();

                pile1WasChosen = chooser.getController().chooseCardsPile(sa, pile1, pile2, !sa.hasParam("FaceDown"));
                CardCollectionView chosenPile = pile1WasChosen ? pile1 : pile2;
                CardCollectionView unchosenPile = !pile1WasChosen ? pile1 : pile2;
                
                String notification = chooser + " chooses Pile " + (pile1WasChosen ? "1" : "2") + ":\n";
                if (!chosenPile.isEmpty()) {
                    for (Card c : chosenPile) {
                        notification += c.getName() + "\n";
                    }
                } else {
                    notification += "(Empty pile)";
                }

                p.getGame().getAction().nofityOfValue(sa, chooser, notification, chooser);

                // take action on the chosen pile
                if (sa.hasParam("ChosenPile")) {
                    for (final Card z : chosenPile) {
                        card.addRemembered(z);
                    }
                    final SpellAbility action = AbilityFactory.getAbility(card.getSVar(sa.getParam("ChosenPile")), card);
                    action.setActivatingPlayer(sa.getActivatingPlayer());
                    ((AbilitySub) action).setParent(sa);
                    AbilityUtils.resolve(action);
                }

                // take action on the unchosen pile
                if (sa.hasParam("UnchosenPile")) {
                    card.clearRemembered();
                    for (final Card z : unchosenPile) {
                        card.addRemembered(z);
                    }
                    
                    final SpellAbility action = AbilityFactory.getAbility(card.getSVar(sa.getParam("UnchosenPile")), card);
                    action.setActivatingPlayer(sa.getActivatingPlayer());
                    ((AbilitySub) action).setParent(sa);
                    AbilityUtils.resolve(action);
                }
            }
        }
    } // end twoPiles resolve
}
