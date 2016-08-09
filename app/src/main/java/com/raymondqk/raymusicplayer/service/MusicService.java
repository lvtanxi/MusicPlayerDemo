package com.raymondqk.raymusicplayer.service;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.raymondqk.raymusicplayer.R;
import com.raymondqk.raymusicplayer.widget.MusicWidgetProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by 陈其康 raymondchan on 2016/8/4 0004.
 */
public class MusicService extends Service {

    /**
     * 循环模式标志
     */
    public static final int MODE_LOOP_ALL = 0;
    public static final int MODE_LOOP_ONE = 1;
    public static final int MODE_RADOM = 2;

    /**
     * 播放状态标志
     */
    public static final int STATE_PLAYING = 0;
    //    public static final int STATE_PAUSE = 1;
    public static final int STATE_STOP = 2;

    private MusicServiceReceiver mMusicServiceReceiver;

    //是否点了喜欢
    private boolean isFavor = false;

    public boolean isFavor() {
        return isFavor;
    }

    public void setFavor(boolean favor) {
        isFavor = favor;
    }

    //循环模式变量
    private int play_mode = MODE_LOOP_ALL;
    //播放状态变量
    private int play_state = STATE_STOP;

    //判断是否为刚打开播放器，第一次播放
    private boolean fisrtPlay = true;

    public boolean isFirstPlay() {
        return fisrtPlay;
    }

    //专辑头像资源id变量
    private int current_Avatar = R.drawable.album_default;

    public int getCurrent_Avatar() {
        return current_Avatar;
    }

    //当前音乐长度
    private int current_duration;

    MusicServiceBinder mBinder = new MusicServiceBinder();
    //媒体播放类
    private MediaPlayer mMediaPlayer;
    //存储头像资源文件id的list
    private List<Integer> mAvatarResIdList = new ArrayList<>();
    //存储音乐文件uri的list
    private List<Uri> mMusicUriList = new ArrayList<>();
    //记录当前播放歌曲索引
    private int currentIndex;

    private List<String> mTitleList = new ArrayList<>();
    private List<String> mArtistList = new ArrayList<>();

    public List<String> getTitleList() {
        return mTitleList;
    }

    public List<Integer> getAvatarResIdList() {
        return mAvatarResIdList;
    }

    public List<String> getArtistList() {
        return mArtistList;
    }

    /**
     * 给mediaplayer设置要从哪个位置播放
     *
     * @param percent
     */
    public void setSeekTo(float percent) {
        mMediaPlayer.seekTo((int) (current_duration * percent));
    }

    /**
     * 响应音乐列表被点击
     *
     * @param position
     */
    public void onListItemClick(int position) {
        if (isFirstPlay()) {
            fisrtPlay = false;
        }
        currentIndex = position;
        current_Avatar = mAvatarResIdList.get(currentIndex % mAvatarResIdList.size());
        play_state = STATE_PLAYING;
        playMusic();
    }

    /**
     * 当实现了该类被销毁后，也从接口列表中移除
     * @param playPreparedCallback
     */
    public void removePlayCallback(PlayPreparedCallback playPreparedCallback) {
        mPlayPreparedCallbackHashSet.remove(playPreparedCallback);
    }


    /**
     * 接口：当前文件播放完成时调用，在实现的类里面回调
     */
    public interface OnCompletionCallback {
        void OnCompletion();
    }

    /**
     * 接口：在MediaPlayer.setDataSource()装载音乐文件后，start()前调用，在实现的类里面回调，完成相关准备工作
     */
    public interface PlayPreparedCallback {
        void onPlayPrepared();
    }


    private OnCompletionCallback mCompletionCallback;

    public void setCompletionCallback(OnCompletionCallback completionCallback) {
        mCompletionCallback = completionCallback;
    }

    //使用集合来存储接口引用，以达到可扩展的目的，无论多少个类实现了该接口，都可以通过集合存储，不需要为每个类都提供一个接口变量 ==== java框架思想
    private HashSet<PlayPreparedCallback> mPlayPreparedCallbackHashSet = new HashSet<PlayPreparedCallback>();

