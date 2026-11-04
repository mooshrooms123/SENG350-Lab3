import java.util.Arrays;

import com.zeroc.Ice.Current;

import Demo.ChatManagerPrx;

//import the java classes from ice file
import BusSystem.BusImage;
import BusSystem.BusObserverPrx;
import BusSystem.OperationsControlCenterPrx;

public class CallbackImp implements BusSystem.BusObserver {

    private ChatManagerPrx server;

    CallbackImp(ChatManagerPrx srvr) {
        server = srvr;
    }

    @Override
    public void notifyCallback(Current current) {
        System.out.println("notified from Server");
        String state[] = server.getState();

        System.out.println("Callback executed: " + Arrays.toString(state));
    }

}
