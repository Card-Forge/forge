package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
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
        
        
        if (!player.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantExileCardAndEncodeOntoYouCreature", CardTranslation.getTranslatedName(host.getName())), null)) {
            return;
        }

        // move host card to exile
        Card movedCard = game.getAction().moveTo(ZoneType.Exile, host, sa);

        // choose a creature
        Card choice = player.getController().chooseSingleEntityForEffect(choices, sa, Localizer.getInstance().getMessage("lblChooseACreatureYouControlToEncode") + " ", true, null);

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
