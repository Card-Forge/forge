package forge.card.abilityfactory.effects;

import java.util.List;

import forge.Card;
import forge.CardCharacteristicName;
import forge.CardLists;
import forge.Singletons;
import forge.card.abilityfactory.AbilityUtils;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class SetStateAllEffect extends SpellEffect {

    @Override
    public void resolve(SpellAbility sa) {

        final Card card = sa.getSourceCard();

        final Target tgt = sa.getTarget();
        Player targetPlayer = null;
        if (tgt != null) {
            targetPlayer = tgt.getTargetPlayers().get(0);
        }

        String valid = "";

        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        // Ugh. If calculateAmount needs to be called with DestroyAll it _needs_
        // to use the X variable
        // We really need a better solution to this
        if (valid.contains("X")) {
            valid = valid.replace("X", Integer.toString(AbilityUtils.calculateAmount(card, "X", sa)));
        }

        List<Card> list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);

        if (targetPlayer != null) {
            list = CardLists.filterControlledBy(list, targetPlayer);
        }

        list = AbilityUtils.filterListByType(list, valid, sa);

        final boolean remChanged = sa.hasParam("RememberChanged");
        if (remChanged) {
            card.clearRemembered();
        }

        for (Card c : list) {
            final String mode = sa.getParam("Mode");
            if (mode != null) {
                if (mode.equals("Transform")) {
                    if (c.hasKeyword("CARDNAME can't transform")) {
                        continue;
                    }
                    if (c.isDoubleFaced()) {
                        if (c.getCurState() == CardCharacteristicName.Original) {
                            if (c.changeToState(CardCharacteristicName.Transformed) && remChanged) {
                                card.addRemembered(c);
                            }
                        } else if (c.getCurState() == CardCharacteristicName.Transformed) {
                            if (c.changeToState(CardCharacteristicName.Original)) {
                                if (remChanged) {
                                    card.addRemembered(c);
                                }

                            }
                        }
                    }
                } else if (mode.equals("Flip")) {
                    if (c.isFlipCard()) {
                        if (c.getCurState() == CardCharacteristicName.Original) {
                            if (c.changeToState(CardCharacteristicName.Flipped)) {
                                c.setFlipStaus(true);
                                if (remChanged) {
                                    card.addRemembered(tgt);
                                }
                            }
                        } else if (c.getCurState() == CardCharacteristicName.Flipped) {
                            if (c.changeToState(CardCharacteristicName.Original)) {
                                c.setFlipStaus(false);
                                if (remChanged) {
                                    card.addRemembered(tgt);
                                }
                            }
                        }
                    }
                } else if (mode.equals("TurnFace")) {
                    if (c.getCurState() == CardCharacteristicName.Original) {
                        if (c.turnFaceDown() && remChanged) {
                            card.addRemembered(tgt);
                        }
                    } else if (c.getCurState() == CardCharacteristicName.FaceDown) {
                        if (c.turnFaceUp() && remChanged) {
                            card.addRemembered(tgt);
                        }
                    }
                }
            } else {
                c.changeToState(CardCharacteristicName.smartValueOf(sa.getParam("NewState")));
            }

        }
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        if (sa.hasParam("Mode")) {
            sb.append(sa.getParam("Mode"));
        } else {
            sb.append(sa.getParam("NewState"));
        }

        sb.append(" permanents.");
        return sb.toString();
    }
}
