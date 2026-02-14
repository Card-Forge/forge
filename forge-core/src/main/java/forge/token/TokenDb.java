package forge.token;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import forge.StaticData;
import forge.card.CardDb;
import forge.card.CardEdition;
import forge.card.CardRules;
import forge.item.IPaperCard;
import forge.item.PaperToken;
import forge.util.Aggregates;

import java.util.*;
import java.util.function.Predicate;

public class TokenDb implements ITokenDatabase {
    // Expected naming convention of scripts
    // token_name
    // minor_demon
    // marit_lage
    // gold

    // colors_power_toughness_cardtypes_sub_types_keywords
    // Some examples:
    // c_3_3_a_phyrexian_wurm_lifelink
    // w_2_2_knight_first_strike

    // The image names should be the same as the script name + _set
    // If that isn't found, consider falling back to the original token
    private final Multimap<String, PaperToken> allTokenByName = HashMultimap.create();
    private final Map<String, PaperToken> extraTokensByName = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);

    private final CardEdition.Collection editions;
    private final Map<String, CardRules> rulesByName;

    public TokenDb(Map<String, CardRules> rules, CardEdition.Collection editions) {
        this.rulesByName = rules;
        this.editions = editions;
    }

    public boolean containsRule(String rule) {
        return this.rulesByName.containsKey(rule);
    }

    public void preloadTokens() {
        for (CardEdition edition : this.editions) {
            for (Map.Entry<String, Collection<CardEdition.EditionEntry>> inSet : edition.getTokens().asMap().entrySet()) {
                String name = inSet.getKey();
                String fullName = String.format("%s_%s", name, edition.getCode().toLowerCase());
                for (CardEdition.EditionEntry t : inSet.getValue()) {
                    allTokenByName.put(fullName, addTokenInSet(edition, name, t));
                }
            }
        }
    }

    protected boolean loadTokenFromSet(CardEdition edition, String name) {
        String fullName = String.format("%s_%s", name, edition.getCode().toLowerCase());
        if (allTokenByName.containsKey(fullName)) {
            return true;
        }
        if (!edition.getTokens().containsKey(name)) {
            return false;
        }

        for (CardEdition.EditionEntry t : edition.getTokens().get(name)) {
            allTokenByName.put(fullName, addTokenInSet(edition, name, t));
        }
        return true;
    }

    protected PaperToken addTokenInSet(CardEdition edition, String name, CardEdition.EditionEntry t) {
        CardRules rules;
        if (rulesByName.containsKey(name)) {
            rules = rulesByName.get(name);
        } else if ("w_2_2_spirit".equals(name) || "w_3_3_spirit".equals(name)) { // Hotfix for Endure Token
            rules = rulesByName.get("w_x_x_spirit");
        } else {
            throw new RuntimeException("wrong token name:" + name);
        }
        return new PaperToken(rules, edition, name, t.collectorNumber(), t.artistName());
    }

    // try all editions to find token
    protected PaperToken fallbackToken(String name) {
        CardDb.CardArtPreference artPreference = StaticData.instance().getCardArtPreference();
        for (CardEdition edition : this.editions.getOrderedEditions(artPreference.latestFirst)) {
            String fullName = String.format("%s_%s", name, edition.getCode().toLowerCase());
            if (loadTokenFromSet(edition, name)) {
                return Aggregates.random(allTokenByName.get(fullName));
            }
        }
        return null;
    }

    @Override
    public PaperToken getToken(String tokenName) {
        return getToken(tokenName, CardEdition.UNKNOWN.getCode());
    }

    @Override
    public PaperToken getToken(String tokenName, String edition) {
        return getToken(tokenName, edition, -1);
    }

    @Override
    public PaperToken getToken(String tokenName, String edition, int artIndex) {
        CardEdition realEdition = editions.getEditionByCodeOrThrow(edition);
        String fullName = String.format("%s_%s", tokenName, realEdition.getCode().toLowerCase());

        // Token exists in edition, return token at artIndex or a random one.
        if (loadTokenFromSet(realEdition, tokenName)) {
            Collection<PaperToken> collection = allTokenByName.get(fullName);

            if (artIndex < 1 || artIndex > collection.size()) {
                return Aggregates.random(collection);
            }

            return Iterables.get(collection, artIndex - 1);
        }
        PaperToken fallback = this.fallbackToken(tokenName);
        if (fallback != null) {
            return fallback;
        }

        CardRules cr = rulesByName.get(tokenName);
        if (!extraTokensByName.containsKey(fullName) && cr != null) {
            try {
                PaperToken pt = new PaperToken(cr, realEdition, tokenName, "", IPaperCard.NO_ARTIST_NAME);
                extraTokensByName.put(fullName, pt);
                return pt;
            } catch(Exception e) {
                throw e;
            }
        }

        return extraTokensByName.get(fullName);
    }

    @Override
    public PaperToken getTokenFromEditions(String tokenName, CardDb.CardArtPreference fromSet) {
        return null;
    }

    @Override
    public PaperToken getTokenFromEditions(String tokenName, Date printedBefore, CardDb.CardArtPreference fromSet) {
        return null;
    }

    @Override
    public PaperToken getTokenFromEditions(String tokenName, Date printedBefore, CardDb.CardArtPreference fromSet, int artIndex) {
        return null;
    }

    @Override
    public PaperToken getFoiled(PaperToken cpi) {
        return null;
    }

    @Override
    public int getPrintCount(String cardName, String edition) {
        return 0;
    }

    @Override
    public int getMaxPrintCount(String cardName) {
        return 0;
    }

    @Override
    public int getArtCount(String cardName, String edition) {
        return 0;
    }

    @Override
    public Collection<PaperToken> getUniqueTokens() {
        return null;
    }

    @Override
    public List<PaperToken> getAllTokens() {
        return new ArrayList<>(allTokenByName.values());
    }

    @Override
    public List<PaperToken> getAllTokens(String tokenName) {
        return null;
    }

    @Override
    public List<PaperToken> getAllTokens(Predicate<PaperToken> predicate) {
        return null;
    }

    @Override
    public Predicate<? super PaperToken> wasPrintedInSets(List<String> allowedSetCodes) {
        return null;
    }

    @Override
    public Iterator<PaperToken> iterator() {
        return allTokenByName.values().iterator();
    }

    public Map<String, CardRules> getRules() { return this.rulesByName;}
}
