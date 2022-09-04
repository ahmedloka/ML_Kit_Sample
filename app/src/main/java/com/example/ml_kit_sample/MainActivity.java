package com.example.ml_kit_sample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.hmf.tasks.Task;
import com.huawei.hms.mlsdk.MLAnalyzerFactory;
import com.huawei.hms.mlsdk.common.MLApplication;
import com.huawei.hms.mlsdk.common.MLFrame;
import com.huawei.hms.mlsdk.text.MLText;
import com.huawei.hms.mlsdk.text.MLTextAnalyzer;

import java.io.FileDescriptor;
import java.io.IOException;

/*
   TODO  LOGIC of THE DEMO
   TODO  1: User will click on FrameLatout to choose img from gallery or from Camera
   TODO 2: After choosing the image we will we will display it in the FrameLayout
   TODO 3: Will extract the text from img and display it in the below of the screen
 */

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_CODE = 1;
    private static final int WRITE_STORAGE_PERMISSION_CODE = 2;

    private TextView mTxtExtractedTxt;
    private ImageView imageView;

    private Uri image_uri;

    private static final int IMAGE_GALLERY_CODE = 123;
    public static final int IMAGE_CAPTURE_CODE = 654;

    public static final String API_KEY =  "DAEDACErX9mVrSrrgyEhOOmJVk5Z86qhod2tHDFBxlaWykzlK64jDO72YVTWdtnQdESebGPTea7c76P2jhKscj3LE9ko9bnAFWHSKA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkTheNeededPermissions();

        init();
    }

    // TODO Check whether the app has the permissions.
    private void checkTheNeededPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                + ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            // The app has the camera permission.
            Toast.makeText(this, "The app has the needed permissions.", Toast.LENGTH_SHORT).show();
        } else {
            // Apply for the camera permission.
            requestNeededPermissions();
            Toast.makeText(this, "Apply for the permissions.", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestNeededPermissions() {
        final String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
                && !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, permissions, CAMERA_PERMISSION_CODE);
            ActivityCompat.requestPermissions(this, permissions, WRITE_STORAGE_PERMISSION_CODE);
        }
    }
    // We will need to ask the user to choose an image
    // this image he/she choose it from gallery or from the mobile camera
    private void init() {
        MLApplication.initialize(getApplicationContext());// Called if your app runs multiple processes.
        MLApplication.getInstance().setApiKey(API_KEY);

        mTxtExtractedTxt = findViewById(R.id.txt_ocr);
        imageView = findViewById(R.id.imageView);

        FrameLayout mFrameLayout = findViewById(R.id.frame_layout);

        AlertDialog.Builder builder = getBuilder();

        mFrameLayout.setOnClickListener(view -> {
            AlertDialog alert = builder.create();
            alert.show();
        });
    }

    //TODO show alert dialog to let user choose Camera or Gallery to take a photo
    @NonNull
    private AlertDialog.Builder getBuilder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.alter_title))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.camera), (dialog, id) -> {
                    chooseImgFromCamera();
                })
                .setNegativeButton(getString(R.string.gallery), (dialog, id) -> {
                    chooseImgFromGallery();
                });
        return builder;
    }

    private void chooseImgFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // It is deprecated and the alternative is startActivityForResult
        startActivityForResult(galleryIntent, IMAGE_GALLERY_CODE);
    }

    private void chooseImgFromCamera() {
        //Check First if the permission already granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED
                    || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED) {
                String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permission, CAMERA_PERMISSION_CODE);
            } else {
                openCamera();
            }
        } else {
            openCamera();
        }
    }

    //TODO opens camera so that user can capture image
    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != CAMERA_PERMISSION_CODE && requestCode != WRITE_STORAGE_PERMISSION_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // The camera permission is granted.
            Toast.makeText(this, "The permissions are granted.", Toast.LENGTH_SHORT).show();
        }
    }

    //TODO taking Image URI
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // We have the the Image URI from the camera now
        if (requestCode == IMAGE_CAPTURE_CODE && resultCode == RESULT_OK) {

            Bitmap bitmap = uriToBitmap(image_uri);
            imageView.setImageBitmap(bitmap);

            detectBitmap(bitmap);
        }
        // We have the the Image URI from the gallery now
        if (requestCode == IMAGE_GALLERY_CODE && resultCode == RESULT_OK && data != null) {
            image_uri = data.getData();
            Bitmap bitmap = uriToBitmap(image_uri);
            imageView.setImageBitmap(bitmap);

            detectBitmap(bitmap);
        }
    }

    //TODO takes URI of the image and returns bitmap
    private Bitmap uriToBitmap(Uri selectedFileUri) {
        try {
            ParcelFileDescriptor parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(selectedFileUri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);

            parcelFileDescriptor.close();
            return image;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // TODO recognizing the text from the images
    private void detectBitmap(Bitmap bitmap) {
        // Method 1: Use default parameter settings to configure the on-device text analyzer. Only Latin-based languages can be recognized.
        MLTextAnalyzer analyzer = MLAnalyzerFactory.getInstance().getLocalTextAnalyzer();
        // Create an MLFrame object using the bitmap, which is the image data in bitmap format.
        MLFrame frame = MLFrame.fromBitmap(bitmap);

        Task<MLText> task = analyzer.asyncAnalyseFrame(frame);
        task.addOnSuccessListener(text -> {
            // Processing for successful recognition.
            mTxtExtractedTxt.setText(text.getStringValue());
        }).addOnFailureListener(e -> {
            // Processing logic for recognition failure.
            Toast.makeText(MainActivity.this, e.getMessage() + " " + e.getCause(), Toast.LENGTH_SHORT).show();
        });
        try {
            analyzer.stop();
        } catch (IOException e) {
            // Exception handling.
            Toast.makeText(this, "Exception " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}