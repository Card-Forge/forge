package forge.card.cardfactory;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardCharactersticName;
import forge.CardList;
import forge.CardUtil;
import forge.Command;
import forge.Singletons;
import forge.card.spellability.Ability;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellPermanent;
import forge.card.trigger.TriggerType;
import forge.control.input.Input;
import forge.control.input.InputPayManaCost;
import forge.game.GameLossReason;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiUtils;
import forge.view.ButtonUtil;

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
    public static Card getCard(final Card card, final String cardName) {

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
                    CardFactoryUtil.makeTokenSaproling(card.getController());
                }

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public boolean canPlay() {
                    final CardList grave = AllZone.getHumanPlayer().getCardsIn(ZoneType.Graveyard);
                    final CardList aiGrave = AllZone.getComputerPlayer().getCardsIn(ZoneType.Graveyard);
                    return ((grave.getType("Creature").size() > 1) || (aiGrave.getType("Creature").size() > 1))
                            && super.canPlay();
                }
            };
            final Input soilTarget = new Input() {

                private boolean once = false;
                private static final long serialVersionUID = 8243511353958609599L;

                @Override
                public void showMessage() {
                    CardList grave = AllZone.getHumanPlayer().getCardsIn(ZoneType.Graveyard);
                    CardList aiGrave = AllZone.getComputerPlayer().getCardsIn(ZoneType.Graveyard);
                    grave = grave.getType("Creature");
                    aiGrave = aiGrave.getType("Creature");

                    if (this.once || ((grave.size() < 2) && (aiGrave.size() < 2))) {
                        this.once = false;
                        this.stop();
                    } else {
                        CardList chooseGrave;
                        if (grave.size() < 2) {
                            chooseGrave = aiGrave;
                        } else if (aiGrave.size() < 2) {
                            chooseGrave = grave;
                        } else {
                            chooseGrave = aiGrave;
                            chooseGrave.addAll(grave);
                        }

                        final Object o = GuiUtils.chooseOne("Choose first creature to exile", chooseGrave.toArray());
                        if (o != null) {
                            CardList newGrave;
                            final Card c = (Card) o;
                            if (c.getOwner().isHuman()) {
                                newGrave = AllZone.getHumanPlayer().getCardsIn(ZoneType.Graveyard);
                            } else {
                                newGrave = AllZone.getComputerPlayer().getCardsIn(ZoneType.Graveyard);
                            }

                            newGrave = newGrave.getType("Creature");
                            newGrave.remove(c);

                            final Object o2 = GuiUtils.chooseOne("Choose second creature to exile", newGrave.toArray());
                            if (o2 != null) {
                                final Card c2 = (Card) o2;
                                newGrave.remove(c2);
                                Singletons.getModel().getGameAction().exile(c);
                                Singletons.getModel().getGameAction().exile(c2);
                                this.once = true;

                                AllZone.getStack().addSimultaneousStackEntry(nightSoil);

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
                    final CardList compCreats = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());
                    final CardList humCreats = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());

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

                    AllZone.getStack().addSimultaneousStackEntry(loseAllLife);

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

                    AllZone.getStack().addSimultaneousStackEntry(loseGame);

                }
            };

            card.addComesIntoPlayCommand(intoPlay);
            card.addDestroyCommand(toGrave);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Copy Artifact")) {
            final Card[] copyTarget = new Card[1];

            final SpellAbility copy = new Spell(card) {
                private static final long serialVersionUID = 4496978456522751302L;

                @Override
                public void resolve() {
                    if (card.getController().isComputer()) {
                        final CardList cards = AllZoneUtil.getCardsIn(ZoneType.Battlefield).getType("Artifact");
                        if (!cards.isEmpty()) {
                            copyTarget[0] = CardFactoryUtil.getBestAI(cards);
                        }
                    }

                    if (copyTarget[0] != null) {
                        Card cloned;

                        AllZone.getTriggerHandler().suppressMode(TriggerType.Transformed);

                        // TODO: transform back and forth
                        cloned = AbstractCardFactory.getCard2(copyTarget[0], card.getOwner());
                        // TODO: untransform

                        card.addAlternateState(CardCharactersticName.Cloner);
                        card.switchStates(CardCharactersticName.Original, CardCharactersticName.Cloner);
                        card.setState(CardCharactersticName.Original);

                        if (copyTarget[0].getCurState() == CardCharactersticName.Transformed && copyTarget[0].isDoubleFaced()) {
                            cloned.setState(CardCharactersticName.Transformed);
                        }

                        CardFactoryUtil.copyCharacteristics(cloned, card);
                        this.grantExtras();

                        // If target is a flipped card, also copy the flipped
                        // state.
                        if (copyTarget[0].isFlip()) {
                            cloned.setState(CardCharactersticName.Flipped);
                            cloned.setImageFilename(CardUtil.buildFilename(cloned));
                            card.addAlternateState(CardCharactersticName.Flipped);
                            card.setState(CardCharactersticName.Flipped);
                            CardFactoryUtil.copyCharacteristics(cloned, card);
                            this.grantExtras();

                            card.setFlip(true);

                            card.setState(CardCharactersticName.Original);
                        } else {
                            card.setFlip(false);
                        }

                        AllZone.getTriggerHandler().clearSuppression(TriggerType.Transformed);
                    }

                    Singletons.getModel().getGameAction().moveToPlay(card);
                }

                private void grantExtras() {
                    // Grant stuff from specific cloners
                    if (cardName.equals("Copy Artifact")) {
                        card.addType("Enchantment");
                    }

                }
            }; // SpellAbility

            final Input runtime = new Input() {
                private static final long serialVersionUID = 8117808324791871452L;

                @Override
                public void showMessage() {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(cardName).append(" - Select an artifact on the battlefield");
                    Singletons.getControl().getControlMatch().showMessage(sb.toString());
                    ButtonUtil.enableOnlyCancel();
                }

                @Override
                public void selectButtonCancel() {
                    this.stop();
                }

                @Override
                public void selectCard(final Card c, final PlayerZone z) {
                    if (z.is(ZoneType.Battlefield) && c.isArtifact()) {
                        copyTarget[0] = c;
                        this.stopSetNext(new InputPayManaCost(copy));
                    }
                }
            };
            // Do not remove SpellAbilities created by AbilityFactory or
            // Keywords.
            card.clearFirstSpell();
            card.addSpellAbility(copy);
            final StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" - enters the battlefield as a copy of selected card.");
            copy.setStackDescription(sb.toString());
            copy.setBeforePayMana(runtime);
        } // *************** END ************ END **************************

        return card;
    }

}
