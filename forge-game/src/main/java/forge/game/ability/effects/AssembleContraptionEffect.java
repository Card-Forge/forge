package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.replacement.ReplacementResult;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.util.Lang;

import java.util.List;
import java.util.Map;

public class AssembleContraptionEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        Card host = sa.getHostCard();

        String defaultAssembler = host.isCreature() ? "Self" : "You";
        String definedAssembler = sa.getParamOrDefault("DefinedAssembler", defaultAssembler);
        List<GameEntity> assemblers = AbilityUtils.getDefinedEntities(host, definedAssembler, sa);

        if(assemblers.isEmpty())
            return "";

        sb.append(Lang.joinHomogenous(assemblers));

        String definedContraption = sa.getParam("DefinedContraption");

        List<Card> tgtCards = definedContraption == null ? null : AbilityUtils.getDefinedCards(host, definedContraption, sa);
        if(tgtCards != null) {
            sb.append(Lang.joinVerb(tgtCards, sa.hasParam("Reassemble") ? " reassemble" : " assemble")).append(" ");
            sb.append(Lang.joinHomogenous(tgtCards)).append(".");
            return sb.toString();
        }

        int amount;
        if (sa.hasParam("Amount")) {
            String amountText = sa.getParam("Amount");
            if(amountText.equals("Result")) {
                //Used for Hard-Hat Area; Shouldn't actually display, usually overridden by a parent ability's trigger
                //description, but gets evaluated regardless and calculateAmount complains since Result isn't defined.
                sb.append(" assembles a number of Contraptions equal to the result.");
                return sb.toString();
            }
            amount = AbilityUtils.calculateAmount(sa.getHostCard(), amountText, sa);
        }
        else
            amount = 1;

        if (assemblers.size() > 1) {
            sb.append(" each");
        }
        sb.append(Lang.joinVerb(assemblers, " assemble")).append(" ");
        sb.append(amount == 1 ? "a Contraption." : (Lang.getNumeral(amount) + " Contraptions."));
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Card host = sa.getHostCard();
        final Game game = host.getGame();

        String defaultAssembler = host.isCreature() ? "Self" : "You";
        String definedAssembler = sa.getParamOrDefault("DefinedAssembler", defaultAssembler);
        List<GameEntity> assemblers = AbilityUtils.getDefinedEntities(host, definedAssembler, sa);

        if(assemblers.isEmpty())
            return;

        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        final CardZoneTable triggerList = AbilityKey.addCardZoneTableParams(moveParams, sa);

        String definedContraption = sa.getParam("DefinedContraption");
        if(definedContraption != null) {
            List<Card> tgtCards = AbilityUtils.getDefinedCards(host, definedContraption, sa);
            //Defined contraptions; (re)assemble them specifically.
            //This could be its own keyword, but it only shows up on two cards and works similarly.
            if (tgtCards.isEmpty())
                return;
            GameEntity assembler = assemblers.get(0);
            Player p = assembler instanceof Player ? (Player) assembler
                    : assembler instanceof Card ? ((Card) assembler).getController()
                    : null;
            if (p == null || !p.isInGame()) return;

            for (Card card : tgtCards) {
                boolean changedControllers = card.getController() != p;
                card.setController(p, game.getNextTimestamp());
                if (card.getZone().getZoneType() != ZoneType.Battlefield)
                    card.getGame().getAction().moveToPlay(card, sa, moveParams);

                if (sa.hasParam("Remember")) {
                    source.addRemembered(card);
                }

                if(changedControllers)
                    game.getAction().controllerChangeZoneCorrection(card);

                //Assign a sprocket. If reassembling, it needs to be a different sprocket than the current one.
                int sprocket = card.getController().getController().chooseSprocket(card, sa.hasParam("Reassemble"));
                card.setSprocket(sprocket);
            }
            triggerList.triggerChangesZoneAll(sa.getHostCard().getGame(), sa);
            return;
        }

        int amount = sa.hasParam("Amount") ? AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Amount"), sa) : 1;

        for (GameEntity assembler : assemblers) {
            Player p = assembler instanceof Player ? (Player) assembler
                    : assembler instanceof Card ? ((Card) assembler).getController()
                    : null;
            if (p == null || !p.isInGame()) continue;
            // Replacement effects
            Map<AbilityKey, Object> replaceMap = AbilityKey.mapFromAffected(p);
            replaceMap.put(AbilityKey.Player, p);
            replaceMap.put(AbilityKey.Cause, assembler);
            final PlayerZone contraptionDeck = p.getZone(ZoneType.ContraptionDeck);
            for (int i = 0; i < amount; i++) {
                if (game.getReplacementHandler().run(ReplacementType.AssembleContraption, replaceMap) != ReplacementResult.NotReplaced) {
                    continue;
                }
                if(contraptionDeck.isEmpty())
                    continue;
                Card contraption = contraptionDeck.get(0);
                contraption = p.getGame().getAction().moveToPlay(contraption, sa, moveParams);
                int sprocket = contraption.getController().getController().chooseSprocket(contraption);
                contraption.setSprocket(sprocket);
                if (sa.hasParam("Remember")) {
                    source.addRemembered(contraption);
                }
            }
        }
        triggerList.triggerChangesZoneAll(sa.getHostCard().getGame(), sa);
    }
}
