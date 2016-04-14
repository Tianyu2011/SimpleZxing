
package com.ztiany2011.simplezxing;

import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.ztiany2011.simplezxing.camera.CameraManager;
import com.ztiany2011.simplezxing.camera.RGBLuminanceSource;

public final class CaptureActivity extends Activity implements SurfaceHolder.Callback {

  private static final String TAG = CaptureActivity.class.getSimpleName();

  public static final int HISTORY_REQUEST_CODE = 0x0000bacc;

  private CameraManager cameraManager;
  private CaptureActivityHandler handler;
  private Result savedResultToShow;
  private boolean hasSurface;
  private Collection<BarcodeFormat> decodeFormats;
  private Map<DecodeHintType,?> decodeHints;
  private String characterSet;
  private InactivityTimer inactivityTimer;
  private BeepManager beepManager;
  private CheckBox light_check;
  private LinearLayout open_gallery;
  CaptureActivity mContext;
  private ProgressDialog mProgress;
  private String photo_path;
  private Bitmap scanBitmap;
  private static final int REQUEST_CODE = 100;
  private static final int PARSE_BARCODE_SUC = 300;
  private static final int PARSE_BARCODE_FAIL = 303;
  private ImageView scanLine;
  private LinearLayout lin_light_check;

  public Handler getHandler() {
    return handler;
  }

  CameraManager getCameraManager() {
    return cameraManager;
  }

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    setContentView(R.layout.capture);

