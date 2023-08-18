package com.woohyman.gui.ui.gamegallery.adapter;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.blankj.utilcode.util.Utils;

import java.util.ArrayList;

import nostalgia.framework.R;
import nostalgia.framework.data.database.GameDescription;
import nostalgia.framework.data.entity.RowItem;
import nostalgia.framework.rom.DownloaderDelegate;
import nostalgia.framework.ui.gamegallery.adapter.AppStoreAdapter;
import nostalgia.framework.ui.gamegallery.adapter.GalleryAdapter;
import nostalgia.framework.utils.NLog;

public class GalleryPagerAdapter extends PagerAdapter {

    public static final String EXTRA_POSITIONS = "EXTRA_POSITIONS";

    private final GalleryAdapter.SORT_TYPES[] tabTypes = {
            GalleryAdapter.SORT_TYPES.SORT_BY_NAME_ALPHA,
            GalleryAdapter.SORT_TYPES.SORT_BY_LAST_PLAYED,
    };

    private int[] yOffsets = new int[tabTypes.length + 1];
    private final ListView[] lists = new ListView[tabTypes.length + 1];
    private final GalleryAdapter[] listAdapters = new GalleryAdapter[tabTypes.length];
    private final Activity activity;
    private final OnItemClickListener listener;

    public GalleryPagerAdapter(Activity activity, OnItemClickListener listener) {
        this.activity = activity;
        this.listener = listener;
        for (int i = 0; i < tabTypes.length; i++) {
            GalleryAdapter adapter = listAdapters[i] = new GalleryAdapter(activity);
            adapter.setSortType(tabTypes[i]);
        }
    }

    @Override
    public int getCount() {
        return tabTypes.length + 1;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position == 0) {
            return "游戏商店";
        } else {
            return tabTypes[position - 1].getTabName();
        }
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0.equals(arg1);
    }

    @NonNull
    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        final ListView list = new ListView(activity);
        list.setCacheColorHint(0x00000000);
        list.setFastScrollEnabled(true);
        list.setSelector(R.drawable.row_game_item_list_selector);
        if (position == 0) {
            ListAdapter adapter = new AppStoreAdapter(Utils.getApp());
            list.setAdapter(adapter);
            list.setOnItemClickListener((arg0, arg1, arg2, arg3) -> {
                RowItem item = (RowItem) adapter.getItem(arg2);
                if (item.getGame().path.isEmpty()) {
                    new DownloaderDelegate(
                            listener,
                            arg1.findViewById(R.id.download_progressBar),
                            item.getGame()).startDownload();
                } else {
                    listener.onItemClick(item.getGame());
                }

            });
        } else {
            ListAdapter adapter = listAdapters[position - 1];
            list.setAdapter(adapter);
            list.setOnItemClickListener((arg0, arg1, arg2, arg3) -> {
                RowItem item = (RowItem) adapter.getItem(arg2);
                listener.onItemClick(item.getGame());
            });
        }
        list.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                NLog.i("list", position + ":" + scrollState + "");
                if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                    yOffsets[position] = list.getFirstVisiblePosition();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
            }
        });
        list.setSelection(yOffsets[position]);
        lists[position] = list;
        container.addView(list);
        return list;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    public void setGames(ArrayList<GameDescription> games) {
        for (GalleryAdapter adapter : listAdapters) {
            adapter.setGames(new ArrayList<>(games));
        }
    }


    public int addGames(ArrayList<GameDescription> newGames) {
        int result = 0;
        for (GalleryAdapter adapter : listAdapters) {
            result = adapter.addGames(new ArrayList<>(newGames));
        }
        return result;
    }

    public void setFilter(String filter) {
        for (GalleryAdapter adapter : listAdapters) {
            adapter.setFilter(filter);
        }
    }

    @Override
    public void notifyDataSetChanged() {
        for (int i = 0; i < tabTypes.length; i++) {
            GalleryAdapter adapter = listAdapters[i];
            adapter.notifyDataSetChanged();
            if (lists[i] != null)
                lists[i].setSelection(yOffsets[i]);
        }
        super.notifyDataSetChanged();
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putIntArray(EXTRA_POSITIONS, yOffsets);
    }

    public void onRestoreInstanceState(Bundle inState) {
        if (inState != null) {
            yOffsets = inState.getIntArray(EXTRA_POSITIONS);
            if (yOffsets == null)
                yOffsets = new int[listAdapters.length];
        }
    }

    public interface OnItemClickListener {
        void onItemClick(GameDescription game);
    }

}