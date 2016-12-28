package com.example.demoivms;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.MediaPlayer.PlayM4.Constants;
import org.MediaPlayer.PlayM4.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private SurfaceHolder surfaceHolder;
    private SurfaceView surfaceView;
    private int mPort;
    private boolean m_bRun = false;
    private InputStream inStream;
    private TextView playTv;
    private boolean isStart = false;
    private static final String TAG = "ligt";
    private Player player;
    HandlerThread t = new HandlerThread("test");
    Handler handler ;
    ReadM4Thread1 runuable=new ReadM4Thread1();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView_fx);
        surfaceView.setKeepScreenOn(true);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (isStart) {
                    resumePlay(holder);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
//                stopPlay();
            }
        });
        playTv = (TextView) findViewById(R.id.play_tv);
        playTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isStart) {
                    playTv.setText("播放");
                    playTv.setClickable(false);
                    isStart = false;
                    stopPlay();
                    playTv.setClickable(true);
                    Toast.makeText(getApplicationContext(), "停止播放...", Toast.LENGTH_SHORT).show();
                } else {
                    playTv.setText("停止");
                    playTv.setClickable(false);
                    isStart = true;
                    startPlay(surfaceHolder);
                    playTv.setClickable(true);
                    Toast.makeText(getApplicationContext(), "开始播放...", Toast.LENGTH_SHORT).show();
                }
            }
        });
        t.start();
        handler= new Handler(t.getLooper());
    }

    public void startPlay(SurfaceHolder surfaceHolder) {
        player = Player.getInstance();
        mPort = player.getPort();
        if (openHkStream(surfaceHolder)) {
            Log.e(TAG, "打开控件成功");
//            if(t.isAlive()||!t.isInterrupted()) {
//                t.mStop();
////                t.mStart();
//
//            }
            runuable.mRsume();
            handler.post(runuable);


        } else {
            Log.e(TAG, "打开控件失败");
        }

    }

    public void resumePlay(SurfaceHolder surfaceHolder) {
        runuable.mRsume();
        Player player = Player.getInstance();
        player.play(mPort,surfaceHolder);
        Log.e(TAG, "恢复播放...");
    }

    public void pausePlay() {
        runuable.mSuspend();
        Player player = Player.getInstance();
        player.pause(mPort, 1);
        Log.e(TAG, "暂停播放...");
    }

    private void openLocalStream() throws Exception {
        m_bRun = true;
        byte[] readData = new byte[1024 * 100];
        inStream = new FileInputStream(new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "test_huagd" + ".mp4"));
//            inStream = getResources().openRawResource(R.raw.test_zsh);
        while (m_bRun) {
            int len = inStream.read(readData);
            if (len == -1) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            }
            Player player = Player.getInstance();
            int port = 0;
            if (!player.inputData(0, readData, len)) {
                Log.e(TAG, "inputData写入数据失败,err=" + player.getLastError(port));
            }

        }
    }

    public void stopPlay() {
        runuable.mStop();
        player.stop(mPort);
        Log.e(TAG, "success to stop play" + mPort);
        player.closeStream(mPort);
        player.freePort(mPort);
        Log.e(TAG, "释放窗口" + mPort);
    }

    private boolean openHkStream(SurfaceHolder surfaceHolder) {
        if (!player.setStreamOpenMode(mPort, Constants.STREAME_FILE)) {
            Log.e(TAG, "fail to setStreamOpenMode,err=" + player.getLastError(mPort));
            return false;
        }
        if (!player.openStream(mPort, new byte[100000], 40, 50 * 1024 * 1024)) {
            Log.e(TAG, "fail to openStream, err=" + player.getLastError(mPort));
            return false;
        }

        if (!player.play(mPort, surfaceHolder)) {
            Log.e(TAG, "fail to play, err=" + player.getLastError(mPort));
            return false;
        }
        Log.e(TAG, "succuss to play, port=" + mPort);
        return true;
    }

    @Override
    public void onDestroy() {
        stopPlay();
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        pausePlay();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode != KeyEvent.KEYCODE_BACK) {
            return false;
        }
        stopPlay();
        setResult(RESULT_OK);
        finish();
        return true;
    }
