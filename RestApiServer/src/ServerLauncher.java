/* Summary

Program uses MS-SQL Server for storing task data.
The database and tables must be created before the launch of the program.


*/


import org.json.*;
import java.util.Hashtable;

public class ServerLauncher
{
    private static final long RUNNING_TIME = 120000; // 2 minutes


    public static void main(String[] args)
    {
        System.out.println("Server stared initialization.");


        try
        {
            new ServerLauncher().LaunchListener();
        }
        catch (DbWrapper.ConnectionException e)
        {
            e.printStackTrace();
        }
        catch (DbWrapper.ExecutionException e)
        {
            e.printStackTrace();
        }

    }

    class ExpirationHandler implements MultiTimer.ExpirationCallback
    {
        @Override
        public void Expired(int guid) {
            try {
                database.WriteData(guid, DbWrapper.Status.FINISHED);
                System.out.println(String.format("Task %d is not at 'finished' status", guid));
            } catch (DbWrapper.NotConnectedException e) {
                e.printStackTrace();
            } catch (DbWrapper.ExecutionException e) {
                e.printStackTrace();
            }
        }
    }


    MultiTimer timer;

    private DbWrapper database = new DbWrapper();

    private void LaunchListener() throws DbWrapper.ConnectionException, DbWrapper.ExecutionException {
        database.Connect();

        timer = new MultiTimer(new ExpirationHandler());
        timer.LaunchTimer();

        NetWrapper wrapper = new NetWrapper(new GetRequestHandler(), new PostRequestHandler());
        wrapper.StartServer();
    }

    class WorkResult
    {
        private int responseCode;
        private String responseText;

        WorkResult(int responseCode, String responseText)
        {
            this.responseCode = responseCode;
            this.responseText = responseText;
        }

        public String getResponseText() {
            return responseText;
        }

        public int getResponseCode() {
            return responseCode;
        }
    }

    private WorkResult HandleGetTask(int guid)
    {
        WorkResult workResult = null;
        try
        {
            if(database.CheckGuid(guid))
            {
                DbWrapper.TaskData data = database.ReadData(guid);

                JSONObject root = new JSONObject();
                root.put("status", data.getStatus().name());
                root.put("timestamp", data.getTimeStamp());

                workResult = new WorkResult(200, root.toString());
            }
            else
            {
                workResult = new WorkResult(404, "404");
            }
        }
        catch (DbWrapper.ExecutionException e)
        {
            e.printStackTrace();
        } catch (DbWrapper.NotConnectedException e)
        {
            e.printStackTrace();
        }

        return workResult;
    }

    private WorkResult HandlePostTask(String requestText)
    {
        WorkResult workResult = null;
        try
        {
            JSONObject root = new JSONObject(requestText);

            if(root.has("guid"))
            {
                int guid = root.getInt("guid");
                database.WriteData(guid, DbWrapper.Status.RUNNING);

                workResult = new WorkResult(202, null);

                timer.RegisterTimer(guid, RUNNING_TIME);

                System.out.println("Task %d was just set to running state.");
            }
            else
            {
                int guid = database.WriteData(DbWrapper.Status.CREATED);
                JSONObject responseJson = new JSONObject();
                responseJson.put("guid", guid);
                workResult = new WorkResult(202, responseJson.toString());

                System.out.println("Task %d was just created.");
            }
        }
        catch (DbWrapper.ExecutionException e)
        {
            e.printStackTrace();
        } catch (DbWrapper.NotConnectedException e)
        {
            e.printStackTrace();
        }

        return workResult;
    }


    class GetRequestHandler implements NetWrapper.MethodGetCallback
    {

        @Override
        public void OnRequestReceived(String url, Hashtable params, NetWrapper.ResponseHandler responseHandler)
        {


            try
            {
                if(!url.equals("/task"))
                {
                    responseHandler.MakeResponse(404, "404");
                    responseHandler.Close();
                    return;
                }

                try
                {
                    String guidStr = (String)params.get("guid");
                    Integer guid = Integer.parseInt(guidStr);

                    WorkResult result = HandleGetTask(guid);
                    responseHandler.MakeResponse(result.getResponseCode(), result.getResponseText());

                    responseHandler.Close();
                }
                catch (NumberFormatException ex)
                {
                    responseHandler.MakeResponse(400, "400");
                }
                finally
                {
                    responseHandler.Close();
                }
            }
            catch (Throwable t)
            {

            }
        }
    }

    class PostRequestHandler implements NetWrapper.MethodPostCallback
    {

        @Override
        public void OnRequestReceived(String url, String body, NetWrapper.ResponseHandler responseHandler)
        {
            try
            {
                try
                {
                    WorkResult result = HandlePostTask(body);
                    responseHandler.MakeResponse(result.getResponseCode(), result.getResponseText());

                    responseHandler.Close();
                }
                catch (NumberFormatException ex)
                {
                    responseHandler.MakeResponse(400, null);
                }
                finally
                {
                    responseHandler.Close();
                }
            }
            catch (Throwable t)
            {

            }
        }
    }
}
