package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import forge.Card;
import forge.CardLists;
import forge.Command;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class ProtectAllEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Card> tgtCards = getTargetCards(sa);

        if (tgtCards.size() > 0) {
            sb.append("Valid card gain protection");
            if (!sa.hasParam("Permanent")) {
                sb.append(" until end of turn");
            }
            sb.append(".");
        }

        return sb.toString();
    } // protectStackDescription()

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getSourceCard();

        final boolean isChoice = sa.getParam("Gains").contains("Choice");
        final ArrayList<String> choices = AbilityUtils.getProtectionList(sa);
        final ArrayList<String> gains = new ArrayList<String>();
        if (isChoice) {
            if (sa.getActivatingPlayer().isHuman()) {
                final String choice = GuiChoose.one("Choose a protection", choices);
                if (null == choice) {
                    return;
                }
                gains.add(choice);
            } else {
                // TODO - needs improvement
                final String choice = choices.get(0);
                gains.add(choice);
                JOptionPane.showMessageDialog(null, "Computer chooses " + gains, "" + host, JOptionPane.PLAIN_MESSAGE);
            }
        } else {
            if (sa.getParam("Gains").equals("ChosenColor")) {
                for (final String color : host.getChosenColor()) {
                    gains.add(color.toLowerCase());
                }
            } else {
                gains.addAll(choices);
            }
        }

        // Deal with permanents
        String valid = "";
        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }
        if (!valid.equals("")) {
            List<Card> list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
            list = CardLists.getValidCards(list, valid, sa.getActivatingPlayer(), host);

            for (final Card tgtC : list) {
                if (tgtC.isInPlay()) {
                    for (final String gain : gains) {
                        tgtC.addExtrinsicKeyword("Protection from " + gain);
                    }

                    if (!sa.hasParam("Permanent")) {
                        // If not Permanent, remove protection at EOT
                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = -6573962672873853565L;

                            @Override
                            public void execute() {
                                if (tgtC.isInPlay()) {
                                    for (final String gain : gains) {
                                        tgtC.removeExtrinsicKeyword("Protection from " + gain);
                                    }
                                }
                            }
                        };
                        if (sa.hasParam("UntilEndOfCombat")) {
                            Singletons.getModel().getGame().getEndOfCombat().addUntil(untilEOT);
                        } else {
                            Singletons.getModel().getGame().getEndOfTurn().addUntil(untilEOT);
                        }
                    }
                }
            }
        }

        // Deal with Players
        String players = "";
        if (sa.hasParam("ValidPlayers")) {
            players = sa.getParam("ValidPlayers");
        }
        if (!players.equals("")) {
            final List<Player> playerList = AbilityUtils.getDefinedPlayers(host, players, sa);
            for (final Player player : playerList) {
                for (final String gain : gains) {
                    player.addKeyword("Protection from " + gain);
                }

                if (!sa.hasParam("Permanent")) {
                    // If not Permanent, remove protection at EOT
                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = -6573962672873853565L;

                        @Override
                        public void execute() {
                            for (final String gain : gains) {
                                player.removeKeyword("Protection from " + gain);
                            }
                        }
                    };
                    if (sa.hasParam("UntilEndOfCombat")) {
                        Singletons.getModel().getGame().getEndOfCombat().addUntil(untilEOT);
                    } else {
                        Singletons.getModel().getGame().getEndOfTurn().addUntil(untilEOT);
                    }
                }
            }
        }

    } // protectAllResolve()

}
