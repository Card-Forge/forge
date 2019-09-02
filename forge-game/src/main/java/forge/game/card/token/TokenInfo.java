package forge.game.card.token;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import forge.ImageKeys;
import forge.StaticData;
import forge.card.CardType;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardUtil;
import forge.game.keyword.KeywordInterface;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.item.PaperToken;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

public class TokenInfo {
    final String name;
    final String imageName;
    final String manaCost;
    final String[] types;
    final String[] intrinsicKeywords;
    final int basePower;
    final int baseToughness;
    final String color;

    public TokenInfo(String name, String imageName, String manaCost, String[] types,
                     String[] intrinsicKeywords, int basePower, int baseToughness) {
        this.name = name;
        this.imageName = imageName;
        this.manaCost = manaCost;
        this.color = manaCost; // FIXME: somehow ensure that color and mana cost are completely differentiated
        this.types = types;
        this.intrinsicKeywords = intrinsicKeywords;
        this.basePower = basePower;
        this.baseToughness = baseToughness;
    }

    public TokenInfo(Card c) {
        // TODO: Figure out how to handle legacy images?
        this.name = c.getName();
        this.imageName = ImageKeys.getTokenImageName(c.getImageKey());
        this.manaCost = c.getManaCost().toString();
        this.color = MagicColor.toShortString(c.getCurrentState().getColor());
        this.types = getCardTypes(c);
        
        List<String> list = Lists.newArrayList();
        for (KeywordInterface inst : c.getKeywords()) {
            list.add(inst.getOriginal());
        }
        
        this.intrinsicKeywords   = list.toArray(new String[0]);
        this.basePower = c.getBasePower();
        this.baseToughness = c.getBaseToughness();
    }

    public TokenInfo(Card c, Card source) {
        // TODO If Source has type/color changes on it, apply them now.
        // Permanently apply them for casccading tokens? Reef Worm?
        this(c);
    }

    public TokenInfo(String str) {
        final String[] tokenInfo = str.split(",");
        int power = 0;
        int toughness = 0;
        String manaCost = "0";
        String[] types = null;
        String[] keywords = null;
        String imageName = null;
        String color = "";
        for (String info : tokenInfo) {
            int index = info.indexOf(':');
            if (index == -1) {
                continue;
            }
            String remainder = info.substring(index + 1);
            if (info.startsWith("P:")) {
                power = Integer.parseInt(remainder);
            } else if (info.startsWith("T:")) {
                toughness = Integer.parseInt(remainder);
            } else if (info.startsWith("Cost:")) {
                manaCost = remainder;
            } else if (info.startsWith("Types:")) {
                types = remainder.split("-");
            } else if (info.startsWith("Keywords:")) {
                keywords = remainder.split("-");
            } else if (info.startsWith("Image:")) {
                imageName = remainder;
            } else if (info.startsWith("Color:")) {
                color = remainder;
            }
        }

        this.name = tokenInfo[0];
        this.imageName = imageName;
        this.manaCost = manaCost;
        this.types = types;
        this.intrinsicKeywords = keywords;
        this.basePower = power;
        this.baseToughness = toughness;
        this.color = color;
    }

    private static String[] getCardTypes(Card c) {
        List<String> relevantTypes = Lists.newArrayList();
        for (CardType.CoreType t : c.getType().getCoreTypes()) {
            relevantTypes.add(t.name());
        }
        Iterables.addAll(relevantTypes, c.getType().getSubtypes());
        if (c.getType().isLegendary()) {
            relevantTypes.add("Legendary");
        }
        return relevantTypes.toArray(new String[relevantTypes.size()]);
    }

    private Card toCard(Game game) {
        final Card c = new Card(game.nextCardId(), game);
        c.setName(name);
        c.setImageKey(ImageKeys.getTokenKey(imageName));

        c.setColor(color.isEmpty() ? manaCost : color);
        c.setToken(true);

        for (final String t : types) {
            c.addType(t);
        }

        c.setBasePower(basePower);
        c.setBaseToughness(baseToughness);
        return c;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(',');
        sb.append("P:").append(basePower).append(',');
        sb.append("T:").append(baseToughness).append(',');
        sb.append("Cost:").append(manaCost).append(',');
        sb.append("Color:").append(color).append(",");
        sb.append("Types:").append(Joiner.on('-').join(types)).append(',');
        sb.append("Keywords:").append(Joiner.on('-').join(intrinsicKeywords)).append(',');
        sb.append("Image:").append(imageName);
        return sb.toString();
    }

    public static List<Card> makeToken(final Card prototype, final Player controller,
            final boolean applyMultiplier, final int num) {
        final List<Card> list = Lists.newArrayList();

        final Game game = controller.getGame();
        int multiplier = num;
        Player player = controller;
        Card proto = prototype;

        final Map<String, Object> repParams = Maps.newHashMap();
        repParams.put("Event", "CreateToken");
        repParams.put("Affected", player);
        repParams.put("Token", prototype);
        repParams.put("TokenNum", multiplier);
        repParams.put("EffectOnly", applyMultiplier);

        switch (game.getReplacementHandler().run(repParams)) {
            case NotReplaced:
                break;
            case Updated: {
                multiplier = (int) repParams.get("TokenNum");
                player = (Player) repParams.get("Affected");
                proto = (Card) repParams.get("Token");
                break;
            }
            default:
                multiplier = 0;
                break;
        }
        if (multiplier <= 0) {
            return list;
        }

        long timestamp = game.getNextTimestamp();

        for (int i = 0; i < multiplier; i++) {
            // need to set owner or copyCard will fail with assign new ID
            proto.setOwner(player);
            Card copy = CardFactory.copyCard(proto, true);
            copy.setTimestamp(timestamp);
            copy.setToken(true);
            list.add(copy);
        }

        return list;
    }

