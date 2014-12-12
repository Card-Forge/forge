package forge.game.ability.effects;

import java.util.Arrays;
import java.util.List;

import forge.GameCommand;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.Ability;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class ControlGainEffect extends SpellAbilityEffect {
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> newController = getTargetPlayers(sa, "NewController");
        if (newController.isEmpty()) {
            newController.add(sa.getActivatingPlayer());
        }

        sb.append(newController).append(" gains control of");

        final CardCollectionView tgts = getDefinedCards(sa);
        if (tgts.isEmpty()) {
        	sb.append(" (nothing)");
        } else {
        	for (final Card c : getDefinedCards(sa)) {
        	    sb.append(" ");
        	    if (c.isFaceDown()) {
        	        sb.append("Face-down creature (").append(c.getId()).append(')');
        	    } else {
        	        sb.append(c);
        	    }
        	}
        }
        sb.append(".");

        return sb.toString();
    }

    private static void doLoseControl(final Card c, final Card host,
            final boolean tapOnLose, final List<String> addedKeywords,
            final long tStamp) {
        if (null == c || c.hasKeyword("Other players can't gain control of CARDNAME.")) {
            return;
        }
        if (c.isInPlay()) {
            c.removeTempController(tStamp);

            if (tapOnLose) {
                c.tap();
            }

            if (null != addedKeywords) {
                for (final String kw : addedKeywords) {
                    c.removeExtrinsicKeyword(kw);
                }
            }
        } // if
        host.removeGainControlTargets(c);
        
    }

    @Override
    public void resolve(SpellAbility sa) {
        Card source = sa.getHostCard();

        final boolean bUntap = sa.hasParam("Untap");
        final boolean bTapOnLose = sa.hasParam("TapOnLose");
        final boolean bNoRegen = sa.hasParam("NoRegen");
        final boolean remember = sa.hasParam("RememberControlled");
        final boolean forget = sa.hasParam("ForgetControlled");
        final List<String> destroyOn = sa.hasParam("DestroyTgt") ? Arrays.asList(sa.getParam("DestroyTgt").split(",")) : null;
        final List<String> kws = sa.hasParam("AddKWs") ? Arrays.asList(sa.getParam("AddKWs").split(" & ")) : null;
        final List<String> lose = sa.hasParam("LoseControl") ? Arrays.asList(sa.getParam("LoseControl").split(",")) : null;

        final List<Player> controllers = getDefinedPlayersOrTargeted(sa, "NewController");

        final Player newController = controllers.isEmpty() ? sa.getActivatingPlayer() : controllers.get(0);
        final Game game = newController.getGame();

        CardCollectionView tgtCards = getDefinedCards(sa);

        if (sa.hasParam("ControlledByTarget")) {
        	tgtCards = CardLists.filterControlledBy(tgtCards, getTargetPlayers(sa));
        } 

        // check for lose control criteria right away
        if (lose != null && lose.contains("LeavesPlay") && !source.isInZone(ZoneType.Battlefield)) {
            return;
        }
        if (lose != null && lose.contains("Untap") && !source.isTapped()) {
            return;
        }

        for (Card tgtC : tgtCards) {

            if (!tgtC.isInPlay() || !tgtC.canBeControlledBy(newController)) {
                continue;
            }

            if (!tgtC.equals(sa.getHostCard()) && !sa.getHostCard().getGainControlTargets().contains(tgtC)) {
                sa.getHostCard().addGainControlTarget(tgtC);
            }

            long tStamp = game.getNextTimestamp();
            if (lose != null) {
                tgtC.addTempController(newController, tStamp);
            } else {
                tgtC.setController(newController, tStamp);
            }

            if (bUntap) {
                tgtC.untap();
            }

            if (null != kws) {
                for (final String kw : kws) {
                    tgtC.addExtrinsicKeyword(kw);
                }
            }

            if (remember && !sa.getHostCard().isRemembered(tgtC)) {
                sa.getHostCard().addRemembered(tgtC);
            }

            if (forget && sa.getHostCard().isRemembered(tgtC)) {
                sa.getHostCard().removeRemembered(tgtC);
            }

            if (lose != null) {
                if (lose.contains("LeavesPlay")) {
                    sa.getHostCard().addLeavesPlayCommand(getLoseControlCommand(tgtC, tStamp, bTapOnLose, source, kws));
                }
                if (lose.contains("Untap")) {
                    sa.getHostCard().addUntapCommand(getLoseControlCommand(tgtC, tStamp, bTapOnLose, source, kws));
                }
                if (lose.contains("LoseControl")) {
                    sa.getHostCard().addChangeControllerCommand(getLoseControlCommand(tgtC, tStamp, bTapOnLose, source, kws));
                }
                if (lose.contains("EOT")) {
                    game.getEndOfTurn().addUntil(getLoseControlCommand(tgtC, tStamp, bTapOnLose, source, kws));
                    tgtC.setSVar("SacMe", "6");
                }
                if (lose.contains("StaticCommandCheck")) {
                    String leftVar = sa.getSVar(sa.getParam("StaticCommandCheckSVar"));
                    String rightVar = sa.getParam("StaticCommandSVarCompare");
                    sa.getHostCard().addStaticCommandList(new Object[]{leftVar, rightVar, tgtC,
                            getLoseControlCommand(tgtC, tStamp, bTapOnLose, source, kws)});
                }
            }

            if (destroyOn != null) {
                if (destroyOn.contains("LeavesPlay")) {
                    sa.getHostCard().addLeavesPlayCommand(getDestroyCommand(tgtC, source, bNoRegen));
                }
                if (destroyOn.contains("Untap")) {
                    sa.getHostCard().addUntapCommand(getDestroyCommand(tgtC, source, bNoRegen));
                }
                if (destroyOn.contains("LoseControl")) {
                    sa.getHostCard().addChangeControllerCommand(getDestroyCommand(tgtC, source, bNoRegen));
                }
            }

            game.getAction().controllerChangeZoneCorrection(tgtC);
        } // end foreach target
    }

    /**
     * <p>
     * getDestroyCommand.
     * </p>
     * 
     * @param i
     *            a int.
     * @return a {@link forge.GameCommand} object.
     */
    private static GameCommand getDestroyCommand(final Card c, final Card hostCard, final boolean bNoRegen) {
        final GameCommand destroy = new GameCommand() {
            private static final long serialVersionUID = 878543373519872418L;

            @Override
            public void run() {
                final Game game = hostCard.getGame();
                final Ability ability = new Ability(hostCard, ManaCost.ZERO) {
                    @Override
                    public void resolve() {

                        if (bNoRegen) {
                            game.getAction().destroyNoRegeneration(c, null);
                        } else {
                            game.getAction().destroy(c, null);
                        }
                    }
                };
                final StringBuilder sb = new StringBuilder();
                sb.append(hostCard).append(" - destroy ").append(c.getName()).append(".");
                if (bNoRegen) {
                    sb.append("  It can't be regenerated.");
                }
                ability.setStackDescription(sb.toString());

                game.getStack().addSimultaneousStackEntry(ability);
            }

        };
        return destroy;
    }

    /**
     * <p>
     * getLoseControlCommand.
     * </p>
     * 
     * @param i
     *            a int.
     * @param originalController
     *            a {@link forge.game.player.Player} object.
     * @return a {@link forge.GameCommand} object.
     */
    private static GameCommand getLoseControlCommand(final Card c,
            final long tStamp, final boolean bTapOnLose, final Card hostCard,
            final List<String> kws) {
        final GameCommand loseControl = new GameCommand() {
            private static final long serialVersionUID = 878543373519872418L;

            @Override
            public void run() { 
                doLoseControl(c, hostCard, bTapOnLose, kws, tStamp);
                c.getSVars().remove("SacMe");
            }
        };

        return loseControl;
    }

    private CardCollectionView getDefinedCards(final SpellAbility sa) {
        final Game game = sa.getHostCard().getGame();
        if (sa.hasParam("AllValid")) {
            return AbilityUtils.filterListByType(game.getCardsIn(ZoneType.Battlefield), sa.getParam("AllValid"), sa);
        }
        return getTargetCards(sa);
    }
}
