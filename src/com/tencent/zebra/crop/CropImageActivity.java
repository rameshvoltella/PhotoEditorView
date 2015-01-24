/* 版权声明：腾讯科技版权所有
 * Copyright(C)2008-2013 Tencent All Rights Reserved
 * PictureCropActivity.java
 * classes : com.tencent.zebra.ui.crop.PictureCropActivity
 * @author 邬振海
 * V 1.0.0
 * Create at 2013-4-3 下午5:28:09
 */

package com.tencent.zebra.crop;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsoluteLayout;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.tencent.photoplus.R;
import com.tencent.zebra.doodle.DoodleActivity;
import com.tencent.zebra.editutil.Util;
import com.tencent.zebra.editutil.Util.Size;
import com.tencent.zebra.ui.ZebraBaseActivity;
import com.tencent.zebra.util.ReportUtils;
import com.tencent.zebra.util.ZebraCustomDialog;
import com.tencent.zebra.util.ZebraProgressDialog;
import com.tencent.zebra.util.log.ZebraLog;

import cooperation.zebra.ZebraPluginProxy;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

public class CropImageActivity extends ZebraBaseActivity implements OnClickListener {
    private static final String TAG = "CropImageActivity";
    // private static final int MASK_FOUR_THREE = 0;
    // private static final int MASK_THREE_FOUR = 1;
    // private static final int MASK_MARGIN = 10;

    private static final int REQ_JUMPTODOODLE = 10001;
    public static final int RES_CANCELFROMDOODLE = -1;
    public static final int RES_OKFROMDOODLE = 1;

    private String mPath;
    // private String nickname;
    // private String uin;
    private String picDimenStr = null;
    private String suffix;
    // private Bitmap bitmapContent;
    // private ImageView ivMain;
    private Button btnCancel;
    private Button btnConfirm;
    private ImageButton ibRotateRight;
    private ImageButton ibRotateLeft;
    // private ImageButton ibRatio;
    // private ScaleGestureDetector mScaleGestureDetector;

    // private int scale_src_point_x = 0;
    // private int scale_src_point_y = 0;
    // private int scale_bmp_src_w = 0;
    // private int scale_bmp_src_h = 0;
    // private int scale_iv_src_w = 0;
    // private int scale_iv_src_h = 0;
    // private float scale_w_size = 0;
    // private float scale_h_size = 0;
    //
    // private int screen_w = 0;
    // private int screen_h = 0;
    // private int bitmapWidth;
    // private int bitmapHeight;

    // private HighlightView hv;
    private AbsoluteLayout ivParent;
    // private int pWidth = 0;
    // private int pHeight = 0;
    // private int currentMask;
    public static MyHandler handler;
    // private Rect innerRect;
    private CropImageView myImageView;

    private Bitmap mSelBitmap;

    private ProgressDialog mProgressDialog;
    private static final int SHOW_DLG = 0X1000;
    private static final int DISMISS_DLG = 0X1001;

    private static final int SAVE_ACTION = 0X1002;
    // 检查图片是否可以用
    private static final int CHECK_BMP_EMPTY = 0X1003;
    // Show toast
    private static final int SHOW_TOAST = 0X1004;

    private static final int SHOW_LOADING_DLG = 0X1005;

    private static final int SAVE_AND_JUMP_DOODLE = 0X1006;

    public static final int HIDE_VIEW_IN_DOODLE = 0X1007;

    private static final int SHOW_JUMP_DOODLE_DLG = 0X1008;

    boolean hasEdit = false;

    private boolean didRotateHappen = false;
    private int cutButtonClickTimes = 0;

    // TODO LouisPeng 这种做法太坑爹了，DoodleActivity接收到之后一定要手动设为null
    public static Bitmap bitmapForDoodle = null;

    public static class MyHandler extends Handler {
        // WeakReference to the outer class's instance.
        private WeakReference<CropImageActivity> mOuter;

