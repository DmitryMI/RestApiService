import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;

public class NetWrapper
{
    public final int PORT = 80;
    private final int BUFFER_SIZE = 1024;

    public interface MethodGetCallback
    {
        void OnRequestReceived(String url, Hashtable params, ResponseHandler responseHandler);
    }

    public interface MethodPostCallback
    {
        void OnRequestReceived(String url, String body, ResponseHandler responseHandler);
    }

    public class ResponseHandler
    {
        private Socket openedSocket;

        public ResponseHandler(Socket socket)
        {
            openedSocket = socket;
        }

        public void MakeResponse(String data) throws Throwable
        {
            if(data == null)
                data = "";
            writeResponse(200, data);
        }

        public void MakeResponse(int code, String data) throws Throwable
        {
            if(data == null)
                data = "";
            writeResponse(code, data);
        }

        public void Close() throws IOException
        {
            openedSocket.close();
        }

        private void writeResponse(int code, String s) throws Throwable
        {
            String response = "HTTP/1.1 %d %s\r\n" +
                    "Server: YarServer/2009-09-09\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Content-Length: " + s.length() + "\r\n" +
                    "Connection: close\r\n\r\n";
            response = String.format(response, code, HttpParser.getHttpReply(code));
            String result = response + s;
            OutputStream os = openedSocket.getOutputStream();
            os.write(result.getBytes());
            os.flush();
        }
    }



    private MethodGetCallback getCallback;
    private MethodPostCallback postCallback;
    private Server serverThreadObj;

    public NetWrapper(MethodGetCallback getCallback, MethodPostCallback postCallback)
    {
        this.getCallback = getCallback;
        this.postCallback = postCallback;
    }

    public void StartServer()
    {
        serverThreadObj = new Server();
        new Thread(serverThreadObj).start();
    }

    public void StopServer()
    {
        if(serverThreadObj != null)
            serverThreadObj.stopServer();
    }

    class Server implements Runnable
    {
        ServerSocket serverSocket;
        private boolean shouldStop;

        public void stopServer()
        {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            shouldStop = true;
        }

        @Override
        public void run()
        {
            try
            {
                serverSocket = new ServerSocket(PORT);
                while (!shouldStop)
                {
                    try
                    {
                        Socket s = serverSocket.accept();
                        System.out.println("Client acceped");
                        new Thread(new SocketProcessor(s)).start();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }

            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

        }
    }

    class SocketProcessor implements Runnable
    {
        private Socket s;
        private InputStream is;

        private SocketProcessor(Socket s) throws IOException
        {
            this.s = s;
            this.is = s.getInputStream();
        }

        private void readInput() throws Throwable
        {
            System.out.println("Reading data... Availible: " + is.available());

            byte[] data = new byte[BUFFER_SIZE];
            int i = 0;
            int availible = is.available();
            while(availible > 0)
            {
                data[i] = (byte)is.read();
                availible = is.available();
                i++;
            }

            String dataString = new String(data);
            System.out.println("Data:\n" + dataString.trim());
            System.out.println("Length: " + dataString.length());

            HttpParser parser = new HttpParser(dataString);
            parser.parseRequest();
            String method = parser.getMethod();
            Hashtable params = parser.getParams();
            String url = parser.getRequestURL();

            ResponseHandler responseHandler = new ResponseHandler(s);

            if(method.equals("GET"))
            {
                System.out.println("Launching callback for GET method");

                getCallback.OnRequestReceived(url, params, responseHandler);
            }
            else if(method.equals("POST"))
            {
                System.out.println("Reading request body");

                // Reading request body
                int curPos = parser.getCurPos();

                StringBuilder bodyBuilder = new StringBuilder();
                while(curPos < dataString.length())
                {
                    bodyBuilder.append(dataString.charAt(curPos));
                    curPos++;
                }

                String body = bodyBuilder.toString();
                body = body.trim();

                System.out.println("Body:\n" + body);

                postCallback.OnRequestReceived(url, body, responseHandler);
            }
            else
            {
                responseHandler.MakeResponse(404, null);
                responseHandler.Close();
            }
        }

        @Override
        public void run()
        {
            try
            {
                readInput();
            }
            catch (Throwable t)
            {

            }
        }
    }

}
