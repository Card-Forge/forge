package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import forge.card.CardStateName;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.StaticData;
import forge.card.CardRulesPredicates;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardZoneTable;
import forge.game.cost.Cost;
import forge.game.cost.CostDiscard;
import forge.game.cost.CostPart;
import forge.game.cost.CostReveal;
import forge.game.keyword.Keyword;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.replacement.ReplacementLayer;
import forge.game.spellability.AlternativeCost;
import forge.game.spellability.LandAbility;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityPredicates;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.CardTranslation;
import forge.util.Lang;
import forge.util.Localizer;

public class PlayEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        sb.append(sa.getActivatingPlayer().toString()).append(" ");

        if (sa.hasParam("ValidSA")) {
            sb.append(sa.hasParam("Optional") ? "may cast " : "cast ");
        } else {
            sb.append(sa.hasParam("Optional") ? "may play " : "plays ");
        }

        final List<Card> tgtCards = getDefinedCardsOrTargeted(sa);

        if (sa.hasParam("Valid")) {
            sb.append("cards");
        } else if (sa.hasParam("DefinedDesc")) {
            sb.append(sa.getParam("DefinedDesc"));
        } else {
            sb.append(Lang.joinHomogenous(tgtCards));
        }
        if (sa.hasParam("WithoutManaCost")) {
            sb.append(" without paying ").append(tgtCards.size()==1 ? "its" : "their").append(" mana cost");
        }
        if (sa.hasParam("IfDesc")) {
            sb.append(" ").append(sa.getParam("IfDesc"));
        }
        sb.append(".");
        return sb.toString();
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = source.getGame();
        boolean optional = sa.hasParam("Optional");
        final boolean remember = sa.hasParam("RememberPlayed");
        final boolean imprint = sa.hasParam("ImprintPlayed");
        final boolean forget = sa.hasParam("ForgetPlayed");
        final boolean hasTotalCMCLimit = sa.hasParam("WithTotalCMC");
        final boolean altCost = sa.hasParam("WithoutManaCost") || sa.hasParam("PlayCost");
        int totalCMCLimit = Integer.MAX_VALUE;
        final Player controller;
        if (sa.hasParam("Controller")) {
            controller = AbilityUtils.getDefinedPlayers(source, sa.getParam("Controller"), sa).get(0);
        } else {
            controller = sa.getActivatingPlayer();
        }

        long controlledByTimeStamp = -1;
        Player controlledByPlayer = null;
        if (sa.hasParam("ControlledByPlayer")) {
            controlledByTimeStamp = game.getNextTimestamp();
            controlledByPlayer = AbilityUtils.getDefinedPlayers(source, sa.getParam("ControlledByPlayer"), sa).get(0);
        }

        CardCollection tgtCards;
        CardCollectionView showCards = new CardCollection();

        if (sa.hasParam("Valid")) {
            List<ZoneType> zones = sa.hasParam("ValidZone") ? ZoneType.listValueOf(sa.getParam("ValidZone")) : ImmutableList.of(ZoneType.Hand);
            tgtCards = new CardCollection(AbilityUtils.filterListByType(game.getCardsIn(zones), sa.getParam("Valid"), sa));
            if (sa.hasParam("ShowCards")) {
                showCards = AbilityUtils.filterListByType(game.getCardsIn(zones), sa.getParam("ShowCards"), sa);
            }
        } else if (sa.hasParam("AnySupportedCard")) {
            final String valid = sa.getParam("AnySupportedCard");
            List<PaperCard> cards = null;
            if (valid.startsWith("Names:")) {
                cards = new ArrayList<>();
                for (String name : valid.substring(6).split(",")) {
                    name = name.replace(";", ",");
                    cards.add(StaticData.instance().getCommonCards().getUniqueByName(name));
                }
            } else if (valid.equalsIgnoreCase("sorcery")) {
                cards = Lists.newArrayList(StaticData.instance().getCommonCards().getUniqueCards());
                final Predicate<PaperCard> cpp = Predicates.compose(CardRulesPredicates.Presets.IS_SORCERY, PaperCard.FN_GET_RULES);
                cards = Lists.newArrayList(Iterables.filter(cards, cpp));
            } else if (valid.equalsIgnoreCase("instant")) {
                cards = Lists.newArrayList(StaticData.instance().getCommonCards().getUniqueCards());
                final Predicate<PaperCard> cpp = Predicates.compose(CardRulesPredicates.Presets.IS_INSTANT, PaperCard.FN_GET_RULES);
                cards = Lists.newArrayList(Iterables.filter(cards, cpp));
            }
            if (sa.hasParam("RandomCopied")) {
                final CardCollection choice = new CardCollection();
                final String num = sa.getParamOrDefault("RandomNum", "1");
                int ncopied = AbilityUtils.calculateAmount(source, num, sa);
                for (PaperCard cp : Aggregates.random(cards, ncopied)) {
                    final Card possibleCard = Card.fromPaperCard(cp, sa.getActivatingPlayer());
                    if (sa.getActivatingPlayer().isAI() && possibleCard.getRules() != null && possibleCard.getRules().getAiHints().getRemAIDecks())
                        continue;
                    // Need to temporarily set the Owner so the Game is set
                    possibleCard.setOwner(sa.getActivatingPlayer());
                    choice.add(possibleCard);
                }
                if (sa.hasParam("ChoiceNum")) {
                    System.err.println("Offering random spells to copy: " + choice.toString());
                    final int choicenum = AbilityUtils.calculateAmount(source, sa.getParam("ChoiceNum"), sa);
                    tgtCards = new CardCollection(
                        controller.getController().chooseCardsForEffect(choice, sa,
                            source + " - " + Localizer.getInstance().getMessage("lblChooseUpTo") + " " + Lang.nounWithNumeral(choicenum, "card"), 0, choicenum, true, null
                        )
                    );
                } else {
                    tgtCards = choice;
                }
                System.err.println("Copying random spell(s): " + tgtCards.toString());
            } else {
                return;
            }
        } else if (sa.hasParam("CopyFromChosenName")) {
            String name = source.getNamedCard();
            if (name.trim().isEmpty()) {
                name = controller.getNamedCard();
                if(name.trim().isEmpty()) {
                    return;
                }
            }
            Card card = Card.fromPaperCard(StaticData.instance().getCommonCards().getUniqueByName(name), controller);
            // so it gets added to stack
            card.setCopiedPermanent(card);
            // Keeps adventures from leaving the recast effect
            card.setCopiedSpell(true);
            card.setToken(true);
            tgtCards = new CardCollection(card);
        } else {
            tgtCards = new CardCollection();
            // filter only cards that didn't change zones
            for (Card c : getTargetCards(sa)) {
                Card gameCard = game.getCardState(c, null);
                if (c.equalsWithGameTimestamp(gameCard)) {
                    tgtCards.add(gameCard);
                } else if (sa.hasParam("ZoneRegardless")) {
                    tgtCards.add(c);
                }
            }
        }

        if (tgtCards.isEmpty()) {
            return;
        }

        if (sa.hasParam("ValidSA")) {
            final String valid[] = sa.getParam("ValidSA").split(",");
            Iterator<Card> it = tgtCards.iterator();
            while (it.hasNext()) {
                Card c = it.next();
                if (!Iterables.any(AbilityUtils.getBasicSpellsFromPlayEffect(c, controller), SpellAbilityPredicates.isValid(valid, controller , source, sa))) {
                    // it.remove will only remove item from the list part of CardCollection
                    tgtCards.asSet().remove(c);
                    it.remove();
                }
            }
            if (tgtCards.isEmpty()) {
                return;
            }
        }

        int amount = 1;
        if (sa.hasParam("Amount")) {
            if (sa.getParam("Amount").equals("All")) {
                amount = tgtCards.size();
            } else {
                amount = AbilityUtils.calculateAmount(source, sa.getParam("Amount"), sa);
            }
        }

        if (hasTotalCMCLimit) {
            totalCMCLimit = AbilityUtils.calculateAmount(source, sa.getParam("WithTotalCMC"), sa);
        }

        if (controlledByPlayer != null) {
            controller.addController(controlledByTimeStamp, controlledByPlayer);
        }

        boolean singleOption = tgtCards.size() == 1 && amount == 1 && optional;
        Map<String, Object> params = hasTotalCMCLimit ? new HashMap<>() : null;

        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        moveParams.put(AbilityKey.LastStateBattlefield, sa.getLastStateBattlefield());
        moveParams.put(AbilityKey.LastStateGraveyard, sa.getLastStateGraveyard());

        while (!tgtCards.isEmpty() && amount > 0 && totalCMCLimit >= 0) {
            if (hasTotalCMCLimit) {
                // filter out cards with mana value greater than limit
                Iterator<Card> it = tgtCards.iterator();
                final String [] valid = {"Spell.cmcLE" + totalCMCLimit};
                while (it.hasNext()) {
                    Card c = it.next();
                    if (!Iterables.any(AbilityUtils.getBasicSpellsFromPlayEffect(c, controller), SpellAbilityPredicates.isValid(valid, controller , c, sa))) {
                        // it.remove will only remove item from the list part of CardCollection
                        tgtCards.asSet().remove(c);
                        it.remove();
                    }
                }
                if (tgtCards.isEmpty())
                    break;
                params.put("CMCLimit", totalCMCLimit);
            }

            controller.getController().tempShowCards(showCards);
            Card tgtCard = controller.getController().chooseSingleEntityForEffect(tgtCards, sa, Localizer.getInstance().getMessage("lblSelectCardToPlay"), !singleOption && optional, params);
            controller.getController().endTempShowCards();
            if (tgtCard == null) {
                break;
            }

            boolean wasFaceDown = false;
            if (tgtCard.isFaceDown()) {
                tgtCard.forceTurnFaceUp();
                wasFaceDown = true;
            }

            if (sa.hasParam("ShowCardToActivator")) {
                game.getAction().revealTo(tgtCard, controller);
            }
            String prompt = sa.hasParam("CastTransformed") ? "lblDoYouWantPlayCardTransformed" : "lblDoYouWantPlayCard";
            if (singleOption && !controller.getController().confirmAction(sa, null, Localizer.getInstance().getMessage(prompt, CardTranslation.getTranslatedName(tgtCard.getName())), tgtCard, null)) {
                if (wasFaceDown) {
                    tgtCard.turnFaceDownNoUpdate();
                    tgtCard.updateStateForView();
                }
                break;
            }

            if (!sa.hasParam("AllowRepeats")) {
                tgtCards.remove(tgtCard);
            }

            if (sa.hasParam("CopyCard")) {
                final Card original = tgtCard;
                final Zone zone = tgtCard.getZone();
                tgtCard = Card.fromPaperCard(tgtCard.getPaperCard(), controller);

                tgtCard.setToken(true);
                tgtCard.setZone(zone);
                // to fix the CMC
                tgtCard.setCopiedPermanent(original);
                tgtCard.setCopiedSpell(true);
                if (zone != null) {
                    zone.add(tgtCard);
                }
            }

            CardStateName state = CardStateName.Original;

            if (sa.hasParam("CastTransformed")) {
                if (!tgtCard.changeToState(CardStateName.Transformed)) {
                    // Failed to transform. In the future, we might need to just remove this option and continue
                    amount--;
                    System.err.println("CastTransformed failed for '" + tgtCard + "'.");
                    continue;
                }
                state = CardStateName.Transformed;
            }

            List<SpellAbility> sas = AbilityUtils.getSpellsFromPlayEffect(tgtCard, controller, state, !altCost);
            if (sa.hasParam("ValidSA")) {
                final String valid[] = sa.getParam("ValidSA").split(",");
                sas.removeIf(sp -> !sp.isValid(valid, controller , source, sa));
            }

            if (hasTotalCMCLimit) {
                Iterator<SpellAbility> it = sas.iterator();
                while (it.hasNext()) {
                    SpellAbility s = it.next();
                    if (s.getPayCosts().getTotalMana().getCMC() > totalCMCLimit)
                        it.remove();
                }
            }

            if (sas.isEmpty()) {
                continue;
            }

            SpellAbility tgtSA;

            if (sa.hasParam("CastFaceDown")) {
                // For Illusionary Mask effect
                tgtSA = CardFactoryUtil.abilityCastFaceDown(tgtCard.getCurrentState(), false, "Morph");
            } else {
                tgtSA = controller.getController().getAbilityToPlay(tgtCard, sas);
            }

            // in case player canceled from choice dialog
            if (tgtSA == null) {
                if (wasFaceDown) {
                    tgtCard.turnFaceDownNoUpdate();
                    tgtCard.updateStateForView();
                }
                continue;
            }

            final CardZoneTable triggerList = new CardZoneTable(game.getLastStateBattlefield(), game.getLastStateGraveyard());
            final Zone originZone = tgtCard.getZone();

            // lands will be played
            if (tgtSA instanceof LandAbility) {
                tgtSA.resolve();
                amount--;
                if (remember) {
                    source.addRemembered(tgtCard);
                }
                if (imprint) {
                    source.addImprintedCard(tgtCard);
                }
                //Forget only if playing was successful
                if (forget) {
                    source.removeRemembered(tgtCard);
                }

                final Zone currentZone = game.getCardState(tgtCard).getZone();
                if (!originZone.equals(currentZone)) {
                    triggerList.put(originZone.getZoneType(), currentZone.getZoneType(), game.getCardState(tgtCard));
                }
                triggerList.triggerChangesZoneAll(game, sa);

                continue;
            }

            final int tgtCMC = tgtSA.getPayCosts().getTotalMana().getCMC();

            // illegal action, cancel early
            if (altCost && tgtSA.costHasManaX() && tgtSA.getPayCosts().getCostMana().getXMin() > 0) {
                continue;
            }

            boolean unpayableCost = tgtSA.getPayCosts().getCostMana().getMana().isNoCost();
            if (sa.hasParam("WithoutManaCost")) {
                tgtSA = tgtSA.copyWithNoManaCost();
            } else if (sa.hasParam("PlayCost")) {
                Cost abCost;
                String cost = sa.getParam("PlayCost");
                if (cost.equals("ManaCost")) {
                    if (unpayableCost) {
                        continue;
                    }
                    abCost = new Cost(source.getManaCost(), false);
                } else if (cost.equals("SuspendCost")) {
                    abCost = Iterables.find(tgtCard.getNonManaAbilities(), s -> s.isKeyword(Keyword.SUSPEND)).getPayCosts();
                } else {
                    if (cost.contains("ConvertedManaCost")) {
                        if (unpayableCost) {
                            continue;
                        }
                        final String costcmc = Integer.toString(tgtCard.getCMC());
                        cost = cost.replace("ConvertedManaCost", costcmc);
                    }
                    abCost = new Cost(cost, false);
                }

                tgtSA = tgtSA.copyWithManaCostReplaced(tgtSA.getActivatingPlayer(), abCost);
            } else if (unpayableCost) {
                continue;
            }

            if (!optional) {
                // 118.8c
                for (CostPart cost : tgtSA.getPayCosts().getCostParts()) {
                    if ((cost instanceof CostDiscard || cost instanceof CostReveal)
                            && !cost.getType().equals("Card") && !cost.getType().equals("Random")) {
                        optional = true;
                        break;
                    }
                }
                if (!optional) {
                    tgtSA.getPayCosts().setMandatory(true);
                }
            }

            if (sa.hasParam("PlayReduceCost")) {
                // for Kefnet only can reduce colorless cost
                String reduce = sa.getParam("PlayReduceCost");
                tgtSA.putParam("ReduceCost", reduce);
                if (!StringUtils.isNumeric(reduce)) {
                    tgtSA.setSVar(reduce, sa.getSVar(reduce));
                }
            }
            if (sa.hasParam("PlayRaiseCost")) {
                String raise = sa.getParam("PlayRaiseCost");
                tgtSA.putParam("RaiseCost", raise);
            }

            if (sa.hasParam("Madness")) {
                tgtSA.setAlternativeCost(AlternativeCost.Madness);
            }

            if (sa.hasParam("CastTransformed")) {
                tgtSA.putParam("CastTransformed", "True");
            }

            if (sa.hasParam("ManaConversion")) {
                tgtSA.putParam("ManaConversion", sa.getParam("ManaConversion"));
            }

            if (tgtSA.usesTargeting() && !optional) {
                tgtSA.getTargetRestrictions().setMandatory(true);
            }

            // can't be done later
            if (sa.hasParam("ReplaceGraveyard")) {
                if (!sa.hasParam("ReplaceGraveyardValid")
                        || tgtSA.isValid(sa.getParam("ReplaceGraveyardValid").split(","), controller, source, sa)) {
                    addReplaceGraveyardEffect(tgtCard, sa, tgtSA, sa.getParam("ReplaceGraveyard"), moveParams);
                }
            }

            // For Illusionary Mask effect
            if (sa.hasParam("ReplaceIlluMask")) {
                addIllusionaryMaskReplace(tgtCard, sa, moveParams);
            }

            // Add controlled by player to target SA so when the spell is resolving, the controller would be changed again
            if (controlledByPlayer != null) {
                tgtSA.setControlledByPlayer(controlledByTimeStamp, controlledByPlayer);
                controller.pushPaidForSA(tgtSA);
                tgtSA.setManaCostBeingPaid(new ManaCostBeingPaid(tgtSA.getPayCosts().getCostMana().getManaCostFor(tgtSA)));
            }

            if (controller.getController().playSaFromPlayEffect(tgtSA)) {
                final Card played = tgtSA.getHostCard();
                if (remember) {
                    source.addRemembered(played);
                }
                if (imprint) {
                    source.addImprintedCard(played);
                }
                //Forgot only if playing was successful
                if (sa.hasParam("ForgetRemembered")) {
                    source.clearRemembered();
                }
                if (forget) {
                    source.removeRemembered(tgtCard);
                }

                final Zone currentZone = game.getCardState(tgtCard).getZone();
                if (!currentZone.equals(originZone)) {
                    //fix Garth One-Eye activated ability and the likes..
                    triggerList.put(originZone == null ? null : originZone.getZoneType(), currentZone.getZoneType(), game.getCardState(tgtCard));
                }
                triggerList.triggerChangesZoneAll(game, sa);
            }

            amount--;
            totalCMCLimit -= tgtCMC;
        }

        // Remove controlled by player if any
        if (controlledByPlayer != null) {
            controller.removeController(controlledByTimeStamp);
            controller.popPaidForSA();
        }
    }

    protected void addReplaceGraveyardEffect(Card c, SpellAbility sa, SpellAbility tgtSA, String zone, Map<AbilityKey, Object> moveParams) {
        final Card hostCard = sa.getHostCard();
        final Game game = hostCard.getGame();
        final Player controller = sa.getActivatingPlayer();
        final String name = hostCard.getName() + "'s Effect";
        final String image = hostCard.getImageKey();
        final Card eff = createEffect(sa, controller, name, image);

        eff.addRemembered(c);

        String repeffstr = "Event$ Moved | ValidCard$ Card.IsRemembered " +
        "| Origin$ Stack | Destination$ Graveyard " +
        "| Description$ If that card would be put into your graveyard this turn, exile it instead.";
        String effect = "DB$ ChangeZone | Defined$ ReplacedCard | Origin$ Stack | Destination$ " + zone;

        ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, eff, true);
        re.setLayer(ReplacementLayer.Other);

        re.setOverridingAbility(AbilityFactory.getAbility(effect, eff));
        eff.addReplacementEffect(re);

        addExileOnMovedTrigger(eff, "Stack");

        // Copy text changes
        if (sa.isIntrinsic()) {
            eff.copyChangedTextFrom(hostCard);
        }

        game.getEndOfTurn().addUntil(exileEffectCommand(game, eff));

        tgtSA.addRollbackEffect(eff);

        game.getAction().moveToCommand(eff, sa);
    }

    protected void addIllusionaryMaskReplace(Card c, SpellAbility sa, Map<AbilityKey, Object> moveParams) {
        final Card hostCard = sa.getHostCard();
        final Game game = hostCard.getGame();
        final Player controller = sa.getActivatingPlayer();
        final String name = hostCard + "'s Effect";
        final String image = hostCard.getImageKey();
        final Card eff = createEffect(sa, controller, name, image);

        eff.addRemembered(c);

        String [] repeffstrs = {
            "Event$ AssignDealDamage | ValidCard$ Card.IsRemembered+faceDown " +
            "| Description$ If the creature that spell becomes as it resolves has not been turned face up" +
            " and would assign or deal damage, be dealt damage, or become tapped, instead it's turned face up" +
            " and assigns or deals damage, is dealt damage, or becomes tapped.",
            "Event$ DealtDamage | ValidCard$ Card.IsRemembered+faceDown",
            "Event$ Tap | ValidCard$ Card.IsRemembered+faceDown"
        };
        String effect = "DB$ SetState | Defined$ ReplacedCard | Mode$ TurnFaceUp";

        for (final String repStr : repeffstrs) {
            ReplacementEffect re = ReplacementHandler.parseReplacement(repStr, eff, true);
            re.putParam("ReplacementResult", "Updated");
            re.setLayer(ReplacementLayer.Other);
            re.setOverridingAbility(AbilityFactory.getAbility(effect, eff));
            eff.addReplacementEffect(re);
        }

        addExileOnMovedTrigger(eff, "Battlefield");
        addExileOnCounteredTrigger(eff);

        game.getAction().moveToCommand(eff, sa);
    }
}
