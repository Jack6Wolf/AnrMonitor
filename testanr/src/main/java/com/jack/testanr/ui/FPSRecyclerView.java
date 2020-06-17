package com.jack.testanr.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


/**
 * @author jack
 * @since 2020/5/9 13:29
 */
public class FPSRecyclerView extends RecyclerView {
    FPSSampleAdpater adapter;

    public FPSRecyclerView(Context context) {
        this(context, null);
    }

    public FPSRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        adapter=new FPSSampleAdpater();
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        setLayoutManager(layoutManager);
        addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST));
        setAdapter(adapter);

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public FPSRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    public void setMegaBytes(Float megaBytes) {
        adapter.setMegaBytes(megaBytes);
    }

    public void notifyDataSetChanged() {
        adapter.notifyDataSetChanged();
    }
}
