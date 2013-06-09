/**
 * Copyright 2010-present Facebook
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nonstop.android.SoC.Facebook;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import android.provider.MediaStore;

import android.view.LayoutInflater;

import android.view.View;
import android.view.ViewGroup;

import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.nonstop.android.SoC.R;
import com.facebook.android.Util;

import com.nonstop.android.SoC.Facebook.SessionEvents.AuthListener;
import com.nonstop.android.SoC.Facebook.SessionEvents.LogoutListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

@SuppressWarnings("deprecation")
public class Hackbook extends Activity implements OnItemClickListener {

    /*
     * Your Facebook Application ID must be set before running this example See
     * http://www.facebook.com/developers/createapp.php
     */
    public static final String APP_ID = "157111564357680";

    private LoginButton mLoginButton;
    private TextView mText;
    private ImageView mUserPic;
    private Handler mHandler_facebook;
    ProgressDialog dialog;

    final static int AUTHORIZE_ACTIVITY_RESULT_CODE = 9;
    final static int PICK_EXISTING_PHOTO_RESULT_CODE = 8;
  
    
    private ListView list;
    String[] main_items = { "Upload Photo" };
    String[] permissions = { "offline_access", "publish_stream", "user_photos", "publish_checkins",
            "photo_upload" };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (APP_ID == null) {
            Util.showAlert(this, "Warning", "Facebook Applicaton ID must be "
                    + "specified before running this example: see FbAPIs.java");
            return;
        }

        setContentView(R.layout.main);
        mHandler_facebook = new Handler();

        mText = (TextView) Hackbook.this.findViewById(R.id.txt);
        mUserPic = (ImageView) Hackbook.this.findViewById(R.id.user_pic);

        // Create the Facebook Object using the app id.
        Utility.mFacebook = new Facebook(APP_ID);
        // Instantiate the asynrunner object for asynchronous api calls.
        Utility.mAsyncRunner = new AsyncFacebookRunner(Utility.mFacebook);

        mLoginButton = (LoginButton) findViewById(R.id.login);

        // restore session if one exists
        SessionStore.restore(Utility.mFacebook, this);
        SessionEvents.addAuthListener(new FbAPIsAuthListener());
        SessionEvents.addLogoutListener(new FbAPIsLogoutListener());

        /*
         * Source Tag: login_tag
         */
        mLoginButton.init(this, AUTHORIZE_ACTIVITY_RESULT_CODE, Utility.mFacebook, permissions);

        if (Utility.mFacebook.isSessionValid()) {
            requestUserData();
        }

        list = (ListView) findViewById(R.id.main_list);

        list.setOnItemClickListener(this);
        list.setAdapter(new ArrayAdapter<String>(this, R.layout.main_list_item, main_items));
        
      
    }

    
    
    @Override
    public void onResume() {
        super.onResume();
        if(Utility.mFacebook != null) {
            if (!Utility.mFacebook.isSessionValid()) {
                mText.setText("You are logged out! ");
                mUserPic.setImageBitmap(null);
            } else {
                Utility.mFacebook.extendAccessTokenIfNeeded(this, null);
            }
        }
       
    }
   


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        /*
         * if this is the activity result from authorization flow, do a call
         * back to authorizeCallback Source Tag: login_tag
         */
            case AUTHORIZE_ACTIVITY_RESULT_CODE: {
                Utility.mFacebook.authorizeCallback(requestCode, resultCode, data);
                break;
            }
            /*
             * if this is the result for a photo picker from the gallery, upload
             * the image after scaling it. You can use the Utility.scaleImage()
             * function for scaling
             */
            case PICK_EXISTING_PHOTO_RESULT_CODE: {
                if (resultCode == Activity.RESULT_OK) {
                    Uri photoUri = data.getData();
                    if (photoUri != null) {
                        Bundle params = new Bundle();
                        try {
                            params.putByteArray("photo",
                                    Utility.scaleImage(getApplicationContext(), photoUri));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        params.putString("caption", "NonstopSoC");
                        Utility.mAsyncRunner.request("me/photos", params, "POST",
                                new PhotoUploadListener(), null);
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "Error selecting image from the gallery.", Toast.LENGTH_SHORT)
                                .show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "No image selected for upload.",
                            Toast.LENGTH_SHORT).show();
                }
                break;
            
           
            }
        }

    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
        switch (position) {
  /*
             * Source Tag: upload_photo You can upload a photo from the media
             * gallery or from a remote server How to upload photo:
             * https://developers.facebook.com/blog/post/498/
             */
            case 0: {
                if (!Utility.mFacebook.isSessionValid()) {
                    Util.showAlert(this, "Warning", "You must first log in.");
                } else {
                    dialog = ProgressDialog.show(Hackbook.this, "",
                            getString(R.string.please_wait), true, true);
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.gallery_remote_title)
                            .setMessage(R.string.gallery_remote_msg)
                            .setPositiveButton(R.string.gallery_button,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(Intent.ACTION_PICK,
                                                    (MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
                                            startActivityForResult(intent,
                                                    PICK_EXISTING_PHOTO_RESULT_CODE);
                                        }

                                    })
                            .setNegativeButton(R.string.remote_button,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            /*
                                             * Source tag: upload_photo_tag
                                             */
                                            Bundle params = new Bundle();
                                            params.putString("url",
                                                    "http://www.facebook.com/images/devsite/iphone_connect_btn.jpg");
                                            params.putString("caption",
                                                    "FbAPIs Sample App photo upload");
                                            Utility.mAsyncRunner.request("me/photos", params,
                                                    "POST", new PhotoUploadListener(), null);
                                        }

                                    }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface d) {
                                    dialog.dismiss();
                                }
                            }).show();
                }
                break;
            }

           
            case 7: {
                if(!Utility.mFacebook.isSessionValid()) {
                    Util.showAlert(this, "Warning", "You must first log in.");
                } else {
                    new TokenRefreshDialog(Hackbook.this).show();
                }
            }
        }
    }




    /*
     * callback for the photo upload
     */
    public class PhotoUploadListener extends BaseRequestListener {

        @Override
        public void onComplete(final String response, final Object state) {
            dialog.dismiss();
            mHandler_facebook.post(new Runnable() {
                @Override
                public void run() {
                    new UploadPhotoResultDialog(Hackbook.this, "Upload Photo executed", response)
                            .show();
                }
            });
        }

        public void onFacebookError(FacebookError error) {
            dialog.dismiss();
            Toast.makeText(getApplicationContext(), "Facebook Error: " + error.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }


    /*
     * Callback for fetching current user's name, picture, uid.
     */
    public class UserRequestListener extends BaseRequestListener {

        @Override
        public void onComplete(final String response, final Object state) {
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(response);

                final String picURL = jsonObject.getJSONObject("picture")
                        .getJSONObject("data").getString("url");
                final String name = jsonObject.getString("name");
                Utility.userUID = jsonObject.getString("id");

                mHandler_facebook.post(new Runnable() {
                    @Override
                    public void run() {
                        mText.setText("Welcome " + name + "!");
                        mUserPic.setImageBitmap(Utility.getBitmap(picURL));
                    }
                });

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    /*
     * The Callback for notifying the application when authorization succeeds or
     * fails.
     */

    public class FbAPIsAuthListener implements AuthListener {

        @Override
        public void onAuthSucceed() {
            requestUserData();
        }

        @Override
        public void onAuthFail(String error) {
            mText.setText("Login Failed: " + error);
        }
    }

    /*
     * The Callback for notifying the application when log out starts and
     * finishes.
     */
    public class FbAPIsLogoutListener implements LogoutListener {
        @Override
        public void onLogoutBegin() {
            mText.setText("Logging out...");
        }

        @Override
        public void onLogoutFinish() {
            mText.setText("You have logged out! ");
            mUserPic.setImageBitmap(null);
        }
    }

    /*
     * Request user name, and picture to show on the main screen.
     */
    public void requestUserData() {
        mText.setText("Fetching user name, profile pic...");
        Bundle params = new Bundle();
        params.putString("fields", "name, picture");
        Utility.mAsyncRunner.request("me", params, new UserRequestListener());
    }

    /**
     * Definition of the list adapter
     */
    public class MainListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public MainListAdapter() {
            mInflater = LayoutInflater.from(Hackbook.this.getBaseContext());
        }

        @Override
        public int getCount() {
            return main_items.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View hView = convertView;
            if (convertView == null) {
                hView = mInflater.inflate(R.layout.main_list_item, null);
                ViewHolder holder = new ViewHolder();
                holder.main_list_item = (TextView) hView.findViewById(R.id.main_api_item);
                hView.setTag(holder);
            }

            ViewHolder holder = (ViewHolder) hView.getTag();

            holder.main_list_item.setText(main_items[position]);

            return hView;
        }

    }

    class ViewHolder {
        TextView main_list_item;
    }

}
