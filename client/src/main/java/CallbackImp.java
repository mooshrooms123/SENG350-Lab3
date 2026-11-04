import java.util.Arrays;

import com.zeroc.Ice.Current;

//import Demo.ChatManagerPrx;
import BusSystem.OperationsControlCenterPrx;

//public class CallbackImp implements Demo.Callback {
public class CallbackImp implements BusSystem.BusObserver {

    //private ChatManagerPrx server;
    private OperationsControlCenterPrx server;

    //CallbackImp(ChatManagerPrx srvr) {
    CallbackImp(OperationsControlCenterPrx srvr) {
        server = srvr;
    }

    //@Override
    //public void notifyCallback(Current current) {
    //    System.out.println("notified from Server");
    //    String state[] = server.getState();
    //
    //    System.out.println("Callback executed: " + Arrays.toString(state));
    //}

    @Override
    public void notifyBus(String message, Current current) {
        System.out.println("Notification: " + message);
    }

}