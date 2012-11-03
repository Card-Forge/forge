package forge.card.abilityfactory.effects;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import forge.Card;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.gui.GuiChoose;

public class ChooseGenericEffect extends SpellEffect {    
    
    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
    
        for (final Player p : getTargetPlayers(sa, params)) {
            sb.append(p).append(" ");
        }
        sb.append("chooses from a list.");

        return sb.toString();
    }

    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final BiMap<String, String> choices = HashBiMap.create();
        for (String s : Arrays.asList(params.get("Choices").split(","))) {
            final HashMap<String, String> theseParams = AbilityFactory.getMapParams(host.getSVar(s), host);
            choices.put(s, theseParams.get("ChoiceDescription"));
        }

        final List<Player> tgtPlayers = getTargetPlayers(sa, params);

        final Target tgt = sa.getTarget();

        for (final Player p : tgtPlayers) {
            if (tgt != null && !p.canBeTargetedBy(sa)) {
                continue;
            }
            SpellAbility chosenSA = null;
            AbilityFactory afChoice = new AbilityFactory();
            if (p.isHuman()) {
                String choice = GuiChoose.one("Choose one", choices.values());
                chosenSA = afChoice.getAbility(host.getSVar(choices.inverse().get(choice)), host);
            } else { //Computer AI
                chosenSA = afChoice.getAbility(host.getSVar(params.get("Choices").split(",")[0]), host);
            }
            chosenSA.setActivatingPlayer(sa.getSourceCard().getController());
            ((AbilitySub) chosenSA).setParent(sa);
            AbilityFactory.resolve(chosenSA, false);
        }
    }

}