package forge.game.ability.effects;

import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.event.GameEventCardModeChosen;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.util.MyRandom;

import java.util.ArrayList;
import java.util.List;

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
        final String[] choices = sa.getParam("Choices").split(",");
        final List<SpellAbility> abilities = new ArrayList<SpellAbility>();
         
        for (String s : choices) {
            abilities.add(AbilityFactory.getAbility(host.getSVar(s), host));
        }
        
        final List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        for (final Player p : tgtPlayers) {
            if (tgt != null && sa.getTargets().isTargeting(p) && !p.canBeTargetedBy(sa)) {
                continue;
            }

            int idxChosen = 0;
            String chosenName;
            if (sa.hasParam("AtRandom")) {
                idxChosen = MyRandom.getRandom().nextInt(choices.length);
                chosenName = choices[idxChosen];
            } else {
                SpellAbility saChosen = p.getController().chooseSingleSpellForEffect(abilities, sa, "Choose one");
                idxChosen = abilities.indexOf(saChosen);
                chosenName = choices[idxChosen];
            }
            SpellAbility chosenSA = AbilityFactory.getAbility(host.getSVar(chosenName), host);
            String chosenValue = abilities.get(idxChosen).getDescription();
            if (sa.hasParam("ShowChoice")) {
                boolean dontNotifySelf = sa.getParam("ShowChoice").equals("ExceptSelf");
                p.getGame().getAction().nofityOfValue(sa, p, chosenValue, dontNotifySelf ? sa.getActivatingPlayer() : null);
            }
            chosenSA.setActivatingPlayer(sa.getActivatingPlayer());
            ((AbilitySub) chosenSA).setParent(sa);
            p.getGame().fireEvent(new GameEventCardModeChosen(p, host.getName(), chosenValue, sa.hasParam("ShowChoice")));
            AbilityUtils.resolve(chosenSA);
        }
    }

}
