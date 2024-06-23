package com.example.notifylistenerdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;

import android.content.ComponentName;
import android.content.Intent;

import android.content.pm.PackageManager;

import android.os.Handler;
import android.os.Message;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static com.example.notifylistenerdemo.NotifyHelper.*;

import com.example.notifylistenerdemo.ble.BLEManager;
import com.example.notifylistenerdemo.ble.OnBleConnectListener;
import com.example.notifylistenerdemo.ble.OnDeviceSearchListener;
import com.example.notifylistenerdemo.permission.PermissionListener;
import com.example.notifylistenerdemo.permission.PermissionRequest;

public class MainActivity extends AppCompatActivity implements NotifyListener {

    private static final int REQUEST_CODE = 9527;
    //通知是消息类型
//    private Boolean isInformation;
    //通知是电话类型
//    private Boolean isCall;
    private TextView textView;


    private static final String TAG = "BLEMain";

    //bt_patch(mtu).bin
    public static final String SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b";  //蓝牙通讯服务
    public static final String WRITE_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8";

    //动态申请权限
    private String[] requestPermissionArray = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_PRIVILEGED,
    };
    // 声明一个集合，在后面的代码中用来存储用户拒绝授权的权限
    private List<String> deniedPermissionList = new ArrayList<>();


    private static final int CONNECT_SUCCESS = 0x01;
    private static final int CONNECT_FAILURE = 0x02;
    private static final int DISCONNECT_SUCCESS = 0x03;
    private static final int SEND_SUCCESS = 0x04;
    private static final int SEND_FAILURE = 0x05;
    private static final int RECEIVE_SUCCESS = 0x06;
    private static final int RECEIVE_FAILURE = 0x07;
    private static final int START_DISCOVERY = 0x08;
    private static final int STOP_DISCOVERY = 0x09;
    private static final int DISCOVERY_DEVICE = 0x0A;
    private static final int DISCOVERY_OUT_TIME = 0x0B;
    private static final int SELECT_DEVICE = 0x0C;
    private static final int BT_OPENED = 0x0D;
    private static final int BT_CLOSED = 0x0E;
    private static final int SET_TEXT = 0x0F;

//    private LVDevicesAdapter lvDevicesAdapter;
//    private ListView lvDevices;


    private Context mContext;
    private BLEManager bleManager;
    private BluetoothDevice curBluetoothDevice;  //当前连接的设备

    private BluetoothGatt bluetoothGatt;
    private BluetoothAdapter bluetoothAdapter;

    private BluetoothDevice bluetoothDevice;
    //当前设备连接状态
    private boolean curConnState = false;

    private String BLEGattServerAddress = "CC:7B:5C:1E:17:3E";

//    private String BLEGattServerAddress = "30:C9:22:12:DB:3E";
    BluetoothGattCharacteristic characteristic;

    private BluetoothLeScanner bluetoothLeScanner;
    private boolean scanning;
    private Handler handler = new Handler();

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private TextView tvBLEState;


    private void scanLeDevice() {
        if (!scanning) {
            // Stops scanning after a predefined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    bluetoothLeScanner.stopScan(leScanCallback);
                }
            }, SCAN_PERIOD);

            scanning = true;
            bluetoothLeScanner.startScan(leScanCallback);
        } else {
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    // Device scan callback.
    private ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
//                    leDeviceListAdapter.addDevice(result.getDevice());
//                    leDeviceListAdapter.notifyDataSetChanged();
                    // log the device
                    Log.d(TAG, "onScanResult: " + result.getDevice().getName() + " " + result.getDevice().getAddress());
                }
            };


    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case START_DISCOVERY:
                    Log.d(TAG, "开始搜索设备...");
                    break;

                case STOP_DISCOVERY:
                    Log.d(TAG, "停止搜索设备...");
                    break;

                case DISCOVERY_DEVICE:  //扫描到设备
                    BLEDevice bleDevice = (BLEDevice) msg.obj;
//                    lvDevicesAdapter.addDevice(bleDevice);

                    break;

                case SELECT_DEVICE:
                    BluetoothDevice bluetoothDevice = (BluetoothDevice) msg.obj;
//                    tvName.setText(bluetoothDevice.getName());
//                    tvAddress.setText(bluetoothDevice.getAddress());
                    curBluetoothDevice = bluetoothDevice;
                    break;

                case CONNECT_FAILURE: //连接失败
                    Log.d(TAG, "连接失败");
