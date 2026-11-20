package forge.web.match;

import forge.StaticData;
import forge.card.CardRules;
import forge.card.CardStateName;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.keyword.Keyword;
import forge.game.player.PlayerView;
import forge.gamemodes.net.DeltaPacket;
import forge.gui.card.CardDetailUtil;
import forge.web.images.Foil;
import forge.web.protocol.PropertyJson;
import forge.web.protocol.ToBrowser.CardFace;
import forge.web.protocol.ToBrowser.Detail;
import forge.web.protocol.ToBrowser.PlayerDetail;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/** What the zoom panel shows for a card or a player. The text is what the desktop client builds with CardDetailUtil. */
final class CardDetails {
    private CardDetails() {
    }

    static PlayerDetail player(final PlayerView player) {
        // The name comes first, and the commander damage messages end in a line break of their own, which leaves blank lines
        final List<String> details = player.getDetails().lines().skip(1).filter(line -> !line.isBlank()).toList();
        // The browser draws commander damage itself, so those lines are left out. They end the list, after the cast counts
        final int casts = player.getCommanders() == null ? 0 : 1 + player.getCommanders().size();
        final int damage = Math.max(0, player.getPlayerCommanderInfo().size() - casts);
        return new PlayerDetail(DeltaPacket.makeDeltaKey(player), player.getName(), details.subList(0, details.size() - damage));
    }

    /** The faces of a card that this viewer may see. mayFlip adds the other face, and is true only for a viewer allowed to see it. */
    static Detail card(final CardView card, final GameView game, final boolean mayView, final boolean mayFlip) {
        final List<CardFace> faces = new ArrayList<>();
        if (mayView) {
            faces.add(face(card, card.getCurrentState(), game));
            if (card.isSplitCard() && card.hasLeftSplitState() && card.hasRightSplitState()) {
                faces.add(face(card, card.getLeftSplitState(), game));
                faces.add(face(card, card.getRightSplitState(), game));
            } else if (mayFlip) {
                faces.add(face(card, card.getAlternateState(), game));
            }
        }
        return new Detail(DeltaPacket.makeDeltaKey(card), faces);
    }

    private static CardFace face(final CardView card, final CardStateView state, final GameView game) {
        final String pt = state.isCreature() ? state.getPower() + "/" + state.getToughness()
                : state.isPlaneswalker() ? state.getLoyalty()
                : state.isBattle() ? state.getDefense() : null;
        return new CardFace(state.getName(), PropertyJson.manaCost(state.getManaCost()),
                state.getType() == null ? "" : state.getType().toString(), pt,
                CardDetailUtil.composeCardText(state, game, true).trim(), Foil.key(card, state),
                state.getColors() == null ? 0 : state.getColors().getColor(),
                StringUtils.isEmpty(state.getSetCode()) ? null : state.getSetCode(),
                state.getRarity(), turn(card, state));
    }

    /** The same turns as the desktop preview, except for an Aftermath card. It is printed upright, so only its second half turns, anticlockwise. */
    private static Integer turn(final CardView card, final CardStateView state) {
        if (card.isFaceDown()) {
            return null;
        }
        if (card.isSplitCard()) {
            // A view fills in keywords for its current state only, so the printed card's second half is read instead
            final CardRules rules = StaticData.instance().getCommonCards().getRules(card.getOracleName(), true);
            final boolean aftermath = rules != null && rules.getOtherPart() != null
                    && rules.hasStartOfKeyword(Keyword.AFTERMATH.toString(), rules.getOtherPart());
            if (!aftermath) {
                return 90;
            }
            return state.getState() == CardStateName.RightSplit ? Integer.valueOf(270) : null;
        }
        return state.isPlane() || state.isPhenomenon() ? 90 : null;
    }
}
