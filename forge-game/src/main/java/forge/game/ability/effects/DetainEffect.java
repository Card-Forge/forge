package forge.game.ability.effects;

import java.util.EnumSet;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.zone.ZoneType;

public class DetainEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        Card source = sa.getHostCard();
        final Game game = source.getGame();

        CardCollection list = getTargetCards(sa);

        if (list.isEmpty()) {
            return;
        }
        Card eff = createEffect(sa, sa.getActivatingPlayer(), "Detain Effect", source.getImageKey());
        eff.addRemembered(list);

        // Add forgot trigger
        addForgetOnMovedTrigger(eff, "Battlefield");

        StaticAbility stAb = eff.addStaticAbility("Mode$ CantAttack,CantBlock,CantBeActivated | ValidCard$ Card.IsRemembered | Description$ Remembered can't attack or block and its activated abilities can't be activated.");
        stAb.setActiveZone(EnumSet.of(ZoneType.Command));
        stAb.setIntrinsic(true);

        addUntilCommand(sa, exileEffectCommand(game, eff), "UntilYourNextTurn", sa.getActivatingPlayer());

        game.getAction().moveToCommand(eff, sa);
    }

}
