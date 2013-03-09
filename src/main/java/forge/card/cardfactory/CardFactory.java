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
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import forge.Card;
import forge.CardCharacteristicName;
import forge.CardColor;
import forge.CardUtil;
import forge.Color;
import forge.card.CardCharacteristics;
import forge.card.CardRules;
import forge.card.CardSplitType;
import forge.card.ICardFace;
import forge.card.cost.Cost;
import forge.card.mana.ManaCost;
import forge.card.replacement.ReplacementHandler;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellPermanent;
import forge.card.spellability.Target;
import forge.card.trigger.TriggerHandler;
import forge.game.player.Player;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.IPaperCard;

/**
 * <p>
 * AbstractCardFactory class.
 * </p>
 * 
 * TODO The map field contains Card instances that have not gone through
 * getCard2, and thus lack abilities. However, when a new Card is requested via
 * getCard, it is this map's values that serve as the templates for the values
 * it returns. This class has another field, allCards, which is another copy of
 * the card database. These cards have abilities attached to them, and are owned
 * by the human player by default. <b>It would be better memory-wise if we had
 * only one or the other.</b> We may experiment in the future with using
 * allCard-type values for the map instead of the less complete ones that exist
 * there today.
 * 
 * @author Forge
 * @version $Id$
 */
public class CardFactory {
    /**
     * <p>
     * copyCard.
     * </p>
     * 
     * @param in
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public final static Card copyCard(final Card in) {
        final CardCharacteristicName curState = in.getCurState();
        if (in.isInAlternateState()) {
            in.setState(CardCharacteristicName.Original);
        }
        final Card out = getCard(CardDb.getCard(in), in.getOwner());
        out.setUniqueNumber(in.getUniqueNumber());

        CardFactory.copyCharacteristics(in, out);
        if (in.hasAlternateState()) {
            for (final CardCharacteristicName state : in.getStates()) {
                in.setState(state);
                if (state == CardCharacteristicName.Cloner) {
                    out.addAlternateState(state);
                }
                out.setState(state);
                CardFactory.copyCharacteristics(in, out);
            }
            in.setState(curState);
            out.setState(curState);
        }

        // I'm not sure if we really should be copying enchant/equip stuff over.
        out.setEquipping(in.getEquipping());
        out.setEquippedBy(in.getEquippedBy());
        out.setEnchantedBy(in.getEnchantedBy());
        out.setEnchanting(in.getEnchanting());
        out.setClones(in.getClones());
        for (final Object o : in.getRemembered()) {
            out.addRemembered(o);
        }
        for (final Card o : in.getImprinted()) {
            out.addImprinted(o);
        }

        return out;

    }

    /**
     * <p>
     * copySpellontoStack.
     * </p>
     * 
     * @param source
     *            a {@link forge.Card} object.
     * @param original
     *            a {@link forge.Card} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param bCopyDetails
     *            a boolean.
     */
    public final static void copySpellontoStack(final Card source, final Card original, final SpellAbility sa,
            final boolean bCopyDetails) {
        //Player originalController = original.getController();
        Player controller = sa.getActivatingPlayer();
        final Card c = copyCard(original);

        // change the color of the copy (eg: Fork)
        final SpellAbility sourceSA = source.getFirstSpellAbility();
        if (null != sourceSA && sourceSA.hasParam("CopyIsColor")) {
            String tmp = "";
            final String newColor = sourceSA.getParam("CopyIsColor");
            if (newColor.equals("ChosenColor")) {
                tmp = CardUtil.getShortColorsString(source.getChosenColor());
            } else {
                tmp = CardUtil.getShortColorsString(new ArrayList<String>(Arrays.asList(newColor.split(","))));
            }
            final String finalColors = tmp;

            c.addColor(finalColors, c, !sourceSA.hasParam("OverwriteColors"), true);
        }
        
        c.clearControllers();
        c.setOwner(controller);
        c.setCopiedSpell(true);
        c.refreshUniqueNumber();

        final SpellAbility copySA;
        if(sa instanceof AbilityActivated)
        {
            copySA = ((AbilityActivated)sa).getCopy();
            copySA.setSourceCard(original);
        }
        else
        {
            copySA = sa.copy();
            copySA.setSourceCard(c);
            SpellAbility subSA = copySA.getSubAbility();
            if (subSA != null) {
                AbilitySub copySubSA = ((AbilitySub) subSA).getCopy();
                copySA.setSubAbility(copySubSA);
                copySubSA.setParent(copySA);
                copySubSA.setSourceCard(c);
                copySubSA.setCopied(true);
            }
        }
        copySA.setCopied(true);
        //remove all costs
        copySA.setPayCosts(new Cost(c, "", sa.isAbility()));
        if (sa.getTarget() != null) {
            Target target = new Target(sa.getTarget());
            target.setSourceCard(c);
            copySA.setTarget(target);
        }
        copySA.setActivatingPlayer(controller);

        if (bCopyDetails) {
            c.addXManaCostPaid(original.getXManaCostPaid());
            c.addMultiKickerMagnitude(original.getMultiKickerMagnitude());
            if (original.getOptionalAdditionalCostsPaid() != null) {
                for (String cost : original.getOptionalAdditionalCostsPaid()) {
                    c.addOptionalAdditionalCostsPaid(cost);
                }
            }
            c.addReplicateMagnitude(original.getReplicateMagnitude());
            if (sa.isReplicate()) {
                copySA.setIsReplicate(true);
            }
        }

        controller.getController().playSpellAbilityForFree(copySA);

        //c.addController(originalController);
    }

