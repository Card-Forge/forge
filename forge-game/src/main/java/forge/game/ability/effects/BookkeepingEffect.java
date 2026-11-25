package forge.game.ability.effects;

import java.util.List;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Keeps track of things for the effects of the same card that come later, and affects nothing in the game itself:
 * <ul>
 *   <li>{@code RememberObjects$} / {@code ForgetObjects$}: add to / remove from the host's remembered objects</li>
 *   <li>{@code RememberAll$ Valid | RememberZone$ Zone}: remember every card of the zone (the battlefield by default)
 *       that matches Valid</li>
 *   <li>{@code ImprintCards$} / {@code ForgetImprinted$}: add to / remove from the host's imprinted cards</li>
 *   <li>{@code NoteCards$ ... | NoteCardsFor$ Name}, {@code ClearNotedCardsFor$ Name,...} and {@code NoteNumber$}: notes the
 *       {@code Defined$} players keep</li>
 * </ul>
 * Scripts spelled these "Pump" (a pump with none of a pump's parameters), which made the AI and the engine treat them as
 * pumps. {@link TargetOnlyEffect} does the same bookkeeping when it also chooses a target.
 */
public class BookkeepingEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getParamOrDefault("StackDescription", "");
    }

    @Override
    public void resolve(final SpellAbility sa) {
        apply(sa);
    }

    /** The bookkeeping of the ability. */
    public static void apply(final SpellAbility sa) {
        final Card host = sa.getHostCard();

        if (sa.hasParam("RememberObjects")) {
            host.addRemembered(AbilityUtils.getDefinedObjects(host, sa.getParam("RememberObjects"), sa));
        }
        if (sa.hasParam("RememberAll")) {
            final List<ZoneType> zones = ZoneType.listValueOf(sa.getParamOrDefault("RememberZone", "Battlefield"));
            final CardCollectionView all = AbilityUtils.filterListByType(host.getGame().getCardsIn(zones),
                    sa.getParam("RememberAll"), sa);
            for (final Card c : all) {
                host.addRemembered(c);
            }
        }
        if (sa.hasParam("NoteCardsFor")) {
            for (final Card c : AbilityUtils.getDefinedCards(host, sa.getParam("NoteCards"), sa)) {
                for (final Player p : getTargetPlayers(sa)) {
                    p.addNoteForName(sa.getParam("NoteCardsFor"), "Id:" + c.getId());
                }
            }
        }
        if (sa.hasParam("ClearNotedCardsFor")) {
            for (final Player p : getTargetPlayers(sa)) {
                for (final String s : sa.getParam("ClearNotedCardsFor").split(",")) {
                    p.clearNotesForName(s);
                }
            }
        }
        if (sa.hasParam("NoteNumber")) {
            final int num = AbilityUtils.calculateAmount(host, sa.getParam("NoteNumber"), sa);
            for (final Player p : getTargetPlayers(sa)) {
                p.noteNumberForName(host.getName(), num);
            }
        }
        if (sa.hasParam("ForgetObjects")) {
            host.removeRemembered(AbilityUtils.getDefinedObjects(host, sa.getParam("ForgetObjects"), sa));
        }
        if (sa.hasParam("ImprintCards")) {
            host.addImprintedCards(AbilityUtils.getDefinedCards(host, sa.getParam("ImprintCards"), sa));
        }
        if (sa.hasParam("ForgetImprinted")) {
            host.removeImprintedCards(AbilityUtils.getDefinedCards(host, sa.getParam("ForgetImprinted"), sa));
        }
    }
}