    hasSurface = false;
    inactivityTimer = new InactivityTimer(this);
    beepManager = new BeepManager(this);
    mContext = this;
  }

  @Override
  protected void onResume() {
    super.onResume();

    DisplayMetrics metrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(metrics);
    cameraManager = new CameraManager(getApplication());
    cameraManager.setManualFramingRect(metrics.widthPixels, metrics.heightPixels);

    light_check = (CheckBox) findViewById(R.id.light_check);
    open_gallery = (LinearLayout) findViewById(R.id.open_gallery);
    lin_light_check = (LinearLayout) findViewById(R.id.lin_light_check);
    lin_light_check.setOnClickListener(new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if(light_check.isChecked()){
				light_check.setChecked(false);
				cameraManager.setTorch(false);
			}else{
				light_check.setChecked(true);
				cameraManager.setTorch(true);
			}
			
		}
	});
    light_check.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if(isChecked){
				cameraManager.setTorch(true);
			}else{
				cameraManager.setTorch(false);
			}
		}
	});
    open_gallery.setOnClickListener(new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Intent innerIntent = new Intent();  
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
			    innerIntent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            } else {
                innerIntent.setAction(Intent.ACTION_GET_CONTENT);
            };
	        innerIntent.setType("image/*");  
	        Intent wrapperIntent = Intent.createChooser(innerIntent, "选择二维码图片");  
	        mContext.startActivityForResult(wrapperIntent, REQUEST_CODE);  
		}
	});
    scanLine = (ImageView) findViewById(R.id.capture_scan_line);
    TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT,
			0.9f);
	animation.setDuration(4500);
	animation.setRepeatCount(-1);
	animation.setRepeatMode(Animation.RESTART);
	scanLine.startAnimation(animation);
    

    beepManager.updatePrefs();
    inactivityTimer.onResume();
    
    decodeFormats = null;
    characterSet = null;
    handler = null;

    SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
    SurfaceHolder surfaceHolder = surfaceView.getHolder();
    if (hasSurface) {
      initCamera(surfaceHolder);
    } else {
      surfaceHolder.addCallback(this);
    }
  }
  

  
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == RESULT_OK){  
            switch(requestCode){  
            case REQUEST_CODE:  
                //获取选中图片的路径  
                photo_path = ImageUntil.getPath(mContext,data.getData());             
                mProgress = new ProgressDialog(mContext);  
                mProgress.setMessage("正在扫描...");  
                mProgress.setCancelable(false);  
                mProgress.show();  
                  
                new Thread(new Runnable() {  
                    @Override  
                    public void run() {  
                        Result result = scanningImage(photo_path);  
                        if (result != null) {  
                            Message m = mHandler.obtainMessage();  
                            m.what = PARSE_BARCODE_SUC;  
                            m.obj = result.getText();  
                            mHandler.sendMessage(m);  
                        } else {  
                            Message m = mHandler.obtainMessage();  
                            m.what = PARSE_BARCODE_FAIL;  
                            m.obj = "Scan failed!";  
                            mHandler.sendMessage(m);  
                        }  
                          
                    }  
                }).start();  
                  
                break;  
              
            }  
        }  
  }
  
  public Result scanningImage(String path) {
		if(TextUtils.isEmpty(path)){
			return null;
		}
		Hashtable<DecodeHintType, String> hints = new Hashtable<DecodeHintType, String>();
		hints.put(DecodeHintType.CHARACTER_SET, "UTF8"); 

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true; 
		scanBitmap = BitmapFactory.decodeFile(path, options);
		options.inJustDecodeBounds = false;
		int sampleSize = (int) (options.outHeight / (float) 200);
		if (sampleSize <= 0)
			sampleSize = 1;
		options.inSampleSize = sampleSize;
		scanBitmap = BitmapFactory.decodeFile(path, options);
		RGBLuminanceSource source = new RGBLuminanceSource(scanBitmap);
		BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(source));
		QRCodeReader reader = new QRCodeReader();
		try {
			return reader.decode(bitmap1, hints);

		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (ChecksumException e) {
			e.printStackTrace();
		} catch (FormatException e) {
			e.printStackTrace();
		}
		return null;
	}

  @Override
  protected void onPause() {
    if (handler != null) {
      handler.quitSynchronously();
      handler = null;
    }
    inactivityTimer.onPause();
    beepManager.close();
    cameraManager.closeDriver();
    if (!hasSurface) {
      SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
      SurfaceHolder surfaceHolder = surfaceView.getHolder();
      surfaceHolder.removeCallback(this);
    }
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    inactivityTimer.shutdown();
    super.onDestroy();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    switch (keyCode) {
      case KeyEvent.KEYCODE_FOCUS:
      case KeyEvent.KEYCODE_CAMERA:
        // Handle these events so they don't launch the Camera app
        return true;
      // Use volume up/down to turn on light
      case KeyEvent.KEYCODE_VOLUME_DOWN:
        cameraManager.setTorch(false);
        return true;
      case KeyEvent.KEYCODE_VOLUME_UP:
        cameraManager.setTorch(true);
        return true;
    }
    return super.onKeyDown(keyCode, event);
  }
  

  private Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			mProgress.dismiss();
			switch (msg.what) {
			case PARSE_BARCODE_SUC:
				dosomething((String)msg.obj);
				break;
			case PARSE_BARCODE_FAIL:
				Toast.makeText(mContext, (String)msg.obj, Toast.LENGTH_LONG).show();
				break;

			}
		}
		
	};

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    if (holder == null) {
      Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
    }
    if (!hasSurface) {
      hasSurface = true;
      initCamera(holder);
    }
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    hasSurface = false;
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

  }

  /**
   * A valid barcode has been found, so give an indication of success and show the results.
   *
   * @param rawResult The contents of the barcode.
   * @param scaleFactor amount by which thumbnail was scaled
   * @param barcode   A greyscale bitmap of the camera data which was decoded.
   */
  public void handleDecode(Result rawResult) {
    inactivityTimer.onActivity();
    beepManager.playBeepSoundAndVibrate();
    dosomething(rawResult.getText());
  }
  
  private void dosomething(String result){
	  Intent intent = new Intent(mContext, ShowResultActivity.class);
	  intent.putExtra("result", result);
	  startActivity(intent);
  }

  private void initCamera(SurfaceHolder surfaceHolder) {
    if (surfaceHolder == null) {
      throw new IllegalStateException("No SurfaceHolder provided");
    }
    if (cameraManager.isOpen()) {
      Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
      return;
    }
    try {
      cameraManager.openDriver(surfaceHolder);
      // Creating the handler starts the preview, which can also throw a RuntimeException.
      if (handler == null) {
        handler = new CaptureActivityHandler(this, decodeFormats, decodeHints, characterSet, cameraManager);
      }
      decodeOrStoreSavedBitmap(null, null);
    } catch (IOException ioe) {
      Log.w(TAG, ioe);
      displayFrameworkBugMessageAndExit();
    } catch (RuntimeException e) {
      // Barcode Scanner has seen crashes in the wild of this variety:
      // java.?lang.?RuntimeException: Fail to connect to camera service
      Log.w(TAG, "Unexpected error initializing camera", e);
      displayFrameworkBugMessageAndExit();
    }
  }

  private void displayFrameworkBugMessageAndExit() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(getString(R.string.app_name));
    builder.setMessage(getString(R.string.msg_camera_framework_bug));
    builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
    builder.setOnCancelListener(new FinishListener(this));
    builder.show();
  }

  public void restartPreviewAfterDelay(long delayMS) {
    if (handler != null) {
      handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
    }
  }

  private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
	    // Bitmap isn't used yet -- will be used soon
	    if (handler == null) {
	      savedResultToShow = result;
	    } else {
	      if (result != null) {
	        savedResultToShow = result;
	      }
	      if (savedResultToShow != null) {
	        Message message = Message.obtain(handler, R.id.decode_succeeded, savedResultToShow);
	        handler.sendMessage(message);
	      }
	      savedResultToShow = null;
	    }
	  }

}
