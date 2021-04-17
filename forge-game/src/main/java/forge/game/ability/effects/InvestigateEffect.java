package forge.game.ability.effects;

import org.apache.commons.lang3.mutable.MutableBoolean;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
import forge.game.card.token.TokenInfo;
import forge.game.event.GameEventCombatChanged;
import forge.game.event.GameEventTokenCreated;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

public class InvestigateEffect extends TokenEffectBase {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final int amount = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("Num", "1"), sa);

        StringBuilder sb = new StringBuilder("Investigate");
        if (amount > 1) {
            sb.append(" ").append(Lang.getNumeral(amount)).append(" times");
        }
        sb.append(". (Create a colorless Clue artifact token with \"{2}, Sacrifice this artifact: Draw a card.\")");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final Game game = card.getGame();

        final int amount = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("Num", "1"), sa);

        final String tokenScript = "c_a_clue_draw";
        final Card prototype = TokenInfo.getProtoType(tokenScript, sa, false);

        for (final Player p : getTargetPlayers(sa)) {
            for (int i = 0; i < amount; i++) {
                CardZoneTable triggerList = new CardZoneTable();
                MutableBoolean combatChanged = new MutableBoolean(false);
                makeTokens(prototype, p, sa, 1, true, false, triggerList, combatChanged);

                triggerList.triggerChangesZoneAll(game, sa);
                p.addInvestigatedThisTurn();

                game.fireEvent(new GameEventTokenCreated());

                if (combatChanged.isTrue()) {
                    game.updateCombatForView();
                    game.fireEvent(new GameEventCombatChanged());
                }
            }
        }
    }

}
