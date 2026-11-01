package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CounterEnumType;
import forge.game.card.sticker.AppliedSticker;
import forge.game.card.sticker.Sticker;
import forge.game.card.sticker.StickerKind;
import forge.game.card.sticker.StickerSheet;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

import org.apache.commons.lang3.StringUtils;

/**
 * CR 123.3. Optional: {@code Kind$}, {@code Optional$}, {@code MaxTickets$}, {@code NoTicketCost$}.
 * Records {@code StickersPlaced} and {@code StickerUniqueVowels} for sub-abilities.
 */
public class PutStickerEffect extends SpellAbilityEffect {

    private static final String PLACED = "StickersPlaced";
    private static final String VOWELS = "StickerUniqueVowels";

    @Override
    protected String getStackDescription(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(sa.getActivatingPlayer()).append(" puts a ");
        if (sa.hasParam("Kind")) {
            sb.append(sa.getParam("Kind").toLowerCase()).append(" ");
        }
        sb.append("sticker on ");
        if (sa.usesTargeting() || sa.hasParam("Defined")) {
            sb.append(StringUtils.join(getTargetCards(sa), ", "));
        } else {
            sb.append(sa.getParamOrDefault("ChoiceTitle", "a permanent they own").toLowerCase()
                    .replace("choose ", ""));
        }
        return sb.toString();
    }

    private static List<Card> chooseObjects(SpellAbility sa, Game game) {
        if (!sa.hasParam("Choices")) {
            return getTargetCards(sa);
        }
        Player chooser = sa.getActivatingPlayer();
        ZoneType zone = sa.hasParam("ChoiceZone")
                ? ZoneType.smartValueOf(sa.getParam("ChoiceZone")) : ZoneType.Battlefield;
        CardCollection pool = CardLists.getValidCards(game.getCardsIn(zone), sa.getParam("Choices"),
                chooser, sa.getHostCard(), sa);
        // CR 123.3b
        pool = CardLists.filter(pool, CardPredicates.isOwner(chooser));
        if (pool.isEmpty()) {
            return new CardCollection();
        }
        String prompt = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle")
                : "Choose a permanent to put a sticker on";
        Card chosen = chooser.getController().chooseSingleEntityForEffect(pool, sa, prompt,
                sa.hasParam("Optional"), null);
        CardCollection result = new CardCollection();
        if (chosen != null) {
            result.add(chosen);
        }
        return result;
    }

    public static List<Sticker> availableStickers(SpellAbility sa, Player owner) {
        // CR 123.3c
        int affordable = sa.hasParam("NoTicketCost") ? Integer.MAX_VALUE
                : owner.getCounters(CounterEnumType.TICKET);
        if (sa.hasParam("MaxTickets")) {
            affordable = Math.min(affordable,
                    AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("MaxTickets"), sa));
        }
        final StickerKind only = sa.hasParam("Kind") ? StickerKind.smartValueOf(sa.getParam("Kind")) : null;
        List<Sticker> options = new ArrayList<>();
        for (Sticker s : StickerSheet.getAvailableStickers(owner, affordable)) {
            if (only == null || s.getKind() == only) {
                options.add(s);
            }
        }
        return options;
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Game game = sa.getActivatingPlayer().getGame();
        final boolean optional = sa.hasParam("Optional");
        sa.setSVar(VOWELS, "0");
        sa.setSVar(PLACED, "0");
        int placed = 0;

        for (final Card target : chooseObjects(sa, game)) {
            if (chooseAndPlaceSticker(sa, target, optional)) {
                sa.setSVar(PLACED, Integer.toString(++placed));
            }
        }
    }

    public static boolean chooseAndPlaceSticker(SpellAbility sa, Card target, boolean optional) {
        final Game game = sa.getActivatingPlayer().getGame();
        final Player owner = target.getOwner();
        // CR 123.3b
        if (owner == null || !owner.equals(sa.getActivatingPlayer())) {
            return false;
        }
        if (!game.getCardState(target, null).equalsWithGameTimestamp(target)) {
            return false;
        }

        final List<Sticker> options = availableStickers(sa, owner);
        if (options.isEmpty()) {
            return false;
        }
        Sticker chosen = owner.getController().chooseSticker(options, target, sa, optional);
        if (chosen == null) {
            return false;
        }

        if (chosen.getTickets() > 0 && !sa.hasParam("NoTicketCost")) {
            owner.subtractCounter(CounterEnumType.TICKET, chosen.getTickets(), owner);
        }

        int position = 0;
        if (chosen.getKind() == StickerKind.NAME) {
            position = owner.getController().chooseStickerNamePosition(chosen, target);
            sa.setSVar(VOWELS, Integer.toString(chosen.getUniqueVowelCount()));
        }
        target.addSticker(new AppliedSticker(chosen, game.getNextTimestamp(), position));


        final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
        runParams.put(AbilityKey.Card, target);
        runParams.put(AbilityKey.Player, owner);
        runParams.put(AbilityKey.StickerKind, chosen.getKind());
        game.getTriggerHandler().runTrigger(TriggerType.StickerPlaced, runParams, false);
        return true;
    }
}
