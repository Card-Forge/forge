package forge.game.ability.effects;

import forge.GameCommand;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class ProtectAllEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Card> tgtCards = getTargetCards(sa);

        if (tgtCards.size() > 0) {
            sb.append("Valid card gain protection");
            if (!sa.hasParam("Permanent")) {
                sb.append(" until end of turn");
            }
            sb.append(".");
        }

        return sb.toString();
    } // protectStackDescription()

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = sa.getActivatingPlayer().getGame();
        final Long timestamp = Long.valueOf(game.getNextTimestamp());

        final boolean isChoice = sa.getParam("Gains").contains("Choice");
        final List<String> choices = ProtectEffect.getProtectionList(sa);
        final List<String> gains = new ArrayList<String>();
        if (isChoice) {
            Player choser = sa.getActivatingPlayer();
            final String choice = choser.getController().chooseProtectionType("Choose a protection", sa, choices);
            if( null == choice)
                return;
            gains.add(choice);
            game.getAction().nofityOfValue(sa, choser, Lang.joinHomogenous(gains), choser);
        } else {
            if (sa.getParam("Gains").equals("ChosenColor")) {
                for (final String color : host.getChosenColors()) {
                    gains.add(color.toLowerCase());
                }
            } else if (sa.getParam("Gains").equals("TargetedCardColor")) {
                for (final Card c : sa.getSATargetingCard().getTargets().getTargetCards()) {
                    ColorSet cs = CardUtil.getColors(c);
                    for(byte col : MagicColor.WUBRG) {
                        if (cs.hasAnyColor(col))
                            gains.add(MagicColor.toLongString(col).toLowerCase());
                    }
                }
            } else {
                gains.addAll(choices);
            }
        }

        // Deal with permanents
        String valid = "";
        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }
        if (!valid.equals("")) {
            CardCollectionView list = game.getCardsIn(ZoneType.Battlefield);
            list = CardLists.getValidCards(list, valid, sa.getActivatingPlayer(), host);

            for (final Card tgtC : list) {
                if (tgtC.isInPlay()) {
                    for (final String gain : gains) {
                        tgtC.addExtrinsicKeyword("Protection from " + gain);
                        tgtC.updateKeywords();
                    }

                    if (!sa.hasParam("Permanent")) {
                        // If not Permanent, remove protection at EOT
                        final GameCommand untilEOT = new GameCommand() {
                            private static final long serialVersionUID = -6573962672873853565L;

                            @Override
                            public void run() {
                                if (tgtC.isInPlay()) {
                                    for (final String gain : gains) {
                                        tgtC.removeExtrinsicKeyword("Protection from " + gain);
                                    }
                                }
                            }
                        };
                        if (sa.hasParam("UntilEndOfCombat")) {
                            game.getEndOfCombat().addUntil(untilEOT);
                        } else {
                            game.getEndOfTurn().addUntil(untilEOT);
                        }
                    }
                }
            }
        }

        // Deal with Players
        String players = "";
        if (sa.hasParam("ValidPlayers")) {
            players = sa.getParam("ValidPlayers");
        }
        if (!players.equals("")) {
            final List<Player> playerList = AbilityUtils.getDefinedPlayers(host, players, sa);
            for (final Player player : playerList) {
                for (final String gain : gains) {
                    player.addChangedKeywords(ImmutableList.of("Protection from " + gain), ImmutableList.<String>of(), timestamp);
                }

                if (!sa.hasParam("Permanent")) {
                    // If not Permanent, remove protection at EOT
                    final GameCommand revokeCommand = new GameCommand() {
                        private static final long serialVersionUID = -6573962672873853565L;

                        @Override
                        public void run() {
                            player.removeChangedKeywords(timestamp);
                        }
                    };
                    if (sa.hasParam("UntilEndOfCombat")) {
                        game.getEndOfCombat().addUntil(revokeCommand);
                    } else {
                        game.getEndOfTurn().addUntil(revokeCommand);
                    }
                }
            }
        }

    } // protectAllResolve()

}
