package forge.game.ability.effects;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.event.GameEventCardModeChosen;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.MyRandom;

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

        final List<SpellAbility> abilities = Lists.newArrayList(sa.getAdditionalAbilityList("Choices"));
        final SpellAbility fallback = sa.getAdditionalAbility("FallbackAbility");
        
        final List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);

        for (final Player p : tgtPlayers) {
            // determine if any of the choices are not valid
            List<SpellAbility> saToRemove = Lists.newArrayList();
            
            for (SpellAbility saChoice : abilities) {
                if (!saChoice.getRestrictions().checkOtherRestrictions(host, saChoice, sa.getActivatingPlayer()) ) {
                    saToRemove.add(saChoice);
                } else if (saChoice.hasParam("UnlessCost") &&
                        "Player.IsRemembered".equals(saChoice.getParam("Defined"))) {
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
        
            if (sa.usesTargeting() && sa.getTargets().isTargeting(p) && !p.canBeTargetedBy(sa)) {
                continue;
            }

            SpellAbility chosenSA = null;
            if (sa.hasParam("AtRandom")) {
                int idxChosen = MyRandom.getRandom().nextInt(abilities.size());
                chosenSA = abilities.get(idxChosen);
            } else {
                chosenSA = p.getController().chooseSingleSpellForEffect(abilities, sa, "Choose one",
                        ImmutableMap.of());
            }
            
            if (chosenSA != null) {
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
            } else {
                // no choices are valid, e.g. maybe all Unless costs are unpayable
                if (fallback != null) {
                    p.getGame().fireEvent(new GameEventCardModeChosen(p, host.getName(), fallback.getDescription(), sa.hasParam("ShowChoice")));
                    AbilityUtils.resolve(fallback);                
                } else {
                    System.err.println("Warning: all Unless costs were unpayable for " + host.getName() +", but it had no FallbackAbility defined. Doing nothing (this is most likely incorrect behavior).");
                }
            }
        }
    }

}
