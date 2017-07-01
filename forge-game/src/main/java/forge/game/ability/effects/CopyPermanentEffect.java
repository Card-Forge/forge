package forge.game.ability.effects;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.ImageKeys;
import forge.StaticData;
import forge.card.CardRulesPredicates;
import forge.card.CardType;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactory;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
import forge.game.combat.Combat;
import forge.game.event.GameEventCombatChanged;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.collect.FCollectionView;
import forge.util.PredicateString.StringOp;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
        final List<String> keywords = Lists.newArrayList();
        final List<String> types = Lists.newArrayList();
        final List<String> svars = Lists.newArrayList();
        final List<String> triggers = Lists.newArrayList();
        final List<String> pumpKeywords = Lists.newArrayList();

        final long timestamp = game.getNextTimestamp();

        if (sa.hasParam("Optional")) {
            if (!sa.getActivatingPlayer().getController().confirmAction(sa, null, "Copy this permanent?")) {
                return;
            }
        }
        if (sa.hasParam("Keywords")) {
            keywords.addAll(Arrays.asList(sa.getParam("Keywords").split(" & ")));
        }
        if (sa.hasParam("PumpKeywords")) {
            pumpKeywords.addAll(Arrays.asList(sa.getParam("PumpKeywords").split(" & ")));
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
                List<PaperCard> copysource = Lists.newArrayList(cards);
                List<Card> choice = Lists.newArrayList();
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
            if (!sa.usesTargeting() || c.canBeTargetedBy(sa)) {

                int multiplier = numCopies;
                
                final Map<String, Object> repParams = Maps.newHashMap();
                repParams.put("Event", "CreateToken");
                repParams.put("Affected", controller);
                repParams.put("TokenNum", multiplier);
                repParams.put("EffectOnly", true);

                switch (game.getReplacementHandler().run(repParams)) {
                case NotReplaced:
                    break;
                case Updated: {
                    multiplier = (int) repParams.get("TokenNum");
                    break;
                }
                default:
                    return ;
                }
                
                final List<Card> crds = Lists.newArrayListWithCapacity(multiplier);

                for (int i = 0; i < multiplier; i++) {
                    final Card copy = CardFactory.copyCopiableCharacteristics(c, sa.getActivatingPlayer());
                    copy.setToken(true);
                    copy.setCopiedPermanent(c);
                    CardFactory.copyCopiableAbilities(c, copy);
                    // add keywords from sa
                    for (final String kw : keywords) {
                        copy.addIntrinsicKeyword(kw);
                    }
                    if (sa.hasParam("SetCreatureTypes")) {
                        String typeLine = "";
                        for (CardType.Supertype st : copy.getType().getSupertypes()) {
                            typeLine += st.name() + " ";
                        }
                        for (CardType.CoreType ct : copy.getType().getCoreTypes()) {
                            typeLine += ct.name() + " ";
                        }

                        StringBuilder newType = new StringBuilder(typeLine);
                        newType.append(sa.getParam("SetCreatureTypes"));

                        copy.setType(CardType.parse(newType.toString()));
                    }

                    if (sa.hasParam("SetColor")) {
                        copy.setColor(MagicColor.fromName(sa.getParam("SetColor")));
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

                    // set power of clone
                    if (sa.hasParam("SetPower")) {
                        String rhs = sa.getParam("SetPower");
                        int power = Integer.MAX_VALUE;
                        try {
                            power = Integer.parseInt(rhs);
                        } catch (final NumberFormatException e) {
                            power = CardFactoryUtil.xCount(copy, copy.getSVar(rhs));
                        }
                        copy.setBasePower(power);
                    }

                    // set toughness of clone
                    if (sa.hasParam("SetToughness")) {
                        String rhs = sa.getParam("SetToughness");
                        int toughness = Integer.MAX_VALUE;
                        try {
                            toughness = Integer.parseInt(rhs);
                        } catch (final NumberFormatException e) {
                            toughness = CardFactoryUtil.xCount(copy, copy.getSVar(rhs));
                        }
                        copy.setBaseToughness(toughness);
                    }

                    if (sa.hasParam("AtEOTTrig")) {
                        addSelfTrigger(sa, sa.getParam("AtEOTTrig"), copy);
                    }
                    
                    if (sa.hasParam("Embalm")) {
                        copy.addType("Zombie");
                        copy.setColor(MagicColor.WHITE);
                        copy.setManaCost(ManaCost.NO_COST);
                        copy.setEmbalmed(true);

                        String name = copy.getName().replace(",", "").replace(" ", "_").toLowerCase();
                        copy.setImageKey(ImageKeys.getTokenKey("embalm_" + name));
                    }
                    if (sa.hasParam("Eternalize")) {
                    	copy.addType("Zombie");
                    	copy.setColor(MagicColor.BLACK);
                    	copy.setManaCost(ManaCost.NO_COST);
                    	copy.setBasePower(4);
                    	copy.setBaseToughness(4);
                        copy.setEternalized(true);

                        String name = copy.getName().replace(",", "").replace(" ", "_").toLowerCase();
                        copy.setImageKey(ImageKeys.getTokenKey("eternalize_" + name));
                    }
                    
                    // remove some characteristic static abilties
                    for (StaticAbility sta : copy.getStaticAbilities()) {
                        if (!sta.hasParam("CharacteristicDefining")) {
                            continue;
                        }
                        if (sa.hasParam("SetPower") || sa.hasParam("Eternalize")) {
                            if (sta.hasParam("SetPower"))
                                copy.removeStaticAbility(sta);
                        }
                        if (sa.hasParam("SetToughness") || sa.hasParam("Eternalize")) {
                            if (sta.hasParam("SetToughness"))
                                copy.removeStaticAbility(sta);
                        }
                        if (sa.hasParam("SetCreatureTypes")) {
                            // currently only Changeling and similar should be affected by that
                            // other cards using AddType$ ChosenType should not
                            if (sta.hasParam("AddType") && "AllCreatureTypes".equals(sta.getParam("AddType"))) {
                                copy.removeStaticAbility(sta);
                            }
                        }
                        if (sa.hasParam("SetColor") || sa.hasParam("Embalm") || sa.hasParam("Eternalize")) {
                            if (sta.hasParam("SetColor")) {
                                copy.removeStaticAbility(sta);
                            }
                        }
                    }
                    if (sa.hasParam("SetCreatureTypes")) {
                        copy.removeIntrinsicKeyword("Changeling");
                    }
                    if (sa.hasParam("SetColor") || sa.hasParam("Embalm") || sa.hasParam("Eternalize")) {
                        copy.removeIntrinsicKeyword("Devoid");
                    }

                    copy.updateStateForView();

                    // Temporarily register triggers of an object created with CopyPermanent
                    //game.getTriggerHandler().registerActiveTrigger(copy, false);
                    final Card copyInPlay = game.getAction().moveToPlay(copy, sa);

                    // when copying something stolen:
                    copyInPlay.setController(controller, 0);
                    copyInPlay.setSetCode(c.getSetCode());

                    copyInPlay.setCloneOrigin(hostCard);
                    sa.getHostCard().addClone(copyInPlay);
                    if (!pumpKeywords.isEmpty()) {
                        copyInPlay.addChangedCardKeywords(pumpKeywords, Lists.<String>newArrayList(), false, timestamp);
                    }
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
                    registerDelayedTrigger(sa, sa.getParam("AtEOT"), crds);
                }
                if (sa.hasParam("ImprintCopied")) {
                    hostCard.addImprintedCards(crds);
                }
            } // end canBeTargetedBy
        } // end foreach Card
    } // end resolve
}