    /**
     * 返回接口实例引用
     *
     * @param playPreparedCallback
     */
    public void setPlayCallback(PlayPreparedCallback playPreparedCallback) {
        mPlayPreparedCallbackHashSet.add(playPreparedCallback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * 在服务解绑或stop时调用
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        //记得释放资源
        mMediaPlayer.stop();
        mMediaPlayer.release();
        unregisterReceiver(mMusicServiceReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("Test", "onStartCommand");
        return START_NOT_STICKY;
    }

    /**
     * Service被绑定或启动时执行，进行相关初始化工作
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate() {
        super.onCreate();
        //初始化音乐文件列表
        initMusicFiles();
        //初始化MediaPlayer
        initMediaPlayer();
        //注册BroadcastReceiver，这是用来接收来自Widget的广播的
        registBroadcastReceiverForWidget();
    }

    /**
     * 注册BroadcastReceiver，这是用来接收来自Widget的广播的
     */
    private void registBroadcastReceiverForWidget() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MusicWidgetProvider.WIDGET_PLAY);
        intentFilter.addAction(MusicWidgetProvider.WIDGET_NEXT);
        intentFilter.addAction(MusicWidgetProvider.WIDGET_PREVIEW);
        mMusicServiceReceiver = new MusicServiceReceiver();
        registerReceiver(mMusicServiceReceiver, intentFilter);
        Log.i("Test", "registReceiver");
    }

