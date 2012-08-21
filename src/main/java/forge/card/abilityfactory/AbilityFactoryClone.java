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
import forge.Card;
import forge.CardCharacteristicName;
import forge.CardUtil;
import forge.GameActionUtil;
import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;
import forge.game.phase.PhaseType;
import forge.game.player.ComputerUtil;

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

        sb.append(sa.getSourceCard());
        sb.append(" becomes a copy of ");
        if (!tgts.isEmpty()) {
          sb.append(tgts.get(0)).append(".");
        }
        else {
          sb.append("target creature.");
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

        if (!copyingSelf) {
            if (tgtCard.isCloned()) { // cloning again
                tgtCard.switchStates(CardCharacteristicName.Cloner, CardCharacteristicName.Original);
                tgtCard.setState(CardCharacteristicName.Original);
                tgtCard.clearStates(CardCharacteristicName.Cloner);
            }
            // add "Cloner" state to clone
            tgtCard.addAlternateState(CardCharacteristicName.Cloner);
            tgtCard.switchStates(CardCharacteristicName.Original, CardCharacteristicName.Cloner);
            tgtCard.setState(CardCharacteristicName.Original);
        }
        else {
            //copy Original state to Cloned
            tgtCard.addAlternateState(CardCharacteristicName.Cloned);
            tgtCard.switchStates(CardCharacteristicName.Original, CardCharacteristicName.Cloned);
            if (tgtCard.isFlipCard()) {
                tgtCard.setState(CardCharacteristicName.Original);
            }
        }

        CardCharacteristicName stateToCopy = null;
        if (copyingSelf) {
            stateToCopy = CardCharacteristicName.Cloned;
        }
        else if (cardToCopy.isFlipCard()) {
            stateToCopy = CardCharacteristicName.Original;
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
        if (cardToCopy.isFlipCard()) {
            if (!copyingSelf) {
                tgtCard.addAlternateState(CardCharacteristicName.Flipped);
                tgtCard.setState(CardCharacteristicName.Flipped);
            }
            CardFactoryUtil.copyState(cardToCopy, CardCharacteristicName.Flipped, tgtCard);
            addExtraCharacteristics(tgtCard, params, origSVars);
            CardFactoryUtil.addAbilityFactoryAbilities(tgtCard);
            for (int i = 0; i < tgtCard.getStaticAbilityStrings().size(); i++) {
                tgtCard.addStaticAbility(tgtCard.getStaticAbilityStrings().get(i));
            }
            if (keepName) {
                tgtCard.setName(originalName);
            }
            tgtCard.setFlipCard(true);
            //keep the Clone card image for the cloned card
            tgtCard.setImageFilename(imageFileName);

            if (!tgtCard.isFlipped()) {
              tgtCard.setState(CardCharacteristicName.Original);
            }
        } else {
            tgtCard.setFlipCard(false);
        }

        //Clean up copy of cloned state
        if (copyingSelf) {
            tgtCard.clearStates(CardCharacteristicName.Cloned);
        }

        //Clear Remembered and Imprint lists
        tgtCard.clearRemembered();
        tgtCard.clearImprinted();

        //keep the Clone card image for the cloned card
        tgtCard.setImageFilename(imageFileName);

        // check if clone is now an Aura that needs to be attached
        if (tgtCard.isAura()) {
            AbilityFactoryAttach.attachAuraOnIndirectEnterBattlefield(tgtCard);
        }

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
