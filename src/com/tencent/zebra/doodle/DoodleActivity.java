package com.tencent.zebra.doodle;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.tencent.photoplus.R;
import com.tencent.zebra.crop.CropImageActivity;
import com.tencent.zebra.doodle.DrawableImageView.OnDrawListener;
import com.tencent.zebra.doodle.ZoomableImageViewBase.DisplayType;
import com.tencent.zebra.editutil.Util;
import com.tencent.zebra.editutil.Util.Size;
import com.tencent.zebra.ui.BaseGlorifyActivity;
import com.tencent.zebra.util.ReportUtils;
import com.tencent.zebra.util.ZebraCustomDialog;
import com.tencent.zebra.util.ZebraProgressDialog;
import com.tencent.zebra.util.log.ZebraLog;

import cooperation.zebra.ZebraPluginProxy;

import java.util.ArrayList;

/**
 * 涂鸦
 * 
 * @author haoni@tencent.com
 * @date 2013-5-8
 */
public class DoodleActivity extends BaseGlorifyActivity implements OnDrawListener{
    public static final String TAG = "DoodleActivity";
	private static final int TAB_DRAW = 0;
	private static final int TAB_ERASE = 1;
	private DrawableImageView imageView;
	private LinearLayout rootView = null;
	private Bitmap bitmap;
	private Button buttonDraw;
	private Button buttonErase;
//	private View penSettingLayout;
	private View eraserSettingLayout;
//	private SeekBar penSizeSeekBar;
//	private SeekBar penColorSeekBar;
//	private SeekBar eraserSizeSeekBar;
	private Button buttonEraseAll;

	// 还原大小用的matrix
	private Matrix imageMatrix;
	private Paint mPaint;
	private int color = 0xf93021;
	private int penSize = 10;
	private int eraserSize = 50;

	private Bitmap original;

	private int mCurTab;
	private Button back;
	private Button confirm;
	private ImageView mUndoBtn;
	private ImageView mEraserBtn;
	private ImageView mRedPenBtn;
	private ImageView mYellowPenBtn;
	private ImageView mOrangePenBtn;
	private ImageView mGreenPenBtn;
	private ImageView mBluePenBtn;
	private ImageView mPinkPenBtn;
	private ImageView mMosaicPenBtn;
	
	private ArrayList<View> mPenList = new ArrayList<View>();

	int mPenSizes[];
	int mPenColors[];
	int mEraserSizes[];

	int penStep = 4;
	int penStepRound = 0;
	int penMax;
	int penStepProgress;

	int colorStep = 8;
	int colorStepRound = 0;
	int colorMax;
	int colorStepProgress;

	int eraserStep = 4;
	int eraserStepRound = 0;
	int eraserMax;
	int eraserStepProgress;
	
