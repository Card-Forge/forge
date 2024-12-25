package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import forge.GameCommand;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.TextUtil;

public class ProtectAllEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Card> tgtCards = getTargetCards(sa);

        if (tgtCards.size() > 0) {
            sb.append("Valid card gain protection");
            if (!"Permanent".equals(sa.getParam("Duration"))) {
                sb.append(" until end of turn");
            }
            sb.append(".");
        }

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = sa.getActivatingPlayer().getGame();
        final long timestamp = game.getNextTimestamp();

        final boolean isChoice = sa.getParam("Gains").contains("Choice");
        final List<String> choices = ProtectEffect.getProtectionList(sa);
        final List<String> gains = new ArrayList<>();
        if (isChoice) {
            Player choser = sa.getActivatingPlayer();
            final String choice = choser.getController().chooseProtectionType(Localizer.getInstance().getMessage("lblChooseAProtection"), sa, choices);
            if( null == choice)
                return;
            gains.add(choice);
            game.getAction().notifyOfValue(sa, choser, Lang.joinHomogenous(gains), choser);
        } else {
            if (sa.getParam("Gains").equals("ChosenColor")) {
                for (final String color : host.getChosenColors()) {
                    gains.add(color.toLowerCase());
                }
            } else if (sa.getParam("Gains").equals("TargetedCardColor")) {
                for (final Card c : sa.getSATargetingCard().getTargets().getTargetCards()) {
                    ColorSet cs = c.getColor();
                    for (byte col : MagicColor.WUBRG) {
                        if (cs.hasAnyColor(col))
                            gains.add(MagicColor.toLongString(col).toLowerCase());
                    }
                }
            } else {
                gains.addAll(choices);
            }
        }

        List<String> gainsKWList = Lists.newArrayList();
        for (String color : gains) {
            gainsKWList.add(TextUtil.concatWithSpace("Protection from", color));
        }

        // Deal with permanents
        final String valid = sa.getParamOrDefault("ValidCards", "");
        if (!valid.isEmpty()) {
            CardCollectionView list = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), valid, sa.getActivatingPlayer(), host, sa);

            for (final Card tgtC : list) {
                tgtC.addChangedCardKeywords(gainsKWList, null, false, timestamp, null, true);

                if (!"Permanent".equals(sa.getParam("Duration"))) {
                    // If not Permanent, remove protection at EOT
                    final GameCommand untilEOT = new GameCommand() {
                        private static final long serialVersionUID = -6573962672873853565L;

                        @Override
                        public void run() {
                            if (tgtC.isInPlay()) {
                                tgtC.removeChangedCardKeywords(timestamp, 0, true);
                            }
                        }
                    };
                    addUntilCommand(sa, untilEOT);
                }
            }
        }

        // Deal with Players
        final String players = sa.getParamOrDefault("ValidPlayers", "");
        if (!players.isEmpty()) {
            for (final Player player : AbilityUtils.getDefinedPlayers(host, players, sa)) {
                player.addChangedKeywords(gainsKWList, ImmutableList.of(), timestamp, 0);

                if (!"Permanent".equals(sa.getParam("Duration"))) {
                    // If not Permanent, remove protection at EOT
                    final GameCommand revokeCommand = new GameCommand() {
                        private static final long serialVersionUID = -6573962672873853565L;

                        @Override
                        public void run() {
                            player.removeChangedKeywords(timestamp, 0);
                        }
                    };
                    addUntilCommand(sa, revokeCommand);
                }
            }
        }
    }

}
