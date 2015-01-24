package com.tencent.zebra.effect;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.tencent.photoplus.R;
import com.tencent.util.BitmapMergeUtil;
import com.tencent.zebra.crop.CropImageView;
import com.tencent.zebra.doodle.DoodleActivity;
import com.tencent.zebra.editutil.Util;
import com.tencent.zebra.util.ReportUtils;
import com.tencent.zebra.util.ZebraProgressDialog;
import com.tencent.zebra.util.log.ZebraLog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

import cooperation.zebra.ZebraPluginProxy;

/**
 * Version 1.0
 * <p/>
 * <p/>
 * Date: 2014-09-24 18:41
 * Author: retryu
 * <p/>
 * <p/>
 * Copyright © 1998-2014 Tencent Technology (Shenzhen) Company Ltd.
 */
public class CropEffect extends Effects implements View.OnClickListener {
    private static final String TAG = "CropImageActivity";


    private static final int REQ_JUMPTODOODLE = 10001;
    public static final int RES_CANCELFROMDOODLE = -1;
    public static final int RES_OKFROMDOODLE = 1;


    private String picDimenStr = null;
    private String suffix;
    public Button btnCancel;
    public Button btnConfirm;
    private Button ibRotateRight;
    private Button ibRotateLeft;
    private AbsoluteLayout ivParent;

    private LinearLayout effectContainer;
    public static MyHandler handler;
    private CropImageView cropImageView;


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
        private WeakReference<CropEffect> mOuter;

