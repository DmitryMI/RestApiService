public class NetWrapper
{
    private Callback postCallback, getCallback;

    public NetWrapper(Callback postCallback, Callback getCallback)
    {
        this.postCallback = postCallback;
        this.getCallback = getCallback;
    }

    public static class NetData
    {

    }

    public static class SenderInfo
    {

    }

    public interface Callback
    {
        void OnDataReceived(NetData data, SenderInfo sender);
    }

    public void SendPackage(NetData data)
    {
        
    }
}
