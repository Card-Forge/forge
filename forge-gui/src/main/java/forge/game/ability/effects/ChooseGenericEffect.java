package forge.game.ability.effects;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.gui.GuiChoose;
import forge.util.Aggregates;

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
        final Card host = sa.getSourceCard();
        final BiMap<String, String> choices = HashBiMap.create();
        for (String s : Arrays.asList(sa.getParam("Choices").split(","))) {
            final Map<String, String> theseParams = AbilityFactory.getMapParams(host.getSVar(s));
            choices.put(s, theseParams.get("ChoiceDescription").replace("CARDNAME", host.getName()));
        }

        final List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);

        final TargetRestrictions tgt = sa.getTargetRestrictions();

        for (final Player p : tgtPlayers) {
            if (tgt != null && !p.canBeTargetedBy(sa)) {
                continue;
            }
            SpellAbility chosenSA = null;
            String choice;
            if (sa.hasParam("AtRandom")) {
                choice = Aggregates.random(choices.keySet());
            } else {
                if (p.isHuman()) {
                    choice = choices.inverse().get(GuiChoose.one("Choose one", choices.values()));
                } else { //Computer AI
                    if ("Random".equals(sa.getParam("AILogic"))) {
                        choice = Aggregates.random(choices.keySet());
                    } else {
                        choice = sa.getParam("Choices").split(",")[0];
                    }
                }
            }
            chosenSA = AbilityFactory.getAbility(host.getSVar(choice), host);
            if (sa.hasParam("ShowChoice")) {
                p.getGame().getAction().nofityOfValue(sa, p, choices.get(choice), null);
            }
            chosenSA.setActivatingPlayer(sa.getSourceCard().getController());
            ((AbilitySub) chosenSA).setParent(sa);
            AbilityUtils.resolve(chosenSA);
        }
    }

}
