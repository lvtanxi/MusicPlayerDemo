package com.raymondqk.raymusicplayer;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.raymondqk.raymusicplayer.adapter.MusicListAdapter;
import com.raymondqk.raymusicplayer.service.MusicService;
import com.raymondqk.raymusicplayer.widget.MusicWidgetProvider;


/**
 * Created by 陈其康 raymondchan on 2016/8/3 0003.
 */
public class MusicListActivity extends AppCompatActivity implements View.OnClickListener {

    private ListView mListView;

    private MusicService mMusicService;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicServiceBinder binder = (MusicService.MusicServiceBinder) service;
            mMusicService = binder.getServiceInstance();
            if (mMusicService != null) {
                Toast.makeText(MusicListActivity.this, "服务绑定成功", Toast.LENGTH_SHORT).show();
                mAdapter = new MusicListAdapter(MusicListActivity.this, mMusicService.getAvatarResIdList(),
                        mMusicService.getTitleList(), mMusicService.getArtistList());
                mListView.setAdapter(mAdapter);
                mMusicService.setPlayCallback(mPlayPreparedCallback);
                checkPlaying();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mMusicService = null;
        }
    };
    private MusicListAdapter mAdapter;

    private MusicService.PlayPreparedCallback mPlayPreparedCallback = new MusicService.PlayPreparedCallback() {
        @Override
        public void onPlayPrepared() {
            mIb_play.setImageResource(R.drawable.pause);
            updateSeekBar();

        }
    };
    private ImageButton mIb_play;
    private ImageButton mIb_next;
    private ImageButton mIb_preview;
    private ImageButton mIb_play_mode;
    private ImageButton mIb_favor;
    private SeekBar mProgressBar;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_musiclist);


        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
        initView();

    }

    private void checkPlaying() {
        if (mMusicService.getPlay_state() == MusicService.STATE_PLAYING) {
            mIb_play.setImageResource(R.drawable.pause);
            updateSeekBar();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onStop() {
        super.onStop();
        mMusicService = null;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    private void initView() {
        //去掉默认导航栏

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_musiclist);
        //设置导航图标 左上角
        toolbar.setNavigationIcon(R.drawable.nav_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
                //                finish();

            }
        });

        toolbar.setTitle(R.string.app_name);
        toolbar.setSubtitle(R.string.menu_list);
        toolbar.inflateMenu(R.menu.list_menu);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_item_search:
                        Toast.makeText(MusicListActivity.this, "search", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.menu_item_setting:
                        Toast.makeText(MusicListActivity.this, "setting", Toast.LENGTH_SHORT).show();
                        break;
                }
                return true;
            }
        });

        mListView = (ListView) findViewById(R.id.lv_music);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mMusicService != null) {
                    mMusicService.onListItemClick(position);
                }
            }
        });

        mIb_play = (ImageButton) findViewById(R.id.ib_play);
        mIb_next = (ImageButton) findViewById(R.id.ib_next);
        mIb_preview = (ImageButton) findViewById(R.id.ib_preview);
        mIb_play_mode = (ImageButton) findViewById(R.id.ib_play_mode);
        mIb_favor = (ImageButton) findViewById(R.id.ib_favor);

        mIb_favor.setOnClickListener(this);
        mIb_next.setOnClickListener(this);
        mIb_preview.setOnClickListener(this);
        mIb_play.setOnClickListener(this);
        mIb_play_mode.setOnClickListener(this);

        mProgressBar = (SeekBar) findViewById(R.id.progressbar);
        mProgressBar.setProgress(0);
        mProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mMusicService != null) {
                    if (!mMusicService.isFirstPlay()) {
                        if (fromUser) { //必须这个判断，是否为用户拉动导致的进度变更，否则会造成播放卡顿现象
                            float percent = (float) progress / (float) mProgressBar.getMax();
                            mMusicService.setSeekTo(percent);
                        }
                    } else {
                        Toast.makeText(MusicListActivity.this, "请先点击播放", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (mMusicService != null) {
                    if (!mMusicService.isFirstPlay()) {
                        mMusicService.stopMediaPlayer();
                    }
                }

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mMusicService != null) {
                    if (!mMusicService.isFirstPlay()) {
                        mMusicService.continueMediaPlayer();
                    } else {
                        mProgressBar.setProgress(0);
                    }
                }

            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        mMusicService.removePlayCallback(mPlayPreparedCallback);
        mMusicService = null;
        mPlayPreparedCallback = null;
        unbindService(mServiceConnection);
        super.onBackPressed();
        //        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ib_play_mode:
                if (mMusicService != null) {
                    if (mMusicService.getPlay_mode() == MusicService.MODE_LOOP_ALL) {
                        mMusicService.setPlay_mode(MusicService.MODE_LOOP_ONE);
                        mIb_play_mode.setImageResource(R.drawable.loop_one);
                        // TODO: 2016/8/4 0004 设置播放模式

                    } else if (mMusicService.getPlay_mode() == MusicService.MODE_LOOP_ONE) {
                        mMusicService.setPlay_mode(MusicService.MODE_RADOM);
                        mIb_play_mode.setImageResource(R.drawable.radom);
                        // TODO: 2016/8/4 0004 设置播放模式
                    } else if (mMusicService.getPlay_mode() == MusicService.MODE_RADOM) {
                        mMusicService.setPlay_mode(MusicService.MODE_LOOP_ALL);
                        mIb_play_mode.setImageResource(R.drawable.loop_all);
                    }
                }
                break;
            case R.id.ib_play:
                if (mMusicService != null) {
                    int play_state = mMusicService.setPlay_state();
                    if (play_state == MusicService.STATE_STOP) {
                        mIb_play.setImageResource(R.drawable.play);
                    } else if (play_state == MusicService.STATE_PLAYING) {
                        mIb_play.setImageResource(R.drawable.pause);
                    }
                }
                break;
            case R.id.ib_next:
                if (!mMusicService.isFirstPlay()) {
                    mMusicService.nextMusic();
                }
                break;
            case R.id.ib_preview:
                if (!mMusicService.isFirstPlay()) {
                    mMusicService.previewMusic();
                }
                break;
            case R.id.ib_favor:
                if (mMusicService.isFavor()) {
                    mIb_favor.setImageResource(R.drawable.favor_default);
                    mMusicService.setFavor(false);
                } else {
                    mMusicService.setFavor(true);
                    mIb_favor.setImageResource(R.drawable.favor_pressed);
                }
                break;

        }
    }

    public void updateSeekBar() {
        if (mMusicService == null)
            return;
        if (mMusicService.getPlay_state() == MusicService.STATE_PLAYING) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        mProgressBar.setProgress((int) (mMusicService.getProgressPercent() * mProgressBar.getMax()));
                        Log.i(MusicWidgetProvider.TEST, "ActivityList updateProgress");
                        updateSeekBar();
                    } catch (Exception e) {
                        Log.e(MusicWidgetProvider.TEST, "ActivityList已被关闭");
                    }


                }
            }, 1000);
        }
    }
}
