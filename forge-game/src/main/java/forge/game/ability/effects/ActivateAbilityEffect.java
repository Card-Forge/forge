package forge.game.ability.effects;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.collect.FCollection;

public class ActivateAbilityEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        sb.append(StringUtils.join(tgtPlayers, ", "));
        sb.append(" activates ");
        sb.append(Lang.nounWithAmount(1, sa.hasParam("ManaAbility") ? "mana ability" : "ability"));
        sb.append(" of each ").append(sa.getParamOrDefault("Type", "Card"));
        sb.append(" they control.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final boolean isManaAb = sa.hasParam("ManaAbility");
        // TODO: improve ai and fix corner cases

        for (final Player p : getTargetPlayers(sa)) {
            if (!p.isInGame()) {
                continue;
            }

            List<Card> list = CardLists.getType(p.getCardsIn(ZoneType.Battlefield), sa.getParamOrDefault("Type", "Card"));
            for (Card c : list) {
                List<SpellAbility> possibleAb = Lists.newArrayList(c.getAllPossibleAbilities(p, true));
                if (isManaAb) {
                    possibleAb.retainAll((FCollection<SpellAbility>)c.getManaAbilities());
                }
                if (possibleAb.isEmpty()) {
                    continue;
                }
                SpellAbility manaAb = p.getController().chooseSingleSpellForEffect(
                        possibleAb, sa, Localizer.getInstance().getMessage("lblChooseManaAbility"), ImmutableMap.of());
                p.getController().playChosenSpellAbility(manaAb);
            }
        }
    }

}