        public MyHandler(CropEffect activity) {
            mOuter = new WeakReference<CropEffect>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final CropEffect outer = mOuter.get();
            if (outer != null) {
                if (msg.what == 0) {
                    try {
                        outer.initUI();
                        if (outer.effectBitmap == null) {
                            handler.sendEmptyMessage(DISMISS_DLG);
                            outer.getThisActivity().finish();
                        } else {
                            if (outer.cropImageView == null) {
                                Bitmap bitmap = outer.effectBitmap.copy(Bitmap.Config.ARGB_8888, true);
                                outer.cropImageView = new CropImageView(outer.getThisActivity(), bitmap, outer.mPath);
//                                ViewGroup.LayoutParams  lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

                                outer.ivParent.addView(outer.cropImageView);
                            } else {
                                Bitmap bitmap = outer.effectBitmap.copy(Bitmap.Config.ARGB_8888, true);
                                outer.cropImageView.setBitmap(bitmap);
                            }
                            outer.cropImageView.setVisibility(View.VISIBLE);
                            handler.sendEmptyMessage(DISMISS_DLG);
                        }
                    } catch (OutOfMemoryError error) {
                        // CameraActivity.SHOULD_KILL_PROCESS = true;
                        try {
                            Toast.makeText(outer.getThisActivity(),
                                    outer.getThisActivity().getResources().getString(R.string.zebra_not_enough_memory),
                                    Toast.LENGTH_LONG).show();
                        } catch (Exception e2) {
                            // TODO 插件很奇怪，这里可能会出现Exception，暂时没有时间跟踪，先备注
                            ZebraLog.e(TAG, "MyHandler.handleMessage", e2);
                        }
                        error.printStackTrace();
                    } catch (Exception e) {
                        try {
                            Toast.makeText(outer.getThisActivity(),
                                    outer.getThisActivity().getResources().getString(R.string.zebra_not_find_picture),
                                    Toast.LENGTH_LONG).show();
                        } catch (Exception e2) {
                            // TODO 插件很奇怪，这里可能会出现Exception，暂时没有时间跟踪，先备注
                            ZebraLog.e(TAG, "MyHandler.handleMessage", e2);
                        }
                        e.printStackTrace();
                    }
                } else if (msg.what == SHOW_DLG) {
                    outer.mProgressDialog = ZebraProgressDialog.show(outer.getThisActivity(), null,
                            outer.getThisActivity().getResources().getString(R.string.zebra_dealing), true, false);
                } else if (msg.what == SHOW_JUMP_DOODLE_DLG) {
                    outer.mProgressDialog = ZebraProgressDialog.show(outer.getThisActivity(), null,
                            outer.getThisActivity().getResources().getString(R.string.zebra_loading), true, false);
                } else if (msg.what == DISMISS_DLG) {
                    if (outer.mProgressDialog != null) {
                        Log.e("debug", "DISMISS mProgressDialog");
                        outer.mProgressDialog.dismiss();
                    }
                } else if (msg.what == SHOW_LOADING_DLG) {
                    outer.mProgressDialog = ZebraProgressDialog.show(outer.getThisActivity(), null,
                            outer.getThisActivity().getResources().getString(R.string.zebra_loading), true, false);
                } else if (msg.what == SAVE_ACTION) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final CropEffect outer = mOuter.get();
                            if (outer != null) {
                                String picpath = outer.onSaveClicked();
                                String picDimenStr = "unkown";
                                CropImageView imageView = outer.cropImageView;
                                if (null != imageView) {
                                    synchronized (imageView) {
                                        picDimenStr = imageView.getSavedWidth() + "x" + imageView.getSavedHeight();
                                        imageView.recycleImage();
                                    }
                                    outer.cropImageView = null;
                                }

                                handler.sendEmptyMessage(DISMISS_DLG);
                                ZebraLog.d(TAG, "PictureCropActivity onClick crop_confirm picpath=" + picpath);
                                // fix tapd 48711520
                                if (TextUtils.isEmpty(picpath)) {
                                    // CropImageActivity.this.setResult(RESULT_CANCELED);
                                    // CropImageActivity.this.finish();
                                    outer.setResultCancel();
                                } else {
                                    //TODO
//                                    ReportUtils.report(f9i
//                                            "CliOper", "", "", "Pic_edit", "Send_cut", 0, 0,
//                                            "" + outer.cutButtonClickTimes, "",
//                                            "", picDimenStr);

                                    // Intent it = new Intent();
                                    // it.putExtra("image_path", picpath);//修改路径
                                    // CropImageActivity.this.setResult(RESULT_OK, it);
                                    // CropImageActivity.this.finish();
                                    // Intent intent = new Intent(getOutActivity(), DemoActivity.class);
                                    // intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    // | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    // startActivity(intent);
//                                    ArrayList<String> paths = new ArrayList<String>();
//                                    paths.add(picpath);
//                                    //TODO
//                                    ZebraPluginProxy.sendPhotoForPhotoPlus(outer.getThisActivity(), outer.getThisActivity().getIntent(),
//                                            paths);
                                }
                            }
                        }
                    }).start();
                } else if (msg.what == SAVE_AND_JUMP_DOODLE) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final CropEffect outer = mOuter.get();
                            if (null != outer) {
                                final CropImageView imageView = outer.cropImageView;
                                if (null != imageView) {
                                    synchronized (imageView) {
                                        // String picpath = onSaveClicked();
                                        // picDimenStr = cropImageView.getSavedWidth() + "x" +
                                        // cropImageView.getSavedHeight();
                                        //
                                        // handler.sendEmptyMessage(DISMISS_DLG);
                                        // ZebraLog.d(TAG, "PictureCropActivity onClick crop_confirm picpath="+picpath);
                                        Intent intent = new Intent(outer.getThisActivity(), DoodleActivity.class);
                                        intent.putExtra("isfromCrop", true);
                                        // intent.putExtra("image_path", picpath);
                                        // intent.putExtra("image_bitmap", cropImageView.getBitmap());
                                        bitmapForDoodle = imageView.getCropBitmap();

                                        imageView.recycleImage();
                                        outer.cropImageView = null;

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

                                        outer.getThisActivity().startActivityForResult(DoodleActivity.class, intent, REQ_JUMPTODOODLE);
                                        outer.getThisActivity().overridePendingTransition(0, 0);
                                    }
                                }
                            }
                        }
                    }).start();
                } else if (msg.what == HIDE_VIEW_IN_DOODLE) {
                    ZebraLog.d("zebra", "CropImageActivity handler handleMessage msg.what == HIDE_VIEW_IN_DOODLE");
                    //TODO
                    // outer.setContentView(R.layout.zebra_crop_activity_hide);
                } else if (msg.what == CHECK_BMP_EMPTY) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final CropEffect outer = mOuter.get();
                            if (null != outer) {
                                try {
                                    if (Util.isExist(outer.mPath)) {
                                        Util.Size size = Util.getBmpSize(outer.mPath);
                                        if (size.height < 64 || size.width < 64) {
                                            Message msg = handler.obtainMessage(SHOW_TOAST);
                                            msg.arg1 = R.string.zebra_smallpic_tip;
                                            msg.sendToTarget();
                                            handler.removeMessages(0);
                                            outer.getThisActivity().finish();
                                            return;
                                        }
                                    }
                                    int[] result = new int[1];
                                    if (outer.effectBitmap == null) {
                                        outer.effectBitmap = Util.getOrResizeBitmap(outer.mPath, true, result);
                                    }
                                    // outer.mSelBitmap = Util.getBitmap(outer, outer.mPath);
                                    if (outer.effectBitmap == null) {
                                        Message msg = handler.obtainMessage(SHOW_TOAST);
                                        if (result[0] == -2) {
                                            msg.arg1 = R.string.zebra_not_enough_memory;
                                        } else {
                                            msg.arg1 = R.string.zebra_file_not_exsit_or_unvalid;
                                        }
                                        msg.sendToTarget();
                                        handler.removeMessages(0);
                                        outer.getThisActivity().finish();
                                    } else {
                                        handler.sendEmptyMessage(0);
                                    }
                                } catch (OutOfMemoryError error) {
                                    Message msg = handler.obtainMessage(SHOW_TOAST);
                                    msg.arg1 = R.string.zebra_not_enough_memory;
                                    msg.sendToTarget();
                                    outer.getThisActivity().finish();
                                    ZebraLog.e("zebra", "OOM error.", error);
                                } catch (Exception e) {
                                    Message msg = handler.obtainMessage(SHOW_TOAST);
                                    msg.arg1 = R.string.zebra_not_find_picture;
                                    msg.sendToTarget();
                                    outer.getThisActivity().finish();
                                    ZebraLog.e("zebra", "error.", e);
                                }
                            }
                        }
                    }).start();
                } else if (msg.what == SHOW_TOAST) {
                    int resid = msg.arg1;
                    try {
                        Toast.makeText(outer.getThisActivity(), outer.getThisActivity().getString(resid), Toast.LENGTH_LONG).show();
                    } catch (Exception e2) {
                        // TODO 插件很奇怪，这里可能会出现Exception，暂时没有时间跟踪，先备注
                        ZebraLog.e(TAG, "MyHandler.handleMessage", e2);
                    }
                }
            }
        }
    }

    private PhotoEffectActivity photoEffectActivity;

    private ViewGroup containner;

    private ViewGroup effecrContainer;

    public CropEffect(PhotoEffectActivity p, ViewGroup v, ViewGroup effecrLayout, String path) {
        super(p, v);
        this.photoEffectActivity = p;
        this.effecrContainer = effecrLayout;
        this.containner = (ViewGroup) v.findViewById(R.id.image_parent);
        this.mPath = path;
        comfirmBarListenner = new ConfirmBarListenner() {
            @Override
            public void onConfrimClick(View v) {

            }

            @Override
            public void onCancelClcick(View v) {

            }
        };

    }

    @Override
    public void confirm(View btnConfirm) {
        super.confirm(btnConfirm);
        Log.e("debug", "confime click");
        hasEdit = true;
        if (cropImageView == null) {
            // CropImageActivity.this.setResult(RESULT_CANCELED);
            // CropImageActivity.this.finish();
            setResultCancel();
            return;
        }

        Button btn = (Button) btnConfirm;
        if (btn.getText().equals(getThisActivity().getResources().getString(R.string.zebra_cut_pic))) {
            ReportUtils.report("0X8004B39", "", "", "", "0X8004B39", 0, 0, "", "", "", "");

            Log.e("debug", "crop_confirm click show");
            ++cutButtonClickTimes;
            cropImageView.setInitalAngle(Util.getExifDegree(mPath));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1
                    && !suffix.toLowerCase(Locale.US).equals("gif") && !suffix.toLowerCase(Locale.US).equals("bmp")) {

                Log.e("debug", "crop_confirm click show SDK_INT");
                int[] result = new int[1];
                Bitmap bm = loadRectFromOrigin(result);
                if (result[0] == -1) {
                    ZebraLog.e(TAG, "[crop_confirm]  crop  faild");
                    Toast.makeText(getThisActivity().getApplicationContext(), getThisActivity().getString(R.string.zebra_crop_failed),
                            Toast.LENGTH_LONG).show();
                    return;
                } else if (result[0] == -2) {
                    ZebraLog.e(TAG, "[crop_confirm]  crop  oom");
                    Toast.makeText(getThisActivity().getApplicationContext(), getThisActivity().getString(R.string.zebra_not_enough_memory),
                            Toast.LENGTH_LONG).show();
                    return;
                } else if (result[0] == -3) {
                    ZebraLog.e(TAG, "[crop_confirm]  crop not  support");
                    Toast.makeText(getThisActivity().getApplicationContext(),
                            getThisActivity().getString(R.string.zebra_crop_unsupport_fileformat_failed),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                ZebraLog.e(TAG, "[crop_confirm]  crop sucessed");
//                handler.sendEmptyMessage(DISMISS_DLG);
                cropImageView.setBitmap(bm);
//                effectBitmap = bm;
//                mPath = Util.saveOutput(photoEffectActivity, mPath, effectBitmap, true);


            } else {
                Log.e("debug", "confime click show SDK_INT save");
                int[] result = new int[1];
                cropImageView.saveImage2Bitmap(result);
                if (result[0] == -1) {
                    Toast.makeText(getThisActivity().getApplicationContext(), getThisActivity().getString(R.string.zebra_crop_failed),
                            Toast.LENGTH_LONG).show();
                    return;
                } else if (result[0] == -2) {
                    Toast.makeText(getThisActivity().getApplicationContext(), getThisActivity().getString(R.string.zebra_not_enough_memory),
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }
            setRightBottomBtnText(R.string.zebra_send_pic);
            Log.e("debug", "confime click hide");
            handler.sendEmptyMessage(DISMISS_DLG);
        } else {

            if (cropImageView.isConfirmAvailable()) {
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        Log.e("debug", "saving:" + saving);
                        if (saving == false) {
                            saving = true;
                            handler.sendEmptyMessage(SHOW_DLG);
                            Log.e("debug", "SHOW_DLG");
                            mPath = Util.saveOutput(photoEffectActivity, mPath, getEffectBitmap(), true);
                            handler.sendEmptyMessageDelayed(DISMISS_DLG, 300);
                            Log.e("debug", "DISMISS");
                            saving = false;


//                            mPath= cropImageView.saveImage2File();
                            ArrayList<String> paths = new ArrayList<String>();
                            paths.add(mPath);
                            Log.e("debug", "confime save" + mPath);

                            //TODO
                            ZebraPluginProxy.sendPhotoForPhotoPlus(getThisActivity(), getThisActivity().getIntent(),
                                    paths);
                            photoEffectActivity.finish();
                        }
                    }
                }.start();


                handler.sendEmptyMessageDelayed(DISMISS_DLG, 100);

            }
        }
    }

    public void setRightBottomBtnText(int nameid) {
        btnConfirm.setText(getThisActivity().getResources().getString(nameid));
    }

    @Override
    public void showEffect() {
        super.showEffect();
        initEffect();
    }

    @Override
    public void initEffect() {
        super.initEffect();
        //TODO
//        ReportUtils.report("CliOper", "", "", "Pic_edit", "Clk_cut", 0, 0, "", "", "", "");

//        mPath = photoEffectActivity.getIntent().getStringExtra("image_path");
        // nickname = getIntent().getStringExtra("self_nick");
        // uin = ""+getIntent().getExtras().getLong("qq", 0);
        // ZebraLog.d(TAG, "CropImageActivity onCreate uin="+uin);

        if (!TextUtils.isEmpty(mPath)) {
            ZebraLog.d("PictureCropActivity", "PictureCropActivity onCreate path.s=" + mPath);
        } else {
            ZebraLog.d("PictureCropActivity", "PictureCropActivity onCreate path = null");
            return;
        }

        handler = new MyHandler(this);
        // check bmp available.
        handler.sendEmptyMessage(SHOW_LOADING_DLG);
        handler.sendEmptyMessageDelayed(CHECK_BMP_EMPTY, 200);

        int index = mPath.lastIndexOf('.');
        suffix = mPath.substring(index + 1);

        // init();

        cutButtonClickTimes = 0;
    }

    private void initUI() {
//      setContentView(R.layout.zebra_crop_activity);
        findView();
        setClick();
    }

    private void findView() {
        //TODO
//        btnCancel = (Button) findViewById(R.id.crop_cancel);
//        btnConfirm = (Button) findViewById(R.id.crop_confirm);
//        ibRotateRight = (ImageButton) findViewById(R.id.rotate_right);
//        ibRotateLeft = (ImageButton) findViewById(R.id.rotate_left);
//        // ibRatio = (ImageButton) findViewById(R.id.ratio);
//        ivParent = (AbsoluteLayout) findViewById(R.id.image_parent);
        ivParent = (AbsoluteLayout) containner;

//        effectContainer=containner
        LayoutInflater layoutInflater = getThisActivity().getLayoutInflater();

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        LinearLayout effectLayout = (LinearLayout) layoutInflater.inflate(R.layout.layout_effect_crop, null);
        effectLayout.setGravity(LinearLayout.HORIZONTAL);
        ibRotateRight = (Button) effectLayout.findViewById(R.id.btn_rotate_right);
        ibRotateLeft = (Button) effectLayout.findViewById(R.id.btn_rotate_left);
        ibRotateRight.setOnClickListener(this);
        ibRotateLeft.setOnClickListener(this);
        effecrContainer.addView(effectLayout, lp);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.btn_rotate_right: {
                hasEdit = true;
                if (cropImageView == null) {
                    // CropImageActivity.this.setResult(RESULT_CANCELED);
                    // CropImageActivity.this.finish();
                    setResultCancel();
                    break;
                } else {
                    didRotateHappen = true;
                    if (!cropImageView.isRotating())
                        cropImageView.rotate(-90);
                    ReportUtils.report("", "", "", "0X8004B3D", "0X8004B3D", 0, 0, "", "", "", "");
                    break;
                }
            }
            case R.id.btn_rotate_left: {
                hasEdit = true;
                if (cropImageView == null) {
                    // CropImageActivity.this.setResult(RESULT_CANCELED);
                    // CropImageActivity.this.finish();
                    setResultCancel();
                    break;
                } else {
                    didRotateHappen = true;
                    if (!cropImageView.isRotating())
                        cropImageView.rotate(90);
                    ReportUtils.report("", "", "", "0X8004B3D", "0X8004B3D", 0, 0, "", "", "", "");
                    break;
                }
            }
        }
    }

    private void setClick() {
        //TODO
//        btnCancel.setOnClickListener(this);
//        btnConfirm.setOnClickListener(this);
//        ibRotateRight.setOnClickListener(this);
//        ibRotateLeft.setOnClickListener(this);
        // ibRatio.setOnClickListener(this);
    }


    private void recycleImage() {
        if (cropImageView != null) {
            synchronized (cropImageView) {
                cropImageView.recycleImage();
                cropImageView = null;
            }
        }
        System.gc();
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
//                if (effectBitmap == null) {
//                mPath = Util.saveOutput(photoEffectActivity, mPath, getEffectBitmap(), true);

//                decoder = BitmapRegionDecoder.newInstance(mPath, true);


//                } else {
//                    Bitmap bitmap =cropImageView.getBitmap();
//                Bitmap bitmap = effectBitmap.copy(Bitmap.Config.ARGB_8888, false);
//                  bitmap = Util.rotate(bitmap,-90);
                int  degree = Util.getExifDegree(mPath);
                Bitmap bitmap =Util.rotateNotRecyle(effectBitmap,degree);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] data = baos.toByteArray();
                decoder = BitmapRegionDecoder.newInstance(data, 0, data.length, false);
