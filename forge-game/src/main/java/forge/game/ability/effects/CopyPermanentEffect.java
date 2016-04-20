package forge.game.ability.effects;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.StaticData;
import forge.card.CardRulesPredicates;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactory;
import forge.game.card.CardLists;
import forge.game.combat.Combat;
import forge.game.event.GameEventCombatChanged;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.collect.FCollectionView;
import forge.util.PredicateString.StringOp;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CopyPermanentEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();


        final List<Card> tgtCards = getTargetCards(sa);

        sb.append("Copy ");
        sb.append(StringUtils.join(tgtCards, ", "));
        sb.append(".");
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(final SpellAbility sa) {
        final Card hostCard = sa.getHostCard();
        final Game game = hostCard.getGame();
        final List<String> keywords = new ArrayList<String>();
        final List<String> types = new ArrayList<String>();
        final List<String> svars = new ArrayList<String>();
        final List<String> triggers = new ArrayList<String>();
        if (sa.hasParam("Optional")) {
            if (!sa.getActivatingPlayer().getController().confirmAction(sa, null, "Copy this permanent?")) {
                return;
            }
        }
        if (sa.hasParam("Keywords")) {
            keywords.addAll(Arrays.asList(sa.getParam("Keywords").split(" & ")));
        }
        if (sa.hasParam("AddTypes")) {
            types.addAll(Arrays.asList(sa.getParam("AddTypes").split(" & ")));
        }
        if (sa.hasParam("AddSVars")) {
            svars.addAll(Arrays.asList(sa.getParam("AddSVars").split(" & ")));
        }
        if (sa.hasParam("Triggers")) {
            triggers.addAll(Arrays.asList(sa.getParam("Triggers").split(" & ")));
        }
        final int numCopies = sa.hasParam("NumCopies") ? AbilityUtils.calculateAmount(hostCard,
                sa.getParam("NumCopies"), sa) : 1;

        Player controller = null;
        if (sa.hasParam("Controller")) {
            final FCollectionView<Player> defined = AbilityUtils.getDefinedPlayers(hostCard, sa.getParam("Controller"), sa);
            if (!defined.isEmpty()) {
                controller = defined.getFirst();
            }
        }
        if (controller == null) {
            controller = sa.getActivatingPlayer();
        }

        List<Card> tgtCards = getTargetCards(sa);
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        if (sa.hasParam("ValidSupportedCopy")) {
            List<PaperCard> cards = Lists.newArrayList(StaticData.instance().getCommonCards().getUniqueCards());
            String valid = sa.getParam("ValidSupportedCopy");
            if (valid.contains("X")) {
                valid = valid.replace("X", Integer.toString(AbilityUtils.calculateAmount(hostCard, "X", sa)));
            }
            if (StringUtils.containsIgnoreCase(valid, "creature")) {
                Predicate<PaperCard> cpp = Predicates.compose(CardRulesPredicates.Presets.IS_CREATURE, PaperCard.FN_GET_RULES);
                cards = Lists.newArrayList(Iterables.filter(cards, cpp));
            }
            if (StringUtils.containsIgnoreCase(valid, "equipment")) {
                Predicate<PaperCard> cpp = Predicates.compose(CardRulesPredicates.Presets.IS_EQUIPMENT, PaperCard.FN_GET_RULES);
                cards = Lists.newArrayList(Iterables.filter(cards, cpp));
            }
            if (sa.hasParam("RandomCopied")) {
                List<PaperCard> copysource = new ArrayList<PaperCard>(cards);
                List<Card> choice = new ArrayList<Card>();
                final String num = sa.hasParam("RandomNum") ? sa.getParam("RandomNum") : "1";
                int ncopied = AbilityUtils.calculateAmount(hostCard, num, sa);
                while(ncopied > 0) {
                    final PaperCard cp = Aggregates.random(copysource);
                    Card possibleCard = Card.fromPaperCard(cp, sa.getActivatingPlayer()); // Need to temporarily set the Owner so the Game is set

                    if (possibleCard.isValid(valid, hostCard.getController(), hostCard, sa)) {
                        choice.add(possibleCard);
                        copysource.remove(cp);
                        ncopied -= 1;
                    }
                }
                tgtCards = choice;
            } else if (sa.hasParam("DefinedName")) {
                String name = sa.getParam("DefinedName");
                if (name.equals("NamedCard")) {
                    if (!hostCard.getNamedCard().isEmpty()) {
                        name = hostCard.getNamedCard();
                    }
                }

                Predicate<PaperCard> cpp = Predicates.compose(CardRulesPredicates.name(StringOp.EQUALS, name), PaperCard.FN_GET_RULES);
                cards = Lists.newArrayList(Iterables.filter(cards, cpp));

                tgtCards.clear();
                if (!cards.isEmpty()) {
                    tgtCards.add(Card.fromPaperCard(cards.get(0), controller));
                }
            }
        }
        hostCard.clearClones();

        for (final Card c : tgtCards) {
            if ((tgt == null) || c.canBeTargetedBy(sa)) {

                int multiplier = numCopies * hostCard.getController().getTokenDoublersMagnitude();
                final List<Card> crds = new ArrayList<Card>(multiplier);

                for (int i = 0; i < multiplier; i++) {
                    final Card copy = CardFactory.copyCopiableCharacteristics(c, sa.getActivatingPlayer());
                    copy.setToken(true);
                    copy.setCopiedPermanent(c);
                    CardFactory.copyCopiableAbilities(c, copy);
                    // add keywords from sa
                    for (final String kw : keywords) {
                        copy.addIntrinsicKeyword(kw);
                    }
                    for (final String type : types) {
                        copy.addType(type);
                    }
                    for (final String svar : svars) {
                        String actualsVar = hostCard.getSVar(svar);
                        String name = svar;
                        if (actualsVar.startsWith("SVar:")) {
                            actualsVar = actualsVar.split("SVar:")[1];
                            name = actualsVar.split(":")[0];
                            actualsVar = actualsVar.split(":")[1];
                        }
                        copy.setSVar(name, actualsVar);
                    }
                    for (final String s : triggers) {
                        final String actualTrigger = hostCard.getSVar(s);
                        final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, copy, true);
                        copy.addTrigger(parsedTrigger);
                    }

                    // Temporarily register triggers of an object created with CopyPermanent
                    //game.getTriggerHandler().registerActiveTrigger(copy, false);
                    final Card copyInPlay = game.getAction().moveToPlay(copy);

                    // when copying something stolen:
                    copyInPlay.setController(controller, 0);
                    copyInPlay.setSetCode(c.getSetCode());

                    copyInPlay.setCloneOrigin(hostCard);
                    sa.getHostCard().addClone(copyInPlay);
                    crds.add(copyInPlay);
                    if (sa.hasParam("RememberCopied")) {
                        hostCard.addRemembered(copyInPlay);
                    }
                    if (sa.hasParam("Tapped")) {
                        copyInPlay.setTapped(true);
                    }
                    if (sa.hasParam("CopyAttacking") && game.getPhaseHandler().inCombat()) {
                        final String attacked = sa.getParam("CopyAttacking");
                        GameEntity defender;
                        if ("True".equals(attacked)) {
                            FCollectionView<GameEntity> defs = game.getCombat().getDefenders();
                            defender = c.getController().getController().chooseSingleEntityForEffect(defs, sa, "Choose which defender to attack with " + c, false);
                        } else {
                            defender = AbilityUtils.getDefinedPlayers(hostCard, sa.getParam("CopyAttacking"), sa).get(0);
                            if (sa.hasParam("ChoosePlayerOrPlaneswalker") && defender != null) {
                                FCollectionView<GameEntity> defs = game.getCombat().getDefendersControlledBy((Player) defender);
                                defender = c.getController().getController().chooseSingleEntityForEffect(defs, sa, "Choose which defender to attack with " + c + " {defender: "+ defender + "}", false);
                            }
                        }
                        game.getCombat().addAttacker(copyInPlay, defender);
                        game.fireEvent(new GameEventCombatChanged());
                    }

                    if (sa.hasParam("CopyBlocking") && game.getPhaseHandler().inCombat() && copyInPlay.isCreature()) {
                        final Combat combat = game.getPhaseHandler().getCombat();
                        final Card attacker = Iterables.getFirst(AbilityUtils.getDefinedCards(hostCard, sa.getParam("CopyBlocking"), sa), null);
                        if (attacker != null) {
                            final boolean wasBlocked = combat.isBlocked(attacker);
                            combat.addBlocker(attacker, copyInPlay);
                            combat.orderAttackersForDamageAssignment(copyInPlay);

                            // Add it to damage assignment order
                            if (!wasBlocked) {
                                combat.setBlocked(attacker, true);
                                combat.addBlockerToDamageAssignmentOrder(attacker, copyInPlay);
                            }

                            game.fireEvent(new GameEventCombatChanged());
                        }
                    }

                    if (sa.hasParam("AttachedTo")) {
                        CardCollectionView list = AbilityUtils.getDefinedCards(hostCard, sa.getParam("AttachedTo"), sa);
                        if (list.isEmpty()) {
                            list = copyInPlay.getController().getGame().getCardsIn(ZoneType.Battlefield);
                            list = CardLists.getValidCards(list, sa.getParam("AttachedTo"), copyInPlay.getController(), copyInPlay);
                        }
                        if (!list.isEmpty()) {
                            Card attachedTo = sa.getActivatingPlayer().getController().chooseSingleEntityForEffect(list, sa, copyInPlay + " - Select a card to attach to.");
                            if (copyInPlay.isAura()) {
                                if (attachedTo.canBeEnchantedBy(copyInPlay)) {
                                    copyInPlay.enchantEntity(attachedTo);
                                } else {//can't enchant
                                    continue;
                                }
                            } else if (copyInPlay.isEquipment()) { //Equipment
                                if (attachedTo.canBeEquippedBy(copyInPlay)) {
                                    copyInPlay.equipCard(attachedTo);
                                } else {
                                    continue;
                                }
                            } else { // Fortification
                                copyInPlay.fortifyCard(attachedTo);
                            }
                        } else {
                            continue;
                        }
                    }

                }
                
                if (sa.hasParam("AtEOT")) {
                    final String location = sa.getParam("AtEOT");
                    registerDelayedTrigger(sa, location, crds);
                }
                if (sa.hasParam("ImprintCopied")) {
                    hostCard.addImprintedCards(crds);
                }
            } // end canBeTargetedBy
        } // end foreach Card
    } // end resolve

    private static void registerDelayedTrigger(final SpellAbility sa, final String location, final List<Card> crds) {
        String delTrig = "Mode$ Phase | Phase$ End Of Turn | TriggerDescription$ "
                + location + " " + crds + " at the beginning of the next end step.";
        final Trigger trig = TriggerHandler.parseTrigger(delTrig, sa.getHostCard(), true);
        for (final Card c : crds) {
            trig.addRemembered(c);
        }
        String trigSA = "";
        if (location.equals("Sacrifice")) {
            trigSA = "DB$ SacrificeAll | Defined$ DelayTriggerRemembered | Controller$ You";
        } else if (location.equals("Exile")) {
            trigSA = "DB$ ChangeZone | Defined$ DelayTriggerRemembered | Origin$ Battlefield | Destination$ Exile";
        }
        trig.setOverridingAbility(AbilityFactory.getAbility(trigSA, sa.getHostCard()));
        sa.getActivatingPlayer().getGame().getTriggerHandler().registerDelayedTrigger(trig);
    }

}
