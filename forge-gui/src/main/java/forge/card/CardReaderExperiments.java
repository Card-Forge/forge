package forge.card;

import forge.CardStorageReader;
import forge.card.CardRules;
import forge.properties.ForgeConstants;
import forge.util.FileUtil;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CardReaderExperiments {

    //utility functions to parse all cards and perform certain actions on each card
    public static void parseAllCards(String[] args) {
        if (args.length < 2) { return; }

        int totalParsedCount = 0;
        final List<List<String>> output = new ArrayList<List<String>>();
        for (int i = 1; i < args.length; i++) {
            output.add(new ArrayList<String>());
        }
        
        final List<File> allFiles = CardStorageReader.collectCardFiles(new ArrayList<File>(), new File(ForgeConstants.CARD_DATA_DIR));
        Charset charset = Charset.forName(CardStorageReader.DEFAULT_CHARSET_NAME);
        final CardRules.Reader rulesReader = new CardRules.Reader();
        for (File file : allFiles) {
            rulesReader.reset();

            InputStreamReader isr;
            try {
                isr = new InputStreamReader(new FileInputStream(file), charset);
                List<String> lines = FileUtil.readAllLines(isr, true);
                CardRules rules = rulesReader.readCard(lines);
                
                System.out.println();
                System.out.print(rules.getName()); //print each card here in case it gets stuck in utility
                
                totalParsedCount++;
                for (int i = 1; i < args.length; i++) {
                    switch (args[i]) {
                    case "updateAbilityManaSymbols":
                        updateAbilityManaSymbols(rules, lines, file, output.get(i - 1));
                        break;
                    }
                }
            }
            catch (FileNotFoundException ex) {
            }
        }

        System.out.println();
        System.out.println();
        System.out.print("Total cards: " + totalParsedCount);

        for (int i = 1; i < args.length; i++) {
            List<String> singleOutput = output.get(i - 1);
            System.out.println();
            System.out.println();
            System.out.println(args[i] + ":");
            System.out.println();
            for (String line : singleOutput) {
                System.out.println(line);
            }
            System.out.println();
            System.out.print("Total cards: " + singleOutput.size());
        }
    }
    
    private static void updateAbilityManaSymbols(CardRules rules, List<String> lines, File file, List<String> output) {
        boolean updated = false;
        
        //ensure mana symbols appear in correct order
        String wubrg = "WUBRG";
        for (int i = 0; i < lines.size(); i++) {
            String newLine = lines.get(i);
            for (int c = 0; c < 5; c++) {
                char ch1 = wubrg.charAt(c);
                char ch2 = wubrg.charAt((c + 1) % 5);
                char ch3 = wubrg.charAt((c + 2) % 5);
                newLine = newLine.replaceAll("(^|\\W)(" + ch2 + "|" + ch3 + ")( ?)(/?)" + ch1 + "(\\W|$)", "$1" + ch1 + "$3$4$2$5");
                newLine = newLine.replaceAll("(^|\\W)\\{(" + ch2 + "|" + ch3 + ")\\}\\{" + ch1 + "\\}(\\W|$)", "$1\\{" + ch1 + "\\}\\{$2\\}$3");
            }
            if (!newLine.equals(lines.get(i))) {
                updated = true;
                lines.set(i, newLine);
                i--; //if something changed, repeat in case more than 2 mana symbols consecutively
            }
        }
        
        //convert {2W} and {PW} to {2/W} and {P/W}, and ensure not {W/2} or {W/P}
        for (int i = 0; i < lines.size(); i++) {
            String newLine = lines.get(i).replaceAll("\\{([WUBRG2P])([WUBRG])\\}", "\\{$1/$2\\}")
                    .replaceAll("\\{([WUBRG])/2\\}", "\\{2/$1\\}")
                    .replaceAll("\\{([WUBRG])/P\\}", "\\{P/$1\\}");
            if (!newLine.equals(lines.get(i))) {
                updated = true;
                lines.set(i, newLine);
            }
        }
        
        //check for oracle text appearing in ability descriptions missing "{G}" formatting
        if (updated) { //if lines updated above, ensure updated oracle text used
            rules = CardRules.fromScript(lines);
        }
        String oracleText = rules.getOracleText();
        String[] sentences = oracleText.replace(rules.getName(), "CARDNAME").split("\\.|\\\\n|\\\"|\\(|\\)");
        for (String s : sentences) {
            int idx = s.indexOf(":");
            if (idx != -1) {
                s = s.substring(idx + 1);
            }
            if (s.isEmpty()) { continue; }
            try {
                String pattern = s.replaceAll("\\{([WUBRGSXYZ]|[0-9]+)\\}", "$1[ ]\\?")
                        .replaceAll("\\{([WUBRG2P])/([WUBRG])\\}", "$1$2[ ]\\?")
                        .replaceAll("\\{C\\}", "Chaos");
                if (pattern.length() != s.length()) {
                    pattern = "Description\\$(.*)" + pattern;
                    s = "Description\\$$1" + s;
                    for (int i = 0; i < lines.size(); i++) {
                        String newLine = lines.get(i).replaceAll(pattern, s);
                        if (newLine.length() != lines.get(i).length()) {
                            updated = true;
                            lines.set(i, newLine);
                        }
                    }
                }
            }
            catch (Exception ex) {
                output.add("<Exception (" + rules.getName() + ") " + ex.getMessage() + ">");
                return;
            }
        }
        
        //convert mana costs in quoted ability descriptions
        //TODO: Uncomment when not flawed (currently doesn't work with hybrid, consecutive symbols, "Untap" in place of Q, or Pay # life/Put +1/+1 or -1/-1 costs
        /*for (int i = 0; i < lines.size(); i++) {
            String newLine = lines.get(i)
                    .replaceAll("Description\\$(.*)\\\"(.*)Tap:(.*)\\\"", "Description\\$$1\\\"$2\\{T\\}:$3\\\"")
                    .replaceAll("Description\\$(.*)\\\"(.*)Tap([ ,][A-Z0-9\\{].*):(.*)\\\"", "Description\\$$1\\\"$2\\{T\\}$3:$4\\\"")
                    .replaceAll("Description\\$(.*)\\\"(.*)([WUBRGQSTXYZ]|[0-9]+):(.*)\\\"", "Description\\$$1\\\"$2\\{$3\\}:$4\\\"")
                    .replaceAll("Description\\$(.*)\\\"(.*)([WUBRGQSTXYZ]|[0-9]+)([ ,].*):(.*)\\\"", "Description\\$$1\\\"$2\\{$3\\}$4:$5\\\"");
            if (!newLine.equals(lines.get(i))) {
                updated = true;
                lines.set(i, newLine);
            }
        }*/

        //check for other key phrases that might be missing "{G}" formatting
        String[] phrases = new String[] {
                "Add * to your mana pool",
                "CostDesc\\$ * \\|"
        };
        for (String phrase : phrases) {
            String pattern = ".*" + phrase.replace("* ", "((([WUBRGSXYZ]|[0-9]+) )+)") + ".*"; 
            Pattern p = Pattern.compile(pattern);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    StringBuilder newLineBuilder = new StringBuilder();
                    newLineBuilder.append(line.substring(0, m.start(1)));
                    for (String sym : m.group(1).split(" ")) {
                        newLineBuilder.append("{" + sym + "}");
                    }
                    newLineBuilder.append(line.substring(m.end(1) - 1)); //-1 so final space appended
                    updated = true;
                    lines.set(i, newLineBuilder.toString());
                }
            }
        }
        
        if (updated) {
            try {
                PrintWriter p = new PrintWriter(file);
                for (int i = 0; i < lines.size(); i++) {
                    if (i < lines.size() - 1) {
                        p.println(lines.get(i));
                    }
                    else {
                        p.print(lines.get(i));
                    }
                }
                p.close();
                output.add(rules.getName());
            } catch (final Exception ex) {
            }
        }
    }
}