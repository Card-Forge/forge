package forge.game.ability.effects;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactoryUtil;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.player.PlayerCollection;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.TextUtil;

public class PumpAllEffect extends SpellAbilityEffect {
    private static void applyPumpAll(final SpellAbility sa,
            final Iterable<Card> list, final int a, final int d,
            final List<String> keywords, final List<ZoneType> affectedZones) {
        
        final Game game = sa.getActivatingPlayer().getGame();
        final long timestamp = game.getNextTimestamp();
        final List<String> kws = Lists.newArrayList();
        final List<String> hiddenkws = Lists.newArrayList();
        
        for (String kw : keywords) {
            if (kw.startsWith("HIDDEN")) {
                hiddenkws.add(kw.substring(7));
            } else {
                kws.add(kw);
            }
        }
        
        for (final Card tgtC : list) {
            // only pump things in the affected zones.
            boolean found = false;
            for (final ZoneType z : affectedZones) {
                if (tgtC.isInZone(z)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                continue;
            }

            boolean redrawPT = false;

            if (a != 0 || d != 0) {
                tgtC.addPTBoost(a, d, timestamp, 0);
                redrawPT = true;
            }

            if (!kws.isEmpty()) {
                tgtC.addChangedCardKeywords(kws, null, false, timestamp, 0);
            }
            if (redrawPT) {
                tgtC.updatePowerToughnessForView();
            }

            if (!hiddenkws.isEmpty()) {
                tgtC.addHiddenExtrinsicKeywords(timestamp, 0, hiddenkws);
            }

            if (sa.hasParam("RememberAllPumped")) {
                sa.getHostCard().addRemembered(tgtC);
            }
        
            if (!"Permanent".equals(sa.getParam("Duration"))) {
                // If not Permanent, remove Pumped at EOT
                final GameCommand untilEOT = new GameCommand() {
                    private static final long serialVersionUID = 5415795460189457660L;

                    @Override
                    public void run() {
                        tgtC.removePTBoost(timestamp, 0);
                        tgtC.removeChangedCardKeywords(timestamp, 0);
                        tgtC.removeHiddenExtrinsicKeywords(timestamp, 0);

                        tgtC.updatePowerToughnessForView();

                        game.fireEvent(new GameEventCardStatsChanged(tgtC));
                    }
                };
                if ("UntilLoseControlOfHost".equals(sa.getParam("Duration"))) {
                    tgtC.addLeavesPlayCommand(untilEOT);
                    tgtC.addChangeControllerCommand(untilEOT);
                } else {
                    addUntilCommand(sa, untilEOT);
                }
            }

            game.fireEvent(new GameEventCardStatsChanged(tgtC));
        }

        if (sa.hasParam("AtEOT") && !Iterables.isEmpty(list)) {
            registerDelayedTrigger(sa, sa.getParam("AtEOT"), list);
        }
    }

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        String desc = "";
        if (sa.hasParam("SpellDescription")) {
            desc = TextUtil.fastReplace(sa.getParam("SpellDescription"), "CARDNAME", sa.getHostCard().getName());
        }

        sb.append(desc);

        return sb.toString();
    } // pumpAllStackDescription()

    @Override
    public void resolve(final SpellAbility sa) {
        final PlayerCollection tgtPlayers = getTargetPlayers(sa);
        final List<ZoneType> affectedZones = Lists.newArrayList();
        final Game game = sa.getActivatingPlayer().getGame();

        if (sa.hasParam("PumpZone")) {
            affectedZones.addAll(ZoneType.listValueOf(sa.getParam("PumpZone")));
        } else {
            affectedZones.add(ZoneType.Battlefield);
        }

        CardCollectionView list;
        if (!sa.usesTargeting() && !sa.hasParam("Defined")) {
            list = game.getCardsIn(affectedZones);
        } else {
            list = tgtPlayers.getCardsIn(affectedZones);
        }

        String valid = "";
        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        list = AbilityUtils.filterListByType(list, valid, sa);

        List<String> keywords = Lists.newArrayList();
        if (sa.hasParam("KW")) {
            keywords.addAll(Arrays.asList(sa.getParam("KW").split(" & ")));
        }
        final int a = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumAtt"), sa, true);
        final int d = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumDef"), sa, true);
        
        if (sa.hasParam("SharedKeywordsZone")) {
            List<ZoneType> zones = ZoneType.listValueOf(sa.getParam("SharedKeywordsZone"));
            String[] restrictions = new String[] {"Card"};
            if (sa.hasParam("SharedRestrictions"))
                restrictions = sa.getParam("SharedRestrictions").split(",");
            keywords = CardFactoryUtil.sharedKeywords(keywords, restrictions, zones, sa.getHostCard(), sa);
        }
        applyPumpAll(sa, list, a, d, keywords, affectedZones);

        replaceDying(sa);
    }

}
