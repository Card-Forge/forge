package forge.game.ability.effects;

import java.util.List;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;
import forge.util.TextUtil;

public class DeclareCombatantsEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);
        boolean attackers = sa.hasParam("DeclareAttackers");
        boolean blockers = sa.hasParam("DeclareBlockers");
        String what = Lang.joinHomogenous(
                attackers
                        ? "which creatures attack"
                        : null,
                blockers
                        ? "which creatures block this turn and how those creatures block"
                        : null
        );
        String duration = "EndOfTurn".equals(sa.getParam("Until")) ? "turn" : "combat";
        return TextUtil.concatWithSpace(Lang.joinHomogenous(tgtPlayers),Lang.joinVerb(tgtPlayers, "choose"),what,"this",TextUtil.addSuffix(duration,"."));
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);

        final boolean attackers = sa.hasParam("DeclareAttackers");
        final boolean blockers = sa.hasParam("DeclareBlockers");

        String until = sa.getParam("Until");
        boolean untilEoT = "EndOfTurn".equals(until);

        long ts = game.getNextTimestamp();

        for (Player p : tgtPlayers) { // Obviously the last player will be applied
            if (attackers) game.addDeclaresAttackers(p, ts);
            if (blockers) game.addDeclaresBlockers(p, ts);

            GameCommand removeOverrides = new GameCommand() {
                private static final long serialVersionUID = -8064627517852651016L;

                @Override
                public void run() {
                    game.removeDeclaresAttackers(ts);
                    game.removeDeclaresBlockers(ts);
                }
            };

            if (untilEoT)
                p.getGame().getEndOfTurn().addUntil(removeOverrides);
            else
                p.getGame().getEndOfCombat().addUntil(removeOverrides);
        }

    }

}
