package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.phase.ExtraTurn;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.util.Lang;
import forge.util.Localizer;

public class AddTurnEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final int numTurns = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumTurns"), sa);

        sb.append(Lang.joinHomogenous(getTargetPlayers(sa)));

        sb.append(" takes ");
        sb.append(numTurns > 1 ? numTurns : "an");
        sb.append(" extra turn");

        if (numTurns > 1) {
            sb.append("s");
        }
        sb.append(" after this one.");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final int numTurns = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumTurns"), sa);

        for (final Player p : getTargetPlayers(sa)) {
            if (!p.isInGame()) {
                continue;
            }
            for (int i = 0; i < numTurns; i++) {
                ExtraTurn extra = p.getGame().getPhaseHandler().addExtraTurn(p);
                if (sa.hasParam("ExtraTurnDelayedTrigger")) {
                    final Trigger delTrig = TriggerHandler.parseTrigger(sa.getSVar(sa.getParam("ExtraTurnDelayedTrigger")), sa.getHostCard(), true);
                    SpellAbility overridingSA = AbilityFactory.getAbility(sa.getSVar(sa.getParam("ExtraTurnDelayedTriggerExcute")), sa.getHostCard());
                    overridingSA.setActivatingPlayer(sa.getActivatingPlayer());
                    delTrig.setOverridingAbility(overridingSA);
                    delTrig.setSpawningAbility(sa.copy(sa.getHostCard(), true));
                    extra.addTrigger(delTrig);
                }
                if (sa.hasParam("SkipUntap")) {
                    extra.setSkipUntapSA(sa);
                }
                if (sa.hasParam("NoSchemes")) {
                    extra.setCantSetSchemesInMotionSA(sa);
                }
                if (sa.hasParam("ShowMessage")) {
                    p.getGame().getAction().notifyOfValue(sa, p, Localizer.getInstance().getMessage("lblPlayerTakesExtraTurn", p.toString()), null);
                }
            }
        }
    }

    public static void createCantSetSchemesInMotionEffect(SpellAbility sa) {
        final Card hostCard = sa.getHostCard();
        final Game game = hostCard.getGame();
        final String name = hostCard.getDisplayName() + "'s Effect";
        final String image = hostCard.getImageKey();

        final Card eff = createEffect(sa, sa.getActivatingPlayer(), name, image);

        String strRe = "Event$ SetInMotion | EffectZone$ Command | Layer$ CantHappen | Description$ Schemes can't be set in Motion";
        ReplacementEffect re = ReplacementHandler.parseReplacement(strRe, eff, true);
        eff.addReplacementEffect(re);

        game.getAction().moveToCommand(eff, sa);
        game.getEndOfTurn().addUntil(() -> game.getAction().exileEffect(eff));
    }

}
