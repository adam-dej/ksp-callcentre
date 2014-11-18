package sk.ksp.callcentrum;

public class DataStorage {

    private DataStorage() {
        if (BuildConfig.serverAddress != null) {
            serverAddress = BuildConfig.serverAddress.split(":")[0];
            serverPort = BuildConfig.serverAddress.split(":")[1];
        }
    }

    private static DataStorage instance;

    public static DataStorage getStorage() {
        if (instance == null) instance = new DataStorage();
        return instance;
    }

    private String serverAddress;
    private String serverPort;

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public String getServerPort() {
        return serverPort;
    }

    public void setServerPort(String serverPort) {
        this.serverPort = serverPort;
    }
}
