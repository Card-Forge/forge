package forge.ai.ability;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Seek picks at random from the cards in the library that match its type, and SeekEffect skips a
 * type whose pool is empty after the cost has already been paid. Runecarved Obelisk taps and
 * sacrifices itself to seek, so seeking a type the library cannot supply throws the artifact away
 * for nothing.
 */
public class SeekAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        for (Player seeker : getSeekers(aiPlayer, sa)) {
            for (String seekType : getSeekTypes(sa)) {
                if (canFindSomething(seeker, seekType, sa)) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }
        }
        return new AiAbilityDecision(0, AiPlayDecision.DoesntImpactGame);
    }

    private static List<Player> getSeekers(Player aiPlayer, SpellAbility sa) {
        if (sa.usesTargeting()) {
            // targets aren't chosen yet at this point, so assume we can aim at ourselves
            return Lists.newArrayList(aiPlayer);
        }
        List<Player> defined = Lists.newArrayList();
        for (String def : sa.getParamOrDefault("Defined", "You").split(" & ")) {
            defined.addAll(AbilityUtils.getDefinedPlayers(sa.getHostCard(), def, sa));
        }
        return defined.isEmpty() ? Lists.newArrayList(aiPlayer) : defined;
    }

    private static List<String> getSeekTypes(SpellAbility sa) {
        if (sa.hasParam("Types")) {
            return Arrays.asList(sa.getParam("Types").split(","));
        }
        return Lists.newArrayList(sa.getParamOrDefault("Type", "Card"));
    }

    /**
     * Mirrors how SeekEffect builds its pool. Types that depend on what the cost sacrifices (such
     * as Spawning Pod's Creature.cmcEQX, where X comes from the sacrificed creature) cannot be
     * resolved before the cost is paid, so those are assumed findable rather than guessed at.
     */
    private static boolean canFindSomething(Player seeker, String seekType, SpellAbility sa) {
        final Card source = sa.getHostCard();
        CardCollection pool;
        if (sa.hasParam("DefinedCards")) {
            pool = AbilityUtils.getDefinedCards(source, sa.getParam("DefinedCards"), sa);
        } else {
            pool = new CardCollection(seeker.getCardsIn(ZoneType.Library));
        }
        if (pool.isEmpty()) {
            return false;
        }
        if (seekType.equals("Card")) {
            return true;
        }
        if (dependsOnCostPayment(seekType, sa)) {
            return true;
        }
        return !CardLists.getValidCards(pool, seekType, source.getController(), source, sa).isEmpty();
    }

    private static boolean dependsOnCostPayment(String seekType, SpellAbility sa) {
        for (String svar : new String[] { "X", "Y", "Z" }) {
            if (seekType.contains(svar) && sa.getSVar(svar).startsWith("Sacrificed$")) {
                return true;
            }
        }
        return false;
    }
}
