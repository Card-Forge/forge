package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.event.GameEventCardModeChosen;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Aggregates;
import forge.util.Lang;

public class ChooseGenericEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        sb.append(Lang.joinHomogenous(getDefinedPlayersOrTargeted(sa)));
        sb.append("chooses from a list.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();

        final List<SpellAbility> abilities = Lists.newArrayList(sa.getAdditionalAbilityList("Choices"));
        if (sa.hasParam("NumRandomChoices")) {
            int n = AbilityUtils.calculateAmount(host, sa.getParam("NumRandomChoices"), sa);
            while (abilities.size() > n) {
                Aggregates.removeRandom(abilities);
            }
        }
        final SpellAbility fallback = sa.getAdditionalAbility("FallbackAbility");
        final int amount = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("ChoiceAmount", "1"), sa);

        for (Player p : getDefinedPlayersOrTargeted(sa)) {
            if (!p.isInGame()) {
                p = getNewChooser(sa, sa.getActivatingPlayer(), p);
            }
            // determine if any of the choices are not valid
            List<SpellAbility> saToRemove = Lists.newArrayList();

            for (SpellAbility saChoice : abilities) {
                if (saChoice.getRestrictions() != null && !saChoice.getRestrictions().checkOtherRestrictions(host, saChoice, sa.getActivatingPlayer())) {
                    saToRemove.add(saChoice);
                } else if (saChoice.hasParam("UnlessCost")) {
                    // generic check for if the cost can be paid
                    Cost unlessCost = new Cost(saChoice.getParam("UnlessCost"), false);
                    if (!unlessCost.canPay(sa, p, true)) {
                        saToRemove.add(saChoice);
                    }
                }
            }
            abilities.removeAll(saToRemove);

            List<SpellAbility> chosenSAs = Lists.newArrayList();
            String prompt = sa.getParamOrDefault("ChoicePrompt", "Choose");
            boolean random = false;

            if (sa.hasParam("AtRandom")) {
                random = true;
                chosenSAs = Aggregates.random(abilities, amount);

                int i = 0;
                while (sa.getParam("AtRandom").equals("Urza") && i < chosenSAs.size()) {
                    if (!chosenSAs.get(i).usesTargeting()) {
                        i++;
                    } else if (sa.getTargetRestrictions().hasCandidates(chosenSAs.get(i))) {
                        p.getController().chooseTargetsFor(chosenSAs.get(i));
                        i++;
                    } else {
                        chosenSAs.set(i, Aggregates.random(abilities));
                    }
                }
            } else if (!abilities.isEmpty()) {
                chosenSAs = p.getController().chooseSpellAbilitiesForEffect(abilities, sa, prompt, amount, ImmutableMap.of());
            }

            if (!chosenSAs.isEmpty()) {
                for (SpellAbility chosenSA : chosenSAs) {
                    String chosenValue = chosenSA.getDescription();
                    if (sa.hasParam("ShowChoice")) {
                        boolean dontNotifySelf = sa.getParam("ShowChoice").equals("ExceptSelf");
                        p.getGame().getAction().notifyOfValue(sa, p, chosenValue, dontNotifySelf ? p : null);
                    }
                    if (sa.hasParam("SetChosenMode")) {
                        sa.getHostCard().setChosenMode(chosenValue);
                    }
                    p.getGame().fireEvent(new GameEventCardModeChosen(p, host.getName(), chosenValue,
                            sa.hasParam("ShowChoice"), random));
                    AbilityUtils.resolve(chosenSA);
                }
            } else {
                // no choices are valid, e.g. maybe all Unless costs are unpayable
                if (fallback != null) {
                    p.getGame().fireEvent(new GameEventCardModeChosen(p, host.getName(), fallback.getDescription(),
                            sa.hasParam("ShowChoice"), random));
                    AbilityUtils.resolve(fallback);
                } else if (!random) {
                    System.err.println("Warning: all Unless costs were unpayable for " + host.getName() +", but it had no FallbackAbility defined. Doing nothing (this is most likely incorrect behavior).");
                }
            }
        }
    }

}