	boolean beedit = false;

//	int mSeekBarThumb[] = { R.drawable.doodle_brush6,
//			R.drawable.doodle_brush12, R.drawable.doodle_brush20,
//			R.drawable.doodle_brush30 };

//	public void initSeekBars() {
//
//		mPenSizes = getResources().getIntArray(R.array.zebra_doodle_pen_size);
//		mPenColors = getResources().getIntArray(R.array.zebra_doodle_pen_colors);
//		mEraserSizes = getResources().getIntArray(R.array.zebra_doodle_eraser_size);
//
//		color = mPenSizes[0];
//		penSize = mPenSizes[0];
//		eraserSize = mEraserSizes[0];
//
//		penMax = penSizeSeekBar.getMax();
//		colorMax = penColorSeekBar.getMax();
//		eraserMax = eraserSizeSeekBar.getMax();
//		penStepProgress = penMax / (penStep - 1);
//		colorStepProgress = colorMax / (colorStep - 1);
//		eraserStepProgress = eraserMax / (eraserStep - 1);
//	}
	private ProgressDialog mProgressDialog;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == SHOW_DLG) {
                mProgressDialog = ZebraProgressDialog.show(getThisActivity(), null, getString(R.string.zebra_loading),
                        true, false);
            } else if (msg.what == INIT_UI) {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                }
                initUI();
            } else if (msg.what == FAIL_EXIT) {
                try {
                    Toast.makeText(getThisActivity(),
                            getResources().getString(R.string.zebra_file_not_exsit_or_unvalid), Toast.LENGTH_LONG)
                            .show();
                } catch (Exception e) {
                    // TODO 插件很奇怪，这里可能会出现Exception，暂时没有时间跟踪，先备注
                    ZebraLog.e(TAG, "Handler.handleMessage", e);
                }
                ReleaseRes();
                // setResult(RESULT_CANCELED);
                // DoodleActivity.this.finish();
                setResultCancel();
            } else if (msg.what == NOT_ENOUGH_MEMORY_EXIT) {
                try {
                    Toast.makeText(getThisActivity(),
                            getResources().getString(R.string.zebra_not_enough_memory), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    // TODO 插件很奇怪，这里可能会出现Exception，暂时没有时间跟踪，先备注
                    ZebraLog.e(TAG, "Handler.handleMessage", e);
                }
                ReleaseRes();
                // setResult(RESULT_CANCELED);
                // DoodleActivity.this.finish();
                setResultCancel();
            } else if (msg.what == TOOSMALL_EXIT) {
                try {
                    Toast.makeText(getThisActivity(),
                            getResources().getString(R.string.zebra_smallpic_tip), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    // TODO 插件很奇怪，这里可能会出现Exception，暂时没有时间跟踪，先备注
                    ZebraLog.e(TAG, "Handler.handleMessage", e);
                }

                ReleaseRes();
                // setResult(RESULT_CANCELED);
                // DoodleActivity.this.finish();
                setResultCancel();
            }
        }
    };
	
	private static int SHOW_DLG = 9000;
	private static int INIT_UI = 9001;
	private static int FAIL_EXIT = 9002;
	private static int NOT_ENOUGH_MEMORY_EXIT = 9003;
	private static int TOOSMALL_EXIT = 9004;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		ReportUtils.report("CliOper", "", "", "Pic_edit", "Clk_graffiti", 0, 0, "", "", "", "");
		
		Bundle extras = getIntent().getExtras();
        if (isFromCrop) {
            original = CropImageActivity.bitmapForDoodle;
            // TODO LouisPeng 这种做法太坑爹了，DoodleActivity接收到之后一定要手动设为null
            CropImageActivity.bitmapForDoodle = null;
        }
		if (original == null && null == mImagePath) {
			mImagePath = extras.getString("image_path");
		}
//		String nickname = extras.getString("self_nick");
//		String uin = ""+extras.getLong("qq", 0);
		beedit = extras.getBoolean("hasEdit",false);
		ZebraLog.d("zebra", "DoodleActivity onCreate beedit = " + beedit);
		handler.sendEmptyMessage(SHOW_DLG);

		
        new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                if (original == null) {
                    Size size = Util.getBmpSize(mImagePath);
                    if (size == null || size.height < 64 || size.width < 64) {
                        handler.sendEmptyMessage(TOOSMALL_EXIT);
                        return;
                    }
                    int[] result = new int[1];
                    original = Util.getOrResizeBitmap(mImagePath, true, result);
                    if (original == null) {
                        if (result[0] == -2) {
                            handler.sendEmptyMessage(NOT_ENOUGH_MEMORY_EXIT);
                        } else {
                            handler.sendEmptyMessage(FAIL_EXIT);
                        }
                    }
                    else {
                        handler.sendEmptyMessage(INIT_UI);
                    }
                } else {
                    handler.sendEmptyMessage(INIT_UI);
                }
            }

        }).start();

	}

	private void initUI() {
		
		setContentView(R.layout.zebra_doodle_layout);
		rootView = (LinearLayout)findViewById(R.id.rootView);
		imageView = (DrawableImageView) findViewById(R.id.imageView);

		try {
			bitmap = original.copy(Bitmap.Config.RGB_565, true);
		} catch (OutOfMemoryError err) {
			ZebraLog.e(TAG, "initUI", err);
			return;
		}
		imageView.setDisplayType(DisplayType.FIT_TO_SCREEN);
		imageView.setOnDrawListener(this);
		initDisplay(bitmap);
		mCurTab = R.id.doodle_red_btn;//TAB_DRAW;
		
		back = (Button) findViewById(R.id.cancel);
		back.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if(CropImageActivity.handler!=null){
					CropImageActivity.handler.sendEmptyMessage(CropImageActivity.HIDE_VIEW_IN_DOODLE);
				}
				if(imageView.hasDrawed()||beedit) {
					showWarningDialog();
				} else {
					ReleaseRes();
//					setResult(RESULT_CANCELED);
//					DoodleActivity.this.finish();
					setResultCancel();
				}
				
			}
		});

		confirm = (Button) findViewById(R.id.confirm);
		confirm.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
