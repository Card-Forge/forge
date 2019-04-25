package forge.game.ability.effects;

import forge.GameCommand;
import forge.card.CardType;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.keyword.KeywordInterface;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.Lang;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import forge.game.card.CardFactoryUtil;
import forge.util.TextUtil;

public class PumpEffect extends SpellAbilityEffect {

    private static void applyPump(final SpellAbility sa, final Card applyTo,
            final int a, final int d, final List<String> keywords,
            final long timestamp) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        //if host is not on the battlefield don't apply
        // Suspend should does Affect the Stack
        if ((sa.hasParam("UntilLoseControlOfHost") || sa.hasParam("UntilHostLeavesPlay"))
                && !(host.isInPlay() || host.isInZone(ZoneType.Stack))) {
            return;
        }

        // do Game Check there in case of LKI
        final Card gameCard = game.getCardState(applyTo, null);
        if (gameCard == null || !applyTo.equalsWithTimestamp(gameCard)) {
            return;
        }
        final List<String> kws = Lists.newArrayList();

        boolean redrawPT = false;
        for (String kw : keywords) {
            if (kw.startsWith("HIDDEN")) {
                gameCard.addHiddenExtrinsicKeyword(kw);
                redrawPT |= kw.contains("CARDNAME's power and toughness are switched");
            } else {
                kws.add(kw);
            }
        }

        gameCard.addTempPowerBoost(a);
        gameCard.addTempToughnessBoost(d);
        gameCard.addChangedCardKeywords(kws, Lists.<String>newArrayList(), false, false, timestamp);
        if (redrawPT) {
            gameCard.updatePowerToughnessForView();
        }
        
        if (sa.hasParam("LeaveBattlefield")) {
            addLeaveBattlefieldReplacement(gameCard, sa, sa.getParam("LeaveBattlefield"));
        }

