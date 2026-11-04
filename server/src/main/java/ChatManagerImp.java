//import java.util.ArrayList;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
//import java.util.concurrent.Semaphore;

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
    private static final String TEAM_FOLDER = "team#";

    // All the buses that subscribed. This list type is safe to use from many threads.
    private List<BusObserverPrx> subscribers = new CopyOnWriteArrayList<>();

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

    // A bus calls this to get added to the list
    @Override
    public void subscribe(BusObserverPrx observer, Current current) {
        subscribers.add(observer);
        System.out.println("Bus subscribed. Total subscribers: " + subscribers.size());
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

            // GENERALINTEREST images also notify every bus.
            // NORMAL images only get saved. This lab doesn't use CRITICAL.
            if (busImage.tag.equals("GENERALINTEREST")) {
                notifyAllBuses("GENERALINTEREST from " + busImage.busId);
            }
        } catch (Exception e) {
            System.out.println("Could not process image from bus " + busImage.busId);
            e.printStackTrace();
        }
    }

    // Saves the image to ./team#/<busId>/
    private void saveImage(BusImage busImage) throws Exception {
        File folder = new File(TEAM_FOLDER + "/" + busImage.busId);

        // Make the folder if it doesn't exist yet
        folder.mkdirs();

        File imageFile = new File(folder, "image_" + nextImageNumber() + ".jpg");
        Files.write(imageFile.toPath(), busImage.image);
    }

    // Sends a notification to every subscribed bus
    private void notifyAllBuses(String message) {
        for (BusObserverPrx subscriber : subscribers) {
            try {
                subscriber.notifyBus(message);
                countNotification();
            } catch (Exception e) {
                // Remove a bus we can't reach, so the other buses still get notified
                System.out.println("Could not reach a bus, removing it");
                subscribers.remove(subscriber);
            }
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