    /**
     * <p>
     * getCard.
     * </p>
     * 
     * @param cardName
     *            a {@link java.lang.String} object.
     * @param owner
     *            a {@link forge.game.player.Player} object.
     * @return a {@link forge.Card} instance, owned by owner; or the special
     *         blankCard
     */
    public final static Card getCard(final IPaperCard cp, final Player owner) {

        //System.out.println(cardName);
        CardRules cardRules = cp.getRules();
        final Card c = readCard(cardRules);
        c.setRules(cardRules);
        c.setOwner(owner);
        buildAbilities(c);

        c.setCurSetCode(cp.getEdition());
        c.setRarity(cp.getRarity());

        String originalPicture = cp.getImageFilename();
        //System.out.println(c.getName() + " -> " + originalPicture);
        c.setImageFilename(originalPicture);
        c.setToken(cp.isToken());

        if (c.hasAlternateState()) {
            if (c.isFlipCard()) {
                c.setState(CardCharacteristicName.Flipped);
                c.setImageFilename(originalPicture); // should assign a 180 degrees rotated picture here?
            }
            else if (c.isDoubleFaced() && cp instanceof CardPrinted) {
                c.setState(CardCharacteristicName.Transformed);
                c.setImageFilename(((CardPrinted)cp).getBackFaceImageFilename());
            }
            else if (c.getRules().getSplitType() == CardSplitType.Split) {
                c.setState(CardCharacteristicName.LeftSplit);
                c.setImageFilename(originalPicture);
                c.setCurSetCode(cp.getEdition());
                c.setRarity(cp.getRarity());
                c.setState(CardCharacteristicName.RightSplit);
                c.setImageFilename(originalPicture);
            }

            c.setCurSetCode(cp.getEdition());
            c.setRarity(cp.getRarity());
            c.setState(CardCharacteristicName.Original);
        }
        
        return c;
    }