        if (!sa.hasParam("Permanent")) {
            // If not Permanent, remove Pumped at EOT
            final GameCommand untilEOT = new GameCommand() {
                private static final long serialVersionUID = -42244224L;

                @Override
                public void run() {
                    gameCard.addTempPowerBoost(-1 * a);
                    gameCard.addTempToughnessBoost(-1 * d);

                    if (keywords.size() > 0) {
                        boolean redrawPT = false;

                        for (String kw : keywords) {
                            redrawPT |= kw.contains("CARDNAME's power and toughness are switched");
                            if (kw.startsWith("HIDDEN")) {
                                gameCard.removeHiddenExtrinsicKeyword(kw);
                                if (redrawPT) {
                                    gameCard.updatePowerToughnessForView();
                                }
                            }
                        }
                        gameCard.removeChangedCardKeywords(timestamp);
                    }

                    game.fireEvent(new GameEventCardStatsChanged(gameCard));
                }
            };
            addUntilCommand(sa, untilEOT);
        }
        game.fireEvent(new GameEventCardStatsChanged(gameCard));
    }

    private static void applyPump(final SpellAbility sa, final Player p,
            final List<String> keywords, final long timestamp) {
        final Card host = sa.getHostCard();
        //if host is not on the battlefield don't apply
        // Suspend should does Affect the Stack
        if ((sa.hasParam("UntilLoseControlOfHost") || sa.hasParam("UntilHostLeavesPlay"))
                && !(host.isInPlay() || host.isInZone(ZoneType.Stack))) {
            return;
        }
        p.addChangedKeywords(keywords, ImmutableList.<String>of(), timestamp);

        if (!sa.hasParam("Permanent")) {
            // If not Permanent, remove Pumped at EOT
            final GameCommand untilEOT = new GameCommand() {
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
            addUntilCommand(sa, untilEOT);
        }
    }

    private static void addUntilCommand(final SpellAbility sa, GameCommand untilEOT) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();

        if (sa.hasParam("UntilEndOfCombat")) {
            game.getEndOfCombat().addUntil(untilEOT);
        } else if (sa.hasParam("UntilYourNextUpkeep")) {
            game.getUpkeep().addUntil(sa.getActivatingPlayer(), untilEOT);
        } else if (sa.hasParam("UntilHostLeavesPlay")) {
            host.addLeavesPlayCommand(untilEOT);
        } else if (sa.hasParam("UntilHostLeavesPlayOrEOT")) {
            host.addLeavesPlayCommand(untilEOT);
            game.getEndOfTurn().addUntil(untilEOT);
        } else if (sa.hasParam("UntilLoseControlOfHost")) {
            host.addLeavesPlayCommand(untilEOT);
            host.addChangeControllerCommand(untilEOT);
        } else if (sa.hasParam("UntilYourNextTurn")) {
            game.getCleanup().addUntil(sa.getActivatingPlayer(), untilEOT);
        } else if (sa.hasParam("UntilUntaps")) {
            host.addUntapCommand(untilEOT);
        } else {
            game.getEndOfTurn().addUntil(untilEOT);
        }
    }

    /*
     * (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#getStackDescription(forge.game.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(final SpellAbility sa) {

        final StringBuilder sb = new StringBuilder();
        List<GameEntity> tgts = Lists.newArrayList();
        tgts.addAll(getTargetCards(sa));
        if ((sa.usesTargeting() && sa.getTargetRestrictions().canTgtPlayer()) || sa.hasParam("Defined")) {
            tgts.addAll(getTargetPlayers(sa));
        }

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

            List<String> keywords = Lists.newArrayList();
            if (sa.hasParam("KW")) {
                keywords.addAll(Arrays.asList(sa.getParam("KW").split(" & ")));
            }
            final int atk = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumAtt"), sa, true);
            final int def = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumDef"), sa, true);

            sb.append("gains ");
            if (sa.hasParam("NumAtt") || sa.hasParam("NumDef")) {
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
    public void resolve(final SpellAbility sa) {

        final List<Card> untargetedCards = Lists.newArrayList();
        final Game game = sa.getActivatingPlayer().getGame();
        final Card host = sa.getHostCard();
        final long timestamp = game.getNextTimestamp();

        String pumpForget = null;
        String pumpImprint = null;

        List<String> keywords = Lists.newArrayList();
        if (sa.hasParam("KW")) {
            keywords.addAll(Arrays.asList(sa.getParam("KW").split(" & ")));
        }
        final int a = AbilityUtils.calculateAmount(host, sa.getParam("NumAtt"), sa, true);
        final int d = AbilityUtils.calculateAmount(host, sa.getParam("NumDef"), sa, true);

        if (sa.hasParam("SharedKeywordsZone")) {
            List<ZoneType> zones = ZoneType.listValueOf(sa.getParam("SharedKeywordsZone"));
            String[] restrictions = sa.hasParam("SharedRestrictions") ? sa.getParam("SharedRestrictions").split(",") : new String[]{"Card"};
            keywords = CardFactoryUtil.sharedKeywords(keywords, restrictions, zones, sa.getHostCard());
        }

        List<GameEntity> tgts = Lists.newArrayList();
        List<Card> tgtCards = getTargetCards(sa);
        List<Player> tgtPlayers = getTargetPlayers(sa);
        tgts.addAll(tgtCards);
        tgts.addAll(tgtPlayers);

        if (sa.hasParam("DefinedKW")) {
            String defined = sa.getParam("DefinedKW");
            String replaced = "";
            if (defined.equals("ChosenType")) {
                replaced = host.getChosenType();
            } else if (defined.equals("CardUIDSource")) {
                replaced = "CardUID_" + String.valueOf(host.getId());
            }
            for (int i = 0; i < keywords.size(); i++) {
                keywords.set(i, TextUtil.fastReplace(keywords.get(i), defined, replaced));
            }
        }
        if (sa.hasParam("DefinedLandwalk")) {
            final String landtype = sa.getParam("DefinedLandwalk");
            final Card c = AbilityUtils.getDefinedCards(host, landtype, sa).get(0);
            for (String type : c.getType()) {
                if (CardType.isALandType(type)) {
                    keywords.add(type + "walk");
                }
            }
        }
        if (sa.hasParam("RandomKeyword")) {
            final String num = sa.hasParam("RandomKWNum") ? sa.getParam("RandomKWNum") : "1";
            final int numkw = AbilityUtils.calculateAmount(host, num, sa);
            List<String> choice = Lists.newArrayList();
            List<String> total = Lists.newArrayList(keywords);
            if (sa.hasParam("NoRepetition")) {
                for (KeywordInterface inst : tgtCards.get(0).getKeywords()) {
                    final String kws = inst.getOriginal();
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
            final String message = sa.hasParam("OptionQuestion")
                    ? TextUtil.fastReplace(sa.getParam("OptionQuestion"), "TARGETS", targets)
                    : TextUtil.concatNoSpace("Apply pump to ", targets, "?");

            if (!sa.getActivatingPlayer().getController().confirmAction(sa, null, message)) {
                return;
            }
        }

        if (sa.hasParam("RememberObjects")) {
            for (final Object o : AbilityUtils.getDefinedObjects(host, sa.getParam("RememberObjects"), sa)) {
                host.addRemembered(o);
            }
        }

        if (sa.hasParam("ForgetObjects")) {
            pumpForget = sa.getParam("ForgetObjects");
        }

        if (pumpForget != null) {
            for (final Object o : AbilityUtils.getDefinedObjects(host, pumpForget, sa)) {
                host.removeRemembered(o);
            }
        }
        if (sa.hasParam("ImprintCards")) {
            pumpImprint = sa.getParam("ImprintCards");
        }

        if (pumpImprint != null) {
            for (final Card c : AbilityUtils.getDefinedCards(host, pumpImprint, sa)) {
                host.addImprintedCard(c);
            }
        }

        if (sa.hasParam("ForgetImprinted")) {
            for (final Card c : AbilityUtils.getDefinedCards(host, sa.getParam("ForgetImprinted"), sa)) {
                host.removeImprintedCard(c);
            }
        }

        if (sa.hasParam("Radiance")) {
            for (final Card c : CardUtil.getRadiance(host, tgtCards.get(0), sa.getParam("ValidTgts")
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
            if (sa.usesTargeting() && !tgtC.canBeTargetedBy(sa)) {
                continue;
            }

            applyPump(sa, tgtC, a, d, keywords, timestamp);
        }

        if (sa.hasParam("AtEOT") && !tgtCards.isEmpty()) {
            registerDelayedTrigger(sa, sa.getParam("AtEOT"), tgtCards);
        }
        
        for (final Card tgtC : untargetedCards) {
            // only pump things in PumpZone
            if (!tgtC.isInZone(pumpZone)) {
                continue;
            }

            applyPump(sa, tgtC, a, d, keywords, timestamp);
        }

        for (Player p : tgtPlayers) {
            if (!p.canBeTargetedBy(sa)) {
                continue;
            }

            applyPump(sa, p, keywords, timestamp);
        }

        replaceDying(sa);
    } // pumpResolve()
}
