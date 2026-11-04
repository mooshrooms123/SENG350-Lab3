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
        void subscribe(BusObserver* observer);

        void sendBusImage(BusImage busImage);
    }
}