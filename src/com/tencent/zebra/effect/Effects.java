package com.tencent.zebra.effect;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.tencent.photoplus.R;
import com.tencent.ttpic.sdk.util.IntentUtils;
import com.tencent.ttpic.sdk.util.Pitu;
import com.tencent.zebra.util.ZebraProgressDialog;

import java.util.ArrayList;
import java.util.logging.Handler;

/**
 * Version 1.0
 * <p/>
 * <p/>
 * Date: 2014-09-24 18:32
 * Author: retryu
 * <p/>
 * <p/>
 * Copyright © 1998-2014 Tencent Technology (Shenzhen) Company Ltd.
 */
public abstract class Effects {


    public static final int SHOW_DLG = 0X1000;
    public static final int DISMISS_DLG = 0X1001;
    public static final int JUMP_TO_PITU = 0X1002;
    public  static boolean  saving=false;
    private PhotoEffectActivity mActivity;
    private ViewGroup mContain;
    public String mPath;
    public ConfirmBarListenner comfirmBarListenner;
    public Bitmap effectBitmap;
    public boolean isProcessing;
    public UIHandler uiHandler;
    public boolean hasEdit;

    public ProgressDialog mProgressDialog;

    //   tab切换暂时不需要显示载入dialog
    public class UIHandler extends android.os.Handler {

        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            switch (msg.what) {
                case SHOW_DLG:
                    mProgressDialog = ZebraProgressDialog.show(mActivity, null,
                            mActivity.getResources().getString(R.string.zebra_dealing), true, false);
                    break;
                case DISMISS_DLG:
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                    }
                    break;
                case JUMP_TO_PITU:
                    ArrayList<Uri> files = new ArrayList<Uri>();
                    files.add(Uri.parse(mPath));
                    String path = (String) msg.obj;
                    Log.e("debug","JUMP_TO_PITU path: "+path);
                    if (path != null) {
                        Intent intent = IntentUtils.buildPituIntent(files, Pitu.Modules.SUB_MODULE_EDITOR_FILTER, Uri.parse(path));
                        mActivity.startActivityForResult(intent, mActivity.REQ_TO_PITU);
                    }
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                    }
            }
        }
    }


    public Effects(PhotoEffectActivity photoEffectActivity, ViewGroup containenr) {
        this.mActivity = photoEffectActivity;
        this.mContain = containenr;
        isProcessing = true;
        uiHandler = new UIHandler();
    }

    public void showEffect() {

    }

    public void hide() {
    }

    public void save() {

    }

    public void initEffect() {
    }


    public void recyleImage() {

    }

    public static interface ConfirmBarListenner {
        public void onConfrimClick(View v);

        public void onCancelClcick(View v);
    }

    public void confirm(View btnConfrime) {

    }

    public void cancel(View btnCancel) {

    }

    public Bitmap getEffectBitmap() {
        return effectBitmap;
    }

    public boolean isProcessing() {
        return isProcessing;
    }


}
