/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.bluetoothchatDJI;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.bluetoothchatDJI.R;
import com.example.android.common.logger.Log;

import org.bouncycastle.jcajce.provider.drbg.DRBG;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothChatFragment extends Fragment {

    private static final String TAG = "BluetoothChatFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private TextView mTxt_Alt;
    private TextView mTxt_Spd;
    private TextView mTxt_P1Lat;
    private TextView mTxt_P1Lon;
    private TextView mTxt_P2Lat;
    private TextView mTxt_P2Lon;
    private TextView mTxt_P3Lat;
    private TextView mTxt_P3Lon;
    private TextView mTxt_P4Lat;
    private TextView mTxt_P4Lon;

    private Button BtnUpload;

    Bundle Uploaded_Data = new Bundle();

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mConversationView = (ListView) view.findViewById(R.id.in);
        // mOutEditText = (EditText) view.findViewById(R.id.edit_text_out);
        // mSendButton = (Button) view.findViewById(R.id.button_send);

        mTxt_Alt = view.findViewById(R.id.blt_txt_Alt);
        mTxt_Spd = view.findViewById(R.id.blt_txt_Spd);

        mTxt_P1Lat = view.findViewById(R.id.blt_txt_P1Lat);
        mTxt_P1Lon = view.findViewById(R.id.blt_txt_P1Lon);
        mTxt_P2Lat = view.findViewById(R.id.blt_txt_P2Lat);
        mTxt_P2Lon = view.findViewById(R.id.blt_txt_P2Lon);
        mTxt_P3Lat = view.findViewById(R.id.blt_txt_P3Lat);
        mTxt_P3Lon = view.findViewById(R.id.blt_txt_P3Lon);
        mTxt_P4Lat = view.findViewById(R.id.blt_txt_P4Lat);
        mTxt_P4Lon = view.findViewById(R.id.blt_txt_P4Lon);
        BtnUpload = view.findViewById(R.id.button_Use_Misn);

        BtnUpload.setEnabled(false);
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);

        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(getActivity(), mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    // mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);

                    if (readMessage != null) {
                        String SeperateValues[] = readMessage.split(",");
                        final String Data_Alt = SeperateValues[0];
                        final String Data_Spd = SeperateValues[1];
                        final String Data_P1Lat = SeperateValues[2];
                        final String Data_P1Lon = SeperateValues[3];
                        final String Data_P2Lat = SeperateValues[4];
                        final String Data_P2Lon = SeperateValues[5];
                        final String Data_P3Lat = SeperateValues[6];
                        final String Data_P3Lon = SeperateValues[7];
                        final String Data_P4Lat = SeperateValues[8];
                        final String Data_P4Lon = SeperateValues[9];

                        mTxt_Alt.setText(String.valueOf(Data_Alt));
                        mTxt_Spd.setText(String.valueOf(Data_Spd));
                        mTxt_P1Lat.setText(String.valueOf(Data_P1Lat));
                        mTxt_P1Lon.setText(String.valueOf(Data_P1Lon));
                        mTxt_P2Lat.setText(String.valueOf(Data_P2Lat));
                        mTxt_P2Lon.setText(String.valueOf(Data_P2Lon));
                        mTxt_P3Lat.setText(String.valueOf(Data_P3Lat));
                        mTxt_P3Lon.setText(String.valueOf(Data_P3Lon));
                        mTxt_P4Lat.setText(String.valueOf(Data_P4Lat));
                        mTxt_P4Lon.setText(String.valueOf(Data_P4Lon));

                        BtnUpload.setEnabled(true);
                        BtnUpload.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                if (null != v) {
                                    Uploaded_Data.putStringArray("AltSpd", new String[]{Data_Alt, Data_Spd});
                                    Uploaded_Data.putStringArray("Point-1", new String[]{Data_P1Lat, Data_P1Lon});
                                    Uploaded_Data.putStringArray("Point-2", new String[]{Data_P2Lat, Data_P2Lon});
                                    Uploaded_Data.putStringArray("Point-3", new String[]{Data_P3Lat, Data_P3Lon});
                                    Uploaded_Data.putStringArray("Point-4", new String[]{Data_P4Lat, Data_P4Lon});

                                    Intent intent_upload = new Intent(v.getContext(), MainActivityDJI.class);
                                    intent_upload.putExtras(Uploaded_Data);
                                    startActivity(intent_upload);
                                }
                            }
                         });
                    }

                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        String saved_Data_Alt = String.valueOf(mTxt_Alt.getText());
        savedInstanceState.putString("txt_Data1", saved_Data_Alt);
        String saved_Data_Spd = String.valueOf(mTxt_Spd.getText());
        savedInstanceState.putString("txt_Data2", saved_Data_Spd);
        String saved_Data_P1Lat = String.valueOf(mTxt_P1Lat.getText());
        savedInstanceState.putString("txt_Data3", saved_Data_P1Lat);
        String saved_Data_P1Lon = String.valueOf(mTxt_P1Lon.getText());
        savedInstanceState.putString("txt_Data4", saved_Data_P1Lon);
        String saved_Data_P2Lat = String.valueOf(mTxt_P2Lat.getText());
        savedInstanceState.putString("txt_Data5", saved_Data_P2Lat);
        String saved_Data_P2Lon = String.valueOf(mTxt_P2Lon.getText());
        savedInstanceState.putString("txt_Data6", saved_Data_P2Lon);
        String saved_Data_P3Lat = String.valueOf(mTxt_P3Lat.getText());
        savedInstanceState.putString("txt_Data7", saved_Data_P3Lat);
        String saved_Data_P3Lon = String.valueOf(mTxt_P3Lon.getText());
        savedInstanceState.putString("txt_Data8", saved_Data_P3Lon);
        String saved_Data_P4Lat = String.valueOf(mTxt_P4Lat.getText());
        savedInstanceState.putString("txt_Data7", saved_Data_P4Lat);
        String saved_Data_P4Lon = String.valueOf(mTxt_P4Lon.getText());
        savedInstanceState.putString("txt_Data8", saved_Data_P4Lon);
    }

}
