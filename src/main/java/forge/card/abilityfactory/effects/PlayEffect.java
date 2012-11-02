package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardCharacteristicName;
import forge.CardLists;
import forge.GameActionUtil;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostMana;
import forge.card.cost.CostPart;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityRestriction;
import forge.card.spellability.Target;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.item.CardDb;

public class PlayEffect extends SpellEffect { 
    @Override
    protected String getStackDescription(Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
    
        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard().getName()).append(" - ");
        } else {
            sb.append(" ");
        }
        sb.append("Play ");
        ArrayList<Card> tgtCards;
    
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }
    
        if (params.containsKey("Valid")) {
            sb.append("cards");
        } else {
            final Iterator<Card> it = tgtCards.iterator();
            while (it.hasNext()) {
                sb.append(it.next());
                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
        }
        if (params.containsKey("WithoutManaCost")) {
            sb.append(" without paying the mana cost");
        }
        sb.append(".");
        return sb.toString();
    }

    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card source = sa.getSourceCard();
        Player activator = sa.getActivatingPlayer();
        boolean optional = params.containsKey("Optional");
        boolean remember = params.containsKey("RememberPlayed");
        boolean wasFaceDown = false;
        int amount = 1;
        if (params.containsKey("Amount") && !params.get("Amount").equals("All")) {
            amount = AbilityFactory.calculateAmount(source, params.get("Amount"), sa);
        }

        if (params.containsKey("Controller")) {
            activator = AbilityFactory.getDefinedPlayers(source, params.get("Controller"), sa).get(0);
        }

        final Player controller = activator;
        List<Card> tgtCards = new ArrayList<Card>();

        final Target tgt = sa.getTarget();
        if (params.containsKey("Valid")) {
            ZoneType zone = ZoneType.Hand;
            if (params.containsKey("ValidZone")) {
                zone = ZoneType.smartValueOf(params.get("ValidZone"));
            }
            tgtCards = Singletons.getModel().getGame().getCardsIn(zone);
            tgtCards = AbilityFactory.filterListByType(tgtCards, params.get("Valid"), sa);
        } else if (params.containsKey("Defined")) {
            tgtCards = new ArrayList<Card>(AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa));
        } else if (tgt != null) {
            tgtCards = new ArrayList<Card>(tgt.getTargetCards());
        }

        if (tgtCards.isEmpty()) {
            return;
        }

        if (params.containsKey("Amount") && params.get("Amount").equals("All")) {
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
                            ArrayList<SpellAbility> spellAbilities = c.getBasicSpells();
                            ArrayList<SpellAbility> sas = new ArrayList<SpellAbility>();
                            for (SpellAbility s : spellAbilities) {
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
                    && !GameActionUtil.showYesNoDialog(source, sb.toString())) {
                // i--;  // This causes an infinite loop (ArsenalNut)
                if (wasFaceDown) {
                    tgtCard.setState(CardCharacteristicName.FaceDown);
                }
                continue;
            }
            if (params.containsKey("ForgetRemembered")) {
                source.clearRemembered();
            }
            if (params.containsKey("CopyCard")) {
                tgtCard = Singletons.getModel().getCardFactory().getCard(CardDb.instance().getCard(tgtCard), sa.getActivatingPlayer());
                // when copying something stolen:
                tgtCard.addController(sa.getActivatingPlayer());

                tgtCard.setToken(true);
                tgtCard.setCopiedSpell(true);
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
            ArrayList<SpellAbility> spellAbilities = tgtCard.getBasicSpells();
            ArrayList<SpellAbility> sas = new ArrayList<SpellAbility>();
            for (SpellAbility s : spellAbilities) {
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

            if (params.containsKey("WithoutManaCost")) {
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
                    newSA.setManaCost("");
                    newSA.setDescription(newSA.getDescription() + " (without paying its mana cost)");
                    Singletons.getModel().getGame().getAction().playSpellAbility(newSA);
                    if (remember) {
                        source.addRemembered(tgtSA.getSourceCard());
                    }
                } else {
                    if (tgtSA instanceof Spell) {
                        Spell spell = (Spell) tgtSA;
                        if (spell.canPlayFromEffectAI(!optional, true) || !optional) {
                            ComputerUtil.playSpellAbilityWithoutPayingManaCost(controller, tgtSA);
                            if (remember) {
                                source.addRemembered(tgtSA.getSourceCard());
                            }
                        }
                    }
                }
            } else {
                if (controller.isHuman()) {
                    Singletons.getModel().getGame().getAction().playSpellAbility(tgtSA);
                    if (remember) {
                        source.addRemembered(tgtSA.getSourceCard());
                    }
                } else {
                    if (tgtSA instanceof Spell) {
                        Spell spell = (Spell) tgtSA;
                        if (spell.canPlayFromEffectAI(!optional, false) || !optional) {
                            ComputerUtil.playStack(tgtSA, controller);
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