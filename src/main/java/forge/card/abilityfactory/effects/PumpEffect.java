package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.CardUtil;
import forge.Command;
import forge.GameActionUtil;
import forge.GameEntity;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class PumpEffect extends SpellEffect {
    
    private void applyPump(final SpellAbility sa, final Card applyTo, final Map<String,String> params, final int a, final int d, final List<String> keywords) {
        //if host is not on the battlefield don't apply
        if (params.containsKey("UntilLoseControlOfHost")
                && !sa.getSourceCard().isInPlay()) {
            return;
        }
    
        applyTo.addTempAttackBoost(a);
        applyTo.addTempDefenseBoost(d);
    
        for (int i = 0; i < keywords.size(); i++) {
            applyTo.addExtrinsicKeyword(keywords.get(i));
            if (keywords.get(i).equals("Suspend")) {
                applyTo.setSuspend(true);
            }
        }
    
        if (!params.containsKey("Permanent")) {
            // If not Permanent, remove Pumped at EOT
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = -42244224L;
    
                @Override
                public void execute() {
                    applyTo.addTempAttackBoost(-1 * a);
                    applyTo.addTempDefenseBoost(-1 * d);
    
                    if (keywords.size() > 0) {
                        for (int i = 0; i < keywords.size(); i++) {
                            applyTo.removeExtrinsicKeyword(keywords.get(i));
                        }
                    }
                }
            };
            if (params.containsKey("UntilEndOfCombat")) {
                Singletons.getModel().getGame().getEndOfCombat().addUntil(untilEOT);
            } else if (params.containsKey("UntilYourNextUpkeep")) {
                Singletons.getModel().getGame().getUpkeep().addUntil(sa.getActivatingPlayer(), untilEOT);
            } else if (params.containsKey("UntilHostLeavesPlay")) {
                sa.getSourceCard().addLeavesPlayCommand(untilEOT);
            } else if (params.containsKey("UntilLoseControlOfHost")) {
                sa.getSourceCard().addLeavesPlayCommand(untilEOT);
                sa.getSourceCard().addChangeControllerCommand(untilEOT);
            } else if (params.containsKey("UntilYourNextTurn")) {
                Singletons.getModel().getGame().getCleanup().addUntilYourNextTurn(sa.getActivatingPlayer(), untilEOT);
            } else if (params.containsKey("UntilUntaps")) {
                sa.getSourceCard().addUntapCommand(untilEOT);
            } else {
                Singletons.getModel().getGame().getEndOfTurn().addUntil(untilEOT);
            }
        }
    }

    private void applyPump(final SpellAbility sa, final Player p, final Map<String,String> params, final List<String> keywords) {
    
        for (int i = 0; i < keywords.size(); i++) {
            p.addKeyword(keywords.get(i));
        }
    
        if (!params.containsKey("Permanent")) {
            // If not Permanent, remove Pumped at EOT
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = -32453460L;
    
                @Override
                public void execute() {
    
                    if (keywords.size() > 0) {
                        for (int i = 0; i < keywords.size(); i++) {
                            p.removeKeyword(keywords.get(i));
                        }
                    }
                }
            };
            if (params.containsKey("UntilEndOfCombat")) {
                Singletons.getModel().getGame().getEndOfCombat().addUntil(untilEOT);
            } else if (params.containsKey("UntilYourNextUpkeep")) {
                Singletons.getModel().getGame().getUpkeep().addUntil(sa.getActivatingPlayer(), untilEOT);
            } else {
                Singletons.getModel().getGame().getEndOfTurn().addUntil(untilEOT);
            }
        }
    }

    /**
     * <p>
     * pumpStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {

        final StringBuilder sb = new StringBuilder();
        ArrayList<GameEntity> tgts = new ArrayList<GameEntity>();

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgts.addAll(tgt.getTargetCards());
            tgts.addAll(tgt.getTargetPlayers());
        } else {
            if (params.containsKey("Defined")) {
                tgts.addAll(AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa));
            }
            if (tgts.isEmpty()) {
                tgts.addAll(AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa));
            }
        }

        if (tgts.size() > 0) {

            if (sa instanceof AbilitySub) {
                sb.append(" ");
            } else {
                sb.append(sa.getSourceCard()).append(" - ");
            }

            if (params.containsKey("StackDescription")) {
                if (params.get("StackDescription").equals("None")) {
                    sb.append("");
                } else {
                sb.append(params.get("StackDescription"));
                }
            }

            else {
                for (final GameEntity c : tgts) {
                    sb.append(c).append(" ");
                }

                if (params.containsKey("Radiance")) {
                    sb.append(" and each other ").append(params.get("ValidTgts"))
                            .append(" that shares a color with ");
                    if (tgts.size() > 1) {
                        sb.append("them ");
                    } else {
                        sb.append("it ");
                    }
                }

                final List<String> keywords = params.containsKey("KW") ? Arrays.asList(params.get("KW").split(" & ")) : new ArrayList<String>();
                final int atk = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumAtt"), sa); 
                final int def = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumDef"), sa); 

                sb.append("gains ");
                if ((atk != 0) || (def != 0)) {
                    if (atk >= 0) {
                        sb.append("+");
                    }
                    sb.append(atk);
                    sb.append("/");
                    if (def >= 0) {
                       sb.append("+");
                    }
                    sb.append(def);
                    sb.append(" ");
                }

                for (int i = 0; i < keywords.size(); i++) {
                    sb.append(keywords.get(i)).append(" ");
                }

                if (!params.containsKey("Permanent")) {
                    sb.append("until end of turn.");
                }
            }
        }

        return sb.toString();
    } // pumpStackDescription()

    /**
     * <p>
     * pumpResolve.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        ArrayList<Card> tgtCards = new ArrayList<Card>();
        final ArrayList<Card> untargetedCards = new ArrayList<Card>();
        final Target tgt = sa.getTarget();
        ArrayList<Player> tgtPlayers = new ArrayList<Player>();
        String pumpRemembered = null;

        final List<String> keywords = params.containsKey("KW") ? Arrays.asList(params.get("KW").split(" & ")) : new ArrayList<String>();
        final int a = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumAtt"), sa); 
        final int d = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumDef"), sa); 
            
        
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            if (params.containsKey("Defined")) {
                tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
            }
            if (tgtPlayers.isEmpty()) {
                tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
            }
        }

        if (params.containsKey("Optional")) {
            if (sa.getActivatingPlayer().isHuman()) {
                final StringBuilder targets = new StringBuilder();
                for (final Card tc : tgtCards) {
                    targets.append(tc);
                }
                final StringBuilder sb = new StringBuilder();
                final String descBasic = "Apply pump to " + targets + "?";
                final String pumpDesc = params.containsKey("OptionQuestion")
                        ? params.get("OptionQuestion").replace("TARGETS", targets) : descBasic;
                sb.append(pumpDesc);
                if (!GameActionUtil.showYesNoDialog(sa.getSourceCard(), sb.toString())) {
                   return;
                }
            } else { //Computer player
                //TODO Add logic here if necessary but I think the AI won't cast
                //the spell in the first place if it would curse its own creature
                //and the pump isn't mandatory
            }
        }

        if (params.containsKey("RememberObjects")) {
            pumpRemembered = params.get("RememberObjects");
        }

        if (pumpRemembered != null) {
            for (final Object o : AbilityFactory.getDefinedObjects(sa.getSourceCard(), pumpRemembered, sa)) {
                if (!sa.getSourceCard().getRemembered().contains(o)) {
                    sa.getSourceCard().addRemembered(o);
                }
            }
        }

        if (params.containsKey("Radiance")) {
            for (final Card c : CardUtil.getRadiance(sa.getSourceCard(), tgtCards.get(0), params.get("ValidTgts")
                    .split(","))) {
                untargetedCards.add(c);
            }
        }

        final ZoneType pumpZone = params.containsKey("PumpZone") ? ZoneType.smartValueOf(params.get("PumpZone"))
                : ZoneType.Battlefield;

        final int size = tgtCards.size();
        for (int j = 0; j < size; j++) {
            final Card tgtC = tgtCards.get(j);

            // only pump things in PumpZone
            if (!Singletons.getModel().getGame().getCardsIn(pumpZone).contains(tgtC)) {
                continue;
            }

            // if pump is a target, make sure we can still target now
            if ((tgt != null) && !tgtC.canBeTargetedBy(sa)) {
                continue;
            }

            this.applyPump(sa, tgtC, params, a, d, keywords);
        }

        for (int i = 0; i < untargetedCards.size(); i++) {
            final Card tgtC = untargetedCards.get(i);
            // only pump things in PumpZone
            if (!Singletons.getModel().getGame().getCardsIn(pumpZone).contains(tgtC)) {
                continue;
            }

            this.applyPump(sa, tgtC, params, a, d, keywords);
        }

        for (Player p : tgtPlayers) {
            if (!p.canBeTargetedBy(sa)) {
                continue;
            }

            this.applyPump(sa, p, params, keywords);
        }
    } // pumpResolve()
}