package com.example.demoivms;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.hik.mcrsdk.rtsp.LiveInfo;
import com.hik.mcrsdk.rtsp.RtspClient;
import com.hikvision.vmsnetsdk.CameraInfo;
import com.hikvision.vmsnetsdk.CameraInfoEx;
import com.hikvision.vmsnetsdk.RealPlayURL;
import com.hikvision.vmsnetsdk.ServInfo;
import com.hikvision.vmsnetsdk.VMSNetSDK;
import com.hikvision.vmsnetsdk.netLayer.msp.deviceInfo.DeviceInfo;

public class Main2Activity extends AppCompatActivity implements View.OnClickListener, LiveCallBack {

    private EditText serverAddr;
    private EditText userName;
    private EditText password;
    private VMSNetSDK net;
    private Button playBtn;
    private SurfaceView surfaceView;
    private Button loginBtn;
    private ServInfo servInfo;
    private String macAddr;
    private static final int MSG_FAILURE = 0;
    private static final int MSG_SUCCESS = 1;
    private static final int MSG_PROGRESS = 2;
    private static final String TAG = "ligt";
    private Button listBtn;
    private LiveControl mLiveControl;
    private RealPlayURL mRealPlayURL;
    private String mToken;
    /** RTSP sdk句柄 */
    private RtspClient mRtspHandle = null;
    private DeviceInfo deviceInfo;
    private VMSNetSDK mVmsNetSDK;
    private String mCameraID = null;
    private String mDeviceID = null;
    private CameraInfoEx cameraInfoEx;
    /**
     * 码流类型
     */
    private int mStreamType = -1;
    /**
     * 获取监控点详情结果
     */
    private boolean getCameraDetailInfoResult = false;

    /**
     * 获取设备详情结果
     */
    private boolean getDeviceInfoResult = false;
    /**
     * 登录设备的用户名
     */
    private String mName;

