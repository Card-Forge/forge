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
            Card manifest = null;
            Card toGrave = null;
            if (!tgtCards.isEmpty()) {
                manifest = p.getController().chooseSingleEntityForEffect(tgtCards, sa, getDefaultMessage(), null);
                tgtCards.remove(manifest);
                toGrave = tgtCards.isEmpty() ? null : tgtCards.getFirst();

                // CR 701.34d If an effect instructs a player to manifest multiple cards from their library, those cards are manifested one at a time.
                Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
                CardZoneTable triggerList = AbilityKey.addCardZoneTableParams(moveParams, sa);
                internalEffect(manifest, p, sa, moveParams);
                if (toGrave != null) {
                    toGrave = game.getAction().moveToGraveyard(toGrave, sa, moveParams);
                }
                triggerList.triggerChangesZoneAll(game, sa);
            }
            Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(p);
            runParams.put(AbilityKey.Card, toGrave);
            game.getTriggerHandler().runTrigger(TriggerType.ManifestDread, runParams, true);
        }
    }
}
