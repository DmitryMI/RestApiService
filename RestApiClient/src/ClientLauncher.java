import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ClientLauncher {
    private static final String USER_AGENT = "JavaClientTester";

    private static HttpURLConnection con;


    public static void main(String[] args) throws IOException {
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(System.in));


        while (true)
        {
            System.out.println("Input G to send GUID = 50 and N to send NO-GUID...");
            char sym = (char)reader.read();
            try
            {
                if(sym == 'N')
                {
                    System.out.println("Sending NO-GUID...");

                    TestPostNoGuid();
                }
                else if(sym == 'G')
                {
                    System.out.println("Sending GUID = 50...");

                    TestPostGuid();
                }
                else
                {
                    System.out.println("Unrecognised command.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Sending test request finished");
        }
    }

    static void TestPostNoGuid() throws IOException
    {
        TestPost("\n");
    }

    // Test POST request
    static void TestPostGuid() throws IOException
    {
        JSONObject json = new JSONObject();
        json.put("guid", 50);

        String urlParameters = json.toString();

        TestPost(urlParameters);
    }

    static void TestPost(String data) throws IOException {
        String url = "http://localhost/task";

        byte[] postData = data.getBytes(StandardCharsets.UTF_8);

        try {

            URL myurl = new URL(url);
            con = (HttpURLConnection) myurl.openConnection();

            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            try (DataOutputStream wr = new DataOutputStream(con.getOutputStream()))
            {
                wr.write(postData);
            }

            StringBuilder content;

            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream())))
            {

                String line;
                content = new StringBuilder();

                while ((line = in.readLine()) != null) {
                    content.append(line);
                    content.append(System.lineSeparator());
                }
            }

            System.out.println(content.toString());

        } finally {

            con.disconnect();
        }
    }


}
