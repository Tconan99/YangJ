package com.jc.yangj;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

/**
 * Created by xueep on 15/12/00.
 */
public class MonitorService extends AccessibilityService {
    private ArrayList<AccessibilityNodeInfo> mNodeInfoList = new ArrayList<AccessibilityNodeInfo>();

    private boolean mLuckyClicked;	//红包是否点击了
    private boolean mContent;		//内容页
    private boolean mContainsLucky; //有红包
    private boolean mContainsOpenLucky; //拆红包
    private boolean mIsAuto; 		// 是否是自动操作
    private boolean mIsNeedBack; 		// 是否是自动操作
    //private Date mLastClickTime;
    private boolean mIsLock = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        final int eventType = event.getEventType();

        if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            Log.i("111", "TYPE_NOTIFICATION_STATE_CHANGED");
            unlockScreen();
            mLuckyClicked = false;

            Notification notification = (Notification) event.getParcelableData();
            List<String> textList = getText(notification);
            if (null != textList && textList.size() > 0) {
                for (String text : textList) {
                    if (!TextUtils.isEmpty(text) && text.contains("[微信红包]")) {
                        final PendingIntent pendingIntent = notification.contentIntent;
                        try {
                            Log.i("MonitorService", "pendingIntent");
                            pendingIntent.send();
                        } catch (PendingIntent.CanceledException e) {
                        }
                        break;
                    }
                }
            }
        }
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.i("MonitorService", "TYPE_WINDOW_STATE_CHANGED");

            if (Const.isHaveNoPerson && mIsNeedBack) {
                mIsNeedBack = false;
                Log.i("1111", "232323232");
                //performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                return;
            }

            run(event);
        }
        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            Log.i("MonitorService", "TYPE_WINDOW_CONTENT_CHANGED");
            if (Const.isNeedInPage) {
                //mLuckyClicked = false;
                run(event);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void unlockScreen() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, "MyWakeLock");

        wakeLock.acquire();
        wakeLock.release();
    }

    private void run(AccessibilityEvent event) {

        if(mIsLock) {
            return;
        }
        mIsLock = true;

//    	if (mLastClickTime != null) {
//    		long now = new Date().getTime();
//    		long last = mLastClickTime.getTime();
//    		if (now-last<3000) {
//    			mIsLock = false;
//    			return;
//    		}
//    	}

        AccessibilityNodeInfo nodeInfo = event.getSource();

//    	while (nodeInfo!=null && nodeInfo.getParent()!=null) {
//    		nodeInfo = nodeInfo.getParent();
//    	}


        if (null != nodeInfo) {
            mNodeInfoList.clear();
            traverseNode(nodeInfo);
            if (mContainsLucky && (!mLuckyClicked||false)) {
                int size = mNodeInfoList.size();
                if (size > 0) {
                    /** step1: get the last hongbao cell to fire click action */
                    AccessibilityNodeInfo cellNode = mNodeInfoList.get(size - 1);
                    cellNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    mContainsLucky = false;
                    mLuckyClicked = true;
                    //mIsAuto = true;
                }
            }
            if (mContainsOpenLucky) {
                int size = mNodeInfoList.size();
                if (size > 0) {
                    /** step2: when hongbao clicked we need to open it, so fire click action */
                    AccessibilityNodeInfo cellNode = mNodeInfoList.get(size - 1);
                    cellNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    mContainsOpenLucky = false;
                    mIsAuto = true;

                    // 播放提示
                    MainActivity.playSound();
                }
            }
            if (mContent) {
                if (mIsAuto) {
                    mIsAuto = false;
                    //mLastClickTime = new Date();
                    if (false && Const.isHaveNoPerson) {
                        performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
                    } else {
                        mIsNeedBack = true;
                        performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                    }

                }
                //performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                mContent = false;
            }
        }

        mIsLock = false;
    }

    @SuppressWarnings("unused")
    private void findView(AccessibilityNodeInfo node) {
        if (null == node) return;

        // 别人的红包
        List<AccessibilityNodeInfo> listGet = node.findAccessibilityNodeInfosByText("领取红包");
        if (listGet!=null) {
            mContainsLucky = true;
            mNodeInfoList.addAll(listGet);
        }

        // 自己的红包
        List<AccessibilityNodeInfo> listLook = node.findAccessibilityNodeInfosByText("查看红包");
        if (listGet!=null && Const.isNeedOpenSelf) {
            mContainsLucky = true;
            mNodeInfoList.addAll(listLook);
        }

        // 拆红包
        List<AccessibilityNodeInfo> listOpen = node.findAccessibilityNodeInfosByText("拆红包");
        if (listOpen!=null) {
            mContainsOpenLucky = true;
            mNodeInfoList.add(node);
        }

        // 红包详情
        List<AccessibilityNodeInfo> listInfo = node.findAccessibilityNodeInfosByText("红包详情");
        if (listInfo!=null) {
            mContent = true;
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

            // 添加判断“檫”的逻辑
            if (node!=null && node.getParent()!=null) {
                int childNumber = node.getParent().getChildCount();
                try {
                    if (childNumber>2 && "android.widget.Button".equals(node.getClassName())) {
                        String text1 = node.getParent().getChild(1).getText().toString();
                        //String text5 = node.getParent().getChild(5).getChild(0).getText().toString();
                        if (text1.contains("发了一个红包")) {
                            //if (text1.contains("发了一个红包") && text5.equals("看看大家的手气")) {
                            mContainsOpenLucky = true;
                            mNodeInfoList.add(node);
                            //Log.i("childNumber", String.valueOf(childNumber));
                        }
                    }
                } catch (Exception e) {

                }
            }

            CharSequence text = node.getText();
            if (null != text && text.length() > 0) {
                String str = text.toString();
                Log.i("MonitorService", str);

                if (str.contains("领取红包") || (Const.isNeedOpenSelf && str.contains("查看红包")) ) {
                    mContainsLucky = true;
                    mNodeInfoList.add(node.getParent());
                }

                if (str.contains("拆红包")) {
                    mContainsOpenLucky = true;
                    mNodeInfoList.add(node);
                }

                if (str.contains("你领取了") || str.contains("你的红包已被领完")) {
                    Log.i("MonitorService", str);
                    mContainsLucky = false;
                    mContainsOpenLucky = false;
                    mNodeInfoList.clear();
                }

                if (str.contains("红包详情") || str.contains("手慢了")) {
                    mContent = true;
                }
            }
        }
    }



    public List<String> getText(Notification notification) {
        if (null == notification) return null;

        RemoteViews views = notification.bigContentView;
        if (views == null) views = notification.contentView;
        if (views == null) return null;

        List<String> text = new ArrayList<String>();
        try {
            Field field = views.getClass().getDeclaredField("mActions");
            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            ArrayList<Parcelable> actions = (ArrayList<Parcelable>) field.get(views);

            // Find the setText() and setTime() reflection actions
            for (Parcelable p : actions) {
                Parcel parcel = Parcel.obtain();
                p.writeToParcel(parcel, 0);
                parcel.setDataPosition(0);

                // The tag tells which type of action it is (2 is ReflectionAction, from the source)
                int tag = parcel.readInt();
                if (tag != 2) continue;

                // View ID
                parcel.readInt();

                String methodName = parcel.readString();
                if (null == methodName) {
                    continue;
                } else if (methodName.equals("setText")) {
                    // Parameter type (10 = Character Sequence)
                    parcel.readInt();

                    // Store the actual string
                    String t = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel).toString().trim();
                    text.add(t);
                }
                parcel.recycle();
            }
        } catch (Exception e) {
        }

        return text;
    }

    @Override
    protected void onServiceConnected() {
        Const.isOpen = true;
        Log.i("11", "Const.isOpen=" + Const.isOpen);
        super.onServiceConnected();
    }

    @Override
    public void onInterrupt() {
        Const.isOpen = false;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Const.isOpen = false;
        Log.i("11", "Const.isOpen=" + Const.isOpen);
        return super.onUnbind(intent);
    }
}
