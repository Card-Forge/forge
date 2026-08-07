package forge.scryfall.uuidmap;

public record CardRecord(
        String setCode,
        String collectorNumber,
        String lang,
        String frontUuid,
        String backUuid
) {}
