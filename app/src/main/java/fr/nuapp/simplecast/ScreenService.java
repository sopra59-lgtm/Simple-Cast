package fr.nuapp.simplecast;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ScreenService extends Service {
    public static final String START = "fr.nuapp.simplecast.START";
    public static final String STOP = "fr.nuapp.simplecast.STOP";
    public static volatile boolean running = false;
    private MediaProjection projection;
    private VirtualDisplay display;
    private ImageReader reader;
    private HandlerThread thread;
    private FrameServer server;
    private volatile byte[] latestFrame;
    private final Handler castHandler = new Handler(Looper.getMainLooper());
    private long lastCastSent = 0;
    private long lastCapture;
    private int width, height, density;
    private MediaProjection.Callback callback;
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        if (STOP.equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        if (!START.equals(intent.getAction()) || running) return START_NOT_STICKY;
        NotificationChannel channel = new NotificationChannel("projection", "Duplication d'écran", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        Notification notification = new Notification.Builder(this, "projection")
            .setContentTitle("Simple Cast : écran partagé")
            .setContentText("Appuyez ici pour revenir à Simple Cast")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE))
            .build();
        if (Build.VERSION.SDK_INT >= 29) startForeground(10, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        else startForeground(10, notification);
        try {
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics(); wm.getDefaultDisplay().getRealMetrics(metrics);
            density = metrics.densityDpi;
            float scale = Math.min(1f, 1280f / Math.max(metrics.widthPixels, metrics.heightPixels));
            width = Math.max(2, (int)(metrics.widthPixels * scale) & ~1);
            height = Math.max(2, (int)(metrics.heightPixels * scale) & ~1);
            thread = new HandlerThread("capture"); thread.start();
            reader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2);
            int code = intent.getIntExtra("code", Activity.RESULT_CANCELED);
            Intent data = intent.getParcelableExtra("data");
            MediaProjectionManager manager = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
            projection = manager.getMediaProjection(code, data);
            callback = new MediaProjection.Callback() { @Override public void onStop() { stopSelf(); } };
            projection.registerCallback(callback, new Handler(thread.getLooper()));
            reader.setOnImageAvailableListener(r -> {
                Image image = r.acquireLatestImage();
                if (image == null) return;
                try {
                    if (SystemClock.elapsedRealtime() - lastCapture < 330) return;
                    lastCapture = SystemClock.elapsedRealtime();
                    Image.Plane plane = image.getPlanes()[0];
                    ByteBuffer buffer = plane.getBuffer();
                    int stride = plane.getRowStride();
                    int pixelStride = plane.getPixelStride();
                    Bitmap padded = Bitmap.createBitmap(width + (stride - pixelStride * width) / pixelStride, height, Bitmap.Config.ARGB_8888);
                    padded.copyPixelsFromBuffer(buffer);
                    Bitmap cropped = Bitmap.createBitmap(padded, 0, 0, width, height);
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    float ratio = Math.min(1f, 640f / Math.max(width, height));
                    Bitmap small = Bitmap.createScaledBitmap(cropped, Math.max(1,(int)(width*ratio)), Math.max(1,(int)(height*ratio)), true);
                    small.compress(Bitmap.CompressFormat.JPEG, 32, output);
                    latestFrame = output.toByteArray();
                    if (small != cropped) small.recycle();
                    byte[] frame = latestFrame;
                    if (frame.length < 45000 && SystemClock.elapsedRealtime()-lastCastSent > 900) {
                        lastCastSent = SystemClock.elapsedRealtime();
                        castHandler.post(() -> sendFrame(frame));
                    }
                    cropped.recycle(); padded.recycle();
                } catch (Exception ignored) { } finally { image.close(); }
            }, new Handler(thread.getLooper()));
            display = projection.createVirtualDisplay("SimpleCast", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader.getSurface(), null, new Handler(thread.getLooper()));
            server = new FrameServer(() -> latestFrame); server.start();
            running = true;
        } catch (Exception error) { stopSelf(); }
        return START_NOT_STICKY;
    }
    private void sendFrame(byte[] frame) {
        try {
            com.google.android.gms.cast.framework.CastSession session = com.google.android.gms.cast.framework.CastContext.getSharedInstance(this).getSessionManager().getCurrentCastSession();
            if(session == null || !session.isConnected()) return;
            org.json.JSONObject data = new org.json.JSONObject();
            data.put("type", "frame");
            data.put("jpeg", android.util.Base64.encodeToString(frame, android.util.Base64.NO_WRAP));
            session.sendMessage(CastOptionsProvider.NAMESPACE, data.toString());
        } catch(Exception e) { android.util.Log.w("SimpleCast", "Cast frame delivery failed", e); }
    }
    @Override public void onDestroy() {
        running = false;
        if (server != null) server.shutdown();
        if (display != null) { display.release(); display = null; }
        if (reader != null) { reader.close(); reader = null; }
        if (projection != null) { if (callback != null) projection.unregisterCallback(callback); projection.stop(); projection = null; }
        if (thread != null) { thread.quitSafely(); thread = null; }
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
