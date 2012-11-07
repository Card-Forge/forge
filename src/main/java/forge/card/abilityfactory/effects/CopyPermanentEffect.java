package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.CardCharacteristicName;
import forge.Command;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.Ability;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.item.CardDb;

public class CopyPermanentEffect extends SpellEffect {
    
    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();


        final List<Card> tgtCards = getTargetCards(sa, params);

        sb.append("Copy ");
        sb.append(StringUtils.join(tgtCards, ", "));
        sb.append(".");
        return sb.toString();
    }

    @Override
    public void resolve(final java.util.Map<String,String> params, final SpellAbility sa) {
        final Card hostCard = sa.getSourceCard();
        final ArrayList<String> keywords = new ArrayList<String>();
        if (params.containsKey("Keywords")) {
            keywords.addAll(Arrays.asList(params.get("Keywords").split(" & ")));
        }
        final int numCopies = params.containsKey("NumCopies") ? AbilityFactory.calculateAmount(hostCard,
                params.get("NumCopies"), sa) : 1;

        final List<Card> tgtCards = getTargetCards(sa, params);
        final Target tgt = sa.getTarget();

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

                        copy = Singletons.getModel().getCardFactory().getCard(CardDb.instance().getCard(c), sa.getActivatingPlayer());

                        // when copying something stolen:
                        copy.addController(sa.getActivatingPlayer());

                        copy.setToken(true);
                        copy.setCopiedToken(true);
                    } else { // isToken()
                        copy = CardFactoryUtil.copyStats(c);

                        copy.setName(c.getName());
                        copy.setImageName(c.getImageName());

                        copy.setOwner(sa.getActivatingPlayer());
                        copy.addController(sa.getActivatingPlayer());

                        copy.setManaCost(c.getManaCost());
                        copy.setColor(c.getColor());
                        copy.setToken(true);

                        copy.setType(c.getType());

                        copy.setBaseAttack(c.getBaseAttack());
                        copy.setBaseDefense(c.getBaseDefense());
                    }

                    // add keywords from params
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
                        copy.setImageFilename(c.getImageFilename());
                        if (!c.isInAlternateState()) {
                            copy.setState(CardCharacteristicName.Flipped);
                        }

                        c.setState(CardCharacteristicName.Flipped);
                    }

                    if (c.isFaceDown()) {
                        c.setState(CardCharacteristicName.FaceDown);
                    }
                    copy = Singletons.getModel().getGame().getAction().moveToPlay(copy);

                    copy.setCloneOrigin(hostCard);
                    sa.getSourceCard().addClone(copy);
                    crds[i] = copy;
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

                    final SpellAbility sac = new Ability(target[index], "0") {
                        @Override
                        public void resolve() {
                            // technically your opponent could steal the token
                            // and the token shouldn't be sacrificed
                            if (target[index].isInPlay()) {
                                if (params.get("AtEOT").equals("Sacrifice")) {
                                    // maybe do a setSacrificeAtEOT, but
                                    // probably not.
                                    Singletons.getModel().getGame().getAction().sacrifice(target[index], sa);
                                } else if (params.get("AtEOT").equals("Exile")) {
                                    Singletons.getModel().getGame().getAction().exile(target[index]);
                                }

                            }
                        }
                    };

                    final Command atEOT = new Command() {
                        private static final long serialVersionUID = -4184510100801568140L;

                        @Override
                        public void execute() {
                            sac.setStackDescription(params.get("AtEOT") + " " + target[index] + ".");
                            Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(sac);
                        }
                    }; // Command
                    if (params.containsKey("AtEOT")) {
                        Singletons.getModel().getGame().getEndOfTurn().addAt(atEOT);
                    }
                    // end copied Kiki code

                }
            } // end canBeTargetedBy
        } // end foreach Card
    } // end resolve

}