package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

            String name = sa.getParamOrDefault("Name", "");
            if (name.equals("ChosenName")) {
                if (sa.getHostCard().hasChosenName()) {
                    name = sa.getHostCard().getChosenName();
                } else {
                    continue;
                }
            }
            if (sa.hasParam("DefinedName")) {
                final CardCollection def = AbilityUtils.getDefinedCards(source, sa.getParam("DefinedName"), sa);
                if (def.size() > 0) {
                    name = def.getFirst().getName();
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
                    name = Aggregates.random(faces).getName();
                } else {
                    name = player.getController().chooseCardName(sa, faces,
                            Localizer.getInstance().getMessage("lblChooseFromSpellbook", CardTranslation.getTranslatedName(source.getName())));
                }
            }
            final ZoneType zone = ZoneType.smartValueOf(sa.getParamOrDefault("Zone", "Library"));
            int amount = sa.hasParam("Amount") ?
                    AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Amount"), sa) : 1;

            CardCollection cards = new CardCollection();

            if (!name.equals("")) {
                while (amount > 0) {
                    Card card = Card.fromPaperCard(StaticData.instance().getCommonCards().getUniqueByName(name), player);
                    card.setTokenCard(true);
                    game.getAction().moveTo(ZoneType.None, card, sa, moveParams);
                    cards.add(card);
                    amount--;
                }

                final CardZoneTable triggerList = new CardZoneTable();
                for (final Card c : cards) {
                    Card made = game.getAction().moveTo(zone, c, sa, moveParams);
                    triggerList.put(ZoneType.None, made.getZone().getZoneType(), made);
                    if (sa.hasParam("RememberMade")) {
                        sa.getHostCard().addRemembered(made);
                    }
                    if (sa.hasParam("ImprintMade")) {
                        sa.getHostCard().addImprintedCard(made);
                    }
                }
                triggerList.triggerChangesZoneAll(game, sa);
                if (zone.equals(ZoneType.Library)) {
                    player.shuffle(sa);}
                }
                    final StringBuilder sb = new StringBuilder();
                    final Card host = sa.getHostCard();
                    Card tgtCard = host;
                    final List<String> pumpKeywords = Lists.newArrayList();

                    if (sa.hasParam("Keywords")) {
                        pumpKeywords.removeAll(Arrays.asList(sa.getParam("Keywords").split(" & ")));
                    }
            }
        }
    }

