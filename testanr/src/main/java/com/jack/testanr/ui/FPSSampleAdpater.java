package com.jack.testanr.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.jack.testanr.R;


/**
 * @author jack
 * @since 2020/5/9 13:29
 */
public class FPSSampleAdpater extends RecyclerView.Adapter<FPSSampleViewHolder>
{

    private float megaBytes = 1;

    public FPSSampleAdpater() {
    }

    @Override
    public FPSSampleViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View itemView = LayoutInflater.from(
                parent.getContext()).inflate(R.layout.sample_item, parent, false);
        return new FPSSampleViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(FPSSampleViewHolder holder, int position)
    {
        holder.onBind(position, megaBytes);
    }

    @Override
    public int getItemCount()
    {
        return 255;
    }

    public void setMegaBytes(float megaBytes) {
        this.megaBytes = megaBytes;
    }
}