//				DataReport.doodleOptHandle();
				if(CropImageActivity.handler!=null){
					CropImageActivity.handler.sendEmptyMessage(CropImageActivity.HIDE_VIEW_IN_DOODLE);
				}
				ReportUtils.report("CliOper", "", "", "Pic_edit", "Send_graffiti", 0, 0, "", "", "", "");
				Bitmap bmp = imageView.commit();
                if (null != bmp) {
                    dealResult(bmp, true);
                }
			}
		});
		
		
		int screenWidth = getResources().getDisplayMetrics().widthPixels;
		int colorSize = getResources().getDimensionPixelSize(R.dimen.zebra_doodle_color_size);
		int margin = (int) ((screenWidth - 6* colorSize) / 6.5);
		
		
		mUndoBtn = (ImageView) findViewById(R.id.doodle_undo_btn);
		LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)mUndoBtn.getLayoutParams();
		lp.leftMargin = margin;
		mUndoBtn.setLayoutParams(lp);
		mUndoBtn.setEnabled(false);
		mUndoBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
//				DataReport.doodleOptHandle();
				imageView.unDo();
				if(!imageView.hasDrawed()) {
					mUndoBtn.setEnabled(false);
				}
				if(mCurTab == R.id.doodle_mosaic_btn) {
					initMosaicPaint();
				}
			}
		});
		
		mEraserBtn = (ImageView) findViewById(R.id.doodle_eraser_btn);
		lp = (LinearLayout.LayoutParams)mEraserBtn.getLayoutParams();
		lp.leftMargin = margin;
		mEraserBtn.setLayoutParams(lp);
		mEraserBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
				mPaint.setAlpha(0);
				mPaint.setStrokeWidth(eraserSize);
				mPaint.setShader(null);
				updatePaint();
				setFocusBg(v.getId());
			}
		});
		mPenList.add(mEraserBtn);
		
		mMosaicPenBtn = (ImageView) findViewById(R.id.doodle_mosaic_btn);
		lp = (LinearLayout.LayoutParams)mMosaicPenBtn.getLayoutParams();
		lp.leftMargin = margin;
		mMosaicPenBtn.setLayoutParams(lp);
		mMosaicPenBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
