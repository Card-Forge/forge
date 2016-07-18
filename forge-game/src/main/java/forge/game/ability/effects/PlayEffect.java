package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.StaticData;
import forge.card.CardRulesPredicates;
import forge.card.CardStateName;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.Lang;

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
        boolean useEncoded = false;
        int amount = 1;
        if (sa.hasParam("Amount") && !sa.getParam("Amount").equals("All")) {
            amount = AbilityUtils.calculateAmount(source, sa.getParam("Amount"), sa);
        }

        if (sa.hasParam("Controller")) {
            activator = AbilityUtils.getDefinedPlayers(source, sa.getParam("Controller"), sa).get(0);
        }

        final Player controller = activator;
        CardCollection tgtCards;

        if (sa.hasParam("Valid")) {
            ZoneType zone = ZoneType.Hand;
            if (sa.hasParam("ValidZone")) {
                zone = ZoneType.smartValueOf(sa.getParam("ValidZone"));
            }
            tgtCards = (CardCollection)AbilityUtils.filterListByType(game.getCardsIn(zone), sa.getParam("Valid"), sa);
        }
        else if (sa.hasParam("Encoded")) {
            final CardCollectionView encodedCards = source.getEncodedCards();
            final int encodedIndex = Integer.parseInt(sa.getParam("Encoded")) - 1;
            tgtCards = new CardCollection(encodedCards.get(encodedIndex));
            useEncoded = true;
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
                final List<PaperCard> copysource = new ArrayList<PaperCard>(cards);
                final CardCollection choice = new CardCollection();
                final String num = sa.hasParam("RandomNum") ? sa.getParam("RandomNum") : "1";
                int ncopied = AbilityUtils.calculateAmount(source, num, sa);
                while(ncopied > 0) {
                    final PaperCard cp = Aggregates.random(copysource);
                    final Card possibleCard = Card.fromPaperCard(cp, null);
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
                    tgtCards = (CardCollection)activator.getController().chooseCardsForEffect(choice, sa, source + " - Choose up to " + Lang.nounWithNumeral(choicenum, "card"), 0, choicenum, true);
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
        while (tgtCards.size() > saidNoTo.size() && amount > 0) {
            Card tgtCard = controller.getController().chooseSingleEntityForEffect(tgtCards, sa, "Select a card to play");
            if (tgtCard == null) {
                return;
            }

            final boolean wasFaceDown;
            if (tgtCard.isFaceDown()) {
                tgtCard.setState(CardStateName.Original, false);
                wasFaceDown = true;
            } else {
                wasFaceDown = false;
            }

            if (optional && !controller.getController().confirmAction(sa, null, String.format("Do you want to play %s?", tgtCard))) {
                if (wasFaceDown) {
                    tgtCard.setState(CardStateName.FaceDown, false);
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
            if (sa.hasParam("ForgetRemembered")) {
                source.clearRemembered();
            }

            final Card original = tgtCard;
            if (sa.hasParam("CopyCard")) {
                final Zone zone = tgtCard.getZone();
                tgtCard = Card.fromPaperCard(tgtCard.getPaperCard(), sa.getActivatingPlayer());

                tgtCard.setToken(true);
                tgtCard.setZone(zone);
                if (zone != null) {
                    zone.add(tgtCard);
                }

                if (useEncoded) {
                    tgtCard.setSVar("IsEncoded", "Number$1");
                }
            }

            if(sa.hasParam("SuspendCast")) {
                tgtCard.setSuspendCast(true);
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
            } else if (sa.hasParam("PlayMadness")) {
                Cost abCost;
                if ("ManaCost".equals(sa.getParam("PlayMadness"))) {
                    abCost = new Cost(source.getManaCost(), false);
                } else {
                    abCost = new Cost(sa.getParam("PlayMadness"), false);
                }

                tgtSA = tgtSA.copyWithDefinedCost(abCost);
                tgtSA.getHostCard().setMadness(true);
            }

            if (tgtSA.usesTargeting() && !optional) {
                tgtSA.getTargetRestrictions().setMandatory(true);
            }

            remember &= controller.getController().playSaFromPlayEffect(tgtSA);
            if (remember) {
                source.addRemembered(tgtSA.getHostCard());
            }

            amount--;
        }
    } // end resolve

}
