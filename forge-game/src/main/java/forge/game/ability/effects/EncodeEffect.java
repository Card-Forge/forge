package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.zone.ZoneType;

public class EncodeEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        for (final Player p : getTargetPlayers(sa)) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a card to encode with Cipher.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player player = sa.getActivatingPlayer();
        final Game game = player.getGame();

        // make list of creatures that controller has on Battlefield
        CardCollectionView choices = game.getCardsIn(ZoneType.Battlefield);
        choices = CardLists.getValidCards(choices, "Creature.YouCtrl", host.getController(), host);

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
        Card movedCard = game.getAction().moveTo(ZoneType.Exile, host);

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

        // add trigger
        final int numEncoded = choice.getEncodedCards().size();
        final StringBuilder cipherTrigger = new StringBuilder();
        cipherTrigger.append("Mode$ DamageDone | ValidSource$ Card.Self | ValidTarget$ Player | Execute$ PlayEncoded").append(numEncoded);
        cipherTrigger.append(" | CombatDamage$ True | OptionalDecider$ You | TriggerDescription$ ");
        cipherTrigger.append("Whenever CARDNAME deals combat damage to a player, its controller may cast a copy of ");
        cipherTrigger.append(movedCard).append(" without paying its mana cost.");
        final String abName = "PlayEncoded" + numEncoded;
        final String abString = "AB$ Play | Cost$ 0 | Encoded$ " + numEncoded + " | WithoutManaCost$ True | CopyCard$ True";
        final Trigger parsedTrigger = TriggerHandler.parseTrigger(cipherTrigger.toString(), choice, false);
        choice.addTrigger(parsedTrigger);
        choice.setSVar(abName, abString);
        return;

    }

}
