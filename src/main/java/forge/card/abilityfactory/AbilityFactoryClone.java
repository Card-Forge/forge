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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardCharactersticName;
import forge.CardColor;
import forge.CardList;
import forge.CardUtil;
import forge.GameActionUtil;
import forge.Singletons;
import forge.card.CardCharacteristics;
import forge.card.cardfactory.AbstractCardFactory;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;
import forge.card.trigger.TriggerType;
import forge.game.phase.PhaseType;
import forge.game.player.ComputerUtil;
import forge.game.zone.ZoneType;

/**
 * <p>
 * AbilityFactoryClone class.
 * </p>
 * 
 * @author Forge
 * @version $Id: AbilityFactoryClone.java 15541 2012-05-14 11:47:16Z Sloth $
 */
public final class AbilityFactoryClone {

    private AbilityFactoryClone() {
        throw new AssertionError();
    }

    // **************************************************************
    // *************************** Clone ****************************
    // **************************************************************

    /**
     * <p>
     * createAbilityClone.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityClone(final AbilityFactory af) {
        class AbilityClone extends AbilityActivated {
            public AbilityClone(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityClone(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 1938171749867734123L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryClone.cloneCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryClone.cloneResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryClone.cloneStackDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryClone.cloneTriggerAI(af, this, mandatory);
            }
        }

        final SpellAbility abClone = new AbilityClone(af.getHostCard(), af.getAbCost(), af.getAbTgt());
        return abClone;
    }

    /**
     * <p>
     * createSpellClone.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellClone(final AbilityFactory af) {
        final SpellAbility spClone = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -4047747186919390520L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryClone.cloneCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryClone.cloneResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryClone.cloneStackDescription(af, this);
            }
        };
        return spClone;
    }

    /**
     * <p>
     * createDrawbackClone.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackClone(final AbilityFactory af) {
        class DrawbackClone extends AbilitySub {
            public DrawbackClone(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackClone(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -8659938411435528741L;

            @Override
            public void resolve() {
                AbilityFactoryClone.cloneResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryClone.clonePlayDrawbackAI(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryClone.cloneStackDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryClone.cloneTriggerAI(af, this, mandatory);
            }
        }

        final SpellAbility dbClone = new DrawbackClone(af.getHostCard(), af.getAbTgt());
        return dbClone;
    }

    /**
     * <p>
     * cloneStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    // TODO update this method
    private static String cloneStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = sa.getSourceCard();
        final Map<String, String> svars = host.getSVars();

        int power = -1;
        if (params.containsKey("Power")) {
            power = AbilityFactory.calculateAmount(host, params.get("Power"), sa);
        }
        int toughness = -1;
        if (params.containsKey("Toughness")) {
            toughness = AbilityFactory.calculateAmount(host, params.get("Toughness"), sa);
        }

        final boolean permanent = params.containsKey("Permanent");
        final ArrayList<String> types = new ArrayList<String>();
        if (params.containsKey("Types")) {
            types.addAll(Arrays.asList(params.get("Types").split(",")));
        }
        final ArrayList<String> keywords = new ArrayList<String>();
        if (params.containsKey("Keywords")) {
            keywords.addAll(Arrays.asList(params.get("Keywords").split(" & ")));
        }
        // allow SVar substitution for keywords
        for (int i = 0; i < keywords.size(); i++) {
            final String k = keywords.get(i);
            if (svars.containsKey(k)) {
                keywords.add("\"" + k + "\"");
                keywords.remove(k);
            }
        }
        final ArrayList<String> colors = new ArrayList<String>();
        if (params.containsKey("Colors")) {
            colors.addAll(Arrays.asList(params.get("Colors").split(",")));
        }

        final StringBuilder sb = new StringBuilder();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard().getName()).append(" - ");
        }

        final Target tgt = sa.getTarget();
        ArrayList<Card> tgts;
        if (tgt != null) {
            tgts = tgt.getTargetCards();
        } else {
            tgts = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Card c : tgts) {
            sb.append(c).append(" ");
        }
        sb.append("become");
        if (tgts.size() == 1) {
            sb.append("s a");
        }
        // if power is -1, we'll assume it's not just setting toughness
        if (power != -1) {
            sb.append(" ").append(power).append("/").append(toughness);
        }

        if (colors.size() > 0) {
            sb.append(" ");
        }
        if (colors.contains("ChosenColor")) {
            sb.append("color of that player's choice");
        } else {
            for (int i = 0; i < colors.size(); i++) {
                sb.append(colors.get(i));
                if (i < (colors.size() - 1)) {
                    sb.append(" and ");
                }
            }
        }
        sb.append(" ");
        if (types.contains("ChosenType")) {
            sb.append("type of player's choice ");
        } else {
            for (int i = types.size() - 1; i >= 0; i--) {
                sb.append(types.get(i));
                sb.append(" ");
            }
        }
        if (keywords.size() > 0) {
            sb.append("with ");
        }
        for (int i = 0; i < keywords.size(); i++) {
            sb.append(keywords.get(i));
            if (i < (keywords.size() - 1)) {
                sb.append(" and ");
            }
        }
        // sb.append(abilities)
        // sb.append(triggers)
        if (!permanent) {
            if (params.containsKey("UntilEndOfCombat")) {
                sb.append(" until end of combat.");
            } else if (params.containsKey("UntilHostLeavesPlay")) {
                sb.append(" until ").append(host).append(" leaves the battlefield.");
            } else if (params.containsKey("UntilYourNextUpkeep")) {
                sb.append(" until your next upkeep.");
            } else if (params.containsKey("UntilControllerNextUntap")) {
                sb.append(" until its controller's next untap step.");
            } else {
                sb.append(" until end of turn.");
            }
        } else {
            sb.append(".");
        }

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    } // end cloneStackDescription()

    /**
     * <p>
     * cloneCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean cloneCanPlayAI(final AbilityFactory af, final SpellAbility sa) {

        final HashMap<String, String> params = af.getMapParams();
        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();

        boolean useAbility = true;

//        if (card.getController().isComputer()) {
//            final CardList creatures = AllZoneUtil.getCreaturesInPlay();
//            if (!creatures.isEmpty()) {
//                cardToCopy = CardFactoryUtil.getBestCreatureAI(creatures);
//            }
//        }

        // TODO - add some kind of check to answer
        // "Am I going to attack with this?"
        // TODO - add some kind of check for during human turn to answer
        // "Can I use this to block something?"

        // don't use instant speed clone abilities outside computers
        // Combat_Begin step
        if (!Singletons.getModel().getGameState().getPhaseHandler().is(PhaseType.COMBAT_BEGIN)
                && Singletons.getModel().getGameState().getPhaseHandler().isPlayerTurn(AllZone.getComputerPlayer()) && !AbilityFactory.isSorcerySpeed(sa)
                && !params.containsKey("ActivationPhases") && !params.containsKey("Permanent")) {
            return false;
        }

        // don't use instant speed clone abilities outside humans
        // Combat_Declare_Attackers_InstantAbility step
        if ((!Singletons.getModel().getGameState().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY) || (AllZone.getCombat()
                .getAttackers().isEmpty())) && Singletons.getModel().getGameState().getPhaseHandler().isPlayerTurn(AllZone.getHumanPlayer())) {
            return false;
        }

        // don't activate during main2 unless this effect is permanent
        if (Singletons.getModel().getGameState().getPhaseHandler().is(PhaseType.MAIN2) && !params.containsKey("Permanent")) {
            return false;
        }

        if (null == tgt) {
            final ArrayList<Card> defined = AbilityFactory.getDefinedCards(source, params.get("Defined"), sa);

            boolean bFlag = false;
            for (final Card c : defined) {
                bFlag |= (!c.isCreature() && !c.isTapped() && !(c.getTurnInZone() == Singletons.getModel().getGameState().getPhaseHandler().getTurn()));

                // for creatures that could be improved (like Figure of Destiny)
                if (c.isCreature() && (params.containsKey("Permanent") || (!c.isTapped() && !c.isSick()))) {
                    int power = -5;
                    if (params.containsKey("Power")) {
                        power = AbilityFactory.calculateAmount(source, params.get("Power"), sa);
                    }
                    int toughness = -5;
                    if (params.containsKey("Toughness")) {
                        toughness = AbilityFactory.calculateAmount(source, params.get("Toughness"), sa);
                    }
                    if ((power + toughness) > (c.getCurrentPower() + c.getCurrentToughness())) {
                        bFlag = true;
                    }
                }

            }

            if (!bFlag) { // All of the defined stuff is cloned, not very
                          // useful
                return false;
            }
        } else {
            tgt.resetTargets();
            useAbility &= AbilityFactoryClone.cloneTgtAI(af, sa);
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            useAbility &= subAb.chkAIDrawback();
        }

        return useAbility;
    } // end cloneCanPlayAI()

    /**
     * <p>
     * clonePlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean clonePlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        // AI should only activate this during Human's turn
        boolean chance = true;

        if (sa.getTarget() != null) {
            chance = AbilityFactoryClone.cloneTgtAI(af, sa);
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    }

    /**
     * <p>
     * cloneTriggerAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean cloneTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) { // If there is a cost payment
            return false;
        }

        boolean chance = true;

        if (sa.getTarget() != null) {
            chance = AbilityFactoryClone.cloneTgtAI(af, sa);
        }

        // Improve AI for triggers. If source is a creature with:
        // When ETB, sacrifice a creature. Check to see if the AI has something
        // to sacrifice

        // Eventually, we can call the trigger of ETB abilities with
        // not mandatory as part of the checks to cast something

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance || mandatory;
    }

    /**
     * <p>
     * cloneTgtAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean cloneTgtAI(final AbilityFactory af, final SpellAbility sa) {
        // This is reasonable for now. Kamahl, Fist of Krosa and a sorcery or
        // two are the only things
        // that clone a target. Those can just use SVar:RemAIDeck:True until
        // this can do a reasonably
        // good job of picking a good target
        return false;
    }

    /**
     * <p>
     * cloneResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void cloneResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        Card tgtCard;
        final Card host = sa.getSourceCard();
        Map<String, String> origSVars = host.getSVars();


        // find cloning source i.e. thing to be copied
        Card cardToCopy = null;
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            cardToCopy = tgt.getTargetCards().get(0);
        }
        else if (params.containsKey("Defined")) {
            ArrayList<Card> cloneSources = AbilityFactory.getDefinedCards(host, params.get("Defined"), sa);
            if (!cloneSources.isEmpty()) {
                cardToCopy = cloneSources.get(0);
            }
        }
        if (cardToCopy == null) {
            return;
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("Do you want to copy " + cardToCopy + "?");
        boolean optional = params.containsKey("Optional");
        if (host.getController().isHuman() && optional
                && !GameActionUtil.showYesNoDialog(host, sb.toString())) {
            return;
        }

        // find target of cloning i.e. card becoming a clone
        ArrayList<Card> cloneTargets = AbilityFactory.getDefinedCards(host, params.get("CloneTarget"), sa);
        if (!cloneTargets.isEmpty()) {
            tgtCard = cloneTargets.get(0);
        }
        else {
            tgtCard = host;
        }

        String imageFileName = host.getImageFilename();

        boolean keepName = params.containsKey("KeepName");
        String originalName = tgtCard.getName();
        boolean copyingSelf = (tgtCard == cardToCopy);

        AllZone.getTriggerHandler().suppressMode(TriggerType.Transformed);

        if (!copyingSelf) {
            if (tgtCard.isCloned()) { // cloning again
                tgtCard.switchStates(CardCharactersticName.Cloner, CardCharactersticName.Original);
                tgtCard.setState(CardCharactersticName.Original);
                tgtCard.clearStates(CardCharactersticName.Cloner);
            }
            // add "Cloner" state to clone
            tgtCard.addAlternateState(CardCharactersticName.Cloner);
            tgtCard.switchStates(CardCharactersticName.Original, CardCharactersticName.Cloner);
            tgtCard.setState(CardCharactersticName.Original);
        }
        else {
            //copy Original state to Cloned
            tgtCard.addAlternateState(CardCharactersticName.Cloned);
            tgtCard.switchStates(CardCharactersticName.Original, CardCharactersticName.Cloned);
            if (tgtCard.isFlip()) {
                tgtCard.setState(CardCharactersticName.Original);
            }
        }

        CardCharactersticName stateToCopy = null;
        if (copyingSelf) {
            stateToCopy = CardCharactersticName.Cloned;
        }
        else if (cardToCopy.isFlip()) {
            stateToCopy = CardCharactersticName.Original;
        }
        else {
            stateToCopy = cardToCopy.getCurState();
        }

        CardFactoryUtil.copyState(cardToCopy, stateToCopy, tgtCard);
        // must call this before addAbilityFactoryAbilities so cloned added abilities are handled correctly
        addExtraCharacteristics(tgtCard, params, origSVars);
        CardFactoryUtil.addAbilityFactoryAbilities(tgtCard);
        for (int i = 0; i < tgtCard.getStaticAbilityStrings().size(); i++) {
            tgtCard.addStaticAbility(tgtCard.getStaticAbilityStrings().get(i));
        }
        if (keepName) {
            tgtCard.setName(originalName);
        }

        // If target is a flipped card, also copy the flipped
        // state.
        if (cardToCopy.isFlip()) {
            tgtCard.addAlternateState(CardCharactersticName.Flipped);
            tgtCard.setState(CardCharactersticName.Flipped);
            CardFactoryUtil.copyState(cardToCopy, CardCharactersticName.Flipped, tgtCard);
            addExtraCharacteristics(tgtCard, params, origSVars);
            CardFactoryUtil.addAbilityFactoryAbilities(tgtCard);
            for (int i = 0; i < tgtCard.getStaticAbilityStrings().size(); i++) {
                tgtCard.addStaticAbility(tgtCard.getStaticAbilityStrings().get(i));
            }
            if (keepName) {
                tgtCard.setName(originalName);
            }
            tgtCard.setFlip(true);

            tgtCard.setState(CardCharactersticName.Original);
        } else {
            tgtCard.setFlip(false);
        }

        //Clean up copy of cloned state
        if (copyingSelf) {
            tgtCard.clearStates(CardCharactersticName.Cloned);
        }

        AllZone.getTriggerHandler().clearSuppression(TriggerType.Transformed);

        //Clear Remembered and Imprint lists
        tgtCard.clearRemembered();
        tgtCard.clearImprinted();

        //keep the Clone card image for the cloned card
        tgtCard.setImageFilename(imageFileName);
    } // cloneResolve

    private static void addExtraCharacteristics(final Card tgtCard, final HashMap<String, String> params, final Map<String, String> origSVars) {
        // additional types to clone
        if (params.containsKey("AddTypes")) {
           for (final String type : Arrays.asList(params.get("AddTypes").split(","))) {
               tgtCard.addType(type);
           }
        }

        // triggers to add to clone
        final ArrayList<String> triggers = new ArrayList<String>();
        if (params.containsKey("AddTriggers")) {
            triggers.addAll(Arrays.asList(params.get("AddTriggers").split(",")));
            for (final String s : triggers) {
                if (origSVars.containsKey(s)) {
                    final String actualTrigger = origSVars.get(s);
                    final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, tgtCard, true);
                    tgtCard.addTrigger(parsedTrigger);
                }
            }
        }

        // SVars to add to clone
        if (params.containsKey("AddSVars")) {
            for (final String s : Arrays.asList(params.get("AddSVars").split(","))) {
                if (origSVars.containsKey(s)) {
                    final String actualsVar = origSVars.get(s);
                    tgtCard.setSVar(s, actualsVar);
                }
            }
        }

        // abilities to add to clone
        if (params.containsKey("AddAbilities")) {
            for (final String s : Arrays.asList(params.get("AddAbilities").split(","))) {
                if (origSVars.containsKey(s)) {
                    //final AbilityFactory newAF = new AbilityFactory();
                    final String actualAbility = origSVars.get(s);
                    // final SpellAbility grantedAbility = newAF.getAbility(actualAbility, tgtCard);
                    // tgtCard.addSpellAbility(grantedAbility);
                    tgtCard.getIntrinsicAbilities().add(actualAbility);
                }
            }
        }

        // keywords to add to clone
        final ArrayList<String> keywords = new ArrayList<String>();
        if (params.containsKey("AddKeywords")) {
            keywords.addAll(Arrays.asList(params.get("AddKeywords").split(" & ")));
            // allow SVar substitution for keywords
            for (int i = 0; i < keywords.size(); i++) {
                final String k = keywords.get(i);
                if (origSVars.containsKey(k)) {
                    keywords.add("\"" + k + "\"");
                    keywords.remove(k);
                }
                if (keywords.get(i).startsWith("HIDDEN")) {
                    tgtCard.addExtrinsicKeyword(keywords.get(i));
                }
                else {
                    tgtCard.addIntrinsicKeyword(keywords.get(i));
                }
            }
        }

        // set power of clone
        if (params.containsKey("IntoPlayTapped")) {
            tgtCard.setTapped(true);
        }

        // set power of clone
        if (params.containsKey("SetPower")) {
            String rhs = params.get("SetPower");
            int power = -1;
            try {
                power = Integer.parseInt(rhs);
            } catch (final NumberFormatException e) {
                power = CardFactoryUtil.xCount(tgtCard, tgtCard.getSVar(rhs));
            }
            tgtCard.setBaseAttack(power);
        }

        // set toughness of clone
        if (params.containsKey("SetToughness")) {
            String rhs = params.get("SetToughness");
            int toughness = -1;
            try {
                toughness = Integer.parseInt(rhs);
            } catch (final NumberFormatException e) {
                toughness = CardFactoryUtil.xCount(tgtCard, tgtCard.getSVar(rhs));
            }
            tgtCard.setBaseDefense(toughness);
        }

        // colors to be added or changed to
        String shortColors = "";
        if (params.containsKey("Colors")) {
            final String colors = params.get("Colors");
            if (colors.equals("ChosenColor")) {
                shortColors = CardUtil.getShortColorsString(tgtCard.getChosenColor());
            } else {
                shortColors = CardUtil.getShortColorsString(new ArrayList<String>(Arrays.asList(colors.split(","))));
            }
        }
        tgtCard.addColor(shortColors, tgtCard, !params.containsKey("OverwriteColors"), true);

    }

 } // end class AbilityFactoryClone
