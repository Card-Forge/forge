package forge.card.abilityfactory.effects;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
        final Card host = sa.getSourceCard();
        final BiMap<String, String> choices = HashBiMap.create();
        for (String s : Arrays.asList(sa.getParam("Choices").split(","))) {
            final Map<String, String> theseParams = AbilityFactory.getMapParams(host.getSVar(s));
            choices.put(s, theseParams.get("ChoiceDescription"));
        }

        final List<Player> tgtPlayers = getTargetPlayers(sa);

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
                chosenSA = afChoice.getAbility(host.getSVar(sa.getParam("Choices").split(",")[0]), host);
            }
            chosenSA.setActivatingPlayer(sa.getSourceCard().getController());
            ((AbilitySub) chosenSA).setParent(sa);
            AbilityFactory.resolve(chosenSA, false);
        }
    }

}
