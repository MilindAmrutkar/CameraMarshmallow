package com.onestechsolution.cameramarshmallow.Activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.onestechsolution.cameramarshmallow.Asynctask.ImageCompression;
import com.onestechsolution.cameramarshmallow.BuildConfig;
import com.onestechsolution.cameramarshmallow.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int PERMISSION_CALLBACK_CONSTANT = 100;
    private static final int REQUEST_PERMISSION_SETTING = 101;

    private int ivImages[] = {R.id.iv_CustomerPhoto_NewLoanActivity, R.id.iv_Item1_NewLoanActivity, R.id.iv_Item2_NewLoanActivity, R.id.iv_Item3_NewLoanActivity, R.id.iv_Item4_NewLoanActivity, R.id.iv_Item5_NewLoanActivity};
    String[] permissionRequired = new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private SharedPreferences permissionStatus;
    ImageView currentImage;
    Bitmap bitmap;
    Uri fileUri, fileUri1;
    private boolean sentToSettings = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permissionStatus = getSharedPreferences("permissionStatus", MODE_PRIVATE);

        /*if((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED)) {
            sendValues.setEnabled(false);
            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 0);
        }*/
    }


    public void takePicture(View view) {
        if(askForPermissions()) {
            proceedAfterPermission();

            for (int i = 0; i < ivImages.length; i++) {
                if(view == (ImageView) findViewById(ivImages[i])){
                    currentImage = (ImageView) findViewById(ivImages[i]);
                    Log.i(TAG, "takePicture: currentImage: "+currentImage);
                }
            }
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            //fileUri = Uri.fromFile(getOutputMediaFile());
            fileUri = FileProvider.getUriForFile(MainActivity.this,
                    BuildConfig.APPLICATION_ID + ".provider",
                    getOutputMediaFile());
            fileUri1 = Uri.fromFile(getOutputMediaFile());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
            //intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri1);
            startActivityForResult(intent, REQUEST_PERMISSION_SETTING);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Log.i(TAG, "onActivityResult: data: "+data.toString());
        if(requestCode == REQUEST_PERMISSION_SETTING) {
            if(ActivityCompat.checkSelfPermission(MainActivity.this, permissionRequired[0])==PackageManager.PERMISSION_GRANTED) {
                proceedAfterPermission();
                if(resultCode == RESULT_OK) {
                    Log.i(TAG, "onActivityResult: currentImage: "+currentImage);
                    currentImage.setImageURI(fileUri);
                    //imageView1.setImageURI(fileUri1);
                    Log.i(TAG, "onActivityResult: fileUri: "+fileUri);
                    Log.i(TAG, "onActivityResult: fileUri1: "+fileUri1);
                    Toast.makeText(this, "Image saved successfully", Toast.LENGTH_SHORT).show();
                    //new ImageCompression(this).execute(fileUri.toString());
                    new ImageCompression(this).execute(fileUri1);
                } else if(resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, "Image was not saved", Toast.LENGTH_SHORT).show();
                }
            }

        }
    }

    private void proceedAfterPermission() {
        Toast.makeText(this, "All permissions are granted", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == PERMISSION_CALLBACK_CONSTANT) {

            //check if all permissions are granted
            boolean allgranted = false;

            for(int i=0; i<grantResults.length; i++) {
                if(grantResults[i]==PackageManager.PERMISSION_GRANTED) {
                    allgranted = true;
                } else {
                    allgranted = false;
                    break;
                }
            }

            if(allgranted) {
                proceedAfterPermission();
            } else if(askForPermissions()) {
                proceedAfterPermission();
            } else {
                Toast.makeText(this, "Unable to get Permission", Toast.LENGTH_SHORT).show();
            }

        }
    }



    private boolean askForPermissions() {
        if(ActivityCompat.checkSelfPermission(MainActivity.this, permissionRequired[0])!= PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(MainActivity.this, permissionRequired[1])!=PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permissionRequired[0])
                    || ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permissionRequired[1])) {
                //Show Information about why you need the permission
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Need Multiple Permissions");
                builder.setMessage("This app needs Camera & Storage permissions.");
                builder.setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MainActivity.this, permissionRequired, PERMISSION_CALLBACK_CONSTANT);
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
            } else if (permissionStatus.getBoolean(permissionRequired[0], false)) {
                //Previously Permission Request was cancelled with 'Dont Ask Again',
                // Redirect to Settings after showing Information about why you need the permission
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Need Multiple Permissions");
                builder.setMessage("This app needs Camera and Storage Permissions");
                builder.setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        sentToSettings = true;
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivityForResult(intent, REQUEST_PERMISSION_SETTING);
                        Toast.makeText(MainActivity.this, "Go to Permissions to grant camera & storage permission", Toast.LENGTH_SHORT).show();
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, permissionRequired, PERMISSION_CALLBACK_CONSTANT);
            }
            //Permission Required
            SharedPreferences.Editor editor = permissionStatus.edit();
            editor.putBoolean(permissionRequired[0], true);
            editor.apply();

        } else {
            proceedAfterPermission();
            return true;
        }
        return false;
    }



    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CameraDemo");

        if(!mediaStorageDir.exists()) {
            if(!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator + "IMG_"+timeStamp+".jpg");

    }

    public String getStringImage(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }


    public void sendValues(View view) {

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if(sentToSettings) {
            if(ActivityCompat.checkSelfPermission(MainActivity.this, permissionRequired[0])==PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(MainActivity.this, permissionRequired[1])!=PackageManager.PERMISSION_GRANTED) {
                proceedAfterPermission();
            }
        }
    }
}
