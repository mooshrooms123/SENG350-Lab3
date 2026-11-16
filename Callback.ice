/* module Demo
{
    sequence<string> StringSeq;

    

    interface Callback{
        void notifyCallback();
    }

    interface ChatManager{

        void subscribe(Callback* callback);

        StringSeq getState();

        void sendMessage(string msg);
    }


}
*/
//new version of the Callback.ice design

module BusSystem
{
    sequence<byte> ImageData;

    struct BusImage
    {
        int busId;
        string tag;
        string timestamp;
        ImageData image;
    }

    interface BusObserver
    {
        void notifyBus(string message);
    }

    interface OperationsControlCenter
    {
        // A bus subscribes with its busId AND its observer (callback) proxy.
        // We need the busId here so the server knows which observer belongs to which bus,
        // otherwise it can't send a CRITICAL alert back to ONLY the bus that sent the image.
        // (In Lab 3 this only took the observer, which was fine since we only ever broadcast)
        void subscribe(int busId, BusObserver* observer);

        void sendBusImage(BusImage busImage);
    }
}