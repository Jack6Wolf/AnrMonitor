package com.jack.testanr;

import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;

import com.jack.monitor.Monitor;
import com.jack.testanr.ui.FPSRecyclerView;

public class FpsActivity extends AppCompatActivity {
    FPSRecyclerView recyclerView;
    SeekBar loadIndicator;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fps);
        recyclerView=findViewById(R.id.recyclerView);
        loadIndicator=findViewById(R.id.loadIndicator);
        setupRadioGroup();


        Monitor.install().startFps(getApplicationContext());
        //或者
//        Monitor.getInstance().startFpsCustom()
//                .redFlagPercentage(0.1f)
//                .startingXPosition(200)
//                .startingYPosition(600)
//                .addFrameDataCallback(new FrameDataCallback() {
//                    @Override
//                    public void doFrame(long previousFrameNS, long currentFrameNS, int droppedFrames) {
//
//                    }
//                })
//                .show(getApplicationContext());
    }
    private void setupRadioGroup() {

        recyclerView.setMegaBytes(50f);
        recyclerView.notifyDataSetChanged();

        loadIndicator.setProgress(50);
        loadIndicator.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                recyclerView.setMegaBytes(Float.valueOf(i));
                recyclerView.notifyDataSetChanged();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Monitor.install().startFps(getApplicationContext());
            }
        });
        findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Monitor.install().stop(getApplicationContext());
            }
        });

    }
}