//    public class ReadM4Thread extends HandlerThread {
//
//        /*结束*/
//        private static final int STOP = -1;
//        /*暂停*/
//        private static final int SUSPEND = 0;
//        /*开始*/
//        private static final int RUNNING = 1;
//
//        private int status = RUNNING;
//
//        private int count = 0;
//
//        public ReadM4Thread(String name) {
//            super(name);
//        }
//
//        public ReadM4Thread(String name, int priority) {
//            super(name, priority);
//        }
//
//
//        @Override
//        public synchronized void run() {
//            m_bRun = true;
//            byte[] readData = new byte[1024 * 100];
//            try {
//                inStream = new FileInputStream(new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "test_zsh" + ".mp4"));
//            } catch (FileNotFoundException e) {
//                System.out.println("没有找到文件" + "test_zsh.mp4");
//                e.printStackTrace();
//            }
////            inStream = getResources().openRawResource(R.raw.test_zsh);
//            while (status != STOP) {
//                if (status == SUSPEND) {
//                    try {
//                        wait();
//                    } catch (InterruptedException e) {
//                        System.out.println("线程异常终止...");
//                        e.printStackTrace();
//                    }
//                } else {
//                    int len = 0;
//                    try {
//                        len = inStream.read(readData);
//                    } catch (IOException e) {
//                        System.out.println("读取文件有误");
//                        e.printStackTrace();
//                    }
//                    if (len == -1) {
//                        try {
//                            Thread.sleep(10);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        break;
//                    }
//                    Player player = Player.getInstance();
//                    int port = 0;
//                    if (!player.inputData(0, readData, len)) {
//                        Log.e(TAG, "inputData写入数据失败,err=" + player.getLastError(port));
//                    }
//
//                }
//
//            }
//        }
//
//        /**
//         * 开始
//         */
//        public void mStart() {
//            start();
//        }
//
//        /**
//         * 唤醒
//         */
//        public synchronized void mRsume() {
//            status = RUNNING;
//            notifyAll();
//        }
//
//        /**
//         * 暂停
//         */
//        public void mSuspend() {
//            status = SUSPEND;
//        }
//
//        /**
//         * 结束
//         */
//        public void mStop() {
//            status = STOP;
//        }
//    }

    public class ReadM4Thread1 implements Runnable {

        /*结束*/
        private static final int STOP = -1;
        /*暂停*/
        private static final int SUSPEND = 0;
        /*开始*/
        private static final int RUNNING = 1;

        private int status = RUNNING;

        private int count = 0;


        @Override
        public synchronized void run() {
            m_bRun = true;
            byte[] readData = new byte[1024 * 100];
            try {
                inStream = new FileInputStream(new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "test_zsh" + ".mp4"));
            } catch (FileNotFoundException e) {
                System.out.println("没有找到文件" + "test_zsh.mp4");
                e.printStackTrace();
            }
//            inStream = getResources().openRawResource(R.raw.test_zsh);
            while (status != STOP) {
                if (status == SUSPEND) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        System.out.println("线程异常终止...");
                        e.printStackTrace();
                    }
                } else {
                    int len = 0;
                    try {
                        len = inStream.read(readData);
                    } catch (IOException e) {
                        System.out.println("读取文件有误");
                        e.printStackTrace();
                    }
                    if (len == -1) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    Player player = Player.getInstance();
                    int port = 0;
                    if (!player.inputData(0, readData, len)) {
                        Log.e(TAG, "inputData写入数据失败,err=" + player.getLastError(port));
                    }

                }

            }
        }

        /**
         * 唤醒
         */
        public synchronized void mRsume() {
            status = RUNNING;
            notifyAll();
        }

        /**
         * 暂停
         */
        public void mSuspend() {
            status = SUSPEND;
        }

        /**
         * 结束
         */
        public void mStop() {
            status = STOP;
        }
    }
}