package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.event.GameEventCardModeChosen;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
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
        
        final List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);

        for (final Player p : tgtPlayers) {
            // determine if any of the choices are not valid
            List<SpellAbility> saToRemove = Lists.newArrayList();
            
            for (SpellAbility saChoice : abilities) {
                if (!saChoice.getRestrictions().checkOtherRestrictions(host, saChoice, sa.getActivatingPlayer()) ) {
                    saToRemove.add(saChoice);
                } else if (saChoice.hasParam("UnlessCost")) {
                    String unlessCost = saChoice.getParam("UnlessCost");
                    // Sac a permanent in presence of Sigarda, Host of Herons
                    // TODO: generalize this by testing if the unless cost can be paid
                    if (unlessCost.startsWith("Sac<")) {
                        if (!p.canSacrificeBy(saChoice)) {
                            saToRemove.add(saChoice);
                        }
                    } else if (unlessCost.startsWith("Discard<")) {
                        if (!p.canDiscardBy(sa)) {
                            saToRemove.add(saChoice);
                        }
                    }
                }
            }
            abilities.removeAll(saToRemove);
        
            if (sa.usesTargeting() && sa.getTargets().contains(p) && !p.canBeTargetedBy(sa)) {
                continue;
            }

            List<SpellAbility> chosenSAs = Lists.newArrayList();
            String prompt = sa.getParamOrDefault("ChoicePrompt","Choose");
            if (sa.hasParam("AtRandom")) {
                Aggregates.random(abilities, amount, chosenSAs);
            } else {
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
                    p.getGame().fireEvent(new GameEventCardModeChosen(p, host.getName(), chosenValue, sa.hasParam("ShowChoice")));
                    AbilityUtils.resolve(chosenSA);
                }
            } else {
                // no choices are valid, e.g. maybe all Unless costs are unpayable
                if (fallback != null) {
                    p.getGame().fireEvent(new GameEventCardModeChosen(p, host.getName(), fallback.getDescription(), sa.hasParam("ShowChoice")));
                    AbilityUtils.resolve(fallback);                
                } else if (!sa.hasParam("AtRandom")) {
                    System.err.println("Warning: all Unless costs were unpayable for " + host.getName() +", but it had no FallbackAbility defined. Doing nothing (this is most likely incorrect behavior).");
                }
            }
        }
    }

}
