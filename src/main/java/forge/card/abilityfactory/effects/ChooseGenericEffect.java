package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

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
    
        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        } else {
            sb.append(" ");
        }
    
        if (params.containsKey("StackDescription")) {
            sb.append(params.get("StackDescription"));
        }
        else {
            ArrayList<Player> tgtPlayers;
    
            final Target tgt = sa.getTarget();
            if (tgt != null) {
                tgtPlayers = tgt.getTargetPlayers();
            } else {
                tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
            }
    
            for (final Player p : tgtPlayers) {
                sb.append(p).append(" ");
            }
            sb.append("chooses from a list.");
        }

    
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

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (!params.containsKey("Defined")) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

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