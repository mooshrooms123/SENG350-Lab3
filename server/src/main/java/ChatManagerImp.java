//import java.util.ArrayList;
import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.Semaphore;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.zeroc.Ice.Current;

//import Demo.CallbackPrx;
import BusSystem.BusImage;
import BusSystem.BusObserverPrx;

//public class ChatManagerImp implements Demo.ChatManager {
public class ChatManagerImp implements BusSystem.OperationsControlCenter {

    //private List<String> messages;
    // Semaphore mSemaphore = new Semaphore(1);
    //private CallbackPrx callbackPrx;

    // CHANGE THIS to your team number, like "team7"
    private static final String TEAM_FOLDER = "group11";

    // All the buses that subscribed, stored as busId -> that bus's observer (callback) proxy.
    // In Lab 3 this was just a list, but a list can't tell us WHICH observer is which bus.
    // With a map we can look up the exact bus that sent a CRITICAL image and only notify that one.
    // ConcurrentHashMap is used because with a threadpool bigger than 1, many threads
    // are subscribing / sending images at the same time, and a normal HashMap isn't safe for that.
    private Map<Integer, BusObserverPrx> subscribers = new ConcurrentHashMap<>();

    // Used to give every saved image a different file name
    private int imageNumber = 0;

    // Numbers used to measure message throughput
    private int messageCount = 0;
    private long firstMessageTime = 0;
    private long lastMessageTime = 0;

    // Numbers used to measure notification throughput
    private int notificationCount = 0;
    private long firstNotificationTime = 0;
    private long lastNotificationTime = 0;

    //ChatManagerImp() {
    //    messages = new ArrayList<>();
    //}

    //@Override
    //public void subscribe(CallbackPrx callback, Current current) {
    //    System.out.println("subscribed");
    //    this.callbackPrx = callback;
    //}

    // A bus calls this to get added to the subscribers (this is the "attach" step of the Observer pattern)
    // It now also gives us its busId so we know whose observer this is.
    // If the same bus subscribes again (like it restarted), put() just replaces its old observer.
    @Override
    public void subscribe(int busId, BusObserverPrx observer, Current current) {
        subscribers.put(busId, observer);
        System.out.println("Bus " + busId + " subscribed. Total subscribers: " + subscribers.size());
    }

    //@Override
    //public String[] getState(Current current) {
    //    System.out.println("GetState");
    //    String[] state = new String[messages.size()];
    //
    //    for (int i = 0; i < state.length; i++) {
    //        state[i] = messages.get(i);
    //    }
    //    return state;
    //}

    //@Override
    //public void sendMessage(String msg, Current current) {
    //
    //    new Thread(() -> {
    //        System.out.println("new Message: " + msg);
    //        messages.add(msg);
    //        try {
    //
    //            Thread.sleep(2000);
    //        } catch (Exception e) {
    //            // TODO: handle exception
    //        }
    //        callbackPrx.notifyCallback();
    //    }).start();
    //}

    // A bus calls this to send one image
    @Override
    public void sendBusImage(BusImage busImage, Current current) {
        try {
            // Every image gets saved, no matter what its tag is
            saveImage(busImage);
            countMessage();

            // Now do something different depending on the tag:
            //   GENERALINTEREST -> tell EVERY subscribed bus  ("GENERALINTEREST from BUSID")
            //   CRITICAL        -> tell ONLY the bus that sent it ("ACTION for BUSID")
            //   NORMAL          -> nothing else, it's already saved above for audit logging
            if (busImage.tag.equals("GENERALINTEREST")) {
                notifyAllBuses("GENERALINTEREST from " + busImage.busId);
            } else if (busImage.tag.equals("CRITICAL")) {
                notifyOriginatingBus(busImage.busId, "ACTION for " + busImage.busId);
            }
        } catch (Exception e) {
            System.out.println("Could not process image from bus " + busImage.busId);
            e.printStackTrace();
        }
    }

