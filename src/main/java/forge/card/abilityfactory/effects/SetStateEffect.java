package forge.card.abilityfactory.effects;

import java.util.Iterator;
import java.util.List;

import forge.Card;
import forge.CardCharacteristicName;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;

public class SetStateEffect extends SpellEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final String conditionDesc = sa.getParam("ConditionDescription");
        if (conditionDesc != null) {
            sb.append(conditionDesc).append(" ");
        }

        final List<Card> tgtCards = getTargetCards(sa);

        if (sa.hasParam("Flip")) {
            sb.append("Flip");
        } else {
            sb.append("Transform ");
        }

        final Iterator<Card> it = tgtCards.iterator();
        while (it.hasNext()) {
            final Card tgtC = it.next();
            if (tgtC.isFaceDown()) {
                sb.append("Morph ").append("(").append(tgtC.getUniqueNumber()).append(")");
            } else {
                sb.append(tgtC);
            }

            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(".");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {

        final Card host = sa.getSourceCard();
        final List<Card> tgtCards = getTargetCards(sa);

        final boolean remChanged = sa.hasParam("RememberChanged");

        for (final Card tgt : tgtCards) {
            if (sa.getTarget() != null) {
                if (!tgt.canBeTargetedBy(sa)) {
                    continue;
                }
            }

            final String mode = sa.getParam("Mode");

            if (mode != null) {
                if (mode.equals("Transform")) {
                    if (tgt.hasKeyword("CARDNAME can't transform")) {
                        continue;
                    }
                    if (tgt.isDoubleFaced()) {
                        if (tgt.getCurState() == CardCharacteristicName.Original) {
                            if (tgt.changeToState(CardCharacteristicName.Transformed)) {
                                if (remChanged) {
                                    host.addRemembered(tgt);
                                }
                            }
                        } else if (tgt.getCurState() == CardCharacteristicName.Transformed) {
                            if (tgt.changeToState(CardCharacteristicName.Original)) {
                                if (remChanged) {
                                    host.addRemembered(tgt);
                                }

                            }
                        }
                    }
                } else if (mode.equals("Flip")) {
                    if (tgt.isFlipCard()) {
                        if (tgt.getCurState() == CardCharacteristicName.Original) {
                            if (tgt.changeToState(CardCharacteristicName.Flipped)) {
                                host.setFlipStaus(true);
                                if (remChanged) {
                                    host.addRemembered(tgt);
                                }
                            }

                        } else if (tgt.getCurState() == CardCharacteristicName.Flipped) {
                            if (tgt.changeToState(CardCharacteristicName.Original)) {
                                host.setFlipStaus(false);
                                if (remChanged) {
                                    host.addRemembered(tgt);
                                }
                            }
                        }
                    }
                } else if (mode.equals("TurnFace")) {
                    if (tgt.getCurState() == CardCharacteristicName.Original) {
                        if (tgt.turnFaceDown() && remChanged) {
                            host.addRemembered(tgt);
                        }
                    } else if (tgt.getCurState() == CardCharacteristicName.FaceDown) {
                        if (tgt.turnFaceUp() && remChanged) {
                            host.addRemembered(tgt);
                        }
                    }
                }
            } else {
                tgt.changeToState(CardCharacteristicName.smartValueOf(sa.getParam("NewState")));
            }

        }

    }

}
