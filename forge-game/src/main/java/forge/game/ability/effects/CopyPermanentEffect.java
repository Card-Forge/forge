package forge.game.ability.effects;

import java.util.Arrays;
import java.util.List;

import forge.util.Lang;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.StaticData;
import forge.card.CardRulesPredicates;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactory;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardZoneTable;
import forge.game.card.TokenCreateTable;
import forge.game.event.GameEventCombatChanged;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.Localizer;
import forge.util.PredicateString.StringOp;
import forge.util.TextUtil;

public class CopyPermanentEffect extends TokenEffectBase {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        if (sa.hasParam("Populate")) {
            return "Populate. (Create a token that's a copy of a creature token you control.)";
        }
        final StringBuilder sb = new StringBuilder();

        final Player activator = sa.getActivatingPlayer();
        final List<Card> tgtCards = getTargetCards(sa);
        boolean justOne = tgtCards.size() == 1;
        boolean addKWs = sa.hasParam("AddKeywords");
        final int numCopies = sa.hasParam("NumCopies") ?
                AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumCopies"), sa) : 1;

        sb.append(activator).append(" creates ").append(Lang.nounWithNumeralExceptOne(numCopies, "token"));
        sb.append(numCopies == 1 ? " that's a copy" : " that are copies").append(" of ");
        sb.append(Lang.joinHomogenous(tgtCards));

        if (addKWs) {
            final List<String> keywords = Lists.newArrayList();
            keywords.addAll(Arrays.asList(sa.getParam("AddKeywords").split(" & ")));
            if (sa.getDescription().contains("except")) {
                sb.append(", except ").append(justOne ? "it has " : "they have ");
            } else {
                sb.append(". ").append(justOne ? "It gains " : "They gain ");
            }
            sb.append(Lang.joinHomogenous(keywords).toLowerCase());
        }

        if (sa.hasParam("AddTriggers")) {
            final String oDesc = sa.getDescription();
            final String trigStg = oDesc.contains("\"") ?
                    oDesc.substring(oDesc.indexOf("\""),oDesc.lastIndexOf("\"") + 1) :
                    "[trigger text parsing error]";
            if (addKWs) {
                sb.append(" and ").append(trigStg);
            } else {
                sb.append(". ").append(justOne ? "It gains " : "They gain ").append(trigStg);
            }
        } else {
            sb.append(".");
        }

        if (sa.hasParam("AtEOT")) {
            String atEOT = sa.getParam("AtEOT");
            String verb = "Sacrifice ";
            if (atEOT.startsWith("Exile")) {
                verb = "Exile ";
            }
            sb.append(" ").append(verb).append(justOne ? "it " : "them ").append("at ");
            String when = "the beginning of the next end step.";
            if (atEOT.endsWith("Combat")) {
                when = "end of combat.";
            }
            sb.append(when);
        }

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

        MutableBoolean combatChanged = new MutableBoolean(false);
        TokenCreateTable tokenTable = new TokenCreateTable();

        if (sa.hasParam("Optional") && !activator.getController().confirmAction(sa, null,
                Localizer.getInstance().getMessage("lblCopyPermanentConfirm"))) {
            return;
        }

        final int numCopies = sa.hasParam("NumCopies") ? AbilityUtils.calculateAmount(host,
                sa.getParam("NumCopies"), sa) : 1;

        List<Player> controllers = Lists.newArrayList();
        if (sa.hasParam("Controller")) {
            controllers = AbilityUtils.getDefinedPlayers(host, sa.getParam("Controller"), sa);
        }
        if (controllers.isEmpty()) {
            controllers.add(activator);
        }

        for (final Player controller : controllers) {
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
                    final String num = sa.getParamOrDefault("RandomNum", "1");
                    int ncopied = AbilityUtils.calculateAmount(host, num, sa);
                    while (ncopied > 0 && !copysource.isEmpty()) {
                        final PaperCard cp = Aggregates.random(copysource);
                        Card possibleCard = Card.fromPaperCard(cp, activator); // Need to temporarily set the Owner so the Game is set

                        if (possibleCard.isValid(valid, host.getController(), host, sa)) {
                            if (host.getController().isAI() && possibleCard.getRules() != null && possibleCard.getRules().getAiHints().getRemAIDecks())
                                continue;
                            choice.add(possibleCard);
                            ncopied -= 1;
                        }
                        copysource.remove(cp);
                    }
                    tgtCards = choice;

                    System.err.println("Copying random permanent(s): " + tgtCards.toString());
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

                // For Mimic Vat with mutated creature, need to choose one imprinted card
                CardCollectionView choices = sa.hasParam("Defined") ? getDefinedCardsOrTargeted(sa) : game.getCardsIn(ZoneType.Battlefield);
                choices = CardLists.getValidCards(choices, sa.getParam("Choices"), activator, host, sa);
                if (!choices.isEmpty()) {
                    String title = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : Localizer.getInstance().getMessage("lblChooseaCard");

                    if (sa.hasParam("WithDifferentNames")) {
                        // any Number of choices with different names
                        while (!choices.isEmpty()) {
                            Card choosen = chooser.getController().chooseSingleEntityForEffect(choices, sa, title, true, null);

                            if (choosen != null) {
                                tgtCards.add(choosen);
                                choices = CardLists.filter(choices, Predicates.not(CardPredicates.sharesNameWith(choosen)));
                            } else if (chooser.getController().confirmAction(sa, PlayerActionConfirmMode.OptionalChoose, Localizer.getInstance().getMessage("lblCancelChooseConfirm"))) {
                                break;
                            }
                        }
                    } else {
                        Card choosen = chooser.getController().chooseSingleEntityForEffect(choices, sa, title, false, null);
                        if (choosen != null) {
                            tgtCards.add(choosen);
                        }
                    }
                }
            } else {
                tgtCards = getDefinedCardsOrTargeted(sa);
            }

            for (final Card c : tgtCards) {
                // 111.5. Similarly, if an effect would create a token that is a copy of an instant or sorcery card, no token is created.
                // instant and sorcery can't enter the battlefield
                // and it can't be replaced by other tokens
                if (c.isInstant() || c.isSorcery()) {
                    continue;
                }

                // if it only targets player, it already got all needed cards from defined
                if (sa.usesTargeting() && !sa.getTargetRestrictions().canTgtPlayer() && !c.canBeTargetedBy(sa)) {
                    continue;
                }
                if (sa.hasParam("ForEach")) {
                    for (Player p : AbilityUtils.getDefinedPlayers(host, sa.getParam("ForEach"), sa)) {
                        Card proto = getProtoType(sa, c, controller);
                        proto.addRemembered(p);
                        tokenTable.put(controller, proto, numCopies);
                    }
                } else {
                    tokenTable.put(controller, getProtoType(sa, c, controller), numCopies);
                }
            } // end foreach Card
        }

        makeTokenTable(tokenTable, true, triggerList, combatChanged, sa);

        if (!useZoneTable) {
            triggerList.triggerChangesZoneAll(game, sa);
            triggerList.clear();
        }
        if (combatChanged.isTrue()) {
            game.updateCombatForView();
            game.fireEvent(new GameEventCombatChanged());
        }
    } // end resolve

    public static Card getProtoType(final SpellAbility sa, final Card original, final Player newOwner) {
        final Card host = sa.getHostCard();
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

        return copy;
    }
}
