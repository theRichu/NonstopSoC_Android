/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nonstop.android.SoC.BluetoothChat;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.nonstop.android.SoC.R;
import com.nonstop.android.SoC.Util;
import com.nonstop.android.SoC.Data.GlasOData.Alarm;
import com.nonstop.android.SoC.Data.GlasOData.Direction;
import com.nonstop.android.SoC.Data.GlasOData.Exceed;
import com.nonstop.android.SoC.Data.GlasOData.OnTime;
import com.nonstop.android.SoC.Data.GlasOData.Status;
import com.nonstop.android.SoC.Facebook.BaseRequestListener;

import com.nonstop.android.SoC.Facebook.Hackbook;
import com.nonstop.android.SoC.Facebook.LoginButton;
import com.nonstop.android.SoC.Facebook.SessionEvents;
import com.nonstop.android.SoC.Facebook.SessionStore;
import com.nonstop.android.SoC.Facebook.UploadPhotoResultDialog;
import com.nonstop.android.SoC.Facebook.Utility;

import com.nonstop.android.SoC.Facebook.Hackbook.PhotoUploadListener;
import com.nonstop.android.SoC.Facebook.SessionEvents.AuthListener;
import com.nonstop.android.SoC.Facebook.SessionEvents.LogoutListener;

/**
 * This is the main Activity that displays the current chat session.
 */
@SuppressWarnings("deprecation")
public class BluetoothChat extends Activity {
	// Debugging
	private static final String TAG = "BluetoothChat";
	private static final boolean D = true;

	public static final String APP_ID = "157111564357680";
	private LoginButton mLoginButton;
	private TextView mText;
	private ImageView mUserPic;
	private Handler mHandler_facebook;
	ProgressDialog dialog;
	
	private char[] photo_data_= new char[100*100];
	private Bitmap photo_converted;

	final static int AUTHORIZE_ACTIVITY_RESULT_CODE = 9;
	final static int PICK_EXISTING_PHOTO_RESULT_CODE = 8;

	String[] permissions = { "offline_access", "publish_stream", "user_photos",
			"publish_checkins", "photo_upload" };

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	private static final int REQUEST_ENABLE_BT = 3;

	// Layout Views
	private TextView mTitle;
	private ListView mConversationView;
	private EditText mOutEditText;
	private Button mSendButton;

	private Button mTurnLeftButton;
	private Button mTurnRightButton;
	private Button mNormalButton;
	private Button mPhotoButton;

	private Button mNoAlarmButton;
	private Button mAroundAlarmButton;
	private Button mOnAlarmButton;
	private Button mOnTimeButton;

	private RadioGroup mRadioGroup;
	private CheckBox mStatusCheckBox;

	private ToggleButton mNaviToggle;

	private ToggleButton mHeartbeatToggle;
	private SeekBar mHeartbeat;

	private ToggleButton mSpeedToggle;
	private SeekBar mSpeed;

	private ToggleButton mDistanceToggle;
	private SeekBar mDiatance;

	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Array adapter for the conversation thread
	private ArrayAdapter<String> mConversationArrayAdapter;
	// String buffer for outgoing messages
	private StringBuffer mOutStringBuffer;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private BluetoothChatService mChatService = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (D)
			Log.e(TAG, "+++ ON CREATE +++");

