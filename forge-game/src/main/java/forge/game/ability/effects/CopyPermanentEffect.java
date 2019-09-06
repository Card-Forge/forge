package forge.game.ability.effects;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.StaticData;
import forge.card.CardRulesPredicates;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactory;
import forge.game.card.CardLists;
import forge.game.card.CardZoneTable;
import forge.game.card.token.TokenInfo;
import forge.game.combat.Combat;
import forge.game.event.GameEventCombatChanged;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.TextUtil;
import forge.util.collect.FCollectionView;
import forge.util.PredicateString.StringOp;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

public class CopyPermanentEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        if (sa.hasParam("Populate")) {
            return "Populate. (Create a token that's a copy of a creature token you control.)";
        }
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
        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = host.getGame();
        final List<String> pumpKeywords = Lists.newArrayList();

        final long timestamp = game.getNextTimestamp();

        if (sa.hasParam("Optional")) {
            if (!activator.getController().confirmAction(sa, null, "Copy this permanent?")) {
                return;
            }
        }

        if (sa.hasParam("PumpKeywords")) {
            pumpKeywords.addAll(Arrays.asList(sa.getParam("PumpKeywords").split(" & ")));
        }

        final int numCopies = sa.hasParam("NumCopies") ? AbilityUtils.calculateAmount(host,
                sa.getParam("NumCopies"), sa) : 1;

        Player controller = null;
        if (sa.hasParam("Controller")) {
            final FCollectionView<Player> defined = AbilityUtils.getDefinedPlayers(host, sa.getParam("Controller"), sa);
            if (!defined.isEmpty()) {
                controller = defined.getFirst();
            }
        }
        if (controller == null) {
            controller = activator;
        }

        List<Card> tgtCards = Lists.newArrayList();

        if (sa.hasParam("ValidSupportedCopy")) {
            List<PaperCard> cards = Lists.newArrayList(StaticData.instance().getCommonCards().getUniqueCards());
            String valid = sa.getParam("ValidSupportedCopy");
            if (valid.contains("X")) {
                valid = TextUtil.fastReplace(valid,
                        "X", Integer.toString(AbilityUtils.calculateAmount(host, "X", sa)));
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
                List<PaperCard> copysource = Lists.newArrayList(cards);
                List<Card> choice = Lists.newArrayList();
                final String num = sa.hasParam("RandomNum") ? sa.getParam("RandomNum") : "1";
                int ncopied = AbilityUtils.calculateAmount(host, num, sa);
                while(ncopied > 0) {
                    final PaperCard cp = Aggregates.random(copysource);
                    Card possibleCard = Card.fromPaperCard(cp, activator); // Need to temporarily set the Owner so the Game is set

                    if (possibleCard.isValid(valid, host.getController(), host, sa)) {
                        choice.add(possibleCard);
                        copysource.remove(cp);
                        ncopied -= 1;
                    }
                }
                tgtCards = choice;
            } else if (sa.hasParam("DefinedName")) {
                String name = sa.getParam("DefinedName");
                if (name.equals("NamedCard")) {
                    if (!host.getNamedCard().isEmpty()) {
                        name = host.getNamedCard();
                    }
                }

                Predicate<PaperCard> cpp = Predicates.compose(CardRulesPredicates.name(StringOp.EQUALS, name), PaperCard.FN_GET_RULES);
                cards = Lists.newArrayList(Iterables.filter(cards, cpp));

                if (!cards.isEmpty()) {
                    tgtCards.add(Card.fromPaperCard(cards.get(0), controller));
                }
            }
        } else if (sa.hasParam("Choices")) {
            Player chooser = activator;
            if (sa.hasParam("Chooser")) {
                final String choose = sa.getParam("Chooser");
                chooser = AbilityUtils.getDefinedPlayers(sa.getHostCard(), choose, sa).get(0);
            }

            CardCollectionView choices = game.getCardsIn(ZoneType.Battlefield);
            choices = CardLists.getValidCards(choices, sa.getParam("Choices"), activator, host);
            if (!choices.isEmpty()) {
                String title = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : "Choose a card ";

                Card choosen = chooser.getController().chooseSingleEntityForEffect(choices, sa, title, false);

                if (choosen != null) {
                    tgtCards.add(choosen);
                }
            }
        } else {
            tgtCards = getDefinedCardsOrTargeted(sa);
        }

        boolean useZoneTable = true;
        CardZoneTable triggerList = sa.getChangeZoneTable();
        if (triggerList == null) {
            triggerList = new CardZoneTable();
            useZoneTable = false;
        }
        if (sa.hasParam("ChangeZoneTable")) {
            sa.setChangeZoneTable(triggerList);
            useZoneTable = true;
        }

        for (final Card c : tgtCards) {
            // if it only targets player, it already got all needed cards from defined
            if (!sa.usesTargeting() || sa.getTargetRestrictions().canTgtPlayer() || c.canBeTargetedBy(sa)) {
                Card proto = getProtoType(sa, c);
                List <Card> token = TokenInfo.makeToken(proto, controller, true, numCopies);

                final List<Card> crds = Lists.newArrayListWithCapacity(token.size());

                for (final Card t : token) {
                    t.setCopiedPermanent(proto);

                    // Temporarily register triggers of an object created with CopyPermanent
                    //game.getTriggerHandler().registerActiveTrigger(copy, false);
                    final Card copyInPlay = game.getAction().moveToPlay(t, sa, null);

                    if (copyInPlay.getZone() != null) {
                        triggerList.put(ZoneType.None, copyInPlay.getZone().getZoneType(), copyInPlay);
                    }

                    // when copying something stolen:
                    //copyInPlay.setSetCode(c.getSetCode());

                    copyInPlay.setCloneOrigin(host);
                    if (!pumpKeywords.isEmpty()) {
                        copyInPlay.addChangedCardKeywords(pumpKeywords, Lists.newArrayList(), false, false, timestamp);
                    }
                    crds.add(copyInPlay);
                    if (sa.hasParam("RememberCopied")) {
                        host.addRemembered(copyInPlay);
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
                            defender = AbilityUtils.getDefinedPlayers(host, sa.getParam("CopyAttacking"), sa).get(0);
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
                        final Card attacker = Iterables.getFirst(AbilityUtils.getDefinedCards(host, sa.getParam("CopyBlocking"), sa), null);
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
                        CardCollectionView list = AbilityUtils.getDefinedCards(host, sa.getParam("AttachedTo"), sa);
                        if (list.isEmpty()) {
                            list = copyInPlay.getController().getGame().getCardsIn(ZoneType.Battlefield);
                            list = CardLists.getValidCards(list, sa.getParam("AttachedTo"), copyInPlay.getController(), copyInPlay);
                        }
                        if (!list.isEmpty()) {
                            Card attachedTo = activator.getController().chooseSingleEntityForEffect(list, sa, copyInPlay + " - Select a card to attach to.");

                            copyInPlay.attachToEntity(attachedTo);
                        } else {
                            continue;
                        }
                    }
                    // need to be done otherwise the token has no State in Details
                    copyInPlay.updateStateForView();
                }

                if (sa.hasParam("AtEOT")) {
                    registerDelayedTrigger(sa, sa.getParam("AtEOT"), crds);
                }
                if (sa.hasParam("ImprintCopied")) {
                    host.addImprintedCards(crds);
                }
            } // end canBeTargetedBy
        } // end foreach Card

        if (!useZoneTable) {
            triggerList.triggerChangesZoneAll(game);
            triggerList.clear();
        }
    } // end resolve


    private Card getProtoType(final SpellAbility sa, final Card original) {
        final Card host = sa.getHostCard();
        final Player newOwner = sa.getActivatingPlayer();
        int id = newOwner == null ? 0 : newOwner.getGame().nextCardId();
        final Card copy = new Card(id, original.getPaperCard(), host.getGame());
        copy.setOwner(newOwner);
        copy.setSetCode(original.getSetCode());

        if (sa.hasParam("Embalm")) {
            copy.setEmbalmed(true);
        }

        if (sa.hasParam("Eternalize")) {
            copy.setEternalized(true);
        }

        copy.setStates(CardFactory.getCloneStates(original, copy, sa));
        // force update the now set State
        copy.setState(copy.getCurrentStateName(), true, true);
        copy.setToken(true);

        if (sa.hasParam("AtEOTTrig")) {
            addSelfTrigger(sa, sa.getParam("AtEOTTrig"), copy);
        }

        return copy;
    }
}