//                }
            } catch (IOException e) {
                ZebraLog.e(TAG, "[loadRectFromOrigin]", e);
                if (null != result) {
                    result[0] = -3;
                }
            }
            if (null != decoder) {
                float[] margin = cropImageView.getCropMargin();
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
                ZebraLog.e(TAG, "[loadRectFromOrigin] marggin" + margin[0] + "  " + margin[1] + "  " + margin[2] + "  " + margin[3]
                        + "result: l:" + resultRect.left
                        + "  right:" + resultRect.right
                        + "  top:" + resultRect.top + "  bottom:"
                        + resultRect.bottom);
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



//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
//            BitmapRegionDecoder decoder = null;
//            try {
//                decoder = BitmapRegionDecoder.newInstance(mPath, true);
////                Bitmap bitmap = effectBitmap.copy(Bitmap.Config.ARGB_8888, true);
////                ByteArrayOutputStream baos = new ByteArrayOutputStream();
////                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
////                byte[] data = baos.toByteArray();
////                decoder = BitmapRegionDecoder.newInstance(data, 0, data.length, true);
//
//            } catch (IOException e) {
//                ZebraLog.e(TAG, "[loadRectFromOrigin]", e);
//                if (null != result) {
//                    result[0] = -3;
//                }
//            }
//            if (null != decoder) {
//                float[] margin = cropImageView.getCropMargin();
//                if (resultRect == null) {
//                    resultRect = new Rect();
//                    resultRect.top = 0;
//                    resultRect.left = 0;
//                    resultRect.right = decoder.getWidth();
//                    resultRect.bottom = decoder.getHeight();
//                }
//                int width = resultRect.width();
//                int height = resultRect.height();
//                resultRect.top = (int) (resultRect.top + height * margin[1]);
//                resultRect.bottom = (int) (resultRect.bottom - height * margin[3]);
//                resultRect.left = (int) (resultRect.left + width * margin[0]);
//                resultRect.right = (int) (resultRect.right - width * margin[2]);
//
//                BitmapFactory.Options options = new BitmapFactory.Options();
//                options.inSampleSize = getSampleSize(resultRect);
//                try {
//                    Bitmap bm = decoder.decodeRegion(resultRect, options);
//                    if (null != result) {
//                        result[0] = 0;
//                    }
//                    return bm;
//                } catch (OutOfMemoryError e) {
//                    ZebraLog.e(TAG, "[loadRectFromOrigin]", e);
//                    if (null != result) {
//                        result[0] = -2;
//                    }
//                } catch (Exception e) {
//                    ZebraLog.e(TAG, "[loadRectFromOrigin]", e);
//                    if (null != result) {
//                        result[0] = -1;
//                    }
//                } finally {
//                    decoder.recycle();
//                }
//            }
//        }
//        return null;




    }

    private int getSampleSize(Rect src) {
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
        return inSampleSize;
    }


    private String onSaveClicked() {
        // fix tapd 48711520
        if (cropImageView == null) {
            return "";
        }
        return cropImageView.saveImage2File();
    }


    private void setResultCancel() {
        ZebraPluginProxy.backToPhoto(getThisActivity().getIntent(), getThisActivity().getOutActivity());
    }


    public PhotoEffectActivity getThisActivity() {
        return photoEffectActivity;
    }

    @Override
    public void recyleImage() {
        if (cropImageView != null) {
            synchronized (cropImageView) {
                cropImageView.recycleImage();
                cropImageView = null;
            }
        }
        System.gc();
    }

    @Override
    public void hide() {
        super.hide();
        if (effecrContainer != null) {
            effecrContainer.removeAllViews();
            ivParent.removeView(cropImageView);
        }
        if (cropImageView != null) {
            ivParent.removeView(cropImageView);
            cropImageView = null;
//            cropImageView.setVisibility(View.GONE);
//            cropImageView.totalAngle = 0;
        }
    }

    @Override
    public Bitmap getEffectBitmap() {
        super.getEffectBitmap();
        if (cropImageView != null) {
            return cropImageView.getCropBitmap();
        }
        return null;
    }


}
