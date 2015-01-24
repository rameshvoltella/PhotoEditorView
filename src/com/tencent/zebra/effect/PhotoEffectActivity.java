package com.tencent.zebra.effect;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.AssetManager;
import android.media.effect.Effect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.tencent.photoplus.R;
import com.tencent.ttpic.sdk.util.Pitu;
import com.tencent.zebra.crop.CropImageActivity;
import com.tencent.zebra.editutil.Util;
import com.tencent.zebra.ui.ZebraBaseActivity;
import com.tencent.zebra.util.ReportUtils;
import com.tencent.zebra.util.ZebraCustomDialog;

import java.io.InputStream;
import java.util.ArrayList;

import cooperation.zebra.ZebraPluginProxy;
import static android.content.Intent.EXTRA_STREAM;

/**
 * Version 1.0
 * <p/>
 * <p/>
 * Date: 2014-09-24 16:58 Author: retryu
 * <p/>
 * <p/>
 * Copyright © 1998-2014 Tencent Technology (Shenzhen) Company Ltd.
 */
public class PhotoEffectActivity extends ZebraBaseActivity implements
		View.OnClickListener {

	private EffectController effectController;
	private ViewGroup mContainner;
	public boolean hasEdit = false;
	private Button btnCancel;
	private Button btnConfirm;
	public static final int REQ_TO_PITU = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_photo_editor);

		String path = getIntent().getStringExtra("image_path");
		Log.e("debug", "path:" + path);

		mContainner = (ViewGroup) findViewById(R.id.image_parent);
		ViewGroup effectLayout = (ViewGroup) findViewById(R.id.layout_effect_opration_container);
		ViewGroup effectTabLayout = (ViewGroup) findViewById(R.id.layout_effect_containner);
		effectController = new EffectController(this, mContainner,
				effectLayout, effectTabLayout, path);

		btnCancel = (Button) findViewById(R.id.crop_cancel);
		btnConfirm = (Button) findViewById(R.id.crop_confirm);
		btnCancel.setOnClickListener(this);
		btnConfirm.setOnClickListener(this);
		effectController.bindConfirm(btnConfirm, btnCancel);

		// TODO btnConfirme和Effect的子类需要解耦
		// effectController.btnCancel = btnCancel;
		// effectController.btnConfirm = btnConfirm;
		// effectController.bindConfirm();

		try {
			AssetManager am = getAssets();
			InputStream is = am.open("fennen.png");
			Log.e("EffectController", " GPUImageFilterTools.filters.filters  sucess " + is);
		} catch (Exception e) {
			Log.e("EffectController", " GPUImageFilterTools.filters.filters  fialed ");
		}
	}

	public int getImageViewWidth() {
		if (null != mContainner)
			return mContainner.getWidth();
		else
			return 0;
	}

	public int getImageViewHeight() {
		if (null != mContainner)
			return mContainner.getHeight();
		else
			return 0;
	}

	// TODO
	public void setRightBottomBtnEnabled(boolean enabled) {

	}

	public void setRightBottomBtnText(int nameid) {
		btnConfirm.setText(nameid);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.crop_cancel: {

            ReportUtils.report("", "", "", "0X8004B3A", "0X8004B3A", 0, 0, "", "", "", "");
			Effects effect = effectController.getEffect();

			// TODO
			if (hasEdit
					|| btnConfirm.getText().equals(
							getResources().getString(R.string.zebra_cut_pic))) {
				showWarningDialog();
			} else if (effect.hasEdit == true) {
				showWarningDialog();
			} else {
				setResultCancel();
				finish();
			}

			break;
		}
		case R.id.crop_confirm: {
			effectController.getEffect().confirm(view);
            ReportUtils.report("", "", "", "0X8004B3A", "0X8004B3B", 0, 0, "", "", "", "");
			if (effectController.getCurrentType() == EffectController.effectType.CROP) {
				CropEffect cropEffect = (CropEffect) effectController
						.getEffect();

			}

			// Effects effect = effectController.getEffect();
			// Util.saveOutput(this, effect.mPath, effect.effectBitmap, true);
			// Toast.makeText(this,"  保存完成",Toast.LENGTH_SHORT).show();
			break;
		}

		}
	}

	private void showWarningDialog() {
		try {
			View.OnClickListener posBtnListener = new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					effectController.getEffect().recyleImage();
					setResultCancel();
					// TODO
					finish();
				}
			};
			Dialog dialog = ZebraCustomDialog.newCustomDialog(
					getThisActivity(),
					getString(R.string.zebra_tips_sure_alert),
					getString(R.string.zebra_doodle_canceltips),
					getString(R.string.zebra_doodle_confirm), posBtnListener,
					getString(R.string.zebra_doodle_cancel), null);
			dialog.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setResultCancel() {
		ZebraPluginProxy.backToPhoto(getIntent(), getOutActivity());
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case REQ_TO_PITU:
			if (data != null) {
				ArrayList<Uri> result = data
						.getParcelableArrayListExtra(EXTRA_STREAM);
				StringBuffer sb = new StringBuffer();
				if (result != null && result.size() > 0) {
					for (Uri uri : result) {
						sb.append(uri.toString());
					}
				}
				updateImage(sb.toString());
				effectController.resetFilterPostion();
			} else {
				effectController.resetFilterPostion();
			}
			break;

		}
	}

	/**
	 * 更新图片
	 * 
	 * @param path
	 */
	public void updateImage(String path) {
		effectController.updateImageFromPath(path);
	}
}
