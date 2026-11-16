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
                ObjectAdapter adapter = communicator.createObjectAdapter("Callback"); //makes adapter start listening to port 6970
                ObjectPrx objectPrx = adapter.add(callbackImp, Util.stringToIdentity("Callback"));
                adapter.activate();

                BusObserverPrx prx = BusObserverPrx.uncheckedCast(objectPrx);

                // subscribe to the control center, now we send our busId along with our observer
                // so the server can match CRITICAL alerts back to this exact bus
                chatManagerPrx.subscribe(busId, prx);

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

                // ---- latency tracking for Task 3 ----
                // sendBusImage is a normal (twoway) ICE call, so it blocks until the server is done with it.
                // For a CRITICAL image the server saves it AND calls notifyBus() back on us before it returns,
                // so timing the sendBusImage call gives us the full round trip:
                //   send image -> server saves it -> server sends "ACTION for BUSID" -> we get it -> call returns
                long totalLatencyNanos = 0;   // add up every image's latency so we can get the average at the end
                long maxLatencyNanos = 0;     // the slowest single image, handy to see spikes
                long minLatencyNanos = Long.MAX_VALUE; // the fastest single image

                for (int i = 0; i < numToSend; i++) {
                    byte[] imageBytes = Files.readAllBytes(images[i].toPath());

                    // pick the tag based on the percentage (last command line arg = % of images that are CRITICAL)
                    // this math spreads the CRITICAL ones out evenly instead of bunching them at the start,
                    // e.g. 50 means every 2nd image is CRITICAL, 100 means every image is CRITICAL
                    String tag = "NORMAL";
                    if ((i + 1) * percent / 100 > i * percent / 100) {
                        tag = "CRITICAL";
                    }

                    String timestamp = LocalDateTime.now().format(format);

                    BusImage busImage = new BusImage(busId, tag, timestamp, imageBytes);

                    // start the timer right before sending (nanoTime is for measuring time gaps, not the clock time)
                    long start = System.nanoTime();
                    chatManagerPrx.sendBusImage(busImage);
                    long latency = System.nanoTime() - start; // how long this one image took, round trip

                    // keep our running totals up to date
                    totalLatencyNanos += latency;
                    if (latency > maxLatencyNanos) {
                        maxLatencyNanos = latency;
                    }
                    if (latency < minLatencyNanos) {
                        minLatencyNanos = latency;
                    }

                    // wait before sending the next image (use 0 for Task 3 so the server is the bottleneck)
                    Thread.sleep(frequency);
                }

                System.out.println("Bus " + busId + " done sending");

                // print the latency results, divide by 1,000,000 to turn nanoseconds into milliseconds
                if (numToSend > 0) {
                    double avgMs = (totalLatencyNanos / (double) numToSend) / 1_000_000.0;
                    System.out.println("Bus " + busId + " latency over " + numToSend + " images:"
                            + " average = " + avgMs + " ms"
                            + ", min = " + (minLatencyNanos / 1_000_000.0) + " ms"
                            + ", max = " + (maxLatencyNanos / 1_000_000.0) + " ms");
                }

                // stay running to get notifications
                communicator.waitForShutdown();

            } catch (Exception e) {
                // saveMessage(dest,)
                e.printStackTrace();
            }
        }

    }
}