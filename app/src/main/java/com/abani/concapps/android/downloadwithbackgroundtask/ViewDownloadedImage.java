package com.abani.concapps.android.downloadwithbackgroundtask;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

public class ViewDownloadedImage extends AppCompatActivity {

    ImageView ivDdownloaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_downloaded_image);

        ivDdownloaded = (ImageView) findViewById(R.id.iv_downloaded);

        if(getIntent() != null){
            String imagePath = getIntent().getStringExtra("imagePath");
            File imgFile = new File(imagePath);
            if(imgFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                ivDdownloaded.setImageBitmap(bitmap);
            }
        }
    }
}
