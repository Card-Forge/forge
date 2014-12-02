package forge.server;

public class ServerUtil {
    public static final int PORT = 51764; //FRGE encoded on 0-25 number scale
    public static final String SERVER_DIR = "http://cardforge.org/server/";

    static {
        /*try {
            Context context = new InitialContext();
            DataSource dataSource = (DataSource) context.lookup("java:comp/env/jdbc/myDB");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }*/
    }

    /*private static class Argument {
        private final String name;
        private final String value;

        public Argument(String name0, String value0) {
            name = name0;
            value = value0;
        }
    }

    private static String post(String filename, Argument... args) throws Exception {
        //build data to send to server
        StringBuilder data = new StringBuilder();
        for (Argument arg : args) {
            if (data.length() > 0) {
                data.append("&");
            }
            data.append(URLEncoder.encode(arg.name, "UTF-8") + "=" +
                    URLEncoder.encode(arg.value, "UTF-8"));
        }

        //send data to server
        URL url = new URL("http://myphpmysqlweb.hostei.com/" + filename);
        URLConnection conn = url.openConnection(); 
        conn.setDoOutput(true); 
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream()); 
        wr.write(data.toString()); 
        wr.flush();

        //read server response
        String line;
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        while((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }*/
}
