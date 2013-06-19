package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Card;
import forge.CardCharacteristicName;
import forge.CardLists;
import forge.card.CardDb;
import forge.card.CardRulesPredicates;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityRestriction;
import forge.game.Game;
import forge.game.ai.ComputerUtil;
import forge.game.ai.ComputerUtilCard;
import forge.game.player.HumanPlay;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.GuiDialog;
import forge.item.PaperCard;
import forge.util.Aggregates;

public class PlayEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
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
    public void resolve(SpellAbility sa) {
        final Card source = sa.getSourceCard();
        Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();
        boolean optional = sa.hasParam("Optional");
        boolean remember = sa.hasParam("RememberPlayed");
        boolean wasFaceDown = false;
        boolean useEncoded = false;
        int amount = 1;
        if (sa.hasParam("Amount") && !sa.getParam("Amount").equals("All")) {
            amount = AbilityUtils.calculateAmount(source, sa.getParam("Amount"), sa);
        }

        if (sa.hasParam("Controller")) {
            activator = AbilityUtils.getDefinedPlayers(source, sa.getParam("Controller"), sa).get(0);
        }

        final Player controller = activator;
        List<Card> tgtCards = new ArrayList<Card>();

        if (sa.hasParam("Valid")) {
            ZoneType zone = ZoneType.Hand;
            if (sa.hasParam("ValidZone")) {
                zone = ZoneType.smartValueOf(sa.getParam("ValidZone"));
            }
            tgtCards = game.getCardsIn(zone);
            tgtCards = AbilityUtils.filterListByType(tgtCards, sa.getParam("Valid"), sa);
        }
        else if (sa.hasParam("Encoded")) {
            final ArrayList<Card> encodedCards = source.getEncoded();
            final int encodedIndex = Integer.parseInt(sa.getParam("Encoded")) - 1;
            tgtCards.add(encodedCards.get(encodedIndex));
            useEncoded = true;
        }
        else if (sa.hasParam("AnySupportedCard")) {
            List<PaperCard> cards = Lists.newArrayList(CardDb.instance().getUniqueCards());
            String valid = sa.getParam("AnySupportedCard");
            if (StringUtils.containsIgnoreCase(valid, "sorcery")) {
                Predicate<PaperCard> cpp = Predicates.compose(CardRulesPredicates.Presets.IS_SORCERY, PaperCard.FN_GET_RULES);
                cards = Lists.newArrayList(Iterables.filter(cards, cpp));
            }
            if (StringUtils.containsIgnoreCase(valid, "instant")) {
                Predicate<PaperCard> cpp = Predicates.compose(CardRulesPredicates.Presets.IS_INSTANT, PaperCard.FN_GET_RULES);
                cards = Lists.newArrayList(Iterables.filter(cards, cpp));
            }
            if (sa.hasParam("RandomCopied")) {
                List<PaperCard> copysource = new ArrayList<PaperCard>(cards);
                List<Card> choice = new ArrayList<Card>();
                final String num = sa.hasParam("RandomNum") ? sa.getParam("RandomNum") : "1";
                int ncopied = AbilityUtils.calculateAmount(source, num, sa);
                while(ncopied > 0) {
                    final PaperCard cp = Aggregates.random(copysource);
                    if (cp.getMatchingForgeCard().isValid(valid, source.getController(), source)) {
                        choice.add(cp.getMatchingForgeCard());
                        copysource.remove(cp);
                        ncopied -= 1;
                    }
                }
                if (sa.hasParam("ChoiceNum")) {
                    final int choicenum = AbilityUtils.calculateAmount(source, sa.getParam("ChoiceNum"), sa);
                    List<Card> afterchoice = new ArrayList<Card>();
                    for (int i = 0; i < choicenum; i++) {
                        afterchoice.add(GuiChoose.oneOrNone(source + "- Choose a Card", choice));
                    }
                    tgtCards = afterchoice;
                } else {
                    tgtCards = choice;
                }

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

        for (int i = 0; i < amount; i++) {
            if (tgtCards.isEmpty()) {
                return;
            }
            Card tgtCard = tgtCards.get(0);
            if (tgtCards.size() > 1) {
                if (controller.isHuman()) {
                    tgtCard = GuiChoose.one("Select a card to play", tgtCards);
                } else {
                    // AI
                    tgtCards = CardLists.filter(tgtCards, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            for (SpellAbility s : c.getBasicSpells()) {
                                Spell spell = (Spell) s;
                                s.setActivatingPlayer(controller);
                                // timing restrictions still apply
                                if (s.getRestrictions().checkTimingRestrictions(c, s) && spell.canPlayFromEffectAI(false, true)) {
                                    return true;
                                }
                            }
                            return false;
                        }
                    });
                    tgtCard = ComputerUtilCard.getBestAI(tgtCards);
                    if (tgtCard == null) {
                        return;
                    }
                }
            }
            if (tgtCard.isFaceDown()) {
                tgtCard.setState(CardCharacteristicName.Original);
                wasFaceDown = true;
            }
            final StringBuilder sb = new StringBuilder();
            sb.append("Do you want to play " + tgtCard + "?");
            if (controller.isHuman() && optional && !GuiDialog.confirm(source, sb.toString())) {
                // i--;  // This causes an infinite loop (ArsenalNut)
                if (wasFaceDown) {
                    tgtCard.setState(CardCharacteristicName.FaceDown);
                }
                continue;
            }
            if (sa.hasParam("ForgetRemembered")) {
                source.clearRemembered();
            }
            if (sa.hasParam("CopyCard")) {
                tgtCard = CardDb.getCard(tgtCard).toForgeCard(sa.getActivatingPlayer());

                tgtCard.setToken(true);
                tgtCard.setCopiedSpell(true);

                if (useEncoded) {
                    tgtCard.setSVar("IsEncoded", "Number$1");
                }
            }
            if(sa.hasParam("SuspendCast")) {
                tgtCard.setSuspendCast(true);
            }
            // lands will be played
            if (tgtCard.isLand()) {
                if (controller.playLand(tgtCard, true) && remember) {
                    source.addRemembered(tgtCard);
                }
                tgtCards.remove(tgtCard);
                continue;
            }

            // get basic spells (no flashback, etc.)
            ArrayList<SpellAbility> sas = new ArrayList<SpellAbility>();
            for (SpellAbility s : tgtCard.getBasicSpells()) {
                final Spell newSA = (Spell) s.copy();
                newSA.setActivatingPlayer(controller);
                SpellAbilityRestriction res = new SpellAbilityRestriction();
                // timing restrictions still apply
                res.setPlayerTurn(s.getRestrictions().getPlayerTurn());
                res.setOpponentTurn(s.getRestrictions().getOpponentTurn());
                res.setPhases(s.getRestrictions().getPhases());
                res.setZone(null);
                newSA.setRestrictions(res);
                // timing restrictions still apply
                if (res.checkTimingRestrictions(tgtCard, newSA) && newSA.checkOtherRestrictions()) {
                    sas.add(newSA);
                }
            }
            if (sas.isEmpty()) {
                return;
            }
            tgtCards.remove(tgtCard);
            SpellAbility tgtSA = null;
            // only one mode can be used
            tgtSA = sa.getActivatingPlayer().getController().getAbilityToPlay(sas);
            boolean noManaCost = sa.hasParam("WithoutManaCost");
            tgtSA = noManaCost ? tgtSA.copyWithNoManaCost() : tgtSA;

            if (tgtSA.usesTargeting() && !optional) {
                tgtSA.getTargetRestrictions().setMandatory(true);
            }

            if (controller.isHuman()) {
                HumanPlay.playSpellAbility(activator, tgtSA);
            } else {
                if (tgtSA instanceof Spell) { // Isn't it ALWAYS a spell?
                    Spell spell = (Spell) tgtSA;
                    if (spell.canPlayFromEffectAI(!optional, noManaCost) || !optional) {
                        if (noManaCost) {
                            ComputerUtil.playSpellAbilityWithoutPayingManaCost(controller, tgtSA, game);
                        } else {
                            ComputerUtil.playStack(tgtSA, controller, game);
                        }
                    } else 
                        remember = false; // didn't play spell
                }
            }
            if (remember) {
                source.addRemembered(tgtSA.getSourceCard());
            }
       
        }
    } // end resolve

}
