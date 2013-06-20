package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import forge.Card;
import forge.CardUtil;
import forge.Command;
import forge.GameEntity;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.TargetRestrictions;
import forge.game.Game;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.Lang;

public class PumpEffect extends SpellAbilityEffect {

    private void applyPump(final SpellAbility sa, final Card applyTo, final int a, final int d, final List<String> keywords) {
        //if host is not on the battlefield don't apply
        if (sa.hasParam("UntilLoseControlOfHost")
                && !sa.getSourceCard().isInPlay()) {
            return;
        }
        final Game game = sa.getActivatingPlayer().getGame();
        final long timestamp = game.getNextTimestamp();
        final ArrayList<String> kws = new ArrayList<String>();
        
        for (String kw : keywords) {
            if (kw.startsWith("HIDDEN")) {
                applyTo.addHiddenExtrinsicKeyword(kw);
            } else {
                kws.add(kw);
                if (kw.equals("Suspend") && !applyTo.hasSuspend()) {
                    applyTo.setSuspend(true);
                    CardFactoryUtil.addSuspendUpkeepTrigger(applyTo);
                    CardFactoryUtil.addSuspendPlayTrigger(applyTo);
                }
            }
        }

        applyTo.addTempAttackBoost(a);
        applyTo.addTempDefenseBoost(d);
        applyTo.addChangedCardKeywords(kws, new ArrayList<String>(), false, timestamp);

        if (!sa.hasParam("Permanent")) {
            // If not Permanent, remove Pumped at EOT
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = -42244224L;

                @Override
                public void run() {
                    applyTo.addTempAttackBoost(-1 * a);
                    applyTo.addTempDefenseBoost(-1 * d);

                    if (keywords.size() > 0) {
                        for (String kw : keywords) {
                            if (kw.startsWith("HIDDEN")) {
                                applyTo.removeHiddenExtrinsicKeyword(kw);
                            }
                        }
                        applyTo.removeChangedCardKeywords(timestamp);
                    }
                }
            };
            if (sa.hasParam("UntilEndOfCombat")) {
                game.getEndOfCombat().addUntil(untilEOT);
            } else if (sa.hasParam("UntilYourNextUpkeep")) {
                game.getUpkeep().addUntil(sa.getActivatingPlayer(), untilEOT);
            } else if (sa.hasParam("UntilHostLeavesPlay")) {
                sa.getSourceCard().addLeavesPlayCommand(untilEOT);
            } else if (sa.hasParam("UntilLoseControlOfHost")) {
                sa.getSourceCard().addLeavesPlayCommand(untilEOT);
                sa.getSourceCard().addChangeControllerCommand(untilEOT);
            } else if (sa.hasParam("UntilYourNextTurn")) {
                game.getCleanup().addUntil(sa.getActivatingPlayer(), untilEOT);
            } else if (sa.hasParam("UntilUntaps")) {
                sa.getSourceCard().addUntapCommand(untilEOT);
            } else {
                game.getEndOfTurn().addUntil(untilEOT);
            }
        }
    }

    private void applyPump(final SpellAbility sa, final Player p, final List<String> keywords) {
        final Game game = p.getGame();
        for (int i = 0; i < keywords.size(); i++) {
            p.addKeyword(keywords.get(i));
        }

        if (!sa.hasParam("Permanent")) {
            // If not Permanent, remove Pumped at EOT
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = -32453460L;

                @Override
                public void run() {

                    if (keywords.size() > 0) {
                        for (int i = 0; i < keywords.size(); i++) {
                            p.removeKeyword(keywords.get(i));
                        }
                    }
                }
            };
            if (sa.hasParam("UntilEndOfCombat")) {
                game.getEndOfCombat().addUntil(untilEOT);
            } else if (sa.hasParam("UntilYourNextUpkeep")) {
                game.getUpkeep().addUntil(sa.getActivatingPlayer(), untilEOT);
            } else {
                game.getEndOfTurn().addUntil(untilEOT);
            }
        }
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {

        final StringBuilder sb = new StringBuilder();
        List<GameEntity> tgts = new ArrayList<GameEntity>();
        tgts.addAll(getTargetCards(sa));
        tgts.addAll(getTargetPlayers(sa));

        if (tgts.size() > 0) {

            for (final GameEntity c : tgts) {
                sb.append(c).append(" ");
            }

            if (sa.hasParam("Radiance")) {
                sb.append(" and each other ").append(sa.getParam("ValidTgts"))
                        .append(" that shares a color with ");
                if (tgts.size() > 1) {
                    sb.append("them ");
                } else {
                    sb.append("it ");
                }
            }

            final List<String> keywords = sa.hasParam("KW") ? Arrays.asList(sa.getParam("KW").split(" & ")) : new ArrayList<String>();
            final int atk = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("NumAtt"), sa);
            final int def = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("NumDef"), sa);

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

            if (!sa.hasParam("Permanent")) {
                sb.append("until end of turn.");
            }

        }

        return sb.toString();
    } // pumpStackDescription()

    @Override
    public void resolve(SpellAbility sa) {
        
        final ArrayList<Card> untargetedCards = new ArrayList<Card>();
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Game game = sa.getActivatingPlayer().getGame();
        List<Player> tgtPlayers = new ArrayList<Player>();
        String pumpRemembered = null;
        String pumpForget = null;
        String pumpImprint = null;
        
        List<String> keywords = sa.hasParam("KW") ? Arrays.asList(sa.getParam("KW").split(" & ")) : new ArrayList<String>();
        final int a = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("NumAtt"), sa);
        final int d = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("NumDef"), sa);

        List<GameEntity> tgts = new ArrayList<GameEntity>();
        List<Card> tgtCards = getTargetCards(sa);
        tgts.addAll(tgtCards);
        tgts.addAll(getTargetPlayers(sa));

        if (sa.hasParam("DefinedChosenKW")) {
            if (sa.getParam("DefinedChosenKW").equals("Type")) {
                final String t = sa.getSourceCard().getChosenType();
                for (int i = 0; i < keywords.size(); i++) {
                    keywords.set(i, keywords.get(i).replaceAll("ChosenType", t));
                }
            }
        }
        if (sa.hasParam("RandomKeyword")) {
            final String num = sa.hasParam("RandomKWNum") ? sa.getParam("RandomKWNum") : "1";
            final int numkw = AbilityUtils.calculateAmount(sa.getSourceCard(), num, sa);
            List<String> choice = new ArrayList<String>();
            List<String> total = new ArrayList<String>(keywords);
            if (sa.hasParam("NoRepetition")) {
                final List<String> tgtCardskws = tgtCards.get(0).getKeyword();
                for (String kws : tgtCardskws) {
                    if (total.contains(kws)) {
                        total.remove(kws);
                    }
                }
            }
            final int min = Math.min(total.size(), numkw);
            for (int i = 0; i < min; i++) {
                final String random = Aggregates.random(total);
                choice.add(random);
                total.remove(random);
            }
            keywords = choice;
        }
        
        if (sa.hasParam("Optional")) {
            final String targets = Lang.joinHomogenous(tgtCards);
            final String message = sa.hasParam("OptionQuestion") ? sa.getParam("OptionQuestion").replace("TARGETS", targets) : "Apply pump to " + targets + "?";

            if ( !sa.getActivatingPlayer().getController().confirmAction(sa, null, message) )
                return;
        }

        if (sa.hasParam("RememberObjects")) {
            pumpRemembered = sa.getParam("RememberObjects");
        }

        if (pumpRemembered != null) {
            for (final Object o : AbilityUtils.getDefinedObjects(sa.getSourceCard(), pumpRemembered, sa)) {
                if (!sa.getSourceCard().getRemembered().contains(o)) {
                    sa.getSourceCard().addRemembered(o);
                }
            }
        }

        if (sa.hasParam("ForgetObjects")) {
            pumpForget = sa.getParam("ForgetObjects");
        }

        if (pumpForget != null) {
            for (final Object o : AbilityUtils.getDefinedObjects(sa.getSourceCard(), pumpForget, sa)) {
                if (sa.getSourceCard().getRemembered().contains(o)) {
                    sa.getSourceCard().removeRemembered(o);
                }
            }
        }
        if (sa.hasParam("ImprintCards")) {
            pumpImprint = sa.getParam("ImprintCards");
        }

        if (pumpImprint != null) {
            for (final Card c : AbilityUtils.getDefinedCards(sa.getSourceCard(), pumpImprint, sa)) {
                if (!sa.getSourceCard().getImprinted().contains(c)) {
                    sa.getSourceCard().addImprinted(c);
                }
            }
        }
        
        if (sa.hasParam("ForgetLastImprinted")) {
            final int size = sa.getSourceCard().getImprinted().size();
            sa.getSourceCard().getImprinted().remove(size - 1);
        }   // Used in a SubAbility to clear the root imprinted card (Archery Training)
        
        if (sa.hasParam("Radiance")) {
            for (final Card c : CardUtil.getRadiance(sa.getSourceCard(), tgtCards.get(0), sa.getParam("ValidTgts")
                    .split(","))) {
                untargetedCards.add(c);
            }
        }

        final ZoneType pumpZone = sa.hasParam("PumpZone") ? ZoneType.smartValueOf(sa.getParam("PumpZone"))
                : ZoneType.Battlefield;

        final int size = tgtCards.size();
        for (int j = 0; j < size; j++) {
            final Card tgtC = tgtCards.get(j);

            // only pump things in PumpZone
            if (!game.getCardsIn(pumpZone).contains(tgtC)) {
                continue;
            }

            // if pump is a target, make sure we can still target now
            if ((tgt != null) && !tgtC.canBeTargetedBy(sa)) {
                continue;
            }

            this.applyPump(sa, tgtC, a, d, keywords);
        }

        for (int i = 0; i < untargetedCards.size(); i++) {
            final Card tgtC = untargetedCards.get(i);
            // only pump things in PumpZone
            if (!game.getCardsIn(pumpZone).contains(tgtC)) {
                continue;
            }

            this.applyPump(sa, tgtC, a, d, keywords);
        }

        for (Player p : tgtPlayers) {
            if (!p.canBeTargetedBy(sa)) {
                continue;
            }

            this.applyPump(sa, p, keywords);
        }
    } // pumpResolve()
}