//				color = 0xb00ecd;
//				mPaint.setColor(color);
//				mPaint.setAlpha(255);
//				mPaint.setXfermode(null);
//				mPaint.setStrokeWidth(penSize);
//				updatePaint();
				if(mCurTab == R.id.doodle_mosaic_btn) {
					return;
				}
				initMosaicPaint();
				setFocusBg(v.getId());
			}
		});
		mPenList.add(mMosaicPenBtn);
		
		mRedPenBtn = (ImageView) findViewById(R.id.doodle_red_btn);
		lp = (LinearLayout.LayoutParams)mRedPenBtn.getLayoutParams();
		lp.leftMargin = margin;
		mRedPenBtn.setLayoutParams(lp);
		mRedPenBtn.setBackgroundResource(R.drawable.zebra_eraser_sel_bg);
		mRedPenBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mPaint == null) {
					return;
				}
				color = 0xf93021;
				mPaint.setColor(color);
				mPaint.setAlpha(255);
				mPaint.setXfermode(null);
				mPaint.setStrokeWidth(penSize);
				mPaint.setShader(null);
				updatePaint();
				setFocusBg(v.getId());
			}
		});
		mPenList.add(mRedPenBtn);
		
		mOrangePenBtn = (ImageView) findViewById(R.id.doodle_orange_btn);
		lp = (LinearLayout.LayoutParams)mOrangePenBtn.getLayoutParams();
		lp.leftMargin = margin;
		mOrangePenBtn.setLayoutParams(lp);
		mOrangePenBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mPaint == null) {
					return;
				}
				color = 0xfd7f32;
				mPaint.setColor(color);
				mPaint.setAlpha(255);
				mPaint.setXfermode(null);
				mPaint.setStrokeWidth(penSize);
				mPaint.setShader(null);
				updatePaint();
				setFocusBg(v.getId());
			}
		});
		mPenList.add(mOrangePenBtn);
		
		mYellowPenBtn = (ImageView) findViewById(R.id.doodle_yellow_btn);
		lp = (LinearLayout.LayoutParams)mYellowPenBtn.getLayoutParams();
		lp.leftMargin = margin;
		mYellowPenBtn.setLayoutParams(lp);
		mYellowPenBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mPaint == null) {
					return;
				}
				color = 0xffe00d;
				mPaint.setColor(color);
				mPaint.setAlpha(255);
				mPaint.setXfermode(null);
				mPaint.setStrokeWidth(penSize);
				mPaint.setShader(null);
				updatePaint();
				setFocusBg(v.getId());
			}
		});
		mPenList.add(mYellowPenBtn);
		
		mGreenPenBtn = (ImageView) findViewById(R.id.doodle_green_btn);
		lp = (LinearLayout.LayoutParams)mGreenPenBtn.getLayoutParams();
		lp.leftMargin = margin;
		mGreenPenBtn.setLayoutParams(lp);
		mGreenPenBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mPaint == null) {
					return;
				}
				color = 0x85e81b;
				mPaint.setColor(color);
				mPaint.setAlpha(255);
				mPaint.setXfermode(null);
				mPaint.setStrokeWidth(penSize);
				mPaint.setShader(null);
				updatePaint();
				setFocusBg(v.getId());
			}
		});
		mPenList.add(mGreenPenBtn);
		
		mBluePenBtn = (ImageView) findViewById(R.id.doodle_blue_btn);
		lp = (LinearLayout.LayoutParams)mBluePenBtn.getLayoutParams();
		lp.leftMargin = margin;
		mBluePenBtn.setLayoutParams(lp);
		mBluePenBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mPaint == null) {
					return;
				}
				color = 0x1792f9;
				mPaint.setColor(color);
				mPaint.setAlpha(255);
				mPaint.setXfermode(null);
				mPaint.setStrokeWidth(penSize);
				mPaint.setShader(null);
				updatePaint();
				setFocusBg(v.getId());
			}
		});
		mPenList.add(mBluePenBtn);
		
		mPinkPenBtn = (ImageView) findViewById(R.id.doodle_pink_btn);
		lp = (LinearLayout.LayoutParams)mPinkPenBtn.getLayoutParams();
		lp.leftMargin = margin;
		lp.rightMargin = margin;
		mPinkPenBtn.setLayoutParams(lp);
		mPinkPenBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mPaint == null) {
					return;
				}
				color = 0xb00ecd;
				mPaint.setColor(color);
				mPaint.setAlpha(255);
				mPaint.setXfermode(null);
				mPaint.setStrokeWidth(penSize);
				mPaint.setShader(null);
				updatePaint();
				setFocusBg(v.getId());
			}
		});
		mPenList.add(mPinkPenBtn);
		
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				initPaint();
			}
		}, 500);
		
	}
	
	private void setFocusBg(int btnId) {
		for(View view : mPenList) {
			if(view.getId() == btnId) {
				mCurTab = btnId;
				if(btnId == R.id.doodle_eraser_btn ||
						btnId == R.id.doodle_mosaic_btn ||
							btnId == R.id.doodle_undo_btn ) {
					view.setPressed(true);
					view.setSelected(true);
				} else {
					view.setBackgroundResource(R.drawable.zebra_eraser_sel_bg);
				}
			} else {
				view.setBackgroundResource(0);
				view.setSelected(false);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		rect = new Rect();
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		
		// 首次打开，在CameraMainActivity显示出来后再跳转至CameraActivity
//	    ZebraLog.e("Zebra", "[BENCHMARK] DoodleActivity onWindowFocusChanged end time="+System.currentTimeMillis()+";hasfocus="+hasFocus);
//	    if(hasFocus){
//	    	CropImageActivity.handler.sendEmptyMessage(CropImageActivity.HIDE_VIEW_IN_DOODLE);
//	    }
	}

	private float colorSeekBarStepWidth;
	private Rect rect;
	private int mX;
	private int mY;
	// final int[] seekBarLocation = new int[2];
	private View bubbleLayout;
	private View bubbleBg;
	private WindowManager windowManager;
	private WindowManager.LayoutParams windowParams;
	private final static int OFFSET_SPACE = 8;

//	public void startMove(int color, int x, int y) {
//		stopMove();
//		windowParams = new WindowManager.LayoutParams();
//		windowParams.gravity = Gravity.LEFT | Gravity.TOP;
//		windowParams.x = x - OFFSET_SPACE;
//		windowParams.y = y;
//		windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
//		windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
//		windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//				| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
//				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
//				| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
//		windowParams.format = PixelFormat.TRANSLUCENT;
//		windowParams.windowAnimations = 0;
//
//		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
//		View layout = inflater.inflate(R.layout.zebra_color_bubble, null);
//		bubbleBg = layout.findViewById(R.id.bubble_bg);
//		bubbleBg.setBackgroundColor(color);
//		windowManager = (WindowManager) this.getSystemService("window");
//		windowManager.addView(layout, windowParams);
//		bubbleLayout = layout;
//	}

	private void initDisplay(Bitmap bmp) {
		if (null == imageMatrix) {
			imageMatrix = new Matrix();
		}
		imageView.setImageBitmap(bmp, imageMatrix.isIdentity() ? null
				: imageMatrix, ZoomableImageViewBase.ZOOM_INVALID,
				ZoomableImageViewBase.ZOOM_INVALID);
	}

	/**
	 * initialize the paint.
	 */
	private void initMosaicPaint() {
		if (mPaint == null) {
			return;
		}
		mPaint.setXfermode(null);
		mPaint.setStrokeWidth(45);

		
		Bitmap xx = null;
        try {
            xx = Bitmap.createBitmap(original.getWidth(), original.getHeight(), Config.RGB_565);// .ARGB_8888);
        } catch (OutOfMemoryError err) {
            ZebraLog.e(TAG, "initMosaicPaint", err);
            return;
        }
		Canvas s = new Canvas(xx);
//		Rect src = new Rect(0,0, bit.getWidth(), bit.getHeight());
//		Rect des = new Rect(0,0, original.getWidth(), original.getHeight());
//		s.drawBitmap(bit, src, des, null);
//		s.drawBitmap(bitdoodle, src, des, null);
		s.drawBitmap(original, new Matrix(), null);
		s.drawBitmap(imageView.getOverlayBitmap(), new Matrix(), null);
//		bit.recycle();
//		bitdoodle.recycle();
		int blur = Math.min(original.getWidth(), original.getHeight()) / 28;
		convert(xx, blur);
//		if(mShaderBitmap != null && !mShaderBitmap.isRecycled()) {
//			mShaderBitmap.recycle();
//			mShaderBitmap = null;
//		}
		
		Bitmap mosaicBitmap = Bitmap.createBitmap(imageView.getWidth(), imageView.getHeight(), Config.RGB_565);//.ARGB_8888);
		Canvas s2 = new Canvas(mosaicBitmap);
		s2.drawBitmap(xx, imageView.getImageMatrix(), null);
		xx.recycle();

		BitmapShader shader = new BitmapShader(mosaicBitmap, Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);
		mPaint.setShader(shader);
		mPaint.setAlpha(255);

		updatePaint();
	}
	
	// convert original bitmap to mosaic
	private void convert(Bitmap bit, int blur) {
//		int [] pixels = new int[original.getWidth() * original.getHeight()];
//		original.getPixels(pixels, 0, original.getWidth(), 0, 0, original.getWidth(), original.getHeight());
//		for(int i = 0; i < original.getHeight(); i ++) {
//			for(int j = 0; j < original.getWidth(); j++) {
//				pixels[i * original.getWidth() + j] = 0xFF000000 | (255 << 16) | (0 << 8) | 0;//0xFFFF0000;
//			}
//		}
//		bit.setPixels(pixels, 0, original.getWidth(), 0, 0, original.getWidth(), original.getHeight());
//		int [] pixels = new int[original.getWidth() * original.getHeight()];
//		bit.getPixels(pixels, 0, original.getWidth(), 0, 0, original.getWidth(), original.getHeight());
		int [] pixels = new int[blur * blur];
		
		int hBlock = original.getHeight() / blur;
		int wBlock = original.getWidth() / blur;
		
		int restH = original.getHeight() % blur;
		int restW = original.getWidth() % blur;
		
		for(int i = 0; i < hBlock; i ++) {
			for(int j = 0; j < wBlock; j++) {
				int YY = i * blur;
				int XX = j * blur;
				int R = 0;
				int G = 0;
				int B = 0;
				bit.getPixels(pixels, 0, blur, XX, YY, blur, blur);
				for(int y = 0; y < blur; y++) {
					for(int x = 0; x < blur; x++) {
//						int color = pixels[YY * original.getWidth() + y * original.getWidth() + XX + x];
						int color = pixels[y * blur + x];
						R += (color >> 16) & 0xFF; 
						G += (color >> 8) & 0xFF; 
						B += color & 0xFF; 
					}
				}
				R = R / blur / blur;
				G = G / blur / blur;
				B = B / blur / blur;
				for(int y = 0; y < blur; y++) {
					for(int x = 0; x < blur; x++) {
//						pixels[YY * original.getWidth() + y * original.getWidth() + XX + x] = 0xFF000000 | (R << 16) | (G << 8) | B;
						pixels[y * blur + x] = 0xFF000000 | (R << 16) | (G << 8) | B;
					}
				}
				bit.setPixels(pixels, 0, blur, XX, YY, blur, blur);
			}
		}
		
		if(restW > 0) {
			for(int i = 0; i < hBlock; i++) {
				int YY = i * blur;
				int XX = wBlock * blur;
				int R = 0;
				int G = 0;
				int B = 0;
				for(int y = 0; y < blur; y++) {
					for(int x = 0; x < restW; x++) {
//						int color = pixels[YY * original.getWidth() + y * original.getWidth() + XX + x];
						int color = bit.getPixel(XX + x, YY + y);
						R += (color >> 16) & 0xFF; 
						G += (color >> 8) & 0xFF; 
						B += color & 0xFF; 
					}
				}
				R = R / blur / restW;
				G = G / blur / restW;
				B = B / blur / restW;
				for(int y = 0; y < blur; y++) {
					for(int x = 0; x < restW; x++) {
//						pixels[YY * original.getWidth() + y * original.getWidth() + XX + x] = 0xFF000000 | (R << 16) | (G << 8) | B;
						bit.setPixel(XX + x, YY + y, 0xFF000000 | (R << 16) | (G << 8) | B);
					}
				}
			}
		}
		
		if(restH > 0) {
			for(int j = 0; j < wBlock; j++) {
				int YY = hBlock * blur;
				int XX = j * blur;
				int R = 0;
				int G = 0;
				int B = 0;
				for(int y = 0; y < restH; y++) {
					for(int x = 0; x < blur; x++) {
//						int color = pixels[YY * original.getWidth() + y * original.getWidth() + XX + x];
						int color = bit.getPixel(XX + x, YY + y);
						R += (color >> 16) & 0xFF; 
						G += (color >> 8) & 0xFF; 
						B += color & 0xFF; 
					}
				}
				R = R / blur / restH;
				G = G / blur / restH;
				B = B / blur / restH;
				for(int y = 0; y < restH; y++) {
					for(int x = 0; x < blur; x++) {
//						pixels[YY * original.getWidth() + y * original.getWidth() + XX + x] = 0xFF000000 | (R << 16) | (G << 8) | B;
						bit.setPixel(XX + x, YY + y, 0xFF000000 | (R << 16) | (G << 8) | B);
					}
				}
			}
		}
//		bit.setPixels(pixels, 0, original.getWidth(), 0, 0, original.getWidth(), original.getHeight());
	}
	
	private void initPaint() {
		mPaint = new Paint(/*Paint.ANTI_ALIAS_FLAG*/);
		mPaint.setAntiAlias(true);
		mPaint.setFilterBitmap(true);
		mPaint.setDither(true);
		mPaint.setColor(color);
		mPaint.setStrokeWidth(penSize);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		
		mPaint.setAlpha(255);
//		mPaint.setXfermode(null);
//		mPaint.setStrokeWidth(penSize);
		updatePaint();
	}

	private void updatePaint() {
		imageView.setPaint(mPaint);
	}

	private void ReleaseRes(){
		mPenList.clear();
		if (bitmap != null && !bitmap.isRecycled()){
			imageView.setImageBitmap(null);
			bitmap.recycle();
			bitmap = null;
		}
		if (null != original && !original.isRecycled()) {
			original.recycle();
			original = null;
		}
		if(mProgressDialog != null&&mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}
		System.gc();
	}
	
	@Override
	public void onDestroy() {
		ZebraLog.d("DoodleActivity", "DoodleActivity onDestroy");
		ReleaseRes();
		super.onDestroy();
	}
	
	private void showWarningDialog() {

//		View view = LayoutInflater.from(getThisActivity()).inflate(R.layout.zebra_photoedit_warn_dialog, null);
//		new AlertDialog.Builder(getThisActivity())
//			.setView(view)
//			.setPositiveButton(R.string.zebra_doodle_confirm,// 设置确定按钮
//					new DialogInterface.OnClickListener() {
//
//						@Override
//						public void onClick(DialogInterface arg0, int arg1) {
//							// TODO Auto-generated method stub
//							ReleaseRes();
////							setResult(RESULT_CANCELED);
////							DoodleActivity.this.finish();
//							setResultCancel();
//						}
//
//					})
//			.setNegativeButton(R.string.zebra_doodle_cancel,
//					new DialogInterface.OnClickListener() {
//
//						public void onClick(DialogInterface dialog,
//								int which) {
//						}
//
//					}).create().show();
		
		try {
            View.OnClickListener posBtnListener = new View.OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    ReleaseRes();
//                  setResult(RESULT_CANCELED);
//                  DoodleActivity.this.finish();
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
        	if(CropImageActivity.handler!=null){
				CropImageActivity.handler.sendEmptyMessage(CropImageActivity.HIDE_VIEW_IN_DOODLE);
			}
        	if(imageView.hasDrawed()||beedit) {
        		showWarningDialog();
        		return true;
        	}
        }
        return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onDrawStart() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDrawEnd() {
		// TODO Auto-generated method stub
		if(imageView.hasDrawed()) {
			mUndoBtn.setEnabled(true);
		} else {
			mUndoBtn.setEnabled(false);
		}
	}
	
	@Override
	public void finish() {
		if (imageView != null && imageView.didScaleHappen()) {
			ReportUtils.report("CliOper", "", "", "Pic_edit", "Zoom_graffiti", 0, 0, "", "", "", "");
		}		
		super.finish();
	}
	
//	@Override
//	public void onActivityResult(int requestCode, int resultCode, Intent data) {
//		super.onActivityResult(requestCode, resultCode, data);
//		PluginProxy.onSendResult(getOutActivity(), requestCode, resultCode, data, false);
//	}
	
	private void setResultCancel() {
		if(isFromCrop) {
			setResult(RESULT_CANCELED);
			DoodleActivity.this.finish();
		} else {
		    ZebraPluginProxy.backToPhoto(getIntent(), getOutActivity());
		}
	}

	@Override
	public void onZoomEnd() {
		// TODO Auto-generated method stub
		if(mCurTab == R.id.doodle_mosaic_btn) {
			initMosaicPaint();
		}
	}
}