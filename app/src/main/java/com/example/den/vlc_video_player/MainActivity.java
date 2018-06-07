package com.example.den.vlc_video_player;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

public class MainActivity extends AppCompatActivity implements IVLCVout.Callback {
    private List<String> list;
    private List<String> listName;
    private TextView txt_ct, txt_td, txt_title;
    private SeekBar seekBar;
    private Handler threadHandler = new Handler();
    private int timeDuration = 0;
    private int curPosition;
    //    private Resources  res = this.getResources();//доступ к ресерсам;
    private static final int REQUEST_PERMITIONS = 1100;
    private ImageButton btn_play;
    private ImageButton btn_pause;
    private ImageButton btn_fwd;
    private ImageButton btn_rev;
    private ImageButton btn_next;
    private ImageButton btn_prev;
    private ImageButton btn_settings;
    private ImageButton btn_back;
    private int curTrackIndex;
    private boolean flagStartPlay = true;
    private boolean flagSavedInstanceState = false;
    private Runnable hideControls;
    private ControlsMode controlsState;
    private LinearLayout root;
    private LinearLayout top_controls;
    private LinearLayout middle_panel;
    private LinearLayout unlock_panel;
    private LinearLayout volume_slider_container;
    private LinearLayout brightness_slider_container;
    private View decorView;
    private View view;
    private int immersiveOptions;
    // size of the video
    private int mVideoHeight;
    private int mVideoWidth;
    private final static int VideoSizeChanged = -1;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private LibVLC mLibVLC;
    private org.videolan.libvlc.MediaPlayer mMediaPlayer;
    private Surface mSurface;
    private boolean flagOnTouchEvent = false;
    private float xTouch;
    private float yTouch;
    ProgressBar volume_slider;
    ProgressBar brightness_slider;
    AudioManager audioManager;
    int maxVolume;
    int currentVolume;
    int MULTIPLICITY_OF_VOLUME_TO_PROGRESSBAR = 10;
    double MULTIPLICITY_OF_BRIGHTNESS_TO_PROGRESSBAR = 100.0;
    WindowManager.LayoutParams layout;
    double currentBrightness;
    double saveBrightness;
    private String APP_PREFERENCES = "appSettings";
    private SharedPreferences mSettings;
    int maxWidthPix;
    int maxHeightPix;
    String name;
    String path;

    public enum ControlsMode {
        LOCK, FULLCONTORLS
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = getLayoutInflater().inflate(R.layout.activity_main, null);
        setContentView(view);

        curTrackIndex = Objects.requireNonNull(getIntent().getExtras()).getInt("position");
        list = getIntent().getExtras().getStringArrayList("listPath");
        listName = getIntent().getExtras().getStringArrayList("listName");
        name = getIntent().getStringExtra("name");
        path = getIntent().getStringExtra("path");

        layout = getWindow().getAttributes();
        currentBrightness = setCurrentBrightness();
        layout.screenBrightness = (float) currentBrightness;
        getWindow().setAttributes(layout);
        start();
    }//onCreate


    private void start() {
        instalVidget();

        curTrackIndex = 0;
        installVideo();

        if (flagSavedInstanceState) {
            mMediaPlayer.setTime(curPosition);
        }
        initializationButtons();
    }


