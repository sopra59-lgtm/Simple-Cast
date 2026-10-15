package fr.nuapp.simplecast;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;

public class MainActivity extends Activity {
    private static final int CAPTURE_REQUEST = 1001;
    private TextView status;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 56, 32, 32);
        layout.setBackgroundColor(Color.rgb(247, 249, 252));

        TextView title = new TextView(this);
        title.setText("Simple Cast"); title.setTextSize(30);
        title.setTextColor(Color.rgb(24,40,66)); layout.addView(title);

        TextView description = new TextView(this);
        description.setText("Pour dupliquer l'écran sur un XGIMI, ouvrez la fonction de diffusion intégrée à Android. Si le projecteur est compatible avec la diffusion système, sélectionnez-le dans cette liste.\n\nMediaProjection est disponible séparément pour autoriser la capture de l'écran dans Simple Cast.");
        description.setTextSize(16); description.setPadding(0, 24, 0, 24);
        layout.addView(description);

        Button systemCast = new Button(this);
        systemCast.setText("Rechercher un écran (diffusion Android)");
        layout.addView(systemCast);
        systemCast.setOnClickListener(v -> openSystemCast());

        Button capture = new Button(this);
        capture.setText("Autoriser la capture MediaProjection");
        layout.addView(capture);
        capture.setOnClickListener(v -> {
            MediaProjectionManager manager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            startActivityForResult(manager.createScreenCaptureIntent(), CAPTURE_REQUEST);
        });

        Button stop = new Button(this);
        stop.setText("Arrêter la capture"); layout.addView(stop);
        stop.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScreenService.class);
            intent.setAction(ScreenService.STOP);
            startService(intent);
            refresh();
        });

        status = new TextView(this);
        status.setPadding(0, 18, 0, 18);
        layout.addView(status);

        Button update = new Button(this);
        update.setText("Vérifier les mises à jour"); layout.addView(update);
        UpdateManager manager = new UpdateManager(this);
        update.setOnClickListener(v -> manager.check(true));

        setContentView(layout);
        refresh();
        manager.check(false);
    }

    private void openSystemCast() {
        try {
            startActivity(new Intent("android.settings.CAST_SETTINGS"));
        } catch (Exception ex) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
                new AlertDialog.Builder(this).setMessage("Votre appareil n'expose pas la page Diffusion. Dans les paramètres Android, recherchez « Diffuser », « Smart View » ou « Projection sans fil ».").setPositiveButton("OK", null).show();
            } catch (Exception e) {
                new AlertDialog.Builder(this).setMessage("Impossible d'ouvrir les paramètres de diffusion sur cet appareil.").setPositiveButton("OK",null).show();
            }
        }
    }

    @Override protected void onResume() { super.onResume(); if (status != null) refresh(); }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == CAPTURE_REQUEST && result == RESULT_OK && data != null) {
            Intent service = new Intent(this, ScreenService.class);
            service.setAction(ScreenService.START);
            service.putExtra("code", result);
            service.putExtra("data", data);
            startForegroundService(service);
            status.postDelayed(this::refresh, 1200);
        }
    }
    private void refresh() {
        status.setText(ScreenService.running
            ? "Capture MediaProjection active. La capture seule ne transmet pas l'écran à un XGIMI."
            : "Capture MediaProjection arrêtée.");
    }
}
