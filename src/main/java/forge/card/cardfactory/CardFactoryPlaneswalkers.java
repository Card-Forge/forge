package forge.card.cardfactory;

import com.esotericsoftware.minlog.Log;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.CardUtil;
import forge.Constant;
import forge.Constant.Zone;
import forge.Counters;
import forge.Phase;
import forge.Player;
import forge.PlayerZone;
import forge.card.cost.Cost;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.gui.GuiUtils;

/**
 * <p>
 * CardFactory_Planeswalkers class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardFactoryPlaneswalkers {

    /**
     * <p>
     * getCard.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param cardName
     *            a {@link java.lang.String} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getCard(final Card card, final String cardName) {
        // All Planeswalkers set their loyality in the beginning
        if (card.getBaseLoyalty() > 0) {
            card.addComesIntoPlayCommand(CardFactoryUtil.entersBattleFieldWithCounters(card, Counters.LOYALTY,
                    card.getBaseLoyalty()));
        }

        //*************** START *********** START **************************
        if (cardName.equals("Sarkhan the Mad")) {

            // Planeswalker book-keeping
            final int[] turn = new int[1];
            turn[0] = -1;

            // ability1
            /*
             * 0: Reveal the top card of your library and put it into your hand.
             * Sarkhan the Mad deals damage to himself equal to that card's
             * converted mana cost.
             */
            final SpellAbility ability1 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.addCounterFromNonEffect(Counters.LOYALTY, 0);
                    turn[0] = AllZone.getPhase().getTurn();

                    final Player player = card.getController();
                    final PlayerZone lib = player.getZone(Constant.Zone.Library);

                    final Card topCard = lib.get(0);
                    final int convertedManaTopCard = CardUtil.getConvertedManaCost(topCard.getManaCost());
                    final CardList showTop = new CardList();
                    showTop.add(topCard);
                    GuiUtils.getChoiceOptional("Revealed top card: ", showTop.toArray());

                    // now, move it to player's hand
                    AllZone.getGameAction().moveToHand(topCard);

                    // now, do X damage to Sarkhan
                    card.addDamage(convertedManaTopCard, card);

                } // resolve()

                @Override
                public boolean canPlayAI() {
                    // the computer isn't really smart enough to play this
                    // effectively, and it doesn't really
                    // help unless there are no cards in his hand
                    return false;
                }

                @Override
                public boolean canPlay() {
                    // looks like standard Planeswalker stuff...
                    // maybe should check if library is empty, or 1 card?
                    return AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                    && (turn[0] != AllZone.getPhase().getTurn()) && Phase.canCastSorcery(card.getController());
                } // canPlay()
            };
            ability1.setDescription("0: Reveal the top card of your library and put it "
                    + "into your hand. Sarkhan the Mad deals damage to himself equal to that card's converted mana cost.");
            final StringBuilder stack1 = new StringBuilder();
            stack1.append(card.getName()).append(" - Reveal top card and do damage.");
            ability1.setStackDescription(stack1.toString());

            // ability2
            /*
             * -2: Target creature's controller sacrifices it, then that player
             * puts a 5/5 red Dragon creature token with flying onto the
             * battlefield.
             */
            final Target target2 = new Target(card, "TgtC");
            final Cost cost2 = new Cost("SubCounter<2/LOYALTY>", cardName, true);
            final SpellAbility ability2 = new AbilityActivated(card, cost2, target2) {
                private static final long serialVersionUID = 4322453486268967722L;

                @Override
                public void resolve() {
                    // card.subtractCounter(Counters.LOYALTY, 2);
                    turn[0] = AllZone.getPhase().getTurn();

                    final Card target = this.getTargetCard();
                    AllZone.getGameAction().sacrifice(target);
                    // in makeToken, use target for source, so it goes into the
                    // correct Zone
                    CardFactoryUtil.makeToken("Dragon", "R 5 5 Dragon", target.getController(), "R", new String[] {
                        "Creature", "Dragon" }, 5, 5, new String[] { "Flying" });

                } // resolve()

                @Override
                public boolean canPlayAI() {
                    CardList creatures = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());
                    creatures = creatures.filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
                            return !(c.isToken() && c.isType("Dragon"));
                        }
                    });
                    return creatures.size() >= 1;
                }

                @Override
                public void chooseTargetAI() {
                    CardList cards = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                    // avoid targeting the dragon tokens we just put in play...
                    cards = cards.filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
                            return !(c.isToken() && c.isType("Dragon"));
                        }
                    });
                    this.setTargetCard(CardFactoryUtil.getCheapestCreatureAI(cards, card, true));
                    Log.debug(
                            "Sarkhan the Mad",
                            "Sarkhan the Mad caused sacrifice of: "
                            + CardFactoryUtil.getCheapestCreatureAI(cards, card, true));
                }

                @Override
                public boolean canPlay() {
                    return AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                    && (card.getCounters(Counters.LOYALTY) >= 2) && (turn[0] != AllZone.getPhase().getTurn())
                    && Phase.canCastSorcery(card.getController());
                } // canPlay()
            };
            ability2.setDescription("-2: Target creature's controller sacrifices it, "
                    + "then that player puts a 5/5 red Dragon creature token with flying onto the battlefield.");

            // ability3
            /*
             * -4: Each Dragon creature you control deals damage equal to its
             * power to target player.
             */
            final Target target3 = new Target(card, "Select target player", "Player");
            final Cost cost3 = new Cost("SubCounter<4/LOYALTY>", cardName, true);
            final SpellAbility ability3 = new AbilityActivated(card, cost3, target3) {
                private static final long serialVersionUID = -5488579738767048060L;

                @Override
                public void resolve() {
                    // card.subtractCounter(Counters.LOYALTY, 4);
                    turn[0] = AllZone.getPhase().getTurn();

                    final Player target = this.getTargetPlayer();
                    final Player player = card.getController();
                    final CardList dragons = player.getCardsIn(Zone.Battlefield).getType("Dragon");
                    for (int i = 0; i < dragons.size(); i++) {
                        final Card dragon = dragons.get(i);
                        final int damage = dragon.getNetAttack();
                        target.addDamage(damage, dragon);
                    }

                } // resolve()

                @Override
                public boolean canPlayAI() {
                    this.setTargetPlayer(AllZone.getHumanPlayer());
                    final CardList dragons = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield).getType("Dragon");
                    return (card.getCounters(Counters.LOYALTY) >= 4) && (dragons.size() >= 1);
                }

                @Override
                public boolean canPlay() {
                    return AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)
                    && (card.getCounters(Counters.LOYALTY) >= 4) && (turn[0] != AllZone.getPhase().getTurn())
                    && Phase.canCastSorcery(card.getController());
                } // canPlay()
            };
            ability3.setDescription("-4: Each Dragon creature you control "
                    + "deals damage equal to its power to target player.");

            card.addSpellAbility(ability1);
            card.addSpellAbility(ability2);
            card.addSpellAbility(ability3);

            card.setSVars(card.getSVars());
            card.setSets(card.getSets());

            return card;
        } // *************** END ************ END **************************

        return card;
    }

} //end class CardFactoryPlaneswalkers
