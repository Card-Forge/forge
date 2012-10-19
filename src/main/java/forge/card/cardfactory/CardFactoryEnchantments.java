package forge.card.cardfactory;

import java.util.List;

import com.google.common.collect.Iterables;

import forge.Card;

import forge.CardLists;
import forge.Command;
import forge.GameActionUtil;
import forge.Singletons;
import forge.card.spellability.Ability;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellPermanent;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;
import forge.control.input.Input;
import forge.game.GameLossReason;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.match.CMatchUI;
import forge.view.ButtonUtil;
import forge.CardPredicates;

/** 
 * TODO: Write javadoc for this type.
 *
 */
class CardFactoryEnchantments {

    /**
     * TODO: Write javadoc for this method.
     * @param card
     * @param cardName
     * @return
     */
    public static void buildCard(final Card card, final String cardName) {

        if (cardName.equals("Bridge from Below")) {
            final SpellAbility spell = new SpellPermanent(card) {
                private static final long serialVersionUID = 7254358703158629514L;

                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };

            // Do not remove SpellAbilities created by AbilityFactory or
            // Keywords.
            card.clearFirstSpell();
            card.addSpellAbility(spell);
        }

     // *************** START *********** START **************************
        else if (cardName.equals("Night Soil")) {
            final SpellAbility nightSoil = new Ability(card, "1") {
                @Override
                public void resolve() {
                    CardFactoryUtil.makeToken("Saproling", "G 1 1 Saproling", card.getController(), "G", new String[] { "Creature",
                    "Saproling" }, 1, 1, new String[] { "" });
                }

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public boolean canPlay() {
                    boolean haveGraveWithSomeCreatures = false;
                    for( Player p : Singletons.getModel().getGameState().getPlayers()) {
                        Iterable<Card> grave = CardLists.filter(p.getCardsIn(ZoneType.Graveyard), CardPredicates.Presets.CREATURES);
                        if( Iterables.size(grave) > 1)
                        {
                            haveGraveWithSomeCreatures = true;
                            break;
                        }
                    }
                    return haveGraveWithSomeCreatures && super.canPlay();
                }
            };
            final Input soilTarget = new Input() {

                private boolean once = false;
                private static final long serialVersionUID = 8243511353958609599L;

                @Override
                public void showMessage() {
                    final Player human = Singletons.getControl().getPlayer();
                    List<Card> grave = human.getCardsIn(ZoneType.Graveyard);
                    List<Card> aiGrave = human.getOpponent().getCardsIn(ZoneType.Graveyard);
                    grave = CardLists.filter(grave, CardPredicates.Presets.CREATURES);
                    aiGrave = CardLists.filter(aiGrave, CardPredicates.Presets.CREATURES);

                    if (this.once || ((grave.size() < 2) && (aiGrave.size() < 2))) {
                        this.once = false;
                        this.stop();
                    } else {
                        List<Card> chooseGrave;
                        if (grave.size() < 2) {
                            chooseGrave = aiGrave;
                        } else if (aiGrave.size() < 2) {
                            chooseGrave = grave;
                        } else {
                            chooseGrave = aiGrave;
                            chooseGrave.addAll(grave);
                        }

                        final Card c = GuiChoose.one("Choose first creature to exile", chooseGrave);
                        if (c != null) {
                            List<Card> newGrave = CardLists.filter(c.getOwner().getCardsIn(ZoneType.Graveyard), CardPredicates.Presets.CREATURES);
                            newGrave.remove(c);

                            final Object o2 = GuiChoose.one("Choose second creature to exile", newGrave);
                            if (o2 != null) {
                                final Card c2 = (Card) o2;
                                newGrave.remove(c2);
                                Singletons.getModel().getGameAction().exile(c);
                                Singletons.getModel().getGameAction().exile(c2);
                                this.once = true;

                                Singletons.getModel().getGameState().getStack().addAndUnfreeze(nightSoil);

                            }
                        }
                    }
                    this.stop();
                }
            };

            final StringBuilder sbDesc = new StringBuilder();
            sbDesc.append("1, Exile two creature cards from a single graveyard: ");
            sbDesc.append("Put a 1/1 green Saproling creature token onto the battlefield.");
            nightSoil.setDescription(sbDesc.toString());

            final StringBuilder sbStack = new StringBuilder();
            sbStack.append(card.getController());
            sbStack.append(" puts a 1/1 green Saproling creature token onto the battlefield.");
            nightSoil.setStackDescription(sbStack.toString());

            nightSoil.setAfterPayMana(soilTarget);
            card.addSpellAbility(nightSoil);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Standstill")) {

            // Do not remove SpellAbilities created by AbilityFactory or
            // Keywords.
            card.clearFirstSpell();

            card.addSpellAbility(new SpellPermanent(card) {
                private static final long serialVersionUID = 6912683989507840172L;

                @Override
                public boolean canPlayAI() {
                    final List<Card> compCreats = getActivatingPlayer().getCreaturesInPlay();
                    final List<Card> humCreats = getActivatingPlayer().getOpponent().getCreaturesInPlay();

                    // only play standstill if comp controls more creatures than
                    // human
                    // this needs some additional rules, maybe add all power +
                    // toughness and compare
                    return (compCreats.size() > humCreats.size());
                }
            });
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Lich")) {
            final SpellAbility loseAllLife = new Ability(card, "0") {
                @Override
                public void resolve() {
                    final int life = card.getController().getLife();
                    card.getController().loseLife(life, card);
                }
            };

            final Command intoPlay = new Command() {
                private static final long serialVersionUID = 1337794055075168785L;

                @Override
                public void execute() {

                    final StringBuilder sb = new StringBuilder();
                    sb.append(cardName).append(" - ").append(card.getController());
                    sb.append(" loses life equal to his or her life total.");
                    loseAllLife.setStackDescription(sb.toString());

                    Singletons.getModel().getGameState().getStack().addSimultaneousStackEntry(loseAllLife);

                }
            };

            final SpellAbility loseGame = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.getController().loseConditionMet(GameLossReason.SpellEffect, card.getName());
                }
            };

