package jyuan.com.drawing;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;

import java.io.File;
import java.util.UUID;

import jyuan.com.drawing.ColorPicker.ColorPickerDialog;
import jyuan.com.drawing.ColorPicker.RGBToHex;
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

        dropboxInitial();
        buttonInitial();
    }

    private void dropboxInitial() {
        // We create a new AuthSession so that we can use the Dropbox API.
        AndroidAuthSession session = buildSession();
        mApi = new DropboxAPI<AndroidAuthSession>(session);

        drawView = (DrawingView) findViewById(R.id.drawing);
    }

    private void buttonInitial() {
        ImageButton newButton, colorButton, eraseButton, saveButton;
        // create new
        newButton = (ImageButton) findViewById(R.id.new_btn);
        newButton.setOnClickListener(this);
        // color
        colorButton = (ImageButton) findViewById(R.id.draw_btn);
        colorButton.setOnClickListener(this);
        // erase image
        eraseButton = (ImageButton) findViewById(R.id.erase_btn);
        eraseButton.setOnClickListener(this);
        // save image
        saveButton = (ImageButton) findViewById(R.id.save_btn);
        saveButton.setOnClickListener(this);
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
                Log.i("color", "Dropbox Error authenticating", e);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.new_btn:
                createNewCanvas();
                break;
            case R.id.draw_btn:
                changeColor();
                break;
            case R.id.erase_btn:
                eraseBrush();
                break;
            case R.id.save_btn:
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
        Log.i("color", "initial color:" + String.valueOf(initialColor));

        ColorPickerDialog colorPickerDialog = new ColorPickerDialog(this, initialColor,
                new ColorPickerDialog.OnColorSelectedListener() {

            @Override
            public void onColorSelected(int color) {
                Log.i("color", String.valueOf(color));
                String hexColor = "#" + RGBToHex.getColorInHexFromRGB(Color.red(color),
                        Color.green(color), Color.blue(color));
                Log.i("color", "HEX Color" + hexColor);
                drawView.setColor(hexColor);
            }

        });
        colorPickerDialog.show();
    }

    private void eraseBrush() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        // set title
        alertDialogBuilder.setTitle("Erase Draw");
        // set dialog message
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
                        String imgSaved = MediaStore.Images.Media.insertImage(
                                MainActivity.this.getContentResolver(),
                                drawView.getDrawingCache(),
                                UUID.randomUUID().toString() + ".png", "drawing");
                        Log.i("color", imgSaved);
                        String filePath = convertMediaUriToPath(Uri.parse(imgSaved));
                        File file = new File(filePath);
                        if (imgSaved != null) {
                            UploadPicture upload = new UploadPicture(MainActivity.this,
                                    mApi, PHOTO_DIR, file);
                            upload.execute();
                        }
                        drawView.destroyDrawingCache();

                        dialog.dismiss();
                    }

                    protected String convertMediaUriToPath(Uri uri) {
                        String[] proj = {MediaStore.Images.Media.DATA};
                        Cursor cursor = getContentResolver().query(uri, proj, null, null, null);
                        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                        cursor.moveToFirst();
                        String path = cursor.getString(column_index);
                        cursor.close();
                        return path;
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