    // Saves the image to ./group11/<busId>/texttag-YYYY-MM-DD-H24:MI:SS_<n>.jpg
    // e.g. ./group11/711/CRITICAL-2026-11-15-18:45:02_42.jpg
    private void saveImage(BusImage busImage) throws Exception {
        File folder = new File(TEAM_FOLDER + "/" + busImage.busId);

        // Make the folder if it doesn't exist yet
        folder.mkdirs();

        LocalDateTime now = LocalDateTime.now();

        // Define the exact format we are required from the lab manual (HH is 24-hour clock, mm is minutes, ss is 2-digit seconds)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss");
        String formattedTime = now.format(formatter);

        // The file name starts with the tag (the "texttag" part of the spec), then the date/time.
        // PROBLEM: we save 200+ images a second, so lots of images land in the same second
        // and would have the exact same name and overwrite each other.
        // FIX (our choice for handling the format): add _<n> before .jpg, where n is a counter
        // that goes up for every image, so every file name is unique.
        // Note: the colons in the time are fine on Linux (the lab machines) but Windows doesn't allow them.
        File imageFile = new File(folder, busImage.tag + "-" + formattedTime + "_" + nextImageNumber() + ".jpg");
        Files.write(imageFile.toPath(), busImage.image);
    }

    // Sends a notification to every subscribed bus (used for GENERALINTEREST)
    // This is the "notify all observers" part of the Observer pattern
    private void notifyAllBuses(String message) {
        // loop over every busId -> observer pair in the map
        for (Map.Entry<Integer, BusObserverPrx> entry : subscribers.entrySet()) {
            try {
                entry.getValue().notifyBus(message);
                countNotification();
            } catch (Exception e) {
                // Remove a bus we can't reach, so the other buses still get notified
                System.out.println("Could not reach bus " + entry.getKey() + ", removing it");
                subscribers.remove(entry.getKey());
            }
        }
    }

    // Sends a notification to ONLY the bus that sent the image (used for CRITICAL)
    // Instead of looping over everyone, we look up the one observer that matches the busId.
    private void notifyOriginatingBus(int busId, String message) {
        BusObserverPrx observer = subscribers.get(busId);

        // If that bus never subscribed (or got removed), there's no one to tell.
        // Just print it instead of crashing, the image is still saved either way.
        if (observer == null) {
            System.out.println("CRITICAL from bus " + busId + " but that bus isn't subscribed, no alert sent");
            return;
        }

        try {
            observer.notifyBus(message);
            countNotification();
        } catch (Exception e) {
            // Same idea as the broadcast, if we can't reach the bus, take it out of the map
            System.out.println("Could not reach bus " + busId + ", removing it");
            subscribers.remove(busId);
        }
    }

    // synchronized means only one thread can run this at a time,
    // so two threads can't get the same number
    private synchronized int nextImageNumber() {
        imageNumber++;
        return imageNumber;
    }

    // Counts one saved message and prints the rate every 100 messages
    private synchronized void countMessage() {
        long now = System.currentTimeMillis();
        if (messageCount == 0) {
            firstMessageTime = now;
        }
        messageCount++;
        lastMessageTime = now;

        if (messageCount % 100 == 0) {
            double seconds = (lastMessageTime - firstMessageTime) / 1000.0;
            System.out.println("Messages: " + messageCount + ", Seconds: " + seconds
                    + ", Messages per second: " + (messageCount / seconds));
        }
    }

    // Counts one sent notification and prints the rate every 100 notifications
    private synchronized void countNotification() {
        long now = System.currentTimeMillis();
        if (notificationCount == 0) {
            firstNotificationTime = now;
        }
        notificationCount++;
        lastNotificationTime = now;

        if (notificationCount % 100 == 0) {
            double seconds = (lastNotificationTime - firstNotificationTime) / 1000.0;
            System.out.println("Notifications: " + notificationCount + ", Seconds: " + seconds
                    + ", Notifications per second: " + (notificationCount / seconds));
        }
    }

}