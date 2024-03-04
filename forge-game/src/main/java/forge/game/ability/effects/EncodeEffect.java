package forge.game.ability.effects;

import java.util.Map;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.CardTranslation;
import forge.util.Localizer;

public class EncodeEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        if (sa.getHostCard().isToken()) {
            return "";
        }

        final StringBuilder sb = new StringBuilder();

        sb.append(sa.getActivatingPlayer());
        sb.append(" chooses a card to encode with Cipher.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();

        if (host.isToken()) {
            return;
        }

        CardCollectionView choices = host.getController().getCreaturesInPlay();

        // if no creatures on battlefield, cannot encoded
        if (choices.isEmpty()) {
            return;
        }

        // Handle choice of whether or not to encoded
        if (!activator.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantExileCardAndEncodeOntoYouCreature", CardTranslation.getTranslatedName(host.getName())), null)) {
            return;
        }

        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        CardZoneTable zoneMovements = AbilityKey.addCardZoneTableParams(moveParams, sa);

        Card moved = game.getAction().exile(host, sa, moveParams);

        zoneMovements.triggerChangesZoneAll(game, sa);

        Card choice = activator.getController().chooseSingleEntityForEffect(choices, sa, Localizer.getInstance().getMessage("lblChooseACreatureYouControlToEncode") + " ", false, null);

        if (choice == null) {
            return;
        }

        StringBuilder codeLog = new StringBuilder();
        codeLog.append("Encoding ").append(host.toString()).append(" to ").append(choice.toString());
        game.getGameLog().add(GameLogEntryType.STACK_RESOLVE, codeLog.toString());

        // store hostcard in encoded array
        choice.addEncodedCard(moved);
        moved.setEncodingCard(choice);
    }

}
