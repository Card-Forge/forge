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
import forge.CardCharacteristicName;
import forge.CardLists;
import forge.Command;
import forge.card.CardRulesPredicates;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.cardfactory.CardFactory;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.mana.ManaCost;
import forge.card.spellability.Ability;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.Game;
import forge.game.ai.ComputerUtilCard;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.item.CardDb;
import forge.item.CardPrinted;
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
        if (sa.hasParam("Keywords")) {
            keywords.addAll(Arrays.asList(sa.getParam("Keywords").split(" & ")));
        }
        final int numCopies = sa.hasParam("NumCopies") ? AbilityUtils.calculateAmount(hostCard,
                sa.getParam("NumCopies"), sa) : 1;

        List<Card> tgtCards = getTargetCards(sa);
        final Target tgt = sa.getTarget();

        if (sa.hasParam("ValidSupportedCopy")) {
            List<CardPrinted> cards = Lists.newArrayList(CardDb.instance().getUniqueCards());
            String valid = sa.getParam("ValidSupportedCopy");
            if (valid.contains("X")) {
                valid = valid.replace("X", Integer.toString(AbilityUtils.calculateAmount(hostCard, "X", sa)));
            }
            if (StringUtils.containsIgnoreCase(valid, "creature")) {
                Predicate<CardPrinted> cpp = Predicates.compose(CardRulesPredicates.Presets.IS_CREATURE, CardPrinted.FN_GET_RULES);
                cards = Lists.newArrayList(Iterables.filter(cards, cpp));
            }
            if (StringUtils.containsIgnoreCase(valid, "equipment")) {
                Predicate<CardPrinted> cpp = Predicates.compose(CardRulesPredicates.Presets.IS_EQUIPMENT, CardPrinted.FN_GET_RULES);
                cards = Lists.newArrayList(Iterables.filter(cards, cpp));
            }
            if (sa.hasParam("RandomCopied")) {
                List<CardPrinted> copysource = new ArrayList<CardPrinted>(cards);
                List<Card> choice = new ArrayList<Card>();
                final String num = sa.hasParam("RandomNum") ? sa.getParam("RandomNum") : "1";
                int ncopied = AbilityUtils.calculateAmount(hostCard, num, sa);
                while(ncopied > 0) {
                    final CardPrinted cp = Aggregates.random(copysource);
                    if (cp.getMatchingForgeCard().isValid(valid, hostCard.getController(), hostCard)) {
                        choice.add(cp.getMatchingForgeCard());
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

                Predicate<CardPrinted> cpp = Predicates.compose(CardRulesPredicates.name(StringOp.EQUALS, name), CardPrinted.FN_GET_RULES);
                cards = Lists.newArrayList(Iterables.filter(cards, cpp));

                tgtCards.clear();
                if (!cards.isEmpty()) {
                    Card c = cards.get(0).getMatchingForgeCard();
                    tgtCards.add(c);
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

                        copy = CardFactory.getCard(CardDb.getCard(c), sa.getActivatingPlayer());

                        // when copying something stolen:
                        copy.setController(controller, 0);

                        copy.setToken(true);
                        copy.setCopiedToken(true);
                    } else { // isToken()
                        copy = CardFactory.copyStats(c);

                        copy.setName(c.getName());
                        copy.setImageKey(c.getImageKey());

                        copy.setOwner(controller);
                        copy.setController(controller, 0);

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

                    // add keywords from sa
                    for (final String kw : keywords) {
                        copy.addIntrinsicKeyword(kw);
                    }

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
                            Card attachedTo = null;
                            if (sa.getActivatingPlayer().isHuman()) {
                                if (list.size() > 1) {
                                    attachedTo = GuiChoose.one(copy + " - Select a card to attach to.", list);
                                } else {
                                    attachedTo = list.get(0);
                                }
                            } else { // AI player
                                attachedTo = ComputerUtilCard.getBestAI(list);
                            }
                            if (copy.isAura()) {
                                if (attachedTo.canBeEnchantedBy(copy)) {
                                    copy.enchantEntity(attachedTo);
                                } else {//can't enchant
                                    continue;
                                }
                            } else { //Equipment
                                copy.equipCard(attachedTo);
                            }
                        } else {
                            continue;
                        }
                    }
                    copy = game.getAction().moveToPlay(copy);

                    copy.setCloneOrigin(hostCard);
                    sa.getSourceCard().addClone(copy);
                    crds[i] = copy;
                    if (sa.hasParam("RememberCopied")) {
                        hostCard.addRemembered(copy);
                    }
                }

                if (wasInAlt) {
                    c.setState(stateName);
                }
                

                // have to do this since getTargetCard() might change
                // if Kiki-Jiki somehow gets untapped again
                final Card[] target = new Card[multiplier];
                for (int i = 0; i < multiplier; i++) {
                    final int index = i;
                    target[index] = crds[index];

                    final SpellAbility sac = new Ability(target[index], ManaCost.ZERO) {
                        @Override
                        public void resolve() {
                            // technically your opponent could steal the token
                            // and the token shouldn't be sacrificed
                            if (target[index].isInPlay()) {
                                if (sa.getParam("AtEOT").equals("Sacrifice")) {
                                    // maybe do a setSacrificeAtEOT, but
                                    // probably not.
                                    game.getAction().sacrifice(target[index], sa);
                                } else if (sa.getParam("AtEOT").equals("Exile")) {
                                    game.getAction().exile(target[index]);
                                }

                            }
                        }
                    };

                    final Command atEOT = new Command() {
                        private static final long serialVersionUID = -4184510100801568140L;

                        @Override
                        public void run() {
                            sac.setStackDescription(sa.getParam("AtEOT") + " " + target[index] + ".");
                            game.getStack().addSimultaneousStackEntry(sac);
                        }
                    }; // Command
                    if (sa.hasParam("AtEOT")) {
                        game.getEndOfTurn().addAt(atEOT);
                    }
                    // end copied Kiki code

                }
            } // end canBeTargetedBy
        } // end foreach Card
    } // end resolve

}
