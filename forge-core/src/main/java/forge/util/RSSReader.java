package forge.util;

import com.apptasticsoftware.rssreader.Item;
import com.apptasticsoftware.rssreader.RssReader;
import org.apache.commons.text.StringEscapeUtils;

import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

public class RSSReader {
    public static String getCommitLog(String commitsAtom, Date buildDateOriginal, Date maxDate) {
        String message = "";
        SimpleDateFormat simpleDate = TextUtil.getSimpleDate();
        try {
            RssReader reader = new RssReader();
            URL url = new URL(commitsAtom);
            InputStream inputStream = url.openStream();
            List<Item> items = reader.read(inputStream).toList();
            StringBuilder logs = new StringBuilder();
            int c = 0;
            for (Item i : items) {
                if (i.getTitle().isEmpty())
                    continue;
                String title = TextUtil.stripNonValidXMLCharacters(i.getTitle().get());
                if (title.contains("Merge"))
                    continue;
                ZonedDateTime zonedDateTime = i.getPubDateZonedDateTime().isPresent() ? i.getPubDateZonedDateTime().get() : null;
                if (zonedDateTime == null)
                    continue;
                Date feedDate = Date.from(zonedDateTime.toInstant());
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
    public static String getLatestReleaseTag(String releaseAtom) {
        String tag = "";
        try {
            RssReader reader = new RssReader();
            URL url = new URL(releaseAtom);
            InputStream inputStream = url.openStream();
            List<Item> items = reader.read(inputStream).toList();
            for (Item i : items) {
                if (i.getLink().isPresent()) {
                    try {
                        String val = i.getLink().get();
                        tag = val.substring(val.lastIndexOf("forge"));
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tag;
    }
}
