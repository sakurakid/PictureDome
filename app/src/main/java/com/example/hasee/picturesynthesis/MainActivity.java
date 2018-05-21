package com.example.hasee.picturesynthesis;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import static org.opencv.core.CvType.CV_32FC3;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.cvtColor;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private View inflate;
    public static final int TAKE_PHOTO = 1;//拍照
    public static final int CHOOSE_PHOTO = 2;//选择相册
    public static final int PICTURE_CUT = 3;//剪切图片
    private Uri imageUri;//相机拍照图片保存地址
    private Uri outputUri;//裁剪万照片保存地址
    private String imagePath;//打开相册选择照片的路径
    private boolean isClickCamera;//是否是拍照裁剪
    private Button takeBtn;//打开相机按钮
    private Button albumBtn;//选择相册按钮
    private ImageView iv1;
    private ImageView iv2;
    private ImageView iv3;//裁剪完毕
    private Dialog dialog;
    private Button cancel;
    private Bitmap bitmap1;
    private Bitmap bitmap2;
    private Bitmap bitmap;//后退的


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv1 = (ImageView)findViewById(R.id.iv_1);
        iv2 = (ImageView)findViewById(R.id.iv_2);
        iv3 = (ImageView)findViewById(R.id.iv_3);
    }

    public void show(View view){
        dialog = new Dialog(this, R.style.ActionSheetDialogStyle);
        inflate = LayoutInflater.from(this).inflate(R.layout.dialog_layout, null);

        albumBtn = (Button) inflate.findViewById(R.id.choosePhoto);
        takeBtn = (Button) inflate.findViewById(R.id.takePhoto);
        cancel = (Button) inflate.findViewById(R.id.btn_cancel);

        albumBtn.setOnClickListener(this);
        takeBtn.setOnClickListener(this);
        cancel.setOnClickListener(this);

        dialog.setContentView(inflate);
        Window dialogWindow = dialog.getWindow();
        dialogWindow.setGravity( Gravity.BOTTOM);
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.y = 20;
        dialogWindow.setAttributes(lp);
        dialog.show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.takePhoto:
                openCamera();//打开相机
                break;
            case R.id.choosePhoto:
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                } else {
                    selectFromAlbum();//打开相册
                }
            case R.id.but_hc:
                //合成
                Bitmap bitmap3 = colortransfer(bitmap1,bitmap2);
                iv3.setImageBitmap(bitmap3);
                break;

        }
    }

    /**
     * 两个图片的合成
     */
    private Bitmap colortransfer(Bitmap bitmap1,Bitmap bitmap2){
        Bitmap resultmap = null;
        Mat src = new Mat();
        Utils.bitmapToMat(bitmap1,src);
        Mat dst = new Mat();
        Utils.bitmapToMat(bitmap2,dst);
        Mat labsrc = new Mat();
        Mat labdst = new Mat();
        Mat result = new Mat();

        Mat result_lab = new Mat(src.rows(),src.cols(),CV_32FC3);
        // Mat binaryMat = new Mat(grayMat.height(),grayMat.width(),CvType.CV_8UC1);

        //转换成lab空间  (1)
        Mat srcImg_32F =new Mat(), targetImg_32F = new Mat();
        src.convertTo(srcImg_32F, CV_32FC3, 1.0f / 255.f);
        dst.convertTo(targetImg_32F, CV_32FC3, 1.0f / 255.0f);
        cvtColor(srcImg_32F, labsrc, Imgproc.COLOR_BGR2Lab);
        cvtColor(targetImg_32F, labdst, Imgproc.COLOR_BGR2Lab);

        //计算平均值
        Vector<Double> srcmeans = new Vector<Double>(3);				//平均值
        Vector<Double> srcstandards = new Vector<Double>(3);			//标准差
        Vector<Double> targetmeans = new Vector<Double>(3);
        Vector<Double> targetstandards = new Vector<Double>(3);
        //ctor<Float> tmp = new Vector<Float>(){0.f,0.f,0.f};
        int srcPixels = src.rows()*src.cols();
        int targetPixels = dst.rows()*dst.cols();

        double sum[] = new double[3];
        //计算源图像的平均值
        for (int row =0;row < src.rows();row++)
        {
            for (int col = 0;col < src.cols();col++)
            {
                double[] data = labsrc.get(row,col);
                for(int tmp=0;tmp<3;tmp++)
                {
                    sum[tmp] += data[tmp];
                }
            }
        }
        for (int i=0;i<3;i++)
        {
            srcmeans.set(i, sum[i] /srcPixels);
        }
        //计算目标图像的平均值
        for (int i = 0; i < 3; i++)
        {
            sum[i] = 0;
        }

        for (int row =0;row < dst.rows();row++)
        {
            for (int col = 0;col < dst.cols();col++)
            {
                double[] data = labdst.get(row,col);
                for(int tmp=0;tmp<3;tmp++)
                {
                    sum[tmp] += data[tmp];
                }
            }
        }
        for (int i=0;i<3;i++)
        {
            targetmeans.set(i, sum[i] /targetPixels);
        }

        //计算源图像的标准差
        double sum_variance[] = new double[3];

        for (int y = 0; y < labsrc.rows(); y++)
        {
            for (int x = 0; x < labsrc.cols(); x++)
            {
//                Vec3f color = labsrc.at<Vec3f>(y, x);
                double[] color = labsrc.get(y,x);
                sum_variance[0] += (color[0] - srcmeans.get(0))*(color[0] - srcmeans.get(0));
                sum_variance[1] += (color[1] - srcmeans.get(1))*(color[1] - srcmeans.get(1));
                sum_variance[2] += (color[2] - srcmeans.get(2))*(color[2] - srcmeans.get(2));
            }
        }
        srcstandards.set(0,Math.sqrt(sum_variance[0] / srcPixels));
        srcstandards.set(1,Math.sqrt(sum_variance[1] / srcPixels));
        srcstandards.set(2,Math.sqrt(sum_variance[2] / srcPixels));

        //计算目标图像的标准差
        for (int i = 0; i < 3; i++)
        {
            sum_variance[i] = 0;
        }

        for (int y = 0; y < labdst.rows(); y++)
        {
            for (int x = 0; x < labdst.cols(); x++)
            {
//                Vec3f color = labsrc.at<Vec3f>(y, x);
                double[] color = labdst.get(y,x);
                sum_variance[0] += (color[0] - targetmeans.get(0))*(color[0] - targetmeans.get(0));
                sum_variance[1] += (color[1] - targetmeans.get(1))*(color[1] - targetmeans.get(1));
                sum_variance[2] += (color[2] - targetmeans.get(2))*(color[2] - targetmeans.get(2));
            }
        }
        targetstandards.set(0,Math.sqrt(sum_variance[0] / targetPixels));
        targetstandards.set(1,Math.sqrt(sum_variance[1] / targetPixels));
        targetstandards.set(2,Math.sqrt(sum_variance[2] / targetPixels));

        int width = src.cols();
        int height = src.rows();

        double deta_rate[] = new double[3];			//标准差比值
        for (int k = 0; k < 3; k++)
        {
            deta_rate[k] = targetstandards.get(k) / srcstandards.get(k);
        }

        for(int y =0;y<height;y++)
        {
            for(int x=0;x<width;x++)
            {
                double[] color = labdst.get(y,x);
                double value[] = new double[3];
                for(int  channel = 0; channel < 3; channel++ )
                {
                    value[channel] = deta_rate[channel]*(color[channel] - srcmeans.get(channel)) + targetmeans.get(channel);
                    result_lab.put(y,x,value);
                }
            }
        }
        Imgproc.cvtColor(result_lab,result,COLOR_BGR2GRAY,1);
        Utils.matToBitmap(result,resultmap);
        return resultmap;
    }

    /***
     * opencv库 加载并初始化回调的函数
     */
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch (status){
                case BaseLoaderCallback.SUCCESS:
                    Log.d("233","加载成功");
                    break;
                default:
                    super.onManagerConnected(status);
                    Log.d("233","加载失败");
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()){
           Log.d("233","可以");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_10,this,mLoaderCallback);
        }else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

    }

    //图片结果处理方法
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case TAKE_PHOTO://拍照
                if (resultCode == RESULT_OK) {
                    cropPhoto(imageUri);//裁剪图片
                }
                break;
            case CHOOSE_PHOTO://打开相册
                // 判断手机系统版本号
                if (Build.VERSION.SDK_INT >= 19) {
                    // 4.4及以上系统使用这个方法处理图片
                    handleImageOnKitKat(data);
                } else {
                    // 4.4以下系统使用这个方法处理图片
                    handleImageBeforeKitKat(data);
                }
                break;
            case PICTURE_CUT://裁剪完成
                isClickCamera = true;
                Bitmap bitmap = null;
                try {
                    if (isClickCamera) {
                        bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(outputUri));
                    } else {
                        bitmap = BitmapFactory.decodeFile(imagePath);
                    }
                    if(iv1.getDrawable()==null){
                        iv1.setImageBitmap(bitmap);
                        bitmap1 = bitmap;
                    }else {
                        iv2.setImageBitmap(bitmap);
                        bitmap2 = bitmap;
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private void openCamera() {
        // 创建File对象，用于存储拍照后的图片
//        File outputImage = new File(getExternalCacheDir(), "output_image.jpg");
//        try {
//            if (outputImage.exists()) {
//                outputImage.delete();
//            }
//            outputImage.createNewFile();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        if (Build.VERSION.SDK_INT < 24) {
//            imageUri = Uri.fromFile(outputImage);
//        } else {
//            //Android 7.0系统开始 使用本地真实的Uri路径不安全,使用FileProvider封装共享Uri
//            //参数二:fileprovider绝对路径 com.dyb.testcamerademo：项目包名
//            imageUri = FileProvider.getUriForFile(MainActivity.this, "com.example.picturesynthesis.fileprovider", outputImage);
//        }
//        // 启动相机程序
//        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
//        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
//        startActivityForResult(intent, TAKE_PHOTO);
        File outputImage = new File(getExternalCacheDir(),"output_image.jpg");
        try{
            if(outputImage.exists()){
                outputImage.delete();
            }
            outputImage.createNewFile();
        }catch (IOException e){
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT >= 24){
            imageUri = FileProvider.getUriForFile(MainActivity.this,
                    "com.example.picturesynthesis.fileprovider",outputImage);
        }else {
            imageUri = Uri.fromFile(outputImage);
        }
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        startActivityForResult(intent,TAKE_PHOTO);

    }


    private void selectFromAlbum() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            openAlbum();
        }
    }

    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_PHOTO); // 打开相册
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openAlbum();
                } else {
                    Toast.makeText(this, "You denied the permission", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }


    /**
     * 裁剪图片
     */
    private void cropPhoto(Uri uri) {
        // 创建File对象，用于存储裁剪后的图片，避免更改原图
        File file = new File(getExternalCacheDir(), "crop_image.jpg");
        try {
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        outputUri = Uri.fromFile(file);
        Intent intent = new Intent("com.android.camera.action.CROP");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.setDataAndType(uri, "image/*");
        //裁剪图片的宽高比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("crop", "false");//可裁剪
        // 裁剪后输出图片的尺寸大小
        //intent.putExtra("outputX", 400);
        //intent.putExtra("outputY", 200);
        intent.putExtra("scale", true);//支持缩放
        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());//输出图片格式
        intent.putExtra("noFaceDetection", true);//取消人脸识别
        startActivityForResult(intent, PICTURE_CUT);
    }

    // 4.4及以上系统使用这个方法处理图片 相册图片返回的不再是真实的Uri,而是分装过的Uri
    @TargetApi(19)
    private void handleImageOnKitKat(Intent data) {
        imagePath = null;
        Uri uri = data.getData();
        Log.d("TAG", "handleImageOnKitKat: uri is " + uri);
        if (DocumentsContract.isDocumentUri(this, uri)) {
            // 如果是document类型的Uri，则通过document id处理
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1]; // 解析出数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // 如果是content类型的Uri，则使用普通方式处理
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            // 如果是file类型的Uri，直接获取图片路径即可
            imagePath = uri.getPath();
        }
        cropPhoto(uri);
    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        // 通过Uri和selection来获取真实的图片路径
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    private void handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        imagePath = getImagePath(uri, null);
        cropPhoto(uri);
    }



}