//                    tvCurConState.setText("连接失败");
                    curConnState = false;
                    break;

                case CONNECT_SUCCESS:  //连接成功
                    Log.d(TAG, "连接成功");
//                    tvCurConState.setText("连接成功");
                    curConnState = true;
//                    llDataSendReceive.setVisibility(View.VISIBLE);
//                    llDeviceList.setVisibility(View.GONE);
                    break;

                case DISCONNECT_SUCCESS:
                    Log.d(TAG, "断开成功");
//                    tvCurConState.setText("断开成功");
                    curConnState = false;

                    break;

                case SEND_FAILURE: //发送失败
                    byte[] sendBufFail = (byte[]) msg.obj;
//                    String sendFail = TypeConversion.bytes2HexString(sendBufFail,sendBufFail.length);
//                    tvSendResult.setText("发送数据失败，长度" + sendBufFail.length + "--> " + sendFail);
                    break;

                case SEND_SUCCESS:  //发送成功
                    byte[] sendBufSuc = (byte[]) msg.obj;
//                    String sendResult = TypeConversion.bytes2HexString(sendBufSuc,sendBufSuc.length);
//                    tvSendResult.setText("发送数据成功，长度" + sendBufSuc.length + "--> " + sendResult);
                    break;

                case RECEIVE_FAILURE: //接收失败
                    String receiveError = (String) msg.obj;
//                    tvReceive.setText(receiveError);
                    break;

                case RECEIVE_SUCCESS:  //接收成功
                    byte[] recBufSuc = (byte[]) msg.obj;
//                    String receiveResult = TypeConversion.bytes2HexString(recBufSuc,recBufSuc.length);
//                    tvReceive.setText("接收数据成功，长度" + recBufSuc.length + "--> " + receiveResult);
                    break;

                case BT_CLOSED:
                    Log.d(TAG, "系统蓝牙已关闭");
                    break;
                case BT_OPENED:
                    Log.d(TAG, "系统蓝牙已打开");
                    break;
                case SET_TEXT:
                    tvBLEState.setText(msg.obj.toString());
                    break;
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermission();
        mContext = this;
        // 初始化视图
        initView();
        NotifyHelper.getInstance().setNotifyListener(this);

        // 检查蓝牙是否可用
        if (!checkBLE()) {
            Toast.makeText(this, "该设备不支持低功耗蓝牙", Toast.LENGTH_SHORT).show();
        }
        //初始化权限
        initPermissions();
        // 启用蓝牙
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        scanLeDevice();
        try {
            if (BluetoothAdapter.checkBluetoothAddress(BLEGattServerAddress)) {
                //It is a valid MAC address.
                bluetoothDevice = bluetoothAdapter.getRemoteDevice(BLEGattServerAddress);
//                btSocket = createBluetoothSocket(device);
            } else {

                Toast.makeText(mContext, "Invalid MAC: Address", Toast.LENGTH_LONG).show();
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            e.printStackTrace();
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothGatt = bluetoothDevice.connectGatt(this, true, gattCallback);
    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                gatt.discoverServices();
                mHandler.obtainMessage(SET_TEXT, "Bluetooth Connect Success\n蓝牙连接成功").sendToTarget();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                mHandler.obtainMessage(SET_TEXT, "Bluetooth Connect Success\n蓝牙连接断开").sendToTarget();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // get the service
                characteristic = gatt.getService(UUID.fromString(SERVICE_UUID)).getCharacteristic(UUID.fromString(WRITE_UUID));
            } else {
                mHandler.obtainMessage(SET_TEXT, "Service Not Found\n服务未发现").sendToTarget();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
//                tvBLEState.setText("发送成功");
            } else {
//                tvBLEState.setText("发送失败");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
//                tvBLEState.setText("接收成功");
            } else {
//                tvBLEState.setText("接收失败");
            }
        }
    };


    /**
     * 检查蓝牙是否可用
     * @return
     */
    private boolean checkBLE() {
        // Use this check to determine whether Bluetooth classic is supported on the device.
// Then you can selectively disable BLE-related features.
        boolean bluetoothAvailable = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);