    private static void buildAbilities(final Card card) {
        final String cardName = card.getName();

        // may have to change the spell

        // this is the "default" spell for permanents like creatures and artifacts 
        if (card.isPermanent() && !card.isAura() && !card.isLand()) {
            card.addSpellAbility(new SpellPermanent(card));
        }

        CardFactoryUtil.parseKeywords(card, cardName);

        for (final CardCharacteristicName state : card.getStates()) {
            if (card.isDoubleFaced() && state == CardCharacteristicName.FaceDown) {
                continue; // Ignore FaceDown for DFC since they have none.
            }
            card.setState(state);
            CardFactoryUtil.addAbilityFactoryAbilities(card);
            final ArrayList<String> stAbs = card.getStaticAbilityStrings();
            if (stAbs.size() > 0) {
                for (int i = 0; i < stAbs.size(); i++) {
                    card.addStaticAbility(stAbs.get(i));
                }
            }

            if ( state == CardCharacteristicName.LeftSplit || state == CardCharacteristicName.RightSplit )
            {
                card.getState(CardCharacteristicName.Original).getSpellAbility().addAll(card.getCharacteristics().getSpellAbility());
            }
        }

        card.setState(CardCharacteristicName.Original);

        // ******************************************************************
        // ************** Link to different CardFactories *******************

        if (card.isCreature()) {
            CardFactoryCreatures.buildCard(card, cardName);
        } else if (card.isPlaneswalker()) {
            CardFactoryPlaneswalkers.buildCard(card);
        } else if (card.isLand()) {
            CardFactoryLands.buildCard(card, cardName);
        } else if (card.isSorcery()) {
            CardFactorySorceries.buildCard(card, cardName);
        } else if (card.isArtifact()) {
            CardFactoryArtifacts.buildCard(card, cardName);
        }

        CardFactoryUtil.setupKeywordedAbilities(card);
    } // getCard2


    
    private static Card readCard(final CardRules rules) {

        final Card card = new Card();

        // 1. The states we may have:
        CardSplitType st = rules.getSplitType();
        switch ( st ) {
            case Split:
                card.addAlternateState(CardCharacteristicName.LeftSplit);
                card.setState(CardCharacteristicName.LeftSplit);
                break;
            
            case Transform: card.setDoubleFaced(true);break;
            case Flip: card.setFlipCard(true); break;
            case None: break;

            default: card.setTransformable(st.getChangedStateName()); break;
        } 

        readCardFace(card, rules.getMainPart());

        if ( st != CardSplitType.None) {
            card.addAlternateState(st.getChangedStateName());
            card.setState(st.getChangedStateName());
            readCardFace(card, rules.getOtherPart());
        }
        
        if (card.isInAlternateState()) {
            card.setState(CardCharacteristicName.Original);
        }

        if ( st == CardSplitType.Split ) {
            card.setName(rules.getName());

            // Combined mana cost
            ManaCost combinedManaCost = ManaCost.combine(rules.getMainPart().getManaCost(), rules.getOtherPart().getManaCost());
            card.setManaCost(combinedManaCost);

            // Combined card color
            CardColor combinedCardColor = new CardColor(card);
            combinedCardColor.addToCardColor(Color.fromColorSet(rules.getMainPart().getColor()));
            combinedCardColor.addToCardColor(Color.fromColorSet(rules.getOtherPart().getColor()));
            ArrayList<CardColor> combinedCardColorArr = new ArrayList<CardColor>();
            combinedCardColorArr.add(combinedCardColor);
            card.setColor(combinedCardColorArr);

            // Super and 'middle' types should use enums.
            List<String> coreTypes = rules.getType().getTypesBeforeDash();
            coreTypes.addAll(rules.getType().getSubTypes());
            card.setType(coreTypes);

            // Combined text based on Oracle text - might not be necessary, temporarily disabled.
            //String combinedText = String.format("%s: %s\n%s: %s", rules.getMainPart().getName(), rules.getMainPart().getOracleText(), rules.getOtherPart().getName(), rules.getOtherPart().getOracleText());
            //card.setText(combinedText);
        }

        return card;
    }

    private static void readCardFace(Card c, ICardFace face) {
        for(String a : face.getAbilities())                 c.addIntrinsicAbility(a);
        for(String k : face.getKeywords())                  c.addIntrinsicKeyword(k);
        for(String r : face.getReplacements())              c.addReplacementEffect(ReplacementHandler.parseReplacement(r, c));
        for(String s : face.getStaticAbilities())           c.addStaticAbilityString(s);
        for(String t : face.getTriggers())                  c.addTrigger(TriggerHandler.parseTrigger(t, c, true));
        for(Entry<String, String> v : face.getVariables())  c.setSVar(v.getKey(), v.getValue());

        c.setName(face.getName());
        c.setManaCost(face.getManaCost());
        c.setText(face.getNonAbilityText());
        if( face.getInitialLoyalty() > 0 ) c.setBaseLoyalty(face.getInitialLoyalty());

        // Super and 'middle' types should use enums.
        List<String> coreTypes = face.getType().getTypesBeforeDash();
        coreTypes.addAll(face.getType().getSubTypes());
        c.setType(coreTypes);

        // What a perverted color code we have!
        CardColor col1 = new CardColor(c);
        col1.addToCardColor(Color.fromColorSet(face.getColor()));
        ArrayList<CardColor> ccc = new ArrayList<CardColor>();
        ccc.add(col1);
        c.setColor(ccc);

        if ( face.getIntPower() >= 0 ) {
            c.setBaseAttack(face.getIntPower());
            c.setBaseAttackString(face.getPower());
        }
        if ( face.getIntToughness() >= 0 ) {
            c.setBaseDefense(face.getIntToughness());
            c.setBaseDefenseString(face.getToughness());
        }
    }

