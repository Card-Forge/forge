package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardCharacteristicName;
import forge.CardLists;
import forge.Singletons;
import forge.card.SpellManaCost;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostMana;
import forge.card.cost.CostPart;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityRestriction;
import forge.game.GameState;
import forge.game.ai.ComputerUtil;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.GuiDialog;
import forge.item.CardDb;

public class PlayEffect extends SpellEffect {
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
        final GameState game = Singletons.getModel().getGame();
        final Card source = sa.getSourceCard();
        Player activator = sa.getActivatingPlayer();
        boolean optional = sa.hasParam("Optional");
        boolean remember = sa.hasParam("RememberPlayed");
        boolean wasFaceDown = false;
        boolean useEncoded = false;
        int amount = 1;
        if (sa.hasParam("Amount") && !sa.getParam("Amount").equals("All")) {
            amount = AbilityFactory.calculateAmount(source, sa.getParam("Amount"), sa);
        }

        if (sa.hasParam("Controller")) {
            activator = AbilityFactory.getDefinedPlayers(source, sa.getParam("Controller"), sa).get(0);
        }

        final Player controller = activator;
        List<Card> tgtCards = new ArrayList<Card>();

        if (sa.hasParam("Valid")) {
            ZoneType zone = ZoneType.Hand;
            if (sa.hasParam("ValidZone")) {
                zone = ZoneType.smartValueOf(sa.getParam("ValidZone"));
            }
            tgtCards = game.getCardsIn(zone);
            tgtCards = AbilityFactory.filterListByType(tgtCards, sa.getParam("Valid"), sa);
        }
        else if (sa.hasParam("Encoded")) {
            final ArrayList<Card> encodedCards = source.getEncoded();
            final int encodedIndex = Integer.parseInt(sa.getParam("Encoded")) - 1;
            tgtCards.add(encodedCards.get(encodedIndex));
            useEncoded = true;
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
                            List<SpellAbility> sas = new ArrayList<SpellAbility>();
                            for (SpellAbility s : c.getBasicSpells()) {
                                Spell spell = (Spell) s;
                                s.setActivatingPlayer(controller);
                                SpellAbilityRestriction res = s.getRestrictions();
                                // timing restrictions still apply
                                if (res.checkTimingRestrictions(c, s) && spell.canPlayFromEffectAI(false, true)) {
                                    sas.add(s);
                                }
                            }
                            if (sas.isEmpty()) {
                                return false;
                            }
                            return true;
                        }
                    });
                    tgtCard = CardFactoryUtil.getBestAI(tgtCards);
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
            if (controller.isHuman() && optional
                    && !GuiDialog.confirm(source, sb.toString())) {
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
                tgtCard = Singletons.getModel().getCardFactory().getCard(CardDb.instance().getCard(tgtCard), sa.getActivatingPlayer());
                // when copying something stolen:
                tgtCard.addController(sa.getActivatingPlayer());

                tgtCard.setToken(true);
                tgtCard.setCopiedSpell(true);

                if (useEncoded) {
                    tgtCard.setSVar("IsEncoded", "Number$1");
                }
            }
            // lands will be played
            if (tgtCard.isLand()) {
                controller.playLand(tgtCard);
                if (remember && controller.canPlayLand()) {
                    source.addRemembered(tgtCard);
                }
                tgtCards.remove(tgtCard);
                continue;
            }

            // get basic spells (no flashback, etc.)
            ArrayList<SpellAbility> sas = new ArrayList<SpellAbility>();
            for (SpellAbility s : tgtCard.getBasicSpells()) {
                final SpellAbility newSA = s.copy();
                newSA.setActivatingPlayer(controller);
                SpellAbilityRestriction res = new SpellAbilityRestriction();
                // timing restrictions still apply
                res.setPlayerTurn(s.getRestrictions().getPlayerTurn());
                res.setOpponentTurn(s.getRestrictions().getOpponentTurn());
                res.setPhases(s.getRestrictions().getPhases());
                res.setZone(null);
                newSA.setRestrictions(res);
                // timing restrictions still apply
                if (res.checkTimingRestrictions(tgtCard, newSA)) {
                    sas.add(newSA);
                }
            }
            if (sas.isEmpty()) {
                return;
            }
            tgtCards.remove(tgtCard);
            SpellAbility tgtSA = null;
            // only one mode can be used
            if (sas.size() == 1) {
                tgtSA = sas.get(0);
            } else if (sa.getActivatingPlayer().isHuman()) {
                tgtSA = GuiChoose.one("Select a spell to cast", sas);
            } else {
                tgtSA = sas.get(0);
            }

            if (tgtSA.getTarget() != null && !optional) {
                tgtSA.getTarget().setMandatory(true);
            }

            if (sa.hasParam("WithoutManaCost")) {
                if (controller.isHuman()) {
                    final SpellAbility newSA = tgtSA.copy();
                    final Cost cost = new Cost(tgtCard, "", false);
                    if (newSA.getPayCosts() != null) {
                        for (final CostPart part : newSA.getPayCosts().getCostParts()) {
                            if (!(part instanceof CostMana)) {
                                cost.getCostParts().add(part);
                            }
                        }
                    }
                    newSA.setPayCosts(cost);
                    newSA.setManaCost(SpellManaCost.NO_COST);
                    newSA.setDescription(newSA.getDescription() + " (without paying its mana cost)");
                    game.getAction().playSpellAbility(newSA, activator);
                    if (remember) {
                        source.addRemembered(tgtSA.getSourceCard());
                    }
                } else {
                    if (tgtSA instanceof Spell) {
                        Spell spell = (Spell) tgtSA;
                        if (spell.canPlayFromEffectAI(!optional, true) || !optional) {
                            ComputerUtil.playSpellAbilityWithoutPayingManaCost((AIPlayer)controller, tgtSA, game);
                            if (remember) {
                                source.addRemembered(tgtSA.getSourceCard());
                            }
                        }
                    }
                }
            } else {
                if (controller.isHuman()) {
                    game.getAction().playSpellAbility(tgtSA, activator);
                    if (remember) {
                        source.addRemembered(tgtSA.getSourceCard());
                    }
                } else {
                    if (tgtSA instanceof Spell) {
                        Spell spell = (Spell) tgtSA;
                        if (spell.canPlayFromEffectAI(!optional, false) || !optional) {
                            ComputerUtil.playStack(tgtSA, controller, game);
                            if (remember) {
                                source.addRemembered(tgtSA.getSourceCard());
                            }
                        }
                    }
                }
            }
        }
    } // end resolve

}
