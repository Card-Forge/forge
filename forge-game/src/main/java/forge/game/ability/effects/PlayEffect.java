package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.GameCommand;
import forge.StaticData;
import forge.card.CardRulesPredicates;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardFactoryUtil;
import forge.game.cost.Cost;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.replacement.ReplacementLayer;
import forge.game.spellability.AlternativeCost;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityPredicates;
import forge.game.trigger.TriggerType;
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

        sb.append("Play ");
        final List<Card> tgtCards = getTargetCards(sa);

        if (sa.hasParam("Valid")) {
            sb.append("cards");
        } else {
            sb.append(StringUtils.join(tgtCards, ", "));
        }
        if (sa.hasParam("WithoutManaCost")) {
            sb.append(" without paying the mana cost");
        }
        sb.append(".");
        return sb.toString();
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card source = sa.getHostCard();
        Player activator = sa.getActivatingPlayer();
        Player controlledByPlayer = null;
        long controlledByTimeStamp = -1;
        final Game game = activator.getGame();
        final boolean optional = sa.hasParam("Optional");
        boolean remember = sa.hasParam("RememberPlayed");
        int amount = 1;
        if (sa.hasParam("Amount") && !sa.getParam("Amount").equals("All")) {
            amount = AbilityUtils.calculateAmount(source, sa.getParam("Amount"), sa);
        }

        if (sa.hasParam("Controller")) {
            activator = AbilityUtils.getDefinedPlayers(source, sa.getParam("Controller"), sa).get(0);
        }

        if (sa.hasParam("ControlledByPlayer")) {
            controlledByTimeStamp = game.getNextTimestamp();
            controlledByPlayer = AbilityUtils.getDefinedPlayers(source, sa.getParam("ControlledByPlayer"), sa).get(0);
        }

        final Player controller = activator;
        CardCollection tgtCards;
        CardCollection showCards = new CardCollection();

        if (sa.hasParam("Valid")) {
            List<ZoneType> zones = sa.hasParam("ValidZone") ? ZoneType.listValueOf(sa.getParam("ValidZone")) : ImmutableList.of(ZoneType.Hand);
            tgtCards = new CardCollection(
                AbilityUtils.filterListByType(game.getCardsIn(zones), sa.getParam("Valid"), sa)
            );
            if (sa.hasParam("ShowCards")) {
                showCards = new CardCollection(AbilityUtils.filterListByType(game.getCardsIn(zones), sa.getParam("ShowCards"), sa));
            }
        } else if (sa.hasParam("AnySupportedCard")) {
            final String valid = sa.getParam("AnySupportedCard");
            List<PaperCard> cards = null;
            if (valid.startsWith("Names:")){
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
                final String num = sa.hasParam("RandomNum") ? sa.getParam("RandomNum") : "1";
                int ncopied = AbilityUtils.calculateAmount(source, num, sa);
                for (PaperCard cp : Aggregates.random(cards, ncopied)) {
                    final Card possibleCard = Card.fromPaperCard(cp, sa.getActivatingPlayer());
                    // Need to temporarily set the Owner so the Game is set
                    possibleCard.setOwner(sa.getActivatingPlayer());
                    choice.add(possibleCard);
                }
                if (sa.hasParam("ChoiceNum")) {
                    System.err.println("Offering random spells to copy: " + choice.toString());
                    final int choicenum = AbilityUtils.calculateAmount(source, sa.getParam("ChoiceNum"), sa);
                    tgtCards = new CardCollection(
                        activator.getController().chooseCardsForEffect(choice, sa,
                            source + " - " + Localizer.getInstance().getMessage("lblChooseUpTo") + " " + Lang.nounWithNumeral(choicenum, "card"), 0, choicenum, true, null
                        )
                    );
                }
                else {
                    tgtCards = choice;
                }
                System.err.println("Copying random spell(s): " + tgtCards.toString());
            }
            else {
                return;
            }
        } else if (sa.hasParam("CopyFromChosenName")) {
            String name = source.getChosenName();
            if (name.trim().isEmpty()) return;
            Card card = Card.fromPaperCard(StaticData.instance().getCommonCards().getUniqueByName(name), controller);
            card.setToken(true);
            tgtCards = new CardCollection();
            tgtCards.add(card);
        } else {
            tgtCards = new CardCollection();
            // filter only cards that didn't changed zones
            for (Card c : getTargetCards(sa)) {
                Card gameCard = game.getCardState(c, null);
                if (c.equalsWithTimestamp(gameCard)) {
                    tgtCards.add(gameCard);
                }
            }
        }

        if (tgtCards.isEmpty()) {
            return;
        }

        if (sa.hasParam("ValidSA")) {
            final String valid[] = {sa.getParam("ValidSA")};
            List<Card> toRemove = Lists.newArrayList();
            for (Card c : tgtCards) {
                if (!Iterables.any(AbilityUtils.getBasicSpellsFromPlayEffect(c, controller), SpellAbilityPredicates.isValid(valid, controller , c, sa))) {
                    toRemove.add(c);
                }
            }
            tgtCards.removeAll(toRemove);
            if (tgtCards.isEmpty()) {
                return;
            }
        }

        if (sa.hasParam("Amount") && sa.getParam("Amount").equals("All")) {
            amount = tgtCards.size();
        }

        if (controlledByPlayer != null) {
            activator.addController(controlledByTimeStamp, controlledByPlayer);
        }

        boolean singleOption = tgtCards.size() == 1 && amount == 1 && optional;

        while (!tgtCards.isEmpty() && amount > 0) {
            activator.getController().tempShowCards(showCards);
            Card tgtCard = controller.getController().chooseSingleEntityForEffect(tgtCards, sa, Localizer.getInstance().getMessage("lblSelectCardToPlay"), !singleOption, null);
            activator.getController().endTempShowCards();
            if (tgtCard == null) {
                break;
            }

            boolean wasFaceDown = false;
            if (tgtCard.isFaceDown()) {
                tgtCard.forceTurnFaceUp();
                wasFaceDown = true;
            }

            if (sa.hasParam("ShowCardToActivator")) {
                game.getAction().revealTo(tgtCard, activator);
            }

            if (singleOption && !controller.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantPlayCard", CardTranslation.getTranslatedName(tgtCard.getName())))) {
                if (wasFaceDown) {
                    tgtCard.turnFaceDownNoUpdate();
                    tgtCard.updateStateForView();
                }
                break;
            }

            if (!sa.hasParam("AllowRepeats")) {
                tgtCards.remove(tgtCard);
            }

            final Card original = tgtCard;
            if (sa.hasParam("CopyCard")) {
                final Zone zone = tgtCard.getZone();
                tgtCard = Card.fromPaperCard(tgtCard.getPaperCard(), sa.getActivatingPlayer());

                tgtCard.setToken(true);
                tgtCard.setZone(zone);
                // to fix the CMC
                tgtCard.setCopiedPermanent(original);
                if (zone != null) {
                    zone.add(tgtCard);
                }
            }

            // lands will be played
            if (tgtCard.isLand()) {
                if (controller.playLand(tgtCard, true)) {
                    amount--;
                    if (remember) {
                        source.addRemembered(tgtCard);
                    }
                } else {
                    tgtCards.remove(tgtCard);
                }
                continue;
            }

            // get basic spells (no flashback, etc.)
            List<SpellAbility> sas = AbilityUtils.getBasicSpellsFromPlayEffect(tgtCard, controller);
            if (sa.hasParam("ValidSA")) {
                final String valid[] = {sa.getParam("ValidSA")};
                sas = Lists.newArrayList(Iterables.filter(sas, SpellAbilityPredicates.isValid(valid, controller , source, sa)));
            }

            if (sas.isEmpty()) {
                continue;
            }

            // play copied cards with linked abilities, e.g. Elite Arcanist
            if (sa.hasParam("CopyOnce")) {
                tgtCards.remove(original);
            }

            SpellAbility tgtSA;

            if (!sa.hasParam("CastFaceDown")) {
                // only one mode can be used
                tgtSA = sa.getActivatingPlayer().getController().getAbilityToPlay(tgtCard, sas);
            } else {
                // For Illusionary Mask effect
                tgtSA = CardFactoryUtil.abilityMorphDown(tgtCard.getCurrentState());
            }
            // in case player canceled from choice dialog
            if (tgtSA == null) {
                if (wasFaceDown) {
                    tgtCard.turnFaceDownNoUpdate();
                    tgtCard.updateStateForView();
                }
                continue;
            }

            if (sa.hasParam("WithoutManaCost")) {
                tgtSA = tgtSA.copyWithNoManaCost();
            } else if (sa.hasParam("PlayCost")) {
                Cost abCost;
                if ("ManaCost".equals(sa.getParam("PlayCost"))) {
                    abCost = new Cost(source.getManaCost(), false);
                } else {
                    abCost = new Cost(sa.getParam("PlayCost"), false);
                }

                tgtSA = tgtSA.copyWithDefinedCost(abCost);
            }

            if (sa.hasParam("PlayReduceCost")) {
                // for Kefnet only can reduce colorless cost
                String reduce = sa.getParam("PlayReduceCost");
                tgtSA.putParam("ReduceCost", reduce);
                if (!StringUtils.isNumeric(reduce)) {
                    tgtSA.setSVar(reduce, sa.getSVar(reduce));
                }
            }

            if (sa.hasParam("Madness")) {
                tgtSA.setAlternativeCost(AlternativeCost.Madness);
            }

            if (tgtSA.usesTargeting() && !optional) {
                tgtSA.getTargetRestrictions().setMandatory(true);
            }

            // can't be done later
            if (sa.hasParam("ReplaceGraveyard")) {
                addReplaceGraveyardEffect(tgtCard, sa, sa.getParam("ReplaceGraveyard"));
            }

            // For Illusionary Mask effect
            if (sa.hasParam("ReplaceIlluMask")) {
                addIllusionaryMaskReplace(tgtCard, sa);
            }

            tgtSA.setSVar("IsCastFromPlayEffect", "True");

            // Add controlled by player to target SA so when the spell is resolving, the controller would be changed again
            if (controlledByPlayer != null) {
                tgtSA.setControlledByPlayer(controlledByTimeStamp, controlledByPlayer);
                activator.pushPaidForSA(tgtSA);
                tgtSA.setManaCostBeingPaid(new ManaCostBeingPaid(tgtSA.getPayCosts().getCostMana().getManaCostFor(tgtSA), tgtSA.getPayCosts().getCostMana().getRestiction()));
            }

            if (controller.getController().playSaFromPlayEffect(tgtSA)) {
                if (remember) {
                    source.addRemembered(tgtSA.getHostCard());
                }

                //Forgot only if playing was successful
                if (sa.hasParam("ForgetRemembered")) {
                    source.clearRemembered();
                }

                if (sa.hasParam("ForgetTargetRemembered")) {
                    source.removeRemembered(tgtCard);
                }
            }

            amount--;
        }

        // Remove controlled by player if any
        if (controlledByPlayer != null) {
            activator.removeController(controlledByTimeStamp);
            activator.popPaidForSA();
        }
    } // end resolve


    protected void addReplaceGraveyardEffect(Card c, SpellAbility sa, String zone) {
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

        final GameCommand endEffect = new GameCommand() {
            private static final long serialVersionUID = -5861759814760561373L;

            @Override
            public void run() {
                game.getAction().exile(eff, null);
            }
        };

        game.getEndOfTurn().addUntil(endEffect);

        eff.updateStateForView();

        // TODO: Add targeting to the effect so it knows who it's dealing with
        game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        game.getAction().moveTo(ZoneType.Command, eff, sa);
        game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
    }

    protected void addIllusionaryMaskReplace(Card c, SpellAbility sa) {
        final Card hostCard = sa.getHostCard();
        final Game game = hostCard.getGame();
        final Player controller = sa.getActivatingPlayer();
        final String name = hostCard.getName() + "'s Effect";
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
        String effect = "DB$ SetState | Defined$ ReplacedCard | Mode$ TurnFace";

        for (int i = 0; i < 3; ++i) {
            ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstrs[i], eff, true);
            re.setLayer(ReplacementLayer.Other);
            re.setOverridingAbility(AbilityFactory.getAbility(effect, eff));
            eff.addReplacementEffect(re);
        }

        addExileOnMovedTrigger(eff, "Battlefield");
        addExileOnCounteredTrigger(eff);

        eff.updateStateForView();

        game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        game.getAction().moveTo(ZoneType.Command, eff, sa);
        game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
    }
}
