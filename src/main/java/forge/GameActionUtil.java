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
package forge;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JOptionPane;

import forge.card.abilityfactory.AbilityFactory;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostDiscard;
import forge.card.cost.CostPart;
import forge.card.cost.CostPayLife;
import forge.card.cost.CostMana;
import forge.card.cost.CostPutCounter;
import forge.card.cost.CostSacrifice;
import forge.card.mana.ManaCost;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityMana;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityRestriction;
import forge.control.input.InputPayDiscardCost;
import forge.control.input.InputPayManaCostAbility;
import forge.control.input.InputPayManaCostUtil;
import forge.control.input.InputPaySacCost;
import forge.game.GameLossReason;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiUtils;
import forge.gui.match.CMatchUI;
import forge.util.MyRandom;

/**
 * <p>
 * GameActionUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class GameActionUtil {

    private GameActionUtil() {
        throw new AssertionError();
    }

    /**
     * <p>
     * executePlayCardEffects.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static void executePlayCardEffects(final SpellAbility sa) {
        // (called in MagicStack.java)

        GameActionUtil.playCardCascade(sa);
        GameActionUtil.playCardRipple(sa);
    }

    /**
     * <p>
     * playCardCascade.
     * </p>
     * 
     * @param sa
     *            a SpellAbility object.
     */
    public static void playCardCascade(final SpellAbility sa) {
        final Card c = sa.getSourceCard();
        final Player controller = sa.getActivatingPlayer();
        final Command cascade = new Command() {
            private static final long serialVersionUID = -845154812215847505L;

            @Override
            public void execute() {

                if (!c.isCopiedSpell()) {
                    final CardList humanNexus = AllZone.getHumanPlayer()
                            .getCardsIn(ZoneType.Battlefield, "Maelstrom Nexus");
                    final CardList computerNexus = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield,
                            "Maelstrom Nexus");

                    final CardList maelstromNexii = new CardList();
                    maelstromNexii.addAll(humanNexus);
                    maelstromNexii.addAll(computerNexus);

                    for (final Card nexus : maelstromNexii) {
                        if (CardUtil.getThisTurnCast("Card.YouCtrl", nexus).size() == 1) {
                            this.doCascade(c, controller);
                        }
                    }
                }

                for (String keyword : c.getKeyword()) {
                    if (keyword.equals("Cascade")) {
                        this.doCascade(c, controller);
                    }
                }
            } // execute()

            void doCascade(final Card c, final Player controller) {
                final Card cascCard = c;

                final Ability ability = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        final CardList topOfLibrary = controller.getCardsIn(ZoneType.Library);
                        final CardList revealed = new CardList();

                        if (topOfLibrary.size() == 0) {
                            return;
                        }

                        Card cascadedCard = null;
                        Card crd;
                        int count = 0;
                        while (cascadedCard == null) {
                            crd = topOfLibrary.get(count++);
                            revealed.add(crd);
                            if ((!crd.isLand() && (crd.getManaCost().getCMC() < cascCard.getManaCost().getCMC()))) {
                                cascadedCard = crd;
                            }

                            if (count == topOfLibrary.size()) {
                                break;
                            }

                        } // while
                        GuiUtils.chooseOneOrNone("Revealed cards:", revealed.toArray());

                        if (cascadedCard != null) {

                            if (cascadedCard.getController().isHuman()) {
                                final StringBuilder title = new StringBuilder();
                                title.append(cascCard.getName()).append(" - Cascade Ability");
                                final StringBuilder question = new StringBuilder();
                                question.append("Cast ").append(cascadedCard.getName());
                                question.append(" without paying its mana cost?");

                                final int answer = JOptionPane.showConfirmDialog(null, question.toString(),
                                        title.toString(), JOptionPane.YES_NO_OPTION);

                                if (answer == JOptionPane.YES_OPTION) {
                                    Singletons.getModel().getGameAction().playCardNoCost(cascadedCard);
                                    revealed.remove(cascadedCard);
                                }
                            } else {
                                final ArrayList<SpellAbility> choices = cascadedCard.getBasicSpells();

                                for (final SpellAbility sa : choices) {
                                    //Spells
                                    if (sa instanceof Spell) {
                                        Spell spell = (Spell) sa;
                                        if (!spell.canPlayFromEffectAI(false, true)) {
                                            continue;
                                        }
                                    } else {
                                        if (!sa.canPlayAI()) {
                                            continue;
                                        }
                                    }
                                    ComputerUtil.playSpellAbilityWithoutPayingManaCost(sa);
                                    revealed.remove(cascadedCard);
                                    break;
                                }
                            }
                        }
                        revealed.shuffle();
                        for (final Card bottom : revealed) {
                            Singletons.getModel().getGameAction().moveToBottomOfLibrary(bottom);
                        }
                    }
                };
                final StringBuilder sb = new StringBuilder();
                sb.append(c).append(" - Cascade.");
                ability.setStackDescription(sb.toString());
                ability.setActivatingPlayer(controller);

                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        };
        cascade.execute();
    } // end playCardCascade

    /**
     * <p>
     * playCardRipple.
     * </p>
     * 
     * @param sa
     *            a SpellAbility object.
     */
    public static void playCardRipple(final SpellAbility sa) {
        final Card c = sa.getSourceCard();
        final Player controller = sa.getActivatingPlayer();
        final Command ripple = new Command() {
            private static final long serialVersionUID = -845154812215847505L;

            @Override
            public void execute() {

                final CardList humanThrummingStone = AllZone.getHumanPlayer().getCardsIn(ZoneType.Battlefield,
                        "Thrumming Stone");
                final CardList computerThrummingStone = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield,
                        "Thrumming Stone");

                for (int i = 0; i < humanThrummingStone.size(); i++) {
                    if (c.getController().isHuman()) {
                        c.addExtrinsicKeyword("Ripple:4");
                    }
                }
                for (int i = 0; i < computerThrummingStone.size(); i++) {
                    if (c.getController().isComputer()) {
                        c.addExtrinsicKeyword("Ripple:4");
                    }
                }
                final ArrayList<String> a = c.getKeyword();
                for (int x = 0; x < a.size(); x++) {
                    if (a.get(x).toString().startsWith("Ripple")) {
                        final String parse = c.getKeyword().get(x).toString();
                        final String[] k = parse.split(":");
                        this.doRipple(c, Integer.valueOf(k[1]), controller);
                    }
                }
            } // execute()

            void doRipple(final Card c, final int rippleCount, final Player controller) {
                final Card rippleCard = c;
                boolean activateRipple = false;
                if (controller.isComputer() || GameActionUtil.showYesNoDialog(c, "Activate Ripple for " + c + "?")) {
                        activateRipple = true;
                }
                if (activateRipple) {
                    final Ability ability = new Ability(c, "0") {
                        @Override
                        public void resolve() {
                            final CardList topOfLibrary = controller.getCardsIn(ZoneType.Library);
                            final CardList revealed = new CardList();
                            int rippleNumber = rippleCount;
                            if (topOfLibrary.size() == 0) {
                                return;
                            }

                            // Shouldn't Have more than Ripple 10, seeing as no
                            // cards exist with a ripple greater than 4
                            final int rippleMax = 10;
                            final Card[] rippledCards = new Card[rippleMax];
                            Card crd;
                            if (topOfLibrary.size() < rippleNumber) {
                                rippleNumber = topOfLibrary.size();
                            }

                            for (int i = 0; i < rippleNumber; i++) {
                                crd = topOfLibrary.get(i);
                                revealed.add(crd);
                                if (crd.getName().equals(rippleCard.getName())) {
                                    rippledCards[i] = crd;
                                }
                            } // for
                            GuiUtils.chooseOneOrNone("Revealed cards:", revealed.toArray());
                            for (int i = 0; i < rippleMax; i++) {
                                if (rippledCards[i] != null) {

                                    if (rippledCards[i].getController().isHuman()) {
                                        final Object[] possibleValues = { "Yes", "No" };
                                        final Object q = JOptionPane.showOptionDialog(null,
                                                "Cast " + rippledCards[i].getName() + "?", "Ripple",
                                                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                                                possibleValues, possibleValues[0]);
                                        if (q.equals(0)) {
                                            Singletons.getModel().getGameAction().playCardNoCost(rippledCards[i]);
                                            revealed.remove(rippledCards[i]);
                                        }
                                    } else {
                                        final ArrayList<SpellAbility> choices = rippledCards[i].getBasicSpells();

                                        for (final SpellAbility sa : choices) {
                                          //Spells
                                            if (sa instanceof Spell) {
                                                Spell spell = (Spell) sa;
                                                if (!spell.canPlayFromEffectAI(false, true)) {
                                                    continue;
                                                }
                                            } else {
                                                if (!sa.canPlayAI() && !sa.getSourceCard().isType("Legendary")) {
                                                    continue;
                                                }
                                            }
                                            ComputerUtil.playSpellAbilityWithoutPayingManaCost(sa);
                                            revealed.remove(rippledCards[i]);
                                            break;
                                        }
                                    }
                                }
                            }
                            revealed.shuffle();
                            for (final Card bottom : revealed) {
                                Singletons.getModel().getGameAction().moveToBottomOfLibrary(bottom);
                            }
                        }
                    };
                    final StringBuilder sb = new StringBuilder();
                    sb.append(c).append(" - Ripple.");
                    ability.setStackDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(ability);

                }
            }
        };
        ripple.execute();
    } // playCardRipple()

    /**
     * <p>
     * payManaDuringAbilityResolve.
     * </p>
     * 
     * @param message
     *            a {@link java.lang.String} object.
     * @param manaCost
     *            a {@link java.lang.String} object.
     * @param paid
     *            a {@link forge.Command} object.
     * @param unpaid
     *            a {@link forge.Command} object.
     */
    public static void payManaDuringAbilityResolve(final String message, final String manaCost, final Command paid,
            final Command unpaid) {
        // temporarily disable the Resolve flag, so the user can payMana for the
        // resolving Ability
        final boolean bResolving = AllZone.getStack().getResolving();
        AllZone.getStack().setResolving(false);
        AllZone.getInputControl().setInput(new InputPayManaCostAbility(message, manaCost, paid, unpaid));
        AllZone.getStack().setResolving(bResolving);
    }

    /**
     * <p>
     * payCostDuringAbilityResolve.
     * </p>
     * 
     * @param message
     *            a {@link java.lang.String} object.
     * @param hostCard
     *            a Card object.
     * @param manaCost
     *            a {@link java.lang.String} object.
     * @param paid
     *            a {@link forge.Command} object.
     * @param unpaid
     *            a {@link forge.Command} object.
     */
    /*public static void payCostDuringAbilityResolve(final String message, Card hostCard, final String manaCost,
            final Command paid, final Command unpaid) {
        if (manaCost.startsWith("PayLife")) {
            String amountString = manaCost.split("<")[1].split(">")[0];
            int amount = amountString.matches("[0-9][0-9]?") ? Integer.parseInt(amountString)
                    : CardFactoryUtil.xCount(hostCard, hostCard.getSVar(amountString));
            if (AllZone.getHumanPlayer().canPayLife(amount) && showYesNoDialog(hostCard, "Do you want to pay "
                    + amount + " life?")) {
                AllZone.getHumanPlayer().payLife(amount, null);
                paid.execute();
            } else {
                unpaid.execute();
            }
            return;
        }
        else if (manaCost.startsWith("AddCounter")) {
            String amountString = manaCost.split("<")[1].split("\\/")[0];
            String counterName = manaCost.split("<")[1].split("\\/")[1].split(">")[0];
            Counters counterType = Counters.valueOf(counterName);
            int amount = amountString.matches("[0-9][0-9]?") ? Integer.parseInt(amountString)
                    : CardFactoryUtil.xCount(hostCard, hostCard.getSVar(amountString));
            String plural = amount > 1 ? "s" : "";
            if (showYesNoDialog(hostCard, "Do you want to put " + amount + " " + counterType.getName()
                    + " counter" + plural + " on " + hostCard + "?")) {
                if (hostCard.canHaveCountersPlacedOnIt(counterType)) {
                    hostCard.addCounterFromNonEffect(counterType, amount);
                    paid.execute();
                } else {
                    unpaid.execute();
                    AllZone.getGameLog().add("ResolveStack", "Trying to pay upkeep for " + hostCard + " but it can't have "
                    + counterType.getName() + " counters put on it.", 2);
                }
            } else {
                unpaid.execute();
            }
            return;
        }
        if (manaCost.equals("0")) {
            if (showYesNoDialog(hostCard, "Do you want to pay 0?")) {
                paid.execute();
            } else {
                unpaid.execute();
            }
            return;
        }

        // temporarily disable the Resolve flag, so the user can payMana for the
        // resolving Ability
        final boolean bResolving = AllZone.getStack().getResolving();
        AllZone.getStack().setResolving(false);
        AllZone.getInputControl().setInput(new InputPayManaCostAbility(message, manaCost, paid, unpaid));
        AllZone.getStack().setResolving(bResolving);
    }*/

    /**
     * <p>
     * payCostDuringAbilityResolve.
     * </p>
     * 
     * @param ability
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param cost
     *            a {@link forge.card.cost.Cost} object.
     * @param paid
     *            a {@link forge.Command} object.
     * @param unpaid
     *            a {@link forge.Command} object.
     */
    public static void payCostDuringAbilityResolve(final SpellAbility ability, final Cost cost, final Command paid, final Command unpaid) {
        final Card source = ability.getSourceCard();
        if (cost.getCostParts().size() > 1) {
            throw new RuntimeException("GameActionUtil::payCostDuringAbilityResolve - Too many payment types - " + source);
        }
        final CostPart costPart = cost.getCostParts().get(0);
        if (costPart instanceof CostPayLife) {
            String amountString = costPart.getAmount();
            final int amount = amountString.matches("[0-9][0-9]?") ? Integer.parseInt(amountString)
                    : CardFactoryUtil.xCount(source, source.getSVar(amountString));
            if (AllZone.getHumanPlayer().canPayLife(amount) && showYesNoDialog(source, "Do you want to pay "
                    + amount + " life?")) {
                AllZone.getHumanPlayer().payLife(amount, null);
                paid.execute();
            } else {
                unpaid.execute();
            }
            return;
        }

        else if (costPart instanceof CostPutCounter) {
            String amountString = costPart.getAmount();
            Counters counterType = ((CostPutCounter) costPart).getCounter();
            int amount = amountString.matches("[0-9][0-9]?") ? Integer.parseInt(amountString)
                    : CardFactoryUtil.xCount(source, source.getSVar(amountString));
            String plural = amount > 1 ? "s" : "";
            if (showYesNoDialog(source, "Do you want to put " + amount + " " + counterType.getName()
                    + " counter" + plural + " on " + source + "?")) {
                if (source.canHaveCountersPlacedOnIt(counterType)) {
                    source.addCounterFromNonEffect(counterType, amount);
                    paid.execute();
                } else {
                    unpaid.execute();
                    AllZone.getGameLog().add("ResolveStack", "Trying to pay upkeep for " + source + " but it can't have "
                    + counterType.getName() + " counters put on it.", 2);
                }
            } else {
                unpaid.execute();
            }
            return;
        }

        else if (costPart instanceof CostSacrifice) {
            final boolean bResolving = AllZone.getStack().getResolving();
            AllZone.getStack().setResolving(false);
            AllZone.getInputControl().setInput(new InputPaySacCost((CostSacrifice) costPart, ability, paid, unpaid));
            AllZone.getStack().setResolving(bResolving);
        }
        else if (costPart instanceof CostDiscard) {
            final boolean bResolving = AllZone.getStack().getResolving();
            AllZone.getStack().setResolving(false);
            AllZone.getInputControl().setInput(new InputPayDiscardCost((CostDiscard) costPart, ability, paid, unpaid));
            AllZone.getStack().setResolving(bResolving);
        }
        else if (costPart instanceof CostMana) {
            if (costPart.getAmount().equals("0")) {
                if (showYesNoDialog(source, "Do you want to pay 0?")) {
                    paid.execute();
                } else {
                    unpaid.execute();
                }
                return;
            }
            // temporarily disable the Resolve flag, so the user can payMana for the
            // resolving Ability
            final boolean bResolving = AllZone.getStack().getResolving();
            AllZone.getStack().setResolving(false);
            AllZone.getInputControl().setInput(new InputPayManaCostAbility(source + "\r\n", ability.getManaCost(), paid, unpaid));
            AllZone.getStack().setResolving(bResolving);
        }
    }

    /**
     * <p>
     * showYesNoDialog.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param question
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean showYesNoDialog(final Card c, final String question) {
        return GameActionUtil.showYesNoDialog(c, question, false);
    }

    /**
     * <p>
     * showYesNoDialog.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param question
     *            a {@link java.lang.String} object.
     * @param defaultNo
     *            true if the default option should be "No", false otherwise
     * @return a boolean.
     */
    public static boolean showYesNoDialog(final Card c, String question, final boolean defaultNo) {
        CMatchUI.SINGLETON_INSTANCE.setCard(c);
        final StringBuilder title = new StringBuilder();
        title.append(c.getName()).append(" - Ability");

        if (!(question.length() > 0)) {
            question = "Activate card's ability?";
        }

        int answer;
        if (defaultNo) {
            final Object[] options = { "Yes", "No" };
            answer = JOptionPane.showOptionDialog(null, question, title.toString(), JOptionPane.YES_NO_OPTION,
                    JOptionPane.PLAIN_MESSAGE, null, options, options[1]);
        } else {
            answer = JOptionPane.showConfirmDialog(null, question, title.toString(), JOptionPane.YES_NO_OPTION);
        }

        return answer == JOptionPane.YES_OPTION;
    }

    /**
     * <p>
     * showInfoDialg.
     * </p>
     * 
     * @param message
     *            a {@link java.lang.String} object.
     */
    public static void showInfoDialg(final String message) {
        JOptionPane.showMessageDialog(null, message);
    }

    /**
     * <p>
     * flipACoin.
     * </p>
     * 
     * @param caller
     *            a {@link forge.game.player.Player} object.
     * @param source
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean flipACoin(final Player caller, final Card source) {
        String choice = "";
        final String[] choices = { "heads", "tails" };

        final boolean flip = MyRandom.getRandom().nextBoolean();
        if (caller.isHuman()) {
            choice = GuiUtils.chooseOne(source.getName() + " - Call coin flip", choices);
        } else {
            choice = choices[MyRandom.getRandom().nextInt(2)];
        }

        final boolean winFlip = flip == choice.equals("heads");
        final String winMsg = winFlip ? " wins flip." : " loses flip.";

        JOptionPane.showMessageDialog(null, source.getName() + " - " + caller + winMsg, source.getName(),
                JOptionPane.PLAIN_MESSAGE);
        return winFlip;
    }

    /**
     * <p>
     * executeLandfallEffects.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public static void executeLandfallEffects(final Card c) {
        if (c.getName().equals("Lotus Cobra")) {
            GameActionUtil.landfallLotusCobra(c);
        }
    }

    /**
     * <p>
     * showLandfallDialog.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    private static boolean showLandfallDialog(final Card c) {
        CMatchUI.SINGLETON_INSTANCE.setCard(c);
        final String[] choices = { "Yes", "No" };

        Object q = null;

        q = GuiUtils.chooseOneOrNone("Use " + c + " Landfall?", choices);

        return (q != null) && q.equals("Yes");
    }

    /**
     * <p>
     * landfallLotusCobra.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    private static void landfallLotusCobra(final Card c) {
        final Ability ability = new Ability(c, "0") {
            @Override
            public void resolve() {
                String color = "";

                final Object o = GuiUtils.chooseOne("Choose mana color", Constant.Color.ONLY_COLORS);
                color = InputPayManaCostUtil.getShortColorString((String) o);

                final AbilityMana abMana = new AbilityMana(c, "0", color) {
                    private static final long serialVersionUID = -2182129023960978132L;
                };
                abMana.produceMana();
            }
        };

        final StringBuilder sb = new StringBuilder();
        sb.append(c.getName()).append(" - add one mana of any color to your mana pool.");
        ability.setStackDescription(sb.toString());

        if (c.getController().isHuman()) {
            if (GameActionUtil.showLandfallDialog(c)) {
                AllZone.getStack().addSimultaneousStackEntry(ability);
            }
        } else {
            // TODO once AI has a mana pool he should choose add Ability and
            // choose a mana as appropriate
        }
    }

    // not restricted to combat damage, not restricted to dealing damage to
    // creatures/players
    /**
     * <p>
     * executeDamageDealingEffects.
     * </p>
     * 
     * @param source
     *            a {@link forge.Card} object.
     * @param damage
     *            a int.
     */
    public static void executeDamageDealingEffects(final Card source, final int damage) {

        if (damage <= 0) {
            return;
        }

        if (source.hasKeyword("Lifelink")) {
            source.getController().gainLife(damage, source);
        }

    }

    // not restricted to combat damage, restricted to dealing damage to
    // creatures
    /**
     * <p>
     * executeDamageToCreatureEffects.
     * </p>
     * 
     * @param source
     *            a {@link forge.Card} object.
     * @param affected
     *            a {@link forge.Card} object.
     * @param damage
     *            a int.
     */
    public static void executeDamageToCreatureEffects(final Card source, final Card affected, final int damage) {

        if (damage <= 0) {
            return;
        }

        if (affected.hasStartOfKeyword("When CARDNAME is dealt damage, destroy it.")) {
            final Ability ability = new Ability(source, "0") {
                @Override
                public void resolve() {
                    Singletons.getModel().getGameAction().destroy(affected);
                }
            };

            final Ability ability2 = new Ability(source, "0") {
                @Override
                public void resolve() {
                    Singletons.getModel().getGameAction().destroyNoRegeneration(affected);
                }
            };

            final StringBuilder sb = new StringBuilder();
            sb.append(affected).append(" - destroy");
            ability.setStackDescription(sb.toString());
            ability2.setStackDescription(sb.toString());
            final int amount = affected
                    .getAmountOfKeyword("When CARDNAME is dealt damage, destroy it. It can't be regenerated.");

            for (int i = 0; i < amount; i++) {
                AllZone.getStack().addSimultaneousStackEntry(ability2);
            }
            final int amount2 = affected.getAmountOfKeyword("When CARDNAME is dealt damage, destroy it.");

            for (int i = 0; i < amount2; i++) {
                AllZone.getStack().addSimultaneousStackEntry(ability);
            }
        }
    }

    // this is for cards like Sengir Vampire
    /**
     * <p>
     * executeVampiricEffects.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public static void executeVampiricEffects(final Card c) {
        final ArrayList<String> a = c.getKeyword();
        for (int i = 0; i < a.size(); i++) {
            if (AllZoneUtil.isCardInPlay(c)
                    && a.get(i)
                            .toString()
                            .startsWith(
                                    "Whenever a creature dealt damage by CARDNAME "
                                            + "this turn is put into a graveyard, put")) {
                final Card thisCard = c;
                final String kw = a.get(i).toString();
                final Ability ability2 = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        Counters counter = Counters.P1P1;
                        if (kw.contains("+2/+2")) {
                            counter = Counters.P2P2;
                        }
                        if (AllZoneUtil.isCardInPlay(thisCard)) {
                            thisCard.addCounter(counter, 1);
                        }
                    }
                }; // ability2

                final StringBuilder sb = new StringBuilder();
                sb.append(c.getName());
                if (kw.contains("+2/+2")) {
                    sb.append(" - gets a +2/+2 counter");
                } else {
                    sb.append(" - gets a +1/+1 counter");
                }
                ability2.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(ability2);

            }
        }
    }

    // not restricted to just combat damage, restricted to players
    /**
     * <p>
     * executeDamageToPlayerEffects.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @param c
     *            a {@link forge.Card} object.
     * @param damage
     *            a int.
     */
    public static void executeDamageToPlayerEffects(final Player player, final Card c, final int damage) {
        if (damage <= 0) {
            return;
        }

        final CardList playerPerms = player.getCardsIn(ZoneType.Battlefield);

        if (AllZoneUtil.isCardInPlay("Lich", player)) {
            final CardList lichs = playerPerms.getName("Lich");
            for (final Card crd : lichs) {
                final Card lich = crd;
                final SpellAbility ability = new Ability(lich, "0") {
                    @Override
                    public void resolve() {
                        for (int i = 0; i < damage; i++) {
                            CardList nonTokens = player.getCardsIn(ZoneType.Battlefield);
                            nonTokens = nonTokens.filter(CardListFilter.NON_TOKEN);
                            if (nonTokens.size() == 0) {
                                player.loseConditionMet(GameLossReason.SpellEffect, lich.getName());
                            } else {
                                player.sacrificePermanent("Select a permanent to sacrifice", nonTokens);
                            }
                        }
                    }
                };

                final StringBuilder sb = new StringBuilder();
                sb.append(lich.getName()).append(" - ").append(lich.getController());
                sb.append(" sacrifices ").append(damage).append(" nontoken permanents.");
                ability.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        }

        if (c.getName().equals("Whirling Dervish") || c.getName().equals("Dunerider Outlaw")) {
            GameActionUtil.playerCombatDamageWhirlingDervish(c);
        }

        if (player.isPlayer(AllZone.getHumanPlayer())) {
            c.getDamageHistory().setDealtDmgToHumanThisTurn(true);
        }
        if (player.isPlayer(AllZone.getComputerPlayer())) {
            c.getDamageHistory().setDealtDmgToComputerThisTurn(true);
        }
    }

    // restricted to combat damage, restricted to players
    /**
     * <p>
     * executeCombatDamageToPlayerEffects.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @param c
     *            a {@link forge.Card} object.
     * @param damage
     *            a int.
     */
    public static void executeCombatDamageToPlayerEffects(final Player player, final Card c, final int damage) {

        if (damage <= 0) {
            return;
        }

        if (c.isCreature() && AllZoneUtil.isCardInPlay("Contested War Zone", player)) {
            final CardList zones = player.getCardsIn(ZoneType.Battlefield, "Contested War Zone");
            for (final Card zone : zones) {
                final Ability ability = new Ability(zone, "0") {
                    @Override
                    public void resolve() {
                        if (AllZoneUtil.isCardInPlay(zone)) {
                            zone.addController(c.getController());
                        }
                    }
                };
                ability.setStackDescription(zone + " - " + c.getController() + " gains control of " + zone);

                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        }

        if (c.hasStartOfKeyword("Poisonous")) {
            final int keywordPosition = c.getKeywordPosition("Poisonous");
            final String parse = c.getKeyword().get(keywordPosition).toString();
            final String[] k = parse.split(" ");
            final int poison = Integer.parseInt(k[1]);
            final Card crd = c;

            final Ability ability = new Ability(c, "0") {
                @Override
                public void resolve() {
                    final Player player = crd.getController();
                    final Player opponent = player.getOpponent();
                    opponent.addPoisonCounters(poison, c);
                }
            };

            final StringBuilder sb = new StringBuilder();
            sb.append(c);
            sb.append(" - Poisonous: ");
            sb.append(c.getController().getOpponent());
            sb.append(" gets ");
            sb.append(poison);
            sb.append(" poison counter");
            if (poison != 1) {
                sb.append("s");
            }
            sb.append(".");

            ability.setStackDescription(sb.toString());
            final ArrayList<String> keywords = c.getKeyword();

            for (int i = 0; i < keywords.size(); i++) {
                if (keywords.get(i).startsWith("Poisonous")) {
                    AllZone.getStack().addSimultaneousStackEntry(ability);
                }

            }
        }

        if (c.getName().equals("Scalpelexis")) {
            GameActionUtil.playerCombatDamageScalpelexis(c);
        } else if (c.getName().equals("Spawnwrithe")) {
            GameActionUtil.playerCombatDamageSpawnwrithe(c);
        } else if (c.isEnchantedBy("Celestial Mantle")) {
            GameActionUtil.executeCelestialMantle(c);
        }

        if (player.isPlayer(AllZone.getHumanPlayer())) {
            c.getDamageHistory().setDealtCombatDmgToHumanThisTurn(true);
        }
        if (player.isPlayer(AllZone.getComputerPlayer())) {
            c.getDamageHistory().setDealtCombatDmgToComputerThisTurn(true);
        }
    } // executeCombatDamageToPlayerEffects

    /**
     * <p>
     * executeCelestialMantle.
     * </p>
     * 
     * @param enchanted
     *            a {@link forge.Card} object.
     */
    private static void executeCelestialMantle(final Card enchanted) {
        final ArrayList<Card> auras = enchanted.getEnchantedBy();
        for (final Card aura : auras) {
            if (aura.getName().equals("Celestial Mantle")) {
                final Ability doubleLife = new Ability(aura, "0") {
                    @Override
                    public void resolve() {
                        final int life = enchanted.getController().getLife();
                        enchanted.getController().setLife(life * 2, aura);
                    }
                };
                doubleLife.setStackDescription(aura.getName() + " - " + enchanted.getController()
                        + " doubles his or her life total.");

                AllZone.getStack().addSimultaneousStackEntry(doubleLife);

            }
        }
    }

    /**
     * <p>
     * playerCombatDamageWhirlingDervish.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    private static void playerCombatDamageWhirlingDervish(final Card c) {
        final int power = c.getNetAttack();
        final Card card = c;

        if (power > 0) {
            final Ability ability2 = new Ability(c, "0") {
                @Override
                public void resolve() {
                    card.addCounter(Counters.P1P1, 1);
                }
            }; // ability2

            final StringBuilder sb = new StringBuilder();
            sb.append(c.getName()).append(" - gets a +1/+1 counter.");
            ability2.setStackDescription(sb.toString());

            final Command dealtDmg = new Command() {
                private static final long serialVersionUID = 2200679209414069339L;

                @Override
                public void execute() {
                    AllZone.getStack().addSimultaneousStackEntry(ability2);

                }
            };
            AllZone.getEndOfTurn().addAt(dealtDmg);

        } // if
    }

    /**
     * <p>
     * playerCombatDamageScalpelexis.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    private static void playerCombatDamageScalpelexis(final Card c) {
        final Player player = c.getController();
        final Player opponent = player.getOpponent();

        if (c.getNetAttack() > 0) {
            final Ability ability = new Ability(c, "0") {
                @Override
                public void resolve() {

                    final CardList libList = opponent.getCardsIn(ZoneType.Library);
                    int count = 0;
                    int broken = 0;
                    for (int i = 0; i < libList.size(); i = i + 4) {
                        Card c1 = null;
                        Card c2 = null;
                        Card c3 = null;
                        Card c4 = null;
                        if (i < libList.size()) {
                            c1 = libList.get(i);
                        } else {
                            broken = 1;
                        }
                        if ((i + 1) < libList.size()) {
                            c2 = libList.get(i + 1);
                        } else {
                            broken = 1;
                        }
                        if ((i + 2) < libList.size()) {
                            c3 = libList.get(i + 2);
                        } else {
                            broken = 1;
                        }
                        if ((i + 3) < libList.size()) {
                            c4 = libList.get(i + 3);
                        } else {
                            broken = 1;
                        }
                        if (broken == 0) {
                            if ((c1.getName().contains(c2.getName()) || c1.getName().contains(c3.getName())
                                    || c1.getName().contains(c4.getName()) || c2.getName().contains(c3.getName())
                                    || c2.getName().contains(c4.getName()) || c3.getName().contains(c4.getName()))) {
                                count = count + 1;
                            } else {
                                broken = 1;
                            }
                        }

                    }
                    count = (count * 4) + 4;
                    int max = count;
                    if (libList.size() < count) {
                        max = libList.size();
                    }

                    for (int j = 0; j < max; j++) {
                        final Card c = libList.get(j);
                        Singletons.getModel().getGameAction().exile(c);
                    }
                }
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Scalpelexis - ").append(opponent);
            sb.append(" exiles the top four cards of his or her library. ");
            sb.append("If two or more of those cards have the same name, repeat this process.");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }
    }

    /**
     * <p>
     * playerCombatDamageSpawnwrithe.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    private static void playerCombatDamageSpawnwrithe(final Card c) {
        final Player player = c.getController();
        final Card crd = c;

        final Ability ability2 = new Ability(c, "0") {
            @Override
            public void resolve() {
                final CardList cl = CardFactoryUtil.makeToken("Spawnwrithe", "", crd.getController(), "2 G",
                        new String[] { "Creature", "Elemental" }, 2, 2, new String[] { "Trample" });

                for (final Card c : cl) {
                    c.setText("Whenever Spawnwrithe deals combat damage to a player, "
                            + "put a token that's a copy of Spawnwrithe onto the battlefield.");
                    c.setCopiedToken(true);
                }
            }
        }; // ability2

        final StringBuilder sb = new StringBuilder();
        sb.append(c.getName()).append(" - ").append(player).append(" puts copy onto the battlefield.");
        ability2.setStackDescription(sb.toString());

        AllZone.getStack().addSimultaneousStackEntry(ability2);

    }

    // Special Conditions
    /**
     * <p>
     * specialConditionsMet.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param specialConditions
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean specialConditionsMet(final Card sourceCard, final String specialConditions) {

        if (specialConditions.contains("CardsInHandMore")) {
            final CardList specialConditionsCardList = sourceCard.getController().getCardsIn(ZoneType.Hand);
            final String condition = specialConditions.split("/")[1];
            if (specialConditionsCardList.size() < Integer.valueOf(condition)) {
                return false;
            }
        }
        if (specialConditions.contains("OppHandEmpty")) {
            final CardList oppHand = sourceCard.getController().getOpponent().getCardsIn(ZoneType.Hand);
            if (!(oppHand.size() == 0)) {
                return false;
            }
        }
        if (specialConditions.contains("TopCardOfLibraryIsBlack")) {
            final PlayerZone lib = sourceCard.getController().getZone(ZoneType.Library);
            if (!(lib.get(0).isBlack())) {
                return false;
            }
        }
        if (specialConditions.contains("LibraryLE")) {
            final CardList library = sourceCard.getController().getCardsIn(ZoneType.Library);
            final String maxnumber = specialConditions.split("/")[1];
            if (library.size() > Integer.valueOf(maxnumber)) {
                return false;
            }
        }
        if (specialConditions.contains("LifeGE")) {
            final int life = sourceCard.getController().getLife();
            final String maxnumber = specialConditions.split("/")[1];
            if (!(life >= Integer.valueOf(maxnumber))) {
                return false;
            }
        }
        if (specialConditions.contains("OppCreatureInPlayGE")) {
            CardList oppInPlay = sourceCard.getController().getOpponent().getCardsIn(ZoneType.Battlefield);
            oppInPlay = oppInPlay.getType("Creature");
            final String maxnumber = specialConditions.split("/")[1];
            if (!(oppInPlay.size() >= Integer.valueOf(maxnumber))) {
                return false;
            }
        }
        if (specialConditions.contains("LandYouCtrlLE")) {
            CardList landInPlay = sourceCard.getController().getCardsIn(ZoneType.Battlefield);
            landInPlay = landInPlay.getType("Land");
            final String maxnumber = specialConditions.split("/")[1];
            if (!(landInPlay.size() <= Integer.valueOf(maxnumber))) {
                return false;
            }
        }
        if (specialConditions.contains("LandOppCtrlLE")) {
            CardList oppLandInPlay = sourceCard.getController().getOpponent().getCardsIn(ZoneType.Battlefield);
            oppLandInPlay = oppLandInPlay.getType("Land");
            final String maxnumber = specialConditions.split("/")[1];
            if (!(oppLandInPlay.size() <= Integer.valueOf(maxnumber))) {
                return false;
            }
        }
        if (specialConditions.contains("OppCtrlMoreCreatures")) {
            CardList creaturesInPlayYou = sourceCard.getController().getCardsIn(ZoneType.Battlefield);
            creaturesInPlayYou = creaturesInPlayYou.getType("Creature");
            CardList creaturesInPlayOpp = sourceCard.getController().getOpponent().getCardsIn(ZoneType.Battlefield);
            creaturesInPlayOpp = creaturesInPlayOpp.getType("Creature");
            if (creaturesInPlayYou.size() > creaturesInPlayOpp.size()) {
                return false;
            }
        }
        if (specialConditions.contains("OppCtrlMoreLands")) {
            CardList landsInPlayYou = sourceCard.getController().getCardsIn(ZoneType.Battlefield);
            landsInPlayYou = landsInPlayYou.getType("Land");
            CardList landsInPlayOpp = sourceCard.getController().getOpponent().getCardsIn(ZoneType.Battlefield);
            landsInPlayOpp = landsInPlayOpp.getType("Land");
            if (landsInPlayYou.size() > landsInPlayOpp.size()) {
                return false;
            }
        }
        if (specialConditions.contains("EnchantedControllerCreaturesGE")) {
            CardList enchantedControllerInPlay = sourceCard.getEnchantingCard().getController()
                    .getCardsIn(ZoneType.Battlefield);
            enchantedControllerInPlay = enchantedControllerInPlay.getType("Creature");
            final String maxnumber = specialConditions.split("/")[1];
            if (!(enchantedControllerInPlay.size() >= Integer.valueOf(maxnumber))) {
                return false;
            }
        }
        if (specialConditions.contains("OppLifeLE")) {
            final int life = sourceCard.getController().getOpponent().getLife();
            final String maxnumber = specialConditions.split("/")[1];
            if (!(life <= Integer.valueOf(maxnumber))) {
                return false;
            }
        }
        if (specialConditions.contains("Threshold")) {
            if (!sourceCard.getController().hasThreshold()) {
                return false;
            }
        }
        if (specialConditions.contains("Imprint")) {
            if (sourceCard.getImprinted().isEmpty()) {
                return false;
            }
        }
        if (specialConditions.contains("Hellbent")) {
            final CardList handcards = sourceCard.getController().getCardsIn(ZoneType.Hand);
            if (handcards.size() > 0) {
                return false;
            }
        }
        if (specialConditions.contains("Metalcraft")) {
            CardList cardsinPlay = sourceCard.getController().getCardsIn(ZoneType.Battlefield);
            cardsinPlay = cardsinPlay.getType("Artifact");
            if (cardsinPlay.size() < 3) {
                return false;
            }
        }
        if (specialConditions.contains("Morbid")) {
            final CardList res = CardUtil.getThisTurnEntered(ZoneType.Graveyard, ZoneType.Battlefield, "Creature", sourceCard);
            if (res.size() < 1) {
                return false;
            }
        }
        // is a card of a certain type/color present?
        if (specialConditions.contains("isPresent")) {

            final String requirements = specialConditions.replaceAll("isPresent ", "");
            CardList cardsinPlay = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
            final String[] conditions = requirements.split(",");
            cardsinPlay = cardsinPlay.getValidCards(conditions, sourceCard.getController(), sourceCard);
            if (cardsinPlay.isEmpty()) {
                return false;
            }
        }
        //is a card of a certain type/color present in yard
        if (specialConditions.contains("isInGraveyard")) {
            final String requirements = specialConditions.replaceAll("isInGraveyard ", "");
            CardList cardsinYards = AllZoneUtil.getCardsIn(ZoneType.Graveyard);
            final String[] conditions = requirements.split(",");
            cardsinYards = cardsinYards.getValidCards(conditions, sourceCard.getController(), sourceCard);
            if (cardsinYards.isEmpty()) {
                return false;
            }
        }
        //is no card of a certain type/color present?
        if (specialConditions.contains("isNotPresent")) {
            final String requirements = specialConditions.replaceAll("isNotPresent ", "");
            CardList cardsInPlay = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
            final String[] conditions = requirements.split(",");
            cardsInPlay = cardsInPlay.getValidCards(conditions, sourceCard.getController(), sourceCard);
            if (!cardsInPlay.isEmpty()) {
                return false;
            }
        }
        if (specialConditions.contains("isEquipped")) {
            if (!sourceCard.isEquipped()) {
                return false;
            }
        }
        if (specialConditions.contains("isEnchanted")) {
            if (!sourceCard.isEnchanted()) {
                return false;
            }
        }
        if (specialConditions.contains("isUntapped")) {
            if (!sourceCard.isUntapped()) {
                return false;
            }
        }
        if (specialConditions.contains("isValid")) {
            final String requirements = specialConditions.replaceAll("isValid ", "");
            if (!sourceCard.isValid(requirements, sourceCard.getController(), sourceCard)) {
                return false;
            }
        }
        if (specialConditions.contains("isYourTurn")) {
            if (!Singletons.getModel().getGameState().getPhaseHandler().isPlayerTurn(sourceCard.getController())) {
                return false;
            }
        }
        if (specialConditions.contains("notYourTurn")) {
            if (!Singletons.getModel().getGameState().getPhaseHandler().isPlayerTurn(sourceCard.getController().getOpponent())) {
                return false;
            }
        }
        if (specialConditions.contains("OppPoisoned")) {
            if (sourceCard.getController().getOpponent().getPoisonCounters() == 0) {
                return false;
            }
        }
        if (specialConditions.contains("OppNotPoisoned")) {
            if (sourceCard.getController().getOpponent().getPoisonCounters() > 0) {
                return false;
            }
        }
        if (specialConditions.contains("ThisTurnCast")) {
            final String valid = specialConditions.split(" ")[1];
            if (CardUtil.getThisTurnCast(valid, sourceCard).size() == 0) {
                return false;
            }
        }
        if (specialConditions.contains("ThisTurnNotCast")) {
            final String valid = specialConditions.split(" ")[1];
            if (CardUtil.getThisTurnCast(valid, sourceCard).size() != 0) {
                return false;
            }
        }
        return true;

    }

    /** Constant <code>stLandManaAbilities</code>. */
    private static Command stLandManaAbilities = new Command() {
        private static final long serialVersionUID = 8005448956536998277L;

        @Override
        public void execute() {

            final HashMap<String, String> produces = new HashMap<String, String>();
            /*
             * for future use boolean naked =
             * AllZoneUtil.isCardInPlay("Naked Singularity"); boolean twist =
             * AllZoneUtil.isCardInPlay("Reality Twist"); //set up what they
             * produce produces.put("Forest", naked || twist ? "B" : "G");
             * produces.put("Island", naked == true ? "G" : "U"); if(naked)
             * produces.put("Mountain", "U"); else if(twist)
             * produces.put("Mountain", "W"); else produces.put("Mountain",
             * "R"); produces.put("Plains", naked || twist ? "R" : "W");
             * if(naked) produces.put("Swamp", "W"); else if(twist)
             * produces.put("Swamp", "G"); else produces.put("Swamp", "B");
             */
            produces.put("Forest", "G");
            produces.put("Island", "U");
            produces.put("Mountain", "R");
            produces.put("Plains", "W");
            produces.put("Swamp", "B");

            CardList lands = AllZoneUtil.getCardsInGame();
            lands = lands.filter(CardListFilter.LANDS);

            // remove all abilities granted by this Command
            for (final Card land : lands) {
                final ArrayList<AbilityMana> sas = land.getManaAbility();
                for (final SpellAbility sa : sas) {
                    if (sa.getType().equals("BasicLandTypeMana")) {
                        land.removeSpellAbility(sa);
                    }
                }
            }

            // add all appropriate mana abilities based on current types
            for (final Card land : lands) {
                if (land.isType("Swamp")) {
                    final AbilityFactory af = new AbilityFactory();
                    final SpellAbility sa = af.getAbility("AB$ Mana | Cost$ T | Produced$ " + produces.get("Swamp")
                            + " | SpellDescription$ Add " + produces.get("Swamp") + " to your mana pool.", land);
                    sa.setType("BasicLandTypeMana");
                    land.addSpellAbility(sa);
                }
                if (land.isType("Forest")) {
                    final AbilityFactory af = new AbilityFactory();
                    final SpellAbility sa = af.getAbility("AB$ Mana | Cost$ T | Produced$ " + produces.get("Forest")
                            + " | SpellDescription$ Add " + produces.get("Forest") + " to your mana pool.", land);
                    sa.setType("BasicLandTypeMana");
                    land.addSpellAbility(sa);
                }
                if (land.isType("Island")) {
                    final AbilityFactory af = new AbilityFactory();
                    final SpellAbility sa = af.getAbility("AB$ Mana | Cost$ T | Produced$ " + produces.get("Island")
                            + " | SpellDescription$ Add " + produces.get("Island") + " to your mana pool.", land);
                    sa.setType("BasicLandTypeMana");
                    land.addSpellAbility(sa);
                }
                if (land.isType("Mountain")) {
                    final AbilityFactory af = new AbilityFactory();
                    final SpellAbility sa = af.getAbility("AB$ Mana | Cost$ T | Produced$ " + produces.get("Mountain")
                            + " | SpellDescription$ Add " + produces.get("Mountain") + " to your mana pool.", land);
                    sa.setType("BasicLandTypeMana");
                    land.addSpellAbility(sa);
                }
                if (land.isType("Plains")) {
                    final AbilityFactory af = new AbilityFactory();
                    final SpellAbility sa = af.getAbility("AB$ Mana | Cost$ T | Produced$ " + produces.get("Plains")
                            + " | SpellDescription$ Add " + produces.get("Plains") + " to your mana pool.", land);
                    sa.setType("BasicLandTypeMana");
                    land.addSpellAbility(sa);
                }
            }
        } // execute()

    }; // stLandManaAbilities

    /** Constant <code>coatOfArms</code>. */
    private static Command coatOfArms = new Command() {
        private static final long serialVersionUID = 583505612126735693L;

        private final CardList gloriousAnthemList = new CardList();

        @Override
        public void execute() {
            final CardList list = this.gloriousAnthemList;
            // reset all cards in list - aka "old" cards
            for (int i2 = 0; i2 < list.size(); i2++) {
                list.get(i2).addSemiPermanentAttackBoost(-1);
                list.get(i2).addSemiPermanentDefenseBoost(-1);
            }
            // add +1/+1 to cards
            list.clear();
            final int num = AllZoneUtil.getCardsIn(ZoneType.Battlefield, "Coat of Arms").size();
            final CardList creatures = AllZoneUtil.getCardsIn(ZoneType.Battlefield).getType("Creature");

            for (Card c : creatures) {
                for (Card c2 : creatures) {
                    if (!c.equals(c2) && c.sharesCreatureTypeWith(c2)) {
                        for (int j = 0; j < num; j++) {
                            c.addSemiPermanentAttackBoost(1);
                            c.addSemiPermanentDefenseBoost(1);
                            this.gloriousAnthemList.add(c);
                        }
                    }
                }
            } // for outer
        } // execute
    }; // coatOfArms

    private static Command alphaStatus = new Command() {
        private static final long serialVersionUID = -3213793711304934358L;

        private final CardList previouslyPumped = new CardList();
        private final ArrayList<Integer> previouslyPumpedValue = new ArrayList<Integer>();

        @Override
        public void execute() {
            final CardList alphaStatuses = AllZone.getHumanPlayer().getCardsIn(ZoneType.Battlefield)
                    .getName("Alpha Status");
            alphaStatuses.addAll(AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield).getName("Alpha Status"));

            final CardList allCreatures = AllZone.getHumanPlayer().getCardsIn(ZoneType.Battlefield).getType("Creature");
            allCreatures.addAll(AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield).getType("Creature"));

            for (int i = 0; i < this.previouslyPumped.size(); i++) {
                this.previouslyPumped.get(i).addSemiPermanentAttackBoost(0 - this.previouslyPumpedValue.get(i));
                this.previouslyPumped.get(i).addSemiPermanentDefenseBoost(0 - this.previouslyPumpedValue.get(i));
            }
            this.previouslyPumped.clear();
            this.previouslyPumpedValue.clear();

            for (final Card alpha : alphaStatuses) {
                final Card enchanted = alpha.getEnchantingCard();
                int totalbuff = 0;

                for (final Card othercreat : allCreatures) {
                    if (enchanted != othercreat && othercreat.sharesCreatureTypeWith(enchanted)) {
                        totalbuff += 2;
                    }
                }

                enchanted.addSemiPermanentAttackBoost(totalbuff);
                enchanted.addSemiPermanentDefenseBoost(totalbuff);
                this.previouslyPumped.add(enchanted);
                this.previouslyPumpedValue.add(totalbuff);
            }
        }

    };

    /** stores the Command. */
    private static Command umbraStalker = new Command() {
        private static final long serialVersionUID = -3500747003228938898L;

        @Override
        public void execute() {
            // get all creatures
            final CardList cards = AllZoneUtil.getCardsIn(ZoneType.Battlefield, "Umbra Stalker");
            for (final Card c : cards) {
                final Player player = c.getController();
                final CardList grave = player.getCardsIn(ZoneType.Graveyard);
                final int pt = CardFactoryUtil.getNumberOfManaSymbolsByColor("B", grave);
                c.setBaseAttack(pt);
                c.setBaseDefense(pt);
            }
        } // execute()
    };

    /** Constant <code>Ajani_Avatar_Token</code>. */
    private static Command ajaniAvatarToken = new Command() {
        private static final long serialVersionUID = 3027329837165436727L;

        @Override
        public void execute() {
            CardList list = AllZoneUtil.getCardsIn(ZoneType.Battlefield);

            list = list.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
                    return c.getName().equals("Avatar") && c.getImageName().equals("W N N Avatar");
                }
            });
            for (int i = 0; i < list.size(); i++) {
                final Card card = list.get(i);
                final int n = card.getController().getLife();
                card.setBaseAttack(n);
                card.setBaseDefense(n);
            } // for
        } // execute
    }; // Ajani Avatar

    /** Constant <code>oldManOfTheSea</code>. */
    private static Command oldManOfTheSea = new Command() {
        private static final long serialVersionUID = 8076177362922156784L;

        @Override
        public void execute() {
            final CardList list = AllZoneUtil.getCardsIn(ZoneType.Battlefield, "Old Man of the Sea");
            for (final Card oldman : list) {
                if (!oldman.getGainControlTargets().isEmpty()) {
                    if (oldman.getNetAttack() < oldman.getGainControlTargets().get(0).getNetAttack()) {
                        final ArrayList<Command> coms = oldman.getGainControlReleaseCommands();
                        for (int i = 0; i < coms.size(); i++) {
                            coms.get(i).execute();
                        }
                    }
                }
            }
        }
    }; // Old Man of the Sea

    /** Constant <code>liuBei</code>. */
    private static Command liuBei = new Command() {

        private static final long serialVersionUID = 4235093010715735727L;

        @Override
        public void execute() {
            final CardList list = AllZoneUtil.getCardsIn(ZoneType.Battlefield, "Liu Bei, Lord of Shu");

            if (list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {

                    final Card c = list.get(i);
                    if (this.getsBonus(c)) {
                        c.setBaseAttack(4);
                        c.setBaseDefense(6);
                    } else {
                        c.setBaseAttack(2);
                        c.setBaseDefense(4);
                    }

                }
            }
        } // execute()

        private boolean getsBonus(final Card c) {
            CardList list = c.getController().getCardsIn(ZoneType.Battlefield);
            list = list.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
                    return c.getName().equals("Guan Yu, Sainted Warrior")
                            || c.getName().equals("Zhang Fei, Fierce Warrior");
                }
            });

            return list.size() > 0;
        }

    }; // Liu_Bei

    /** Constant <code>soundTheCallWolf</code>. */
    private static Command soundTheCallWolf = new Command() {
        private static final long serialVersionUID = 4614281706799537283L;

        @Override
        public void execute() {
            CardList list = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
            list = list.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
                    return c.getName().equals("Wolf")
                            && c.hasKeyword("This creature gets +1/+1 for each card "
                                    + "named Sound the Call in each graveyard.");
                }
            });

            for (int i = 0; i < list.size(); i++) {
                final Card c = list.get(i);
                c.setBaseAttack(1 + this.countSoundTheCalls());
                c.setBaseDefense(c.getBaseAttack());
            }
        }

        private int countSoundTheCalls() {
            CardList list = AllZoneUtil.getCardsIn(ZoneType.Graveyard);
            list = list.getName("Sound the Call");
            return list.size();
        }

    }; // sounTheCallWolf

    /** Constant <code>Tarmogoyf</code>. */
    private static Command tarmogoyf = new Command() {
        private static final long serialVersionUID = 5895665460018262987L;

        @Override
        public void execute() {
            // get all creatures
            final CardList list = AllZoneUtil.getCardsIn(ZoneType.Battlefield, "Tarmogoyf");

            for (int i = 0; i < list.size(); i++) {
                final Card c = list.get(i);
                c.setBaseAttack(this.countDiffTypes());
                c.setBaseDefense(c.getBaseAttack() + 1);
            }

        } // execute()

        private int countDiffTypes() {
            final CardList list = AllZoneUtil.getCardsIn(ZoneType.Graveyard);

            int count = 0;
            for (int q = 0; q < list.size(); q++) {
                if (list.get(q).isCreature()) {
                    count++;
                    break;
                }
            }
            for (int q = 0; q < list.size(); q++) {
                if (list.get(q).isSorcery()) {
                    count++;
                    break;
                }
            }
            for (int q = 0; q < list.size(); q++) {
                if (list.get(q).isInstant()) {
                    count++;
                    break;
                }
            }
            for (int q = 0; q < list.size(); q++) {
                if (list.get(q).isArtifact()) {
                    count++;
                    break;
                }
            }

            for (int q = 0; q < list.size(); q++) {
                if (list.get(q).isEnchantment()) {
                    count++;
                    break;
                }
            }

            for (int q = 0; q < list.size(); q++) {
                if (list.get(q).isLand()) {
                    count++;
                    break;
                }
            }

            for (int q = 0; q < list.size(); q++) {
                if (list.get(q).isPlaneswalker()) {
                    count++;
                    break;
                }
            }

            for (int q = 0; q < list.size(); q++) {
                if (list.get(q).isTribal()) {
                    count++;
                    break;
                }
            }
            return count;
        }
    };

    /** Constant <code>commands</code>. */
    private static HashMap<String, Command> commands = new HashMap<String, Command>();

    static {
        // Please add cards in alphabetical order so they are easier to find

        GameActionUtil.getCommands().put("Ajani_Avatar_Token", GameActionUtil.ajaniAvatarToken);
        GameActionUtil.getCommands().put("Alpha_Status", GameActionUtil.alphaStatus);
        GameActionUtil.getCommands().put("Coat_of_Arms", GameActionUtil.coatOfArms);

        GameActionUtil.getCommands().put("Liu_Bei", GameActionUtil.liuBei);
        GameActionUtil.getCommands().put("Old_Man_of_the_Sea", GameActionUtil.oldManOfTheSea);
        GameActionUtil.getCommands().put("Sound_the_Call_Wolf", GameActionUtil.soundTheCallWolf);
        GameActionUtil.getCommands().put("Tarmogoyf", GameActionUtil.tarmogoyf);
        GameActionUtil.getCommands().put("Umbra_Stalker", GameActionUtil.umbraStalker);

        // The commands above are in alphabetical order by cardname.
    }

    /**
     * <p>
     * doPowerSink.
     * </p>
     * 
     * @param p
     *            a {@link forge.game.player.Player} object.
     */
    public static void doPowerSink(final Player p) {
        // get all lands with mana abilities
        CardList lands = AllZoneUtil.getPlayerLandsInPlay(p);
        lands = lands.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return c.getManaAbility().size() > 0;
            }
        });
        // tap them
        for (final Card c : lands) {
            c.tap();
        }

        // empty mana pool
        p.getManaPool().clearPool();
    }

    /**
     * Gets the commands.
     * 
     * @return the commands
     */
    public static HashMap<String, Command> getCommands() {
        return GameActionUtil.commands;
    }

    /**
     * Sets the commands.
     * 
     * @param commands0
     *            the commands to set
     */
    public static void setCommands(final HashMap<String, Command> commands0) {
        GameActionUtil.commands = commands0;
    }

    /**
     * Gets the st land mana abilities.
     * 
     * @return the stLandManaAbilities
     */
    public static Command getStLandManaAbilities() {
        return GameActionUtil.stLandManaAbilities;
    }

    /**
     * Sets the st land mana abilities.
     * 
     * @param stLandManaAbilitiesIn
     *            the new st land mana abilities
     */
    public static void setStLandManaAbilities(final Command stLandManaAbilitiesIn) {
        GameActionUtil.stLandManaAbilities = stLandManaAbilitiesIn;
    }

    /**
     * <p>
     * getAlternativeCosts.
     * </p>
     * 
     * @param sa
     *            a SpellAbility.
     * @return an ArrayList<SpellAbility>.
     * get alternative costs as additional spell abilities
     */
    public static final ArrayList<SpellAbility> getAlternativeCosts(SpellAbility sa) {
        ArrayList<SpellAbility> alternatives = new ArrayList<SpellAbility>();
        Card source = sa.getSourceCard();
        if (!sa.isBasicSpell()) {
            return alternatives;
        }
        for (final String keyword : source.getKeyword()) {
            if (sa.isSpell() && keyword.startsWith("Flashback")) {
                final SpellAbility flashback = sa.copy();
                flashback.setFlashBackAbility(true);
                SpellAbilityRestriction sar = new SpellAbilityRestriction();
                sar.setVariables(sa.getRestrictions());
                sar.setZone(ZoneType.Graveyard);
                flashback.setRestrictions(sar);

                // there is a flashback cost (and not the cards cost)
                if (!keyword.equals("Flashback")) {
                    final Cost fbCost = new Cost(source, keyword.substring(10), false);
                    flashback.setPayCosts(fbCost);
                }
                alternatives.add(flashback);
            }
            if (sa.isSpell() && keyword.equals("May be played without paying its mana cost")) {
                final SpellAbility newSA = sa.copy();
                SpellAbilityRestriction sar = new SpellAbilityRestriction();
                sar.setVariables(sa.getRestrictions());
                sar.setZone(null);
                newSA.setRestrictions(sar);
                final Cost cost = new Cost(source, "", false);
                if (newSA.getPayCosts() != null) {
                    for (final CostPart part : newSA.getPayCosts().getCostParts()) {
                        if (!(part instanceof CostMana)) {
                            cost.getCostParts().add(part);
                        }
                    }
                }
                newSA.setBasicSpell(false);
                newSA.setPayCosts(cost);
                newSA.setManaCost("");
                newSA.setDescription(sa.getDescription() + " (without paying its mana cost)");
                alternatives.add(newSA);
            }
            if (sa.isSpell() && keyword.equals("May be played by your opponent without paying its mana cost")) {
                final SpellAbility newSA = sa.copy();
                SpellAbilityRestriction sar = new SpellAbilityRestriction();
                sar.setVariables(sa.getRestrictions());
                sar.setZone(null);
                sar.setOpponentOnly(true);
                newSA.setRestrictions(sar);
                final Cost cost = new Cost(source, "", false);
                if (newSA.getPayCosts() != null) {
                    for (final CostPart part : newSA.getPayCosts().getCostParts()) {
                        if (!(part instanceof CostMana)) {
                            cost.getCostParts().add(part);
                        }
                    }
                }
                newSA.setBasicSpell(false);
                newSA.setPayCosts(cost);
                newSA.setManaCost("");
                newSA.setDescription(sa.getDescription() + " (without paying its mana cost)");
                alternatives.add(newSA);
            }
            if (sa.isSpell() && keyword.startsWith("May be played without paying its mana cost and as though it has flash")) {
                final SpellAbility newSA = sa.copy();
                SpellAbilityRestriction sar = new SpellAbilityRestriction();
                sar.setVariables(sa.getRestrictions());
                sar.setInstantSpeed(true);
                newSA.setRestrictions(sar);
                final Cost cost = new Cost(source, "", false);
                if (newSA.getPayCosts() != null) {
                    for (final CostPart part : newSA.getPayCosts().getCostParts()) {
                        if (!(part instanceof CostMana)) {
                            cost.getCostParts().add(part);
                        }
                    }
                }
                newSA.setBasicSpell(false);
                newSA.setPayCosts(cost);
                newSA.setManaCost("");
                newSA.setDescription(sa.getDescription() + " (without paying its mana cost and as though it has flash)");
                alternatives.add(newSA);
            }
            if (sa.isSpell() && keyword.startsWith("Alternative Cost")) {
                final SpellAbility newSA = sa.copy();
                final Cost cost = new Cost(source, keyword.substring(17), false);
                if (newSA.getPayCosts() != null) {
                    for (final CostPart part : newSA.getPayCosts().getCostParts()) {
                        if (!(part instanceof CostMana)) {
                            cost.getCostParts().add(part);
                        }
                    }
                }
                newSA.setBasicSpell(false);
                newSA.setPayCosts(cost);
                newSA.setManaCost("");
                newSA.setDescription(sa.getDescription() + " (by paying " + cost.toSimpleString() + " instead of its mana cost)");
                alternatives.add(newSA);
            }
        }
        return alternatives;
    }

    /**
     * <p>
     * getSpliceAbilities.
     * </p>
     * 
     * @param sa
     *            a SpellAbility.
     * @return an ArrayList<SpellAbility>.
     * get abilities with all Splice options
     */
    public static final ArrayList<SpellAbility> getSpliceAbilities(SpellAbility sa) {
        ArrayList<SpellAbility> newSAs = new ArrayList<SpellAbility>();
        ArrayList<SpellAbility> allSAs = new ArrayList<SpellAbility>();
        allSAs.add(sa);
        Card source = sa.getSourceCard();
        
        if (!sa.isSpell() || !source.isType("Arcane") /*|| sa.getAbilityFactory() == null*/) {
            return newSAs;
        }
        
        for (Card c : sa.getActivatingPlayer().getCardsIn(ZoneType.Hand)) {
            for (String keyword : c.getKeyword()) {
                if (!keyword.startsWith("Splice")) {
                    continue;
                }
                String newSubSAString = c.getCharacteristics().getIntrinsicAbility().get(0);
                newSubSAString = newSubSAString.replace("SP", "DB");
                final AbilityFactory af = new AbilityFactory();
                final AbilitySub newSubSA = (AbilitySub) af.getAbility(newSubSAString, source);
                ArrayList<SpellAbility> addSAs = new ArrayList<SpellAbility>();
                // Add the subability to all existing variants
                for (SpellAbility s : allSAs) {
                    final Cost newCost = new Cost(source, keyword.substring(19), false);
                    final SpellAbility newSA = s.copy();
                    Cost oldCost = newSA.getPayCosts();
                    if (newSA.getPayCosts() != null) {
                        for (final CostPart part : oldCost.getCostParts()) {
                            if (!(part instanceof CostMana)) {
                                newCost.getCostParts().add(part);
                            } else {
                                ManaCost manaCost1 = new ManaCost(part.toString());
                                manaCost1.combineManaCost(newCost.getCostMana().toString());
                                newCost.getCostMana().setMana(manaCost1.toString());
                            }
                        }
                    }
                    newSA.setBasicSpell(false);
                    newSA.setPayCosts(newCost);
                    newSA.setManaCost("");
                    newSA.setDescription(s.getDescription() + " (Splicing " + c + " onto it)");
                    SpellAbility child = newSA;
                    while (child.getSubAbility() != null) {
                        AbilitySub newChild = (AbilitySub) child.getSubAbility().getCopy();
                        child.setSubAbility(newChild);
                        newChild.setParent(child);
                        child = newChild;
                    }
                    child.setSubAbility(newSubSA);
                    newSubSA.setParent(child);
                    newSAs.add(newSA);
                    addSAs.add(newSA);
                }
                allSAs.addAll(addSAs);
                break;
            }
        }

        return newSAs;
    }
} // end class GameActionUtil
