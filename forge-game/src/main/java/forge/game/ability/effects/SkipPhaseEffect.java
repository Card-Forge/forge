package forge.game.ability.effects;

import java.util.List;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.replacement.ReplacementLayer;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

public class SkipPhaseEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final String duration = sa.getParam("Duration");
        final String phase = sa.getParam("Phase");
        final String step = sa.getParam("Step");

        List<Player> tgtPlayers = getTargetPlayers(sa);
        for (final Player player : tgtPlayers) {
            sb.append(player).append(" ");
            sb.append("skips their ");
            if (duration == null) {
                sb.append("next ");
            }
            if (phase != null) {
                sb.append(phase.toLowerCase()).append(" phase.");
            } else {
                sb.append(step.toLowerCase()).append(" step.");
            }
        }

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final String duration = sa.getParam("Duration");
        final String phase = sa.getParam("Phase");
        final String step = sa.getParam("Step");

        List<Player> tgtPlayers = getTargetPlayers(sa);
        for (final Player player : tgtPlayers) {
            createSkipPhaseEffect(sa, player, duration, phase, step);
        }
    }

    public static void createSkipPhaseEffect(SpellAbility sa, final Player player,
            final String duration, final String phase, final String step) {
        final Card hostCard = sa.getHostCard();
        final Game game = hostCard.getGame();
        final String name = hostCard + "'s Effect";
        final String image = hostCard.getImageKey();
        final boolean isNextThisTurn = duration != null && duration.equals("NextThisTurn");

        final Card eff = createEffect(sa, player, name, image);

        final StringBuilder sb = new StringBuilder();
        sb.append("Event$ BeginPhase | ActiveZones$ Command | ValidPlayer$ You | Phase$ ");
        sb.append(phase != null ? phase : step);
        if (duration != null && !isNextThisTurn) {
            sb.append(" | Skip$ True");
        }
        sb.append("| Description$ Skip ");
        if (duration == null || isNextThisTurn) {
            sb.append("your next ");
        } else {
            sb.append("each ");
        }
        if (phase != null) {
            sb.append(phase.toLowerCase()).append(" phase");
        } else {
            sb.append(step.toLowerCase()).append(" step");
        }
        if (duration == null) {
            sb.append(".");
        } else {
            if (game.getPhaseHandler().getPlayerTurn().equals(player)) {
                sb.append(" of this turn.");
            } else {
                sb.append(" of your next turn.");
            }
        }

        final String repeffstr = sb.toString();
        ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, eff, true);
        // Set to layer to Control so it will be applied before "would begin your X phase/step" replacement effects
        // (Any layer before Other is OK, since default layer is Other.)
        re.setLayer(ReplacementLayer.Control);
        if (duration == null || isNextThisTurn) {
            String exilestr = "DB$ ChangeZone | Defined$ Self | Origin$ Command | Destination$ Exile";
            SpellAbility exile = AbilityFactory.getAbility(exilestr, eff);
            re.setOverridingAbility(exile);
        }
        if (duration != null) {
            final GameCommand endEffect = new GameCommand() {
                private static final long serialVersionUID = -5861759814760561373L;

                @Override
                public void run() {
                    game.getAction().exile(eff, null, null);
                }
            };

            addUntilCommand(sa, endEffect);
        }
        eff.addReplacementEffect(re);

        if (sa.hasParam("Start")) {
            final GameCommand startEffect = new GameCommand() {
                private static final long serialVersionUID = -5861749814760561373L;

                @Override
                public void run() {
                    game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
                    game.getAction().moveTo(ZoneType.Command, eff, sa, AbilityKey.newMap());
                    eff.updateStateForView();
                    game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
                }
            };
            game.getUpkeep().addUntil(player, startEffect);
        } else {
            game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
            game.getAction().moveTo(ZoneType.Command, eff, sa, AbilityKey.newMap());
            eff.updateStateForView();
            game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
        }
    }
}
