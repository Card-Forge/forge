package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.Command;
import forge.GameEntity;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class ControlGainEffect extends SpellEffect { 
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(Map<String, String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
    
        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        } else {
            sb.append(" ");
        }
    
        ArrayList<Card> tgtCards;
    
        final Target tgt = sa.getTarget();
        if ((tgt != null) && !params.containsKey("Defined")) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }
    
        ArrayList<Player> newController = AbilityFactory.getDefinedPlayers(sa.getSourceCard(),
                params.get("NewController"), sa);
        if ((tgt != null) && tgt.getTargetPlayers() != null && !tgt.getTargetPlayers().isEmpty()) {
            newController = tgt.getTargetPlayers();
        }
        if (newController.size() == 0) {
            newController.add(sa.getActivatingPlayer());
        }
    
        sb.append(newController).append(" gains control of ");
    
        for (final Card c : tgtCards) {
            sb.append(" ");
            if (c.isFaceDown()) {
                sb.append("Morph");
            } else {
                sb.append(c);
            }
        }
        sb.append(".");
    
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }
    
        return sb.toString();
    }

    private void doLoseControl(final Card c, final Card host, final boolean tapOnLose,
            final List<String> addedKeywords, final GameEntity newController) {
        if (null == c) {
            return;
        }
        if (c.isInPlay()) {
            c.removeController(newController);
            // Singletons.getModel().getGameAction().changeController(new ArrayList<Card>(c),
            // c.getController(), originalController);
    
            if (tapOnLose) {
                c.tap();
            }
    
            if (null != addedKeywords) {
                for (final String kw : addedKeywords) {
                    c.removeExtrinsicKeyword(kw);
                }
            }
        } // if
        host.clearGainControlTargets();
        host.clearGainControlReleaseCommands();
    }

    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        List<Card> tgtCards = new ArrayList<Card>();
        Card source = sa.getSourceCard();
    
        
        final boolean bUntap = params.containsKey("Untap");
        final boolean bTapOnLose = params.containsKey("TapOnLose");
        final boolean bNoRegen = params.containsKey("NoRegen");
        final List<String> destroyOn = params.containsKey("DestroyTgt") ? Arrays.asList(params.get("DestroyTgt").split(",")) : null;
        final List<String> kws = params.containsKey("AddKWs") ? Arrays.asList(params.get("AddKWs").split(" & ")) : null ;
        final List<String> lose = params.containsKey("LoseControl") ? Arrays.asList(params.get("LoseControl").split(",")) : null;

        final Target tgt = sa.getTarget();
        if (params.containsKey("AllValid")) {
            tgtCards = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
            tgtCards = AbilityFactory.filterListByType(tgtCards, params.get("AllValid"), sa);
        } else if ((tgt != null) && !params.containsKey("Defined")) {
            tgtCards.addAll(tgt.getTargetCards());
        } else {
            tgtCards.addAll(AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa));
        }
    
        ArrayList<Player> controllers = new ArrayList<Player>();
    
        if (params.containsKey("NewController")) {
            controllers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("NewController"), sa);
        } else if ((tgt != null) && (tgt.getTargetPlayers() != null) && tgt.canTgtPlayer()) {
            controllers = tgt.getTargetPlayers();
        }
    
        GameEntity newController;
    
        if (controllers.size() == 0) {
            if (sa.isSpell()) {
                newController = sa.getActivatingPlayer();
            } else {
                newController = source;
            }
        } else {
            newController = controllers.get(0);
        }
        // check for lose control criteria right away
        if (lose != null && lose.contains("LeavesPlay") && !source.isInZone(ZoneType.Battlefield)) {
            return;
        }
        if (lose != null && lose.contains("Untap") && !source.isTapped()) {
            return;
        }
    
        final int size = tgtCards.size();
        for (int j = 0; j < size; j++) {
            final Card tgtC = tgtCards.get(j);
            final Player originalController = tgtC.getController();
    
            if (!tgtC.equals(sa.getSourceCard()) && !sa.getSourceCard().getGainControlTargets().contains(tgtC)) {
                sa.getSourceCard().addGainControlTarget(tgtC);
            }
    
            if (tgtC.isInPlay()) {
    
                if (!tgtC.equals(newController)) {
                    tgtC.addController(newController);
                }
                // Singletons.getModel().getGameAction().changeController(new ArrayList<Card>(tgtC),
                // tgtC.getController(), newController.get(0));
    
                if (bUntap) {
                    tgtC.untap();
                }
    
                if (null != kws) {
                    for (final String kw : kws) {
                        tgtC.addExtrinsicKeyword(kw);
                    }
                }
            }
    
            // end copied
    
            final Card hostCard = sa.getAbilityFactory().getHostCard(); 
            if (lose != null) {
                if (lose.contains("LeavesPlay")) {
                    sa.getSourceCard().addLeavesPlayCommand(this.getLoseControlCommand(tgtC, originalController, newController, bTapOnLose, hostCard, kws));
                }
                if (lose.contains("Untap")) {
                    sa.getSourceCard().addUntapCommand(this.getLoseControlCommand(tgtC, originalController, newController, bTapOnLose, hostCard, kws));
                }
                if (lose.contains("LoseControl")) {
                    sa.getSourceCard().addChangeControllerCommand(this.getLoseControlCommand(tgtC, originalController, newController, bTapOnLose, hostCard, kws));
                }
                if (lose.contains("EOT")) {
                    Singletons.getModel().getGame().getEndOfTurn().addAt(this.getLoseControlCommand(tgtC, originalController, newController, bTapOnLose, hostCard, kws));
                }
            }
    
            if (destroyOn != null) {
                if (destroyOn.contains("LeavesPlay")) {
                    sa.getSourceCard().addLeavesPlayCommand(this.getDestroyCommand(tgtC, hostCard, bNoRegen));
                }
                if (destroyOn.contains("Untap")) {
                    sa.getSourceCard().addUntapCommand(this.getDestroyCommand(tgtC, hostCard, bNoRegen));
                }
                if (destroyOn.contains("LoseControl")) {
                    sa.getSourceCard().addChangeControllerCommand(this.getDestroyCommand(tgtC, hostCard, bNoRegen));
                }
            }
    
            sa.getSourceCard().clearGainControlReleaseCommands();
            sa.getSourceCard().addGainControlReleaseCommand(this.getLoseControlCommand(tgtC, originalController, newController, bTapOnLose, hostCard, kws));
    
        } // end foreach target
    }

    /**
     * <p>
     * getDestroyCommand.
     * </p>
     * 
     * @param i
     *            a int.
     * @return a {@link forge.Command} object.
     */
    private Command getDestroyCommand(final Card c, final Card hostCard, final boolean bNoRegen) {
        final Command destroy = new Command() {
            private static final long serialVersionUID = 878543373519872418L;
    
            @Override
            public void execute() {
                final Ability ability = new Ability(hostCard, "0") {
                    @Override
                    public void resolve() {
    
                        if (bNoRegen) {
                            Singletons.getModel().getGame().getAction().destroyNoRegeneration(c);
                        } else {
                            Singletons.getModel().getGame().getAction().destroy(c);
                        }
                    }
                };
                final StringBuilder sb = new StringBuilder();
                sb.append(hostCard).append(" - destroy ").append(c.getName()).append(".");
                if (bNoRegen) {
                    sb.append("  It can't be regenerated.");
                }
                ability.setStackDescription(sb.toString());
    
                Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(ability);
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
     * @return a {@link forge.Command} object.
     */
    private Command getLoseControlCommand(final Card c, final Player originalController, final GameEntity newController, 
            final boolean bTapOnLose, final Card hostCard, final List<String> kws ) {
        final Command loseControl = new Command() {
            private static final long serialVersionUID = 878543373519872418L;
    
            @Override
            public void execute() { doLoseControl(c, hostCard, bTapOnLose, kws, newController); } // execute()
        };
    
        return loseControl;
    }
    
}