    /**
     * 初始化MediaPlayer
     */
    private void initMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        //设置播放结束的监听事件
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                //判断当前播放模式是否为单曲循环
                if (play_mode != MODE_LOOP_ONE) {
                    //如果非单曲循环，则进行播放下一首的操作
                    mCompletionCallback.OnCompletion();//这是一个回调，让Activity更新UI等操作
                } else {
                    //若为单曲循环，则直接开始播放，不需要重新setDataSource
                    mMediaPlayer.start();
                }
            }
        });
    }

    /**
     * 准备音乐文件
     * 这里面日后学习了扫描sdcard和获取系统媒体资源之后，重新设计
     * 目前是使用raw里面的文件
     */
    private void initMusicFiles() {
        //android 获取raw 绝对路径 -- raw资源转uri
        for (int i = 0; i < 10; i++) {     // 通过循环重复加载uri到list里面，模拟有多首歌曲的情况
            //将raw资源转化为uri
            Uri uri = Uri.parse("android.resource://com.raymondqk.raymusicplayer/" + R.raw.missyou);
            //加入到musicList
            mMusicUriList.add(uri);
            //把头像资源加入到头像的list里面 与music同步加入，根据index就可以将music和头像对应起来，这是目前的暂缓之策
            // 日后应当根据music的title找到对应的头像图片
            mAvatarResIdList.add(R.drawable.avatar_joyce);

            // TODO: 2016/8/4 0004 因为MediaPlayer似乎无法读取文件里面的歌曲信息，如标题和艺术家，所以目前这样处理着
            mTitleList.add("好想你");
            mArtistList.add("Joyce");

            //这是第二首
            uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.stillalive);
            mMusicUriList.add(uri);
            mAvatarResIdList.add(R.drawable.avatar_bigbang);
            mTitleList.add("STILL ALIVE");
            mArtistList.add("BIGBANG");
        }
    }


    /**
     * 设置当前播放模式：单曲、循环
     *
     * @param play_mode 循环模式 : 可选 MODE_LOOP_ALL/MODE_LOOP_ONE
     */
    public void setPlay_mode(int play_mode) {
        this.play_mode = play_mode;
    }

    /**
     * 设置当前播放状态：播放中、暂停,同时进行播放操作，是播放还是暂停，暂时未添加停止项
     * 外部只需要通知播放暂停按键被按下了即可，剩下操作留给service来判断
     */
    public int setPlay_state() {

        /*
        当前是播放状态，则进行暂停操作
         */

        if (play_state == STATE_PLAYING) {
            play_state = STATE_STOP;
            mMediaPlayer.pause();
        } else if (play_state == STATE_STOP) {
            play_state = STATE_PLAYING;
            if (fisrtPlay) {
                 /*
                因为刚打开播放器，未添加音乐文件给MediaPlayer，所以做这么一个判断，以防出bug
                若第一次播放，执行播放函数playMusic()，设置setDataSource等操作
                 */
                fisrtPlay = false;
                playMusic();
            } else {
                 /*
                若不是第一次播放，则代表是播放过程中暂停了，现在继续即可。
                 */
                mMediaPlayer.start();
            }
        }
        return play_state;
    }


    /**
     * 获取当前循环模式
     *
     * @return
     */
    public int getPlay_mode() {
        return play_mode;
    }

    /**
     * 获取当前播放器状态：暂停、播放
     *
     * @return
     */
    public int getPlay_state() {
        return play_state;
    }

    /**
     * 获取当前歌曲名
     *
     * @return
     */
    public String title() {
        return mTitleList.get(currentIndex % mTitleList.size());
    }

    /**
     * 获取当前歌手
     *
     * @return
     */
    public String getArtist() {
        return mArtistList.get(currentIndex % mArtistList.size());
    }


    /**
     * 播放音乐
     */
    public void playMusic() {
        Log.i("Test", "play");
        //重置MediaPlayer，确保能顺利载入datasource以及播放，这里的策略是每次要播放新歌曲时，即第一次播放，切歌时，都进行一次reset
        mMediaPlayer.reset();
        try {//prepare()会抛出异常
            //设置数据源
            mMediaPlayer.setDataSource(MusicService.this, mMusicUriList.get(currentIndex % mMusicUriList.size()));
            mMediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Test", "无法播放音乐");
            mMediaPlayer.reset();
        }
        //在数据源装载好，MediaPlayer准备就绪之后，播放之前，做些定制化的准备工作，如通知Activity更新UI
        beforePlay();
        //播放音乐
        mMediaPlayer.start();
    }

    /**
     * 播放音乐前的准备工作
     */
    private void beforePlay() {
        //获取当前音乐文件的总时长
        current_duration = mMediaPlayer.getDuration();
        //获得当前音乐文件对应的头像专辑图的资源id
        current_Avatar = mAvatarResIdList.get(currentIndex % mAvatarResIdList.size());
        //我们定义了一个Hashset，用来存放每个类中接口的实例，通过遍历这个集合，就可以做到让每个类响应回调
        for (PlayPreparedCallback playPreparedCallback : mPlayPreparedCallbackHashSet) {
            //接口回调，在实现了接口的类中实现
            playPreparedCallback.onPlayPrepared();
        }

    }

    /**
     * 上一首
     */
    public void nextMusic() {
        if (!fisrtPlay) {
            //歌曲索引加一，指向下一首歌曲
            currentIndex++;
            //设置完索引，就让播放函数载入当前索引对应的音乐文件
            playMusic();
        }
    }

    /**
     * 下一首
     */
    public void previewMusic() {
        if (!fisrtPlay) {
            if (currentIndex > 0) {
                //歌曲索引减一，指向上一首歌曲
                currentIndex--;
            } else {
                //上面的代码会导致索引出现负值，作如下处理
                //当前索引为0，即第一首，负值为队尾的索引值，即最后一首
                currentIndex = mMusicUriList.size() - 1;
            }
            //设置完索引，就让播放函数载入当前索引对应的音乐文件
            playMusic();
        }
    }

    /**
     * 暂停当前播放
     */
    public void stopMediaPlayer() {
        mMediaPlayer.pause();
    }

    /**
     * 继续当前播放
     */
    public void continueMediaPlayer() {
        mMediaPlayer.start();
    }

    /**
     * 获得当前音乐长度的字符串 格式 02:25
     *
     * @return
     */
    public String getCurrent_duration() {
        return getTimeStrByMils(current_duration);

    }


    /**
     * 获得当前播放进度的字符串 格式 02:25
     *
     * @return
     */
    public String getCurrent_pisition() {
        return getTimeStrByMils(mMediaPlayer.getCurrentPosition());
    }


    /**
     * 毫秒转 02：25 格式的字符串
     *
     * @param mils
     * @return
     */
    private String getTimeStrByMils(int mils) {
        int seconds = mils / 1000;
        int min = seconds / 60;
        int sec = seconds % 60;
        String min_str;
        String sec_str;
        if (min < 10) {
            min_str = "0" + min;
        } else {
            min_str = min + "";
        }
        if (sec < 10) {
            sec_str = "0" + sec;
        } else {
            sec_str = sec + "";
        }
        return min_str + ":" + sec_str;
    }

    /**
     * 获得当前进度的百分比
     *
     * @return
     */
    public float getProgressPercent() {

        return (float) mMediaPlayer.getCurrentPosition() / (float) mMediaPlayer.getDuration();
    }

    /**
     * 创建一个Binder类，用于绑定服务时传给Activity
     */
    public class MusicServiceBinder extends Binder {
        //返回当前服务的实例引用
        public MusicService getServiceInstance() {
            return MusicService.this;
        }
    }

    /**
     * 用与接收来自Widget的Broadcast
     */
    class MusicServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent != null) {
                if (TextUtils.equals(intent.getAction(), MusicWidgetProvider.WIDGET_PLAY)) {
                    setPlay_state();
                    Log.i("TEST", "service-onReceive-PLAY");
                } else if (TextUtils.equals(intent.getAction(), MusicWidgetProvider.WIDGET_NEXT)) {
                    nextMusic();
                    Log.i("TEST", "service-onReceive-next");
                } else if (TextUtils.equals(intent.getAction(), MusicWidgetProvider.WIDGET_PREVIEW)) {
                    previewMusic();
                    Log.i("TEST", "service-onReceive-PRE");
                }
            }

        }
    }
}

