package fr.nuapp.simplecast;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

/**
 * Simple-Cast minimal home. System casting stays independent from the optional
 * MediaProjection capture service, which remains available in the overflow menu.
 */
public class MainActivity extends Activity {
    private static final int CAPTURE_REQUEST = 1001;
    private UpdateManager updates;

    private int dp(float dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable oval(int color, int stroke, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        if (strokeWidth > 0) drawable.setStroke(dp(strokeWidth), stroke);
        return drawable;
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(10, 17, 33));
        getWindow().setNavigationBarColor(Color.rgb(10, 17, 33));
        getWindow().getDecorView().setSystemUiVisibility(0);

        updates = new UpdateManager(this);
        FrameLayout page = new FrameLayout(this);
        page.setBackgroundColor(Color.rgb(10, 17, 33));

        // Minimal header: app identity and overflow menu.
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(25), dp(12), dp(12), dp(12));

        TextView title = new TextView(this);
        title.setText("Simple Cast");
        title.setTextColor(Color.WHITE);
        title.setTextSize(23);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        header.addView(title, new LinearLayout.LayoutParams(0, dp(52), 1f));

        ImageButton more = new ImageButton(this);
        more.setImageResource(R.drawable.ic_more_vert);
        more.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        more.setPadding(dp(13), dp(13), dp(13), dp(13));
        more.setContentDescription("Menu : mises à jour et options");
        more.setBackground(new RippleDrawable(
            ColorStateList.valueOf(0x33FFFFFF),
            oval(Color.TRANSPARENT, 0, 0),
            new ShapeDrawable(new OvalShape())
        ));
        header.addView(more, new LinearLayout.LayoutParams(dp(52), dp(52)));

        FrameLayout.LayoutParams headerParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP);
        headerParams.topMargin = dp(12);
        page.addView(header, headerParams);

        // Central cast button with subtle concentric rings.
        FrameLayout hero = new FrameLayout(this);
        FrameLayout.LayoutParams heroParams = new FrameLayout.LayoutParams(dp(316), dp(316), Gravity.CENTER);
        page.addView(hero, heroParams);

        View outerRing = new View(this);
        outerRing.setBackground(oval(0x00162C54, 0x2244A1F7, 1));
        FrameLayout.LayoutParams outer = new FrameLayout.LayoutParams(dp(310), dp(310), Gravity.CENTER);
        hero.addView(outerRing, outer);

        View innerRing = new View(this);
        innerRing.setBackground(oval(0x00162C54, 0x3263B2FD, 1));
        FrameLayout.LayoutParams inner = new FrameLayout.LayoutParams(dp(260), dp(260), Gravity.CENTER);
        hero.addView(innerRing, inner);

        GradientDrawable castGradient = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[] { 0xFF178DEB, 0xFF3466E8, 0xFF705DE9 }
        );
        castGradient.setShape(GradientDrawable.OVAL);
        castGradient.setStroke(dp(1), 0x669DCBFF);

        FrameLayout castButton = new FrameLayout(this);
        castButton.setClickable(true);
        castButton.setFocusable(true);
        castButton.setContentDescription("Diffuser l'écran sur un appareil compatible");
        castButton.setBackground(new RippleDrawable(
            ColorStateList.valueOf(0x44FFFFFF), castGradient, oval(Color.WHITE, 0, 0)));
        castButton.setElevation(dp(16));
        FrameLayout.LayoutParams castParams = new FrameLayout.LayoutParams(dp(218), dp(218), Gravity.CENTER);
        hero.addView(castButton, castParams);

        ImageView castIcon = new ImageView(this);
        castIcon.setImageResource(R.drawable.ic_cast);
        castIcon.setContentDescription(null);
        castIcon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(dp(112), dp(112), Gravity.CENTER);
        castButton.addView(castIcon, iconParams);

        castButton.setOnClickListener(v -> openSystemCast());
        more.setOnClickListener(v -> showMenu(more));
        setContentView(page);
        updates.check(false);
    }

    private void showMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, "Vérifier les mises à jour");
        menu.getMenu().add(0, 2, 1, "Capture MediaProjection (avancé)");
        if (ScreenService.running) menu.getMenu().add(0, 3, 2, "Arrêter la capture");
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) { updates.check(true); return true; }
            if (item.getItemId() == 2) {
                MediaProjectionManager manager =
                    (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                startActivityForResult(manager.createScreenCaptureIntent(), CAPTURE_REQUEST);
                return true;
            }
            if (item.getItemId() == 3) {
                Intent stop = new Intent(this, ScreenService.class);
                stop.setAction(ScreenService.STOP);
                startService(stop);
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void openSystemCast() {
        try {
            startActivity(new Intent("android.settings.CAST_SETTINGS"));
        } catch (Exception ignored) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
                new AlertDialog.Builder(this)
                    .setMessage("Recherchez « Diffuser », « Smart View » ou « Projection sans fil » dans les paramètres Android.")
                    .setPositiveButton("OK", null).show();
            } catch (Exception error) {
                new AlertDialog.Builder(this)
                    .setMessage("Impossible d'ouvrir les paramètres de diffusion sur cet appareil.")
                    .setPositiveButton("OK", null).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == CAPTURE_REQUEST && result == RESULT_OK && data != null) {
            Intent service = new Intent(this, ScreenService.class);
            service.setAction(ScreenService.START);
            service.putExtra("code", result);
            service.putExtra("data", data);
            startForegroundService(service);
        }
    }
}
