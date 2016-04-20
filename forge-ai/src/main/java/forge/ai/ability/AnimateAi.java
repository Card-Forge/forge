package forge.ai.ability;

import com.google.common.collect.Iterables;

import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.card.CardType;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactory;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityContinuous;
import forge.game.staticability.StaticAbilityLayer;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * AbilityFactoryAnimate class.
 * </p>
 * 
 * @author Forge
 * @version $Id: AbilityFactoryAnimate.java 17608 2012-10-20 22:27:27Z Max mtg $
 */

public class AnimateAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Card source = sa.getHostCard();
        final Game game = aiPlayer.getGame();
        final PhaseHandler ph = game.getPhaseHandler();
        
        // TODO - add some kind of check to answer
        // "Am I going to attack with this?"
        // TODO - add some kind of check for during human turn to answer
        // "Can I use this to block something?"
        
        if (sa.getConditions() != null && !sa.getConditions().areMet(sa) && sa.getSubAbility() == null) {
        	return false;
        }

        //interrupt sacrifice effect
        if (!game.getStack().isEmpty()) {
            SpellAbility topStack = game.getStack().peekAbility();
            if (topStack.getApi() == ApiType.Sacrifice) {
                final String valid = topStack.getParamOrDefault("SacValid", "Card.Self");
                String num = topStack.getParam("Amount");
                num = (num == null) ? "1" : num;
                final int nToSac = AbilityUtils.calculateAmount(topStack.getHostCard(), num, topStack);
                CardCollection list =
                        CardLists.getValidCards(aiPlayer.getCardsIn(ZoneType.Battlefield), valid.split(","),
                                aiPlayer.getOpponent(), topStack.getHostCard(), topStack);
                list = CardLists.filter(list, CardPredicates.canBeSacrificedBy(topStack));
                ComputerUtilCard.sortByEvaluateCreature(list);
                if (!list.isEmpty() && list.size() == nToSac && ComputerUtilCost.canPayCost(sa, aiPlayer)) {
                    Card animatedCopy = CardFactory.copyCard(source, true);
                    becomeAnimated(animatedCopy, source.hasSickness(), sa);
                    list.add(animatedCopy);
                    list = CardLists.getValidCards(list, valid.split(","), aiPlayer.getOpponent(), topStack.getHostCard(), topStack);
                    list = CardLists.filter(list, CardPredicates.canBeSacrificedBy(topStack));
                    if (ComputerUtilCard.evaluateCreature(animatedCopy) < ComputerUtilCard.evaluateCreature(list.get(0))
                            && list.contains(animatedCopy)) {
                        return true;
                    }
                }
            }
        }

        // Launch the Fleet (why is this an Animate ability?)
        if (sa.hasParam("AILogic") && "Attacking".equals(sa.getParam("AILogic"))) {
            if (ph.getPlayerTurn().isOpponentOf(aiPlayer) || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)) { 
                return false;
            }
            List<Card> list = CardLists.getValidCards(aiPlayer.getCreaturesInPlay(), tgt.getValidTgts(), aiPlayer, source, sa);
            for (Card c : list) {
                if (ComputerUtilCard.doesCreatureAttackAI(aiPlayer, c)) {
                    sa.getTargets().add(c);
                }
            }
            return !sa.getTargets().isEmpty();
        }
        
        // don't use instant speed animate abilities outside computers
        // Combat_Begin step
        if (!ph.is(PhaseType.COMBAT_BEGIN)
                && ph.isPlayerTurn(aiPlayer)
                && !SpellAbilityAi.isSorcerySpeed(sa)
                && !sa.hasParam("ActivationPhases") && !sa.hasParam("Permanent")) {
            return false;
        }

        Player opponent = aiPlayer.getWeakestOpponent();
        // don't animate if the AI won't attack anyway
        if (ph.isPlayerTurn(aiPlayer)
                && aiPlayer.getLife() < 6
                && opponent.getLife() > 6
                && Iterables.any(opponent.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.CREATURES)
                && !sa.hasParam("AILogic")
                && !sa.hasParam("Permanent")) {
            return false;
        }

        // don't use instant speed animate abilities outside humans
        // Combat_Declare_Attackers_InstantAbility step
        if (ph.getPlayerTurn().isOpponentOf(aiPlayer) && !sa.hasParam("Permanent") &&
                (!ph.is(PhaseType.COMBAT_DECLARE_ATTACKERS, opponent) || game.getCombat() != null && game.getCombat().getAttackersOf(aiPlayer).isEmpty())) {
            return false;
        }

        // don't activate during main2 unless this effect is permanent
        if (ph.is(PhaseType.MAIN2) && !sa.hasParam("Permanent") && !sa.hasParam("UntilYourNextTurn")) {
            return false;
        }

        if (null == tgt) {
            final List<Card> defined = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa);

            boolean bFlag = false;
            if (sa.hasParam("AILogic")) {
                if ("EOT".equals(sa.getParam("AILogic"))) {
                    if (ph.getPhase().isBefore(PhaseType.MAIN2)) {
                        return false;
                    } else {
                        bFlag = true;
                    }
                } if ("Never".equals(sa.getParam("AILogic"))) {
                    return false;
                }
            } else {
            	boolean givesHaste = sa.hasParam("Keywords") && sa.getParam("Keywords").contains("Haste");
            	for (final Card c : defined) {
	                bFlag |= !c.isCreature() && !c.isTapped()
	                        && (c.getTurnInZone() != ph.getTurn() || givesHaste || ph.getPlayerTurn().isOpponentOf(aiPlayer))
	                        && !c.isEquipping();
	                
	                // for creatures that could be improved (like Figure of Destiny)
	                if (!bFlag && c.isCreature() && (sa.hasParam("Permanent") || (!c.isTapped() && !c.isSick()))) {
	                    int power = -5;
	                    if (sa.hasParam("Power")) {
	                        power = AbilityUtils.calculateAmount(source, sa.getParam("Power"), sa);
	                    }
	                    int toughness = -5;
	                    if (sa.hasParam("Toughness")) {
	                        toughness = AbilityUtils.calculateAmount(source, sa.getParam("Toughness"), sa);
	                    }
	                    if (sa.hasParam("Keywords")) {
	                    	for (String keyword : sa.getParam("Keywords").split(" & ")) {
	                    		if (!source.hasKeyword(keyword)) {
	                    			bFlag = true;
	                    		}
	                    	}
	                    }
	                    if (power + toughness > c.getCurrentPower() + c.getCurrentToughness()) {
	                        bFlag = true;
	                    }
	                }

	                if (!SpellAbilityAi.isSorcerySpeed(sa) && !sa.hasParam("Permanent")) {
	                    Card animatedCopy = CardFactory.getCard(c.getPaperCard(), aiPlayer, c.getGame());
	                    AnimateAi.becomeAnimated(animatedCopy, c.hasSickness(), sa);
	                    if (ph.isPlayerTurn(aiPlayer) && !ComputerUtilCard.doesSpecifiedCreatureAttackAI(aiPlayer, animatedCopy)) {
	                        return false;
	                    }
	                    if (ph.getPlayerTurn().isOpponentOf(aiPlayer) && !ComputerUtilCard.doesSpecifiedCreatureBlock(aiPlayer, animatedCopy)) {
	                        return false;
	                    }
	                }
            	}
            }

            if (!bFlag) { // All of the defined stuff is animated, not very
                          // useful
                return false;
            }
        } else {
            sa.resetTargets();
            if (!animateTgtAI(sa)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        if (sa.usesTargeting()) {
            sa.resetTargets();
            if (!animateTgtAI(sa)) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        if (sa.usesTargeting() && !animateTgtAI(sa) && !mandatory) {
            return false;
        }

        // Improve AI for triggers. If source is a creature with:
        // When ETB, sacrifice a creature. Check to see if the AI has something
        // to sacrifice

        // Eventually, we can call the trigger of ETB abilities with
        // not mandatory as part of the checks to cast something
        if (sa.hasParam("AITgts")) {
        	final TargetRestrictions tgt = sa.getTargetRestrictions();
            final Card animateSource = sa.getHostCard();
            CardCollectionView list = aiPlayer.getGame().getCardsIn(tgt.getZone());
            list = CardLists.getValidCards(list, tgt.getValidTgts(), sa.getActivatingPlayer(), animateSource, sa);
            CardCollection prefList = CardLists.getValidCards(list, sa.getParam("AITgts"), sa.getActivatingPlayer(), animateSource);
            if (!prefList.isEmpty()){
	        	CardLists.shuffle(prefList);
	        	sa.getTargets().add(prefList.getFirst());
            }
        }
        return true;
    }

    private boolean animateTgtAI(final SpellAbility sa) {
        // This is reasonable for now. Kamahl, Fist of Krosa and a sorcery or
        // two are the only things
        // that animate a target. Those can just use SVar:RemAIDeck:True until
        // this can do a reasonably
        // good job of picking a good target
        return false;
    }

    public static void becomeAnimated(final Card card, final boolean hasOriginalCardSickness, final SpellAbility sa) {
        //duplicating AnimateEffect.resolve
        final Card source = sa.getHostCard();
        final Game game = sa.getActivatingPlayer().getGame();
        final Map<String, String> svars = source.getSVars();
        final long timestamp = game.getNextTimestamp();
        card.setSickness(hasOriginalCardSickness);

        // AF specific sa
        int power = -1;
        if (sa.hasParam("Power")) {
            power = AbilityUtils.calculateAmount(source, sa.getParam("Power"), sa);
        }
        int toughness = -1;
        if (sa.hasParam("Toughness")) {
            toughness = AbilityUtils.calculateAmount(source, sa.getParam("Toughness"), sa);
        }

        final CardType types = new CardType();
        if (sa.hasParam("Types")) {
            types.addAll(Arrays.asList(sa.getParam("Types").split(",")));
        }

        final CardType removeTypes = new CardType();
        if (sa.hasParam("RemoveTypes")) {
            removeTypes.addAll(Arrays.asList(sa.getParam("RemoveTypes").split(",")));
        }

        // allow ChosenType - overrides anything else specified
        if (types.hasSubtype("ChosenType")) {
            types.clear();
            types.add(source.getChosenType());
        }

        final List<String> keywords = new ArrayList<String>();
        if (sa.hasParam("Keywords")) {
            keywords.addAll(Arrays.asList(sa.getParam("Keywords").split(" & ")));
        }

        final List<String> removeKeywords = new ArrayList<String>();
        if (sa.hasParam("RemoveKeywords")) {
            removeKeywords.addAll(Arrays.asList(sa.getParam("RemoveKeywords").split(" & ")));
        }

        final List<String> hiddenKeywords = new ArrayList<String>();
        if (sa.hasParam("HiddenKeywords")) {
            hiddenKeywords.addAll(Arrays.asList(sa.getParam("HiddenKeywords").split(" & ")));
        }
        // allow SVar substitution for keywords
        for (int i = 0; i < keywords.size(); i++) {
            final String k = keywords.get(i);
            if (svars.containsKey(k)) {
                keywords.add(svars.get(k));
                keywords.remove(k);
            }
        }

        // colors to be added or changed to
        String tmpDesc = "";
        if (sa.hasParam("Colors")) {
            final String colors = sa.getParam("Colors");
            if (colors.equals("ChosenColor")) {
                tmpDesc = CardUtil.getShortColorsString(source.getChosenColors());
            }
            else {
                tmpDesc = CardUtil.getShortColorsString(new ArrayList<String>(Arrays.asList(colors.split(","))));
            }
        }
        final String finalDesc = tmpDesc;

        // abilities to add to the animated being
        final List<String> abilities = new ArrayList<String>();
        if (sa.hasParam("Abilities")) {
            abilities.addAll(Arrays.asList(sa.getParam("Abilities").split(",")));
        }

        // replacement effects to add to the animated being
        final List<String> replacements = new ArrayList<String>();
        if (sa.hasParam("Replacements")) {
            replacements.addAll(Arrays.asList(sa.getParam("Replacements").split(",")));
        }

        // triggers to add to the animated being
        final List<String> triggers = new ArrayList<String>();
        if (sa.hasParam("Triggers")) {
            triggers.addAll(Arrays.asList(sa.getParam("Triggers").split(",")));
        }

        // static abilities to add to the animated being
        final List<String> stAbs = new ArrayList<String>();
        if (sa.hasParam("staticAbilities")) {
            stAbs.addAll(Arrays.asList(sa.getParam("staticAbilities").split(",")));
        }

        // sVars to add to the animated being
        final List<String> sVars = new ArrayList<String>();
        if (sa.hasParam("sVars")) {
            sVars.addAll(Arrays.asList(sa.getParam("sVars").split(",")));
        }
        
        //duplicating AnimateEffectBase.doAnimate
        boolean removeSuperTypes = false;
        boolean removeCardTypes = false;
        boolean removeSubTypes = false;
        boolean removeCreatureTypes = false;

        if (sa.hasParam("OverwriteTypes")) {
            removeSuperTypes = true;
            removeCardTypes = true;
            removeSubTypes = true;
            removeCreatureTypes = true;
        }

        if (sa.hasParam("KeepSupertypes")) {
            removeSuperTypes = false;
        }

        if (sa.hasParam("KeepCardTypes")) {
            removeCardTypes = false;
        }

        if (sa.hasParam("RemoveSuperTypes")) {
            removeSuperTypes = true;
        }

        if (sa.hasParam("RemoveCardTypes")) {
            removeCardTypes = true;
        }

        if (sa.hasParam("RemoveSubTypes")) {
            removeSubTypes = true;
        }

        if (sa.hasParam("RemoveCreatureTypes")) {
            removeCreatureTypes = true;
        }

        if ((power != -1) || (toughness != -1)) {
            card.addNewPT(power, toughness, timestamp);
        }

        if (!types.isEmpty() || !removeTypes.isEmpty() || removeCreatureTypes) {
            card.addChangedCardTypes(types, removeTypes, removeSuperTypes, removeCardTypes, removeSubTypes,
                    removeCreatureTypes, timestamp);
        }

        card.addChangedCardKeywords(keywords, removeKeywords, sa.hasParam("RemoveAllAbilities"), timestamp);

        for (final String k : hiddenKeywords) {
            card.addHiddenExtrinsicKeyword(k);
        }

        card.addColor(finalDesc, !sa.hasParam("OverwriteColors"), timestamp);
        
        //back to duplicating AnimateEffect.resolve
        //TODO will all these abilities/triggers/replacements/etc. lead to memory leaks or unintended effects?
     // remove abilities
        final List<SpellAbility> removedAbilities = new ArrayList<SpellAbility>();
        boolean clearAbilities = sa.hasParam("OverwriteAbilities");
        boolean clearSpells = sa.hasParam("OverwriteSpells");
        boolean removeAll = sa.hasParam("RemoveAllAbilities");

        if (clearAbilities || clearSpells || removeAll) {
            for (final SpellAbility ab : card.getSpellAbilities()) {
                if (removeAll || (ab.isAbility() && clearAbilities)
                        || (ab.isSpell() && clearSpells)) {
                    card.removeSpellAbility(ab);
                    removedAbilities.add(ab);
                }
            }
        }

        // give abilities
        final List<SpellAbility> addedAbilities = new ArrayList<SpellAbility>();
        if (abilities.size() > 0) {
            for (final String s : abilities) {
                final String actualAbility = source.getSVar(s);
                final SpellAbility grantedAbility = AbilityFactory.getAbility(actualAbility, source);
                addedAbilities.add(grantedAbility);
                card.addSpellAbility(grantedAbility);
            }
        }

        // Grant triggers
        final List<Trigger> addedTriggers = new ArrayList<Trigger>();
        if (triggers.size() > 0) {
            for (final String s : triggers) {
                final String actualTrigger = source.getSVar(s);
                final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, source, false);
                addedTriggers.add(card.addTrigger(parsedTrigger));
            }
        }

        // give replacement effects
        final List<ReplacementEffect> addedReplacements = new ArrayList<ReplacementEffect>();
        if (replacements.size() > 0) {
            for (final String s : replacements) {
                final String actualReplacement = source.getSVar(s);
                final ReplacementEffect parsedReplacement = ReplacementHandler.parseReplacement(actualReplacement, source, false);
                addedReplacements.add(card.addReplacementEffect(parsedReplacement));
            }
        }

        // suppress triggers from the animated card
        final List<Trigger> removedTriggers = new ArrayList<Trigger>();
        if (sa.hasParam("OverwriteTriggers") || removeAll) {
            final FCollectionView<Trigger> triggersToRemove = card.getTriggers();
            for (final Trigger trigger : triggersToRemove) {
                trigger.setSuppressed(true);
                removedTriggers.add(trigger);
            }
        }

        // give static abilities (should only be used by cards to give
        // itself a static ability)
        if (stAbs.size() > 0) {
            for (final String s : stAbs) {
                final String actualAbility = source.getSVar(s);
                final StaticAbility stAb = card.addStaticAbility(actualAbility);
                if ("Continuous".equals(stAb.getMapParams().get("Mode"))) {
                    for (final StaticAbilityLayer layer : stAb.getLayers()) {
                        StaticAbilityContinuous.applyContinuousAbility(stAb, new CardCollection(card), layer);
                    }
                } 
            }
        }

        // give sVars
        if (sVars.size() > 0) {
            for (final String s : sVars) {
                String actualsVar = source.getSVar(s);
                String name = s;
                if (actualsVar.startsWith("SVar:")) {
                    actualsVar = actualsVar.split("SVar:")[1];
                    name = actualsVar.split(":")[0];
                    actualsVar = actualsVar.split(":")[1];
                }
                card.setSVar(name, actualsVar);
            }
        }

        // suppress static abilities from the animated card
        final List<StaticAbility> removedStatics = new ArrayList<StaticAbility>();
        if (sa.hasParam("OverwriteStatics") || removeAll) {
            final FCollectionView<StaticAbility> staticsToRemove = card.getStaticAbilities();
            for (final StaticAbility stAb : staticsToRemove) {
                stAb.setTemporarilySuppressed(true);
                removedStatics.add(stAb);
            }
        }

        // suppress static abilities from the animated card
        final List<ReplacementEffect> removedReplacements = new ArrayList<ReplacementEffect>();
        if (sa.hasParam("OverwriteReplacements") || removeAll) {
            for (final ReplacementEffect re : card.getReplacementEffects()) {
                re.setTemporarilySuppressed(true);
                removedReplacements.add(re);
            }
        }
        ComputerUtilCard.applyStaticContPT(game, card, null);
    }
}