    public List<Card> makeTokenWithMultiplier(final Player controller, int amount, final boolean applyMultiplier) {
        return makeToken(makeOneToken(controller), controller, applyMultiplier, amount);
    }

    static public List<Card> makeTokensFromPrototype(Card prototype, final Player controller, int amount, final boolean applyMultiplier) {
        return makeToken(prototype, controller, applyMultiplier, amount);
    }

    public Card makeOneToken(final Player controller) {
        final Game game = controller.getGame();
        final Card c = toCard(game);

        c.setOwner(controller);
        c.setToken(true);
        CardFactoryUtil.setupKeywordedAbilities(c);
        // add them later to prevent setupKeywords from adding them multiple times
        for (final String kw : intrinsicKeywords) {
            c.addIntrinsicKeyword(kw);
        }
        return c;
    }

    static public Card getProtoType(final String script, final SpellAbility sa) {
        // script might be null, or sa might be null
        if (script == null || sa == null) {
            return null;
        }
        final Card host = sa.getHostCard();
        final Game game = host.getGame();

        String edition = ObjectUtils.firstNonNull(sa.getOriginalHost(), host).getSetCode();
        PaperToken token = StaticData.instance().getAllTokens().getToken(script, edition);

        if (token != null) {
            final Card result = Card.fromPaperCard(token, null, game);

            if (sa.hasParam("TokenPower")) {
                String str = sa.getParam("TokenPower");
                result.setBasePowerString(str);
                result.setBasePower(AbilityUtils.calculateAmount(host, str, sa));
            }

            if (sa.hasParam("TokenToughness")) {
                String str = sa.getParam("TokenToughness");
                result.setBaseToughnessString(str);
                result.setBaseToughness(AbilityUtils.calculateAmount(host, str, sa));
            }

            // update Token with CardTextChanges
            Map<String, String> colorMap = sa.getChangedTextColors();
            Map<String, String> typeMap = sa.getChangedTextTypes();
            if (!colorMap.isEmpty()) {
                if (!result.isColorless()) {
                    // change Token Colors
                    byte color = CardUtil.getColors(result).getColor();

                    for (final Map.Entry<String, String> e : colorMap.entrySet()) {
                        byte v = MagicColor.fromName(e.getValue());
                        // Any used by Swirl the Mists
                        if ("Any".equals(e.getKey())) {
                            for (final byte c : MagicColor.WUBRG) {
                                // try to replace color flips
                                if ((color & c) != 0) {
                                    color &= ~c;
                                    color |= v;
                                }
                            }
                        } else {
                            byte c = MagicColor.fromName(e.getKey());
                            // try to replace color flips
                            if ((color & c) != 0) {
                                color &= ~c;
                                color |= v;
                            }
                        }
                    }

                    result.setColor(color);
                }
            }
            if (!typeMap.isEmpty()) {
                String oldName = result.getName();

                CardType type = new CardType(result.getType());
                String joinedName = StringUtils.join(type.getSubtypes(), " ");
                final boolean nameGenerated = oldName.equals(joinedName);
                boolean typeChanged = false;

                if (!Iterables.isEmpty(type.getSubtypes())) {
                    for (final Map.Entry<String, String> e : typeMap.entrySet()) {
                        if (type.hasSubtype(e.getKey())) {
                            type.remove(e.getKey());
                            type.add(e.getValue());
                            typeChanged = true;
                        }
                    }
                }

                if (typeChanged) {
                    result.setType(type);

                    // update generated Name
                    if (nameGenerated) {
                        result.setName(StringUtils.join(type.getSubtypes(), " "));
                    }
                }
            }

            // replace Intrinsic Keyword
            List<KeywordInterface> toRemove = Lists.newArrayList();
            List<String> toAdd = Lists.newArrayList();
            for (final KeywordInterface k : result.getCurrentState().getIntrinsicKeywords()) {
                final String o = k.getOriginal();
                // only Modifiable should go there
                if (!CardUtil.isKeywordModifiable(o)) {
                    continue;
                }
                String r = o;
                // replace types
                for (final Map.Entry<String, String> e : typeMap.entrySet()) {
                    final String key = e.getKey();
                    final String pkey = CardType.getPluralType(key);
                    final String value = e.getValue();
                    final String pvalue = CardType.getPluralType(e.getValue());
                    r = r.replaceAll(pkey, pvalue);
                    r = r.replaceAll(key, value);
                }
                // replace color words
                for (final Map.Entry<String, String> e : colorMap.entrySet()) {
                    final String vName = e.getValue();
                    final String vCaps = StringUtils.capitalize(vName);
                    final String vLow = vName.toLowerCase();
                    if ("Any".equals(e.getKey())) {
                        for (final byte c : MagicColor.WUBRG) {
                            final String cName = MagicColor.toLongString(c);
                            final String cNameCaps = StringUtils.capitalize(cName);
                            final String cNameLow = cName.toLowerCase();
                            r = r.replaceAll(cNameCaps, vCaps);
                            r = r.replaceAll(cNameLow, vLow);
                        }
                    } else {
                        final String cName = e.getKey();
                        final String cNameCaps = StringUtils.capitalize(cName);
                        final String cNameLow = cName.toLowerCase();
                        r = r.replaceAll(cNameCaps, vCaps);
                        r = r.replaceAll(cNameLow, vLow);
                    }
                }
                if (!r.equals(o)) {
                    toRemove.add(k);
                    toAdd.add(r);
                }
            }
            for (final KeywordInterface k : toRemove) {
                result.getCurrentState().removeIntrinsicKeyword(k);
            }
            result.addIntrinsicKeywords(toAdd);

            result.getCurrentState().changeTextIntrinsic(colorMap, typeMap);
            return result;
        }

        return null;
    }
}