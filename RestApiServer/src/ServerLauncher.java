public class ServerLauncher
{
    public static void main(String[] args)
    {
        System.out.println("Server stared initialization.");

        DbWrapper dbWrapper = new DbWrapper();

        try
        {
            dbWrapper.Connect();

            DbWrapper.TaskData taskData = dbWrapper.ReadData(31360854);

            System.out.println(taskData);
        }
        catch (DbWrapper.ExecutionException e)
        {
            e.printStackTrace();
        } catch (DbWrapper.NotConnectedException e) {
            e.printStackTrace();
        } catch (DbWrapper.ConnectionException e) {
            e.printStackTrace();
        }
    }
}
