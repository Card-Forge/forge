package forge.game.ability.effects;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.event.GameEventCardModeChosen;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.util.MyRandom;

import java.util.List;

import com.google.common.collect.Lists;

public class ChooseGenericEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        for (final Player p : getTargetPlayers(sa)) {
            sb.append(p).append(" ");
        }
        sb.append("chooses from a list.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();

        final List<SpellAbility> abilities = Lists.<SpellAbility>newArrayList(sa.getAdditionalAbilityList("Choices"));
        
        final List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        for (final Player p : tgtPlayers) {
            if (tgt != null && sa.getTargets().isTargeting(p) && !p.canBeTargetedBy(sa)) {
                continue;
            }

            SpellAbility chosenSA = null;
            if (sa.hasParam("AtRandom")) {
                int idxChosen = MyRandom.getRandom().nextInt(abilities.size());
                chosenSA = abilities.get(idxChosen);
            } else {
                chosenSA = p.getController().chooseSingleSpellForEffect(abilities, sa, "Choose one");
            }
            String chosenValue = chosenSA.getDescription();
            if (sa.hasParam("ShowChoice")) {
                boolean dontNotifySelf = sa.getParam("ShowChoice").equals("ExceptSelf");
                p.getGame().getAction().nofityOfValue(sa, p, chosenValue, dontNotifySelf ? sa.getActivatingPlayer() : null);
            }
            if (sa.hasParam("SetChosenMode")) {
                sa.getHostCard().setChosenMode(chosenValue);
            }
            p.getGame().fireEvent(new GameEventCardModeChosen(p, host.getName(), chosenValue, sa.hasParam("ShowChoice")));
            AbilityUtils.resolve(chosenSA);
        }
    }

}
