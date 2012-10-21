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
package forge.card.abilityfactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import forge.Card;

import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

/**
 * <p>
 * AbilityFactory_Token class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityFactoryToken extends AbilityFactory {
    private AbilityFactory abilityFactory = null;

    private final String tokenAmount;
    private final String tokenName;
    private final String[] tokenTypes;
    private String tokenOwner;
    private final String[] tokenColors;
    private final String[] tokenKeywords;
    private final String tokenPower;
    private final String tokenToughness;
    private final String tokenImage;
    private String[] tokenAbilities;
    private String[] tokenTriggers;
    private String[] tokenSVars;
    private String[] tokenStaticAbilities;
    private boolean tokenTapped;
    private boolean tokenAttacking;

    /**
     * <p>
     * Constructor for AbilityFactory_Token.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     */
    public AbilityFactoryToken(final AbilityFactory af) {
        this.abilityFactory = af;

        final HashMap<String, String> mapParams = af.getMapParams();
        String image;
        String[] keywords;

        if (mapParams.containsKey("TokenKeywords")) {
            // TODO: Change this Split to a semicolon or something else
            keywords = mapParams.get("TokenKeywords").split("<>");
        } else {
            keywords = new String[0];
        }

        if (mapParams.containsKey("TokenImage")) {
            image = mapParams.get("TokenImage");
        } else {
            image = "";
        }

        if (mapParams.containsKey("TokenTapped")) {
            this.tokenTapped = mapParams.get("TokenTapped").equals("True");
        } else {
            this.tokenTapped = false;
        }
        if (mapParams.containsKey("TokenAttacking")) {
            this.tokenAttacking = mapParams.get("TokenAttacking").equals("True");
        } else {
            this.tokenAttacking = false;
        }

        if (mapParams.containsKey("TokenAbilities")) {
            this.tokenAbilities = mapParams.get("TokenAbilities").split(",");
        } else {
            this.tokenAbilities = null;
        }
        if (mapParams.containsKey("TokenTriggers")) {
            this.tokenTriggers = mapParams.get("TokenTriggers").split(",");
        } else {
            this.tokenTriggers = null;
        }
        if (mapParams.containsKey("TokenSVars")) {
            this.tokenSVars = mapParams.get("TokenSVars").split(",");
        } else {
            this.tokenSVars = null;
        }
        if (mapParams.containsKey("TokenStaticAbilities")) {
            this.tokenStaticAbilities = mapParams.get("TokenStaticAbilities").split(",");
        } else {
            this.tokenStaticAbilities = null;
        }

        this.tokenAmount = mapParams.get("TokenAmount");
        this.tokenPower = mapParams.get("TokenPower");
        this.tokenToughness = mapParams.get("TokenToughness");
        this.tokenName = mapParams.get("TokenName");
        this.tokenTypes = mapParams.get("TokenTypes").split(",");
        this.tokenColors = mapParams.get("TokenColors").split(",");
        this.tokenKeywords = keywords;
        this.tokenImage = image;
        if (mapParams.containsKey("TokenOwner")) {
            this.tokenOwner = mapParams.get("TokenOwner");
        } else {
            this.tokenOwner = "You";
        }
    }

    /**
     * <p>
     * getAbility.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getAbility() {
        class AbilityToken extends AbilityActivated {
            public AbilityToken(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityToken(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 8460074843405764620L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryToken.this.tokenCanPlayAI(getActivatingPlayer(), this);
            }

            @Override
            public void resolve() {
                AbilityFactoryToken.this.doResolve(this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryToken.this.doStackDescription(this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryToken.this.tokenDoTriggerAI(getActivatingPlayer(), this, mandatory);
            }
        }
        final SpellAbility abToken = new AbilityToken(this.abilityFactory.getHostCard(),
                this.abilityFactory.getAbCost(), this.abilityFactory.getAbTgt());

        return abToken;
    }

    /**
     * <p>
     * getSpell.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getSpell() {
        final SpellAbility spToken = new Spell(this.abilityFactory.getHostCard(), this.abilityFactory.getAbCost(),
                this.abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = -8041427947613029670L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryToken.this.tokenCanPlayAI(getActivatingPlayer(), this);
            }

            @Override
            public void resolve() {
                AbilityFactoryToken.this.doResolve(this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryToken.this.doStackDescription(this);
            }

            @Override
            public boolean canPlayFromEffectAI(final boolean mandatory, final boolean withOutManaCost) {
                if (withOutManaCost) {
                    return AbilityFactoryToken.this.tokenDoTriggerAINoCost(getActivatingPlayer(), this, mandatory);
                }
                return AbilityFactoryToken.this.tokenDoTriggerAI(getActivatingPlayer(), this, mandatory);
            }
        };

        return spToken;
    }

    /**
     * <p>
     * getDrawback.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getDrawback() {
        class DrawbackToken extends AbilitySub {
            public DrawbackToken(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackToken(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 7239608350643325111L;

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryToken.this.doStackDescription(this);
            }

            @Override
            public void resolve() {
                AbilityFactoryToken.this.doResolve(this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryToken.this.tokenDoTriggerAI(getActivatingPlayer(), this, mandatory);
            }
        }
        final SpellAbility dbToken = new DrawbackToken(this.abilityFactory.getHostCard(),
                this.abilityFactory.getAbTgt()); // Spell

        return dbToken;
    }

    /**
     * <p>
     * tokenCanPlayAI.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean tokenCanPlayAI(final Player ai, final SpellAbility sa) {
        final Cost cost = sa.getPayCosts();
        final AbilityFactory af = sa.getAbilityFactory();
        final HashMap<String, String> mapParams = af.getMapParams();

        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        Player opp = ai.getOpponent();
        for (final String type : this.tokenTypes) {
            if (type.equals("Legendary")) {
                // Don't kill AIs Legendary tokens
                if (ai.getCardsIn(ZoneType.Battlefield, this.tokenName).size() > 0) {
                    return false;
                }
            }
        }

        boolean haste = false;
        boolean oneShot = false;
        for (final String kw : this.tokenKeywords) {
            if (kw.equals("Haste")) {
                haste = true;
            }
            if (kw.equals("At the beginning of the end step, exile CARDNAME.")
                    || kw.equals("At the beginning of the end step, sacrifice CARDNAME.")) {
                oneShot = true;
            }
        }

        PhaseHandler ph = Singletons.getModel().getGame().getPhaseHandler(); 
        // Don't generate tokens without haste before main 2 if possible
        if (ph.getPhase().isBefore(PhaseType.MAIN2)
                && ph.isPlayerTurn(ai) && !haste
                && !mapParams.containsKey("ActivationPhases")) {
            return false;
        }
        if ((ph.isPlayerTurn(ai)
                || ph.getPhase().isBefore(
                        PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY))
                && !mapParams.containsKey("ActivationPhases") && !mapParams.containsKey("PlayerTurn")
                && !AbilityFactory.isSorcerySpeed(sa) && !haste) {
            return false;
        }
        if ((ph.getPhase().isAfter(PhaseType.COMBAT_BEGIN) || Singletons.getModel().getGame().getPhaseHandler().isPlayerTurn(
                opp))
                && oneShot) {
            return false;
        }

        // prevent run-away activations - first time will always return true
        final Random r = MyRandom.getRandom();
        final Card source = sa.getSourceCard();

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            if (tgt.canOnlyTgtOpponent()) {
                tgt.addTarget(opp);
            } else {
                tgt.addTarget(ai);
            }
        }

        if (cost != null) {
            if (!CostUtil.checkLifeCost(ai, cost, source, 4, null)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(ai, cost, source)) {
                return false;
            }

            if (!CostUtil.checkSacrificeCost(ai, cost, source)) {
                return false;
            }

            if (!CostUtil.checkRemoveCounterCost(cost, source)) {
                return false;
            }
        }

        if (this.tokenAmount.equals("X") || this.tokenPower.equals("X") || this.tokenToughness.equals("X")) {
            int x = AbilityFactory.calculateAmount(this.abilityFactory.getHostCard(), this.tokenAmount, sa);
            if (source.getSVar("X").equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                x = ComputerUtil.determineLeftoverMana(sa, ai);
                source.setSVar("PayX", Integer.toString(x));
            }
            if (x <= 0) {
                return false;
            }
        }

        if (AbilityFactory.playReusable(sa)) {
            return true;
        }

        if (Singletons.getModel().getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)) {
            return true;
        }
        if (sa.isAbility()) {
            return (r.nextFloat() < .9);
        }

        return (r.nextFloat() < .8);
    }

    /**
     * <p>
     * tokenDoTriggerAI.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean tokenDoTriggerAI(final Player ai, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa, ai)) {
            return false;
        }

        return tokenDoTriggerAINoCost(ai, sa, mandatory);
    }

    /**
     * <p>
     * tokenDoTriggerAINoCost.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean tokenDoTriggerAINoCost(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final Card source = sa.getSourceCard();
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            if (tgt.canOnlyTgtOpponent()) {
                tgt.addTarget(ai.getOpponent());
            } else {
                tgt.addTarget(ai);
            }
        }
        if (this.tokenAmount.equals("X") || this.tokenPower.equals("X") || this.tokenToughness.equals("X")) {
            int x = AbilityFactory.calculateAmount(this.abilityFactory.getHostCard(), this.tokenAmount, sa);
            if (source.getSVar("X").equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                x = ComputerUtil.determineLeftoverMana(sa, ai);
                source.setSVar("PayX", Integer.toString(x));
            }
            if (x <= 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * doStackDescription.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private String doStackDescription(final SpellAbility sa) {

        final HashMap<String, String> params = this.abilityFactory.getMapParams();
        final StringBuilder sb = new StringBuilder();
        final Card host = this.abilityFactory.getHostCard();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(host.getName()).append(" - ");
        }

        if (params.containsKey("StackDescription")) {
            sb.append(params.get("StackDescription"));
        }
        else {

            final int finalPower = AbilityFactory.calculateAmount(this.abilityFactory.getHostCard(), this.tokenPower, sa);
            final int finalToughness = AbilityFactory.calculateAmount(this.abilityFactory.getHostCard(),
                    this.tokenToughness, sa);
            final int finalAmount = AbilityFactory.calculateAmount(this.abilityFactory.getHostCard(), this.tokenAmount, sa);

            final String substitutedName = this.tokenName.equals("ChosenType") ? host.getChosenType() : this.tokenName;

            sb.append("Put (").append(finalAmount).append(") ").append(finalPower).append("/").append(finalToughness);
            sb.append(" ").append(substitutedName).append(" token");
            if (finalAmount != 1) {
                sb.append("s");
            }
            sb.append(" onto the battlefield");

            if (this.tokenOwner.equals("Opponent")) {
                sb.append(" under your opponent's control.");
            } else {
                sb.append(".");
            }
        }

        if (sa.getSubAbility() != null) {
            sb.append(sa.getSubAbility().getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * doResolve.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private void doResolve(final SpellAbility sa) {
        final Card host = this.abilityFactory.getHostCard();
        String imageName = "";
        Player controller;
        String cost = "";
        // Construct colors
        final String[] substitutedColors = Arrays.copyOf(this.tokenColors, this.tokenColors.length);
        for (int i = 0; i < substitutedColors.length; i++) {
            if (substitutedColors[i].equals("ChosenColor")) {
                // this currently only supports 1 chosen color
                substitutedColors[i] = host.getChosenColor().get(0);
            }
        }
        String colorDesc = "";
        for (final String col : substitutedColors) {
            if (col.equalsIgnoreCase("White")) {
                colorDesc += "W ";
            } else if (col.equalsIgnoreCase("Blue")) {
                colorDesc += "U ";
            } else if (col.equalsIgnoreCase("Black")) {
                colorDesc += "B ";
            } else if (col.equalsIgnoreCase("Red")) {
                colorDesc += "R ";
            } else if (col.equalsIgnoreCase("Green")) {
                colorDesc += "G ";
            } else if (col.equalsIgnoreCase("Colorless")) {
                colorDesc = "C";
            }
        }
        if (this.tokenImage.equals("")) {
            imageName += colorDesc.replace(" ", "") + " " + this.tokenPower + " " + this.tokenToughness + " " + this.tokenName;
        } else {
            imageName = this.tokenImage;
        }
        // System.out.println("AF_Token imageName = " + imageName);

        for (final char c : colorDesc.toCharArray()) {
            cost += c + ' ';
        }

        cost = colorDesc.replace('C', '1').trim();

        controller = AbilityFactory.getDefinedPlayers(this.abilityFactory.getHostCard(), this.tokenOwner, sa).get(0);

        final int finalPower = AbilityFactory.calculateAmount(this.abilityFactory.getHostCard(), this.tokenPower, sa);
        final int finalToughness = AbilityFactory.calculateAmount(this.abilityFactory.getHostCard(),
                this.tokenToughness, sa);
        final int finalAmount = AbilityFactory.calculateAmount(this.abilityFactory.getHostCard(), this.tokenAmount, sa);

        final String[] substitutedTypes = Arrays.copyOf(this.tokenTypes, this.tokenTypes.length);
        for (int i = 0; i < substitutedTypes.length; i++) {
            if (substitutedTypes[i].equals("ChosenType")) {
                substitutedTypes[i] = host.getChosenType();
            }
        }
        final String substitutedName = this.tokenName.equals("ChosenType") ? host.getChosenType() : this.tokenName;

        final String remember = this.abilityFactory.getMapParams().get("RememberTokens");
        for (int i = 0; i < finalAmount; i++) {
            final List<Card> tokens = CardFactoryUtil.makeToken(substitutedName, imageName, controller, cost,
                    substitutedTypes, finalPower, finalToughness, this.tokenKeywords);

            // Grant abilities
            if (this.tokenAbilities != null) {
                final AbilityFactory af = new AbilityFactory();
                for (final String s : this.tokenAbilities) {
                    final String actualAbility = this.abilityFactory.getHostCard().getSVar(s);
                    for (final Card c : tokens) {
                        final SpellAbility grantedAbility = af.getAbility(actualAbility, c);
                        c.addSpellAbility(grantedAbility);
                    }
                }
            }

            // Grant triggers
            if (this.tokenTriggers != null) {

                for (final String s : this.tokenTriggers) {
                    final String actualTrigger = this.abilityFactory.getHostCard().getSVar(s);

                    for (final Card c : tokens) {

                        final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, c, true);
                        final String ability = this.abilityFactory.getHostCard().getSVar(
                                parsedTrigger.getMapParams().get("Execute"));
                        parsedTrigger.setOverridingAbility(new AbilityFactory().getAbility(ability, c));
                        c.addTrigger(parsedTrigger);
                    }
                }
            }

            // Grant SVars
            if (this.tokenSVars != null) {
                for (final String s : this.tokenSVars) {
                    String actualSVar = this.abilityFactory.getHostCard().getSVar(s);
                    String name = s;
                    if (actualSVar.startsWith("SVar")) {
                        actualSVar = actualSVar.split("SVar:")[1];
                        name = actualSVar.split(":")[0];
                        actualSVar = actualSVar.split(":")[1];
                    }
                    for (final Card c : tokens) {
                        c.setSVar(name, actualSVar);
                    }
                }
            }

            // Grant static abilities
            if (this.tokenStaticAbilities != null) {
                for (final String s : this.tokenStaticAbilities) {
                    final String actualAbility = this.abilityFactory.getHostCard().getSVar(s);
                    for (final Card c : tokens) {
                        c.addStaticAbility(actualAbility);
                    }
                }
            }

            for (final Card c : tokens) {
                if (this.tokenTapped) {
                    c.setTapped(true);
                }
                if (this.tokenAttacking) {
                    Singletons.getModel().getGame().getCombat().addAttacker(c);
                }
                if (remember != null) {
                    Singletons.getModel().getGame().getCardState(sa.getSourceCard()).addRemembered(c);
                }
                if (this.abilityFactory.getMapParams().get("RememberSource") != null) {
                    Singletons.getModel().getGame().getCardState(c).addRemembered(host);
                }
            }
        }
    }
}
