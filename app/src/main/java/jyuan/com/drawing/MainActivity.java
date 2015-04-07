package jyuan.com.drawing;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import jyuan.com.drawing.ColorPicker.ColorPickerDialog;
import jyuan.com.drawing.util.ImageUtil;
import jyuan.com.drawing.util.UploadPicture;


public class MainActivity extends Activity implements View.OnClickListener {

    private final Context context = this;
    private DrawingView drawView;

    final static private String APP_KEY = "5n0xvg2d0q8p7xz";
    final static private String APP_SECRET = "7jk1u8lnys3ucct";

    private static final String ACCOUNT_PREFS_NAME = "prefs";
    private static final String ACCESS_KEY_NAME = "ACCESS_KEY";
    private static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";
    private static final String PHOTO_DIR = "/Photos/";

    DropboxAPI<AndroidAuthSession> mApi;

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
                        drawView.startNew();
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
        int initialColor = drawView.getInitialColor();
        Log.i(getClass().getSimpleName(), "initial color:" + String.valueOf(initialColor));

        ColorPickerDialog colorPickerDialog = new ColorPickerDialog(this, initialColor,
                new ColorPickerDialog.OnColorSelectedListener() {

                    @Override
                    public void onColorSelected(int color) {
                        Log.i(getClass().getSimpleName(), String.valueOf(color));
                        String hexColor = "#" + ImageUtil.getColorInHexFromRGB(Color.red(color),
                                Color.green(color), Color.blue(color));
                        Log.i(getClass().getSimpleName(), "HEX Color" + hexColor);
                        drawView.setColor(hexColor);
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
            mApi.getSession().startOAuth2Authentication(MainActivity.this);
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
                                BufferedOutputStream bos = new BufferedOutputStream(outputStream);
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);

                                bos.flush();
                                bos.close();
                                UploadPicture upload = new UploadPicture(MainActivity.this,
                                    mApi, PHOTO_DIR, file);
                                upload.execute();
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

    public String generateImageFileName() {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        return "DRAWING_" + timeStamp + ".png";
    }

    public File generateImageFile() {
        String imageFileName = generateImageFileName();
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(storageDir, imageFileName);
    }

    private void dropboxInitial() {
        // We create a new AuthSession so that we can use the Dropbox API.
        AndroidAuthSession session = buildSession();
        mApi = new DropboxAPI<>(session);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AndroidAuthSession session = mApi.getSession();

        if (session.authenticationSuccessful()) {
            try {
                // Mandatory call to complete the auth
                session.finishAuthentication();

                // Store it locally in our app for later use
                storeAuth(session);
                isLogin = true;
            } catch (IllegalStateException e) {
                Log.i(getClass().getSimpleName(), "Dropbox Error authenticating", e);
            }
        }
    }

    private AndroidAuthSession buildSession() {
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);

        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
        loadAuth(session);
        return session;
    }

    private void loadAuth(AndroidAuthSession session) {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        String key = prefs.getString(ACCESS_KEY_NAME, null);
        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
        if (key == null || secret == null || key.length() == 0 || secret.length() == 0) return;

        if (key.equals("oauth2:")) {
            // If the key is set to "oauth2:", then we can assume the token is for OAuth 2.
            session.setOAuth2AccessToken(secret);
        } else {
            // Still support using old OAuth 1 tokens.
            session.setAccessTokenPair(new AccessTokenPair(key, secret));
        }
    }

    private void storeAuth(AndroidAuthSession session) {
        // Store the OAuth 2 access token, if there is one.
        String oauth2AccessToken = session.getOAuth2AccessToken();
        if (oauth2AccessToken != null) {
            SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(ACCESS_KEY_NAME, "oauth2:");
            edit.putString(ACCESS_SECRET_NAME, oauth2AccessToken);
            edit.apply();
            return;
        }
        // Store the OAuth 1 access token, if there is one.  This is only necessary if
        // you're still using OAuth 1.
        AccessTokenPair oauth1AccessToken = session.getAccessTokenPair();
        if (oauth1AccessToken != null) {
            SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(ACCESS_KEY_NAME, oauth1AccessToken.key);
            edit.putString(ACCESS_SECRET_NAME, oauth1AccessToken.secret);
            edit.apply();
            return;
        }
    }


}
