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
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.replacement.ReplacementLayer;
import forge.game.spellability.AlternativeCost;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.CardTranslation;

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

        final Player controller = activator;
        CardCollection tgtCards;
        CardCollection showCards = new CardCollection();

        if (sa.hasParam("Valid")) {
            List<ZoneType> zones = sa.hasParam("ValidZone") ? ZoneType.listValueOf(sa.getParam("ValidZone")) : ImmutableList.of(ZoneType.Hand);
            tgtCards = new CardCollection(
                AbilityUtils.filterListByType(game.getCardsIn(zones), sa.getParam("Valid"), sa)
            );
            if ( sa.hasParam("ShowCards") ) {
                showCards = new CardCollection(AbilityUtils.filterListByType(game.getCardsIn(zones), sa.getParam("ShowCards"), sa));
            }
        }
        else if (sa.hasParam("AnySupportedCard")) {
            List<PaperCard> cards = Lists.newArrayList(StaticData.instance().getCommonCards().getUniqueCards());
            final String valid = sa.getParam("AnySupportedCard");
            if (StringUtils.containsIgnoreCase(valid, "sorcery")) {
                final Predicate<PaperCard> cpp = Predicates.compose(CardRulesPredicates.Presets.IS_SORCERY, PaperCard.FN_GET_RULES);
                cards = Lists.newArrayList(Iterables.filter(cards, cpp));
            }
            if (StringUtils.containsIgnoreCase(valid, "instant")) {
                final Predicate<PaperCard> cpp = Predicates.compose(CardRulesPredicates.Presets.IS_INSTANT, PaperCard.FN_GET_RULES);
                cards = Lists.newArrayList(Iterables.filter(cards, cpp));
            }
            if (sa.hasParam("RandomCopied")) {
                final List<PaperCard> copysource = new ArrayList<>(cards);
                final CardCollection choice = new CardCollection();
                final String num = sa.hasParam("RandomNum") ? sa.getParam("RandomNum") : "1";
                int ncopied = AbilityUtils.calculateAmount(source, num, sa);
                while(ncopied > 0) {
                    final PaperCard cp = Aggregates.random(copysource);
                    final Card possibleCard = Card.fromPaperCard(cp, sa.getActivatingPlayer());
                    // Need to temporarily set the Owner so the Game is set
                    possibleCard.setOwner(sa.getActivatingPlayer());

                    if (possibleCard.isValid(valid, source.getController(), source, sa)) {
                        choice.add(possibleCard);
                        copysource.remove(cp);
                        ncopied -= 1;
                    }
                }
                if (sa.hasParam("ChoiceNum")) {
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
            }
            else {
                return;
            }
        }
        else {
            tgtCards = getTargetCards(sa);
        }

        if (tgtCards.isEmpty()) {
            return;
        }

        if (sa.hasParam("Amount") && sa.getParam("Amount").equals("All")) {
            amount = tgtCards.size();
        }

        final CardCollection saidNoTo = new CardCollection();
        while (tgtCards.size() > saidNoTo.size() && saidNoTo.size() < amount && amount > 0) {
            activator.getController().tempShowCards(showCards);
            Card tgtCard = controller.getController().chooseSingleEntityForEffect(tgtCards, sa, Localizer.getInstance().getMessage("lblSelectCardToPlay"), null);
            activator.getController().endTempShowCards();
            if (tgtCard == null) {
                return;
            }

            final boolean wasFaceDown;
            if (tgtCard.isFaceDown()) {
                tgtCard.forceTurnFaceUp();
                wasFaceDown = true;
            } else {
                wasFaceDown = false;
            }

            if (sa.hasParam("ShowCardToActivator")) {
                game.getAction().revealTo(tgtCard, activator);
            }

            if (optional && !controller.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantPlayCard", CardTranslation.getTranslatedName(tgtCard.getName())))) {
                if (wasFaceDown) {
                    tgtCard.turnFaceDownNoUpdate();
                }
                saidNoTo.add(tgtCard);
                continue;
            }

            if (!sa.hasParam("AllowRepeats")) {
                tgtCards.remove(tgtCard);
            }

            if (wasFaceDown) {
                tgtCard.updateStateForView();
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
                    saidNoTo.add(tgtCard);
                }
                continue;
            }

            // get basic spells (no flashback, etc.)
            final List<SpellAbility> sas = AbilityUtils.getBasicSpellsFromPlayEffect(tgtCard, controller);
            if (sas.isEmpty()) {
                continue;
            }

            // play copied cards with linked abilities, e.g. Elite Arcanist
            if (sa.hasParam("CopyOnce")) {
                tgtCards.remove(original);
            }

            // only one mode can be used
            SpellAbility tgtSA = sa.getActivatingPlayer().getController().getAbilityToPlay(tgtCard, sas);
            final boolean noManaCost = sa.hasParam("WithoutManaCost");
            if (noManaCost) {
                tgtSA = tgtSA.copyWithNoManaCost();
                // FIXME: a hack to get cards like Detonate only allow legal targets when cast without paying mana cost (with X=0).
                if (tgtSA.hasParam("ValidTgtsWithoutManaCost")) {
                    tgtSA.getTargetRestrictions().changeValidTargets(tgtSA.getParam("ValidTgtsWithoutManaCost").split(","));
                }
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
                tgtSA.getMapParams().put("ReduceCost", reduce);
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
            
            tgtSA.setSVar("IsCastFromPlayEffect", "True");

            if (controller.getController().playSaFromPlayEffect(tgtSA)) {
                if (remember) {
                    source.addRemembered(tgtSA.getHostCard());
                }

                //Forgot only of playing was successful
                if (sa.hasParam("ForgetRemembered")) {
                    source.clearRemembered();
                }

                if (sa.hasParam("ForgetTargetRemembered")) {
                    source.removeRemembered(tgtCard);
                }
            }

            amount--;
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
}