		// Set up the window layout
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.bluetooth_chat);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.custom_title);

		// Set up the custom title
		mTitle = (TextView) findViewById(R.id.title_left_text);
		mTitle.setText(R.string.app_name);
		mTitle = (TextView) findViewById(R.id.title_right_text);

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		if (APP_ID == null) {
			com.facebook.android.Util
					.showAlert(
							this,
							"Warning",
							"Facebook Applicaton ID must be "
									+ "specified before running this example: see FbAPIs.java");
			return;
		}

		// setContentView(R.layout.main);
		mHandler_facebook = new Handler();

		mText = (TextView) this.findViewById(R.id.txt);
		mUserPic = (ImageView) this.findViewById(R.id.user_pic);

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
		mLoginButton.init(this, AUTHORIZE_ACTIVITY_RESULT_CODE,
				Utility.mFacebook, permissions);

		if (Utility.mFacebook.isSessionValid()) {
			requestUserData();
		}

	}

	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		} else {
			if (mChatService == null)
				setupChat();
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (D)
			Log.e(TAG, "+ ON RESUME +");

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		if (mChatService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
				// Start the Bluetooth chat services
				mChatService.start();
			}
		}
		if (Utility.mFacebook != null) {
			if (!Utility.mFacebook.isSessionValid()) {
				mText.setText("You are logged out! ");
				mUserPic.setImageBitmap(null);
			} else {
				Utility.mFacebook.extendAccessTokenIfNeeded(this, null);
			}
		}
	}

	private void setupChat() {
		Log.d(TAG, "setupChat()");

		// Initialize the array adapter for the conversation thread
		mConversationArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.message);
		mConversationView = (ListView) findViewById(R.id.in);
		mConversationView.setAdapter(mConversationArrayAdapter);

		// Initialize the compose field with a listener for the return key
		mOutEditText = (EditText) findViewById(R.id.edit_text_out);
		mOutEditText.setOnEditorActionListener(mWriteListener);

		// Initialize the send button with a listener that for click events
		mSendButton = (Button) findViewById(R.id.button_send);
		mSendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Send a message using content of the edit text widget
				TextView view = (TextView) findViewById(R.id.edit_text_out);
				String message = view.getText().toString();
				sendMessage(message);
			}
		});
		mTurnLeftButton = (Button) findViewById(R.id.button_turnleft);
		mTurnLeftButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				mChatService.mGlasOData.setDirection(Direction.LEFT);
				sendMessage(mChatService.mGlasOData.makePacket());
			}
		});
		mTurnRightButton = (Button) findViewById(R.id.button_turnright);
		mTurnRightButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mChatService.mGlasOData.setDirection(Direction.RIGHT);
				sendMessage(mChatService.mGlasOData.makePacket());
			}
		});
		mNormalButton = (Button) findViewById(R.id.button_normal);
		mNormalButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mChatService.mGlasOData.setDirection(Direction.NORMAL);
				sendMessage(mChatService.mGlasOData.makePacket());
			}
		});

		mNoAlarmButton = (Button) findViewById(R.id.button_no_alarm);
		mNoAlarmButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mChatService.mGlasOData.setAlarm_(Alarm.NOUSE);
				sendMessage(mChatService.mGlasOData.makePacket());
			}
		});
		mAroundAlarmButton = (Button) findViewById(R.id.button_around_alarm);
		mAroundAlarmButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mChatService.mGlasOData.setAlarm_(Alarm.AROUND);
				sendMessage(mChatService.mGlasOData.makePacket());
			}
		});
		mOnAlarmButton = (Button) findViewById(R.id.button_on_alarm);
		mOnAlarmButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mChatService.mGlasOData.setAlarm_(Alarm.ONTIME);
				sendMessage(mChatService.mGlasOData.makePacket());
			}
		});
		mOnTimeButton = (Button) findViewById(R.id.button_ontime);
		mOnTimeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mChatService.mGlasOData.setOnTime(OnTime.ON);
				sendMessage(mChatService.mGlasOData.makePacket());
				mChatService.mGlasOData.setOnTime(OnTime.OFF);
			}
		});
		mPhotoButton = (Button) findViewById(R.id.button_photo);
		mPhotoButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				if (!Utility.mFacebook.isSessionValid()) {
					// Util.showAlert(this, "Warning",
					// "You must first log in.");
				} else {
					/*
					 * Source tag: upload_photo_tag
					 */
					dialog = ProgressDialog.show(BluetoothChat.this, "",
							getString(R.string.please_wait), true, true);
					Bundle params = new Bundle();
					params.putString("url",
							"http://www.facebook.com/images/devsite/iphone_connect_btn.jpg");
					params.putString("caption",
							"FbAPIs Sample App photo upload");
					Utility.mAsyncRunner.request("me/photos", params, "POST",
							new PhotoUploadListener(), null);
				}
			}
		});

		mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup1);
		mRadioGroup
				.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
					public void onCheckedChanged(RadioGroup arg0, int arg1) {
						if (arg1 == R.id.radio_exceed) {

							mChatService.mGlasOData.setExceed(Exceed.EXCEED);
						} else if (arg1 == R.id.radio_exceedhigh) {

							mChatService.mGlasOData
									.setExceed(Exceed.EXCEEDHIGH);
						} else if (arg1 == R.id.radio_normalspeed) {
							mChatService.mGlasOData
									.setExceed(Exceed.NORMALSPEED);
						}
						sendMessage(mChatService.mGlasOData.makePacket());
					}
				});

		mStatusCheckBox = (CheckBox) findViewById(R.id.checkBox_status);
		mStatusCheckBox
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if(isChecked){
						mChatService.mGlasOData.setStatus(Status.WRONG);
						}else{
						mChatService.mGlasOData.setStatus(Status.NORMAL);
						}
					}
				});

		mNaviToggle = (ToggleButton) findViewById(R.id.toggle_navi);
		mNaviToggle
				.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							if (D)
								Log.e(TAG, "++ NAVI ON ++");
							mChatService.navi_stat_ = 1;

						} else {
							if (D)
								Log.e(TAG, "++ NAVI OFF ++");
							mChatService.navi_stat_ = 0;

						}
					}
				});
		mHeartbeatToggle = (ToggleButton) findViewById(R.id.toggle_Heartbeat);
		mHeartbeatToggle
				.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							if (D)
								Log.e(TAG, "++ HEARTBEAT ON ++");
							mChatService.heartbeat_stat_ = 1;
						} else {
							if (D)
								Log.e(TAG, "++ HEARTBEAT OFF ++");
							mChatService.heartbeat_stat_ = 0;
						}
						if (mChatService.heartbeat_stat_ == 0
								&& mChatService.distance_stat_ == 0
								&& mChatService.speed_stat_ == 0) {
							mChatService.sens_stat_ = 0;
						} else {
							mChatService.sens_stat_ = 1;
						}
					}
				});

		mHeartbeat = (SeekBar) findViewById(R.id.seekBar_Heartbeat);
		mHeartbeat
				.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						mChatService.mGlasOData.setHeartbeat_(progress);
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {

					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {

					}
				});

		mSpeedToggle = (ToggleButton) findViewById(R.id.toggle_Velocity);
		mSpeedToggle
				.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							if (D)
								Log.e(TAG, "++ SpeedSens ON ++");
							mChatService.speed_stat_ = 1;
						} else {
							if (D)
								Log.e(TAG, "++ SpeedSens OFF ++");
							mChatService.speed_stat_ = 0;
						}
						if (mChatService.heartbeat_stat_ == 0
								&& mChatService.distance_stat_ == 0
								&& mChatService.speed_stat_ == 0) {
							mChatService.sens_stat_ = 0;
						} else {
							mChatService.sens_stat_ = 1;
						}
					}
				});

		mSpeed = (SeekBar) findViewById(R.id.seekBar_Velocity);
		mSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				mChatService.mGlasOData.setVelocity_(progress);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

		mDistanceToggle = (ToggleButton) findViewById(R.id.toggle_Distance);
		mDistanceToggle
				.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							if (D)
								Log.e(TAG, "++ Distance ON ++");
							mChatService.distance_stat_ = 1;
						} else {
							if (D)
								Log.e(TAG, "++ Distance OFF ++");
							mChatService.distance_stat_ = 0;
						}
						if (mChatService.heartbeat_stat_ == 0
								&& mChatService.distance_stat_ == 0
								&& mChatService.speed_stat_ == 0) {
							mChatService.sens_stat_ = 0;
						} else {
							mChatService.sens_stat_ = 1;
						}
					}
				});

		mDiatance = (SeekBar) findViewById(R.id.seekBar_Distance);
		mDiatance
				.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						mChatService.mGlasOData.setDistance_(progress);
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {

					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {

					}
				});
		// Initialize the BluetoothChatService to perform bluetooth connections
		mChatService = new BluetoothChatService(this, mHandler);

		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer("");
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
		if (D)
			Log.e(TAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		if (D)
			Log.e(TAG, "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop the Bluetooth chat services
		if (mChatService != null)
			mChatService.stop();
		if (D)
			Log.e(TAG, "--- ON DESTROY ---");
	}

	private void ensureDiscoverable() {
		if (D)
			Log.d(TAG, "ensure discoverable");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	/**
	 * Sends a message.
	 * 
	 * @param message
	 *            A string of text to send.
	 */
	private void sendMessage(String message) {
		// Check that we're actually connected before trying anything
		if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		// Check that there's actually something to send
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothChatService to write
			byte[] send = message.getBytes();
			mChatService.write(send);

			// Reset out string buffer to zero and clear the edit text field
			mOutStringBuffer.setLength(0);
			mOutEditText.setText(mOutStringBuffer);
		}
	}

	// The action listener for the EditText widget, to listen for the return key
	private TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
		public boolean onEditorAction(TextView view, int actionId,
				KeyEvent event) {
			// If the action is a key-up event on the return key, send the
			// message
			if (actionId == EditorInfo.IME_NULL
					&& event.getAction() == KeyEvent.ACTION_UP) {
				String message = view.getText().toString();
				sendMessage(message);
			}
			if (D)
				Log.i(TAG, "END onEditorAction");
			return true;
		}
	};

	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				if (D)
					Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED:
					mTitle.setText(R.string.title_connected_to);
					mTitle.append(mConnectedDeviceName);
					mConversationArrayAdapter.clear();
					break;
				case BluetoothChatService.STATE_CONNECTING:
					mTitle.setText(R.string.title_connecting);
					break;
				case BluetoothChatService.STATE_LISTEN:
				case BluetoothChatService.STATE_NONE:
					mTitle.setText(R.string.title_not_connected);
					break;
				}
				break;
			case MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer
				String writeMessage = new String(writeBuf);
				mConversationArrayAdapter.add("Me:  "
						+ Util.encodeStringToHex(writeMessage));
				break;
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				// construct a string from the valid bytes in the buffer
				String readMessage = new String(readBuf, 0, msg.arg1);
				mConversationArrayAdapter.add(mConnectedDeviceName + ":  "
						+ Util.encodeStringToHex(readMessage));
				
				//TODO: 데이터 받았을
				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE_SECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, true);
			}
			break;
		case REQUEST_CONNECT_DEVICE_INSECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, false);
			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				setupChat();
			} else {
				// User did not enable Bluetooth or an error occured
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
			/*
			 * if this is the activity result from authorization flow, do a call
			 * back to authorizeCallback Source Tag: login_tag
			 */
		case AUTHORIZE_ACTIVITY_RESULT_CODE: {
			Utility.mFacebook.authorizeCallback(requestCode, resultCode, data);
			break;
		}
		/*
		 * if this is the result for a photo picker from the gallery, upload the
		 * image after scaling it. You can use the Utility.scaleImage() function
		 * for scaling
		 */
		case PICK_EXISTING_PHOTO_RESULT_CODE: {
			if (resultCode == Activity.RESULT_OK) {
				Uri photoUri = data.getData();
				if (photoUri != null) {
					Bundle params = new Bundle();
					try {
						params.putByteArray("photo", Utility.scaleImage(
								getApplicationContext(), photoUri));
					} catch (IOException e) {
						e.printStackTrace();
					}
					params.putString("caption", "NonstopSoC");
					Utility.mAsyncRunner.request("me/photos", params, "POST",
							new PhotoUploadListener(), null);
				} else {
					Toast.makeText(getApplicationContext(),
							"Error selecting image from the gallery.",
							Toast.LENGTH_SHORT).show();
				}
			} else {
				Toast.makeText(getApplicationContext(),
						"No image selected for upload.", Toast.LENGTH_SHORT)
						.show();
			}
			break;

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
					new UploadPhotoResultDialog(BluetoothChat.this,
							"Upload Photo executed", response).show();
				}
			});
		}

		public void onFacebookError(FacebookError error) {
			dialog.dismiss();
			Toast.makeText(getApplicationContext(),
					"Facebook Error: " + error.getMessage(), Toast.LENGTH_LONG)
					.show();
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

	class ViewHolder {
		TextView main_list_item;
	}

	private void connectDevice(Intent data, boolean secure) {
		// Get the device MAC address
		String address = data.getExtras().getString(
				DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		// Get the BLuetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		mChatService.connect(device, secure);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent serverIntent = null;
		if (item.getItemId() == R.id.secure_connect_scan) {

			// Launch the DeviceListActivity to see devices and do scan
			serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
			return true;

		} else if (item.getItemId() == R.id.discoverable) {
			// Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		}
		return false;
	}

}