    /**
     * 登录设备的密码
     */
    private String mPassword;
    private CameraInfo cameraInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initUI();
        initData();
    }

    private void initData() {
        mVmsNetSDK = VMSNetSDK.getInstance();
        servInfo = new ServInfo();
        macAddr = getMacAddr();
        mLiveControl = new LiveControl();
        mLiveControl.setLiveCallBack(this);
        mRealPlayURL = new RealPlayURL();
        /*获取CameraInfo*/
//        cameraInfo = TempData.getIns().getCameraInfo();
//        mCameraID = cameraInfo.getId();
//        cameraInfoEx = new CameraInfoEx();
//        cameraInfoEx.setId(mCameraID);
        mVmsNetSDK = VMSNetSDK.getInstance();
        if (mVmsNetSDK == null)
        {
            Log.e(TAG, "mVmsNetSDK is null");
            return;
        }
//        String serAddr = "http://10/50.2.155";
//        String sessionid = servInfo.getSessionID();
//
//        getCameraDetailInfo(serAddr, sessionid);
        // RTSP SDK
        mRtspHandle = RtspClient.getInstance();
        if (null == mRtspHandle)
        {
            Log.e(TAG, "initialize:" + "RealPlay mRtspHandle is null!");
            return;
        }
    }

    /**
     * 初始化页面
     */
    private void initUI() {
        serverAddr = (EditText) findViewById(R.id.server_addr);
        userName = (EditText) findViewById(R.id.userName);
        password = (EditText) findViewById(R.id.password);
        playBtn = (Button) findViewById(R.id.play);
        loginBtn = (Button) findViewById(R.id.login);
        listBtn = (Button) findViewById(R.id.list);
        surfaceView = (SurfaceView) findViewById(R.id.video);
        loginBtn.setOnClickListener(this);
        playBtn.setOnClickListener(this);
        listBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.play) {
            startPreview();
        } else if (v.getId() == R.id.login) {
            doLogin();
        } else if (v.getId() == R.id.list) {
            getList();
        }
    }

    /**
     * 获取摄像头列表
     */
    private void getList() {

    }

    /**
     * 登录
     */
    private void doLogin() {
        final String serAdrr = serverAddr.getText().toString().trim();
        final String name = userName.getText().toString().trim();
        final String pswd = password.getText().toString().trim();
        new Thread() {
            @Override
            public void run() {
                handler.sendEmptyMessage(MSG_PROGRESS);
                boolean ret = VMSNetSDK.getInstance().login(serAdrr, name, pswd, macAddr, servInfo);
                if (ret) {
                    TempData.getInstance().setLoginData(servInfo);
                    handler.sendEmptyMessage(MSG_SUCCESS);
                } else {
                    handler.sendEmptyMessage(MSG_FAILURE);
                }
            }
        }.start();
    }

    /**
     * 开始预览
     */
    private void startPreview() {
        new Thread()
        {
            @Override
            public void run()
            {
                super.run();
                mLiveControl.setLiveParams(getPlayUrl(mStreamType), mName, mPassword);

                if (mLiveControl.LIVE_PLAY == mLiveControl.getLiveState())
                {
                    mLiveControl.stop();
                }

                if (mLiveControl.LIVE_INIT == mLiveControl.getLiveState())
                {
                    mLiveControl.startLive(surfaceView);
                }
            }
        }.start();
    }

    /**
     * 获取登录设备mac地址
     *
     * @return
     */
    protected String getMacAddr() {
        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        String mac = wm.getConnectionInfo().getMacAddress();
        return mac == null ? "" : mac;
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SUCCESS:
                    Log.e(TAG,TempData.getInstance().getLoginData().getSessionID());
                    Toast.makeText(Main2Activity.this, "登陆成功", Toast.LENGTH_LONG).show();
                    String serAddr = "http://10/50.2.155";
                    String sessionid = servInfo.getSessionID();
                    getCameraDetailInfo(serAddr, sessionid);
                    break;
                case MSG_FAILURE:
                    Toast.makeText(Main2Activity.this, "登陆失败", Toast.LENGTH_LONG).show();
                    break;
                case MSG_PROGRESS:
                    Toast.makeText(Main2Activity.this, "登陆中...", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    @Override
    public void onMessageCallback(int message) {

    }

    /**
     * 该方法是获取播放地址的，当mStreamType=2时，获取的是MAG，当mStreamType =1时获取的子码流，当mStreamType = 0时获取的是主码流
     * 由于该方法中部分参数是监控点的属性，所以需要先获取监控点信息，具体获取监控点信息的方法见resourceActivity。
     *
     * @param streamType 2、表示MAG取流方式；1、表示子码流取流方式；0、表示主码流取流方式；
     * @return String 播放地址 ：2、表示返回的是MAG的播放地址;1、表示返回的是子码流的播放地址；0、表示返回的是主码流的播放地址。
     * @since V1.0
     */
    private String getPlayUrl(int streamType)
    {
        String url = "";

        if (mRealPlayURL == null)
        {
            return null;
        }

        // 获取播放Token
        mToken = VMSNetSDK.getInstance().getPlayToken(servInfo.getSessionID());
        DebugLog.info(TAG, "mToken is :" + mToken);

        Log.d(TAG, "generateLiveUrl MagStreamSerAddr:" + servInfo.getMagServer().getMagStreamSerAddr());
        Log.d(TAG, "generateLiveUrl MagStreamSerPort:" + servInfo.getMagServer().getMagStreamSerPort());
//        Log.d(TAG, "generateLiveUrl cameraId:" + cameraInfoEx.getId());
        Log.d(TAG, "generateLiveUrl token:" + mToken);
        Log.d(TAG, "generateLiveUrl streamType:" + streamType);
        Log.d(TAG, "generateLiveUrl appNetId:" + servInfo.getAppNetId());
//        Log.d(TAG, "generateLiveUrl deviceNetID:" + cameraInfoEx.getDeviceNetId());
        Log.d(TAG, "generateLiveUrl userAuthority:" + servInfo.getUserAuthority());
//        Log.d(TAG, "generateLiveUrl cascadeFlag:" + cameraInfoEx.getCascadeFlag());
        Log.d(TAG, "generateLiveUrl internet:" + servInfo.isInternet());

        LiveInfo liveInfo = new LiveInfo();
        liveInfo.setMagIp(servInfo.getMagServer().getMagStreamSerAddr());
        liveInfo.setMagPort(servInfo.getMagServer().getMagStreamSerPort());
//        liveInfo.setCameraIndexCode(cameraInfoEx.getId());
        liveInfo.setToken(mToken);
        // 转码不区分主子码流
        liveInfo.setStreamType(streamType);
        liveInfo.setMcuNetID(servInfo.getAppNetId());
//        liveInfo.setDeviceNetID(cameraInfoEx.getDeviceNetId());
        liveInfo.setiPriority(servInfo.getUserAuthority());
//        liveInfo.setCascadeFlag(cameraInfoEx.getCascadeFlag());

//        if (deviceInfo != null)
//        {
//            if (cameraInfoEx.getCascadeFlag() == LiveInfo.CASCADE_TYPE_YES)
//            {
//                deviceInfo.setLoginName("admin");
//                deviceInfo.setLoginPsw("12345");
//            }
//        }

        if (servInfo.isInternet())
        {
            liveInfo.setIsInternet(LiveInfo.NETWORK_TYPE_INTERNET);
            // 获取不转码地址
            liveInfo.setbTranscode(false);
            mRealPlayURL.setUrl1(mRtspHandle.generateLiveUrl(liveInfo));

            // 获取转码地址
            // 使用默认转码参数cif 128 15 h264 ps
            liveInfo.setbTranscode(true);
            mRealPlayURL.setUrl2(mRtspHandle.generateLiveUrl(liveInfo));
        }
        else
        {
            liveInfo.setIsInternet(LiveInfo.NETWORK_TYPE_LOCAL);
            liveInfo.setbTranscode(false);
            // 内网不转码
            mRealPlayURL.setUrl1(mRtspHandle.generateLiveUrl(liveInfo));
            mRealPlayURL.setUrl2("");
        }

        Log.d(TAG, "url1:" + mRealPlayURL.getUrl1());
        Log.d(TAG, "url2:" + mRealPlayURL.getUrl2());

        url = mRealPlayURL.getUrl1();
        if (streamType == 2 && mRealPlayURL.getUrl2() != null && mRealPlayURL.getUrl2().length() > 0)
        {
            url = mRealPlayURL.getUrl2();
        }
        Log.i(TAG, "mRTSPUrl" + url);

        return url;
    }
    /**
     * 获取监控点详情方法
     *
     * @param serAddr 服务器地址
     * @param sessionid 会话ID
     */
    private void getCameraDetailInfo(final String serAddr, final String sessionid)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                getCameraDetailInfoResult =
                        Main2Activity.this.mVmsNetSDK.getCameraInfoEx(serAddr, sessionid, mCameraID, cameraInfoEx);
                Log.i(TAG, "result is :" + getCameraDetailInfoResult);

                mDeviceID = cameraInfoEx.getDeviceId();
                Log.i(TAG, "mDeviceID is :" + mDeviceID);
                deviceInfo = new DeviceInfo();

                // 获取设备信息
                getDeviceInfoResult =
                        Main2Activity.this.mVmsNetSDK.getDeviceInfo(serAddr, sessionid, mDeviceID, deviceInfo);
                if (!getDeviceInfoResult || null == deviceInfo || TextUtils.isEmpty(deviceInfo.getLoginName())
                        || TextUtils.isEmpty(deviceInfo.getLoginPsw()))
                {
                    deviceInfo.setLoginName("admin");
                    deviceInfo.setLoginPsw("12345");
                }
                mName = deviceInfo.getLoginName();
                mPassword = deviceInfo.getLoginPsw();

                DebugLog.info(TAG,
                        "ret is :" + getDeviceInfoResult + "----------------" + deviceInfo.getDeviceName() + "--------"
                                + "deviceLoginName is " + mName + "---" + "deviceLoginPassword is " + mPassword + "-----"
                                + "deviceID is " + mDeviceID);
            }
        }).start();

    }
    /**
     * 取流流程：
     * 1.登录，获取serverInfo
     * 2.获取监控点位，List<CameraInfo>
     * 3.点击某个CameraInfo,获取CameraInfoEx
     * 4.预览
     */
}
