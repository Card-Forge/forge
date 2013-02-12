package forge.card.ability.effects;

import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.CardPredicates.Presets;
import forge.card.ability.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.GuiDialog;

public class EncodeEffect extends SpellEffect {
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
        final Card host = sa.getSourceCard();
        final Player player = sa.getActivatingPlayer();

        // make list of creatures that controller has on Battlefield
        List<Card> choices = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        choices = CardLists.getValidCards(choices, "Creature.YouCtrl", host.getController(), host);

        // if no creatures on battlefield, cannot encoded
        if (choices.size() == 0) {

        }
        // Handle choice of whether or not to encoded
        final StringBuilder sb = new StringBuilder();
        sb.append("Do you want to exile " + host + " and encode it onto a creature you control?");
        if (player.isHuman()
                && !GuiDialog.confirm(host, sb.toString())) {
            return;
        }
        // Note: AI will always choose to encode
        // TODO add better AI choice here

        // move host card to exile
        Card movedCard = Singletons.getModel().getGame().getAction().moveTo(ZoneType.Exile, host);

        // choose a creature
        Card choice = null;
        if (player.isHuman()) {
            final String choiceTitle = "Choose a creature you control to encode ";
            choice = GuiChoose.oneOrNone(choiceTitle, choices);
        }
        else { // Computer
            // TODO: move this to AI method
            String logic = sa.getParam("AILogic");
            if (logic == null) {
                // Base Logic is choose "best"
                choice = CardFactoryUtil.getBestAI(choices);
            } else if ("WorstCard".equals(logic)) {
                choice = CardFactoryUtil.getWorstAI(choices);
            } else if (logic.equals("BestBlocker")) {
                if (!CardLists.filter(choices, Presets.UNTAPPED).isEmpty()) {
                    choices = CardLists.filter(choices, Presets.UNTAPPED);
                }
                choice = CardFactoryUtil.getBestCreatureAI(choices);
            } else if (logic.equals("Clone")) {
                if (!CardLists.getValidCards(choices, "Permanent.YouDontCtrl,Permanent.nonLegendary", host.getController(), host).isEmpty()) {
                    choices = CardLists.getValidCards(choices, "Permanent.YouDontCtrl,Permanent.nonLegendary", host.getController(), host);
                }
                choice = CardFactoryUtil.getBestAI(choices);
            }
        }
        if (choice == null) {
          return;
        }

        // store hostcard in encoded array
        choice.addEncoded(movedCard);

        // add trigger
        final int numEncoded = choice.getEncoded().size();
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
