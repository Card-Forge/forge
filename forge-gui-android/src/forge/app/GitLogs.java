package forge.app;

import forge.util.TextUtil;
import org.apache.commons.text.StringEscapeUtils;

import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class GitLogs {
    public String getLatest(Date buildDateOriginal, Date maxDate) {
        String message = "";
        try {
            URL url = new URL("https://github.com/Card-Forge/forge/commits/master.atom");
            InputStream inputStream = url.openStream();
            List<AtomReader.Entry> entries = new AtomReader().parse(inputStream);
            StringBuilder logs = new StringBuilder();
            SimpleDateFormat simpleDate = TextUtil.getSimpleDate();
            SimpleDateFormat atomDate = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
            int c = 0;
            for (AtomReader.Entry entry : entries) {
                if (entry.title == null)
                    continue;
                String title = TextUtil.stripNonValidXMLCharacters(entry.title);
                if (title.contains("Merge"))
                    continue;
                if (entry.updated == null)
                    continue;
                Date feedDate = atomDate.parse(entry.updated);
                if (buildDateOriginal != null && feedDate.before(buildDateOriginal))
                    continue;
                if (maxDate != null && feedDate.after(maxDate))
                    continue;
                logs.append(simpleDate.format(feedDate)).append(" | ").append(StringEscapeUtils.unescapeXml(title).replace("\n", "").replace("        ", "")).append("\n\n");
                if (c >= 15)
                    break;
                c++;
            }
            if (logs.length() > 0)
                message += ("\n\nLatest Changes:\n\n" + logs);
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return message;
    }
}
