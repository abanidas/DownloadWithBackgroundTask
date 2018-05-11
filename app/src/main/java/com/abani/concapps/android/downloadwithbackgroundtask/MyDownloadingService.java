package com.abani.concapps.android.downloadwithbackgroundtask;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by user on 5/6/2018.
 */

public class MyDownloadingService extends Service {

    private static final String DOWNLOAD_COMPLETE_NOTIFICATION_CHANNEL_ID = "download_complete_id";
    private static final String NOTIFICATION_ACTION_CANCEL  = "cancel";
    private static final String NOTIFICATION_ACTION_OPEN_IMAGE = "open_image";
    private static final int DOWNLOAD_COMPLETE_NOTIFICATION_ID = 222;
    private static final int REQUEST_CODE_CANCEL_NOTIFICATION_ACTION = 12;
    private static final int REQUEST_CODE_OPEN_NOTIFICATION_ACTION = 15;

    private int downloadProgress = 0;
    private final IBinder mBinder = new MyDownloadBinder();
    private String downloadedImageBitmap;
    private String downlaodStatus = "";

    public class MyDownloadBinder extends Binder {
        MyDownloadingService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MyDownloadingService.this;
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        downlaodStatus = "";
        String urlString = intent.getStringExtra("imageUrl");
        if (urlString == null || urlString.trim().equals("")){
            downlaodStatus = "failed";
            Toast.makeText(this, "Please enter the URL ", Toast.LENGTH_SHORT).show();
        }
        else {

            new DownloadAsyncTask().execute(urlString);
        }
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(NOTIFICATION_ACTION_OPEN_IMAGE)) {
            Intent imgIntent = new Intent(this, ViewDownloadedImage.class);
            imgIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            imgIntent.putExtra("imagePath", downloadedImageBitmap);
            startActivity(imgIntent);

        }
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancelAll();
        return super.onStartCommand(intent, flags, startId);
    }

    private void pushNotification(){
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    DOWNLOAD_COMPLETE_NOTIFICATION_CHANNEL_ID,
                    getString(R.string.main_notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this,DOWNLOAD_COMPLETE_NOTIFICATION_CHANNEL_ID)
                .setColor(ContextCompat.getColor(this, R.color.colorAccent))
                .setSmallIcon(R.drawable.ic_file_download_black_24dp)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_file_download_black_24dp))
                .setContentTitle(getString(R.string.download_completed_notification_title))
                .setContentText(getString(R.string.download_completed_notification_body))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        getString(R.string.download_completed_notification_body)))
                .addAction(cancelNotificationAction())
                .addAction(openImageAction())
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setContentIntent(contentIntent())
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }
        notificationManager.notify(DOWNLOAD_COMPLETE_NOTIFICATION_ID, notificationBuilder.build());
    }

    private NotificationCompat.Action cancelNotificationAction() {

        Intent cancelIntent = new Intent(this, MyDownloadingService.class);
        cancelIntent.setAction(NOTIFICATION_ACTION_CANCEL);
        PendingIntent pendingIntent = PendingIntent.getService(this,
                REQUEST_CODE_CANCEL_NOTIFICATION_ACTION,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action action = new NotificationCompat.Action(R.drawable.ic_cancel_black_24px,
                "CANCEL", pendingIntent);
        return action;
    }

    private NotificationCompat.Action openImageAction() {

        NotificationCompat.Action action = new NotificationCompat.Action(R.drawable.ic_file_download_black_24dp,
                "OPEN", contentIntent());
        return action;
    }

    private PendingIntent contentIntent() {
        Intent startServiceIntent = new Intent(this, MyDownloadingService.class);
        startServiceIntent.setAction(NOTIFICATION_ACTION_OPEN_IMAGE);
        return PendingIntent.getService(
                this,
                REQUEST_CODE_OPEN_NOTIFICATION_ACTION,
                startServiceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public int getDownloadProgress(){
        return downloadProgress;
    }
    public String getDownlaodStatus(){
        return downlaodStatus;
    }

    class DownloadAsyncTask extends AsyncTask<String, Void, Void>{

        @Override
        protected Void doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                URLConnection connection = url.openConnection();

                String contentType = connection.getHeaderField("Content-Type");
                boolean isiImage = contentType.startsWith("image/");

                if (isiImage) {

                    downlaodStatus = "downloading";

                    connection.connect();

                    int fileLength = connection.getContentLength();

                    File rootSDCard = Environment.getExternalStorageDirectory().getAbsoluteFile();

                    String filename="downloadedFile."+getMimeType(urls[0]);;
                    File file = new File(rootSDCard,filename);

                    if(!file.exists())
                    {
                        file.createNewFile();
                    }
                    FileOutputStream fileOutput = new FileOutputStream(file);

                    InputStream input = new BufferedInputStream(connection.getInputStream());
                    ByteArrayOutputStream out = new ByteArrayOutputStream();

                    byte data[] = new byte[1024];
                    long total = 0;
                    int count;

                    while ((count = input.read(data)) != -1) {
                        total += count;
                        downloadProgress = (int) (total * 100 / fileLength);
                        fileOutput.write(data, 0, count);
                        out.write(data, 0, count);
                    }
                    fileOutput.flush();
                    out.flush();
                    String filePath = "";
                    if (fileLength == total){
                        filePath=file.getPath();
                    }

                    byte[] imageByte = out.toByteArray();
                    out.close();
                    downloadedImageBitmap = filePath;

                    downloadProgress = 100;
                    input.close();
                }

            } catch (Exception e) {
                e.printStackTrace();

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (downloadProgress >= 100) {
                pushNotification();
            } else {
                downlaodStatus = "failed";
                Toast.makeText(MyDownloadingService.this, "Please enter an URL of image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public String getMimeType(String uriSting) {
        String extension;

        Uri uri = Uri.parse(uriSting);

        //Check uri format to avoid null
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            //If scheme is a content
            final MimeTypeMap mime = MimeTypeMap.getSingleton();
            extension = mime.getExtensionFromMimeType(getContentResolver().getType(uri));
        } else {
            //If scheme is a File
            //This will replace white spaces with %20 and also other special characters. This will avoid returning null values on file name with spaces and special characters.
            extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(uri.getPath())).toString());

        }
        return extension;
    }
}