        public MyHandler(CropImageActivity activity) {
            mOuter = new WeakReference<CropImageActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final CropImageActivity outer = mOuter.get();
            if (outer != null) {
                if (msg.what == 0) {
                    try {
                        outer.initUI();
                        if (outer.mSelBitmap == null) {
                            handler.sendEmptyMessage(DISMISS_DLG);
                            outer.finish();
                        } else {
                            outer.myImageView = new CropImageView(outer, outer.mSelBitmap, outer.mPath);
                            outer.ivParent.addView(outer.myImageView);
                            handler.sendEmptyMessage(DISMISS_DLG);
                        }
                    } catch (OutOfMemoryError error) {
                        // CameraActivity.SHOULD_KILL_PROCESS = true;
                        try {
                            Toast.makeText(outer.getThisActivity(),
                                     outer.getResources().getString(R.string.zebra_not_enough_memory),
                                    Toast.LENGTH_LONG).show();
                        } catch (Exception e2) {
                            // TODO 插件很奇怪，这里可能会出现Exception，暂时没有时间跟踪，先备注
                            ZebraLog.e(TAG, "MyHandler.handleMessage", e2);
                        }
                        error.printStackTrace();
                    } catch (Exception e) {
                        try {
                            Toast.makeText(outer.getThisActivity(),
                                    outer.getResources().getString(R.string.zebra_not_find_picture),
                                    Toast.LENGTH_LONG).show();
                        } catch (Exception e2) {
                            // TODO 插件很奇怪，这里可能会出现Exception，暂时没有时间跟踪，先备注
                            ZebraLog.e(TAG, "MyHandler.handleMessage", e2);
                        }
                        e.printStackTrace();
                    }
                } else if (msg.what == SHOW_DLG) {
                    outer.mProgressDialog = ZebraProgressDialog.show(outer.getThisActivity(), null,
                            outer.getResources().getString(R.string.zebra_dealing), true, false);
                } else if (msg.what == SHOW_JUMP_DOODLE_DLG) {
                    outer.mProgressDialog = ZebraProgressDialog.show(outer.getThisActivity(), null,
                            outer.getResources().getString(R.string.zebra_loading), true, false);
                } else if (msg.what == DISMISS_DLG) {
                    if (outer.mProgressDialog != null) {
                        outer.mProgressDialog.dismiss();
                    }
                } else if (msg.what == SHOW_LOADING_DLG) {
                    outer.mProgressDialog = ZebraProgressDialog.show(outer.getThisActivity(), null,
                            outer.getResources().getString(R.string.zebra_loading), true, false);
                } else if (msg.what == SAVE_ACTION) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final CropImageActivity outer = mOuter.get();
                            if (outer != null) {
                                String picpath = outer.onSaveClicked();
                                String picDimenStr = "unkown";
                                CropImageView imageView = outer.myImageView;
                                if (null != imageView) {
									synchronized (imageView) {
										picDimenStr = imageView.getSavedWidth() + "x" + imageView.getSavedHeight();
										imageView.recycleImage();
									}
									outer.myImageView = null;
                                }

                                handler.sendEmptyMessage(DISMISS_DLG);
                                ZebraLog.d(TAG, "PictureCropActivity onClick crop_confirm picpath=" + picpath);
                                // fix tapd 48711520
                                if (TextUtils.isEmpty(picpath)) {
                                    // CropImageActivity.this.setResult(RESULT_CANCELED);
                                    // CropImageActivity.this.finish();
                                    outer.setResultCancel();
                                } else {

                                    ReportUtils.report(
                                            "CliOper", "", "", "Pic_edit", "Send_cut", 0, 0,
                                            "" + outer.cutButtonClickTimes, "",
                                            "", picDimenStr);

                                    // Intent it = new Intent();
                                    // it.putExtra("image_path", picpath);//修改路径
                                    // CropImageActivity.this.setResult(RESULT_OK, it);
                                    // CropImageActivity.this.finish();
                                    // Intent intent = new Intent(getOutActivity(), DemoActivity.class);
                                    // intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    // | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    // startActivity(intent);
                                    ArrayList<String> paths = new ArrayList<String>();
                                    paths.add(picpath);
                                    ZebraPluginProxy.sendPhotoForPhotoPlus(outer.getOutActivity(), outer.getIntent(),
                                            paths);
                                }
                            }
                        }
                    }).start();
                } else if (msg.what == SAVE_AND_JUMP_DOODLE) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final CropImageActivity outer = mOuter.get();
                            if (null != outer) {
                            	final CropImageView imageView = outer.myImageView;
								if (null != imageView) {
									synchronized (imageView) {
										// String picpath = onSaveClicked();
										// picDimenStr = myImageView.getSavedWidth() + "x" +
										// myImageView.getSavedHeight();
										//
										// handler.sendEmptyMessage(DISMISS_DLG);
										// ZebraLog.d(TAG, "PictureCropActivity onClick crop_confirm picpath="+picpath);
										Intent intent = new Intent(outer.getThisActivity(), DoodleActivity.class);
										intent.putExtra("isfromCrop", true);
										// intent.putExtra("image_path", picpath);
										// intent.putExtra("image_bitmap", myImageView.getBitmap());
										bitmapForDoodle = imageView.getCropBitmap();

										imageView.recycleImage();
										outer.myImageView = null;

										// 记录来源
										intent.putExtra("market", "photo");
										// ZebraLog.d(TAG,
										// "PictureCropActivity onClick crop_confirm nickname="+nickname);
										// intent.putExtra("self_nick", nickname);
										ZebraLog.d(TAG, "PictureCropActivity onClick crop_confirm hasEdit="
										        + outer.hasEdit);
										intent.putExtra("hasEdit", Boolean.valueOf(outer.hasEdit));
										// if(uin!=null){
										// intent.putExtra("qq", Long.parseLong(uin));
										// }

										outer.startActivityForResult(DoodleActivity.class, intent, REQ_JUMPTODOODLE);
										outer.overridePendingTransition(0, 0);
									}
								}
                            }
                        }
                    }).start();
                } else if (msg.what == HIDE_VIEW_IN_DOODLE) {
                    ZebraLog.d("zebra", "CropImageActivity handler handleMessage msg.what == HIDE_VIEW_IN_DOODLE");
                    outer.setContentView(R.layout.zebra_crop_activity_hide);
                } else if (msg.what == CHECK_BMP_EMPTY) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final CropImageActivity outer = mOuter.get();
                            if (null != outer) {
                                try {
                                    Size size = Util.getBmpSize(outer.mPath);
                                    if (size.height < 64 || size.width < 64) {
                                        Message msg = handler.obtainMessage(SHOW_TOAST);
                                        msg.arg1 = R.string.zebra_smallpic_tip;
                                        msg.sendToTarget();
                                        handler.removeMessages(0);
                                        outer.finish();
                                        return;
                                    }
                                    int[] result = new int[1];
                                    outer.mSelBitmap = Util.getOrResizeBitmap(outer.mPath, true, result);
                                    // outer.mSelBitmap = Util.getBitmap(outer, outer.mPath);
                                    if (outer.mSelBitmap == null) {
                                        Message msg = handler.obtainMessage(SHOW_TOAST);
                                        if (result[0] == -2) {
                                            msg.arg1 = R.string.zebra_not_enough_memory;
                                        } else {
                                            msg.arg1 = R.string.zebra_file_not_exsit_or_unvalid;
                                        }
                                        msg.sendToTarget();
                                        handler.removeMessages(0);
                                        outer.finish();
                                    } else {
                                        handler.sendEmptyMessage(0);
                                    }
                                } catch (OutOfMemoryError error) {
                                    Message msg = handler.obtainMessage(SHOW_TOAST);
                                    msg.arg1 = R.string.zebra_not_enough_memory;
                                    msg.sendToTarget();
                                    outer.finish();
                                    ZebraLog.e("zebra", "OOM error.", error);
                                } catch (Exception e) {
                                    Message msg = handler.obtainMessage(SHOW_TOAST);
                                    msg.arg1 = R.string.zebra_not_find_picture;
                                    msg.sendToTarget();
                                    outer.finish();
                                    ZebraLog.e("zebra", "error.", e);
                                }
                            }
                        }
                    }).start();
                } else if (msg.what == SHOW_TOAST) {
                    int resid = msg.arg1;
                    try {
                        Toast.makeText(outer.getThisActivity(), outer.getString(resid), Toast.LENGTH_LONG).show();
                    } catch (Exception e2) {
                        // TODO 插件很奇怪，这里可能会出现Exception，暂时没有时间跟踪，先备注
                        ZebraLog.e(TAG, "MyHandler.handleMessage", e2);
                    }
                }

            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ReportUtils.report("CliOper", "", "", "Pic_edit", "Clk_cut", 0, 0, "", "", "", "");

        mPath = getIntent().getStringExtra("image_path");
        // nickname = getIntent().getStringExtra("self_nick");
        // uin = ""+getIntent().getExtras().getLong("qq", 0);
        // ZebraLog.d(TAG, "CropImageActivity onCreate uin="+uin);

        if (!TextUtils.isEmpty(mPath)) {
            ZebraLog.d("PictureCropActivity", "PictureCropActivity onCreate path.s=" + mPath);
        } else {
            ZebraLog.d("PictureCropActivity", "PictureCropActivity onCreate path = null");
            return;
        }

        initHandler();
        // check bmp available.
        handler.sendEmptyMessage(SHOW_LOADING_DLG);
        handler.sendEmptyMessageDelayed(CHECK_BMP_EMPTY, 200);

        int index = mPath.lastIndexOf('.');
        suffix = mPath.substring(index + 1);

        // init();

        cutButtonClickTimes = 0;
    }

    private void initUI() {
        setContentView(R.layout.zebra_crop_activity);
        findView();
        setClick();
    }

    private void findView() {
        btnCancel = (Button) findViewById(R.id.crop_cancel);
        btnConfirm = (Button) findViewById(R.id.crop_confirm);
        ibRotateRight = (ImageButton) findViewById(R.id.btn_doodle);
        ibRotateLeft = (ImageButton) findViewById(R.id.btn_rotate);
        // ibRatio = (ImageButton) findViewById(R.id.ratio);
        ivParent = (AbsoluteLayout) findViewById(R.id.image_parent);
    }

    private void setClick() {
        btnCancel.setOnClickListener(this);
        btnConfirm.setOnClickListener(this);
        ibRotateRight.setOnClickListener(this);
        ibRotateLeft.setOnClickListener(this);
        // ibRatio.setOnClickListener(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        ZebraLog.d("CropImageActivity", "CropImageActivity onActivityResult requestCode = " + requestCode
                + ";resultCode = " + resultCode);
        // if(isWaitForResult) {
        // PluginProxy.onSendResult(getOutActivity(), requestCode, resultCode, data, false);
        // return;
        // }
        switch (requestCode) {
        case REQ_JUMPTODOODLE: { // 水印相机返回
            switch (resultCode) {
            case RESULT_CANCELED: {
                recycleImage();
                // CropImageActivity.this.setResult(RESULT_CANCELED);
                // CropImageActivity.this.finish();
                setResultCancel();
            }
                break;
            case RESULT_OK: {
                String picpath = null;
                if (data != null) {
                    picpath = data.getStringExtra("image_path");
                }
                if (TextUtils.isEmpty(picpath)) {
                    // CropImageActivity.this.setResult(RESULT_CANCELED);
                    // CropImageActivity.this.finish();
                    setResultCancel();
                } else {
                    if (picDimenStr == null) {
                        picDimenStr = "1x1";
                    }
                    ReportUtils.report(
                            "CliOper", "", "", "Pic_edit", "Send_cut", 0, 0, "" + cutButtonClickTimes, "", "",
                            picDimenStr);

                    // Intent it = new Intent();
                    // it.putExtra("image_path", picpath);//修改路径
                    // CropImageActivity.this.setResult(RESULT_OK, it);
                    // CropImageActivity.this.finish();
                    ArrayList<String> paths = new ArrayList<String>();
                    paths.add(picpath);
                    ZebraPluginProxy.sendPhotoForPhotoPlus(getOutActivity(), getIntent(), paths);
                }
            }
                break;
            default: {
                recycleImage();
                // CropImageActivity.this.setResult(RESULT_CANCELED);
                // CropImageActivity.this.finish();
                setResultCancel();
            }
                break;
            }
        }
            break;
        }
    }

    private void initHandler() {
        handler = new MyHandler(this);
    }

    // private void init() {
    // ivParent.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
    // boolean isFirst = true;//默认调用两次，这里只让它执行一次回调
    // @Override
    // public void onGlobalLayout() {
    // if (isFirst) {
    // isFirst = false;
    // if(null != handler)
    // handler.sendEmptyMessage(0);
    // }
    // }
    // });
    // }

    public int getImageViewWidth() {
        if (null != ivParent)
            return ivParent.getWidth();
        else
            return 0;
    }

    public int getImageViewHeight() {
        if (null != ivParent)
            return ivParent.getHeight();
        else
            return 0;
    }

    @Override
    public void onDestroy() {
        // myImageView.recycleImage();
        try {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
        } catch (Exception e) {
            // dialog的dismiss有可能发生BadWindowToken，需要小心
            e.printStackTrace();
        }
        recycleImage();
        super.onDestroy();
    }

    private void recycleImage() {
        if (myImageView != null) {
            synchronized (myImageView) {
                myImageView.recycleImage();
                myImageView = null;
            }
        }
        System.gc();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.crop_cancel: {
            if (hasEdit || btnConfirm.getText().equals(getResources().getString(R.string.zebra_cut_pic))) {
                showWarningDialog();
            } else {
                recycleImage();
                // CropImageActivity.this.setResult(RESULT_CANCELED);
                // CropImageActivity.this.finish();
                setResultCancel();
            }
            break;
        }
        case R.id.crop_confirm: {

            hasEdit = true;
            if (myImageView == null) {
                // CropImageActivity.this.setResult(RESULT_CANCELED);
                // CropImageActivity.this.finish();
                setResultCancel();
                break;
            }

            if (btnConfirm.getText().equals(getResources().getString(R.string.zebra_cut_pic))) {
                ++cutButtonClickTimes;
                myImageView.setInitalAngle(Util.getExifDegree(mPath));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1
                        && !suffix.toLowerCase(Locale.US).equals("gif") && !suffix.toLowerCase(Locale.US).equals("bmp")) {
                    int[] result = new int[1];
                    ZebraLog.e(TAG, "[crop_confirm]  crop  start");
                    Bitmap bm = loadRectFromOrigin(result);
                    if (result[0] == -1) {
                        ZebraLog.e(TAG, "[crop_confirm]  crop  faild");
                        Toast.makeText(getApplicationContext(), getString(R.string.zebra_crop_failed),
                                Toast.LENGTH_LONG).show();
                        break;
                    } else if (result[0] == -2) {
                        ZebraLog.e(TAG, "[crop_confirm]  crop  oom");
                        Toast.makeText(getApplicationContext(), getString(R.string.zebra_not_enough_memory),
                                Toast.LENGTH_LONG).show();
                        break;
                    } else if (result[0] == -3) {
                        ZebraLog.e(TAG, "[crop_confirm]  crop not  support");
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.zebra_crop_unsupport_fileformat_failed),
                                Toast.LENGTH_LONG).show();
                        break;
                    }
                    ZebraLog.e(TAG, "[crop_confirm]  crop  sucessed");
                    myImageView.setBitmap(bm);
                } else {
                    int[] result = new int[1];
                    myImageView.saveImage2Bitmap(result);
                    if (result[0] == -1) {
                        Toast.makeText(getApplicationContext(), getString(R.string.zebra_crop_failed),
                                Toast.LENGTH_LONG).show();
                        break;
                    } else if (result[0] == -2) {
                        Toast.makeText(getApplicationContext(), getString(R.string.zebra_not_enough_memory),
                                Toast.LENGTH_LONG).show();
                        break;
                    }
                }
                setRightBottomBtnText(R.string.zebra_send_pic);
            } else {
                if (myImageView.isConfirmAvailable()) {

                    handler.sendEmptyMessage(SHOW_DLG);
                    // 加载本地图片LOCAL_PHOTO
                    // DataReport.getInstance().report(ReportInfo.create(ReportConfig.OPL1_PHOTO_SOURCE,
                    // ReportConfig.OPL2_LOCAL_PHOTO));

                    handler.sendEmptyMessageDelayed(SAVE_ACTION, 100);
                    // handler.sendEmptyMessageDelayed(DISMISS_DLG, 100);

                }
            }
            break;
        }
        case R.id.btn_doodle: {
            hasEdit = true;
            if (myImageView == null) {
                // CropImageActivity.this.setResult(RESULT_CANCELED);
                // CropImageActivity.this.finish();
                setResultCancel();
                break;
            }
            else {
                if (btnConfirm.getText().equals(getResources().getString(R.string.zebra_cut_pic))) {
                    ++cutButtonClickTimes;
                    myImageView.setInitalAngle(Util.getExifDegree(mPath));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1
                            && !suffix.toLowerCase(Locale.US).equals("gif")
                            && !suffix.toLowerCase(Locale.US).equals("bmp")) {
                        int[] result = new int[1];
                        Bitmap bm = loadRectFromOrigin(result);
                        if (result[0] == -1) {
                            Toast.makeText(getApplicationContext(), getString(R.string.zebra_crop_failed),
                                    Toast.LENGTH_LONG).show();
                            break;
                        } else if (result[0] == -2) {
                            Toast.makeText(getApplicationContext(), getString(R.string.zebra_not_enough_memory),
                                    Toast.LENGTH_LONG).show();
                            break;
                        } else if (result[0] == -3) {
                            Toast.makeText(getApplicationContext(),
                                    getString(R.string.zebra_crop_unsupport_fileformat_failed),
                                    Toast.LENGTH_LONG).show();
                            break;
                        }
                        myImageView.setBitmap(bm);
                    } else {
                        int[] result = new int[1];
                        myImageView.saveImage2Bitmap(result);
                        if (result[0] == -1) {
                            Toast.makeText(getApplicationContext(), getString(R.string.zebra_crop_failed),
                                    Toast.LENGTH_LONG).show();
                            break;
                        } else if (result[0] == -2) {
                            Toast.makeText(getApplicationContext(), getString(R.string.zebra_not_enough_memory),
                                    Toast.LENGTH_LONG).show();
                            break;
                        }
                    }
                    setRightBottomBtnText(R.string.zebra_send_pic);
                }

                if (myImageView.isConfirmAvailable()) {
                    handler.sendEmptyMessage(SHOW_JUMP_DOODLE_DLG);
                    // 加载本地图片LOCAL_PHOTO
                    // DataReport.getInstance().report(ReportInfo.create(ReportConfig.OPL1_PHOTO_SOURCE,
                    // ReportConfig.OPL2_LOCAL_PHOTO));

                    handler.sendEmptyMessageDelayed(SAVE_AND_JUMP_DOODLE, 100);
                    // handler.sendEmptyMessageDelayed(DISMISS_DLG, 100);
                }
                break;
            }
        }
        case R.id.btn_rotate: {
            hasEdit = true;
            if (myImageView == null) {
                // CropImageActivity.this.setResult(RESULT_CANCELED);
                // CropImageActivity.this.finish();
                setResultCancel();
                break;
            } else {
                didRotateHappen = true;
                if (!myImageView.isRotating())
                    myImageView.rotate(90);
                break;
            }
        }
        // case R.id.ratio: {
        // // fix tapd 48711497
        // if(myImageView == null) {
        // CropImageActivity.this.setResult(RESULT_CANCELED);
        // CropImageActivity.this.finish();
        // break;
        // } else {
        // myImageView.changeMaskType();
        // setRatioIcon();
        // break;
        // }
        // }
        }
    }

    private Rect resultRect;

    /**
     * @param result 输出结果，0成功，-1失败，-2内存不足, -3格式不支持
     * @return
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
    private Bitmap loadRectFromOrigin(int[] result) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
            BitmapRegionDecoder decoder = null;
            try {
                decoder = BitmapRegionDecoder.newInstance(mPath, true);
            } catch (IOException e) {
                ZebraLog.e(TAG, "[loadRectFromOrigin]", e);
                if (null != result) {
                    result[0] = -3;
                }
            }
            if (null != decoder) {
                float[] margin = myImageView.getCropMargin();
                if (resultRect == null) {
                    resultRect = new Rect();
                    resultRect.top = 0;
                    resultRect.left = 0;
                    resultRect.right = decoder.getWidth();
                    resultRect.bottom = decoder.getHeight();
                }
                int width = resultRect.width();
                int height = resultRect.height();
                resultRect.top = (int) (resultRect.top + height * margin[1]);
                resultRect.bottom = (int) (resultRect.bottom - height * margin[3]);
                resultRect.left = (int) (resultRect.left + width * margin[0]);
                resultRect.right = (int) (resultRect.right - width * margin[2]);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = getSampleSize(resultRect);
                try {
                    Bitmap bm = decoder.decodeRegion(resultRect, options);
                    if (null != result) {
                        result[0] = 0;
                    }
                    return bm;
                } catch (OutOfMemoryError e) {
                    ZebraLog.e(TAG, "[loadRectFromOrigin]", e);
                    if (null != result) {
                        result[0] = -2;
                    }
                } catch (Exception e) {
                    ZebraLog.e(TAG, "[loadRectFromOrigin]", e);
                    if (null != result) {
                        result[0] = -1;
                    }
                } finally {
                    decoder.recycle();
                }
            }
        }
        return null;
    }



    private int getSampleSize(Rect src) {

        // int memSize = ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
        // int inSampleSize = memSize >= 36 ? 1 : 2;

        // BitmapFactory.Options options = new BitmapFactory.Options();
        // options.inJustDecodeBounds = false;
        int inSampleSize = 1;
        if (inSampleSize == 1) {
            if ((src.height() * src.width()) > (1200 * 1600)) {
                int shortSide = Math.min(src.width(), src.height());
                int longSide = Math.max(src.width(), src.height());
                float scale = Math.min(1200 / (float) shortSide, 1600 / (float) longSide);
                int reqWidth = (int) (src.width() * scale);
                int reqHeight = (int) (src.height() * scale);
                inSampleSize = Util.calculateInSampleSize(src.width(), src.height(), reqWidth, reqHeight);
            } else {
                inSampleSize = 1;
            }
        }
        // else if(inSampleSize == 2){
        // if((src.height() * src.width())>(1500*2000)){
        // inSampleSize = 2;
        // }
        // else{
        // inSampleSize = 1;
        // }
        // }

        return inSampleSize;

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
            if (hasEdit || btnConfirm.getText().equals(getResources().getString(R.string.zebra_cut_pic))) {
                showWarningDialog();
                return true;
            } else {
                recycleImage();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    // public void setRatioIcon(){
    // if (smyImageView == null) {
    // return;
    // }
    // if(myImageView.beFourAndThree()){
    // ibRatio.setImageResource(R.drawable.zebra_four_three);
    // }else{
    // ibRatio.setImageResource(R.drawable.zebra_three_four);
    // }
    // }

    public void setRightBottomBtnEnabled(boolean enabled) {
        btnConfirm.setEnabled(enabled);
    }

    public void setRightBottomBtnText(int nameid) {
        btnConfirm.setText(getResources().getString(nameid));
    }

    private String onSaveClicked() {
        /*
         * if (null == innerRect) return null; int centerPointX = MASK_MARGIN + (innerRect.right - innerRect.left) / 2;
         * int centerPointY = innerRect.top + (innerRect.bottom - innerRect.top) / 2; float resultScale = (float)
         * bitmapWidth / (float) scale_iv_src_w; int width = (int) (innerRect.width() * resultScale); int height = (int)
         * (innerRect.height() * resultScale); Rect resultRect = new Rect(centerPointX - width / 2, centerPointY -
         * height / 2, centerPointX + width / 2, centerPointY + height / 2); String result = null; Bitmap croppedImage =
         * Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565); Canvas canvas = new Canvas(croppedImage); Rect
         * dstRect = new Rect(0, 0, width, height); canvas.drawBitmap(bitmapContent, resultRect, dstRect, null); try {
         * canvas.drawBitmap(bitmapContent, innerRect, dstRect, null); result = Util.saveBitmap("test", croppedImage); }
         * catch (Exception e) { ZebraLog.d(TAG, "save bitmap error!"); } catch (OutOfMemoryError er) { ZebraLog.d(TAG,
         * "save bitmap error OutOfMemoryError!"); } return result;
         */

        // fix tapd 48711520
        if (myImageView == null) {
            return "";
        }
        return myImageView.saveImage2File();
    }

    private void showWarningDialog() {

        // View view = LayoutInflater.from(getThisActivity()).inflate(R.layout.zebra_photoedit_warn_dialog, null);
        // new AlertDialog.Builder(getThisActivity())
        // .setView(view)
        // .setPositiveButton(R.string.zebra_doodle_confirm,// 设置确定按钮
        // new DialogInterface.OnClickListener() {
        //
        // @Override
        // public void onClick(DialogInterface arg0, int arg1) {
        // recycleImage();
        // // CropImageActivity.this.setResult(RESULT_CANCELED);
        // // CropImageActivity.this.finish();
        // setResultCancel();
        // }
        //
        // })
        // .setNegativeButton(R.string.zebra_doodle_cancel,
        // new DialogInterface.OnClickListener() {
        //
        // public void onClick(DialogInterface dialog,
        // int which) {
        // }
        //
        // }).create().show();

        try {
            View.OnClickListener posBtnListener = new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    recycleImage();
                    // CropImageActivity.this.setResult(RESULT_CANCELED);
                    // CropImageActivity.this.finish();
                    setResultCancel();

                }
            };
            Dialog dialog = ZebraCustomDialog.newCustomDialog(getThisActivity(),
                    getString(R.string.zebra_tips), getString(R.string.zebra_doodle_canceltips),
                    getString(R.string.zebra_doodle_confirm), posBtnListener,
                    getString(R.string.zebra_doodle_cancel), null);
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void finish() {
        if (didRotateHappen) {
            ReportUtils.report("CliOper", "", "", "Pic_edit", "Zoom_cut", 0, 0, "", "", "", "");
        }

        super.finish();
    }

    private void setResultCancel() {
        ZebraPluginProxy.backToPhoto(getIntent(), getOutActivity());
    }
}
