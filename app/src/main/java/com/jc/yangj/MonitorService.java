package com.jc.yangj;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.RemoteViews;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xueep on 15/12/00.
 */
public class MonitorService extends AccessibilityService {
    private ArrayList<AccessibilityNodeInfo> mNodeInfoList = new ArrayList<AccessibilityNodeInfo>();

    private boolean mLuckyInfo;             // 内容页
    private boolean mContainsLucky;         // 有红包

    private boolean mContainsOpenLucky;     // 拆红包
    private boolean mOpenLuckyClicked;      // 拆红包是否点击了
    private boolean mIsAutoModel;           // 自动模式

    @Override
    public synchronized void onAccessibilityEvent(AccessibilityEvent event) {
        final int eventType = event.getEventType();

        if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            LogUtils.log("onAccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED");
            unlockScreen();
            // mLuckyClicked = false;

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

                        }
                        break;
                    }
                }
            }
        } else if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // LogUtils.log("onAccessibilityEvent.TYPE_WINDOW_STATE_CHANGED");
            if (!Const.isHaveNoPerson && mIsAutoModel) {
                mIsAutoModel = false;
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                return;
            }
            mOpenLuckyClicked = false;
            run(event);
        } else if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            // LogUtils.log("MonitorServiceJump.TYPE_WINDOW_CONTENT_CHANGED");
            run(event);
        }

    }

    @SuppressWarnings("deprecation")
    private void unlockScreen() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "YangJ:wakeLock");
            if (wakeLock != null) {
                wakeLock.acquire(24 * 60 * 60 * 1000);
                wakeLock.release();
            }
        }
    }

    private void run(AccessibilityEvent event) {
        // LogUtils.log("MonitorServiceJump.run");
        AccessibilityNodeInfo nodeInfo = event.getSource();

        if (null != nodeInfo) {
            mNodeInfoList.clear();
            traverseNode(nodeInfo);
            if (mContainsLucky) {
                // LogUtils.log("MonitorServiceJump.mContainsLucky");
                int size = mNodeInfoList.size();
                if (size > 0) {
                    mContainsLucky = false;
                    mOpenLuckyClicked = false;
                    AccessibilityNodeInfo cellNode = mNodeInfoList.get(size - 1);
                    if (cellNode != null) {
                        cellNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }
            }
            if (mContainsOpenLucky && !mOpenLuckyClicked) {
                mOpenLuckyClicked = true;
                // LogUtils.log("MonitorServiceJump.mContainsOpenLucky");
                int size = mNodeInfoList.size();
                if (size > 0) {
                    if (null == nodeInfo) {
                        return;
                    }

                    // 播放提示
                    MainActivity.playSound();

                    AccessibilityNodeInfo cellNode = mNodeInfoList.get(size - 1);
                    cellNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    mContainsOpenLucky = false;
                    mIsAutoModel = true;
                }
            }
            if (mLuckyInfo) {
                // ("MonitorServiceJump.mLuckyInfo");
                if (mIsAutoModel) {

                }
                mLuckyInfo = false;
            }
        }

    }

    private final int NONE = 0;
    private final int WX_RED = 1;
    private final int WX_GONE = 2;

    private int traverseNode(AccessibilityNodeInfo node) {
        if (null == node) return NONE;
        // LogUtils.log("MonitorService.deep" + deep);

        final int count = node.getChildCount();
        if (count > 0) {
            boolean isHaveRED = false;
            boolean isHaveGone = false;
            for (int i = 0; i < count; ++i) {
                AccessibilityNodeInfo childNode = node.getChild(i);
                int result = traverseNode(childNode);
                if (result == WX_RED) {
                    isHaveRED = true;
                }
                if (result == WX_GONE) {
                    isHaveGone = true;
                }
            }

            if (!isHaveGone && isHaveRED) {
                mContainsLucky = true;
                mNodeInfoList.add(node);
            }
        } else {
            CharSequence text = node.getText();
            if (null != text && text.length() > 0) {
                String str = text.toString();
                LogUtils.log("MonitorService." + str);

                // 添加判断“開”的逻辑
                if (str.contains("看看大家的手气")) {
                    if (node.getParent() != null && node.getParent().getParent() != null) {
                        AccessibilityNodeInfo parent = node.getParent().getParent();
                        if (parent.getChildCount() > 3 && parent.getChild(2) != null) {
                            AccessibilityNodeInfo openButton = parent.getChild(2);
                            LogUtils.log("ClassName -> " + openButton.getClassName());
                            mContainsOpenLucky = true;
                            mNodeInfoList.add(openButton);
                        }
                    }
                } else if (str.contains("领取红包") || (Const.isNeedOpenSelf && str.contains("查看红包"))) {
                    mContainsLucky = true;
                    mNodeInfoList.add(node.getParent());
                } else if (str.contains("微信红包")) {
                    return WX_RED;
                } else if (str.contains("已过期") || str.contains("已被领完") || str.contains("已领取")) {
                    return WX_GONE;
                } else if (str.contains("红包详情") || str.contains("手慢了")) {
                    mLuckyInfo = true;
                }
            }
            // LogUtils.log("MonitorService11." + node);
        }
        return NONE;
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
        LogUtils.log("Const.isOpen=." + Const.isOpen);
        super.onServiceConnected();
    }

    @Override
    public void onInterrupt() {
        Const.isOpen = false;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Const.isOpen = false;
        LogUtils.log("Const.isOpen=" + Const.isOpen);
        return super.onUnbind(intent);
    }
}
