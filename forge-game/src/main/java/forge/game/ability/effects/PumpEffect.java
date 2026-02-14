package forge.game.ability.effects;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import forge.util.*;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import forge.GameCommand;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardUtil;
import forge.game.card.perpetual.PerpetualKeywords;
import forge.game.card.perpetual.PerpetualPTBoost;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class PumpEffect extends SpellAbilityEffect {

    private static void applyPump(final SpellAbility sa, final Card applyTo,
            final int a, final int d, final List<String> keywords,
            final long timestamp) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final String duration = sa.getParam("Duration");
        final boolean perpetual = "Perpetual".equals(duration);

        // do Game Check there in case of LKI
        final Card gameCard = game.getCardState(applyTo, null);
        if (gameCard == null || !applyTo.equalsWithGameTimestamp(gameCard)) {
            return;
        }
        final List<String> kws = Lists.newArrayList();
        final List<String> hiddenKws = Lists.newArrayList();

        boolean redrawPT = false;
        for (String kw : keywords) {
            if (kw.startsWith("HIDDEN")) {
                hiddenKws.add(kw.substring(7));
                redrawPT |= kw.contains("CARDNAME's power and toughness are switched");
            } else {
                kws.add(kw);
            }
        }

        if (a != 0 || d != 0) {
            if (perpetual) {
                gameCard.addPerpetual(new PerpetualPTBoost(timestamp, a, d));
            }
            gameCard.addPTBoost(a, d, timestamp, 0);
            redrawPT = true;
        }

        if (!kws.isEmpty()) {
            if (perpetual) {
                gameCard.addPerpetual(new PerpetualKeywords(timestamp, kws, Lists.newArrayList(), false));
            }
            gameCard.addChangedCardKeywords(kws, Lists.newArrayList(), false, timestamp, null);
        }
        if (!hiddenKws.isEmpty()) {
            gameCard.addHiddenExtrinsicKeywords(timestamp, 0, hiddenKws);
        }
        if (redrawPT) {
            gameCard.updatePTforView();
        }

        if (sa.hasParam("CanBlockAny")) {
            gameCard.addCanBlockAny(timestamp);
        }
        if (sa.hasParam("CanBlockAmount")) {
            int v = AbilityUtils.calculateAmount(host, sa.getParam("CanBlockAmount"), sa, true);
            gameCard.addCanBlockAdditional(v, timestamp);
        }

        if (sa.hasParam("LeaveBattlefield")) {
            addLeaveBattlefieldReplacement(gameCard, sa, sa.getParam("LeaveBattlefield"));
        }

        if (sa.hasParam("RememberPumped")) {
            host.addRemembered(gameCard);
        }

        if (!"Permanent".equals(duration) && !perpetual) {
            // If not Permanent, remove Pumped at EOT
            final GameCommand untilEOT = new GameCommand() {
                private static final long serialVersionUID = -42244224L;

                @Override
                public void run() {
                    host.removeGainControlTargets(gameCard);

                    gameCard.removePTBoost(timestamp, 0);
                    boolean updateText = gameCard.removeCanBlockAny(timestamp);
                    updateText |= gameCard.removeCanBlockAdditional(timestamp);

                    if (keywords.size() > 0) {
                        gameCard.removeHiddenExtrinsicKeywords(timestamp, 0);
                        gameCard.removeChangedCardKeywords(timestamp, 0);
                    }
                    gameCard.updatePTforView();
                    if (updateText) {
                        gameCard.updateAbilityTextForView();
                    }

                    game.fireEvent(new GameEventCardStatsChanged(gameCard));
                }
            };
            if ("UntilUntaps".equals(duration)) {
                host.addGainControlTarget(gameCard);
            }
            addUntilCommand(sa, untilEOT);
        }
        game.fireEvent(new GameEventCardStatsChanged(gameCard));
    }

    private static void applyPump(final SpellAbility sa, final Player p,
            final List<String> keywords, final long timestamp) {
        final String duration = sa.getParam("Duration");

        if (!keywords.isEmpty()) {
            p.addChangedKeywords(keywords, ImmutableList.of(), timestamp, 0);
        }

        if (!"Permanent".equals(duration)) {
            // If not Permanent, remove Pumped at EOT
            final GameCommand untilEOT = new GameCommand() {
                private static final long serialVersionUID = -32453460L;

                @Override
                public void run() {
                    p.removeChangedKeywords(timestamp, 0);
                }
            };
            addUntilCommand(sa, untilEOT);
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
        tgts.addAll(getCardsfromTargets(sa));
        if ((sa.usesTargeting() && sa.getTargetRestrictions().canTgtPlayer()) || sa.hasParam("Defined")) {
            tgts.addAll(getTargetPlayers(sa));
        }

        if (tgts.size() > 0) {
            List<String> keywords = Lists.newArrayList();
            if (sa.hasParam("KW")) {
                if (sa.getParam("KW").equals("HIDDEN This card doesn't untap during your next untap step.")) {
                    if (sa instanceof AbilitySub) {
                        sb.append(tgts.size() == 1 ? "It doesn't " : "They don't ");
                    } else {
                        sb.append(Lang.joinHomogenous(tgts)).append(tgts.size() == 1 ? " doesn't " : " don't ");
                    }
                    sb.append("untap during ");
                    String whose = "your";
                    for (GameEntity t : tgts) {
                        final Card c = (Card) t;
                        if (!(c.getOwner() == sa.getActivatingPlayer())) {
                            whose = (tgts.size() == 1 ? "its controller's" : "their controller's");
                            break;
                        }
                    }
                    sb.append(whose).append(" next untap step.");
                    return sb.toString();
                }
                keywords.addAll(Arrays.asList(sa.getParam("KW").split(" & ")));
            }

            if (sa.hasParam("IfDesc")) {
                if (sa.getParam("IfDesc").equals("True") && sa.hasParam("SpellDescription")) {
                    String ifDesc = sa.getParam("SpellDescription");
                    sb.append(ifDesc, 0, ifDesc.indexOf(",") + 1);
                } else {
                    sb.append(sa.getParam("IfDesc"));
                }
                sb.append(" ");
            }

            if (sa instanceof AbilitySub && sa.getRootAbility().getTargets().containsAll(tgts)) {
                //try to avoid having the same long list of targets twice in a StackDescription
                sb.append(tgts.size() == 1 && tgts.get(0) instanceof Card ? "It " : "They ");
            } else {
                sb.append(Lang.joinHomogenous(tgts)).append(" ");
            }

            if (sa.hasParam("Radiance")) {
                sb.append("and each other ").append(sa.getParam("ValidTgts"))
                        .append(" that shares a color with ");
                sb.append(tgts.size() > 1 ? "them " : "it ");
            }

            final int atk = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumAtt"), sa, true);
            final int def = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumDef"), sa, true);

            boolean gets = sa.hasParam("NumAtt") || sa.hasParam("NumDef");
            boolean gains = !keywords.isEmpty();

            if (gets) {
                sb.append("gets ");
                if (atk != 0) {
                    sb.append(atk > 0 ? "+" : "").append(atk).append("/");
                } else {
                    sb.append(def < 0 ? "-" : "+").append(atk).append("/");
                }
                if (def != 0) {
                    sb.append(def > 0 ? "+" : "").append(def).append(" ");
                } else {
                    sb.append(atk < 0 ? "-" : "+").append(def).append(" ");
                }
                sb.append(gains ? "and gains " : "");
            } else if (gains) {
                sb.append("gains ");
            }

            for (int i = 0; i < keywords.size(); i++) {
                sb.append(keywords.get(i).toLowerCase());
                sb.append(keywords.size() > 2 && i+1 != keywords.size() ? ", " : "");
                sb.append(keywords.size() == 2 && i == 0 ? " " : "");
                sb.append(i+2 == keywords.size() ? "and " : "");
            }

            if (sa.hasParam("CanBlockAny")) {
                if (gets || gains) {
                    sb.append(" and ");
                }
                sb.append("can block any number of creatures");
            } else if (sa.hasParam("CanBlockAmount")) {
                if (gets || gains) {
                    sb.append(" and ");
                }
                String n = sa.getParam("CanBlockAmount");
                sb.append("can block an additional ");
                sb.append("1".equals(n) ? "creature" : Lang.nounWithNumeral(n, "creature"));
                sb.append(" each combat");
            }

            String duration = sa.getParam("Duration");
            if (!"Permanent".equals(duration)) {
                if ("UntilUntaps".equals(duration)) {
                    sb.append(" for as long as CARDNAME remains tapped.");
                } else {
                    sb.append(" until end of turn.");
                }
            } else {
                sb.append(".");
            }
        }

        return sb.toString();
    }

    @Override
    public void resolve(final SpellAbility sa) {
        if (!checkValidDuration(sa.getParam("Duration"), sa)) {
            return;
        }

        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();
        final Card host = sa.getHostCard();
        final long timestamp = game.getNextTimestamp();
        List<Card> tgtCards = getCardsfromTargets(sa);
        List<Player> tgtPlayers = getTargetPlayers(sa);

        if (sa.hasParam("Optional")) {
            final String targets = Lang.joinHomogenous(tgtCards);
            final String message = sa.hasParam("OptionQuestion")
                    ? TextUtil.fastReplace(sa.getParam("OptionQuestion"), "TARGETS", targets)
                    : Localizer.getInstance().getMessage("lblApplyPumpToTarget", targets);

            if (!activator.getController().confirmAction(sa, null, message, null)) {
                return;
            }
        }

        List<String> keywords = Lists.newArrayList();
        if (sa.hasParam("KW")) {
            keywords.addAll(Arrays.asList(sa.getParam("KW").split(" & ")));
        } else if (sa.hasParam("KWChoice")) {
            List<String> options = Arrays.asList(sa.getParam("KWChoice").split(","));
            String chosen = activator.getController().chooseKeywordForPump(options, sa,
                    Localizer.getInstance().getMessage("lblChooseKeyword"), tgtCards.get(0));
            keywords.add(chosen);
        }
        
        int a = 0;
        int d = 0;
        if (sa.hasParam("NumAtt") && !sa.getParam("NumAtt").equals("Double") && !sa.getParam("NumAtt").equals("Triple")) {
            a = AbilityUtils.calculateAmount(host, sa.getParam("NumAtt"), sa, true);
        }
        if (sa.hasParam("NumDef") && !sa.getParam("NumDef").equals("Double") && !sa.getParam("NumDef").equals("Triple")) {
            d = AbilityUtils.calculateAmount(host, sa.getParam("NumDef"), sa, true);
        }

        if (sa.hasParam("SharedKeywordsZone")) {
            List<ZoneType> zones = ZoneType.listValueOf(sa.getParam("SharedKeywordsZone"));
            String[] restrictions = sa.hasParam("SharedRestrictions") ? sa.getParam("SharedRestrictions").split(",") : new String[]{"Card"};
            keywords = CardFactoryUtil.sharedKeywords(keywords, restrictions, zones, host, sa);
        }

        if (sa.hasParam("DefinedKW")) {
            String defined = sa.getParam("DefinedKW");
            if (defined.equals("ChosenType")) {
                if (!host.hasChosenType()) {
                    return;
                }
                String replaced = host.getChosenType();
                for (int i = 0; i < keywords.size(); i++) {
                    String s = keywords.get(i);
                    s = s.replaceAll(defined, replaced);
                    keywords.set(i, s);
                }
            } else if (defined.equals("ChosenPlayer")) {
                if (!host.hasChosenPlayer()) {
                    return;
                }
                Player cp = host.getChosenPlayer();
                for (int i = 0; i < keywords.size(); i++) {
                    String s = keywords.get(i);
                    s = s.replaceAll("ChosenPlayerUID", String.valueOf(cp.getId()));
                    s = s.replaceAll("ChosenPlayerName", cp.getName());
                    keywords.set(i, s);
                }
            } else if (defined.equals("ChosenColor")) {
                if (!host.hasChosenColor()) {
                    return;
                }
                for (int i = 0; i < keywords.size(); i++) {
                    String s = keywords.get(i);
                    s = s.replaceAll("ChosenColor", StringUtils.capitalize(host.getChosenColor()));
                    s = s.replaceAll("chosenColor", host.getChosenColor().toLowerCase());
                    keywords.set(i, s);
                }
            } else { // anything else needs to be defined players?
                PlayerCollection players = AbilityUtils.getDefinedPlayers(host, defined, sa);
                if (players.isEmpty()) return;
                List<String> newKeywords = Lists.newArrayList();
                keywords.removeIf(input -> {
                    if (!input.contains("ChosenPlayerUID") && !input.contains("ChosenPlayerName")) {
                        return false;
                    }
                    for (Player p : players) {
                        String replacedID = String.valueOf(p.getId());
                        String replacedName = p.getName();

                        String s = input.replaceAll("ChosenPlayerUID", replacedID);
                        s = s.replaceAll("ChosenPlayerName", replacedName);
                        newKeywords.add(s);
                    }
                    return true;
                });
                keywords.addAll(newKeywords);
            }
        }
        if (sa.hasParam("DefinedLandwalk")) {
            final String landtype = sa.getParam("DefinedLandwalk");
            for (final Card c : AbilityUtils.getDefinedCards(host, landtype, sa)) {
                for (String type : c.getType().getLandTypes()) {
                    keywords.add("Landwalk:" +type);
                }
            }
        }
        if (sa.hasParam("RandomKeyword")) {
            final String num = sa.getParamOrDefault("RandomKWNum", "1");
            final int numkw = AbilityUtils.calculateAmount(host, num, sa);
            List<String> choice = Lists.newArrayList();
            List<String> total = Lists.newArrayList(keywords);
            if (sa.hasParam("NoRepetition")) {
                for (String kw : keywords) {
                    if (tgtCards.get(0).hasKeyword(kw)) {
                        total.remove(kw);
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

        if (sa.hasParam("RememberObjects")) {
            host.addRemembered(AbilityUtils.getDefinedObjects(host, sa.getParam("RememberObjects"), sa));
        }

        if (sa.hasParam("NoteCardsFor")) {
            for (final Card c : AbilityUtils.getDefinedCards(host, sa.getParam("NoteCards"), sa)) {
                for (Player p : tgtPlayers) {
                    p.addNoteForName(sa.getParam("NoteCardsFor"), "Id:" + c.getId());
                }
            }
        }
        if (sa.hasParam("ClearNotedCardsFor")) {
            for (Player p : tgtPlayers) {
                for (String s : sa.getParam("ClearNotedCardsFor").split(",")) {
                    p.clearNotesForName(s);
                }
            }
        }

        if (sa.hasParam("NoteNumber")) {
            int num = AbilityUtils.calculateAmount(host, sa.getParam("NoteNumber"), sa);
            for (Player p : tgtPlayers) {
                p.noteNumberForName(host.getName(), num);
            }
        }

        if (sa.hasParam("ForgetObjects")) {
            host.removeRemembered(AbilityUtils.getDefinedObjects(host, sa.getParam("ForgetObjects"), sa));
        }

        if (sa.hasParam("ImprintCards")) {
            host.addImprintedCards(AbilityUtils.getDefinedCards(host, sa.getParam("ImprintCards"), sa));
        }

        if (sa.hasParam("ForgetImprinted")) {
            host.removeImprintedCards(AbilityUtils.getDefinedCards(host, sa.getParam("ForgetImprinted"), sa));
        }

        List<ZoneType> pumpZones = sa.hasParam("PumpZone") ? ZoneType.listValueOf(sa.getParam("PumpZone"))
                : ZoneType.listValueOf("Battlefield");

        for (Card tgtC : tgtCards) {
            // CR 702.26e
            if (tgtC.isPhasedOut()) {
                continue;
            }

            // only pump things in PumpZones
            if (!tgtC.isInZones(pumpZones)) {
                continue;
            }

            // substitute specific tgtC mana cost for keyword placeholder CardManaCost
            List<String> affectedKeywords = Lists.newArrayList(keywords);

            if (!affectedKeywords.isEmpty()) {
                affectedKeywords = affectedKeywords.stream().map(input -> {
                    if (input.contains("CardManaCost")) {
                        input = input.replace("CardManaCost", tgtC.getManaCost().getShortString());
                    } else if (input.contains("ConvertedManaCost")) {
                        final String costcmc = Integer.toString(tgtC.getCMC());
                        input = input.replace("ConvertedManaCost", costcmc);
                    }
                    return input;
                }).collect(Collectors.toList());
            }

            if (sa.hasParam("NumAtt") && sa.getParam("NumAtt").equals("Double")) {
                a = tgtC.getNetPower();
            }
            if (sa.hasParam("NumDef") && sa.getParam("NumDef").equals("Double")) {
                d = tgtC.getNetToughness();
            }

            if (sa.hasParam("NumAtt") && sa.getParam("NumAtt").equals("Triple")) {
                a = tgtC.getNetPower() *2;
            }
            if (sa.hasParam("NumDef") && sa.getParam("NumDef").equals("Triple")) {
                d = tgtC.getNetToughness() *2;
            }

            applyPump(sa, tgtC, a, d, affectedKeywords, timestamp);
        }

        if (sa.hasParam("AtEOT") && !tgtCards.isEmpty()) {
            registerDelayedTrigger(sa, sa.getParam("AtEOT"), tgtCards);
        }

        for (final Card tgtC : CardUtil.getRadiance(sa)) {
            // only pump things in PumpZone
            if (!tgtC.isInZones(pumpZones)) {
                continue;
            }

            applyPump(sa, tgtC, a, d, keywords, timestamp);
        }

        for (Player p : tgtPlayers) {
            if (!p.isInGame()) {
                continue;
            }

            applyPump(sa, p, keywords, timestamp);
        }

        replaceDying(sa);
    }
}
