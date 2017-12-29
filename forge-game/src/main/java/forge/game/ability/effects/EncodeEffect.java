package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

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
        final Player player = sa.getActivatingPlayer();
        final Game game = player.getGame();

        if (host.isToken()) {
            return;
        }
        
        // make list of creatures that controller has on Battlefield
        CardCollectionView choices = host.getController().getCreaturesInPlay();

        // if no creatures on battlefield, cannot encoded
        if (choices.isEmpty()) {
            return;
        }
        // Handle choice of whether or not to encoded
        
        
        final StringBuilder sb = new StringBuilder();
        sb.append("Do you want to exile " + host + " and encode it onto a creature you control?");
        if (!player.getController().confirmAction(sa, null, sb.toString())) {
            return;
        }

        // move host card to exile
        Card movedCard = game.getAction().moveTo(ZoneType.Exile, host, sa);

        // choose a creature
        Card choice = player.getController().chooseSingleEntityForEffect(choices, sa, "Choose a creature you control to encode ", true);

        if (choice == null) {
          return;
        }

        StringBuilder codeLog = new StringBuilder();
        codeLog.append("Encoding ").append(host.toString()).append(" to ").append(choice.toString());
        game.getGameLog().add(GameLogEntryType.STACK_RESOLVE, codeLog.toString());

        // store hostcard in encoded array
        choice.addEncodedCard(movedCard);
        movedCard.setEncodingCard(choice);

        return;

    }

}
