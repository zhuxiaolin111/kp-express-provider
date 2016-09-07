package com.kp_express_provider;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;

import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkUIClient;
import org.xwalk.core.XWalkView;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.UUID;

import butterknife.OnClick;

public class MainActivity extends Activity {


    XWalkView webView;
    private XWalkView xWalkView;
    private String TAG = "KP-EXPRESS";
    private WebView mWebView;
    private int appType = 2;
    private String siteUrl = "http://60.205.115.210/provider/";
    private BluetoothAdapter adapter;
    private boolean isStartBlueTooth = false;
    private String appLang = "chinese";

    private LocationMode tempMode = LocationMode.Hight_Accuracy;
    private String tempcoor = "bd09ll";
    private LocationClient mLocationClient;
    public MyLocationListener mMyLocationListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        setContentView(R.layout.activity_main);



        mLocationClient = new LocationClient(MainActivity.this);
        mMyLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(mMyLocationListener);
        initLocation();

        adapter = BluetoothAdapter.getDefaultAdapter();


        initGPS();
        startApp();
    }

    public void startApp() {
        xWalkView = (XWalkView) findViewById(R.id.webView);
        xWalkView.load("javascript:document.body.contentEditable=true;", null);
        xWalkView.addJavascriptInterface(new JsInterface(this), "kp");
        xWalkView.setUIClient(new XWalkUIClient(xWalkView));
        xWalkView.setResourceClient(new XWalkResourceClient(xWalkView) {
            @Override
            public void onLoadFinished(XWalkView view, String url) {
                super.onLoadFinished(view, url);
            }

            @Override
            public void onLoadStarted(XWalkView view, String url) {
                super.onLoadStarted(view, url);
            }
        });
        if (getResources().getConfiguration().locale.getCountry().equals("CN")) {
            appLang = "chinese";
        } else if (getResources().getConfiguration().locale.getCountry().equals("KR")) {
            appLang = "korean";
        } else {
            appLang = "english";
        }
        xWalkView.load(siteUrl + "index.php?appType=" + appType + "&appLang=" + appLang, null);
        /*mWebView = (WebView) findViewById(R.id.webView);
        WebSettings setting = mWebView.getSettings();
		setting.setJavaScriptEnabled(true);
		setting.setJavaScriptCanOpenWindowsAutomatically(true);
		mWebView.clearCache(true);
		mWebView.setWebChromeClient(new WebChromeClient());
		mWebView.setWebViewClient(new WebViewClient() {			
			@Override
			public void onPageFinished(WebView view, String url) {
				// TODO Auto-generated method stub
				super.onPageFinished(view, url);
				mLocationClient.start();
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				// TODO Auto-generated method stub
				view.loadUrl(url);
				return true;
			}
		});	
		
		if (getResources().getConfiguration().locale.getCountry().equals("CN")){
			appLang = "chinese";
		}else if(getResources().getConfiguration().locale.getCountry().equals("KR")){
			appLang = "korean";
		}else{
			appLang = "english";
		}
		
		
		mWebView.loadUrl(siteUrl + "login.php?appType="+ appType + "&appLang=" + appLang);*/
    }

    private void initLocation() {
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(tempMode);
        option.setCoorType(tempcoor);
        int span = 30000;
        option.setScanSpan(span);
        option.setIsNeedAddress(true);
        option.setOpenGps(true);
        option.setLocationNotify(true);
        option.setIgnoreKillProcess(true);
        option.setEnableSimulateGps(false);
        option.setIsNeedLocationDescribe(true);
        option.setIsNeedLocationPoiList(true);
        mLocationClient.setLocOption(option);
    }

    private void sendLocation(BDLocation location) {
        mWebView.loadUrl("javascript:recvLocationInfo('" + location.getLatitude() + "','" + location.getLongitude() + "');", null);
        mLocationClient.stop();
    }

    @OnClick(R.id.webView)
    public void onClick() {
    }

    public class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            // TODO Auto-generated method stub
            if (location.getLocType() == BDLocation.TypeGpsLocation) {
                sendLocation(location);
                //Log.d("TypeGpsLocation", location.getAddrStr());
            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {
                sendLocation(location);
                //Log.d("TypeNetWorkLocation", location.getAddrStr());
            } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {
                sendLocation(location);
                //Log.d("TypeOffLineLocation", location.getAddrStr());
            } else if (location.getLocType() == BDLocation.TypeServerError) {
                //Log.d("TypeServerError", location.getAddrStr());
            } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
                //Log.d("TypeNetWorkException", location.getAddrStr());
            } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
                //Log.d("TypeCriteriaException", location.getAddrStr());
            }
        }

    }

    ConnectThread connectThread;

    public synchronized void connect(BluetoothDevice device, BluetoothAdapter madapter) {
        connectThread = new ConnectThread(device, madapter);
        connectThread.start();
    }


    ReceiveDatas connectBluetooth;

    public class ConnectThread extends Thread {
        BluetoothSocket mySocket;
        final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
        UUID MY_UUID = UUID.fromString(SPP_UUID);
        BluetoothAdapter mAdapterr;

        public ConnectThread(BluetoothDevice device, BluetoothAdapter mAdapterr) {
            this.mAdapterr = mAdapterr;
            int sdk = Build.VERSION.SDK_INT;
            if (sdk >= 10) {
                try {
                    mySocket = device
                            .createInsecureRfcommSocketToServiceRecord(MY_UUID);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    mySocket = device
                            .createRfcommSocketToServiceRecord(MY_UUID);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }

        public void run() {
            try {
                this.mAdapterr.cancelDiscovery();
                mySocket.connect();

                connectBluetooth = new ReceiveDatas(mySocket);
                connectBluetooth.start();
            } catch (IOException e) {
                try {
                    mySocket.close();
                } catch (IOException ee) {
                    // TODO Auto-generated catch block
                    ee.printStackTrace();
                }
            }

        }


    }

    public class ReceiveDatas extends Thread {
        BluetoothSocket mmSocket;
        InputStream mmInStream;
        BufferedReader br;

        public ReceiveDatas(BluetoothSocket socket) {

            this.mmSocket = socket;
            InputStream tempIn = null;

            try {
                tempIn = socket.getInputStream();
            } catch (IOException e) {
                // TODO Auto-generated catch block
            }
            mmInStream = tempIn;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            String reply = null;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    br = new BufferedReader(new InputStreamReader(mmInStream));
                    while (!((reply = br.readLine()) == null)) {
                        final String freeLen = reply;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Code for WebView goes here
                                mWebView.loadUrl("javascript:recvSpaceEvent('" + freeLen + "');", null);
                            }
                        });
                        System.out.println("收到数据：" + reply);
                    }
                } catch (IOException e) {
                    try {
                        if (mmInStream != null) {
                            mmInStream.close();
                        }
                        Log.i("info", "出错了");
                        break;
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    public static String InputStreamTOString(InputStream in, String encoding) throws Exception {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int count = -1;
        while ((count = in.read(data, 0, 1024)) != -1)
            outStream.write(data, 0, count);

        data = null;
        return new String(outStream.toByteArray(), "ISO-8859-1");
    }


    @JavascriptInterface
    public void startBlueTooth(String deviceName) {
        if (adapter == null) {
            mWebView.loadUrl("javascript:alert('请开启蓝牙~')", null);
        } else {
            if (!adapter.isEnabled()) {
                adapter.enable();
                adapter.cancelDiscovery();
            }
            while (!adapter.startDiscovery()) {
                Log.e("BlueTooth", "配对中");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
            for (BluetoothDevice devices : pairedDevices) {
                if (devices.getName().equals(deviceName) && isStartBlueTooth == false) {
                    connect(devices, adapter);
                    isStartBlueTooth = true;
                }
            }
        }

    }

    @JavascriptInterface
    public void stopBlueTooth() {
        adapter.disable();
    }

    @JavascriptInterface
    public void exitApp() {
        System.exit(0);
        Process.killProcess(Process.myPid());
    }


    @Override
    public void onStop() {
        super.onStop();
    }


    private void initGPS() {
        LocationManager locationManager = (LocationManager) this
                .getSystemService(Context.LOCATION_SERVICE);
        // 判断GPS模块是否开启，如果没有则开启
        if (!locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(MainActivity.this, "请打开GPS",
                    Toast.LENGTH_SHORT).show();
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("温馨提示");
            dialog.setMessage("请您打开GPS");
            dialog.setIcon(R.drawable.kpexpress150x150);
            dialog.setPositiveButton("确定",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {

                            // 转到手机设置界面，用户设置GPS
                            Intent intent = new Intent(
                                    Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(intent, 0); // 设置完成后返回到原来的界面

                        }
                    });
            dialog.setNeutralButton("取消", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    arg0.dismiss();
                }
            });
            dialog.show();
        } else {
            // 弹出Toast
//			Toast.makeText(TrainDetailsActivity.this, "GPS is ready",
//					Toast.LENGTH_LONG).show();
//			// 弹出对话框
//		new AlertDialog.Builder(this).setMessage("GPS is ready").setPositiveButton("OK", null).show();
        }
    }


}