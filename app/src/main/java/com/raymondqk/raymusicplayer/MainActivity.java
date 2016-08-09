package com.raymondqk.raymusicplayer;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.raymondqk.raymusicplayer.customview.AvatarCircle;
import com.raymondqk.raymusicplayer.service.MusicService;
import com.raymondqk.raymusicplayer.widget.MusicWidgetProvider;

/**
 * Created by 陈其康 raymondchan on 2016/8/3 0003.
 * 当前进度：完成主界面布局
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private AvatarCircle mAvatarCircle;
    private boolean isClickAvatar;
    private ImageButton mIb_play_mode;
    private ImageButton mIb_play;
    private ImageButton mIb_preview;
    private ImageButton mIb_next;
    private TextView mTv_duration;

    private MusicService mMusicService;
    private Intent mMusicSeviceIntent;

    MusicService.OnCompletionCallback mOnCompletionCallback = new MusicService.OnCompletionCallback() {
        @Override
        public void OnCompletion() {
            //判断当前播放模式，若不为单曲循环，则播放下一首
            if (mMusicService.getPlay_mode() != MusicService.MODE_LOOP_ONE) {
                playNext();
            }

        }
    };

    MusicService.PlayPreparedCallback mPlayPreparedCallback = new MusicService.PlayPreparedCallback() {
        @Override
        public void onPlayPrepared() {
            mTv_duration.setText(mMusicService.getCurrent_duration());
            mTv_title.setText(mMusicService.title());
            mTv_artist.setText(mMusicService.getArtist());
            mAvatarCircle.setImageResource(mMusicService.getCurrent_Avatar());
            mIb_play.setImageResource(R.drawable.pause);
            updateSeekBar();


        }
    };
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicServiceBinder binder = (MusicService.MusicServiceBinder) service;
            mMusicService = binder.getServiceInstance();
            if (mMusicService != null) {
                Toast.makeText(MainActivity.this, "音乐服务绑定成功", Toast.LENGTH_SHORT).show();
                mMusicService.setCompletionCallback(mOnCompletionCallback);
                mMusicService.setPlayCallback(mPlayPreparedCallback);
            } else {
                Toast.makeText(MainActivity.this, "音乐服务绑定失败", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mMusicService = null;

        }
    };

    private Handler mHandler = new Handler();
    private TextView mTv_position;
    private SeekBar mProgress;
    private TextView mTv_title;
    private TextView mTv_artist;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        getSupportActionBar().setSubtitle("播放界面");
        getSupportActionBar().setIcon(R.drawable.preview_selector);
        mAvatarCircle = (AvatarCircle) findViewById(R.id.avatar_main);
        mAvatarCircle.setOnClickListener(this);
        mIb_play_mode = (ImageButton) findViewById(R.id.ib_play_mode);
        mIb_play = (ImageButton) findViewById(R.id.ib_play);
        mIb_preview = (ImageButton) findViewById(R.id.ib_preview);
        mIb_next = (ImageButton) findViewById(R.id.ib_next);

        mIb_next.setOnClickListener(this);
        mIb_play.setOnClickListener(this);
        mIb_play_mode.setOnClickListener(this);
        mIb_preview.setOnClickListener(this);

        mTv_duration = (TextView) findViewById(R.id.tv_main_time);
        mTv_duration.setText("00:00");
        mTv_position = (TextView) findViewById(R.id.tv_pass_time);
        mTv_position.setText("00:00");

        mTv_title = (TextView) findViewById(R.id.tv_main_title);
        mTv_artist = (TextView) findViewById(R.id.tv_main_artist);

        mProgress = (SeekBar) findViewById(R.id.progressbar);
        mProgress.setProgress(0);
        mProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!mMusicService.isFirstPlay()) {
                    if (fromUser) { //必须这个判断，是否为用户拉动导致的进度变更，否则会造成播放卡顿现象
                        float percent = (float) progress / (float) mProgress.getMax();
                        mMusicService.setSeekTo(percent);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "请先点击播放", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (!mMusicService.isFirstPlay()) {
                    mMusicService.stopMediaPlayer();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!mMusicService.isFirstPlay()) {
                    mMusicService.continueMediaPlayer();
                } else {
                    mProgress.setProgress(0);
                }
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMusicService != null) {
            checkPlaying();
            mAvatarCircle.setImageResource(mMusicService.getCurrent_Avatar());
        }


    }

    private void checkPlaying() {
        if (mMusicService.getPlay_state() == MusicService.STATE_PLAYING) {
            mIb_play.setImageResource(R.drawable.pause);
            updateSeekBar();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        serviceInit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMusicService = null;
        unbindService(mServiceConnection);
        stopService(mMusicSeviceIntent);
        Log.i("Test", "MainActivity onDestroy");
    }

    private void serviceInit() {
        mMusicSeviceIntent = new Intent();
        mMusicSeviceIntent.setClass(MainActivity.this, MusicService.class);
        bindService(mMusicSeviceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_list:
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, MusicListActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.avatar_main:
                if (isClickAvatar) {
                    mAvatarCircle.setImageResource(R.drawable.avatar_joyce2);
                    isClickAvatar = !isClickAvatar;
                } else {
                    mAvatarCircle.setImageResource(R.drawable.avatar_joyce);
                    isClickAvatar = !isClickAvatar;
                }
                break;
            case R.id.ib_play_mode:
                switchPlayMode();
                break;
            case R.id.ib_play:
                playCurrent();
                break;
            case R.id.ib_next:
                playNext();
                break;
            case R.id.ib_preview:
                playPreview();
                break;
        }
    }

    private void switchPlayMode() {
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
    }

    private void playCurrent() {
        if (mMusicService != null) {
            int play_state = mMusicService.setPlay_state();
            if (play_state == MusicService.STATE_STOP) {
                mIb_play.setImageResource(R.drawable.play);
            } else if (play_state == MusicService.STATE_PLAYING) {
                mIb_play.setImageResource(R.drawable.pause);
            }
        }
    }

    private void playPreview() {
        if (!mMusicService.isFirstPlay()) {
            mMusicService.previewMusic();
        }
    }

    private void playNext() {
        if (!mMusicService.isFirstPlay()) {
            mMusicService.nextMusic();
        }
    }


    public void updateSeekBar() {
        if (mMusicService != null) {

            if (mMusicService.getPlay_state() == MusicService.STATE_PLAYING) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mTv_position.setText(mMusicService.getCurrent_pisition());
                            mProgress.setProgress((int) (mMusicService.getProgressPercent() * mProgress.getMax()));
                            updateSeekBar();
                        } catch (Exception e) {
                            Log.e(MusicWidgetProvider.TEST, "Activity已被关闭");
                        }

                    }
                }, 1000);
            }
        }
    }
}


