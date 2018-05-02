package com.example.den.vlc_video_player;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
//import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
//import android.widget.VideoView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.media.VideoView;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity implements SelectedVideoInterface, IVLCVout.Callback {
    private List<String> list;
    private List<String> listName;
    //    private org.videolan.libvlc.media.VideoView videoView;
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
    private ImageButton btn_stop;
    private ImageButton btn_settings;
    private ImageButton btn_back;
    private Button buttonPermis;
    private int curTrackIndex;
    private boolean flagStartPlay = true;
    private boolean flagSavedInstanceState = false;
    private Runnable hideControls;
    private ControlsMode controlsState;
    private LinearLayout root;
    private LinearLayout top_controls;
    private LinearLayout middle_panel;
    private LinearLayout unlock_panel;
    private RelativeLayout layoutButtonPermiss;
    private View decorView;
    private View view;
    private int immersiveOptions;
    private DialogPlayList dialogPlayList;
    // size of the video
    private int mVideoHeight;
    private int mVideoWidth;
    private final static int VideoSizeChanged = -1;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private LibVLC mLibVLC;
    private org.videolan.libvlc.MediaPlayer mMediaPlayer;
    private Surface mSurface;

    public enum ControlsMode {
        LOCK, FULLCONTORLS
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = getLayoutInflater().inflate(R.layout.activity_main, null);
        setContentView(view);

        buttonPermis = findViewById(R.id.idButtonPermission);
        layoutButtonPermiss = findViewById(R.id.idLayoutButtonPermiss);
        buttonPermis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //install after running
                MainActivityPermissionsDispatcher.getListVodeoWithPermissionCheck(MainActivity.this);
            }
        });

        if (savedInstanceState != null) {
            curTrackIndex = savedInstanceState.getInt("curTrackIndex");
            list = savedInstanceState.getStringArrayList("list");
            listName = savedInstanceState.getStringArrayList("listName");
            curPosition = savedInstanceState.getInt("curPosition");
            timeDuration = savedInstanceState.getInt("timeDuration");
            flagStartPlay = savedInstanceState.getBoolean("flagStartPlay");
            flagSavedInstanceState = true;

            if (list == null) {
                Snackbar.make(view, "Нельзя запустить плеер без разрешений!", Snackbar.LENGTH_LONG).show();
            } else if (list.size() == 0) {
                buttonPermis.setVisibility(View.GONE);
                Snackbar.make(view, "На устройстве отсутствует видео файлы", Snackbar.LENGTH_LONG).show();
            } else {
                layoutButtonPermiss.setVisibility(View.GONE);
                instalVidget();
                installVideo();
                initializationButtons();
            }
        } else
            MainActivityPermissionsDispatcher.getListVodeoWithPermissionCheck(this);//install after running
        if (dialogPlayList != null && !dialogPlayList.isVisible()) {
            flagStartPlay = true;
        }
    }//onCreate

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
                    String videoSource = list.get(curTrackIndex);
                    Media m = new Media(mLibVLC, videoSource);
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
                if (flagStartPlay) {//если играл
                    //threadHandler.post(updateSeekBar);
                }
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
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (list != null) {
                            mMediaPlayer.stop();//mMediaPlayer = null
                        }
                        finish();
                    }
                });
        btn_play.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!mMediaPlayer.isPlaying()) {
                            flagStartPlay = true;
                            changePlayToPause(true);
                            mMediaPlayer.play();
                            mMediaPlayer.setTime(curPosition);
                        }
                    }
                });
        btn_pause.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mMediaPlayer.isPlaying()) {
                            flagStartPlay = false;
                            changePlayToPause(false);
                            mMediaPlayer.pause();
                        }
                    }
                });
        btn_fwd.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if ((curPosition + 5000) < mMediaPlayer.getLength()) {
                            curPosition += 5000;
                            changeCurPosition();
                        }
                    }
                });
        btn_rev.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if ((curPosition - 5000) > 0) {
                            curPosition -= 5000;
                            changeCurPosition();
                        }
                    }
                });
        btn_prev.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (curTrackIndex != 0) {
                            curTrackIndex -= 1;
                            curPosition = 0;
                            installVideo();
                        } else installVideo();
                        changePlayToPause(true);
                        mMediaPlayer.play();
                    }
                });
        btn_next.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (curTrackIndex != list.size() - 1) {
                            curTrackIndex += 1;
                            curPosition = 0;
                            installVideo();
                        } else installVideo();
                        changePlayToPause(true);
                        mMediaPlayer.play();
                    }
                });
        btn_stop.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mMediaPlayer.stop();//mMediaPlayer = null
                        changePlayToPause(false);
                    }
                });
        btn_settings.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialogPlayList = new DialogPlayList();
                        Bundle args = new Bundle();//создаем Bundle для передачи в диалог информации
                        args.putStringArrayList("listName", (ArrayList<String>) listName);
                        dialogPlayList.setArguments(args);//показать данные в диалоге
                        dialogPlayList.show(getSupportFragmentManager(), "dialogPlayList");// отображение диалогового окна в фрагменте
                        if (mMediaPlayer.isPlaying()) {
                            changePlayToPause(false);
                            mMediaPlayer.pause();
                            flagStartPlay = false;
                            curPosition = (int) mMediaPlayer.getTime();
                        }
                    }
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
        Log.d("kkk", "jjj");
    }

    @Override
    public void onHardwareAccelerationError(IVLCVout vlcVout) {
        Log.d("kkk", "jjj");
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

    @Override
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

    private void changeCurPosition(){
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
        btn_stop = findViewById(R.id.btn_stop);
        btn_settings = findViewById(R.id.btn_settings);
        txt_ct = findViewById(R.id.txt_currentTime);
        txt_td = findViewById(R.id.txt_totalDuration);
        seekBar = findViewById(R.id.seekbar);

        root = findViewById(R.id.root);
        root.setVisibility(View.VISIBLE);
        LinearLayout seekbar_time = findViewById(R.id.seekbar_time);
        seekbar_time.setVisibility(View.VISIBLE);
        LinearLayout top = findViewById(R.id.top);
        top.setVisibility(View.VISIBLE);
        LinearLayout bottom_controls = findViewById(R.id.controls);
        bottom_controls.setVisibility(View.VISIBLE);

        immersiveOptions = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(immersiveOptions);
    }//instalVidget

    private void setSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        if (mVideoWidth * mVideoHeight <= 1)
            return;

        // get screen size
        int w = getWindow().getDecorView().getWidth();
        int h = getWindow().getDecorView().getHeight();

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
    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void getListVodeo() {
        layoutButtonPermiss = findViewById(R.id.idLayoutButtonPermiss);
        layoutButtonPermiss.setVisibility(View.GONE);
        list = new ArrayList<>();
        listName = new ArrayList<>();
        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        if (cursor == null) {
            Toast.makeText(this, "Ошибка", Toast.LENGTH_LONG).show();
            return;
        } else if (!cursor.moveToFirst()) {
            Snackbar.make(view, "На устройстве отсутствует видео файлы", Snackbar.LENGTH_INDEFINITE).show();
            return;
        } else {
            instalVidget();
            int dataColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATA);
            int dataColumnName = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME);
            do {
                String name = cursor.getString(dataColumnName);
                if (name != null) listName.add(name);
                list.add(cursor.getString(dataColumn));
            } while (cursor.moveToNext());
        }
        cursor.close();
        curTrackIndex = 0;
        installVideo();
        if (flagSavedInstanceState) {
            mMediaPlayer.setTime(curPosition);
        }
        initializationButtons();
    }

    @OnPermissionDenied({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void permissionsDenied() {
        Snackbar.make(view, "Нельзя запустить плеер без разрешений!", Snackbar.LENGTH_LONG).show();
    }//permissionsDenied

    @OnNeverAskAgain({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void onNeverAskAgain() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Получите разрешения!")
                .setMessage("Нельзя запустить плеер без разрешений!")
                .setPositiveButton("Хорошо", (dialog, which) -> {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Не хочу", (dialog, which) -> dialog.dismiss()).create()
                .show();
    }

    @OnShowRationale({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void showRationale(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setTitle("Получите разрешения!")
                .setMessage("Необходимо получить разрешения для доступа к списку видеофайлов")
                .setPositiveButton("Хорошо", (dialog, button) -> request.proceed())
                .setNegativeButton("Не хочу", (dialog, button) -> request.cancel())
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        //install after running
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    //========================================================================================
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                showControls();
                break;
        }
        return super.onTouchEvent(event);
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
        threadHandler.removeCallbacks(hideControls);
        threadHandler.postDelayed(hideControls, 3000);
    }

    //===========================================================================
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("curTrackIndex", curTrackIndex);
        outState.putInt("timeDuration", timeDuration);
        outState.putStringArrayList("list", (ArrayList<String>) list);
        outState.putStringArrayList("listName", (ArrayList<String>) listName);
        outState.putInt("curPosition", curPosition);
        outState.putBoolean("flagStartPlay", flagStartPlay);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
        threadHandler.removeCallbacks(updateSeekBar);
    }//onDestroy
}
