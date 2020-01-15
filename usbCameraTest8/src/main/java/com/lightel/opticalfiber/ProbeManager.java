package com.lightel.opticalfiber;

public final class ProbeManager {

    public Probe DI1000 = new Probe("DI1000", false);
    public Probe DI1000L = new Probe("DI1000L", false);
    public Probe DI2000 = new Probe("DI2000", false);
    public Probe DI3000 = new Probe("DI3000", false);
    public Probe DI5000 = new Probe("DI500", false);

    private static volatile ProbeManager sInstance = null;

    private ProbeManager() {
        if (sInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    // Double check locking pattern
    public static ProbeManager getInstance() {
        if (sInstance == null) {
            synchronized (ProbeManager.class) {
                if (sInstance == null) sInstance = new ProbeManager();
            }
        }
        return sInstance;
    }

    static class Probe {
        public String probeName;
        public boolean enable;

        Probe(String probeName, boolean enable) {
            this.probeName = probeName;
            this.enable = enable;
        }
    }
}