    /**
     * <p>
     * Copies stats like power, toughness, etc.
     * </p>
     * 
     * @param sim
     *            a {@link java.lang.Object} object.
     * @return a {@link forge.Card} object.
     */
    public static Card copyStats(final Card sim) {
        final Card c = new Card();
    
        c.setFlipCard(sim.isFlipCard());
        c.setDoubleFaced(sim.isDoubleFaced());
        c.setCurSetCode(sim.getCurSetCode());
    
        final CardCharacteristicName origState = sim.getCurState();
        for (final CardCharacteristicName state : sim.getStates()) {
            c.addAlternateState(state);
            c.setState(state);
            sim.setState(state);
            CardFactory.copyCharacteristics(sim, c);
        }
    
        sim.setState(origState);
        c.setState(origState);
        c.setRules(sim.getRules());
    
        return c;
    } // copyStats()

    /**
     * Copy characteristics.
     * 
     * @param from
     *            the from
     * @param to
     *            the to
     */
    private static void copyCharacteristics(final Card from, final Card to) {
        to.setBaseAttack(from.getBaseAttack());
        to.setBaseDefense(from.getBaseDefense());
        to.setBaseLoyalty(from.getBaseLoyalty());
        to.setBaseAttackString(from.getBaseAttackString());
        to.setBaseDefenseString(from.getBaseDefenseString());
        to.setIntrinsicKeyword(from.getIntrinsicKeyword());
        to.setName(from.getName());
        to.setType(from.getCharacteristics().getType());
        to.setText(from.getSpellText());
        to.setManaCost(from.getManaCost());
        to.setColor(from.getColor());
        to.setSVars(from.getSVars());
        to.setIntrinsicAbilities(from.getIntrinsicAbilities());
    
        to.setImageFilename(from.getImageFilename());
        to.setTriggers(from.getTriggers());
        to.setReplacementEffects(from.getReplacementEffects());
        to.setStaticAbilityStrings(from.getStaticAbilityStrings());
    
    }

    /**
     * Copy characteristics.
     * 
     * @param from
     *            the from
     * @param stateToCopy
     *            the state to copy
     * @param to
     *            the to
     */
    public static void copyState(final Card from, final CardCharacteristicName stateToCopy, final Card to) {
    
        // copy characteristics not associated with a state
        to.setBaseLoyalty(from.getBaseLoyalty());
        to.setBaseAttackString(from.getBaseAttackString());
        to.setBaseDefenseString(from.getBaseDefenseString());
        to.setText(from.getSpellText());
    
        // get CardCharacteristics for desired state
        CardCharacteristics characteristics = from.getState(stateToCopy);
        to.getCharacteristics().copy(characteristics);
        // handle triggers and replacement effect through Card class interface
        to.setTriggers(characteristics.getTriggers());
        to.setReplacementEffects(characteristics.getReplacementEffects());
    }

    public static void copySpellAbility(SpellAbility from, SpellAbility to) {
        to.setDescription(from.getDescription());
        to.setStackDescription(from.getDescription());
    
        if (from.getSubAbility() != null) {
            to.setSubAbility(from.getSubAbility().getCopy());
        }
        if (from.getRestrictions() != null) {
            to.setRestrictions(from.getRestrictions());
        }
        if (from.getConditions() != null) {
            to.setConditions(from.getConditions());
        }
    
        for (String sVar : from.getSVars()) {
            to.setSVar(sVar, from.getSVar(sVar));
        }
    }

} // end class AbstractCardFactory
