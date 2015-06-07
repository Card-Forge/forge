package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollection;
import forge.util.Lang;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import java.util.List;

public class ActivateAbilityEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        sb.append(StringUtils.join(tgtPlayers, ", "));
        sb.append(" activates ");
        sb.append(Lang.nounWithAmount(1, sa.hasParam("ManaAbility") ? "mana ability" : "ability"));
        sb.append(" of each ").append(sa.getParamOrDefault("Type", "Card"));
        sb.append(" he or she controls.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final boolean isManaAb = sa.hasParam("ManaAbility");
        // TODO: improve ai and fix corner cases

        for (final Player p : getTargetPlayers(sa)) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                List<Card> list = CardLists.getType(p.getCardsIn(ZoneType.Battlefield), sa.getParamOrDefault("Type", "Card")); 
                for (Card c : list) {
                    List<SpellAbility> possibleAb = Lists.newArrayList(c.getAllPossibleAbilities(p, true));
                    if (isManaAb) {
                        possibleAb.retainAll((FCollection<SpellAbility>)c.getManaAbilities());
                    }
                    if (possibleAb.isEmpty()) {
                        continue;
                    }
                    SpellAbility manaAb = p.getController().chooseSingleSpellForEffect(possibleAb, sa, "Choose a mana ability:");
                    p.getController().playChosenSpellAbility(manaAb);
                }
            }
        }
    }

}
