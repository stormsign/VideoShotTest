package com.zjb.test.videoshottest.view.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.zjb.test.videoshottest.R;
import com.zjb.test.videoshottest.view.widget.VideoRecorderView;

public class MainActivity extends AppCompatActivity {

    private boolean recording ;
    private VideoRecorderView videoRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i("VRV", "main activity");
        videoRecorder = (VideoRecorderView) findViewById(R.id.videoRecorder);
//        findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                record(v);
//            }
//        });
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) videoRecorder.getLayoutParams();
        DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
        int width = dm.widthPixels;
        params.height = width * 4 / 3;
        videoRecorder.setLayoutParams(params);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void record(View view){
        Log.i("VRV", "is recording now ?  "+recording);
        recording = !recording;
        if (recording) {
            videoRecorder.record(new VideoRecorderView.OnRecordFinishListener() {
                @Override
                public void onRecordFinish() {
                    Toast.makeText(MainActivity.this, "结束", Toast.LENGTH_SHORT).show();
                }
            });
        }else {
            videoRecorder.stop();
        }
    }
}
