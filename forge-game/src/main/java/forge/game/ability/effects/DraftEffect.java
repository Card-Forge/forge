package forge.game.ability.effects;

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
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

import java.util.*;

 public class DraftEffect extends SpellAbilityEffect {
     @Override
     protected String getStackDescription(SpellAbility sa) {
         final Card source = sa.getHostCard();
         final Player player = AbilityUtils.getDefinedPlayers(source, sa.getParam("Defined"), sa).get(0);
         final ZoneType zone = ZoneType.smartValueOf(sa.getParamOrDefault("Zone", "Hand"));

         final StringBuilder sb = new StringBuilder();

         sb.append(player).append(" drafts a card from ").append(source.getName()).append("'s spellbook");
         if (zone.equals("Hand")) {
             sb.append(".");
         } else if (zone.equals("Battlefield")) {
             sb.append(" and puts it onto the battlefield.");
         } else if (zone.equals("Exile")) {
             sb.append(", then exiles it.");
         }

         return sb.toString();
     }

     @Override
     public void resolve(SpellAbility sa) {
         Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
         moveParams.put(AbilityKey.LastStateBattlefield, sa.getLastStateBattlefield());
         moveParams.put(AbilityKey.LastStateGraveyard, sa.getLastStateGraveyard());
         final Card source = sa.getHostCard();
         final Player player = AbilityUtils.getDefinedPlayers(source, sa.getParam("Defined"), sa).get(0);
         final Game game = player.getGame();
         final ZoneType zone = ZoneType.smartValueOf(sa.getParamOrDefault("Zone", "Hand"));
         List<String> spellbook = Arrays.asList(sa.getParam("Spellbook").split(","));
         final int numToDraft = AbilityUtils.calculateAmount(source,
                 sa.getParamOrDefault("DraftNum", "1"), sa);
         CardCollection drafted = new CardCollection();

         for (int i = 0; i < numToDraft; i++) {
             String chosen = "";
             Collections.shuffle(spellbook);
             List<ICardFace> faces = new ArrayList<>();
             for (String name : spellbook.subList(0, 3)) {
                 // Cardnames that include "," must use ";" instead in Spellbook$ (i.e. Tovolar; Dire Overlord)
                 // name = name.replace(";", ",");
                 // faces.add(StaticData.instance().getCommonCards().getFaceByName(name));
             }
             chosen = player.getController().chooseCardName(sa, faces,
                     Localizer.getInstance().getMessage("lblChooseCardDraft"));
             if (!chosen.equals("")) {
                 Card card = Card.fromPaperCard(StaticData.instance().getCommonCards().getUniqueByName(chosen), player);
                 card.setTokenCard(true);
                 game.getAction().moveTo(ZoneType.None, card, sa, moveParams);
                 drafted.add(card);
             }
         }

         final CardZoneTable triggerList = new CardZoneTable();
         for (final Card c : drafted) {
             game.getAction().moveTo(zone, c, sa, moveParams);
             if (c != null) {
                 triggerList.put(ZoneType.None, c.getZone().getZoneType(), c);
             }
             if (sa.hasParam("RememberDrafted")) {
                 source.addRemembered(c);
             }
         }
         triggerList.triggerChangesZoneAll(game, sa);
         if (zone.equals(ZoneType.Library)) {
             player.shuffle(sa);
         }
     }
 }
