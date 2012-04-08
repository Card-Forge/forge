/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.card.cardfactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.swing.JOptionPane;

import com.esotericsoftware.minlog.Log;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardCharactersticName;
import forge.CardList;
import forge.CardListFilter;
import forge.CardUtil;
import forge.Command;
import forge.Constant;
import forge.Counters;
import forge.GameActionUtil;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.cost.Cost;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilityStatic;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellPermanent;
import forge.card.spellability.Target;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerType;
import forge.control.input.Input;
import forge.control.input.InputPayManaCost;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiUtils;
import forge.util.MyRandom;
import forge.view.ButtonUtil;

/**
 * <p>
 * CardFactory_Creatures class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardFactoryCreatures {

    private static void getCard_ForceOfSavagery(final Card card, final String cardName) {
        final SpellAbility spell = new SpellPermanent(card) {
            private static final long serialVersionUID = 1603238129819160467L;

            @Override
            public boolean canPlayAI() {
                final CardList list = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield);

                return list.containsName("Glorious Anthem") || list.containsName("Gaea's Anthem");
            }
        };
        // Do not remove SpellAbilities created by AbilityFactory or
        // Keywords.
        card.clearFirstSpell();
        card.addSpellAbility(spell);
    }

    private static void getCard_GilderBairn(final Card card, final String cardName) {
        final Cost abCost = new Cost(card, "2 GU Untap", true);
        final Target tgt = new Target(card, "Select target permanent.", new String[] { "Permanent" });
        final AbilityActivated a1 = new AbilityActivated(card, abCost, tgt) {
            private static final long serialVersionUID = -1847685865277129366L;

            @Override
            public void resolve() {
                final Card c = this.getTargetCard();

                if (c.sumAllCounters() == 0) {
                    return;
                } else if (AllZoneUtil.isCardInPlay(c) && c.canBeTargetedBy(this)) {
                    // zerker clean up:
                    for (final Counters c1 : Counters.values()) {
                        if (c.getCounters(c1) > 0) {
                            c.addCounter(c1, c.getCounters(c1));
                        }
                    }
                }
            }

            @Override
            public void chooseTargetAI() {
                CardList perms = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield);
                perms = perms.getTargetableCards(this).filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card c) {
                        return (c.sumAllCounters() > 0);
                    }
                });
                perms.shuffle();
                this.setTargetCard(perms.get(0)); // TODO improve this.
            }

            @Override
            public boolean canPlayAI() {
                CardList perms = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield);
                perms = perms.getTargetableCards(this).filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card c) {
                        return (c.sumAllCounters() > 0);
                    }
                });
                return perms.size() > 0;
            }
        }; // SpellAbility

        card.addSpellAbility(a1);
        final StringBuilder sb = new StringBuilder();
        sb.append(abCost);
        sb.append("For each counter on target permanent, ");
        sb.append("put another of those counters on that permanent.");
        a1.setDescription(sb.toString());
    }

    private static void getCard_PrimalPlasma(final Card card, final String cardName) {
        card.setBaseAttack(3);
        card.setBaseDefense(3);
        final SpellAbility ability = new Ability(card, "0") {
            @Override
            public void resolve() {
                String choice = "";
                final String[] choices = { "3/3", "2/2 with flying", "1/6 with defender" };

                if (card.getController().isHuman()) {
                    choice = GuiUtils.chooseOne("Choose one", choices);
                } else {
                    choice = choices[MyRandom.getRandom().nextInt(3)];
                }

                if (choice.equals("2/2 with flying")) {
                    card.setBaseAttack(2);
                    card.setBaseDefense(2);
                    card.addIntrinsicKeyword("Flying");
                }
                if (choice.equals("1/6 with defender")) {
                    card.setBaseAttack(1);
                    card.setBaseDefense(6);
                    card.addIntrinsicKeyword("Defender");
                    card.addType("Wall");
                }

            } // resolve()
        }; // SpellAbility
        final Command intoPlay = new Command() {
            private static final long serialVersionUID = 8957338395786245312L;

            @Override
            public void execute() {
                final StringBuilder sb = new StringBuilder();
                sb.append(card.getName()).append(" - choose: 3/3, 2/2 flying, 1/6 defender");
                ability.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        };
        card.addComesIntoPlayCommand(intoPlay);
    }

    private static void getCard_MinotaurExplorer(final Card card, final String cardName) {
        final SpellAbility creature = new SpellPermanent(card) {
            private static final long serialVersionUID = -7326018877172328480L;

            @Override
            public boolean canPlayAI() {
                int reqHand = 1;
                if (AllZone.getZoneOf(card).is(ZoneType.Hand)) {
                    reqHand++;
                }

                // Don't play if it would sacrifice as soon as it comes into
                // play
                return AllZone.getComputerPlayer().getCardsIn(ZoneType.Hand).size() > reqHand;
            }
        };


        final SpellAbility ability = new Ability(card, "0") {
            @Override
            public void resolve() {
                final CardList hand = card.getController().getCardsIn(ZoneType.Hand);
                if (hand.size() == 0) {
                    Singletons.getModel().getGameAction().sacrifice(card);
                } else {
                    card.getController().discardRandom(this);
                }
            }
        }; // SpellAbility

        final Command intoPlay = new Command() {
            private static final long serialVersionUID = 4986114285467649619L;

            @Override
            public void execute() {
                final StringBuilder sb = new StringBuilder();
                sb.append(card.getController());
                sb.append(" - discards at random or sacrifices ").append(cardName);
                ability.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        };

        card.clearFirstSpell();
        card.addFirstSpellAbility(creature);
        card.addComesIntoPlayCommand(intoPlay);
    }

    private static void getCard_PhylacteryLich(final Card card, final String cardName) {
        final Command intoPlay = new Command() {
            private static final long serialVersionUID = -1601957445498569156L;

            @Override
            public void execute() {

                if (card.getController().isHuman()) {
                    final CardList artifacts = AllZone.getHumanPlayer().getCardsIn(ZoneType.Battlefield)
                            .getType("Artifact");

                    if (artifacts.size() != 0) {
                        Object o;
                        o = GuiUtils.chooseOne("Select an artifact put a phylactery counter on", artifacts.toArray());
                        if (o != null) {
                            final Card c = (Card) o;
                            c.addCounter(Counters.PHYLACTERY, 1);
                        }
                    }

                } else { // computer
                    CardList art = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield);
                    art = art.filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
                            return c.isArtifact();
                        }
                    });

                    CardList list = new CardList(art);
                    list = list.filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
                            return c.getIntrinsicKeyword().contains("Indestructible");
                        }
                    });

                    Card chosen = null;
                    if (!list.isEmpty()) {
                        chosen = list.get(0);
                    } else if (!art.isEmpty()) {
                        chosen = art.get(0);
                    }
                    chosen.addCounter(Counters.PHYLACTERY, 1);
                } // else
            } // execute()
        };
        // Do not remove SpellAbilities created by AbilityFactory or
        // Keywords.
        card.clearFirstSpell();
        card.addSpellAbility(new SpellPermanent(card) {

            private static final long serialVersionUID = -1506199222879057809L;

            @Override
            public boolean canPlayAI() {
                return (!AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield).getType("Artifact").isEmpty() && AllZone
                        .getZoneOf(this.getSourceCard()).is(ZoneType.Hand));
            }
        });
        card.addComesIntoPlayCommand(intoPlay);
    }

    private static void getCard_SkySwallower(final Card card, final String cardName) {
        final SpellAbility ability = new Ability(card, "0") {

            @Override
            public void resolve() {
                // TODO - this needs to be targeted
                final Player opp = card.getController().getOpponent();

                CardList list = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
                list = list.getValidCards("Card.Other+YouCtrl".split(","), card.getController(), card);

                for (final Card c : list) {
                    c.addController(opp);
                }
            } // resolve()
        }; // SpellAbility

        final Command intoPlay = new Command() {
            private static final long serialVersionUID = -453410206437839334L;

            @Override
            public void execute() {
                final StringBuilder sb = new StringBuilder();
                sb.append(card.getController().getOpponent());
                sb.append(" gains control of all other permanents you control");
                ability.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        };
        card.addComesIntoPlayCommand(intoPlay);
    }

    private static void getCard_JhoiraOfTheGhitu(final Card card, final String cardName) {
        final Stack<Card> chosen = new Stack<Card>();
        final SpellAbility ability = new Ability(card, "2") {
            @Override
            public boolean canPlay() {
                CardList possible = card.getController().getCardsIn(ZoneType.Hand);
                possible = possible.filter(CardListFilter.NON_LANDS);
                return !possible.isEmpty() && super.canPlay();
            }

            @Override
            public boolean canPlayAI() {
                return false;
            }

            @Override
            public void resolve() {
                final Card c = chosen.pop();
                c.addCounter(Counters.TIME, 4);
                c.setSuspend(true);
            }
        };

        ability.setAfterPayMana(new Input() {
            private static final long serialVersionUID = -1647181037510967127L;

            @Override
            public void showMessage() {
                ButtonUtil.disableAll();
                Singletons.getControl().getControlMatch().showMessage("Exile a nonland card from your hand.");
            }

            @Override
            public void selectCard(final Card c, final PlayerZone zone) {
                if (zone.is(ZoneType.Hand) && !c.isLand()) {
                    Singletons.getModel().getGameAction().exile(c);
                    chosen.push(c);
                    final StringBuilder sb = new StringBuilder();
                    sb.append(card.toString()).append(" - Suspending ").append(c.toString());
                    ability.setStackDescription(sb.toString());
                    AllZone.getStack().add(ability);
                    this.stop();
                }
            }
        });

        card.addSpellAbility(ability);
    }

    private static void getCard_VedalkenPlotter(final Card card, final String cardName) {
        final Card[] target = new Card[2];
        final int[] index = new int[1];

        final Ability ability = new Ability(card, "") {
            @Override
            public boolean canPlayAI() {
                return false;
            }

            @Override
            public void resolve() {
                final Card crd0 = target[0];
                final Card crd1 = target[1];

                if ((crd0 != null) && (crd1 != null)) {
                    final Player p0 = crd0.getController();
                    final Player p1 = crd1.getController();
                    crd0.addController(p1);
                    crd1.addController(p0);
                }

            } // resolve()
        }; // SpellAbility

        final Input input = new Input() {

            private static final long serialVersionUID = -7143706716256752987L;

            @Override
            public void showMessage() {
                if (index[0] == 0) {
                    Singletons.getControl().getControlMatch().showMessage("Select target land you control.");
                } else {
                    Singletons.getControl().getControlMatch().showMessage("Select target land opponent controls.");
                }

                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                this.stop();
            }

            @Override
            public void selectCard(final Card c, final PlayerZone zone) {
                // must target creature you control
                if ((index[0] == 0) && !c.getController().equals(card.getController())) {
                    return;
                }

                // must target creature you don't control
                if ((index[0] == 1) && c.getController().equals(card.getController())) {
                    return;
                }

                if (c.isLand() && zone.is(ZoneType.Battlefield) && c.canBeTargetedBy(ability)) {
                    // System.out.println("c is: " +c);
                    target[index[0]] = c;
                    index[0]++;
                    this.showMessage();

                    if (index[0] == target.length) {
                        AllZone.getStack().add(ability);
                        this.stop();
                    }
                }
            } // selectCard()
        }; // Input

        final Command comesIntoPlay = new Command() {
            private static final long serialVersionUID = 6513203926272187582L;

            @Override
            public void execute() {
                index[0] = 0;
                if (card.getController().isHuman()) {
                    AllZone.getInputControl().setInput(input);
                }
            }
        };

        final StringBuilder sb = new StringBuilder();
        sb.append(cardName);
        sb.append(" - Exchange control of target land you control ");
        sb.append("and target land an opponent controls.");
        ability.setStackDescription(sb.toString());

        card.addComesIntoPlayCommand(comesIntoPlay);
    }

    private static void getCard_PainterServant(final Card card, final String cardName) {
        final long[] timeStamp = new long[1];
        final String[] color = new String[1];

        final Command comesIntoPlay = new Command() {
            private static final long serialVersionUID = 333134223161L;

            @Override
            public void execute() {
                if (card.getController().isHuman()) {
                    final String[] colors = Constant.Color.ONLY_COLORS;

                    final Object o = GuiUtils.chooseOne("Choose color", colors);
                    color[0] = (String) o;
                } else {
                    // AI chooses the color that appears in the keywords of
                    // the most cards in its deck, hand and on battlefield
                    final CardList list = new CardList();
                    list.addAll(AllZone.getComputerPlayer().getCardsIn(ZoneType.Library));
                    list.addAll(AllZone.getComputerPlayer().getCardsIn(ZoneType.Hand));
                    list.addAll(AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield));

                    color[0] = Constant.Color.WHITE;
                    int max = list.getKeywordsContain(color[0]).size();

                    final String[] colors = { Constant.Color.BLUE, Constant.Color.BLACK, Constant.Color.RED,
                            Constant.Color.GREEN };
                    for (final String c : colors) {
                        final int cmp = list.getKeywordsContain(c).size();
                        if (cmp > max) {
                            max = cmp;
                            color[0] = c;
                        }
                    }
                }
                final ArrayList<String> colors = new ArrayList<String>();
                colors.add(color[0]);
                card.setChosenColor(colors);
                final String s = CardUtil.getShortColor(color[0]);

                timeStamp[0] = AllZone.getColorChanger().addColorChanges(s, card, true, true);
            }
        }; // Command

        final Command leavesBattlefield = new Command() {
            private static final long serialVersionUID = 2559212590399132459L;

            @Override
            public void execute() {
                final String s = CardUtil.getShortColor(color[0]);
                AllZone.getColorChanger().removeColorChanges(s, card, true, timeStamp[0]);
            }
        };

        card.addComesIntoPlayCommand(comesIntoPlay);
        card.addLeavesPlayCommand(leavesBattlefield);
    }

    private static void getCard_Stangg(final Card card, final String cardName) {

        final Ability ability = new Ability(card, "0") {
            @Override
            public void resolve() {
                final CardList cl = CardFactoryUtil.makeToken("Stangg Twin", "RG 3 4 Stangg Twin",
                        card.getController(), "R G", new String[] { "Legendary", "Creature", "Human", "Warrior" },
                        3, 4, new String[] { "" });

                cl.get(0).addLeavesPlayCommand(new Command() {
                    private static final long serialVersionUID = 3367390368512271319L;

                    @Override
                    public void execute() {
                        if (AllZoneUtil.isCardInPlay(card)) {
                            Singletons.getModel().getGameAction().sacrifice(card);
                        }
                    }
                });
            }
        };
        final StringBuilder sb = new StringBuilder();
        sb.append("When Stangg enters the battlefield, if Stangg is on the battlefield, ");
        sb.append("put a legendary 3/4 red and green Human Warrior creature token ");
        sb.append("named Stangg Twin onto the battlefield.");
        ability.setStackDescription(sb.toString());

        card.addComesIntoPlayCommand(new Command() {
            private static final long serialVersionUID = 6667896040611028600L;

            @Override
            public void execute() {
                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        });

        card.addLeavesPlayCommand(new Command() {
            private static final long serialVersionUID = 1786900359843939456L;

            @Override
            public void execute() {
                final CardList list = AllZoneUtil.getCardsIn(ZoneType.Battlefield, "Stangg Twin");

                if (list.size() == 1) {
                    Singletons.getModel().getGameAction().exile(list.get(0));
                }
            }
        });
    }

    private static void getCard_RhysTheRedeemed(final Card card, final String cardName) {
        final Cost abCost = new Cost(card, "4 GW GW T", true);
        final AbilityActivated copyTokens1 = new AbilityActivated(card, abCost, null) {
            private static final long serialVersionUID = 6297992502069547478L;

            @Override
            public void resolve() {
                CardList allTokens = AllZoneUtil.getCreaturesInPlay(card.getController());
                allTokens = allTokens.filter(CardListFilter.TOKEN);

                CardFactoryUtil.copyTokens(allTokens);
            }

            @Override
            public boolean canPlayAI() {
                CardList allTokens = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());
                allTokens = allTokens.filter(CardListFilter.TOKEN);

                return allTokens.size() >= 2;
            }
        };

        card.addSpellAbility(copyTokens1);
        final StringBuilder sbDesc = new StringBuilder();
        sbDesc.append(abCost).append("For each creature token you control, ");
        sbDesc.append("put a token that's a copy of that creature onto the battlefield.");
        copyTokens1.setDescription(sbDesc.toString());

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append(card.getName());
        sbStack.append(" - For each creature token you control, put a token ");
        sbStack.append("that's a copy of that creature onto the battlefield.");
        copyTokens1.setStackDescription(sbStack.toString());
    }

    private static void getCard_TrevaTheRenewer(final Card card, final String cardName) {
        final Player player = card.getController();

        final Ability ability2 = new Ability(card, "2 W") {
            @Override
            public void resolve() {
                int lifeGain = 0;
                if (card.getController().isHuman()) {
                    final String[] choices = { "white", "blue", "black", "red", "green" };
                    final Object o = GuiUtils.chooseOneOrNone("Select Color: ", choices);
                    Log.debug("Treva, the Renewer", "Color:" + o);
                    lifeGain = CardFactoryUtil.getNumberOfPermanentsByColor((String) o);

                } else {
                    final CardList list = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
                    final String color = CardFactoryUtil.getMostProminentColor(list);
                    lifeGain = CardFactoryUtil.getNumberOfPermanentsByColor(color);
                }

                card.getController().gainLife(lifeGain, card);
            }

            @Override
            public boolean canPlay() {
                // this is set to false, since it should only TRIGGER
                return false;
            }
        }; // ability2
           // card.clearSpellAbility();
        card.addSpellAbility(ability2);

        final StringBuilder sb2 = new StringBuilder();
        sb2.append(card.getName()).append(" - ").append(player);
        sb2.append(" gains life equal to permanents of the chosen color.");
        ability2.setStackDescription(sb2.toString());
    }

    private static void getCard_SphinxJwar(final Card card, final String cardName) {
        final SpellAbility ability1 = new Ability(card, "0") {
            @Override
            public void resolve() {
                final Player player = card.getController();
                final PlayerZone lib = player.getZone(ZoneType.Library);

                if (lib.size() < 1) {
                    return;
                }

                final CardList cl = new CardList();
                cl.add(lib.get(0));

                GuiUtils.chooseOneOrNone("Top card", cl.toArray());
            }

            @Override
            public boolean canPlayAI() {
                return false;
            }
        }; // SpellAbility

        final StringBuilder sb1 = new StringBuilder();
        sb1.append(card.getName()).append(" - look at top card of library.");
        ability1.setStackDescription(sb1.toString());

        ability1.setDescription("You may look at the top card of your library.");
        card.addSpellAbility(ability1);
    }

    private static void getCard_MasterOfTheWildHunt(final Card card, final String cardName) {
        final Cost abCost = new Cost(card, "T", true);
        final Target abTgt = new Target(card, "Target a creature to Hunt", "Creature".split(","));
        final AbilityActivated ability = new AbilityActivated(card, abCost, abTgt) {
            private static final long serialVersionUID = 35050145102566898L;

            @Override
            public boolean canPlayAI() {
                CardList wolves = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield);
                wolves = wolves.getType("Wolf");

                wolves = wolves.filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card c) {
                        return c.isUntapped() && c.isCreature();
                    }
                });
                int power = 0;
                for (int i = 0; i < wolves.size(); i++) {
                    power += wolves.get(i).getNetAttack();
                }

                if (power == 0) {
                    return false;
                }

                final int totalPower = power;

                CardList targetables = AllZone.getHumanPlayer().getCardsIn(ZoneType.Battlefield);

                targetables = targetables.getTargetableCards(this).filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card c) {
                        return c.isCreature() && (c.getNetDefense() <= totalPower);
                    }
                });

                if (targetables.size() == 0) {
                    return false;
                }

                this.getTarget().resetTargets();
                this.setTargetCard(CardFactoryUtil.getBestCreatureAI(targetables));

                return true;
            }

            @Override
            public void resolve() {
                CardList wolves = card.getController().getCardsIn(ZoneType.Battlefield);
                wolves = wolves.getType("Wolf");

                wolves = wolves.filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card c) {
                        return c.isUntapped() && c.isCreature();
                    }
                });

                final Card target = this.getTargetCard();

                if (wolves.size() == 0) {
                    return;
                }

                if (!(target.canBeTargetedBy(this) && AllZoneUtil.isCardInPlay(target))) {
                    return;
                }

                for (final Card c : wolves) {
                    c.tap();
                    target.addDamage(c.getNetAttack(), c);
                }

                if (target.getController().isHuman()) { // Human choose
                                                        // spread damage
                    for (int x = 0; x < target.getNetAttack(); x++) {
                        AllZone.getInputControl().setInput(
                                CardFactoryUtil.masterOfTheWildHuntInputTargetCreature(this, wolves, new Command() {
                                    private static final long serialVersionUID = -328305150127775L;

                                    @Override
                                    public void execute() {
                                        getTargetCard().addDamage(1, target);
                                        Singletons.getModel().getGameAction().checkStateEffects();
                                    }
                                }));
                    }
                } else { // AI Choose spread Damage
                    final CardList damageableWolves = wolves.filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
                            return (c.predictDamage(target.getNetAttack(), target, false) > 0);
                        }
                    });

                    if (damageableWolves.size() == 0) {
                        // can't damage
                        // anything
                        return;
                    }

                    CardList wolvesLeft = damageableWolves.filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
                            return !c.hasKeyword("Indestructible");
                        }
                    });

                    for (int i = 0; i < target.getNetAttack(); i++) {
                        wolvesLeft = wolvesLeft.filter(new CardListFilter() {
                            @Override
                            public boolean addCard(final Card c) {
                                return (c.getKillDamage() > 0)
                                        && ((c.getKillDamage() <= target.getNetAttack()) || target
                                                .hasKeyword("Deathtouch"));
                            }
                        });

                        // Kill Wolves that can be killed first
                        if (wolvesLeft.size() > 0) {
                            final Card best = CardFactoryUtil.getBestCreatureAI(wolvesLeft);
                            best.addDamage(1, target);
                            if ((best.getKillDamage() <= 0) || target.hasKeyword("Deathtouch")) {
                                wolvesLeft.remove(best);
                            }
                        } else {
                            // Add -1/-1s to Random Indestructibles
                            if (target.hasKeyword("Infect") || target.hasKeyword("Wither")) {
                                final CardList indestructibles = damageableWolves.filter(new CardListFilter() {
                                    @Override
                                    public boolean addCard(final Card c) {
                                        return c.hasKeyword("Indestructible");
                                    }
                                });
                                indestructibles.shuffle();
                                indestructibles.get(0).addDamage(1, target);
                            }

                            // Then just add Damage randomnly

                            else {
                                damageableWolves.shuffle();
                                wolves.get(0).addDamage(1, target);
                            }
                        }
                    }
                }
            } // resolve()
        }; // SpellAbility

        final StringBuilder sb = new StringBuilder();
        sb.append("Tap: Tap all untapped Wolf creatures you control. ");
        sb.append("Each Wolf tapped this way deals damage equal to its ");
        sb.append("power to target creature. That creature deals damage ");
        sb.append("equal to its power divided as its controller ");
        sb.append("chooses among any number of those Wolves.");
        ability.setDescription(sb.toString());

        card.addSpellAbility(ability);
    }

    private static void getCard_ApocalypseHydra(final Card card, final String cardName) {
        final SpellAbility spell = new SpellPermanent(card) {
            private static final long serialVersionUID = -11489323313L;

            /*
             * @Override public boolean canPlayAI() { return super.canPlay()
             * && (5 <= (ComputerUtil.getAvailableMana().size() - 2)); }
             */

            @Override
            public void resolve() {
                int xCounters = card.getXManaCostPaid();
                final Card c = Singletons.getModel().getGameAction().moveToPlay(this.getSourceCard());

                if (xCounters >= 5) {
                    xCounters = 2 * xCounters;
                }
                c.addCounter(Counters.P1P1, xCounters);
            }
        };
        // Do not remove SpellAbilities created by AbilityFactory or
        // Keywords.
        card.clearFirstSpell();
        card.addSpellAbility(spell);
    }

    private static void getCard_MoltenHydra(final Card card, final String cardName) {
        final Target target = new Target(card, "TgtCP");
        final Cost abCost = new Cost(card, "T", true);
        final AbilityActivated ability2 = new AbilityActivated(card, abCost, target) {
            private static final long serialVersionUID = 2626619319289064289L;

            @Override
            public boolean canPlay() {
                return (card.getCounters(Counters.P1P1) > 0) && super.canPlay();
            }

            @Override
            public boolean canPlayAI() {
                return this.getCreature().size() != 0;
            }

            @Override
            public void chooseTargetAI() {
                if (AllZone.getHumanPlayer().getLife() < card.getCounters(Counters.P1P1)) {
                    this.setTargetPlayer(AllZone.getHumanPlayer());
                } else {
                    final CardList list = this.getCreature();
                    list.shuffle();
                    this.setTargetCard(list.get(0));
                }
            } // chooseTargetAI()

            CardList getCreature() {

                // toughness of 1
                CardList list = CardFactoryUtil.getHumanCreatureAI(card.getCounters(Counters.P1P1), this, true);
                list = list.filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card c) {
                        final int total = card.getCounters(Counters.P1P1);
                        return (total >= c.getKillDamage());
                    }
                });
                return list;
            } // getCreature()

            @Override
            public void resolve() {
                final int total = card.getCounters(Counters.P1P1);
                if (this.getTargetCard() != null) {
                    if (AllZoneUtil.isCardInPlay(this.getTargetCard())
                            && this.getTargetCard().canBeTargetedBy(this)) {
                        this.getTargetCard().addDamage(total, card);
                    }
                } else {
                    this.getTargetPlayer().addDamage(total, card);
                }
                card.subtractCounter(Counters.P1P1, total);
            } // resolve()
        }; // SpellAbility

        card.addSpellAbility(ability2);

        final StringBuilder sbDesc = new StringBuilder();
        sbDesc.append(abCost).append("Remove all +1/+1 counters from ");
        sbDesc.append(cardName).append(":  ").append(cardName);
        sbDesc.append(" deals damage to target creature or player equal to the ");
        sbDesc.append("number of +1/+1 counters removed this way.");
        ability2.setDescription(sbDesc.toString());

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append("Molten Hydra deals damage to number of +1/+1 ");
        sbStack.append("counters on it to target creature or player.");
        ability2.setStackDescription(sbStack.toString());
    }

    private static void getCard_AcademyRector(final Card card, final String cardName) {
        final SpellAbility ability = new Ability(card, "0") {

            @Override
            public void resolve() {

                if (card.getController().isHuman()) {
                    final StringBuilder question = new StringBuilder();
                    if (card.getName().equals("Academy Rector")) {
                        question.append("Exile ").append(card.getName()).append(" and place ");
                    } else {
                        question.append("Place ");
                    }
                    question.append("an enchantment from your library onto the battlefield?");

                    if (GameActionUtil.showYesNoDialog(card, question.toString())) {
                        if (card.getName().equals("Academy Rector")) {
                            Singletons.getModel().getGameAction().exile(card);
                        }
                        CardList list = AllZone.getHumanPlayer().getCardsIn(ZoneType.Library);
                        list = list.getType("Enchantment");

                        if (list.size() > 0) {
                            final Object objectSelected = GuiUtils.chooseOneOrNone("Choose an enchantment",
                                    list.toArray());

                            if (objectSelected != null) {

                                final Card c = (Card) objectSelected;
                                Singletons.getModel().getGameAction().moveToPlay(c);

                                if (c.isAura()) {

                                    final String[] enchantThisType = { "" };
                                    final String[] message = { "" };

                                    // The type following "Enchant" maybe
                                    // upercase or lowercase, cardsfolder
                                    // has both
                                    // Note that I am being overly cautious.

                                    if (c.hasKeyword("Enchant creature without flying")
                                            || c.hasKeyword("Enchant Creature without flying")) {
                                        enchantThisType[0] = "Creature.withoutFlying";
                                        message[0] = "Select a creature without flying";
                                    } else if (c.hasKeyword("Enchant creature with converted mana cost 2 or less")
                                            || c.hasKeyword("Enchant Creature with "
                                                    + "converted mana cost 2 or less")) {
                                        enchantThisType[0] = "Creature.cmcLE2";
                                        message[0] = "Select a creature with converted mana cost 2 or less";
                                    } else if (c.hasKeyword("Enchant red or green creature")) {
                                        enchantThisType[0] = "Creature.Red,Creature.Green";
                                        message[0] = "Select a red or green creature";
                                    } else if (c.hasKeyword("Enchant tapped creature")) {
                                        enchantThisType[0] = "Creature.tapped";
                                        message[0] = "Select a tapped creature";
                                    } else if (c.hasKeyword("Enchant creature") || c.hasKeyword("Enchant Creature")) {
                                        enchantThisType[0] = "Creature";
                                        message[0] = "Select a creature";
                                    } else if (c.hasKeyword("Enchant wall") || c.hasKeyword("Enchant Wall")) {
                                        enchantThisType[0] = "Wall";
                                        message[0] = "Select a Wall";
                                    } else if (c.hasKeyword("Enchant land you control")
                                            || c.hasKeyword("Enchant Land you control")) {
                                        enchantThisType[0] = "Land.YouCtrl";
                                        message[0] = "Select a land you control";
                                    } else if (c.hasKeyword("Enchant land") || c.hasKeyword("Enchant Land")) {
                                        enchantThisType[0] = "Land";
                                        message[0] = "Select a land";
                                    } else if (c.hasKeyword("Enchant artifact") || c.hasKeyword("Enchant Artifact")) {
                                        enchantThisType[0] = "Artifact";
                                        message[0] = "Select an artifact";
                                    } else if (c.hasKeyword("Enchant enchantment")
                                            || c.hasKeyword("Enchant Enchantment")) {
                                        enchantThisType[0] = "Enchantment";
                                        message[0] = "Select an enchantment";
                                    }

                                    final CardList allCards = AllZoneUtil.getCardsIn(ZoneType.Battlefield);

                                    // Make sure that we were able to match
                                    // the selected aura with our list of
                                    // criteria

                                    if ((enchantThisType[0] != "") && (message[0] != "")) {

                                        final CardList choices = allCards.getValidCards(enchantThisType[0],
                                                card.getController(), card);
                                        final String msg = message[0];

                                        AllZone.getInputControl().setInput(new Input() {
                                            private static final long serialVersionUID = -6271957194091955059L;

                                            @Override
                                            public void showMessage() {
                                                Singletons.getControl().getControlMatch().showMessage(msg);
                                                ButtonUtil.enableOnlyOK();
                                            }

                                            @Override
                                            public void selectButtonOK() {
                                                this.stop();
                                            }

                                            @Override
                                            public void selectCard(final Card card, final PlayerZone zone) {
                                                if (choices.contains(card)) {

                                                    if (AllZoneUtil.isCardInPlay(card)) {
                                                        c.enchantEntity(card);
                                                        this.stop();
                                                    }
                                                }
                                            } // selectCard()
                                        }); // Input()

                                    } // if we were able to match the
                                      // selected aura with our list of
                                      // criteria
                                } // If enchantment selected is an aura
                            } // If an enchantment is selected
                        } // If there are enchantments in library

                        card.getController().shuffle();
                    } // If answered yes to may exile
                } // If player is human

                // player is the computer
                else {
                    CardList list = AllZone.getComputerPlayer().getCardsIn(ZoneType.Library);
                    list = list.filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
                            return c.isEnchantment() && !c.isAura();
                        }
                    });

                    if (list.size() > 0) {
                        final Card c = CardFactoryUtil.getBestEnchantmentAI(list, this, false);

                        Singletons.getModel().getGameAction().moveToPlay(c);
                        if (card.getName().equals("Academy Rector")) {
                            Singletons.getModel().getGameAction().exile(card);
                        }
                        card.getController().shuffle();
                    }
                } // player is the computer
            } // resolve()
        }; // ability

        final StringBuilder sb = new StringBuilder();
        if (card.getName().equals("Academy Rector")) {
            sb.append("Academy Rector - ").append(card.getController());
            sb.append(" may exile this card and place an enchantment ");
            sb.append("from his library onto the battlefield.");
        } else {
            sb.append("Lost Auramancers - ").append(card.getController());
            sb.append(" may place an enchantment from his library onto the battlefield.");
        }
        ability.setStackDescription(sb.toString());

        final Command destroy = new Command() {
            private static final long serialVersionUID = -4352349741511065318L;

            @Override
            public void execute() {

                if (card.getName().equals("Lost Auramancers") && (card.getCounters(Counters.TIME) <= 0)) {
                    AllZone.getStack().addSimultaneousStackEntry(ability);

                } else if (card.getName().equals("Academy Rector")) {
                    AllZone.getStack().addSimultaneousStackEntry(ability);

                }

            } // execute()
        }; // Command destroy

        card.addDestroyCommand(destroy);
    }

    private static void getCard_KinsbaileBorderguard(final Card card, final String cardName) {
        final SpellAbility ability = new Ability(card, "0") {
            @Override
            public void resolve() {
                card.addCounter(Counters.P1P1, this.countKithkin());
                // System.out.println("all counters: "
                // +card.sumAllCounters());
            } // resolve()

            public int countKithkin() {
                CardList kithkin = card.getController().getCardsIn(ZoneType.Battlefield);
                kithkin = kithkin.filter(new CardListFilter() {

                    @Override
                    public boolean addCard(final Card c) {
                        return (c.isType("Kithkin")) && !c.equals(card);
                    }

                });
                return kithkin.size();

            }
        };
        final Command intoPlay = new Command() {
            private static final long serialVersionUID = -7067218066522935060L;

            @Override
            public void execute() {
                final StringBuilder sb = new StringBuilder();
                sb.append("Kinsbaile Borderguard enters the battlefield with a ");
                sb.append("+1/+1 counter on it for each other Kithkin you control.");
                ability.setStackDescription(sb.toString());
                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        };

        final SpellAbility ability2 = new Ability(card, "0") {
            @Override
            public void resolve() {
                for (int i = 0; i < card.sumAllCounters(); i++) {
                    this.makeToken();
                }
            } // resolve()

            public void makeToken() {
                CardFactoryUtil.makeToken("Kithkin Soldier", "W 1 1 Kithkin Soldier", card.getController(), "W",
                        new String[] { "Creature", "Kithkin", "Soldier" }, 1, 1, new String[] { "" });
            }
        };

        final Command destroy = new Command() {
            private static final long serialVersionUID = 304026662487997331L;

            @Override
            public void execute() {
                final StringBuilder sb = new StringBuilder();
                sb.append("When Kinsbaile Borderguard is put into a graveyard ");
                sb.append("from play, put a 1/1 white Kithkin Soldier creature ");
                sb.append("token onto the battlefield for each counter on it.");
                ability2.setStackDescription(sb.toString());
                AllZone.getStack().addSimultaneousStackEntry(ability2);

            }
        };

        card.addComesIntoPlayCommand(intoPlay);
        card.addDestroyCommand(destroy);
    }

    private static void getCard_MultikickerP1P1(final Card card, final String cardName) {
        final AbilityStatic ability = new AbilityStatic(card, "0") {
            @Override
            public void resolve() {
                card.addCounter(Counters.P1P1, card.getMultiKickerMagnitude());
                card.setMultiKickerMagnitude(0);
            }
        };
        final StringBuilder sb = new StringBuilder();
        sb.append(cardName);
        sb.append(" enters the battlefield with a +1/+1 counter ");
        sb.append("on it for each time it was kicked.");
        ability.setStackDescription(sb.toString());

        final Command comesIntoPlay = new Command() {
            private static final long serialVersionUID = 4245563898487609274L;

            @Override
            public void execute() {
                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        };
        card.addComesIntoPlayCommand(comesIntoPlay);
    }

    private static void getCard_VampireHexmage(final Card card, final String cardName) {
        /*
         * Sacrifice Vampire Hexmage: Remove all counters from target
         * permanent.
         */

        final Cost cost = new Cost(card, "Sac<1/CARDNAME>", true);
        final Target tgt = new Target(card, "Select a permanent", "Permanent".split(","));
        final SpellAbility ability = new AbilityActivated(card, cost, tgt) {
            private static final long serialVersionUID = -5084369399105353155L;

            @Override
            public boolean canPlayAI() {

                // Dark Depths:
                CardList list = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield, "Dark Depths");
                list = list.filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card crd) {
                        return crd.getCounters(Counters.ICE) >= 3;
                    }
                });

                if (list.size() > 0) {
                    tgt.addTarget(list.get(0));
                    return true;
                }

                // Get rid of Planeswalkers:
                list = AllZone.getHumanPlayer().getCardsIn(ZoneType.Battlefield);
                list = list.filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card crd) {
                        return crd.isPlaneswalker() && (crd.getCounters(Counters.LOYALTY) >= 5);
                    }
                });

                if (list.size() > 0) {
                    tgt.addTarget(list.get(0));
                    return true;
                }

                return false;
            }

            @Override
            public void resolve() {
                final Card c = this.getTargetCard();
                for (final Counters counter : Counters.values()) {
                    if (c.getCounters(counter) > 0) {
                        c.setCounter(counter, 0, false);
                    }
                }
            }
        };
        card.addSpellAbility(ability);
    }

    private static void getCard_SurturedGhoul(final Card card, final String cardName) {
        final int[] numCreatures = new int[1];
        final int[] sumPower = new int[1];
        final int[] sumToughness = new int[1];

        final Command intoPlay = new Command() {
            private static final long serialVersionUID = -75234586897814L;

            @Override
            public void execute() {
                int intermSumPower = 0;
                int intermSumToughness = 0;
                // intermSumPower = intermSumToughness = 0;
                CardList creats = card.getController().getCardsIn(ZoneType.Graveyard);
                creats = creats.filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card c) {
                        return c.isCreature() && !c.equals(card);
                    }
                });

                if (card.getController().isHuman()) {
                    if (creats.size() > 0) {
                        final List<Card> selection = GuiUtils.chooseNoneOrMany("Select creatures to sacrifice",
                                creats.toArray());

                        numCreatures[0] = selection.size();
                        for (int m = 0; m < selection.size(); m++) {
                            intermSumPower += selection.get(m).getBaseAttack();
                            intermSumToughness += selection.get(m).getBaseDefense();
                            Singletons.getModel().getGameAction().exile(selection.get(m));
                        }
                    }

                } // human
                else {
                    int count = 0;
                    for (int i = 0; i < creats.size(); i++) {
                        final Card c = creats.get(i);
                        if ((c.getNetAttack() <= 2) && (c.getNetDefense() <= 3)) {
                            intermSumPower += c.getBaseAttack();
                            intermSumToughness += c.getBaseDefense();
                            Singletons.getModel().getGameAction().exile(c);
                            count++;
                        }
                        // is this needed?
                        AllZone.getComputerPlayer().getZone(ZoneType.Battlefield).updateObservers();
                    }
                    numCreatures[0] = count;
                }
                sumPower[0] = intermSumPower;
                sumToughness[0] = intermSumToughness;
                card.setBaseAttack(sumPower[0]);
                card.setBaseDefense(sumToughness[0]);
            }
        };
        // Do not remove SpellAbilities created by AbilityFactory or
        // Keywords.
        card.clearFirstSpell();
        card.addComesIntoPlayCommand(intoPlay);
        card.addSpellAbility(new SpellPermanent(card) {
            private static final long serialVersionUID = 304885517082977723L;

            @Override
            public boolean canPlayAI() {
                // get all creatures
                CardList list = AllZone.getComputerPlayer().getCardsIn(ZoneType.Graveyard);
                list = list.filter(CardListFilter.CREATURES);
                return 0 < list.size();
            }
        });
    }

    private static void getCard_NamelessRace(final Card card, final String cardName) {
        /*
         * As Nameless Race enters the battlefield, pay any amount of life.
         * The amount you pay can't be more than the total number of white
         * nontoken permanents your opponents control plus the total number
         * of white cards in their graveyards. Nameless Race's power and
         * toughness are each equal to the life paid as it entered the
         * battlefield.
         */
        final SpellAbility ability = new Ability(card, "0") {
            @Override
            public void resolve() {
                final Player player = card.getController();
                final Player opp = player.getOpponent();
                int max = 0;
                CardList play = opp.getCardsIn(ZoneType.Battlefield);
                play = play.filter(CardListFilter.NON_TOKEN);
                play = play.filter(CardListFilter.WHITE);
                max += play.size();

                CardList grave = opp.getCardsIn(ZoneType.Graveyard);
                grave = grave.filter(CardListFilter.WHITE);
                max += grave.size();

                final String[] life = new String[max + 1];
                for (int i = 0; i <= max; i++) {
                    life[i] = String.valueOf(i);
                }

                final Object o = GuiUtils.chooseOne("Nameless Race - pay X life", life);
                final String answer = (String) o;
                int loseLife = 0;
                try {
                    loseLife = Integer.parseInt(answer.trim());
                } catch (final NumberFormatException nfe) {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(card.getName());
                    sb.append(" - NumberFormatException: ");
                    sb.append(nfe.getMessage());
                    System.out.println(sb.toString());
                }

                card.setBaseAttack(loseLife);
                card.setBaseDefense(loseLife);

                player.loseLife(loseLife, card);
            } // resolve()
        }; // SpellAbility

        final Command intoPlay = new Command() {
            private static final long serialVersionUID = 931101364538995898L;

            @Override
            public void execute() {
                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        };

        final StringBuilder sb = new StringBuilder();
        sb.append(cardName).append(" - pay any amount of life.");
        ability.setStackDescription(sb.toString());

        card.addComesIntoPlayCommand(intoPlay);
    }

    private static void getCard_PhyrexianScuta(final Card card, final String cardName) {
        final Cost abCost = new Cost(card, "3 B PayLife<3>", false);
        final SpellAbility kicker = new Spell(card, abCost, null) {
            private static final long serialVersionUID = -6420757044982294960L;

            @Override
            public void resolve() {
                card.setKicked(true);
                Singletons.getModel().getGameAction().moveToPlay(card);
                card.addCounterFromNonEffect(Counters.P1P1, 2);
            }

            @Override
            public boolean canPlay() {
                return super.canPlay() && (card.getController().getLife() >= 3);
            }

        };
        kicker.setKickerAbility(true);
        kicker.setManaCost("3 B");
        kicker.setDescription("Kicker - Pay 3 life.");

        final StringBuilder sb = new StringBuilder();
        sb.append(card.getName()).append(" - Creature 3/3 (Kicked)");
        kicker.setStackDescription(sb.toString());

        card.addSpellAbility(kicker);
    }

    private static void getCard_YoseiTheMorningStar(final Card card, final String cardName) {
        final CardList targetPerms = new CardList();
        final SpellAbility ability = new Ability(card, "0") {
            @Override
            public void resolve() {
                final Player p = this.getTargetPlayer();
                if (p.canBeTargetedBy(this)) {
                    p.setSkipNextUntap(true);
                    for (final Card c : targetPerms) {
                        if (AllZoneUtil.isCardInPlay(c) && c.canBeTargetedBy(this)) {
                            c.tap();
                        }
                    }
                }
                targetPerms.clear();
            } // resolve()
        };

        final Input targetInput = new Input() {
            private static final long serialVersionUID = -8727869672234802473L;

            @Override
            public void showMessage() {
                if (targetPerms.size() == 5) {
                    this.done();
                }
                final StringBuilder sb = new StringBuilder();
                sb.append("Select up to 5 target permanents.  Selected (");
                sb.append(targetPerms.size()).append(") so far.  Click OK when done.");
                Singletons.getControl().getControlMatch().showMessage(sb.toString());
                ButtonUtil.enableOnlyOK();
            }

            @Override
            public void selectButtonOK() {
                this.done();
            }

            private void done() {
                // here, we add the ability to the stack since it's
                // triggered.
                final StringBuilder sb = new StringBuilder();
                sb.append(card.getName());
                sb.append(" - tap up to 5 permanents target player controls. ");
                sb.append("Target player skips his or her next untap step.");
                ability.setStackDescription(sb.toString());

                //adding ability to stack first cause infinite loop (with observers notification)
                //so it has to be stop first and add ability later
                this.stop();
                AllZone.getStack().add(ability);
            }

            @Override
            public void selectCard(final Card c, final PlayerZone zone) {
                if (zone.is(ZoneType.Battlefield, ability.getTargetPlayer()) && !targetPerms.contains(c)) {
                    if (c.canBeTargetedBy(ability)) {
                        targetPerms.add(c);
                    }
                }
                this.showMessage();
            }
        }; // Input

        final Input playerInput = new Input() {
            private static final long serialVersionUID = 4765535692144126496L;

            @Override
            public void showMessage() {
                final StringBuilder sb = new StringBuilder();
                sb.append(card.getName()).append(" - Select target player");
                Singletons.getControl().getControlMatch().showMessage(sb.toString());
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectPlayer(final Player p) {
                if (p.canBeTargetedBy(ability)) {
                    ability.setTargetPlayer(p);
                    this.stopSetNext(targetInput);
                }
            }

            @Override
            public void selectButtonCancel() {
                this.stop();
            }
        };

        final Command destroy = new Command() {
            private static final long serialVersionUID = -3868616119471172026L;

            @Override
            public void execute() {
                final Player player = card.getController();
                final CardList list = CardFactoryUtil.getHumanCreatureAI(ability, true);

                if (player.isHuman()) {
                    AllZone.getInputControl().setInput(playerInput);
                } else if (list.size() != 0) {
                    final Card target = CardFactoryUtil.getBestCreatureAI(list);
                    ability.setTargetCard(target);
                    AllZone.getStack().addSimultaneousStackEntry(ability);

                }
            } // execute()
        };
        card.addDestroyCommand(destroy);
    }

    private static void getCard_PhyrexianDreadnought(final Card card, final String cardName) {
        final Player player = card.getController();
        final CardList toSac = new CardList();

        final Ability sacOrSac = new Ability(card, "") {
            @Override
            public void resolve() {
                if (player.isHuman()) {
                    final Input target = new Input() {
                        private static final long serialVersionUID = 2698036349873486664L;

                        @Override
                        public void showMessage() {
                            String toDisplay = cardName + " - Select any number of creatures to sacrifice.  ";
                            toDisplay += "Currently, (" + toSac.size() + ") selected with a total power of: "
                                    + getTotalPower();
                            toDisplay += "  Click OK when Done.";
                            Singletons.getControl().getControlMatch().showMessage(toDisplay);
                            ButtonUtil.enableAll();
                        }

                        @Override
                        public void selectButtonOK() {
                            this.done();
                        }

                        @Override
                        public void selectButtonCancel() {
                            toSac.clear();
                            Singletons.getModel().getGameAction().sacrifice(card);
                            this.stop();
                        }

                        @Override
                        public void selectCard(final Card c, final PlayerZone zone) {
                            if (c.isCreature() && zone.is(ZoneType.Battlefield, AllZone.getHumanPlayer())
                                    && !toSac.contains(c)) {
                                toSac.add(c);
                            }
                            this.showMessage();
                        } // selectCard()

                        private void done() {
                            if (getTotalPower() >= 12) {
                                for (final Card sac : toSac) {
                                    Singletons.getModel().getGameAction().sacrifice(sac);
                                }
                            } else {
                                Singletons.getModel().getGameAction().sacrifice(card);
                            }
                            toSac.clear();
                            this.stop();
                        }
                    }; // Input
                    AllZone.getInputControl().setInput(target);
                }
            } // end resolve

            private int getTotalPower() {
                int sum = 0;
                for (final Card c : toSac) {
                    sum += c.getNetAttack();
                }
                return sum;
            }
        }; // end sacOrSac

        final Command comesIntoPlay = new Command() {
            private static final long serialVersionUID = 7680692311339496770L;

            @Override
            public void execute() {
                final StringBuilder sb = new StringBuilder();
                sb.append("When ").append(cardName);
                sb.append(" enters the battlefield, sacrifice it unless you sacrifice ");
                sb.append("any number of creatures with total power 12 or greater.");
                sacOrSac.setStackDescription(sb.toString());
                AllZone.getStack().addSimultaneousStackEntry(sacOrSac);

            }
        };

        card.addComesIntoPlayCommand(comesIntoPlay);
    }

    private static void getCard_ClonesSeries(final Card card, final String cardName) {
        final Card[] copyTarget = new Card[1];

        final SpellAbility copy = new Spell(card) {
            private static final long serialVersionUID = 4496978456522751302L;

            @Override
            public void resolve() {
                String imageFileName = card.getImageFilename();

                if (card.getController().isComputer()) {
                    final CardList creatures = AllZoneUtil.getCreaturesInPlay();
                    if (!creatures.isEmpty()) {
                        copyTarget[0] = CardFactoryUtil.getBestCreatureAI(creatures);
                    }
                }

                if (copyTarget[0] != null) {
                    Card cloned;

                    AllZone.getTriggerHandler().suppressMode(TriggerType.Transformed);

                    if (copyTarget[0].isToken()) {
                        cloned = CardFactoryUtil.copyStats(copyTarget[0]);

                        cloned.setName(copyTarget[0].getName());
                        cloned.setImageName(copyTarget[0].getImageName());

                        cloned.setOwner(this.getActivatingPlayer());
                        cloned.addController(this.getActivatingPlayer());

                        cloned.setManaCost(copyTarget[0].getManaCost());
                        cloned.setColor(copyTarget[0].getColor());
                        cloned.setToken(true);

                        cloned.setType(copyTarget[0].getType());

                        cloned.setBaseAttack(copyTarget[0].getBaseAttack());
                        cloned.setBaseDefense(copyTarget[0].getBaseDefense());
                    }
                    else {
                        Card origin = copyTarget[0];
                        // TODO: transform back before copying
                        cloned = AbstractCardFactory.getCard2(origin, card.getOwner());
                        // TODO: transform origin back to how it was (if needed)
                    }
                    card.addAlternateState(CardCharactersticName.Cloner);
                    card.switchStates(CardCharactersticName.Original, CardCharactersticName.Cloner);
                    card.setState(CardCharactersticName.Original);

                    if (copyTarget[0].getCurState() == CardCharactersticName.Transformed && copyTarget[0].isDoubleFaced()) {
                        cloned.setState(CardCharactersticName.Transformed);
                    }

                    CardFactoryUtil.copyCharacteristics(cloned, card);
                    CardFactoryUtil.addAbilityFactoryAbilities(card);
                    for (int i = 0; i < card.getStaticAbilityStrings().size(); i++) {
                        card.addStaticAbility(card.getStaticAbilityStrings().get(i));
                    }
                    this.grantExtras();

                    // If target is a flipped card, also copy the flipped
                    // state.
                    if (copyTarget[0].isFlip()) {
                        cloned.setState(CardCharactersticName.Flipped);
                        cloned.setImageFilename(CardUtil.buildFilename(cloned));
                        card.addAlternateState(CardCharactersticName.Flipped);
                        card.setState(CardCharactersticName.Flipped);
                        CardFactoryUtil.copyCharacteristics(cloned, card);
                        CardFactoryUtil.addAbilityFactoryAbilities(card);
                        for (int i = 0; i < card.getStaticAbilityStrings().size(); i++) {
                            card.addStaticAbility(card.getStaticAbilityStrings().get(i));
                        }
                        this.grantExtras();

                        card.setFlip(true);

                        card.setState(CardCharactersticName.Original);
                    } else {
                        card.setFlip(false);
                    }

                    AllZone.getTriggerHandler().clearSuppression(TriggerType.Transformed);

                }

                //keep the Clone card image for the cloned card
                card.setImageFilename(imageFileName);
                Singletons.getModel().getGameAction().moveToPlay(card);
            }

            private void grantExtras() {
                // Grant stuff from specific cloners
                if (cardName.equals("Vesuvan Doppelganger")) {
                    final String keyword = "At the beginning of your upkeep, you may have this "
                            + "creature become a copy of target creature except it doesn't copy that "
                            + "creature's color. If you do, this creature gains this ability.";
                    card.addIntrinsicKeyword(keyword);
                    card.addColor("U");
                } else if (cardName.equals("Quicksilver Gargantuan")) {
                    card.setBaseAttack(7);
                    card.setBaseDefense(7);
                } else if (cardName.equals("Phyrexian Metamorph")) {
                    card.addType("Artifact");
                } else if (cardName.equals("Phantasmal Image")) {
                    final Trigger t = forge.card.trigger.TriggerHandler
                            .parseTrigger(
                                    "Mode$ BecomesTarget | ValidTarget$ Card.Self | Execute$ TrigSac | TriggerDescription$ When this creature becomes the target of a spell or ability, sacrifice it.",
                                    card, true);
                    card.addTrigger(t);
                    card.setSVar("TrigSac", "AB$Sacrifice | Cost$ 0 | Defined$ Self");
                    card.addType("Illusion");
                } else if (cardName.equals("Evil Twin")) {
                    final AbilityFactory af = new AbilityFactory();

                    final SpellAbility ab = af
                            .getAbility(
                                    "AB$Destroy | Cost$ U B T | ValidTgts$ Creature.sameName | TgtPrompt$ Select target creature with the same name. | SpellDescription$ Destroy target creature with the same name as this creature.",
                                    card);

                    card.addSpellAbility(ab);
                } else if (cardName.equals("Sakashima the Impostor")) {
                    final AbilityFactory af = new AbilityFactory();
                    final SpellAbility ab = af
                            .getAbility(
                                    "AB$DelayedTrigger | Cost$ 2 U U | Mode$ Phase | Phase$ End of Turn | Execute$ TrigReturnSak | TriggerDescription$ Return CARDNAME to it's owners hand at the beginning of the next end step.",
                                    card);

                    card.addSpellAbility(ab);
                    card.setSVar("TrigReturnSak",
                            "AB$ChangeZone | Cost$ 0 | Defined$ Self | Origin$ Battlefield | Destination$ Hand");
                    card.setName("Sakashima the Impostor");
                    card.addType("Legendary");
                }
            }
        }; // SpellAbility

        final Input runtime = new Input() {
            private static final long serialVersionUID = 7615038074569687330L;

            @Override
            public void showMessage() {
                String message = "Select a creature ";
                if (cardName.equals("Phyrexian Metamorph")) {
                    message += "or artifact ";
                }
                message += "on the battlefield";
                Singletons.getControl().getControlMatch().showMessage(cardName + " - " + message);
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                this.stop();
            }

            @Override
            public void selectCard(final Card c, final PlayerZone z) {
                if (z.is(ZoneType.Battlefield)
                        && (c.isCreature() || (cardName.equals("Phyrexian Metamorph") && c.isArtifact()))) {
                    if (cardName.equals("Jwari Shapeshifter") && !c.isType("Ally")) {
                        return;
                    }
                    copyTarget[0] = c;
                    this.stopSetNext(new InputPayManaCost(copy));
                }
            }
        };

        final Input graveyardRuntime = new Input() {
            private static final long serialVersionUID = 6950318443268022876L;

            @Override
            public void showMessage() {
                final String message = "Select a creature in a graveyard";
                final CardList choices = AllZoneUtil.getCardsIn(ZoneType.Graveyard);
                final Object o = GuiUtils.chooseOneOrNone(message, choices.toArray());
                if (null == o) {
                    this.stop();
                } else {
                    final Card c = (Card) o;
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
        if (cardName.equals("Body Double")) {
            copy.setBeforePayMana(graveyardRuntime);
        } else {
            copy.setBeforePayMana(runtime);
        }
    }

    private static void getCard_Nebuhadnezzar(final Card card, final String cardName) {
        /*
         * X, T: Name a card. Target opponent reveals X cards at random from
         * his or her hand. Then that player discards all cards with that
         * name revealed this way. Activate this ability only during your
         * turn.
         */
        final Cost abCost = new Cost(card, "X T", true);
        final Target target = new Target(card, "Select target opponent", "Opponent".split(","));
        final AbilityActivated discard = new AbilityActivated(card, abCost, target) {
            private static final long serialVersionUID = 4839778470534392198L;

            @Override
            public void resolve() {
                // name a card
                final String choice = JOptionPane.showInputDialog(null, "Name a card", cardName,
                        JOptionPane.QUESTION_MESSAGE);
                final CardList hand = this.getTargetPlayer().getCardsIn(ZoneType.Hand);
                int numCards = card.getXManaCostPaid();
                numCards = Math.min(hand.size(), numCards);

                final CardList revealed = new CardList();
                for (int i = 0; i < numCards; i++) {
                    final Card random = CardUtil.getRandom(hand.toArray());
                    revealed.add(random);
                    hand.remove(random);
                }
                if (!revealed.isEmpty()) {
                    GuiUtils.chooseOne("Revealed at random", revealed.toArray());
                } else {
                    GuiUtils.chooseOne("Revealed at random", new String[] { "Nothing to reveal" });
                }

                for (final Card c : revealed) {
                    if (c.getName().equals(choice)) {
                        c.getController().discard(c, this);
                    }
                }
            }

            @Override
            public boolean canPlayAI() {
                return false;
            }
        };

        discard.getRestrictions().setPlayerTurn(true);

        final StringBuilder sbDesc = new StringBuilder();
        sbDesc.append(abCost).append("Name a card. ");
        sbDesc.append("Target opponent reveals X cards at random from his or her hand. ");
        sbDesc.append("Then that player discards all cards with that name revealed this way. ");
        sbDesc.append("Activate this ability only during your turn.");
        discard.setDescription(sbDesc.toString());

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append(cardName).append(" - name a card.");
        discard.setStackDescription(sbStack.toString());

        card.addSpellAbility(discard);
    }

    private static void getCard_DuctCrawler(final Card card, final String cardName) {
        final String theCost;
        if (cardName.equals("Duct Crawler")) {
            theCost = "1 R";
        } else if (cardName.equals("Shrewd Hatchling")) {
            theCost = "UR";
        } else { // if (cardName.equals("Spin Engine") ||
                 // cardName.equals("Screeching Griffin")) {
            theCost = "R";
        }

        final SpellAbility finalAb = new AbilityActivated(card, new Cost(card, theCost, true), new Target(card,
                "Select target creature.", "Creature")) {
            private static final long serialVersionUID = 2391351140880148283L;

            @Override
            public void resolve() {
                final StringBuilder keywordBuilder = new StringBuilder("HIDDEN CARDNAME can't block ");
                keywordBuilder.append(this.getSourceCard().toString());

                final AbilityFactory createAb = new AbilityFactory();
                final StringBuilder abilityBuilder = new StringBuilder("AB$Pump | Cost$ ");
                abilityBuilder.append(theCost);
                abilityBuilder.append(" | Tgt$ TgtC | IsCurse$ True | KW$ ");
                abilityBuilder.append(keywordBuilder.toString());
                abilityBuilder.append(" | SpellDescription$ Target creature can't block CARDNAME this turn.");
                final SpellAbility myAb = createAb.getAbility(abilityBuilder.toString(), card);

                myAb.getTarget().setTargetChoices(this.getChosenTarget().getTargetChoices());
                myAb.resolve();
            }

            @Override
            public String getStackDescription() {
                return this.getSourceCard().toString() + " - Target creature can't block "
                        + this.getSourceCard().getName() + " this turn.";
            }
        };
        finalAb.setDescription(theCost + ": Target creature can't block CARDNAME this turn.");

        card.addSpellAbility(finalAb);
    }

//    // This is a hardcoded card template
//
//    private static void getCard_(final Card card, final String cardName) {
//    }

    public static Card getCard(final Card card, final String cardName) {

        if (cardName.equals("Force of Savagery")) {
            getCard_ForceOfSavagery(card, cardName);
        } else if (cardName.equals("Gilder Bairn")) {
            getCard_GilderBairn(card, cardName);
        } else if (cardName.equals("Primal Plasma") || cardName.equals("Primal Clay")) {
            getCard_PrimalPlasma(card, cardName);
        } else if (cardName.equals("Minotaur Explorer") || cardName.equals("Balduvian Horde") || cardName.equals("Pillaging Horde")) {
            getCard_MinotaurExplorer(card, cardName);
        } else if (cardName.equals("Phylactery Lich")) {
            getCard_PhylacteryLich(card, cardName);
        } else if (cardName.equals("Sky Swallower")) {
            getCard_SkySwallower(card, cardName);
        } else if (cardName.equals("Jhoira of the Ghitu")) {
            getCard_JhoiraOfTheGhitu(card, cardName);
        } else if (cardName.equals("Vedalken Plotter")) {
            getCard_VedalkenPlotter(card, cardName);
        } else if (cardName.equals("Painter's Servant")) {
            getCard_PainterServant(card, cardName);
        } else if (cardName.equals("Stangg")) {
            getCard_Stangg(card, cardName);
        } else if (cardName.equals("Rhys the Redeemed")) {
            getCard_RhysTheRedeemed(card, cardName);
        } else if (cardName.equals("Treva, the Renewer")) {
            getCard_TrevaTheRenewer(card, cardName);
        } else if (cardName.equals("Sphinx of Jwar Isle")) {
            getCard_SphinxJwar(card, cardName);
        } else if (cardName.equals("Master of the Wild Hunt")) {
            getCard_MasterOfTheWildHunt(card, cardName);
        } else if (cardName.equals("Apocalypse Hydra")) {
            getCard_ApocalypseHydra(card, cardName);
        } else if (cardName.equals("Molten Hydra")) {
            getCard_MoltenHydra(card, cardName);
        } else if (cardName.equals("Academy Rector") || cardName.equals("Lost Auramancers")) {
            getCard_AcademyRector(card, cardName);
        } else if (cardName.equals("Kinsbaile Borderguard")) {
            getCard_KinsbaileBorderguard(card, cardName);
        } else if (cardName.equals("Gnarlid Pack") || cardName.equals("Apex Hawks") || cardName.equals("Enclave Elite")
                || cardName.equals("Quag Vampires") || cardName.equals("Skitter of Lizards") || cardName.equals("Joraga Warcaller")) {
            getCard_MultikickerP1P1(card, cardName);
        } else if (cardName.equals("Vampire Hexmage")) {
            getCard_VampireHexmage(card, cardName);
        } else if (cardName.equals("Sutured Ghoul")) {
            getCard_SurturedGhoul(card, cardName);
        } else if (cardName.equals("Nameless Race")) {
            getCard_NamelessRace(card, cardName);
        } else if (cardName.equals("Phyrexian Scuta")) {
            getCard_PhyrexianScuta(card, cardName);
        } else if (cardName.equals("Yosei, the Morning Star")) {
            getCard_YoseiTheMorningStar(card, cardName);
        } else if (cardName.equals("Phyrexian Dreadnought")) {
            getCard_PhyrexianDreadnought(card, cardName);
        } else if (cardName.equals("Clone") || cardName.equals("Vesuvan Doppelganger")
                || cardName.equals("Quicksilver Gargantuan") || cardName.equals("Jwari Shapeshifter")
                || cardName.equals("Phyrexian Metamorph") || cardName.equals("Phantasmal Image")
                || cardName.equals("Body Double") || cardName.equals("Evil Twin")
                || cardName.equals("Sakashima the Impostor")) {
            getCard_ClonesSeries(card, cardName);
        } else if (cardName.equals("Nebuchadnezzar")) {
            getCard_Nebuhadnezzar(card, cardName);
        } else if (cardName.equals("Duct Crawler") || cardName.equals("Shrewd Hatchling")
                || cardName.equals("Spin Engine") || cardName.equals("Screeching Griffin")) {
            getCard_DuctCrawler(card, cardName);
        }

        // ***************************************************
        // end of card specific code
        // ***************************************************

        if ((CardFactoryCreatures.hasKeyword(card, "Level up") != -1)
                && (CardFactoryCreatures.hasKeyword(card, "maxLevel") != -1)) {
            final int n = CardFactoryCreatures.hasKeyword(card, "Level up");
            final int m = CardFactoryCreatures.hasKeyword(card, "maxLevel");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                final String parseMax = card.getKeyword().get(m).toString();

                card.removeIntrinsicKeyword(parse);
                card.removeIntrinsicKeyword(parseMax);

                final String[] k = parse.split(":");
                final String manacost = k[1];

                final String[] l = parseMax.split(":");
                final int maxLevel = Integer.parseInt(l[1]);

                final SpellAbility levelUp = new AbilityActivated(card, manacost) {
                    private static final long serialVersionUID = 3998280279949548652L;

                    @Override
                    public void resolve() {
                        card.addCounter(Counters.LEVEL, 1);
                    }

                    @Override
                    public boolean canPlayAI() {
                        // Todo: Improve Level up code
                        return card.getCounters(Counters.LEVEL) < maxLevel;
                    }

                };
                levelUp.getRestrictions().setSorcerySpeed(true);
                card.addSpellAbility(levelUp);

                final StringBuilder sbDesc = new StringBuilder();
                sbDesc.append("Level up ").append(manacost).append(" (").append(manacost);
                sbDesc.append(": Put a level counter on this. Level up only as a sorcery.)");
                levelUp.setDescription(sbDesc.toString());

                final StringBuilder sbStack = new StringBuilder();
                sbStack.append(card).append(" - put a level counter on this.");
                levelUp.setStackDescription(sbStack.toString());

                card.setLevelUp(true);

            }
        } // level up

        return card;
    }

    /**
     * <p>
     * hasKeyword.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param k
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    private static int hasKeyword(final Card c, final String k) {
        final ArrayList<String> a = c.getKeyword();
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).toString().startsWith(k)) {
                return i;
            }
        }

        return -1;
    }
}
