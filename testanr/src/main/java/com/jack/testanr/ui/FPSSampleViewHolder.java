package com.jack.testanr.ui;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.SystemClock;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.jack.testanr.R;


/**
 * @author jack
 * @since 2020/5/9 13:29
 */
public class FPSSampleViewHolder extends RecyclerView.ViewHolder {

    private int[] data;
    private ImageView colorImg;
    private TextView bindTime;

    public FPSSampleViewHolder(View itemView) {
        super(itemView);
        colorImg = (ImageView) itemView.findViewById(R.id.colorImg);
        bindTime = (TextView) itemView.findViewById(R.id.bindTime);
        data = new int[1024 * 10];
    }

    public void onBind(int value, float megaBytes) {

        configureColor(value);

        int total = (int) (megaBytes * 100f);
        long start = SystemClock.elapsedRealtime();
        int startInt = (int) start;
        //创造一些阻塞UI的逻辑，达到丢帧目的
        for (int i = 0; i < total; i++) {
            for (int e = 0; e < data.length; e++) {
                data[e] = startInt;
            }
        }
        long end = SystemClock.elapsedRealtime();
        long bindTimeMs = end - start;

        bindTime.setText(bindTimeMs + "ms onBind()");
    }

    private void configureColor(int value) {
        int multiplier = 22;
        int hundred = value / 100;
        int tens = (value - (hundred) * 100) / 10;
        int ones = value - (hundred * 100) - (tens * 10);
        int r = hundred * multiplier;
        int g = tens * multiplier;
        int b = ones * multiplier;
        int colorVal = Color.rgb(r, g, b);
        colorImg.setImageDrawable(new ColorDrawable(colorVal));
    }
}
