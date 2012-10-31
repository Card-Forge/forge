package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Iterator;

import forge.Card;
import forge.CardCharacteristicName;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

public class SetStateEffect extends SpellEffect {
    
    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getAbilityFactory().getHostCard();

        final String conditionDesc = params.get("ConditionDescription");
        if (conditionDesc != null) {
            sb.append(conditionDesc).append(" ");
        }

        ArrayList<Card> tgtCards;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(host).append(" - ");
        }

        if (params.containsKey("Flip")) {
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
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        ArrayList<Card> tgtCards;
        final Card host = sa.getAbilityFactory().getHostCard(); 

        if (sa.getTarget() != null) {
            tgtCards = sa.getTarget().getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(host, params.get("Defined"), sa);
        }

        final boolean remChanged = params.containsKey("RememberChanged");

        for (final Card tgt : tgtCards) {
            if (sa.getTarget() != null) {
                if (!tgt.canBeTargetedBy(sa)) {
                    continue;
                }
            }

            final String mode = params.get("Mode");

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
                tgt.changeToState(CardCharacteristicName.smartValueOf(params.get("NewState")));
            }

        }

    }

    // //////////////////////////////////////////////
    // changeStateAll //
    // //////////////////////////////////////////////

}