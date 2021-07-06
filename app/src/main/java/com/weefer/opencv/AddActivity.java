package com.weefer.opencv;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.weefer.opencv.utils.CameraView;
import com.weefer.opencv.utils.Labels;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import at.markushi.ui.CircleButton;

public class AddActivity extends Activity implements CvCameraViewListener2 {

    private static final String TAG = "OCVSample::Activity";
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    public static final int JAVA_DETECTOR = 0;
    public static final int NATIVE_DETECTOR = 1;

    public static final int TRAINING = 0;
    public static final int SEARCHING = 1;
    public static final int IDLE = 2;

    private static final int frontCam = 1;
    private static final int backCam = 2;
    private int faceState = IDLE;
    private Mat mRgba;
    private Mat mGray;
    private File mCascadeFile;
    private CascadeClassifier mJavaDetector;

    private int mDetectorType = JAVA_DETECTOR;
    private String[] mDetectorName;

    private float mRelativeFaceSize = 0.2f;
    private int mAbsoluteFaceSize = 0;
    private int mLikely = 999;

    String mPath = "";

    private CameraView mOpenCvCameraView;
    private int mChooseCamera = backCam;

    EditText text;
    TextView textresult;
    private ImageView Iv;
    Bitmap mBitmap;
    Handler mHandler;

    CtrRecognizer fr;
    ToggleButton toggleButtonRec, toggleButtonTrain;
    CircleButton imCamera;

    static final long MAXIMG = 10;

    int countImages = 0;

    Labels labelsFile;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    fr = new CtrRecognizer(mPath);
                    String s = getResources().getString(R.string.Straininig);
                    Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
                    fr.load();

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public AddActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.add_activity);

        mOpenCvCameraView = findViewById(R.id.surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/KodeTR/";

        Log.e("KodeTR : ", mPath);

        labelsFile = new Labels(mPath);

        Iv = findViewById(R.id.imageView1);
        textresult = findViewById(R.id.textView1);

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.obj == "IMG") {
                    Canvas canvas = new Canvas();
                    canvas.setBitmap(mBitmap);
                    Iv.setImageBitmap(mBitmap);
                    if (countImages >= MAXIMG - 1) {
                        toggleButtonRec.setChecked(false);
                        grabarOnclick();
                    }
                } else {
                    textresult.setText(msg.obj.toString());
                }
            }
        };
        text = findViewById(R.id.editText1);
        toggleButtonRec = findViewById(R.id.toggleButton2);
        toggleButtonTrain = findViewById(R.id.toggleButton1);
        imCamera = findViewById(R.id.imageButton1);
        text.setVisibility(View.INVISIBLE);
        textresult.setVisibility(View.INVISIBLE);
        toggleButtonRec.setVisibility(View.INVISIBLE);

        text.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((text.getText().toString().length() > 0) && (toggleButtonTrain.isChecked()))
                    toggleButtonRec.setVisibility(View.VISIBLE);
                else
                    toggleButtonRec.setVisibility(View.INVISIBLE);
                return false;
            }
        });

        toggleButtonTrain.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (toggleButtonTrain.isChecked()) {
                    textresult.setVisibility(View.VISIBLE);
                    text.setVisibility(View.VISIBLE);
                    textresult.setText(getResources().getString(R.string.SFaceName));
                    if (text.getText().toString().length() > 0)
                        toggleButtonRec.setVisibility(View.VISIBLE);
                } else {
                    textresult.setText("");
                    text.setVisibility(View.INVISIBLE);
                    textresult.setText("");
                    {
                        toggleButtonRec.setVisibility(View.INVISIBLE);
                        text.setVisibility(View.INVISIBLE);
                    }
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.Straininig), Toast.LENGTH_LONG).show();
                    fr.train();
                }
            }
        });

        toggleButtonRec.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                grabarOnclick();
            }
        });
        imCamera.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mChooseCamera == frontCam) {
                    mChooseCamera = backCam;
                    mOpenCvCameraView.setCamBack();
                } else {
                    mChooseCamera = frontCam;
                    mOpenCvCameraView.setCamFront();
                }
            }
        });

        boolean success = (new File(mPath)).mkdirs();
        if (!success) {
            Log.e("Error", "Error creating directory");
        }
    }

    void grabarOnclick() {
        if (toggleButtonRec.isChecked())
            faceState = TRAINING;
        else {
            if (faceState == TRAINING) ;
            countImages = 0;
            faceState = IDLE;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }

        MatOfRect faces = new MatOfRect();
        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        } else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] facesArray = faces.toArray();

        if ((facesArray.length == 1)
                && (faceState == TRAINING)
                && (countImages < MAXIMG)
                && (!text.getText().toString().isEmpty())) {

            Mat m;
            Rect r = facesArray[0];

            m = mRgba.submat(r);
            mBitmap = Bitmap.createBitmap(m.width(), m.height(), Bitmap.Config.ARGB_8888);

            Utils.matToBitmap(m, mBitmap);
            Message msg = new Message();
            String textTochange = "IMG";
            msg.obj = textTochange;
            mHandler.sendMessage(msg);

            if (countImages < MAXIMG) {
                fr.add(m, text.getText().toString());
                countImages++;
            }

        } else if ((facesArray.length > 0) && (faceState == SEARCHING)) {

            Mat m;
            m = mGray.submat(facesArray[0]);
            mBitmap = Bitmap.createBitmap(m.width(), m.height(), Bitmap.Config.ARGB_8888);

            Utils.matToBitmap(m, mBitmap);
            Message msg = new Message();
            String textTochange = "IMG";
            msg.obj = textTochange;
            mHandler.sendMessage(msg);

            textTochange = fr.predict(m);
            mLikely = fr.getProb();
            msg = new Message();
            msg.obj = textTochange;
            mHandler.sendMessage(msg);
        }
        for (int i = 0; i < facesArray.length; i++)
            Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);
        return mRgba;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            startActivity(new Intent(AddActivity.this, MainFace.class));
            AddActivity.this.finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}