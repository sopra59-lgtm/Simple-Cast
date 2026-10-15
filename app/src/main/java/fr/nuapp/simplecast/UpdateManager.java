package fr.nuapp.simplecast;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.Toast;
import org.json.*;
import java.net.*;
import java.io.*;

/** Public GitHub Release update discovery and user-confirmed installation. */
public final class UpdateManager {
    private static final String API = "https://api.github.com/repos/sopra59-lgtm/Simple-Cast/releases/latest";
    private final Activity activity;
    private final Handler main = new Handler(Looper.getMainLooper());
    private long downloadId = -1;
    private String expectedFile;
    public UpdateManager(Activity activity) { this.activity=activity; }

    public void check(boolean manual) {
        new Thread(() -> {
            try {
                HttpURLConnection conn=(HttpURLConnection)new URL(API).openConnection();
                conn.setConnectTimeout(10000);conn.setReadTimeout(10000);
                conn.setRequestProperty("Accept","application/vnd.github+json");
                conn.setRequestProperty("User-Agent","Simple-Cast-Android");
                if(conn.getResponseCode()==404) { if(manual)message("Aucune version publiée dans GitHub Releases pour le moment."); conn.disconnect(); return; }
                if(conn.getResponseCode()!=200)throw new IOException("GitHub HTTP "+conn.getResponseCode());
                ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=conn.getInputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
                JSONObject release=new JSONObject(out.toString("UTF-8"));
                int latest=Integer.parseInt(release.optString("tag_name","").replaceFirst("^v","").split("\\.")[0]);
                // Release tags use vVERSION_CODE (e.g. v2).
                int current=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).versionCode;
                if(latest<=current){if(manual)message("Application à jour.");return;}
                JSONArray assets=release.getJSONArray("assets");String url=null;
                for(int i=0;i<assets.length();i++){JSONObject asset=assets.getJSONObject(i);if(asset.getString("name").equals("simple-cast.apk")){url=asset.getString("browser_download_url");break;}}
                if(url==null)throw new IOException("APK introuvable dans la release");
                final String download=url;
                main.post(()->new AlertDialog.Builder(activity).setTitle("Mise à jour disponible")
                    .setMessage("Une nouvelle version de Simple-Cast est disponible. Télécharger et lancer l'installation ?")
                    .setNegativeButton("Plus tard",null).setPositiveButton("Mettre à jour",(d,w)->download(download)).show());
            }catch(Exception e){if(manual)message("Recherche impossible : "+e.getMessage());}
        }).start();
    }
    private void message(String text){main.post(()->Toast.makeText(activity,text,Toast.LENGTH_LONG).show());}
    private void download(String url){
        try {
            DownloadManager dm=(DownloadManager)activity.getSystemService(Context.DOWNLOAD_SERVICE);
            expectedFile="simple-cast-update-"+System.currentTimeMillis()+".apk";
            DownloadManager.Request req=new DownloadManager.Request(Uri.parse(url));
            req.setTitle("Mise à jour Simple-Cast");req.setDescription("Téléchargement de la nouvelle version");
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalFilesDir(activity,Environment.DIRECTORY_DOWNLOADS,expectedFile);
            downloadId=dm.enqueue(req);
            poll(dm,0);
        }catch(Exception e){message("Téléchargement impossible : "+e.getMessage());}
    }
    private void poll(DownloadManager dm,int attempts){
        if(attempts>120){message("Téléchargement trop long.");return;}
        main.postDelayed(()->{
            try(Cursor c=dm.query(new DownloadManager.Query().setFilterById(downloadId))){
                if(c==null||!c.moveToFirst())return;
                int state=c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                if(state==DownloadManager.STATUS_SUCCESSFUL){
                    Uri apk=dm.getUriForDownloadedFile(downloadId);
                    Intent install=new Intent(Intent.ACTION_VIEW);
                    install.setDataAndType(apk,"application/vnd.android.package-archive");
                    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
                    if(Build.VERSION.SDK_INT>=26&&!activity.getPackageManager().canRequestPackageInstalls()){
                        message("Autorise Simple-Cast à installer des applications, puis relance la mise à jour.");
                        Intent settings=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName()));
                        activity.startActivity(settings);return;
                    }
                    activity.startActivity(install);
                }else if(state==DownloadManager.STATUS_FAILED)message("Le téléchargement a échoué.");
                else poll(dm,attempts+1);
            }catch(Exception e){message("Installation impossible : "+e.getMessage());}
        },1000);
    }
}
