package forge.game.ability.effects;

import java.util.Map;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;

public class ManifestDreadEffect extends ManifestEffect {
    @Override
    protected void manifestLoop(SpellAbility sa, Player p, final int amount) {
        final Game game = p.getGame();
        for (int i = 0; i < amount; i++) {
            CardCollection tgtCards = p.getTopXCardsFromLibrary(2);
            CardCollection toGrave = new CardCollection();
            if (!tgtCards.isEmpty()) {
                Card manifest = p.getController().chooseSingleEntityForEffect(tgtCards, sa, getDefaultMessage(), null);
                tgtCards.remove(manifest);

                Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
                CardZoneTable triggerList = AbilityKey.addCardZoneTableParams(moveParams, sa);
                manifest = internalEffect(manifest, p, sa, moveParams);
                // CR 701.60a
                if (!manifest.isManifested()) {
                    tgtCards.add(manifest);
                }
                for (Card c : tgtCards) {
                    toGrave.add(game.getAction().moveToGraveyard(c, sa, moveParams));
                }
                triggerList.triggerChangesZoneAll(game, sa);
            }
            Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(p);
            runParams.put(AbilityKey.Cards, toGrave);
            game.getTriggerHandler().runTrigger(TriggerType.ManifestDread, runParams, true);
        }
    }
}
