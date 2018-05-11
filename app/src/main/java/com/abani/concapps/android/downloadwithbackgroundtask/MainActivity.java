package com.abani.concapps.android.downloadwithbackgroundtask;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    EditText editTextURL;
    Button btnDownload;
    ProgressBar progressBar;
    TextView txtProgress;
    TextView txtStatus;
    TextView downloadingText;

    MyDownloadingService myDownloadingService;
    boolean mBound = false;

    Timer timer;
    int progress = 0;

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextURL = (EditText) findViewById(R.id.editTextURL);
        btnDownload = (Button) findViewById(R.id.buttonDownload);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        txtProgress = (TextView) findViewById(R.id.textProgress);
        txtStatus = (TextView) findViewById(R.id.textStatus);
        downloadingText = (TextView) findViewById(R.id.textDownloading);

        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBound){
                    unbindService(mConnection);

                }
                downloadingText.setVisibility(View.VISIBLE);
                txtStatus.setText("Downloading");
                downloadingText.setText("Downloading...");
                Intent intent = new Intent(MainActivity.this, MyDownloadingService.class);
                intent.putExtra("imageUrl", editTextURL.getText().toString());
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                startGettingDownloadProgress();
            }
        });

        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    1
            );
        }
    }

    private void startGettingDownloadProgress() {
        if (timer != null){
            stopGettingDownloadProgress();
        }
        timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        String status="";
                        if (myDownloadingService != null) {
                            progress = myDownloadingService.getDownloadProgress();
                            status = myDownloadingService.getDownlaodStatus();
                        }

                        if (status.equals("failed")){
                            txtStatus.setText("failed");
                            downloadingText.setVisibility(View.INVISIBLE);
                        }
                        showProgress();
                        if (progress >= 100){
                            downloadingText.setText("Download Complete");
                            txtStatus.setText("DOWNLOADED");
                            stopGettingDownloadProgress();
                        }

                    };
                });

            }
        };

        timer.schedule(timerTask, 0, 5);
    }

    private void stopGettingDownloadProgress() {
        timer.cancel();
    }

    public void showProgress() {
        if (progress>100){
            progress = 100;
        }
        progressBar.setProgress(progress);
        txtProgress.setText(progress+"%");
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MyDownloadingService.MyDownloadBinder binder = (MyDownloadingService.MyDownloadBinder) service;
            myDownloadingService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }
}
