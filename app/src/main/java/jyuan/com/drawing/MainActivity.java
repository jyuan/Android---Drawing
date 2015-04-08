package jyuan.com.drawing;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import jyuan.com.drawing.ColorPicker.ColorPickerDialog;
import jyuan.com.drawing.util.UploadPicture;


public class MainActivity extends Activity implements View.OnClickListener {

    private final Context context = this;
    private DrawingView drawView;

    final static private String APP_KEY = "5n0xvg2d0q8p7xz";
    final static private String APP_SECRET = "7jk1u8lnys3ucct";

    private static final String DROPBOX = "dropbox";
    private static final String OAUTH = "OAuth";
    private static final String PHOTOS = "/Photos/";

    DropboxAPI<AndroidAuthSession> dropboxAPI;

    // whether the user has log in with Dropbox
    private boolean isLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawView = (DrawingView) findViewById(R.id.drawing);

        dropboxInitial();
        buttonInitial();
    }

    private void buttonInitial() {
        // create new
        Button newButton = (Button) findViewById(R.id.new_button);
        // color
        Button colorButton = (Button) findViewById(R.id.color_button);
        // erase image
        Button eraseButton = (Button) findViewById(R.id.erase_button);
        // save image
        Button saveButton = (Button) findViewById(R.id.save_button);

        newButton.setOnClickListener(this);
        colorButton.setOnClickListener(this);
        eraseButton.setOnClickListener(this);
        saveButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.new_button:
                createNewCanvas();
                break;
            case R.id.color_button:
                changeColor();
                break;
            case R.id.erase_button:
                erase();
                break;
            case R.id.save_button:
                saveImage();
                break;
        }
    }

    private void createNewCanvas() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        // set title
        alertDialogBuilder.setTitle("New Draw");
        // set dialog message
        alertDialogBuilder
                .setMessage("Start a new draw")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        drawView.eraseImage(false);
                        drawView.createNewDrawing();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void changeColor() {
        drawView.eraseImage(false);
        int initialColor = drawView.getCurrentColor();
        Log.i(getClass().getSimpleName(), "initial color:" + String.valueOf(initialColor));

        ColorPickerDialog colorPickerDialog = new ColorPickerDialog(this, initialColor,
                new ColorPickerDialog.OnColorSelectedListener() {

                    @Override
                    public void onColorSelected(int color) {
                        Log.i(getClass().getSimpleName(), "changed color:" + String.valueOf(color));
                        drawView.setCurrentColor(color);
                    }

                });
        colorPickerDialog.show();
    }

    private void erase() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setTitle("Erase Draw");
        alertDialogBuilder
                .setMessage("Use your finger to erase some part you are not satisfied")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        drawView.eraseImage(true);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void saveImage() {
        if (!isLogin) {
            dropboxAPI.getSession().startOAuth2Authentication(MainActivity.this);
        } else {
            uploadImage();
        }
    }

    private void uploadImage() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        // set title
        alertDialogBuilder.setTitle("Save Draw");
        // set dialog message
        alertDialogBuilder
                .setMessage("Save the image into Dropbox")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        drawView.eraseImage(false);

                        drawView.setDrawingCacheEnabled(true);
                        Bitmap bitmap = drawView.getDrawingCache();

                        File file = generateImageFile();
                        OutputStream outputStream;
                        try {
                            if (bitmap != null) {
                                outputStream = new FileOutputStream(file);
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

                                outputStream.flush();
                                outputStream.close();
                                uploadImageIntoDropbox(file);
                            }
                        } catch (IOException e) {
                            Log.e(getClass().getSimpleName(), e.getMessage());
                        }
                        drawView.destroyDrawingCache();

                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    /**
     * generate the file name of image
     *
     * @return file name
     */
    public String generateImageFileName() {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        return "DRAWING_" + timeStamp + ".png";
    }

    /**
     * create a image file
     *
     * @return file
     */
    public File generateImageFile() {
        String imageFileName = generateImageFileName();
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(storageDir, imageFileName);
    }

    public void uploadImageIntoDropbox(File file) {
        UploadPicture upload = new UploadPicture(MainActivity.this, dropboxAPI, PHOTOS, file);
        upload.execute();
    }

    private void dropboxInitial() {
        // We create a new AuthSession so that we can use the Dropbox API.
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
        loadAuth(session);
        dropboxAPI = new DropboxAPI<>(session);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dropboxAPI.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                dropboxAPI.getSession().finishAuthentication();

                // Store it locally in our app for later use
                storeAuth(dropboxAPI.getSession());
                isLogin = true;
            } catch (IllegalStateException e) {
                Log.i(getClass().getSimpleName(), "Dropbox Error authenticating", e);
            }
        }
    }

    private void loadAuth(AndroidAuthSession session) {
        SharedPreferences prefs = getSharedPreferences(DROPBOX, 0);
        String oAuth = prefs.getString(OAUTH, null);
        if (oAuth == null || oAuth.length() == 0) {
            return;
        }
        session.setOAuth2AccessToken(oAuth);
        isLogin = true;
    }

    private void storeAuth(AndroidAuthSession session) {
        String oauth2AccessToken = session.getOAuth2AccessToken();
        if (oauth2AccessToken != null) {
            SharedPreferences prefs = getSharedPreferences(DROPBOX, 0);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(OAUTH, oauth2AccessToken);
            edit.apply();
        }
    }
}
