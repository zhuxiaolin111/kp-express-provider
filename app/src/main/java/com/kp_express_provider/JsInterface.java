package com.kp_express_provider;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

/**
 * Created by Administrator on 2016/8/22 0022.
 */
public class JsInterface {
    private Context context;
    public JsInterface(Context context) {
        this.context = context;
    }
    @org.xwalk.core.JavascriptInterface
    public void startRing() {
        NotificationManager mgr = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        Notification nt = new Notification();
        nt.defaults = Notification.DEFAULT_SOUND;
        int soundId = 1;
        mgr.notify(soundId, nt);
    }
}
