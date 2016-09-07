package com.kp_express_provider;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class SplashActivity extends Activity{
	private UpdateInfo info;
	private TextView tv_splash_version;
	private static final int GET_INFO_SUCCESS = 10;
	private static final int SERVER_ERROR = 11;
	private static final int SERVER_URL_ERROR = 12;
	private static final int IO_ERROR = 13;
	private static final int XML_PARSER_ERROR = 14;
	private static final String DL_ID = "downloadId";
	private SharedPreferences prefs;
	private DownloadManager downloadManager;
	private String componentName = "com.kp_express_provider";
	private String componentActivityName = "com.kp_express_provider.MainActivity";
	protected static final String TAG = "SplashActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		this.tv_splash_version = (TextView) this.findViewById(R.id.tv_splash_version);
		this.tv_splash_version.setText("Version："+getVersion());
		
		
		
		
	}
	
	private class CheckVersionTask implements Runnable{
		@Override
		public void run() {
			Message msg = Message.obtain();
			
			String serverUrl = getResources().getString(R.string.server_url);
			
			
			
			if (isNetworkConnected(getApplicationContext())==false) {
				Intent mIntent = new Intent();
				ComponentName comp = new ComponentName("com.android.settings",
						"com.android.settings.Settings");
				mIntent.setComponent(comp);
				mIntent.setAction("android.intent.action.VIEW");
				startActivity(mIntent);
			}else{
				downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
				prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				
				try {
					URL url = new URL(serverUrl);   
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setConnectTimeout(5000);
					conn.setRequestMethod("GET");
					int code = conn.getResponseCode();
					if(code == 200){
						InputStream is = conn.getInputStream();
						info = UpdateInfoParser.getUpdateInfo(is);
						msg.what = GET_INFO_SUCCESS;
						handler.sendMessage(msg);
					}else{
						msg.what = SERVER_ERROR;
						handler.sendMessage(msg);
					}
				} catch (MalformedURLException e) {
					msg.what = SERVER_URL_ERROR;
					handler.sendMessage(msg);
					handler.sendMessage(msg);
				} catch (IOException e) {
					msg.what = IO_ERROR;
					handler.sendMessage(msg);
					e.printStackTrace();
				} catch (XmlPullParserException e) {
					msg.what = XML_PARSER_ERROR;
					handler.sendMessage(msg);
					e.printStackTrace();
				}
			}
			
			
		}
	}
	
	private Handler handler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case XML_PARSER_ERROR:
			//	Toast.makeText(getApplicationContext(), "XML parsing exception", Toast.LENGTH_LONG).show();
				loadMainUI();
				break;
			case IO_ERROR:
			//	Toast.makeText(getApplicationContext(), "IO exception", Toast.LENGTH_LONG).show();
				loadMainUI();
				break;
			case SERVER_URL_ERROR:
			//	Toast.makeText(getApplicationContext(), "Server URL error", Toast.LENGTH_LONG).show();
				loadMainUI();
				break;
			case SERVER_ERROR:
			//	Toast.makeText(getApplicationContext(), "Server exception", Toast.LENGTH_LONG).show();
				loadMainUI();
				break;
			case GET_INFO_SUCCESS:
				String serverVersion = info.getVersion();
				String currentVersion = getVersion(); 
				
				if(currentVersion.equals(serverVersion)){
					Log.i(TAG, "Version number same, enter the main interface!");
					loadMainUI();
				}else{
					Log.i(TAG, "Version number is not the same!");
					showUpdateDialog();
				}
				break;

			default:
				break;
			}
		}
		
	};
	
	public void loadMainUI(){   
		try{
			Intent mIntent = new Intent();
			ComponentName comp = new ComponentName(componentName,
					componentActivityName);
			mIntent.setComponent(comp);
			mIntent.setAction("android.intent.action.VIEW");
			startActivity(mIntent);
			finish(); 
		}catch(Exception ex){
		//	Toast.makeText(getApplicationContext(), "First installation, must be upgrade!", Toast.LENGTH_LONG).show();
			showUpdateDialog();
		}
		
		
	}
	
	protected void showUpdateDialog() {
		AlertDialog.Builder builder = new Builder(this);
		builder.setIcon(getResources().getDrawable(R.drawable.notification));
		builder.setTitle("Upgrade");
		builder.setMessage(info.getDescription());
		builder.setPositiveButton("Upgrade", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Log.i(TAG, "Upgrade，URL: "+info.getApkurl());
				String apkURL = info.getApkurl();
				if (isNetworkConnected(getApplicationContext())) {
					if (!prefs.contains(DL_ID)) {
						downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
						Uri resource = Uri.parse(apkURL);
						DownloadManager.Request request = new DownloadManager.Request(
								resource);
						request.setAllowedNetworkTypes(Request.NETWORK_MOBILE
								| Request.NETWORK_WIFI);
						request.setAllowedOverRoaming(false);
						MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
						String mimeString = mimeTypeMap
								.getMimeTypeFromExtension(MimeTypeMap
										.getFileExtensionFromUrl(apkURL));
						request.setMimeType(mimeString);
						request.setShowRunningNotification(true);
						request.setVisibleInDownloadsUi(true);
						request.setDestinationInExternalPublicDir("/download/",
								"RMS.apk");
						request.setTitle("Update");
						long id = downloadManager.enqueue(request);
						prefs.edit().putLong(DL_ID, id).commit();
					} else {
						queryDownloadStatus();
					}
					
				} else {
					Intent mIntent = new Intent();
					ComponentName comp = new ComponentName("com.android.settings",
							"com.android.settings.Settings");
					mIntent.setComponent(comp);
					mIntent.setAction("android.intent.action.VIEW");
					startActivity(mIntent);
				}
			}
		});
		builder.setNegativeButton("Cancel", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				loadMainUI();
			}
		});
		
		builder.create().show();
	}
	
	public String getVersion(){
		PackageManager pm = getPackageManager();
		try {
			PackageInfo info = pm.getPackageInfo(componentName, 0);
			return info.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.v("intent",
					""
							+ intent.getLongExtra(
									DownloadManager.EXTRA_DOWNLOAD_ID, 0));
			queryDownloadStatus();
		}
	};
	
	private void queryDownloadStatus() {
		DownloadManager.Query query = new DownloadManager.Query();
		query.setFilterById(prefs.getLong(DL_ID, 0));
		Cursor c = downloadManager.query(query);
		if (c.moveToFirst()) {
			int status = c.getInt(c
					.getColumnIndex(DownloadManager.COLUMN_STATUS));
			switch (status) {
			case DownloadManager.STATUS_PAUSED:
				Log.v("down", "STATUS_PAUSED");
			case DownloadManager.STATUS_PENDING:
				Log.v("down", "STATUS_PENDING");
			case DownloadManager.STATUS_RUNNING:
				Log.v("down", "STATUS_RUNNING");
				break;
			case DownloadManager.STATUS_SUCCESSFUL:
				Log.v("down", "STATUS_OK");
				String fileName = c.getString(c
						.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
				if(checkApkExist(this,"com.rms")==false){
					AutoInstall.setUrl(fileName);
					AutoInstall.install(this);
				}else{
					
				}
				
				break;
			case DownloadManager.STATUS_FAILED:
				Log.v("down", "STATUS_FAILED");
				downloadManager.remove(prefs.getLong(DL_ID, 0));
				prefs.edit().clear().commit();
				break;
			}
		}
	}
	
	public boolean checkApkExist(Context context, String packageName) {
		if (packageName == null || "".equals(packageName))
			return false;
		try {
			ApplicationInfo info = context.getPackageManager()
					.getApplicationInfo(packageName,
							PackageManager.GET_UNINSTALLED_PACKAGES);
			return true;
		} catch (NameNotFoundException e) {
			return false;
		}
	}
	
	public boolean isNetworkConnected(Context context) {
		if (context != null) {
			ConnectivityManager mConnectivityManager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo mNetworkInfo = mConnectivityManager
					.getActiveNetworkInfo();
			if (mNetworkInfo != null) {
				return mNetworkInfo.isAvailable();
			}
		}
		return false;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(receiver, new IntentFilter(
				DownloadManager.ACTION_DOWNLOAD_COMPLETE));
		new Thread(new CheckVersionTask()){}.start();
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		unregisterReceiver(receiver);
		
	}
	
	
}