    private void installVideo() {
        if (list != null) {
            if (list.size() != 0) {
                releasePlayer();

                hideControls = new Runnable() {
                    @Override
                    public void run() {
                        hideAllControls();
                    }
                };

                try {
                    ArrayList<String> options = new ArrayList<String>();
                    options.add("-vvv"); // verbosity
                    options.add("--extraintf=logger");
                    options.add("--verbose=0");
                    options.add("--log-verbose=0");
                    options.add("--rtsp-tcp");
                    options.add("--aout=opensles");
                    options.add("--audio-time-stretch"); // time stretching
                    mLibVLC = new LibVLC(getApplicationContext(), options);
                    // Create media player
                    mMediaPlayer = new MediaPlayer(mLibVLC);
                    mMediaPlayer.setEventListener(mPlayerListener);
                    //set videoSource
//                    String videoSource = list.get(curTrackIndex);
                    Media m = new Media(mLibVLC, path);
                    mMediaPlayer.setMedia(m);
                    // Set up video output
                    mSurfaceView = findViewById(R.id.surface);
                    mSurfaceHolder = mSurfaceView.getHolder();
                    mSurface = mSurfaceHolder.getSurface();
                    final IVLCVout vout = mMediaPlayer.getVLCVout();
                    vout.setVideoSurface(mSurface, mSurfaceHolder);
                    //vout.setSubtitlesView(mSurfaceSubtitles);
                    vout.attachViews();
                    vout.addCallback(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
//                if (flagStartPlay) {//если играл
//                    //threadHandler.post(updateSeekBar);
//                }
                threadHandler.postDelayed(hideControls, 3500);
                controlsState = ControlsMode.FULLCONTORLS;
            }
        }
    }

    private void initializationButtons() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                curPosition = seekBar.getProgress();
                changeCurPosition();
            }
        });
        btn_back.setOnClickListener(
                view -> {
                    if (list != null) {
                        mMediaPlayer.stop();
                    }
                    finish();
                });
        btn_play.setOnClickListener(
                view -> {
                    if (!mMediaPlayer.isPlaying()) {
                        flagStartPlay = true;
                        changePlayToPause(true);
                        mMediaPlayer.play();
                        mMediaPlayer.setTime(curPosition);
                    }
                });
        btn_pause.setOnClickListener(
                view -> {
                    if (mMediaPlayer.isPlaying()) {
                        flagStartPlay = false;
                        changePlayToPause(false);
                        mMediaPlayer.pause();
                    }
                });
        btn_fwd.setOnClickListener(
                view -> {
                    if ((curPosition + 5000) < mMediaPlayer.getLength()) {
                        curPosition += 5000;
                        changeCurPosition();
                    }
                });
        btn_rev.setOnClickListener(
                view -> {
                    if ((curPosition - 5000) > 0) {
                        curPosition -= 5000;
                        changeCurPosition();
                    }
                });
        btn_prev.setOnClickListener(
                view -> {
                    if (curTrackIndex != 0) {
                        curTrackIndex -= 1;
                        curPosition = 0;
                        installVideo();
                    } else installVideo();
                    changePlayToPause(true);
                    mMediaPlayer.play();
                });
        btn_next.setOnClickListener(
                view -> {
                    if (curTrackIndex != list.size() - 1) {
                        curTrackIndex += 1;
                        curPosition = 0;
                        installVideo();
                    } else installVideo();
                    changePlayToPause(true);
                    mMediaPlayer.play();
                });
        btn_settings.setOnClickListener(
                view -> {
                    if (mMediaPlayer.isPlaying()) {
                        changePlayToPause(false);
                        mMediaPlayer.pause();
                        flagStartPlay = false;
                        curPosition = (int) mMediaPlayer.getTime();
                    }
                    setResult(RESULT_OK);
                    finish();
                });
    }//initializationButtons


    private void changePlayToPause(boolean flag) {
        btn_pause.setVisibility(flag ? View.VISIBLE : View.GONE);
        btn_play.setVisibility(flag ? View.GONE : View.VISIBLE);
    }//changePausePlay


    //==================================================================================================================
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setSize(mVideoWidth, mVideoHeight);
    }


    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return MainActivity.this;
    }
    //==================================================================================================================


    @Override
    public void onNewLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        Message msg = Message.obtain(mHandler, VideoSizeChanged, width, height);
        msg.sendToTarget();
    }


    @Override
    public void onSurfacesCreated(IVLCVout vlcVout) {
        setTimeWidget();
        if (flagStartPlay) {//если играл
            mMediaPlayer.play(); // начинаем воспроизведение автоматически
            mMediaPlayer.setTime(curPosition);//устанавливаем с какого времени начать воспроизведение
        } else {//был на паузе
            changePlayToPause(false);
        }
    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {
    }

    @Override
    public void onHardwareAccelerationError(IVLCVout vlcVout) {
    }

    //=================================================================================================
    private MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this);
    private Handler mHandler = new MyPlayerListener(this);

    private static class MyPlayerListener extends Handler implements MediaPlayer.EventListener {
        private WeakReference<MainActivity> mOwner;

        public MyPlayerListener(MainActivity owner) {
            mOwner = new WeakReference<MainActivity>(owner);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity player = mOwner.get();

            // SamplePlayer events
            if (msg.what == VideoSizeChanged) {
                player.setSize(msg.arg1, msg.arg2);
            }
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            MainActivity player = mOwner.get();

            switch (event.type) {
                case MediaPlayer.Event.EndReached:
                    player.changePlayToPause(false);
                    player.mMediaPlayer.stop();
                    player.threadHandler.removeCallbacks(player.updateSeekBar);
                    player.curPosition = 0;
                    player.setTimeWidget();
                    break;
                case MediaPlayer.Event.Playing:
                    player.timeDuration = (int) player.mMediaPlayer.getLength();
                    player.setTimeWidget();
                    player.threadHandler.post(player.updateSeekBar);
                    player.changePlayToPause(true);
                    break;
                case MediaPlayer.Event.Paused:
                    player.threadHandler.removeCallbacks(player.updateSeekBar);
                    player.changePlayToPause(false);
                    break;
                case MediaPlayer.Event.Stopped:
                    player.threadHandler.removeCallbacks(player.updateSeekBar);
                    break;
                default:
                    break;
            }
        }
    }

    // ==============================================================================================
    private void releasePlayer() {
        if (mLibVLC == null)
            return;
        mMediaPlayer.stop();
        mSurfaceView = null;
        mSurfaceHolder = null;
        mSurface = null;
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();
        mLibVLC.release();
        mLibVLC = null;
        threadHandler.removeCallbacks(updateSeekBar);
        mVideoWidth = 0;
        mVideoHeight = 0;
    }


    public void selectVideo(int position) {
        flagStartPlay = true;
        curTrackIndex = position;
        curPosition = 0;
        installVideo();
        changePlayToPause(true);
        mMediaPlayer.play();
    }

    public void setTimeWidget() {
        if (timeDuration > 0) {
            String time = millisecondsToString(timeDuration);
            txt_td.setText(time);
            seekBar.setMax(timeDuration);
            txt_title.setText(listName.get(curTrackIndex));
            txt_ct.setText(millisecondsToString(curPosition));
            seekBar.setProgress(curPosition);
        }
    }

    private void changeCurPosition() {
        mMediaPlayer.setTime(curPosition);
        txt_ct.setText(millisecondsToString(curPosition));
        seekBar.setProgress(curPosition);
    }

    private Runnable updateSeekBar = new Runnable() {
        public void run() {
            txt_ct.setText(millisecondsToString(curPosition));
            seekBar.setProgress(curPosition);
            curPosition += 200;
            threadHandler.postDelayed(this, 200);
        }
    };

    private String millisecondsToString(int milliseconds) {
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds));
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds));
        if (TimeUnit.MILLISECONDS.toHours(timeDuration) == 0) {
            return String.format("%02d:%02d", minutes, seconds);
        } else return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void instalVidget() {
        txt_title = findViewById(R.id.txt_title);
        btn_back = findViewById(R.id.btn_back);
        btn_play = findViewById(R.id.btn_play);
        btn_pause = findViewById(R.id.btn_pause);
        btn_fwd = findViewById(R.id.btn_fwd);
        btn_rev = findViewById(R.id.btn_rev);
        btn_prev = findViewById(R.id.btn_prev);
        btn_next = findViewById(R.id.btn_next);
        btn_settings = findViewById(R.id.btn_settings);
        txt_ct = findViewById(R.id.txt_currentTime);
        txt_td = findViewById(R.id.txt_totalDuration);
        seekBar = findViewById(R.id.seekbar);

        root = findViewById(R.id.root);
        root.setVisibility(View.VISIBLE);
        volume_slider_container = findViewById(R.id.volume_slider_container);
        brightness_slider_container = findViewById(R.id.brightness_slider_container);
        LinearLayout seekbar_time = findViewById(R.id.seekbar_time);
        seekbar_time.setVisibility(View.VISIBLE);
        LinearLayout top = findViewById(R.id.top);
        top.setPadding(0, getStatusBarHeight(), 0, 0);//устанавливаем отступ на высоту StatusBar
        if (hasNavBar(this)){//если присутствует NavBar
            if ( isSystemBarOnBottom(this)){// если NavBar снизу
                  Log.d("fff", "ddd");

            }else {
                int rotation =  this.getWindowManager().getDefaultDisplay().getRotation();
                int reqOr = this.getRequestedOrientation();

                String aVerReleaseStr = Build.VERSION.RELEASE;
                int dotInd = aVerReleaseStr.indexOf(".");
                if (dotInd >= 0) {
                    aVerReleaseStr = aVerReleaseStr.replaceAll("\\.", "");
                    aVerReleaseStr = new StringBuffer(aVerReleaseStr).insert(dotInd, ".").toString();
                }

                float androidVer = Float.parseFloat(aVerReleaseStr);
                if (rotation == 3 && reqOr == 6 && androidVer >= 7.1) {
                    // buttons are on the left side.
                    Log.d("fff", "ddd");
                }else {
                    Log.d("fff", "ddd");
                }
            }

        }
        top.setVisibility(View.VISIBLE);
        LinearLayout bottom_controls = findViewById(R.id.controls);
        bottom_controls.setVisibility(View.VISIBLE);

        immersiveOptions = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN//появляется фон вверху
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE//не понятно
                | View.SYSTEM_UI_FLAG_LOW_PROFILE//не понятно
                | View.SYSTEM_UI_FLAG_FULLSCREEN//время и батарея
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION//нижние кнопки
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION ////нижние кнопки не прячутся за кнопками
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY//не понятно
        );
        decorView = getWindow().getDecorView();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//устанавливаем флаг на запрет отключения экрана
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);//поворот при выключенном разрешениии
    }//instalVidget


    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0)
            result = getResources().getDimensionPixelSize(resourceId);
        return result;
    }

    public static boolean hasNavBar(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point realPoint = new Point();
        Display display = wm.getDefaultDisplay();
        display.getRealSize(realPoint);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels + metrics.widthPixels != realPoint.y + realPoint.x;
    }

    public static boolean isSystemBarOnBottom(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point realPoint = new Point();
        Display display = wm.getDefaultDisplay();
        display.getRealSize(realPoint);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        Configuration cfg = context.getResources().getConfiguration();
        boolean canMove = (metrics.widthPixels != metrics.heightPixels &&
                cfg.smallestScreenWidthDp < 600);

        return (!canMove || metrics.widthPixels < metrics.heightPixels);
    }


    private void setSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        if (mVideoWidth * mVideoHeight <= 1)
            return;

        // get screen size
        int w = decorView.getWidth();
        int h = decorView.getHeight();

        // getWindow().getDecorView() doesn't always take orientation into
        // account, we have to correct the values
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (w > h && isPortrait || w < h && !isPortrait) {
            int i = w;
            w = h;
            h = i;
        }

        float videoAR = (float) mVideoWidth / (float) mVideoHeight;
        float screenAR = (float) w / (float) h;

        if (screenAR < videoAR)
            h = (int) (w / videoAR);
        else
            w = (int) (h * videoAR);

        // force surface buffer size
        mSurfaceHolder.setFixedSize(mVideoWidth, mVideoHeight);

        // set display size
        ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
        lp.width = w;
        lp.height = h;
        mSurfaceView.setLayoutParams(lp);
        mSurfaceView.invalidate();
    }


    //===========================================================================

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!flagOnTouchEvent) {
                    showControls();
                } else hideAllControls();
                xTouch = event.getX();
                yTouch = event.getY();
                DisplayMetrics displayMetrics = this.getResources()
                        .getDisplayMetrics();
                //определяем ширину и высоту дисплея
                maxWidthPix = displayMetrics.widthPixels;
                maxHeightPix = displayMetrics.heightPixels;
                //получаем текущую громкость устройства
                audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
                currentVolume = Objects.requireNonNull(audioManager).getStreamVolume(AudioManager.STREAM_MUSIC);

                currentBrightness = setCurrentBrightness();
                break;
            case MotionEvent.ACTION_UP:
                volume_slider_container.setVisibility(View.GONE);
                brightness_slider_container.setVisibility(View.GONE);

                //сохраняем данные яркости в настройки
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putFloat("saveBrightness", (float) saveBrightness);
                editor.apply();
                break;
            case MotionEvent.ACTION_MOVE:
                float yMove = event.getY();
                float xMove = event.getX();
                float difference = (yTouch - yMove);
                Log.d("sss", "xMove: " + xMove);
                if (xTouch > maxWidthPix / 2) {
                    setVolume(difference);
                } else setBrightness(difference);
                break;
        }//switch
        return super.onTouchEvent(event);
    }//onTouchEvent


    private void setBrightness(float difference) {
        if (Math.abs(difference) > 10) {
            if (brightness_slider == null) {
                brightness_slider = findViewById(R.id.brightness_slider);
            }//if
            brightness_slider_container.setVisibility(View.VISIBLE);
            //устанавливаем текущюю яркость в прогресбаре
            brightness_slider.setProgress((int) (currentBrightness * 100.0));
            //расчитываем на сколько увеличиваем яркость
            int progressScreen = (int) (difference / 10);
            //устанавливаем значение в ProgressBar
            brightness_slider.setProgress((int) (currentBrightness * 100.0 + progressScreen));
            saveBrightness = (float) (brightness_slider.getProgress() / 100.0);
            layout.screenBrightness = (float) saveBrightness;
            getWindow().setAttributes(layout);
        }//if
    }


    private void setVolume(float difference) {
        if (Math.abs(difference) > MULTIPLICITY_OF_VOLUME_TO_PROGRESSBAR) {
            if (volume_slider == null) {
                volume_slider = findViewById(R.id.volume_slider);

                //получаем максимальную громкость устройства
                maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                //устанавливаем макс. деление гормкости в прогресбаре
                volume_slider.setMax(maxVolume * MULTIPLICITY_OF_VOLUME_TO_PROGRESSBAR);
            }//if
            volume_slider_container.setVisibility(View.VISIBLE);
            //устанавливаем текущюю громкость в прогресбаре
            volume_slider.setProgress(currentVolume * MULTIPLICITY_OF_VOLUME_TO_PROGRESSBAR);
            //расчитываем на сколько увеличиваем громкость
            int progressScreen = (int) (difference / 6);
            //устанавливаем значение в ProgressBar
            volume_slider.setProgress(currentVolume * MULTIPLICITY_OF_VOLUME_TO_PROGRESSBAR + progressScreen);
            int new_volume = volume_slider.getProgress() / MULTIPLICITY_OF_VOLUME_TO_PROGRESSBAR;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, new_volume, 0);
        }//if
    }


    private double setCurrentBrightness() {
        if (mSettings == null)
            mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        if (mSettings.contains("saveBrightness")) {
            currentBrightness = mSettings.getFloat("saveBrightness", 0.5f);
        } else currentBrightness = 0.5f;//присваиваем дефолтные данные
        return currentBrightness;
    }


    private void hideAllControls() {
        if (controlsState == ControlsMode.FULLCONTORLS) {
            if (root.getVisibility() == View.VISIBLE) {
                root.setVisibility(View.GONE);
            }
        } else if (controlsState == ControlsMode.LOCK) {
            if (unlock_panel.getVisibility() == View.VISIBLE) {
                unlock_panel.setVisibility(View.GONE);
            }
        }
        decorView.setSystemUiVisibility(immersiveOptions);
        flagOnTouchEvent = false;
    }

    private void showControls() {
        if (controlsState == ControlsMode.FULLCONTORLS) {
            if (root.getVisibility() == View.GONE) {
                root.setVisibility(View.VISIBLE);
            }
        } else if (controlsState == ControlsMode.LOCK) {
            if (unlock_panel.getVisibility() == View.GONE) {
                unlock_panel.setVisibility(View.VISIBLE);
            }
        }
        decorView.setSystemUiVisibility(View.VISIBLE);
        threadHandler.removeCallbacks(hideControls);
        threadHandler.postDelayed(hideControls, 3000);
        flagOnTouchEvent = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
        threadHandler.removeCallbacks(updateSeekBar);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//удаляем флаг на запрет отключения экрана
    }//onDestroy
}
