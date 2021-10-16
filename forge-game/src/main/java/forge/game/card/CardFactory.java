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
package forge.game.card;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import forge.ImageKeys;
import forge.StaticData;
import forge.card.*;
import forge.card.mana.ManaCost;
import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.*;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.WrappedAbility;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.util.CardTranslation;
import forge.util.TextUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
     *            a {@link forge.game.card.Card} object.
     * @return a {@link forge.game.card.Card} object.
     */
    public final static Card copyCard(final Card in, boolean assignNewId) {
        Card out;
        if (!(in.isToken() || in.getCopiedPermanent() != null)) {
            out = assignNewId ? getCard(in.getPaperCard(), in.getOwner(), in.getGame())
                              : getCard(in.getPaperCard(), in.getOwner(), in.getId(), in.getGame());
        } else { // token
            out = CardFactory.copyStats(in, in.getController(), assignNewId);
            out.setToken(true);

            // need to copy this values for the tokens
            out.setEmbalmed(in.isEmbalmed());
            out.setEternalized(in.isEternalized());
        }

        out.setZone(in.getZone());
        out.setState(in.getFaceupCardStateName(), true);
        out.setBackSide(in.isBackSide());

        // this's necessary for forge.game.GameAction.unattachCardLeavingBattlefield(Card)
        out.setAttachedCards(in.getAttachedCards());
        out.setEntityAttachedTo(in.getEntityAttachedTo());

        for (final Object o : in.getRemembered()) {
            out.addRemembered(o);
        }
        for (final Card o : in.getImprintedCards()) {
            out.addImprintedCard(o);
        }
        out.setCommander(in.isRealCommander());
        //out.setFaceDown(in.isFaceDown());

        return out;
    }

    /**
     * <p>
     * copySpellHost.
     * Helper function for copySpellAbilityAndPossiblyHost.
     * creates a copy of the card hosting the ability we want to copy.
     * Updates various attributes of the card that the copy needs,
     * which wouldn't ordinarily get set during a simple Card.copy() call.
     * </p>
     * */
    private final static Card copySpellHost(final SpellAbility sourceSA, final SpellAbility targetSA, Player controller) {
        final Card source = sourceSA.getHostCard();
        final Card original = targetSA.getHostCard();
        final Card c = copyCard(original, true);

        // clear remember/imprint for copied spells
        c.clearRemembered();
        c.clearImprintedCards();

        if (sourceSA.hasParam("NonLegendary")) {
            c.removeType(CardType.Supertype.Legendary);
        }

        // change the color of the copy (eg: Fork)
        if (sourceSA.hasParam("CopyIsColor")) {
            String tmp = "";
            final String newColor = sourceSA.getParam("CopyIsColor");
            if (newColor.equals("ChosenColor")) {
                tmp = CardUtil.getShortColorsString(source.getChosenColors());
            } else {
                tmp = CardUtil.getShortColorsString(Lists.newArrayList(newColor.split(",")));
            }
            final String finalColors = tmp;

            c.addColor(finalColors, !sourceSA.hasParam("OverwriteColors"), c.getTimestamp(), 0, false);
        }

        c.clearControllers();
        c.setOwner(controller);
        c.setCopiedSpell(true);
        c.setCopiedPermanent(original);

        c.setXManaCostPaidByColor(original.getXManaCostPaidByColor());
        c.setKickerMagnitude(original.getKickerMagnitude());

        for (OptionalCost cost : original.getOptionalCostsPaid()) {
            c.addOptionalCostPaid(cost);
        }
        if (targetSA.isBestow()) {
            c.animateBestow();
        }
        return c;
    }
    /**
     * <p>
     * copySpellAbilityAndPossiblyHost.
     * creates a copy of the Spell/ability `sa`, and puts it on the stack.
     * if sa is a spell, that spell's host is also copied.
     * </p>
     *
     * @param source
     *            a {@link forge.game.card.Card} object. The card doing the copying.
     * @param original
     *            a {@link forge.game.card.Card} object. The host of the spell or ability being copied.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object. The spell or ability being copied.
     * @param bCopyDetails
     *            a boolean.
     */
    public final static SpellAbility copySpellAbilityAndPossiblyHost(final SpellAbility sourceSA, final SpellAbility targetSA, final Player controller) {
        //it is only necessary to copy the host card if the SpellAbility is a spell, not an ability
        final Card c = targetSA.isSpell() && !sourceSA.hasParam("UseOriginalHost") ?
            copySpellHost(sourceSA, targetSA, controller) : targetSA.getHostCard();

        final SpellAbility copySA;
        if (targetSA.isTrigger() && targetSA.isWrapper()) {
            copySA = getCopiedTriggeredAbility((WrappedAbility)targetSA, c, controller);
        } else {
            copySA = targetSA.copy(c, controller, false);
        }

        copySA.setCopied(true);

        if (targetSA.usesTargeting()) {
            // do for SubAbilities too?
            copySA.setTargets(targetSA.getTargets().clone());
        }

        //remove all costs
        if (!copySA.isTrigger()) {
            copySA.setPayCosts(new Cost("", targetSA.isAbility()));
        }
        copySA.setActivatingPlayer(controller);

        copySA.setPaidHash(targetSA.getPaidHash());
        return copySA;
    }

    public final static Card getCard(final IPaperCard cp, final Player owner, final Game game) {
        return getCard(cp, owner, owner == null ? -1 : owner.getGame().nextCardId(), game);
    }
    public final static Card getCard(final IPaperCard cp, final Player owner, final int cardId, final Game game) {
        CardRules cardRules = cp.getRules();
        final Card c = readCard(cardRules, cp, cardId, game);
        c.setRules(cardRules);
        c.setOwner(owner);
        buildAbilities(c);

        c.setSetCode(cp.getEdition());
        c.setRarity(cp.getRarity());

        // Would like to move this away from in-game entities
        String originalPicture = cp.getImageKey(false);
        c.setImageKey(originalPicture);
        c.setToken(cp.isToken());

        if (c.hasAlternateState()) {
            if (c.isFlipCard()) {
                c.setState(CardStateName.Flipped, false);
                c.setImageKey(cp.getImageKey(true));
            }
            else if (c.hasBackSide() && cp instanceof PaperCard && cardRules != null) {
                c.setState(cardRules.getSplitType().getChangedStateName(), false);
                c.setImageKey(cp.getImageKey(true));
            }
            else if (c.isSplitCard()) {
                c.setState(CardStateName.LeftSplit, false);
                c.setImageKey(originalPicture);
                c.setSetCode(cp.getEdition());
                c.setRarity(cp.getRarity());
                c.setState(CardStateName.RightSplit, false);
                c.setImageKey(originalPicture);
            } else if (c.isAdventureCard()) {
                c.setState(CardStateName.Adventure, false);
                c.setImageKey(originalPicture);
            }

            c.setSetCode(cp.getEdition());
            c.setRarity(cp.getRarity());
            c.setState(CardStateName.Original, false);
        }

        return c;
    }

    private static void buildAbilities(final Card card) {
        for (final CardStateName state : card.getStates()) {
            if (card.hasBackSide() && state == CardStateName.FaceDown) {
                continue; // Ignore FaceDown for DFC since they have none.
            }
            card.setState(state, false);

            // ******************************************************************
            // ************** Link to different CardFactories *******************
            if (state == CardStateName.LeftSplit || state == CardStateName.RightSplit) {
                for (final SpellAbility sa : card.getSpellAbilities()) {
                    sa.setCardState(card.getState(state));
                }
                CardFactoryUtil.setupKeywordedAbilities(card);
                final CardState original = card.getState(CardStateName.Original);
                original.addNonManaAbilities(card.getCurrentState().getNonManaAbilities());
                original.addIntrinsicKeywords(card.getCurrentState().getIntrinsicKeywords()); // Copy 'Fuse' to original side
                original.getSVars().putAll(card.getCurrentState().getSVars()); // Unfortunately need to copy these to (Effect looks for sVars on execute)
            } else if (state != CardStateName.Original) {
            	CardFactoryUtil.setupKeywordedAbilities(card);
            }
            if (state == CardStateName.Adventure) {
                CardFactoryUtil.setupAdventureAbility(card);
            }
        }

        card.setState(CardStateName.Original, false);
        // need to update keyword cache for original spell
        if (card.isSplitCard()) {
            card.updateKeywordsCache(card.getCurrentState());
        }

        // ******************************************************************
        // ************** Link to different CardFactories *******************
        if (card.isPlane()) {
            buildPlaneAbilities(card);
        }
        CardFactoryUtil.setupKeywordedAbilities(card); // Should happen AFTER setting left/right split abilities to set Fuse ability to both sides
        card.getView().updateState(card);
    }

    private static void buildPlaneAbilities(Card card) {
        StringBuilder triggerSB = new StringBuilder();
        triggerSB.append("Mode$ PlanarDice | Result$ Planeswalk | TriggerZones$ Command | Secondary$ True | ");
        triggerSB.append("TriggerDescription$ Whenever you roll Planeswalk, put this card on the bottom of its owner's planar deck face down, ");
        triggerSB.append("then move the top card of your planar deck off that planar deck and turn it face up");

        String rolledWalk = "DB$ Planeswalk";

        Trigger planesWalkTrigger = TriggerHandler.parseTrigger(triggerSB.toString(), card, true);
        planesWalkTrigger.setOverridingAbility(AbilityFactory.getAbility(rolledWalk, card));
        card.addTrigger(planesWalkTrigger);

        StringBuilder saSB = new StringBuilder();
        saSB.append("AB$ RollPlanarDice | Cost$ X | SorcerySpeed$ True | Activator$ Player | ActivationZone$ Command | ");
        saSB.append("SpellDescription$ Roll the planar dice. X is equal to the amount of times the planar die has been rolled this turn.");

        SpellAbility planarRoll = AbilityFactory.getAbility(saSB.toString(), card);
        planarRoll.setSVar("X", "Count$RolledThisTurn");

        card.addSpellAbility(planarRoll);
    }

    private static Card readCard(final CardRules rules, final IPaperCard paperCard, int cardId, Game game) {
        final Card card = new Card(cardId, paperCard, game);

        // 1. The states we may have:
        CardSplitType st = rules.getSplitType();
        if (st == CardSplitType.Split) {
            card.addAlternateState(CardStateName.LeftSplit, false);
            card.setState(CardStateName.LeftSplit, false);
        }

        readCardFace(card, rules.getMainPart());

        if (st != CardSplitType.None) {
            card.addAlternateState(st.getChangedStateName(), false);
            card.setState(st.getChangedStateName(), false);
            if (rules.getOtherPart() != null) {
                readCardFace(card, rules.getOtherPart());
            } else if (!rules.getMeldWith().isEmpty()) {
                readCardFace(card, StaticData.instance().getCommonCards().getRules(rules.getMeldWith()).getOtherPart());
            }
        }

        if (card.isInAlternateState()) {
            card.setState(CardStateName.Original, false);
        }

        if (st == CardSplitType.Split) {
            card.setName(rules.getName());

            // Combined mana cost
            ManaCost combinedManaCost = ManaCost.combine(rules.getMainPart().getManaCost(), rules.getOtherPart().getManaCost());
            card.setManaCost(combinedManaCost);

            // Combined card color
            final byte combinedColor = (byte) (rules.getMainPart().getColor().getColor() | rules.getOtherPart().getColor().getColor());
            card.setColor(combinedColor);
            card.setType(new CardType(rules.getType()));

            // Combined text based on Oracle text - might not be necessary, temporarily disabled.
            //String combinedText = String.format("%s: %s\n%s: %s", rules.getMainPart().getName(), rules.getMainPart().getOracleText(), rules.getOtherPart().getName(), rules.getOtherPart().getOracleText());
            //card.setText(combinedText);
        }
        return card;
    }

    private static void readCardFace(Card c, ICardFace face) {
        // Build English oracle and translated oracle mapping
        if (c.getId() >= 0) {
            CardTranslation.buildOracleMapping(face.getName(), face.getOracleText());
        }

        // Name first so Senty has the Card name
        c.setName(face.getName());

        for (Entry<String, String> v : face.getVariables())  c.setSVar(v.getKey(), v.getValue());

        for (String r : face.getReplacements())              c.addReplacementEffect(ReplacementHandler.parseReplacement(r, c, true, c.getCurrentState()));
        for (String s : face.getStaticAbilities())           c.addStaticAbility(s);
        for (String t : face.getTriggers())                  c.addTrigger(TriggerHandler.parseTrigger(t, c, true, c.getCurrentState()));

        // keywords not before variables
        c.addIntrinsicKeywords(face.getKeywords(), false);

        c.setManaCost(face.getManaCost());
        c.setText(face.getNonAbilityText());

        c.getCurrentState().setBaseLoyalty(face.getInitialLoyalty());

        c.setOracleText(face.getOracleText());

        // Super and 'middle' types should use enums.
        c.setType(new CardType(face.getType()));

        c.setColor(face.getColor().getColor());

        if (face.getIntPower() != Integer.MAX_VALUE) {
            c.setBasePower(face.getIntPower());
            c.setBasePowerString(face.getPower());
        }
        if (face.getIntToughness() != Integer.MAX_VALUE) {
            c.setBaseToughness(face.getIntToughness());
            c.setBaseToughnessString(face.getToughness());
        }

        // SpellPermanent only for Original State
        if (c.getCurrentStateName() == CardStateName.Original || c.getCurrentStateName() == CardStateName.Modal) {
            // this is the "default" spell for permanents like creatures and artifacts
            if (c.isPermanent() && !c.isAura() && !c.isLand()) {
                SpellAbility sa = new SpellPermanent(c);

                // Currently only for Modal, might react different when state is always set
                //if (c.getCurrentStateName() == CardStateName.Modal) {
                    sa.setCardState(c.getCurrentState());
                //}
                c.addSpellAbility(sa);
            }
            // TODO add LandAbility there when refactor MayPlay
        }

        CardFactoryUtil.addAbilityFactoryAbilities(c, face.getAbilities());
    }

    /**
     * Copy the copiable characteristics of one card to another, taking the
     * states of both cards into account.
     *
     * @param from the {@link Card} to copy from.
     * @param to the {@link Card} to copy to.
     */
    public static void copyCopiableCharacteristics(final Card from, final Card to) {
    	final boolean toIsFaceDown = to.isFaceDown();
    	if (toIsFaceDown) {
    		// If to is face down, copy to its front side
    		to.setState(CardStateName.Original, false);
    		copyCopiableCharacteristics(from, to);
    		to.setState(CardStateName.FaceDown, false);
    		return;
    	}

    	final boolean fromIsFlipCard = from.isFlipCard();
        final boolean fromIsTransformedCard = from.getCurrentStateName() == CardStateName.Transformed || from.getCurrentStateName() == CardStateName.Meld;

    	if (fromIsFlipCard) {
    		if (to.getCurrentStateName().equals(CardStateName.Flipped)) {
    			copyState(from, CardStateName.Original, to, CardStateName.Original);
    		} else {
    			copyState(from, CardStateName.Original, to, to.getCurrentStateName());
    		}
    		copyState(from, CardStateName.Flipped, to, CardStateName.Flipped);
    	} else if (fromIsTransformedCard) {
            copyState(from, from.getCurrentStateName(), to, CardStateName.Original);
        } else {
            copyState(from, from.getCurrentStateName(), to, to.getCurrentStateName());
        }
    }

    /**
     * <p>
     * Copy stats like power, toughness, etc. from one card to another.
     * </p>
     * <p>
     * The copy is made independently for each state of the input {@link Card}.
     * This amounts to making a full copy of the card, including the current
     * state.
     * </p>
     *
     * @param in
     *            the {@link forge.game.card.Card} to be copied.
     * @param newOwner
     * 			  the {@link forge.game.player.Player} to be the owner of the newly
     * 			  created Card.
     * @return a new {@link forge.game.card.Card}.
     */
    public static Card copyStats(final Card in, final Player newOwner, boolean assignNewId) {
        int id = in.getId();
        if (assignNewId) {
            id = newOwner == null ? 0 : newOwner.getGame().nextCardId();
        }
        final Card c = new Card(id, in.getPaperCard(), in.getGame());

        c.setOwner(newOwner);
        c.setSetCode(in.getSetCode());

        for (final CardStateName state : in.getStates()) {
            CardFactory.copyState(in, state, c, state);
        }

        c.setState(in.getCurrentStateName(), false);
        c.setRules(in.getRules());

        return c;
    } // copyStats()

    /**
     * Copy characteristics of a particular state of one card to those of a
     * (possibly different) state of another.
     *
     * @param from
     *            the {@link Card} to copy from.
     * @param fromState
     *            the {@link CardStateName} of {@code from} to copy from.
     * @param to
     *            the {@link Card} to copy to.
     * @param toState
     *            the {@link CardStateName} of {@code to} to copy to.
     */
    public static void copyState(final Card from, final CardStateName fromState, final Card to,
            final CardStateName toState) {
        copyState(from, fromState, to, toState, true);
    }
    public static void copyState(final Card from, final CardStateName fromState, final Card to,
            final CardStateName toState, boolean updateView) {
        // copy characteristics not associated with a state
        to.setBasePowerString(from.getBasePowerString());
        to.setBaseToughnessString(from.getBaseToughnessString());
        to.setText(from.getSpellText());

        // get CardCharacteristics for desired state
        if (!to.getStates().contains(toState)) {
            to.addAlternateState(toState, updateView);
        }
        final CardState toCharacteristics = to.getState(toState), fromCharacteristics = from.getState(fromState);
        toCharacteristics.copyFrom(fromCharacteristics, false);
    }

    public static void copySpellAbility(SpellAbility from, SpellAbility to, final Card host, final Player p, final boolean lki) {
        if (from.getTargetRestrictions() != null) {
            to.setTargetRestrictions(from.getTargetRestrictions());
        }
        to.setDescription(from.getOriginalDescription());
        to.setStackDescription(from.getOriginalStackDescription());

        if (from.getSubAbility() != null) {
            to.setSubAbility((AbilitySub) from.getSubAbility().copy(host, p, lki));
        }
        for (Map.Entry<String, SpellAbility> e : from.getAdditionalAbilities().entrySet()) {
            to.setAdditionalAbility(e.getKey(), e.getValue().copy(host, p, lki));
        }
        for (Map.Entry<String, List<AbilitySub>> e : from.getAdditionalAbilityLists().entrySet()) {
            to.setAdditionalAbilityList(e.getKey(), Lists.transform(e.getValue(), new Function<AbilitySub, AbilitySub>() {
                @Override
                public AbilitySub apply(AbilitySub input) {
                    return (AbilitySub) input.copy(host, p, lki);
                }
            }));
        }
        if (from.getRestrictions() != null) {
            to.setRestrictions((SpellAbilityRestriction) from.getRestrictions().copy());
        }
        if (from.getConditions() != null) {
            to.setConditions((SpellAbilityCondition) from.getConditions().copy());
        }

        // do this after other abilities are copied
        if (p != null) {
            to.setActivatingPlayer(p, lki);
        }

        //to.changeText();
    }

    /**
     * Copy triggered ability
     *
     * return a wrapped ability
     */
    public static SpellAbility getCopiedTriggeredAbility(final WrappedAbility sa, final Card newHost, final Player controller) {
        if (!sa.isTrigger()) {
            return null;
        }

        return new WrappedAbility(sa.getTrigger(), sa.getWrappedAbility().copy(newHost, controller, false), sa.isOptionalTrigger() ? controller : null);
    }

    public static CardCloneStates getCloneStates(final Card in, final Card out, final CardTraitBase sa) {
        final Card host = sa.getHostCard();
        final Map<String,String> origSVars = host.getSVars();
        final List<String> types = Lists.newArrayList();
        final List<String> keywords = Lists.newArrayList();
        final List<String> removeKeywords = Lists.newArrayList();
        List<String> creatureTypes = null;
        final CardCloneStates result = new CardCloneStates(in, sa);

        final String newName = sa.getParamOrDefault("NewName", null);
        String shortColors = "";

        if (sa.hasParam("AddTypes")) {
            types.addAll(Arrays.asList(sa.getParam("AddTypes").split(" & ")));
        }

        if (sa.hasParam("AddKeywords")) {
            keywords.addAll(Arrays.asList(sa.getParam("AddKeywords").split(" & ")));
        }

        if (sa.hasParam("AddColors")) {
            shortColors = CardUtil.getShortColorsString(Arrays.asList(sa.getParam("AddColors")
                    .split(" & ")));
        }

        if (sa.hasParam("RemoveKeywords")) {
            removeKeywords.addAll(Arrays.asList(sa.getParam("RemoveKeywords").split(" & ")));
        }

        if (sa.hasParam("SetColor")) {
            shortColors = CardUtil.getShortColorsString(Arrays.asList(sa.getParam("SetColor").split(",")));
        }

        if (sa.hasParam("SetCreatureTypes")) {
            creatureTypes = ImmutableList.copyOf(sa.getParam("SetCreatureTypes").split(" "));
        }

        // TODO handle Volrath's Shapeshifter

        if (in.isFaceDown()) {
            // if something is cloning a facedown card, it only clones the
            // facedown state into original
            final CardState ret = new CardState(out, CardStateName.Original);
            ret.copyFrom(in.getFaceDownState(), false);
            result.put(CardStateName.Original, ret);
        } else if (in.isFlipCard()) {
            // if something is cloning a flip card, copy both original and
            // flipped state
            final CardState ret1 = new CardState(out, CardStateName.Original);
            ret1.copyFrom(in.getState(CardStateName.Original, true), false);
            result.put(CardStateName.Original, ret1);

            final CardState ret2 = new CardState(out, CardStateName.Flipped);
            ret2.copyFrom(in.getState(CardStateName.Flipped, true), false);
            result.put(CardStateName.Flipped, ret2);
        } else if (in.isAdventureCard()) {
            final CardState ret1 = new CardState(out, CardStateName.Original);
            ret1.copyFrom(in.getState(CardStateName.Original, true), false);
            result.put(CardStateName.Original, ret1);

            final CardState ret2 = new CardState(out, CardStateName.Adventure);
            ret2.copyFrom(in.getState(CardStateName.Adventure, true), false);
            result.put(CardStateName.Adventure, ret2);
        } else {
            // in all other cases just copy the current state to original
            final CardState ret = new CardState(out, CardStateName.Original);
            ret.copyFrom(in.getState(in.getCurrentStateName(), true), false);
            result.put(CardStateName.Original, ret);
        }

        // update all states, both for flip cards
        for (Map.Entry<CardStateName, CardState> e : result.entrySet()) {
            final CardState originalState = out.getState(e.getKey());
            final CardState state = e.getValue();
            // update the names for the states
            if (sa.hasParam("KeepName")) {
                state.setName(originalState.getName());
            } else if (newName != null) {
                state.setName(newName);
            }

            if (sa.hasParam("AddColors")) {
                state.addColor(shortColors);
            }

            if (sa.hasParam("SetColor")) {
                state.setColor(shortColors);
            }

            if (sa.hasParam("NonLegendary")) {
                state.removeType(CardType.Supertype.Legendary);
            }

            state.addType(types);

            if (creatureTypes != null) {
                state.setCreatureTypes(creatureTypes);
            }

            state.addIntrinsicKeywords(keywords);
            for (String kw : removeKeywords) {
                state.removeIntrinsicKeyword(kw);
            }

            if (sa.hasParam("SetPower")) {
                state.setBasePower(Integer.parseInt(sa.getParam("SetPower")));
            }
            if (sa.hasParam("SetToughness")) {
                state.setBaseToughness(Integer.parseInt(sa.getParam("SetToughness")));
            }
            if (sa.hasParam("SetLoyalty")) {
                state.setBaseLoyalty(String.valueOf(sa.getParam("SetLoyalty")));
            }

            // Planning a Vizier of Many Faces rework; always might come in handy
            if (sa.hasParam("RemoveCost")) {
                state.setManaCost(ManaCost.NO_COST);
            }

            // SVars to add to clone
            if (sa.hasParam("AddSVars") || sa.hasParam("GainTextSVars")) {
                final String str = sa.getParamOrDefault("GainTextSVars", sa.getParam("AddSVars"));
                for (final String s : str.split(",")) {
                    if (origSVars.containsKey(s)) {
                        final String actualsVar = origSVars.get(s);
                        state.setSVar(s, actualsVar);
                    }
                }
            }

            // triggers to add to clone
            if (sa.hasParam("AddTriggers")) {
                for (final String s : sa.getParam("AddTriggers").split(",")) {
                    if (origSVars.containsKey(s)) {
                        final String actualTrigger = origSVars.get(s);
                        final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, out, true, state);
                        state.addTrigger(parsedTrigger);
                    }
                }
            }

            // abilities to add to clone
            if (sa.hasParam("AddAbilities") || sa.hasParam("GainTextAbilities")) {
                final String str = sa.getParamOrDefault("GainTextAbilities", sa.getParam("AddAbilities"));
                for (final String s : str.split(",")) {
                    if (origSVars.containsKey(s)) {
                        final String actualAbility = origSVars.get(s);
                        final SpellAbility grantedAbility = AbilityFactory.getAbility(actualAbility, out);
                        grantedAbility.setIntrinsic(true);
                        state.addSpellAbility(grantedAbility);
                    }
                }
            }

            // static abilities to add to clone
            if (sa.hasParam("AddStaticAbilities")) {
                final String str = sa.getParam("AddStaticAbilities");
                for (final String s : str.split(",")) {
                    if (origSVars.containsKey(s)) {
                        final String actualStatic = origSVars.get(s);
                        final StaticAbility grantedStatic = new StaticAbility(actualStatic, out, sa.getCardState());
                        grantedStatic.setIntrinsic(true);
                        state.addStaticAbility(grantedStatic);
                    }
                }
            }

            if (sa.hasParam("GainThisAbility") && (sa instanceof SpellAbility)) {
                SpellAbility root = ((SpellAbility) sa).getRootAbility();

                if (root.isTrigger() && root.getTrigger() != null) {
                    state.addTrigger(root.getTrigger().copy(out, false));
                } else if (root.isReplacementAbility()) {
                    state.addReplacementEffect(root.getReplacementEffect().copy(out, false));
                } else {
                    state.addSpellAbility(root.copy(out, false));
                }
            }

            // Special Rules for Embalm and Eternalize
            if (sa.hasParam("Embalm")  && out.isEmbalmed()) {
                state.addType("Zombie");
                state.setColor(MagicColor.WHITE);
                state.setManaCost(ManaCost.NO_COST);

                String name = TextUtil.fastReplace(
                        TextUtil.fastReplace(host.getName(), ",", ""),
                        " ", "_").toLowerCase();
                String set = host.getSetCode().toLowerCase();
                state.setImageKey(ImageKeys.getTokenKey("embalm_" + name + "_" + set));
            }

            if (sa.hasParam("Eternalize") && out.isEternalized()) {
                state.addType("Zombie");
                state.setColor(MagicColor.BLACK);
                state.setManaCost(ManaCost.NO_COST);
                state.setBasePower(4);
                state.setBaseToughness(4);

                String name = TextUtil.fastReplace(
                    TextUtil.fastReplace(host.getName(), ",", ""),
                        " ", "_").toLowerCase();
                String set = host.getSetCode().toLowerCase();
                state.setImageKey(ImageKeys.getTokenKey("eternalize_" + name + "_" + set));
            }

            // set the host card for copied replacement effects
            // needed for copied xPaid ETB effects (for the copy, xPaid = 0)

            if (sa.hasParam("GainTextOf") && originalState != null) {
                state.setSetCode(originalState.getSetCode());
                state.setRarity(originalState.getRarity());
                state.setImageKey(originalState.getImageKey());
            }

            // remove some characteristic static abilties
            for (StaticAbility sta : state.getStaticAbilities()) {
                if (!sta.hasParam("CharacteristicDefining")) {
                    continue;
                }

                if (sa.hasParam("SetPower") || sa.hasParam("Eternalize")) {
                    if (sta.hasParam("SetPower"))
                        state.removeStaticAbility(sta);
                }
                if (sa.hasParam("SetToughness") || sa.hasParam("Eternalize")) {
                    if (sta.hasParam("SetToughness"))
                        state.removeStaticAbility(sta);
                }
                if (sa.hasParam("SetCreatureTypes")) {
                    // currently only Changeling and similar should be affected by that
                    // other cards using AddType$ ChosenType should not
                    if (sta.hasParam("AddType") && CardType.AllCreatureTypes.equals(sta.getParam("AddType"))) {
                        state.removeStaticAbility(sta);
                    }
                }
                if (sa.hasParam("SetColor") || sa.hasParam("Embalm") || sa.hasParam("Eternalize")) {
                    if (sta.hasParam("SetColor")) {
                        state.removeStaticAbility(sta);
                    }
                }
            }

            // remove some keywords
            if (sa.hasParam("SetCreatureTypes")) {
                state.removeIntrinsicKeyword("Changeling");
            }
            if (sa.hasParam("SetColor") || sa.hasParam("Embalm") || sa.hasParam("Eternalize")) {
                state.removeIntrinsicKeyword("Devoid");
            }
        }
        return result;
    }

    public static CardCloneStates getMutatedCloneStates(final Card card, final CardTraitBase sa) {
        final Card top = card.getTopMergedCard();
        final CardStateName state = top.getCurrentStateName();
        final CardState ret = new CardState(card, state);
        if (top.isCloned()) {
            ret.copyFrom(top.getState(state, true), false);
        } else {
            ret.copyFrom(top.getOriginalState(state), false);
        }

        boolean first = true;
        for (final Card c : card.getMergedCards()) {
            if (first) {
                first = false;
                continue;
            }
            ret.addAbilitiesFrom(c.getCurrentState(), false);
        }

        final CardCloneStates result = new CardCloneStates(top, sa);
        result.put(state, ret);

        // For face down, flipped, transformed, melded or MDFC card, also copy the original state to avoid crash
        if (state != CardStateName.Original) {
            final CardState ret1 = new CardState(card, CardStateName.Original);
            ret1.copyFrom(top.getState(CardStateName.Original, true), false);
            result.put(CardStateName.Original, ret1);
        }

        return result;
    }

} // end class AbstractCardFactory
