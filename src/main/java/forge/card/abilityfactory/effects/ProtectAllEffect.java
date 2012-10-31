package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import forge.Card;
import forge.CardLists;
import forge.Command;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class ProtectAllEffect extends SpellEffect { 
    /**
     * <p>
     * protectAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    
    @Override
    protected String getStackDescription(Map<String,String> params, SpellAbility sa) {
        final Card host = sa.getAbilityFactory().getHostCard();

        final StringBuilder sb = new StringBuilder();

        ArrayList<Card> tgtCards;
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (tgtCards.size() > 0) {

            if (sa instanceof AbilitySub) {
                sb.append(" ");
            } else {
                sb.append(host).append(" - ");
            }

            if (params.containsKey("SpellDescription")) {
                sb.append(params.get("SpellDescription"));
            } else {
                sb.append("Valid card gain protection");
                if (!params.containsKey("Permanent")) {
                    sb.append(" until end of turn");
                }
                sb.append(".");
            }
        }

        return sb.toString();
    } // protectStackDescription()

    /**
     * <p>
     * protectAllResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card host = sa.getAbilityFactory().getHostCard();

        final boolean isChoice = params.get("Gains").contains("Choice");
        final ArrayList<String> choices = AbilityFactory.getProtectionList(params);
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
            if (params.get("Gains").equals("ChosenColor")) {
                for (final String color : host.getChosenColor()) {
                    gains.add(color.toLowerCase());
                }
            } else {
                gains.addAll(choices);
            }
        }

        // Deal with permanents
        String valid = "";
        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }
        if (!valid.equals("")) {
            List<Card> list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
            list = CardLists.getValidCards(list, valid, sa.getActivatingPlayer(), host);

            for (final Card tgtC : list) {
                if (tgtC.isInPlay()) {
                    for (final String gain : gains) {
                        tgtC.addExtrinsicKeyword("Protection from " + gain);
                    }

                    if (!params.containsKey("Permanent")) {
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
                        if (params.containsKey("UntilEndOfCombat")) {
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
        if (params.containsKey("ValidPlayers")) {
            players = params.get("ValidPlayers");
        }
        if (!players.equals("")) {
            final ArrayList<Player> playerList = AbilityFactory.getDefinedPlayers(host, players, sa);
            for (final Player player : playerList) {
                for (final String gain : gains) {
                    player.addKeyword("Protection from " + gain);
                }

                if (!params.containsKey("Permanent")) {
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
                    if (params.containsKey("UntilEndOfCombat")) {
                        Singletons.getModel().getGame().getEndOfCombat().addUntil(untilEOT);
                    } else {
                        Singletons.getModel().getGame().getEndOfTurn().addUntil(untilEOT);
                    }
                }
            }
        }

    } // protectAllResolve()

} // end class AbilityFactory_Protection