            final Command toGrave = new Command() {
                private static final long serialVersionUID = 5863295714122376047L;

                @Override
                public void execute() {

                    final StringBuilder sb = new StringBuilder();
                    sb.append(cardName).append(" - ").append(card.getController());
                    sb.append("loses the game.");
                    loseGame.setStackDescription(sb.toString());

                    Singletons.getModel().getGameState().getStack().addSimultaneousStackEntry(loseGame);

                }
            };

            card.addComesIntoPlayCommand(intoPlay);
            card.addDestroyCommand(toGrave);
        } // *************** END ************ END **************************

         // *************** START *********** START **************************
        else if (cardName.equals("Sylvan Library")) {

            final Ability ability = new Ability(card, "") {
                @Override
                public void resolve() {
                    final Player player = card.getController();
                    if (player.isHuman()) {
                        final String cardQuestion = "Pay 4 life and keep in hand?";
                        player.drawCards(2);
                        int numPutBack = 0;
                        for (Card c : player.getCardsIn(ZoneType.Hand)) {
                            if (c.getDrawnThisTurn()) {
                                numPutBack++;
                            }
                        }
                        numPutBack = Math.min(2, numPutBack);
                        for (int i = 0; i < numPutBack; i++) {
                            final StringBuilder sb = new StringBuilder();
                            sb.append(card).append(" - Select a card drawn this turn: ").append(numPutBack - i)
                                .append(" of " + numPutBack);
                            final String prompt = sb.toString();
                            Singletons.getModel().getMatch().getInput().setInput(new Input() {
                                private static final long serialVersionUID = -3389565833121544797L;

                                @Override
                                public void showMessage() {
                                    if (player.getZone(ZoneType.Hand).size() == 0) {
                                        this.stop();
                                    }
                                    CMatchUI.SINGLETON_INSTANCE.showMessage(prompt);
                                    ButtonUtil.disableAll();
                                }

                                @Override
                                public void selectCard(final Card card, final PlayerZone zone) {
                                    if (zone.is(ZoneType.Hand) && card.getDrawnThisTurn()) {
                                        if (player.canPayLife(4) && GameActionUtil.showYesNoDialog(card, cardQuestion)) {
                                            player.payLife(4, card);
                                            // card stays in hand
                                        } else {
                                            Singletons.getModel().getGameAction().moveToLibrary(card);
                                        }
                                        this.stop();
                                    }
                                }
                            }); // end Input
                        }
                    } else {
                        // Computer, but he's too stupid to play this
                    }
                } // resolve
            }; // Ability

            final StringBuilder sb = new StringBuilder();
            sb.append("At the beginning of your draw step, you may draw two additional cards. ");
            sb.append("If you do, choose two cards in your hand drawn this turn. For each of those cards, ");
            sb.append("pay 4 life or put the card on top of your library.");
            ability.setStackDescription(sb.toString());

            final StringBuilder sbTrg = new StringBuilder();
            sbTrg.append("Mode$ Phase | Phase$ Draw | ValidPlayer$ You | OptionalDecider$ You | ");
            sbTrg.append("TriggerZones$ Battlefield | TriggerDescription$ At the beginning of ");
            sbTrg.append("your draw step, you may draw two additional cards. If you do, choose two ");
            sbTrg.append("cards in your hand drawn this turn. For each of those cards, ");
            sbTrg.append("pay 4 life or put the card on top of your library.");
            final Trigger drawStepTrigger = TriggerHandler.parseTrigger(sbTrg.toString(), card, true);

            drawStepTrigger.setOverridingAbility(ability);
            card.addTrigger(drawStepTrigger);
        } // *************** END ************ END **************************
    }

}
