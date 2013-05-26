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
package forge.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;


import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.CardPredicates.Presets;
import forge.CardUtil;
import forge.Command;
import forge.Constant;
import forge.CounterType;
import forge.card.MagicColor;
import forge.card.ability.AbilityFactory;
import forge.card.ability.AbilityFactory.AbilityRecordType;
import forge.card.ability.AbilityUtils;
import forge.card.ability.ApiType;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostBeingPaid;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilityManaPart;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.OptionalCost;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityRestriction;
import forge.game.ai.AiController;
import forge.game.player.HumanPlay;
import forge.game.player.Player;
import forge.game.player.PlayerControllerAi;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.GuiDialog;
import forge.sound.SoundEffectType;
import forge.util.TextUtil;


/**
 * <p>
 * GameActionUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class GameActionUtil {
    
    private static final class CascadeAbility extends Ability {
        private final Player controller;
        private final Card cascCard;

        /**
         * TODO: Write javadoc for Constructor.
         * @param sourceCard
         * @param manaCost
         * @param controller
         * @param cascCard
         */
        private CascadeAbility(Card sourceCard, ManaCost manaCost, Player controller, Card cascCard) {
            super(sourceCard, manaCost);
            this.controller = controller;
            this.cascCard = cascCard;
        }

        @Override
        public void resolve() {
            final GameState game =controller.getGame(); 
            final List<Card> topOfLibrary = controller.getCardsIn(ZoneType.Library);
            final List<Card> revealed = new ArrayList<Card>();

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
            GuiChoose.oneOrNone("Revealed cards:", revealed);

            if (cascadedCard != null) {
                Player p = cascadedCard.getController();
                // returns boolean, but spell resolution stays inside the method anyway (for now)
                if ( p.getController().playCascade(cascadedCard, cascCard) )
                    revealed.remove(cascadedCard);
            }
            CardLists.shuffle(revealed);
            for (final Card bottom : revealed) {
                game.getAction().moveToBottomOfLibrary(bottom);
            }
        }
    }

    private static final class CascadeExecutor implements Command {
        private final Card c;
        private final GameState game;
        private final Player controller;
        
        private static final long serialVersionUID = -845154812215847505L;

        /**
         * TODO: Write javadoc for Constructor.
         * @param controller
         * @param c
         */
        private CascadeExecutor(Player controller, Card c, final GameState game) {
            this.controller = controller;
            this.c = c;
            this.game = game;
        }

        @Override
        public void run() {
            if (!c.isCopiedSpell()) {
                final List<Card> maelstromNexii = CardLists.filter(controller.getGame().getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals("Maelstrom Nexus"));

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

            final Ability ability = new CascadeAbility(c, ManaCost.ZERO, controller, cascCard);
            final StringBuilder sb = new StringBuilder();
            sb.append(c).append(" - Cascade.");
            ability.setStackDescription(sb.toString());
            ability.setActivatingPlayer(controller);

            game.getStack().addSimultaneousStackEntry(ability);

        }
    }
    
    /** 
     * TODO: Write javadoc for this type.
     *
     */
    private static final class RippleAbility extends Ability {
        private final Player controller;
        private final int rippleCount;
        private final Card rippleCard;
    
        /**
         * TODO: Write javadoc for Constructor.
         * @param sourceCard
         * @param manaCost
         * @param controller
         * @param rippleCount
         * @param rippleCard
         */
        private RippleAbility(Card sourceCard, ManaCost manaCost, Player controller, int rippleCount,
                Card rippleCard) {
            super(sourceCard, manaCost);
            this.controller = controller;
            this.rippleCount = rippleCount;
            this.rippleCard = rippleCard;
        }
    
        @Override
        public void resolve() {
            final List<Card> topOfLibrary = controller.getCardsIn(ZoneType.Library);
            final List<Card> revealed = new ArrayList<Card>();
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
            GuiChoose.oneOrNone("Revealed cards:", revealed);
            for (int i = 0; i < rippleMax; i++) {
                if (rippledCards[i] != null) {
                    Player p = rippledCards[i].getController();
    
                    if (p.isHuman()) {
                        if (GuiDialog.confirm(rippledCards[i], "Cast " + rippledCards[i].getName() + "?")) {
                            HumanPlay.playCardWithoutPayingManaCost(p, rippledCards[i]);
                            revealed.remove(rippledCards[i]);
                        }
                    } else {
                        final AiController aic = ((PlayerControllerAi)p.getController()).getAi();
                        SpellAbility saPlayed = aic.chooseAndPlaySa(rippledCards[i].getBasicSpells(), false, true);
                        if ( saPlayed != null )
                            revealed.remove(rippledCards[i]);
                    }
                }
            }
            CardLists.shuffle(revealed);
            for (final Card bottom : revealed) {
                controller.getGame().getAction().moveToBottomOfLibrary(bottom);
            }
        }
    
    }

    /** 
     * TODO: Write javadoc for this type.
     *
     */
    private static final class RippleExecutor implements Command {
        private final Player controller;
        private final Card c;
        private static final long serialVersionUID = -845154812215847505L;

        /**
         * TODO: Write javadoc for Constructor.
         * @param controller
         * @param c
         */
        private RippleExecutor(Player controller, Card c) {
            this.controller = controller;
            this.c = c;
        }

        @Override
        public void run() {

            final List<Card> thrummingStones = controller.getCardsIn(ZoneType.Battlefield, "Thrumming Stone");
            for (int i = 0; i < thrummingStones.size(); i++) {
                c.addExtrinsicKeyword("Ripple:4");
            }

            for (String parse : c.getKeyword()) {
                if (parse.startsWith("Ripple")) {
                    final String[] k = parse.split(":");
                    this.doRipple(c, Integer.valueOf(k[1]), controller);
                }
            }
        } // execute()

        void doRipple(final Card c, final int rippleCount, final Player controller) {
            final Card rippleCard = c;

            if (controller.isComputer() || GuiDialog.confirm(c, "Activate Ripple for " + c + "?")) {

                final Ability ability = new RippleAbility(c, ManaCost.ZERO, controller, rippleCount, rippleCard);
                final StringBuilder sb = new StringBuilder();
                sb.append(c).append(" - Ripple.");
                ability.setStackDescription(sb.toString());
                ability.setDescription(sb.toString());
                ability.setActivatingPlayer(controller);

                controller.getGame().getStack().addSimultaneousStackEntry(ability);

            }
        }
    }

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
        // (called from MagicStack.java)

        final GameState game = sa.getActivatingPlayer().getGame(); 
        final Command cascade = new CascadeExecutor(sa.getActivatingPlayer(), sa.getSourceCard(), game);
        cascade.run();
        final Command ripple = new RippleExecutor(sa.getActivatingPlayer(), sa.getSourceCard());
        ripple.run();
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
        if (!c.isInPlay()) return;

        for (final String kw : c.getKeyword()) {
            if(!kw.startsWith("Whenever a creature dealt damage by CARDNAME this turn is put into a graveyard, put")) {
                continue;
            }
            final Card thisCard = c;

            final Ability ability2 = new Ability(c, ManaCost.ZERO) {
                @Override
                public void resolve() {
                    CounterType counter = CounterType.P1P1;
                    if (kw.contains("+2/+2")) {
                        counter = CounterType.P2P2;
                    }
                    if (thisCard.isInPlay()) {
                        thisCard.addCounter(counter, 1, true);
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

            c.getGame().getStack().addSimultaneousStackEntry(ability2);

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

        if (c.hasStartOfKeyword("Poisonous")) {
            final int keywordPosition = c.getKeywordPosition("Poisonous");
            final String parse = c.getKeyword().get(keywordPosition).toString();
            final String[] k = parse.split(" ");
            final int poison = Integer.parseInt(k[1]);

            final Ability ability = new Ability(c, ManaCost.ZERO) {
                @Override
                public void resolve() {
                    player.addPoisonCounters(poison, c);
                }
            };

            final StringBuilder sb = new StringBuilder();
            sb.append(c);
            sb.append(" - Poisonous: ");
            sb.append(c.getController().getOpponent());
            sb.append(" gets ").append(poison).append(" poison counter");
            if (poison != 1) {
                sb.append("s");
            }
            sb.append(".");

            ability.setStackDescription(sb.toString());

            for (String kw : c.getKeyword()) {
                if (kw.startsWith("Poisonous")) {
                    player.getGame().getStack().addSimultaneousStackEntry(ability);
                }
            }
        }

        if (c.getName().equals("Scalpelexis")) {
            GameActionUtil.playerCombatDamageScalpelexis(c);
        }
        if (c.isEnchantedBy("Celestial Mantle")) {
            GameActionUtil.executeCelestialMantle(c);
        }

        c.getDamageHistory().registerCombatDamage(player);

        // Play the Life Loss sound
        player.getGame().getEvents().post(SoundEffectType.LifeLoss);
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
                final Ability doubleLife = new Ability(aura, ManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        final int life = enchanted.getController().getLife();
                        enchanted.getController().setLife(life * 2, aura);
                    }
                };
                doubleLife.setStackDescription(aura.getName() + " - " + enchanted.getController()
                        + " doubles his or her life total.");

                enchanted.getGame().getStack().addSimultaneousStackEntry(doubleLife);

            }
        }
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
        final GameState game = player.getGame();

        if (c.getNetAttack() > 0) {
            final Ability ability = new Ability(c, ManaCost.ZERO) {
                @Override
                public void resolve() {

                    final List<Card> libList = new ArrayList<Card>(opponent.getCardsIn(ZoneType.Library));
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
                        game.getAction().exile(c);
                    }
                }
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Scalpelexis - ").append(opponent);
            sb.append(" exiles the top four cards of his or her library. ");
            sb.append("If two or more of those cards have the same name, repeat this process.");
            ability.setStackDescription(sb.toString());
            ability.setDescription(sb.toString());

            game.getStack().addSimultaneousStackEntry(ability);

        }
    }

    /** stores the Command. */
    private static Function<GameState, ?> umbraStalker = new Function<GameState, Object>() {
        @Override
        public Object apply(GameState game) {
            // get all creatures
            final List<Card> cards = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals("Umbra Stalker"));
            for (final Card c : cards) {
                final Player player = c.getController();
                final List<Card> grave = player.getCardsIn(ZoneType.Graveyard);
                final int pt = CardFactoryUtil.getNumberOfManaSymbolsByColor("B", grave);
                c.setBaseAttack(pt);
                c.setBaseDefense(pt);
            }
            return null;
        } // execute()
    };

    /** Constant <code>oldManOfTheSea</code>. */
    private static Function<GameState, ?> oldManOfTheSea = new Function<GameState, Object>() {

        @Override
        public Object apply(GameState game) {
            final List<Card> list = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals("Old Man of the Sea"));
            for (final Card oldman : list) {
                if (!oldman.getGainControlTargets().isEmpty()) {
                    if (oldman.getNetAttack() < oldman.getGainControlTargets().get(0).getNetAttack()) {
                        final List<Command> coms = oldman.getGainControlReleaseCommands();
                        for (int i = 0; i < coms.size(); i++) {
                            coms.get(i).run();
                        }
                    }
                }
            }
            return null;
        }
    }; // Old Man of the Sea

    /** Constant <code>liuBei</code>. */
    private static Function<GameState, ?> liuBei = new Function<GameState, Object>() {

        @Override
        public Object apply(GameState game) {
            final List<Card> list = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals("Liu Bei, Lord of Shu"));

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
            return null;
        } // execute()

        private boolean getsBonus(final Card c) {
            for (Card card : c.getController().getCardsIn(ZoneType.Battlefield)) {
                if (card.getName().equals("Guan Yu, Sainted Warrior")
                        || card.getName().equals("Zhang Fei, Fierce Warrior")) {
                    return true;
                }
            }
            return false;
        }

    }; // Liu_Bei

    /** Constant <code>commands</code>. */
    private final static HashMap<String, Function<GameState, ?>> commands = new HashMap<String, Function<GameState, ?>>();

    static {
        // Please add cards in alphabetical order so they are easier to find

        GameActionUtil.getCommands().put("Liu_Bei", GameActionUtil.liuBei);
        GameActionUtil.getCommands().put("Old_Man_of_the_Sea", GameActionUtil.oldManOfTheSea);
        GameActionUtil.getCommands().put("Umbra_Stalker", GameActionUtil.umbraStalker);

        // The commands above are in alphabetical order by cardname.
    }

    /**
     * Gets the commands.
     * 
     * @return the commands
     */
    public static Map<String, Function<GameState, ?>> getCommands() {
        return GameActionUtil.commands;
    }

    /**
     * Gets the st land mana abilities.
     * @param game 
     * 
     * @return the stLandManaAbilities
     */
    public static void grantBasicLandsManaAbilities(GameState game) {
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

        List<Card> lands = game.getCardsIn(ZoneType.Battlefield);
        lands = CardLists.filter(lands, Presets.LANDS);

        // remove all abilities granted by this Command
        for (final Card land : lands) {
            List<SpellAbility> origManaAbs = Lists.newArrayList(land.getManaAbility());
            List<SpellAbility> manaAbs = land.getCharacteristics().getManaAbility();
            // will get comodification exception without a different list
            for (final SpellAbility sa : origManaAbs) {
                if (sa.getType().equals("BasicLandTypeMana")) {
                    manaAbs.remove(sa);
                }
            }
        }

        // add all appropriate mana abilities based on current types
        for (String landType : Constant.Color.BASIC_LANDS) {
            String color = MagicColor.toShortString(Constant.Color.BASIC_LAND_TYPE_TO_COLOR_MAP.get(landType));
            String abString = "AB$ Mana | Cost$ T | Produced$ " + color + " | SpellDescription$ Add " + color + " to your mana pool.";
            for (final Card land : lands) {
                if (land.isType(landType)) {
                    final SpellAbility sa = AbilityFactory.getAbility(abString, land);
                    sa.setType("BasicLandTypeMana");
                    land.getCharacteristics().getManaAbility().add(sa);
                }
            }
        }
    } // stLandManaAbilities

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
                    flashback.setPayCosts(new Cost(keyword.substring(10), false));
                }
                alternatives.add(flashback);
            }
            if (sa.isSpell() && keyword.equals("May be played without paying its mana cost")) {
                final SpellAbility newSA = sa.copy();
                SpellAbilityRestriction sar = new SpellAbilityRestriction();
                sar.setVariables(sa.getRestrictions());
                sar.setZone(null);
                newSA.setRestrictions(sar);
                newSA.setBasicSpell(false);
                newSA.setPayCosts(newSA.getPayCosts().copyWithNoMana());
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
                newSA.setBasicSpell(false);
                newSA.setPayCosts(newSA.getPayCosts().copyWithNoMana());
                newSA.setDescription(sa.getDescription() + " (without paying its mana cost)");
                alternatives.add(newSA);
            }
            if (sa.isSpell() && keyword.startsWith("May be played without paying its mana cost and as though it has flash")) {
                final SpellAbility newSA = sa.copy();
                SpellAbilityRestriction sar = new SpellAbilityRestriction();
                sar.setVariables(sa.getRestrictions());
                sar.setInstantSpeed(true);
                newSA.setRestrictions(sar);
                newSA.setBasicSpell(false);
                newSA.setPayCosts(newSA.getPayCosts().copyWithNoMana());
                newSA.setDescription(sa.getDescription() + " (without paying its mana cost and as though it has flash)");
                alternatives.add(newSA);
            }
            if (sa.isSpell() && keyword.startsWith("Alternative Cost")) {
                final SpellAbility newSA = sa.copy();
                newSA.setBasicSpell(false);
                String kw = keyword;
                if (keyword.contains("ConvertedManaCost")) {
                    final String cmc = Integer.toString(sa.getSourceCard().getCMC());
                    kw = keyword.replace("ConvertedManaCost", cmc);
                }
                final Cost cost = new Cost(kw.substring(17), false).add(newSA.getPayCosts().copyWithNoMana());
                newSA.setPayCosts(cost);
                newSA.setDescription(sa.getDescription() + " (by paying " + cost.toSimpleString() + " instead of its mana cost)");
                alternatives.add(newSA);
            }
            if (sa.isSpell() && keyword.equals("You may cast CARDNAME any time you could cast an instant if you pay 2 more to cast it.")) {
                final SpellAbility newSA = sa.copy();
                newSA.setBasicSpell(false);
                String cost = source.getManaCost().toString();
                ManaCostBeingPaid newCost = new ManaCostBeingPaid(cost);
                newCost.increaseColorlessMana(2);
                cost = newCost.toString();
                final Cost actualcost = new Cost(cost, false);
                newSA.setPayCosts(actualcost);
                SpellAbilityRestriction sar = new SpellAbilityRestriction();
                sar.setVariables(sa.getRestrictions());
                sar.setInstantSpeed(true);
                newSA.setRestrictions(sar);
                newSA.setDescription(sa.getDescription() + " (by paying " + actualcost.toSimpleString() + " instead of its mana cost)");
                alternatives.add(newSA);
            }
            if (sa.hasParam("Equip") && sa instanceof AbilityActivated && keyword.equals("EquipInstantSpeed")) {
                final SpellAbility newSA = ((AbilityActivated) sa).getCopy();
                SpellAbilityRestriction sar = new SpellAbilityRestriction();
                sar.setVariables(sa.getRestrictions());
                sar.setSorcerySpeed(false);
                sar.setInstantSpeed(true);
                newSA.setRestrictions(sar);
                newSA.setDescription(sa.getDescription() + " (you may activate any time you could cast an instant )");
                alternatives.add(newSA);
            }
        }
        return alternatives;
    }

    /**
     * get optional additional costs.
     * 
     * @param original
     *            the original sa
     * @return an ArrayList<SpellAbility>.
     */
    public static List<SpellAbility> getOptionalCosts(final SpellAbility original) {
        final List<SpellAbility> abilities = new ArrayList<SpellAbility>();

        final Card source = original.getSourceCard();
        abilities.add(original);
        if (!original.isSpell()) {
            return abilities;
        }

        // Buyback, Kicker
        for (String keyword : source.getKeyword()) {
            if (keyword.startsWith("AlternateAdditionalCost")) {
                final List<SpellAbility> newAbilities = new ArrayList<SpellAbility>();
                String[] costs = TextUtil.split(keyword, ':');
                for (SpellAbility sa : abilities) {
                    final SpellAbility newSA = sa.copy();
                    newSA.setBasicSpell(false);
                    
                    final Cost cost1 = new Cost(costs[1], false);
                    newSA.setDescription(sa.getDescription() + " (Additional cost " + cost1.toSimpleString() + ")");
                    newSA.setPayCosts(cost1.add(sa.getPayCosts()));
                    if (newSA.canPlay()) {
                        newAbilities.add(newSA);
                    }

                    //second option
                    final SpellAbility newSA2 = sa.copy();
                    newSA2.setBasicSpell(false);

                    final Cost cost2 = new Cost(costs[2], false);
                    newSA2.setDescription(sa.getDescription() + " (Additional cost " + cost2.toSimpleString() + ")");
                    newSA2.setPayCosts(cost2.add(sa.getPayCosts()));
                    if (newSA2.canPlay()) {
                        newAbilities.add(newAbilities.size(), newSA2);
                    }
                }
                abilities.clear();
                abilities.addAll(newAbilities);
            } else if (keyword.startsWith("Buyback")) {
                for (int i = 0; i < abilities.size(); i++) {
                    final SpellAbility newSA = abilities.get(i).copy();
                    newSA.setBasicSpell(false);
                    newSA.setPayCosts(new Cost(keyword.substring(8), false).add(newSA.getPayCosts()));
                    newSA.setDescription(newSA.getDescription() + " (with Buyback)");
                    newSA.addOptionalCost(OptionalCost.Buyback);
                    if ( newSA.canPlay() )
                        abilities.add(++i, newSA);
                }
            } else if (keyword.startsWith("Kicker")) {
                for (int i = 0; i < abilities.size(); i++) {
                    String[] sCosts = TextUtil.split(keyword.substring(7), ':');
                    int iUnKicked = i;
                    for(int j = 0; j < sCosts.length; j++) {
                        final SpellAbility newSA = abilities.get(iUnKicked).copy();
                        newSA.setBasicSpell(false);
                        final Cost cost = new Cost(sCosts[j], false);
                        newSA.setDescription(newSA.getDescription() + " (Kicker " + cost.toSimpleString() + ")");
                        newSA.setPayCosts(cost.add(newSA.getPayCosts()));
                        newSA.addOptionalCost(j == 0 ? OptionalCost.Kicker1 : OptionalCost.Kicker2);
                        if ( newSA.canPlay() )
                            abilities.add(++i, newSA);
                    }
                    if(sCosts.length == 2) { // case for both kickers - it's hardcoded since they never have more that 2 kickers
                        final SpellAbility newSA = abilities.get(iUnKicked).copy();
                        newSA.setBasicSpell(false);
                        final Cost cost1 = new Cost(sCosts[0], false);
                        final Cost cost2 = new Cost(sCosts[1], false);
                        newSA.setDescription(newSA.getDescription() + String.format(" (Both kickers: %s and %s)", cost1.toSimpleString(), cost2.toSimpleString()));
                        newSA.setPayCosts(cost2.add(cost1.add(newSA.getPayCosts())));
                        newSA.addOptionalCost(OptionalCost.Kicker1);
                        newSA.addOptionalCost(OptionalCost.Kicker2);
                        if ( newSA.canPlay() )
                            abilities.add(++i, newSA);
                    }
                }
            } else if (keyword.startsWith("Conspire")) {
                for (int i = 0; i < abilities.size(); i++) {
                    final SpellAbility newSA = abilities.get(i).copy();
                    newSA.setBasicSpell(false);
                    final String conspireCost = "tapXType<2/Creature.SharesColorWith/untapped creature you control that shares a color with " + source.getName() + ">";
                    newSA.setPayCosts(new Cost(conspireCost, false).add(newSA.getPayCosts()));
                    newSA.setDescription(newSA.getDescription() + " (Conspire)");
                    newSA.addOptionalCost(OptionalCost.Conspire);
                    if ( newSA.canPlay() )
                        abilities.add(++i, newSA);
                }
            }
        }

        // Splice
        final List<SpellAbility> newAbilities = new ArrayList<SpellAbility>();
        for (SpellAbility sa : abilities) {
            if( sa.isSpell() && sa.getSourceCard().isType("Arcane") && sa.getApi() != null ) {
                newAbilities.addAll(GameActionUtil.getSpliceAbilities(sa));
            }
        }
        abilities.addAll(newAbilities);
        

        return abilities;
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
    private  static final ArrayList<SpellAbility> getSpliceAbilities(SpellAbility sa) {
        ArrayList<SpellAbility> newSAs = new ArrayList<SpellAbility>();
        ArrayList<SpellAbility> allSaCombinations = new ArrayList<SpellAbility>();
        allSaCombinations.add(sa);
        Card source = sa.getSourceCard();

    
        for (Card c : sa.getActivatingPlayer().getCardsIn(ZoneType.Hand)) {
            if (c.equals(source)) {
                continue;
            }

            String spliceKwCost = null;
            for (String keyword : c.getKeyword()) {
                if (keyword.startsWith("Splice")) {
                    spliceKwCost = keyword.substring(19); 
                    break;
                }
            }

            if( spliceKwCost == null )
                continue;

            Map<String, String> params = AbilityFactory.getMapParams(c.getCharacteristics().getUnparsedAbilities().get(0));
            AbilityRecordType rc = AbilityRecordType.getRecordType(params);
            ApiType api = rc.getApiTypeOf(params);
            AbilitySub subAbility = (AbilitySub) AbilityFactory.getAbility(AbilityRecordType.SubAbility, api, params, null, c);

            // Add the subability to all existing variants
            for (int i = 0; i < allSaCombinations.size(); ++i) {
                //create a new spell copy
                final SpellAbility newSA = allSaCombinations.get(i).copy();
                newSA.setBasicSpell(false);
                newSA.setPayCosts(new Cost(spliceKwCost, false).add(newSA.getPayCosts()));
                newSA.setDescription(newSA.getDescription() + " (Splicing " + c + " onto it)");
                newSA.addSplicedCards(c);

                // copy all subAbilities
                SpellAbility child = newSA;
                while (child.getSubAbility() != null) {
                    AbilitySub newChild = child.getSubAbility().getCopy();
                    child.setSubAbility(newChild);
                    child.setActivatingPlayer(newSA.getActivatingPlayer());
                    child = newChild;
                }

                //add the spliced ability to the end of the chain
                child.setSubAbility(subAbility);

                //set correct source and activating player to all the spliced abilities
                child = subAbility;
                while (child != null) {
                    child.setSourceCard(source);
                    child.setActivatingPlayer(newSA.getActivatingPlayer());
                    child = child.getSubAbility();
                }
                newSAs.add(newSA);
                allSaCombinations.add(++i, newSA);
            }
        }
    
        return newSAs;
    }

    /**
     * <p>
     * hasUrzaLands.
     * </p>
     * 
     * @param p
     *            a {@link forge.game.player.Player} object.
     * @return a boolean.
     */
    private static boolean hasUrzaLands(final Player p) {
        final List<Card> landsControlled = p.getCardsIn(ZoneType.Battlefield);
        return Iterables.any(landsControlled, CardPredicates.nameEquals("Urza's Mine"))
                && Iterables.any(landsControlled, CardPredicates.nameEquals("Urza's Tower"))
                && Iterables.any(landsControlled, CardPredicates.nameEquals("Urza's Power Plant"));
    }

    /**
     * <p>
     * generatedMana.
     * </p>
     * 
     * @param abMana
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    public static String generatedMana(final SpellAbility sa) {
        // Calculate generated mana here for stack description and resolving

        int amount = sa.hasParam("Amount") ? AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("Amount"), sa) : 1;

        AbilityManaPart abMana = sa.getManaPart();
        String baseMana;
        if (abMana.isComboMana()) {
            baseMana = abMana.getExpressChoice();
            if (baseMana.isEmpty()) {
                baseMana = abMana.getOrigProduced();
            }
        }
        else if (abMana.isAnyMana()) {
            baseMana = abMana.getExpressChoice();
            if (baseMana.isEmpty()) {
                baseMana = "Any";
            }
        }
        else if (sa.getApi() == ApiType.ManaReflected) {
            baseMana = abMana.getExpressChoice();
        }
        else {
            baseMana = abMana.mana();
        }

        if (sa.hasParam("Bonus")) {
            // For mana abilities that get a bonus
            // Bonus currently MULTIPLIES the base amount. Base Amounts should
            // ALWAYS be Base
            int bonus = 0;
            if (sa.getParam("Bonus").equals("UrzaLands")) {
                if (hasUrzaLands(sa.getActivatingPlayer())) {
                    bonus = Integer.parseInt(sa.getParam("BonusProduced"));
                }
            }

            amount += bonus;
        }

        if (sa.getSubAbility() != null) {
            // Mark SAs with subAbilities as undoable. These are generally things like damage, and other stuff
            // that's hard to track and remove
            sa.setUndoable(false);
        } else {      
            try {
                if ((sa.getParam("Amount") != null) && (amount != Integer.parseInt(sa.getParam("Amount")))) {
                    sa.setUndoable(false);
                }
            } catch (final NumberFormatException n) {
                sa.setUndoable(false);
            }
        }

        final StringBuilder sb = new StringBuilder();
        if (amount == 0) {
            sb.append("0");
        }
        else if (abMana.isComboMana()) {
            // amount is already taken care of in resolve method for combination mana, just append baseMana
            sb.append(baseMana);
        }
        else {
            if(StringUtils.isNumeric(baseMana)) {
                sb.append(amount * Integer.parseInt(baseMana));
            } else {
                sb.append(baseMana);
                for (int i = 1; i < amount; i++) {
                    sb.append(" ").append(baseMana);
                }
            }
        }
        return sb.toString();
    }
} // end class GameActionUtil
