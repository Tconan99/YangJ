package com.jc.yangj;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Path;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xueep on 15/12/00.
 */
public class MonitorService extends AccessibilityService {

    @Override
    public synchronized void onAccessibilityEvent(AccessibilityEvent event) {
        final int eventType = event.getEventType();



        if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            LogUtils.log("onAccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED");

            Notification notification = (Notification) event.getParcelableData();
            List<String> textList = getText(notification);
            if (null != textList && textList.size() > 0) {
                for (String text : textList) {
                    LogUtils.log("message=" + text);
                    if (!TextUtils.isEmpty(text) && text.contains("[微信红包]")) {
                        final PendingIntent pendingIntent = notification.contentIntent;
                        try {
                            pendingIntent.send();
                        } catch (PendingIntent.CanceledException e) {
                            System.err.println(e.getMessage());
                        }
                        break;
                    }
                }
            }
        } else if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            LogUtils.log("onAccessibilityEvent -> " + eventType + " -> " + event);
            String className = event.getClassName().toString();
            LogUtils.log("className -> " + className);
            if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyNotHookReceiveUI".equals(className)) {
                openPacket3();
            } else if ("com.tencent.mm.ui.LauncherUI".equals(className)) {
                clickPacket();
            } else {
                LogUtils.log("event -> " + event);
            }
        } else if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            clickPacket();
        } else {
            LogUtils.log("onAccessibilityEvent -> " + eventType + " -> " + event);
        }

    }

    private void clickPacket() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> nodeInfoList = nodeInfo.findAccessibilityNodeInfosByText("微信红包");
            for (AccessibilityNodeInfo info : nodeInfoList) {
                if (info != null) {
                    AccessibilityNodeInfo parent = info.getParent();
                    if (parent != null && parent.getChildCount() > 1) {
                        AccessibilityNodeInfo type = parent.getChild(1);
                        if (type != null && type.getText() != null) {
                            String typeValue = type.getText().toString();
                            if ("已过期".equals(typeValue) || "已被领完".equals(typeValue) || "已领取".equals(typeValue)) {
                                continue;
                            }
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    }
                }
            }
        }
    }

    private void openPacket3() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            nodeInfo.getText();
        }



        Path path = new Path();

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float dpi = metrics.densityDpi;
        if (640 == dpi) { //1440
            path.moveTo(720, 1575);
        } else if (320 == dpi) { //720p
            path.moveTo(355, 780);
        } else if (480 == dpi) { //1080p
            path.moveTo(540, 1312);
        }

        GestureDescription.Builder builder = new GestureDescription.Builder();
        GestureDescription gestureDescription = builder.addStroke(new GestureDescription.StrokeDescription(path, 1050, 200)).build();
        dispatchGesture(gestureDescription, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                Log.d("tl", "onCompleted");
                // mMutex = false;
                super.onCompleted(gestureDescription);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                // mMutex = false;
                super.onCancelled(gestureDescription);
            }
        }, null);
    }

    private void openPacket2() {

        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> nodeInfoList = nodeInfo.findAccessibilityNodeInfosByText("看看大家的手气");
            if (nodeInfoList.size() > 0) {
                Toast.makeText(this.getApplicationContext(), "open", Toast.LENGTH_SHORT).show();
                // nodeInfo.getChild(2).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }

//        performGlobalAction(GLOBAL_ACTION_HOME);
//
//        new android.os.Handler().postDelayed(
//                new Runnable() {
//                    public void run() {
//                        performGlobalAction(GLOBAL_ACTION_BACK);
//                    }
//                },
//                1000);
    }

    private void openPacket() {
        // 正在加载之后 不会触发可接收的事件 所以需要如下处理 循环有风险 break需谨慎
        int count = 1;
        while (true) {
            AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
            if (nodeInfo != null) {
                if (nodeInfo.getChildCount() > 3
                        && nodeInfo.getChild(3) != null
                        && nodeInfo.getChild(3).getChildCount() == 1
                        && nodeInfo.getChild(3).getChild(0).getText() != null
                        && nodeInfo.getChild(3).getChild(0).getText().toString().equals("看看大家的手气"))  {
                    Toast.makeText(this.getApplicationContext(), "open", Toast.LENGTH_SHORT).show();
                    // nodeInfo.getChild(2).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    break;
                } else {
                    if (nodeInfo.getChildCount() != 1
                            || nodeInfo.getChild(0) == null
                            || nodeInfo.getChild(0).getChildCount() != 2
                            || nodeInfo.getChild(0).getChild(0) == null
                            || !nodeInfo.getChild(0).getChild(0).getClassName().toString().equals("android.widget.ProgressBar")
                            || nodeInfo.getChild(0).getChild(1) == null
                            || nodeInfo.getChild(0).getChild(1).getText() == null
                            || !nodeInfo.getChild(0).getChild(1).getText().toString().equals("正在加载...")) {
                        break;
                    }

//                    List<AccessibilityNodeInfo> loading = nodeInfo.findAccessibilityNodeInfosByText("正在加载...");
//                    // Toast.makeText(this.getApplicationContext(), "don't open2", Toast.LENGTH_SHORT).show();
//                    if (loading.size() == 0) {
//                        break;
//                    }
                    LogUtils.log("don't open2 -> " + nodeInfo);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // Toast.makeText(this.getApplicationContext(), "don't open1", Toast.LENGTH_SHORT).show();
                LogUtils.log("don't open1");

                performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // performGlobalAction(GLOBAL_ACTION_BACK);

                if (count > 0) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    count --;
                } else {
                    break;
                }
            }
        }
    }

    private void traverseNode(AccessibilityNodeInfo node) {
        if (null == node) return;

        final int count = node.getChildCount();
        if (count > 0) {
            for (int i = 0; i < count; ++i) {
                AccessibilityNodeInfo childNode = node.getChild(i);
                traverseNode(childNode);

            }
        } else {
            CharSequence text = node.getText();
            if (null != text && text.length() > 0) {
                String str = text.toString();
                LogUtils.log("MonitorService." + str);

            }
            // LogUtils.log("MonitorService11." + node);
        }
    }


    /**
     * @param notification 通知栏
     * @return 包含的字符串
     */
    public List<String> getText(Notification notification) {
        if (null == notification) return null;

        List<String> text = new ArrayList<>();

        RemoteViews views = notification.bigContentView;
        if (views == null) {
            views = notification.contentView;
        }

        if (views == null) {
            if (notification.tickerText != null) {
                text.add(notification.tickerText.toString());
                return text;
            } else {
                return null;
            }
        }

        try {
            Field field = views.getClass().getDeclaredField("mActions");
            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            ArrayList<Parcelable> actions = (ArrayList<Parcelable>) field.get(views);

            for (Parcelable p : actions) {
                Parcel parcel = Parcel.obtain();
                p.writeToParcel(parcel, 0);
                parcel.setDataPosition(0);

                int tag = parcel.readInt();
                if (tag != 2) continue;

                parcel.readInt();

                String methodName = parcel.readString();
                if (null == methodName) {
                    continue;
                } else if (methodName.equals("setText")) {
                    parcel.readInt();

                    String t = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel).toString().trim();
                    text.add(t);
                }
                parcel.recycle();
            }
        } catch (Exception e) {
            Log.e("YangJ", "error", e);
        }

        return text;
    }

    @Override
    protected void onServiceConnected() {
        Const.isOpen = true;
        super.onServiceConnected();
    }

    @Override
    public void onInterrupt() {
        Const.isOpen = false;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Const.isOpen = false;
        return super.onUnbind(intent);
    }
}
