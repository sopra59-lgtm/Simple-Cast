package fr.nuapp.simplecast;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Looper;
import java.util.LinkedHashMap;
import java.util.Map;

/** Discovers Google Cast advertisements on the local network; discovery is not mirroring. */
public final class CastDiscovery {
    public interface Listener { void changed(String[] names); }
    private final NsdManager nsd;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String,String> devices = new LinkedHashMap<>();
    private final Listener listener;
    private NsdManager.DiscoveryListener discovery;
    public CastDiscovery(Context context, Listener listener) {
        nsd=(NsdManager)context.getSystemService(Context.NSD_SERVICE);
        this.listener=listener;
    }
    public void start() {
        if (discovery != null) return;
        discovery = new NsdManager.DiscoveryListener() {
            @Override public void onDiscoveryStarted(String serviceType) { }
            @Override public void onServiceFound(NsdServiceInfo service) {
                main.post(() -> {
                    devices.put(service.getServiceName(), service.getServiceName());
                    listener.changed(devices.values().toArray(new String[0]));
                });
            }
            @Override public void onServiceLost(NsdServiceInfo service) {
                main.post(() -> {
                    devices.remove(service.getServiceName());
                    listener.changed(devices.values().toArray(new String[0]));
                });
            }
            @Override public void onDiscoveryStopped(String type) { }
            @Override public void onStartDiscoveryFailed(String type, int error) { stop(); }
            @Override public void onStopDiscoveryFailed(String type, int error) { }
        };
        try { nsd.discoverServices("_googlecast._tcp.", NsdManager.PROTOCOL_DNS_SD, discovery); }
        catch (Exception e) { discovery = null; }
    }
    public void stop() {
        NsdManager.DiscoveryListener current = discovery;
        discovery = null;
        if(current != null) try { nsd.stopServiceDiscovery(current); } catch(Exception ignored) { }
    }
}