// Use this check to determine whether BLE is supported on the device. Then
// you can selectively disable BLE-related features.
        boolean bluetoothLEAvailable = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        if (bluetoothAvailable && bluetoothLEAvailable) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * 请求权限
     *
     * @param view
     */
    public void requestPermission(View view) {
        if (!isNLServiceEnabled()) {
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivityForResult(intent, REQUEST_CODE);
        } else {
            showMsg("通知服务已开启");
            toggleNotificationListenerService();
        }
    }

    /**
     * 请求权限
     *
     */
    public void requestPermission() {
        if (!isNLServiceEnabled()) {
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivityForResult(intent, REQUEST_CODE);
        } else {
            showMsg("通知服务已开启");
            toggleNotificationListenerService();
        }
    }

    /**
     * 是否启用通知监听服务
     *
     * @return
     */
    public boolean isNLServiceEnabled() {
        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(this);
        if (packageNames.contains(getPackageName())) {
            return true;
        }
        return false;
    }

    /**
     * 切换通知监听器服务
     */
    public void toggleNotificationListenerService() {
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(getApplicationContext(), NotifyService.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        pm.setComponentEnabledSetting(new ComponentName(getApplicationContext(), NotifyService.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (isNLServiceEnabled()) {
                showMsg("通知服务已开启");
                toggleNotificationListenerService();
            } else {
                showMsg("通知服务未开启");
            }
        }
    }


    private void showMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * 收到通知
     *
     * @param type 通知类型
     */
    @Override
    public void onReceiveMessage(int type) {
        switch (type) {
            case N_MESSAGE:
                textView.setText("Receive Message\n收到短信消息");
                //震动一下，通过蓝牙传递一个特征值，表名这是消息
                characteristic.setValue("1");
                break;
            case N_CALL:
                textView.setText("Receive Calling\n收到来电消息");
                //震动一下，通过蓝牙传递一个特征值，表名这是电话
                characteristic.setValue("0");
                break;
            case N_WX:
                textView.setText("Receive WeChat Message\n收到微信消息");
                characteristic.setValue("1");
                break;
            case N_QQ:
                textView.setText("Receive QQ Message\n收到QQ消息");
                characteristic.setValue("1");
//                this.isInformation=true;
                //将this.isInformation通过某种渠道传递给蓝牙
                break;
            default:
                break;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothGatt.writeCharacteristic(characteristic);
    }

    /**
     * 移除通知
     *
     * @param type 通知类型
     */
    @Override
    public void onRemovedMessage(int type) {
        switch (type) {
            case N_MESSAGE:
                textView.setText("Remove \n移除短信消息");
                break;
            case N_CALL:
                textView.setText("Remove \n移除来电消息");
                break;
            case N_WX:
                textView.setText("Remove\n移除微信消息");
                break;
            case N_QQ:
                textView.setText("Remove\n移除QQ消息");
                break;
            default:
                break;
        }
    }

    /**
     * 收到通知
     *
     * @param sbn 状态栏通知
     */
    @Override
    public void onReceiveMessage(StatusBarNotification sbn) {
        if (sbn.getNotification() == null) return;
        //消息内容
        String msgContent = "";
        if (sbn.getNotification().tickerText != null) {
            msgContent = sbn.getNotification().tickerText.toString();
        }

        //消息时间
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINESE).format(new Date(sbn.getPostTime()));
        textView.setText(String.format(Locale.getDefault(),
                "APP Name：%s\nMessage Content：%s\nTime：%s\n",
                sbn.getPackageName(), msgContent, time));
    }

    /**
     * 移除通知
     *
     * @param sbn 状态栏通知
     */
    @Override
    public void onRemovedMessage(StatusBarNotification sbn) {
        textView.setText("通知移除");
    }


    /**
     * 初始化视图
     */
    private void initView() {
        tvBLEState = findViewById(R.id.tvBLEState);
        textView = findViewById(R.id.textView);
    }

    /**
     * 初始化权限
     */
    private void initPermissions() {
        //Android 6.0以上动态申请权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final PermissionRequest permissionRequest = new PermissionRequest();
            permissionRequest.requestRuntimePermission(MainActivity.this, requestPermissionArray, new PermissionListener() {
                @Override
                public void onGranted() {
                    Log.d(TAG, "所有权限已被授予");
                }

                //用户勾选“不再提醒”拒绝权限后，关闭程序再打开程序只进入该方法！
                @Override
                public void onDenied(List<String> deniedPermissions) {
                    deniedPermissionList = deniedPermissions;
                    for (String deniedPermission : deniedPermissionList) {
                        Log.e(TAG, "被拒绝权限：" + deniedPermission);
                    }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnConnBLE:
                if (bluetoothGatt != null) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    bluetoothDevice.connectGatt(this, true, gattCallback);
                }
                break;
            default:
                break;
        }
    }
}