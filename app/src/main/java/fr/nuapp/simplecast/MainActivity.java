package fr.nuapp.simplecast;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.Collections;

public class MainActivity extends Activity {
    private static final int CAPTURE_REQUEST = 1001;
    private TextView status;
    private CastDiscovery discovery;
    private LinearLayout devicesContainer;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 64, 32, 32);
        layout.setBackgroundColor(Color.rgb(247,249,252));
        TextView heading = new TextView(this); heading.setText("Simple Cast"); heading.setTextSize(30); heading.setTextColor(Color.rgb(24,40,66));
        layout.addView(heading);
        TextView help = new TextView(this);
        help.setText("Prototype de duplication d'écran sur réseau local. Ouvrez l'adresse indiquée dans un navigateur sur le même Wi-Fi. La prise en charge native de Chromecast nécessite un récepteur Cast dédié.");
        help.setTextSize(16); help.setPadding(0, 24, 0, 24); layout.addView(help);
        status = new TextView(this); status.setTextSize(18); layout.addView(status);
        TextView title = new TextView(this); title.setText("Appareils Chromecast détectés"); title.setTextSize(20); layout.addView(title);
        devicesContainer = new LinearLayout(this); devicesContainer.setOrientation(LinearLayout.VERTICAL); layout.addView(devicesContainer);
        TextView empty = new TextView(this); empty.setText("Recherche en cours sur le Wi-Fi…"); devicesContainer.addView(empty);
        discovery = new CastDiscovery(this, names -> {
            devicesContainer.removeAllViews();
            if (names.length == 0) { TextView t = new TextView(this); t.setText("Aucun Chromecast détecté. Vérifiez le Wi-Fi et l’isolation réseau."); devicesContainer.addView(t); }
            for (String name : names) { Button b = new Button(this); b.setText(name); b.setOnClickListener(v -> new android.app.AlertDialog.Builder(this).setTitle(name).setMessage("Appareil découvert. La duplication directe nécessite encore un récepteur Google Cast personnalisé et un transport vidéo compatible. Aucune diffusion n’est lancée.").setPositiveButton("OK",null).show()); devicesContainer.addView(b); }
        });
        Button start = new Button(this); start.setText("Démarrer la duplication"); layout.addView(start);
        Button stop = new Button(this); stop.setText("Arrêter"); layout.addView(stop);
        start.setVisibility(android.view.View.GONE); stop.setVisibility(android.view.View.GONE);
        start.setOnClickListener(v -> {
            MediaProjectionManager manager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            startActivityForResult(manager.createScreenCaptureIntent(), CAPTURE_REQUEST);
        });
        stop.setOnClickListener(v -> { Intent i = new Intent(this, ScreenService.class); i.setAction(ScreenService.STOP); startService(i); refreshStatus(); });
        Button update = new Button(this); update.setText("Vérifier les mises à jour"); layout.addView(update);
        UpdateManager updates = new UpdateManager(this);
        update.setOnClickListener(v -> updates.check(true));
        setContentView(layout); refreshStatus();
        updates.check(false);
    }
    @Override protected void onResume() { super.onResume(); if (status != null) refreshStatus(); if(discovery != null) discovery.start(); }
    @Override protected void onPause() { if(discovery != null) discovery.stop(); super.onPause(); }
    @Override protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        if (req == CAPTURE_REQUEST && result == RESULT_OK && data != null) {
            Intent service = new Intent(this, ScreenService.class);
            service.setAction(ScreenService.START);
            service.putExtra("code", result);
            service.putExtra("data", data);
            startForegroundService(service);
            status.postDelayed(this::refreshStatus, 1000);
        }
    }
    private void refreshStatus() {
        String ip = "adresse IP inconnue";
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces()))
                for (java.net.InetAddress address : Collections.list(ni.getInetAddresses()))
                    if (!address.isLoopbackAddress() && address instanceof Inet4Address && address.isSiteLocalAddress()) { ip = address.getHostAddress(); break; }
        } catch (Exception ignored) {}
        status.setText("Recherche des appareils Google Cast sur le réseau local…");
    }
}
