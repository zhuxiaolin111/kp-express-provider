package com.kp_express_provider;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class AutoInstall {
	private static String mUrl;
	private static Context mContext;
	

	public static void setUrl(String url) {
		mUrl = url;
	}

	public static void install(Context context) {
		mContext = context;
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.parse(mUrl),
				"application/vnd.android.package-archive");
		mContext.startActivity(intent);
	}
}
