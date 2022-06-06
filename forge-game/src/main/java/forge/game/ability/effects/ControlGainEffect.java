package forge.game.ability.effects;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.event.GameEventCombatChanged;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

public class ControlGainEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> newController = getDefinedPlayersOrTargeted(sa, "NewController");
        if (newController.isEmpty()) {
            newController.add(sa.getActivatingPlayer());
        }

        sb.append(newController.get(0)).append(" gains control of");

        final CardCollectionView tgts = getDefinedCards(sa);
        if (tgts.isEmpty()) {
        	sb.append(" (nothing)");
        } else {
            for (final Card c : tgts) {
                sb.append(" ");
                if (c.isFaceDown()) {
                    sb.append("Face-down creature (").append(c.getId()).append(')');
                } else {
                    sb.append(c);
                }
            }
        }
        if (sa.hasParam("LoseControl")) {
            String loseCont = sa.getParam("LoseControl");
            if (loseCont.contains("EOT")) {
                sb.append(" until end of turn");
            } else if (loseCont.contains("Untap")) {
                sb.append(" for as long as ").append(sa.getHostCard()).append(" remains tapped");
            } else if (loseCont.contains("LoseControl")) {
                sb.append(" for as long as you control ").append(sa.getHostCard());
            } else if (loseCont.contains("LeavesPlay")) {
                sb.append(" for as long as ").append(sa.getHostCard()).append( "remains on the battlefield");
            } else if (loseCont.equals("StaticCommandCheck")) {
                sb.append(" for as long as that creature remains enchanted");
            } else if (loseCont.equals("UntilTheEndOfYourNextTurn")) {
                sb.append(" until the end of your next turn");
            }
        }
        sb.append(".");

        if (sa.hasParam("Untap")) {
            sb.append(" Untap it.");
        }
        final List<String> keywords = sa.hasParam("AddKWs") ? Arrays.asList(sa.getParam("AddKWs").split(" & ")) : null;
        if (sa.hasParam("AddKWs")) {
            sb.append(" It gains ");
            for (int i = 0; i < keywords.size(); i++) {
                sb.append(keywords.get(i).toLowerCase());
                sb.append(keywords.size() > 2 && i+1 != keywords.size() ? ", " : "");
                sb.append(keywords.size() == 2 && i == 0 ? " " : "");
                sb.append(i+2 == keywords.size() ? "and " : "");
            }
            sb.append(" until end of turn.");
        }


        return sb.toString();
    }

    private static void doLoseControl(final Card c, final Card host,
            final boolean tapOnLose, final long tStamp) {
        if (null == c || c.hasKeyword("Other players can't gain control of CARDNAME.")) {
            return;
        }
        final Game game = host.getGame();
        if (c.isInPlay()) {
            c.removeTempController(tStamp);

            game.getAction().controllerChangeZoneCorrection(c);

            if (tapOnLose) {
                c.tap(false);
            }
        }
        host.removeGainControlTargets(c);
    }

    @Override
    public void resolve(SpellAbility sa) {
        Card source = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();

        final boolean bUntap = sa.hasParam("Untap");
        final boolean bTapOnLose = sa.hasParam("TapOnLose");
        final boolean remember = sa.hasParam("RememberControlled");
        final boolean forget = sa.hasParam("ForgetControlled");
        final List<String> keywords = sa.hasParam("AddKWs") ? Arrays.asList(sa.getParam("AddKWs").split(" & ")) : null;
        final List<String> lose = sa.hasParam("LoseControl") ? Arrays.asList(sa.getParam("LoseControl").split(",")) : null;

        final List<Player> controllers = getDefinedPlayersOrTargeted(sa, "NewController");

        final Player newController = controllers.isEmpty() ? activator : controllers.get(0);
        final Game game = newController.getGame();

        CardCollectionView tgtCards = null;
        if (sa.hasParam("Choices")) {
            Player chooser = sa.hasParam("Chooser") ? AbilityUtils.getDefinedPlayers(source,
                    sa.getParam("Chooser"), sa).get(0) : activator;
            CardCollectionView choices = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield),
                    sa.getParam("Choices"), activator, source, sa);
            if (!choices.isEmpty()) {
                String title = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") :
                    Localizer.getInstance().getMessage("lblChooseaCard") +" ";
                tgtCards = chooser.getController().chooseCardsForEffect(choices, sa, title, 1, 1, false, null);
            }
        } else {
            tgtCards = getDefinedCards(sa);
        }

        if (tgtCards != null & sa.hasParam("ControlledByTarget")) {
            tgtCards = CardLists.filterControlledBy(tgtCards, getTargetPlayers(sa));
        }

        // in case source was LKI or still resolving
        if (source.isLKI() || source.getZone().is(ZoneType.Stack)) {
            source = game.getCardState(source);
        }

        // check for lose control criteria right away
        if (lose != null && lose.contains("LeavesPlay") && !source.isInPlay()) {
            return;
        }
        if (lose != null && lose.contains("LoseControl") && source.getController() != sa.getActivatingPlayer()) {
            return;
        }
        if (lose != null && lose.contains("Untap") && !source.isTapped()) {
            return;
        }

        boolean combatChanged = false;
        for (Card tgtC : tgtCards) {
            if (!tgtC.isInPlay() || !tgtC.canBeControlledBy(newController)) {
                continue;
            }

            if (!tgtC.equals(source) && !source.getGainControlTargets().contains(tgtC)) {
                source.addGainControlTarget(tgtC);
            }

            long tStamp = game.getNextTimestamp();
            tgtC.addTempController(newController, tStamp);

            if (bUntap) {
                tgtC.untap(true);
            }

            final List<String> kws = Lists.newArrayList();
            final List<String> hiddenKws = Lists.newArrayList();
            if (null != keywords) {
                for (final String kw : keywords) {
                    if (kw.startsWith("HIDDEN")) {
                        hiddenKws.add(kw.substring(7));
                    } else {
                        kws.add(kw);
                    }
                }
            }

            if (!kws.isEmpty()) {
                tgtC.addChangedCardKeywords(kws, Lists.newArrayList(), false, tStamp, 0);
                game.fireEvent(new GameEventCardStatsChanged(tgtC));
            }
            if (hiddenKws.isEmpty()) {
                tgtC.addHiddenExtrinsicKeywords(tStamp, 0, hiddenKws);
            }

            if (remember && !source.isRemembered(tgtC)) {
                source.addRemembered(tgtC);
            }

            if (forget && source.isRemembered(tgtC)) {
                source.removeRemembered(tgtC);
            }

            if (lose != null) {
                final GameCommand loseControl = getLoseControlCommand(tgtC, tStamp, bTapOnLose, source);
                if (lose.contains("LeavesPlay") && source != tgtC) { // Only return control if host and target are different cards
                    source.addLeavesPlayCommand(loseControl);
                }
                if (lose.contains("Untap")) {
                    source.addUntapCommand(loseControl);
                }
                if (lose.contains("LoseControl")) {
                    source.addChangeControllerCommand(loseControl);
                }
                if (lose.contains("EOT")) {
                    game.getEndOfTurn().addUntil(loseControl);
                    tgtC.addChangedSVars(Collections.singletonMap("SacMe", "6"), tStamp, 0);
                }
                if (lose.contains("EndOfCombat")) {
                    game.getEndOfCombat().addUntil(loseControl);
                    tgtC.addChangedSVars(Collections.singletonMap("SacMe", "6"), tStamp, 0);
                }
                if (lose.contains("StaticCommandCheck")) {
                    String leftVar = sa.getSVar(sa.getParam("StaticCommandCheckSVar"));
                    String rightVar = sa.getParam("StaticCommandSVarCompare");
                    source.addStaticCommandList(new Object[]{leftVar, rightVar, tgtC, loseControl});
                }
                if (lose.contains("UntilTheEndOfYourNextTurn")) {
                    if (game.getPhaseHandler().isPlayerTurn(sa.getActivatingPlayer())) {
                        game.getEndOfTurn().registerUntilEnd(sa.getActivatingPlayer(), loseControl);
                    } else {
                        game.getEndOfTurn().addUntilEnd(sa.getActivatingPlayer(), loseControl);
                    }
                }
            }

            if (keywords != null) {
                // Add keywords only until end of turn
                final GameCommand untilKeywordEOT = new GameCommand() {
                    private static final long serialVersionUID = -42244224L;

                    @Override
                    public void run() {
                        tgtC.removeHiddenExtrinsicKeywords(tStamp, 0);
                        tgtC.removeChangedCardKeywords(tStamp, 0);
                    }
                };
                game.getEndOfTurn().addUntil(untilKeywordEOT);
            }

            game.getAction().controllerChangeZoneCorrection(tgtC);

            if (addToCombat(tgtC, tgtC.getController(), sa, "Attacking", "Blocking")) {
                combatChanged = true;
            }
        } // end foreach target

        if (combatChanged) {
            game.updateCombatForView();
            game.fireEvent(new GameEventCombatChanged());
        }
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
            final long tStamp, final boolean bTapOnLose, final Card hostCard) {
        final GameCommand loseControl = new GameCommand() {
            private static final long serialVersionUID = 878543373519872418L;

            @Override
            public void run() { 
                doLoseControl(c, hostCard, bTapOnLose, tStamp);
                c.removeChangedSVars(tStamp, 0);
            }
        };

        return loseControl;
    }

    private CardCollectionView getDefinedCards(final SpellAbility sa) {
        final Game game = sa.getHostCard().getGame();
        if (sa.hasParam("AllValid")) {
            return AbilityUtils.filterListByType(game.getCardsIn(ZoneType.Battlefield), sa.getParam("AllValid"), sa);
        }
        return getDefinedCardsOrTargeted(sa);
    }
}
