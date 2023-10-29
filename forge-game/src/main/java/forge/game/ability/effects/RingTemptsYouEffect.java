package forge.game.ability.effects;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.util.Localizer;

import java.util.Map;

public class RingTemptsYouEffect extends EffectEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return Localizer.getInstance().getMessage("lblTheRingTempts", sa.getActivatingPlayer());
    }

    @Override
    public void resolve(SpellAbility sa) {
        Player p = sa.getActivatingPlayer();
        Game game = p.getGame();
        Card card = sa.getHostCard();

        if (p.getTheRing() == null)
            p.createTheRing(card);

        //increment ring tempted you for property
        p.incrementRingTemptedYou();
        p.setRingLevel(p.getNumRingTemptedYou());

        // Then choose a ring-bearer (You may keep the same one). Auto pick if <2 choices.
        CardCollection creatures = p.getCreaturesInPlay();
        Card ringBearer = p.getController().chooseSingleEntityForEffect(creatures, sa, Localizer.getInstance().getMessageorUseDefault("lblChooseRingBearer", "Choose your Ring-bearer"), false, null);
        p.setRingBearer(ringBearer);

        // 701.52a That creature becomes your Ring-bearer until another player gains control of it.
        if (ringBearer != null) {
            GameCommand loseCommand = new GameCommand() {
                private static final long serialVersionUID = 1L;
                @Override
                public void run() {
                    if (ringBearer.isRingBearer()) {
                        p.clearRingBearer();
                    }
                }
            };
            ringBearer.addChangeControllerCommand(loseCommand);
            ringBearer.addLeavesPlayCommand(loseCommand);
        }

        // Run triggers
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(p);
        runParams.put(AbilityKey.Card, ringBearer);
        game.getTriggerHandler().runTrigger(TriggerType.RingTemptsYou, runParams, false);
    }
}
