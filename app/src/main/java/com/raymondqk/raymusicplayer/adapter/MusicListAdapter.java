package com.raymondqk.raymusicplayer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.raymondqk.raymusicplayer.R;
import com.raymondqk.raymusicplayer.customview.AvatarCircle;

import java.util.ArrayList;

/**
 * Created by 陈其康 raymondchan on 2016/8/4 0004.
 */
public class MusicListAdapter extends BaseAdapter {

    private Context mContext;
    private LayoutInflater mLayoutInflater;
    private ArrayList<Integer> avatars;
    private ArrayList<String> titles;
    private ArrayList<String> artists;

    public MusicListAdapter(Context context, ArrayList<Integer> avatars,ArrayList<String> titles,ArrayList<String> artists) {
        mContext = context;
        mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        this.artists = artists;
        this.avatars = avatars;
        this.titles =titles;
    }

    @Override
    public int getCount() {
        return titles.size();
    }

    @Override
    public Object getItem(int position) {
        return titles.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.list_item,null);
            viewHolder = new ViewHolder();
            viewHolder.avatarCircle = (AvatarCircle) convertView.findViewById(R.id.avatar_list_item);
            viewHolder.artist = (TextView) convertView.findViewById(R.id.tv_artist_list_item);
            viewHolder.title = (TextView) convertView.findViewById(R.id.tv_title_list_item);
            convertView.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.avatarCircle.setImageResource(avatars.get(position));
        viewHolder.title.setText(titles.get(position));
        viewHolder.artist.setText(artists.get(position));
        return convertView;
    }

    class ViewHolder {
        AvatarCircle avatarCircle;
        TextView title;
        TextView artist;
    }
}
