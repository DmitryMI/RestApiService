import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class DbWrapper
{
    private final String MSSQL_LOGIN = "JavaServer";
    private final String MSSQL_DNAME = "JavaServerDb";
    private final String MSSQL_PASWD = "25565";
    private final String MSSQL_SNAME = "DMITRYBIGPC";

    private String serverName, dbName, userName, password;
    private Random random = new Random();

    private Connection connection;

    // Public sub-elements
    public enum Status
    {
        CREATED, RUNNING, FINISHED
    }

    public static class ConnectionException extends Exception
    {

    }

    public static class NotConnectedException extends Exception
    {
        public NotConnectedException()
        {
            super("Object is not connected to a database. Call Connect() before using any other methods");
        }
    }

    public static class ExecutionException extends Exception
    {
        public ExecutionException(String sqlInfo)
        {
            super(sqlInfo);
        }
    }

    public static class TaskData
    {
        private int guid;
        private Date timeStamp;
        private Status status;

        public TaskData(int guid, Date timeStamp, Status status)
        {
            this.guid = guid;
            this.timeStamp = timeStamp;
            this.status = status;
        }

        public int getGuid()
        {
            return guid;
        }

        public void setGuid(int guid)
        {
            this.guid = guid;
        }

        public Date getTimeStamp() {
            return timeStamp;
        }

        public void setTimeStamp(Date timeStamp) {
            this.timeStamp = timeStamp;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        @Override
        public String toString()
        {
            return String.format("%d, %s, %s", guid, timeStamp);
        }
    }

    public DbWrapper()
    {
        serverName = MSSQL_SNAME;
        dbName = MSSQL_DNAME;
        userName = MSSQL_LOGIN;
        password = MSSQL_PASWD;
    }

    public DbWrapper(String serverName, String dbName, String uName, String pwd)
    {
        this.serverName = serverName;
        this.dbName = dbName;
        this.userName = uName;
        this.password = pwd;
    }

    public void Connect() throws ExecutionException, ConnectionException {
        try
        {
            String instanceName = serverName;
            String connectionUrl = "jdbc:sqlserver://%1$s;databaseName=%2$s;user=%3$s;password=%4$s;";
            String connectionString = String.format(connectionUrl, instanceName, dbName, userName, password);
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

            connection = DriverManager.getConnection(connectionString);
        }
        catch (ClassNotFoundException ex)
        {
            ex.printStackTrace();
            throw new ConnectionException();
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
            throw new ExecutionException(ex.getMessage());
        }
    }

    private ArrayList<Integer> LoadAllGuids() throws SQLException
    {
        String query = "use JavaServerDb select taskGuid from Tasks";

        Statement statement = connection.createStatement();
        ResultSet qResult = statement.executeQuery(query);

        ArrayList<Integer> list = new ArrayList<>();

        int columnId = qResult.findColumn("taskGuid");

        while(qResult.next())
        {
            list.add(qResult.getInt(columnId));
        }

        return list;
    }

    private String StatusToString(Status st)
    {
        return st.name();
    }

    private Status StatusFromString(String str)
    {
        return Status.valueOf(str);
    }

    private int GenerateUniqueGuid() throws SQLException
    {
        ArrayList<Integer> allGuids = LoadAllGuids();

        int guid;

        do
        {
            guid = random.nextInt() & 0xfffffff;
        }
        while(allGuids.contains(guid));

        return guid;
    }


    public boolean CheckGuid(int guid) throws ExecutionException, NotConnectedException {
        if(connection == null)
        {
            throw new NotConnectedException();
        }

        String query = "use JavaServerDb select COUNT(*) as GuidCounts from Tasks where Tasks.taskGuid = %d";
        query = String.format(query, guid);
        try {
            Statement statement = connection.createStatement();
            ResultSet qResult = statement.executeQuery(query);
            qResult.next();
            int count = qResult.getInt("GuidCounts");
            return count != 0;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            throw new ExecutionException(e.toString());
        }
    }

    public void WriteData(int guid, Status status) throws NotConnectedException, ExecutionException {
        if(connection == null)
        {
            throw new NotConnectedException();
        }

        try
        {
            WriteForGuid(guid, status);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            throw new ExecutionException(e.getMessage());
        }
    }

    public int WriteData(Status status) throws NotConnectedException, ExecutionException {
        if(connection == null)
        {
            throw new NotConnectedException();
        }

        try
        {
            int guid = GenerateUniqueGuid();

            WriteForGuid(guid, status);

            return guid;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            throw new ExecutionException(e.getMessage());
        }
    }

    private void WriteForGuid(int guid, Status status) throws SQLException, NotConnectedException, ExecutionException
    {
        String query;
        if(!CheckGuid(guid))
        {
            query = String.format("insert into Tasks (taskGuid, dtStamp, taskStatus) values \n" +
                            "(\n" +
                            "\t%d, CURRENT_TIMESTAMP, '%s'\n" +
                            ")",
                    guid, StatusToString(status));
        }
        else
        {
            query = String.format("use JavaServerDb update Tasks SET taskStatus = '%s' where taskGuid = %d",
                    StatusToString(status), guid);
        }

        Statement statement = connection.createStatement();
        statement.execute(query);
    }

    public TaskData ReadData(int guid) throws NotConnectedException, ExecutionException
    {
        if(connection == null)
        {
            throw new NotConnectedException();
        }

        String query = "use JavaServerDb select dtStamp, taskStatus from Tasks where taskGuid = %d";
        query = String.format(query, guid);

        try
        {
            Statement statement = connection.createStatement();
            ResultSet qResult = statement.executeQuery(query);

            int dtStampId = qResult.findColumn("dtStamp");
            int taskStatusId = qResult.findColumn("taskStatus");
            TaskData result;
            if(qResult.next())
            {
                Date dtStamp = qResult.getDate(dtStampId);
                String statusString = qResult.getString(taskStatusId);
                Status taskStatus = StatusFromString(statusString);
                result = new TaskData(guid, dtStamp, taskStatus);
            }
            else
            {
                result = null;
            }

            return result;
        }
        catch (SQLException ex)
        {
            throw new ExecutionException(ex.getMessage());
        }
    }
}
