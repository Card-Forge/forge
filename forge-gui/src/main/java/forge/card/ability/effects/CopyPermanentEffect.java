package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Card;
import forge.CardLists;
import forge.Command;
import forge.GameEntity;
import forge.Singletons;
import forge.card.CardCharacteristicName;
import forge.card.CardRulesPredicates;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.cardfactory.CardFactory;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.mana.ManaCost;
import forge.card.spellability.Ability;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.TargetRestrictions;
import forge.game.Game;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.PredicateString.StringOp;

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
        final Card hostCard = sa.getSourceCard();
        final Game game = hostCard.getGame();
        final ArrayList<String> keywords = new ArrayList<String>();
        if (sa.hasParam("Optional")) {
            if (!sa.getActivatingPlayer().getController().confirmAction(sa, null, "Copy this permanent?")) {
                return;
            }
        }
        if (sa.hasParam("Keywords")) {
            keywords.addAll(Arrays.asList(sa.getParam("Keywords").split(" & ")));
        }
        final int numCopies = sa.hasParam("NumCopies") ? AbilityUtils.calculateAmount(hostCard,
                sa.getParam("NumCopies"), sa) : 1;

        List<Card> tgtCards = getTargetCards(sa);
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        if (sa.hasParam("ValidSupportedCopy")) {
            List<PaperCard> cards = Lists.newArrayList(Singletons.getMagicDb().getCommonCards().getUniqueCards());
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
                    Card possibleCard = Card.fromPaperCard(cp, null);
                    // Need to temporarily set the Owner so the Game is set
                    possibleCard.setOwner(sa.getActivatingPlayer());
                    
                    if (possibleCard.isValid(valid, hostCard.getController(), hostCard)) {
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
                    tgtCards.add(Card.fromPaperCard(cards.get(0), null));
                }
            }
        }

        Player controller = null;
        if (sa.hasParam("Controller")) {
            List<Player> defined = AbilityUtils.getDefinedPlayers(hostCard, sa.getParam("Controller"), sa);
            if (!defined.isEmpty()) {
                controller = defined.get(0);
            }
        }
        if (controller == null) {
            controller = sa.getActivatingPlayer();
        }

        hostCard.clearClones();

        for (final Card c : tgtCards) {
            if ((tgt == null) || c.canBeTargetedBy(sa)) {

                boolean wasInAlt = false;
                CardCharacteristicName stateName = CardCharacteristicName.Original;
                if (c.isInAlternateState()) {
                    stateName = c.getCurState();
                    wasInAlt = true;
                    c.setState(CardCharacteristicName.Original);
                }

                // start copied Kiki code
                int multiplier = numCopies * hostCard.getController().getTokenDoublersMagnitude();
                final Card[] crds = new Card[multiplier];

                for (int i = 0; i < multiplier; i++) {
                    // TODO Use central copy methods
                    Card copy;
                    if (!c.isToken() || c.isCopiedToken()) {
                        // copy creature and put it onto the battlefield

                        copy = CardFactory.getCard(c.getPaperCard(), sa.getActivatingPlayer());

                        copy.setToken(true);
                        copy.setCopiedToken(true);
                    } else { // isToken()
                        copy = CardFactory.copyStats(c, controller);

                        copy.setName(c.getName());
                        copy.setImageKey(c.getImageKey());

                        copy.setManaCost(c.getManaCost());
                        copy.setColor(c.getColor());
                        copy.setToken(true);

                        copy.setType(c.getType());

                        copy.setBaseAttack(c.getBaseAttack());
                        copy.setBaseDefense(c.getBaseDefense());

                        CardFactoryUtil.addAbilityFactoryAbilities(copy);
                        for (String s : copy.getStaticAbilityStrings()) {
                            copy.addStaticAbility(s);
                        }
                    }

                    // when copying something stolen:
                    copy.setController(controller, 0);
                    copy.setCurSetCode(c.getCurSetCode());

                    if (c.isDoubleFaced()) { // Cloned DFC's can't transform
                        if (wasInAlt) {
                            copy.setState(CardCharacteristicName.Transformed);
                        }
                    }
                    if (c.isFlipCard()) { // Cloned Flips CAN flip.
                        copy.setState(CardCharacteristicName.Original);
                        c.setState(CardCharacteristicName.Original);
                        copy.setImageKey(c.getImageKey());
                        if (!c.isInAlternateState()) {
                            copy.setState(CardCharacteristicName.Flipped);
                        }

                        c.setState(CardCharacteristicName.Flipped);
                    }

                    if (c.isFaceDown()) {
                        c.setState(CardCharacteristicName.FaceDown);
                    }

                    if (sa.hasParam("AttachedTo")) {
                        List<Card> list = AbilityUtils.getDefinedCards(hostCard,
                                sa.getParam("AttachedTo"), sa);
                        if (list.isEmpty()) {
                            list = copy.getController().getGame().getCardsIn(ZoneType.Battlefield);
                            list = CardLists.getValidCards(list, sa.getParam("AttachedTo"), copy.getController(), copy);
                        }
                        if (!list.isEmpty()) {
                            Card attachedTo = sa.getActivatingPlayer().getController().chooseSingleCardForEffect(list, sa, copy + " - Select a card to attach to.");
                            if (copy.isAura()) {
                                if (attachedTo.canBeEnchantedBy(copy)) {
                                    copy.enchantEntity(attachedTo);
                                } else {//can't enchant
                                    continue;
                                }
                            } else if (copy.isEquipment()) { //Equipment
                                if (attachedTo.canBeEquippedBy(copy)) {
                                    copy.equipCard(attachedTo);
                                } else {
                                    continue;
                                }
                            } else { // Fortification
                                copy.fortifyCard(attachedTo);
                            }
                        } else {
                            continue;
                        }
                    }

                    // add keywords from sa
                    for (final String kw : keywords) {
                        copy.addIntrinsicKeyword(kw);
                    }

                    copy = game.getAction().moveToPlay(copy);

                    copy.setCloneOrigin(hostCard);
                    sa.getSourceCard().addClone(copy);
                    crds[i] = copy;
                    if (sa.hasParam("RememberCopied")) {
                        hostCard.addRemembered(copy);
                    }
                    if (sa.hasParam("Tapped")) {
                        copy.setTapped(true);
                    }
                    if (sa.hasParam("CopyAttacking") && game.getPhaseHandler().inCombat()) {
                        final GameEntity defender = AbilityUtils.getDefinedPlayers(hostCard, sa.getParam("CopyAttacking"), sa).get(0);
                        game.getCombat().addAttacker(copy, defender);
                    }
                    
                    if (sa.hasParam("AtEOT")) {
                        final String location = sa.getParam("AtEOT");
                        final Card source = copy;
                    
                        final SpellAbility sac = new Ability(copy, ManaCost.ZERO) {
                            @Override
                            public void resolve() {
                                // technically your opponent could steal the token
                                // and the token shouldn't be sacrificed
                                if (source.isInPlay()) {
                                    if (location.equals("Sacrifice")) {
                                        game.getAction().sacrifice(source, sa);
                                    } else if (location.equals("Exile")) {
                                        game.getAction().exile(source);
                                    }
    
                                }
                            }
                        };

                        final Command atEOT = new Command() {
                            private static final long serialVersionUID = -4184510100801568140L;
    
                            @Override
                            public void run() {
                                sac.setStackDescription(sa.getParam("AtEOT") + " " + source + ".");
                                game.getStack().addSimultaneousStackEntry(sac);
                            }
                        };
                    
                        game.getEndOfTurn().addAt(atEOT);
                    }
                }

                if (wasInAlt) {
                    c.setState(stateName);
                }
            } // end canBeTargetedBy
        } // end foreach Card
    } // end resolve

}
