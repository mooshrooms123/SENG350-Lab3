import java.io.File;
import java.net.Inet4Address;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;

//import Demo.CallbackPrx;
//import Demo.ChatManagerPrx;

//import the java classes from ice file
import BusSystem.BusImage;
import BusSystem.BusObserverPrx;
import BusSystem.OperationsControlCenterPrx;



public class Client {
    public static void main(String[] args) {

        // command line arguments
        String directory = args[0];
        int busId = Integer.parseInt(args[0]);
        int maxImages = Integer.parseInt(args[1]);
        int frequency = Integer.parseInt(args[2]);
        int percent = Integer.parseInt(args[3]);

        try (com.zeroc.Ice.Communicator communicator = com.zeroc.Ice.Util.initialize(args, "client.cfg")) {

            OperationsControlCenterPrx chatManagerPrx = OperationsControlCenterPrx
                    .checkedCast(communicator.propertyToProxy("ChatManager.Proxy"));

            try {

                CallbackImp callbackImp = new CallbackImp(chatManagerPrx);
                ObjectAdapter adapter = communicator.createObjectAdapter("Callback");
                ObjectPrx objectPrx = adapter.add(callbackImp, Util.stringToIdentity("Callback"));
                adapter.activate();

                BusObserverPrx prx = BusObserverPrx.uncheckedCast(objectPrx);
                chatManagerPrx.subscribe(prx);

                //String hostname = Inet4Address.getLocalHost().getHostName();
                //System.out.println("hostname: " + hostname);

                //chatManagerPrx.sendMessage(hostname + ": Hello World");

                // get all the images in the bus's folder
                File[] images = new File(directory).listFiles();

                // 0 means send all the images
                int numToSend = maxImages;
                if (maxImages == 0 || maxImages > images.length) {
                    numToSend = images.length;
                }

                DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                for (int i = 0; i < numToSend; i++) {
                    byte[] imageBytes = Files.readAllBytes(images[i].toPath());

                    // pick the tag based on the percentage
                    String tag = "NORMAL";
                    if (i % 100 < percent) {
                        tag = "GENERALINTEREST";
                    }

                    String timestamp = LocalDateTime.now().format(format);

                    BusImage busImage = new BusImage(busId, tag, timestamp, imageBytes);
                    chatManagerPrx.sendBusImage(busImage);

                    // wait before sending the next image
                    Thread.sleep(frequency);
                }

                System.out.println("Bus " + busId + " done sending");

                // stay running to get notifications
                communicator.waitForShutdown();

            } catch (Exception e) {
                // saveMessage(dest,)
                e.printStackTrace();
            }
        }

    }
}