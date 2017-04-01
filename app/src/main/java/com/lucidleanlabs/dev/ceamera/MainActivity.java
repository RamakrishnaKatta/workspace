package com.lucidleanlabs.dev.ceamera;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Random;

import static com.lucidleanlabs.dev.ceamera.R.id.camera_image;

public class MainActivity extends Activity implements Callback,
        OnClickListener {

    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    private static final int CAMERA_CAPTURE_VIDEO_REQUEST_CODE = 200;

    private static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;


    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private ImageButton flashCameraButton;
    private int cameraId;
    private boolean flashmode = false;
    private int rotation;
    ImageButton screenshot;
    RelativeLayout camview;
    private Uri uri;
    Bitmap inputBMP= null,bmp,bmp1;
    ImageView camImage ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // camera surface view created
        cameraId = CameraInfo.CAMERA_FACING_BACK;
        flashCameraButton = (ImageButton) findViewById(R.id.flash);
        surfaceView = (SurfaceView) findViewById(R.id.surfacview);
        surfaceHolder = surfaceView.getHolder();
        screenshot =(ImageButton)findViewById(R.id.screen_shot);
        surfaceHolder.addCallback(this);
        flashCameraButton.setOnClickListener(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        camview = (RelativeLayout)findViewById(R.id.cam_view);
        camImage = (ImageView)findViewById(camera_image);

        if (!getBaseContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FLASH)) {
            flashCameraButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!openCamera(CameraInfo.CAMERA_FACING_BACK)) {
            alertCameraDialog();
        }

    }

    private boolean openCamera(int id) {
        boolean result = false;
        cameraId = id;
        releaseCamera();
        try {
            camera = Camera.open(cameraId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (camera != null) {
            try {
                setUpCamera(camera);
                camera.setErrorCallback(new ErrorCallback() {

                    @Override
                    public void onError(int error, Camera camera) {

                    }
                });
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
                result = true;
            } catch (IOException e) {
                e.printStackTrace();
                result = false;
                releaseCamera();
            }
        }
        return result;
    }

    private void setUpCamera(Camera c) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degree = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degree = 0;
                break;
            case Surface.ROTATION_90:
                degree = 90;
                break;
            case Surface.ROTATION_180:
                degree = 180;
                break;
            case Surface.ROTATION_270:
                degree = 270;
                break;

            default:
                break;
        }

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            // frontFacing
            rotation = (info.orientation + degree) % 330;
            rotation = (360 - rotation) % 360;
        } else {
            // Back-facing
            rotation = (info.orientation - degree + 360) % 360;
        }
        c.setDisplayOrientation(rotation);
        Parameters params = c.getParameters();

        showFlashButton(params);

        List<String> focusModes = params.getSupportedFlashModes();
        if (focusModes != null) {
            if (focusModes
                    .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFlashMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
        }

        params.setRotation(rotation);
    }

    private void showFlashButton(Parameters params) {
        boolean showFlash = (getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FLASH) && params.getFlashMode() != null)
                && params.getSupportedFlashModes() != null
                && params.getSupportedFocusModes().size() > 1;

        flashCameraButton.setVisibility(showFlash ? View.VISIBLE
                : View.INVISIBLE);

    }

    private void releaseCamera() {
        try {
            if (camera != null) {
                camera.setPreviewCallback(null);
                camera.setErrorCallback(null);
                camera.stopPreview();
                camera.release();
                camera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("error", e.toString());
            camera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.flash:
                flashOnButton();
                break;

            case R.id.capture_image:
                takeImage();
                break;
            case R.id.screen_shot:
                takescreenshot();
            default:
                break;
        }
    }


    public void takescreenshot(){
        Random num = new Random();
        int nu = num.nextInt(1000);
        camview.setDrawingCacheEnabled(true);
        camview.buildDrawingCache(true);
        Bitmap bitmap = Bitmap.createBitmap(camview.getDrawingCache());
        camview.setDrawingCacheEnabled(false);
        ByteArrayOutputStream outputStream =  new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.JPEG,100,outputStream);
        byte[] bitmapdata = outputStream.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bitmapdata);


        String picId = String.valueOf(nu);
        String myfile = "Image" +picId+".jpeg";

        File dir_image =  new File(Environment.getExternalStorageDirectory()+File.separator+"L Catalogue");
        dir_image.mkdirs();

        try{
            File tmpfile = new File(dir_image,myfile);
            FileOutputStream fileOutputStream = new FileOutputStream(tmpfile);
            byte[] buf =  new byte[1024];
            int len;
            while((len = inputStream.read(buf))>0){
                fileOutputStream.write(buf,0,len);
            }
            inputStream.close();
            fileOutputStream.close();
            Toast.makeText(getApplicationContext(), "the file is saved at : SD/L catalogue", Toast.LENGTH_SHORT).show();
            bmp1 = null;
            camImage.setImageBitmap(bmp1);
            camera.startPreview();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private PictureCallback mPicture = new PictureCallback() {   //THIS METHOD AND THE METHOD BELOW
        //CONVERT THE CAPTURED IMAGE IN A JPG FILE AND SAVE IT

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File dir_image2 = new  File(Environment.getExternalStorageDirectory()+
                    File.separator+"Ultimate Entity Detector");
            dir_image2.mkdirs();  //AGAIN CHOOSING FOLDER FOR THE PICTURE(WHICH IS LIKE A SURFACEVIEW
            //SCREENSHOT)

            File tmpFile = new File(dir_image2,"TempGhost.jpg"); //MAKING A FILE IN THE PATH
            //dir_image2(SEE RIGHT ABOVE) AND NAMING IT "TempGhost.jpg" OR ANYTHING ELSE
            try { //SAVING
                FileOutputStream fos = new FileOutputStream(tmpFile);
                fos.write(data);
                fos.close();
                //grabImage();
            } catch (FileNotFoundException e) {
                Toast.makeText(getApplicationContext(),"Error",Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(),"Error",Toast.LENGTH_LONG).show();
            }

            String path = (Environment.getExternalStorageDirectory()+
                    File.separator+"Ultimate EntityDetector"+
                    File.separator+"TempGhost.jpg");//<---

            BitmapFactory.Options options = new BitmapFactory.Options();//<---
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;//<---
            bmp1 = BitmapFactory.decodeFile(path, options);//<---     *********(SEE BELOW)
            //THE LINES ABOVE READ THE FILE WE SAVED BEFORE AND CONVERT IT INTO A BitMap
            camImage.setImageBitmap(bmp1); //SETTING THE BitMap AS IMAGE IN AN IMAGEVIEW(SOMETHING
            //LIKE A BACKGROUNG FOR THE LAYOUT)

            tmpFile.delete();
            takescreenshot();//CALLING THIS METHOD TO TAKE A SCREENSHOT
            //********* THAT LINE MIGHT CAUSE A CRASH ON SOME PHONES (LIKE XPERIA T)<----(SEE HERE)
            //IF THAT HAPPENDS USE THE LINE "bmp1 =decodeFile(tmpFile);" WITH THE METHOD BELOW

        }
    };

    public Bitmap decodeFile(File f) {  //FUNCTION BY Arshad Parwez
        Bitmap b = null;
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;

            FileInputStream fis = new FileInputStream(f);
            BitmapFactory.decodeStream(fis, null, o);
            fis.close();
            int IMAGE_MAX_SIZE = 1000;
            int scale = 1;
            if (o.outHeight > IMAGE_MAX_SIZE || o.outWidth > IMAGE_MAX_SIZE) {
                scale = (int) Math.pow(
                        2,
                        (int) Math.round(Math.log(IMAGE_MAX_SIZE
                                / (double) Math.max(o.outHeight, o.outWidth))
                                / Math.log(0.5)));
            }

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            fis = new FileInputStream(f);
            b = BitmapFactory.decodeStream(fis, null, o2);
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return b;
    }


    private void takeImage() {
        camera.takePicture(null, null, new PictureCallback() {

            private File imageFile;

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                try {
                    // convert byte array into bitmap
                    Bitmap loadedImage = null;
                    Bitmap rotatedBitmap = null;
                    loadedImage = BitmapFactory.decodeByteArray(data, 0,
                            data.length);

                    // rotate Image
                    Matrix rotateMatrix = new Matrix();
                    rotateMatrix.postRotate(rotation);
                    rotatedBitmap = Bitmap.createBitmap(loadedImage, 0, 0,
                            loadedImage.getWidth(), loadedImage.getHeight(),
                            rotateMatrix, false);
                    String state = Environment.getExternalStorageState();
                    File folder = null;
                    if (state.contains(Environment.MEDIA_MOUNTED)) {
                        folder = new File(Environment
                                .getExternalStorageDirectory() + "/Demo");
                    } else {
                        folder = new File(Environment
                                .getExternalStorageDirectory() + "/Demo");
                    }

                    boolean success = true;
                    if (!folder.exists()) {
                        success = folder.mkdirs();
                    }
                    if (success) {
                        java.util.Date date = new java.util.Date();
                        imageFile = new File(folder.getAbsolutePath()
                                + File.separator
                                + new Timestamp(date.getTime()).toString()
                                + "Image.jpg");

                        imageFile.createNewFile();
                    } else {
                        Toast.makeText(getBaseContext(), "Image Not saved",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ByteArrayOutputStream ostream = new ByteArrayOutputStream();

                    // save image into gallery
                    rotatedBitmap.compress(CompressFormat.JPEG, 100, ostream);

                    FileOutputStream fout = new FileOutputStream(imageFile);
                    fout.write(ostream.toByteArray());
                    fout.close();
                    ContentValues values = new ContentValues();

                    values.put(Images.Media.DATE_TAKEN,
                            System.currentTimeMillis());
                    values.put(Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.MediaColumns.DATA,
                            imageFile.getAbsolutePath());

                    MainActivity.this.getContentResolver().insert(
                            Images.Media.EXTERNAL_CONTENT_URI, values);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
    }

    private void flipCamera() {
        int id = (cameraId == CameraInfo.CAMERA_FACING_BACK ? CameraInfo.CAMERA_FACING_FRONT
                : CameraInfo.CAMERA_FACING_BACK);
        if (!openCamera(id)) {
            alertCameraDialog();
        }
    }

    private void alertCameraDialog() {
        AlertDialog.Builder dialog = createAlert(MainActivity.this,
                "Camera info", "error to open camera");
        dialog.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();

            }
        });

        dialog.show();
    }

    private Builder createAlert(Context context, String title, String message) {

        AlertDialog.Builder dialog = new AlertDialog.Builder(
                new ContextThemeWrapper(context,
                        android.R.style.Theme_Holo_Light_Dialog));
        dialog.setIcon(R.mipmap.ic_launcher_round);
        if (title != null)
            dialog.setTitle(title);
        else
            dialog.setTitle("Information");
        dialog.setMessage(message);
        dialog.setCancelable(false);
        return dialog;

    }

    private void flashOnButton() {
        if (camera != null) {
            try {
                Parameters param = camera.getParameters();
                param.setFlashMode(!flashmode ? Parameters.FLASH_MODE_TORCH
                        : Parameters.FLASH_MODE_OFF);
                camera.setParameters(param);
                flashmode = !flashmode;
            } catch (Exception e) {
                // TODO: handle exception
            }

        }
    }
}





























//
//import android.app.Activity;
//import android.content.ContentValues;
//import android.content.DialogInterface;
//import android.content.pm.PackageManager;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.graphics.Camera;
//import android.graphics.Matrix;
//import android.hardware.Camera.CameraInfo;
//import android.os.Bundle;
//import android.os.Environment;
//import android.provider.MediaStore;
//import android.support.v7.app.AlertDialog;
//import android.support.v7.view.ActionMode;
//import android.util.Log;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.view.Surface;
//import android.view.SurfaceHolder;
//import android.view.SurfaceView;
//import android.hardware.camera2.*;
//import android.view.View;
//import android.view.WindowManager;
//import android.widget.ImageButton;
//import android.widget.Toast;
//
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.sql.Timestamp;
//import java.util.List;
//
//public class MainActivity extends Activity implements ActionMode.Callback,View.OnClickListener{
//
//    private SurfaceView surfaceView;
//    Camera camera;
//    ImageButton flip_camera,flash_camera,capture_image;
//    int cameraId;
//    boolean flashmode= true;
//    int rotation;
//
//    public static final int MY_PERMISSIONS_REQUEST_CAMERA = 100;
//    public static final String ALLOw_KEY ="ALLOWED";
//    public static final String CAMERA_PREF = "camera_pref";
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        cameraId = android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
//        flip_camera = (ImageButton)findViewById(R.id.switch_camera);
//        flash_camera= (ImageButton)findViewById(R.id.flash);
//        capture_image = (ImageButton)findViewById(R.id.capture_image);
//        surfaceView = (SurfaceView)findViewById(R.id.surfacview);
//
//
//        surfaceHolder.addCallback(this);
//        flip_camera.setOnClickListener(this);
//        capture_image.setOnClickListener(this);
//        flash_camera.setOnClickListener(this);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//
//        if (Camera.getNumberOfCameras() > 1){
//            flip_camera.setVisibility(View.VISIBLE);
//        }
//        if (!getBaseContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)){
//            flash_camera.setVisibility(View.GONE);
//        }
//
//
//        @Override
//        public void surfaceCreated(SurfaceHolder holder) {
//            if (!openCamera(CameraInfo.CAMERA_FACING_BACK)){
//                alertCameraDialog();
//            }
//
//        }
//
//        private boolean openCamera(int id){
//                boolean result = false;
//                cameraId = id;
//                releaseCamera();
//        try {
//            camera =Camera.open(cameraId);
//
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }if (camera != null){
//        try{
//            setUpCamera(camera);
//            camera.setErrorCallback(new android.hardware.Camera.ErrorCallback() {
//                @Override
//                public void onError(int error, android.hardware.Camera camera) {
//
//                }
//            });
//            camera.setPreviewDisplay)(surfaceHolder);
//            camera.startPreview();
//            result = true;
//        }catch (IOException e){
//            e.printStackTrace();
//            result = false;
//            releaseCamera();
//
//        }
//
//    }
//
//    private void setUpCamera(Camera c ) {
//        Camera.CameraInfo info = new Camera.CameraInfo();
//        Camera.getCameraInfo(cameraId, info);
//        rotation = getWindowManager().getDefaultDisplay().getRotation();
//        int degree = 0;
//        switch (rotation) {
//            case Surface.ROTATION_0:
//                degree = 0;
//                break;
//            case Surface.ROTATION_90:
//                degree = 90;
//                break;
//            case Surface.ROTATION_180:
//                degree = 180;
//                break;
//            case Surface.ROTATION_270:
//                degree = 270;
//                break;
//
//            default:
//                break;
//        }
//
//        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//            // frontFacing
//            rotation = (info.orientation + degree) % 330;
//            rotation = (360 - rotation) % 360;
//        } else {
//            // Back-facing
//            rotation = (info.orientation - degree + 360) % 360;
//        }
//        c.setDisplayOrientation(rotation);
//        android.hardware.Camera.Parameters params = c.getParameters();
//
//        showFlashButton(params);
//
//        List<String> focusModes = params.getSupportedFlashModes();
//        if (focusModes != null) {
//            if (focusModes
//                    .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
//                params.setFlashMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
//            }
//        }
//
//        params.setRotation(rotation);
//    }
//    private void showFlashButton(android.hardware.Camera.Parameters params) {
//        boolean showFlash = (getPackageManager().hasSystemFeature(
//                PackageManager.FEATURE_CAMERA_FLASH) && params.getFlashMode() != null)
//                && params.getSupportedFlashModes() != null
//                && params.getSupportedFocusModes().size() > 1;
//
//        flashCameraButton.setVisibility(showFlash ? View.VISIBLE
//                : View.INVISIBLE);
//
//    }
//
//    private void releaseCamera() {
//        try {
//            if (camera != null) {
//                camera.setPreviewCallback(null);
//                camera.setErrorCallback(null);
//                camera.stopPreview();
//                camera.release();
//                camera = null;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            Log.e("error", e.toString());
//            camera = null;
//        }
//    }
//
//    @Override
//        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//
//        }
//
//        @Override
//        public void surfaceDestroyed(SurfaceHolder holder) {
//
//        }
//
//
//
//    }
//
//    private void alertCameraDialog() {
//        AlertDialog.Builder dialog = createAlert(MainActivity.this,"Camera Info", "error to open camera");
//        dialog.setNegativeButton("OK", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.cancel();
//            }
//        });
//        dialog.show();
//
//    }
//
//    @Override
//    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
//        return false;
//    }
//
//    @Override
//    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
//        return false;
//    }
//
//    @Override
//    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
//        return false;
//    }
//
//    @Override
//    public void onDestroyActionMode(ActionMode mode) {
//
//    }
//
//    @Override
//    public void onClick(View v) {
//        switch (v.getId()) {
//            case R.id.flash:
//                flash_camera();
//                break;
//            case R.id.switch_camera:
//                flip();
//                break;
//            case R.id.capture_image:
//                takeImage();
//                break;
//
//            default:
//                break;
//        }
//
//    }
//
//    private void takeImage() {
//        camera.takePicture(null, null, new android.hardware.Camera.PictureCallback() {
//
//
//            @Override
//            public void onPictureTaken(byte[] data, Camera camera) {
//                try {
//                    // convert byte array into bitmap
//                    Bitmap loadedImage = BitmapFactory.decodeByteArray(data, 0,
//                            data.length);
//
//                    // rotate Image
//                    Matrix rotateMatrix = new Matrix();
//                    rotateMatrix.postRotate(rotation);
//                    Bitmap rotatedBitmap = Bitmap.createBitmap(loadedImage, 0,
//                            0, loadedImage.getWidth(), loadedImage.getHeight(),
//                            rotateMatrix, false);
//                    String state = Environment.getExternalStorageState();
//                    File folder = null;
//                    if (state.contains(Environment.MEDIA_MOUNTED)) {
//                        folder = new File(Environment
//                                .getExternalStorageDirectory() + "/Demo");
//                    } else {
//                        folder = new File(Environment
//                                .getExternalStorageDirectory() + "/Demo");
//                    }
//
//                    boolean success = true;
//                    if (!folder.exists()) {
//                        success = folder.mkdirs();
//                    }
//                    if (success) {
//                        java.util.Date date = new java.util.Date();
//                        imageFile = new File(folder.getAbsolutePath()
//                                + File.separator
//                                + new Timestamp(date.getTime()).toString()
//                                + "Image.jpg");
//
//                        imageFile.createNewFile();
//                    } else {
//                        Toast.makeText(getBaseContext(), "Image Not saved",
//                                Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//
//                    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
//
//                    // save image into gallery
//                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);
//
//                    FileOutputStream fout = new FileOutputStream(imageFile);
//                    fout.write(ostream.toByteArray());
//                    fout.close();
//                    ContentValues values = new ContentValues();
//
//                    values.put(MediaStore.Images.Media.DATE_TAKEN,
//                            System.currentTimeMillis());
//                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
//                    values.put(MediaStore.MediaColumns.DATA,
//                            imageFile.getAbsolutePath());
//
//                    CameraDemoActivity.this.getContentResolver().insert(
//                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//
//            }
//        });
//
//    }
//
//    private void flip() {
//        int id = (cameraId == CameraInfo.CAMERA_FACING_BACK ? CameraInfo.CAMERA_FACING_FRONT
//                : CameraInfo.CAMERA_FACING_BACK);
//        if (!openCamera(id)) {
//            alertCameraDialog();
//        }
//
//    }
//
//    private void flash_camera() {
//        if (camera != null) {
//            try {
//                Parameters param = camera.getParameters();
//                param.setFlashMode(!flashmode ? Parameters.FLASH_MODE_TORCH
//                        : Parameters.FLASH_MODE_OFF);
//                camera.setParameters(param);
//                flashmode = !flashmode;
//            } catch (Exception e) {
//                // TODO: handle exception
//            }
//
//        }
//
//    }
//
//
//}
