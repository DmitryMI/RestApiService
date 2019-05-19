import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MultiTimer
{
    public final long TIMER_PERIOD_MILLISECOND = 1000;

    public MultiTimer(ExpirationCallback callback)
    {
        this.callback = callback;
        idList = new ArrayList<>();
        timeList = new ArrayList<>();
    }

    public interface ExpirationCallback
    {
         void Expired(int id);
    }

    private Timer timer;

    private ArrayList<Integer> idList;
    private ArrayList<Long> timeList;

    private ExpirationCallback callback;

    public void RegisterTimer(int id, long timeout)
    {
        int index = idList.indexOf(id);
        if(index == -1)
        {
            idList.add(id);
            timeList.add(timeout);
            System.out.println("Timer with id " + id + " was registered.");
        }
        else
        {
            SetTimeout(index, timeout);
        }
    }

    private void SetTimeout(int index, long timeout)
    {
        timeList.set(index, timeout);
        System.out.println(String.format("Timer for Id %d was updated.", idList.get(index)));
    }

    private long GetTimeout(int index)
    {
        return timeList.get(index);
    }

    private void DecrementTimeout(int index, long decrement)
    {
        long timeout = timeList.get(index) - decrement;
        timeList.set(index, timeout);
        System.out.println(String.format("Timer for Id %d was decremented.", idList.get(index)));
    }

    private void FinishTimeout(int index)
    {
        int id = idList.get(index);

        idList.remove(index);
        timeList.remove(index);

        System.out.println(String.format("Timer for Id %d expired.", id));

        if(callback != null)
        {
            callback.Expired(id);
        }
    }

    public void LaunchTimer()
    {
        if(timer != null)
        {
            timer.cancel();
        }

        timer = new Timer();
        timer.schedule(new TimerElapsedHandler(), TIMER_PERIOD_MILLISECOND, TIMER_PERIOD_MILLISECOND);
    }


    class TimerElapsedHandler extends TimerTask
    {
        @Override
        public void run()
        {
            //System.out.println("Timer tick!");

            for(int i = 0; i < idList.size(); i++)
            {
                DecrementTimeout(i, TIMER_PERIOD_MILLISECOND);
                if(GetTimeout(i) <= 0)
                {
                    FinishTimeout(i);
                }
            }
        }
    }
}
