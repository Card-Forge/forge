package forge.game.ability.effects;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.Lists;
import forge.StaticData;
import forge.card.ICardFace;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.CardTranslation;
import forge.util.Localizer;

public class MakeCardEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        moveParams.put(AbilityKey.LastStateBattlefield, sa.getLastStateBattlefield());
        moveParams.put(AbilityKey.LastStateGraveyard, sa.getLastStateGraveyard());
        final Card source = sa.getHostCard();
        final PlayerCollection players = AbilityUtils.getDefinedPlayers(source, sa.getParam("Defined"), sa);

        for (final Player player : players) {
            final Game game = player.getGame();

            List<String> names = Lists.newArrayList();
            if (sa.hasParam("Name")) {
                final String n = sa.getParam("Name");
                if (n.equals("ChosenName")) {
                    if (source.hasChosenName()) {
                        names.add(source.getChosenName());
                    } else {
                        names.add(n);
                    }
                }
            }
            if (sa.hasParam("DefinedName")) {
                final CardCollection def = AbilityUtils.getDefinedCards(source, sa.getParam("DefinedName"), sa);
                if (def.size() > 0) {
                    for (final Card c : def) {
                        names.add(c.getName());
                    }
                }
            } else if (sa.hasParam("Spellbook")) {
                List<String> spellbook = Arrays.asList(sa.getParam("Spellbook").split(","));
                List<ICardFace> faces = new ArrayList<>();
                for (String s : spellbook) {
                    // Cardnames that include "," must use ";" instead in Spellbook$ (i.e. Tovolar; Dire Overlord)
                    s = s.replace(";", ",");
                    faces.add(StaticData.instance().getCommonCards().getFaceByName(s));
                }
                if (sa.hasParam("AtRandom")) {
                    names.add(Aggregates.random(faces).getName());
                } else {
                    names.add(player.getController().chooseCardName(sa, faces,
                            Localizer.getInstance().getMessage("lblChooseFromSpellbook",
                                    CardTranslation.getTranslatedName(source.getName()))));
                }
            }
            final ZoneType zone = ZoneType.smartValueOf(sa.getParamOrDefault("Zone", "Library"));
            final int amount = sa.hasParam("Amount") ?
                    AbilityUtils.calculateAmount(source, sa.getParam("Amount"), sa) : 1;

            CardCollection cards = new CardCollection();

            for (final String name : names) {
                int toMake = amount;
                if (!name.equals("")) {
                    while (toMake > 0) {
                        Card card = Card.fromPaperCard(StaticData.instance().getCommonCards().getUniqueByName(name),
                                player);
                        if (sa.hasParam("TokenCard")) {
                            card.setTokenCard(true);
                        }
                        game.getAction().moveTo(ZoneType.None, card, sa, moveParams);
                        cards.add(card);
                        toMake--;
                    }
                }
            }

            final CardZoneTable triggerList = new CardZoneTable();
            for (final Card c : cards) {
                Card made = game.getAction().moveTo(zone, c, sa, moveParams);
                triggerList.put(ZoneType.None, made.getZone().getZoneType(), made);
                if (sa.hasParam("RememberMade")) {
                    source.addRemembered(made);
                }
                if (sa.hasParam("ImprintMade")) {
                    source.addImprintedCard(made);
                }
            }
            triggerList.triggerChangesZoneAll(game, sa);
            if (zone.equals(ZoneType.Library)) {
                player.shuffle(sa);
            }
        }
    }
}
