package com.prestigio.launcher.mdm;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.whitebyte.wifihotspotutils.WifiApManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by yanis on 27.3.14.
 */
public class DialogPassword extends DialogFragment {
    WifiApManager mWifiManager;
    WifiConfiguration myConfig;
    final LinkedHashMap<String, Integer> KeyManagement = new LinkedHashMap<String, Integer>(3);
    ArrayAdapter<String> adapter;

    private class ViewHolder{
        ViewHolder(View view){
            passwordEditText = (EditText) view.findViewById(R.id.password);
            ssidEditText = (EditText) view.findViewById(R.id.SSID);
            securitySpinner = (Spinner) view.findViewById(R.id.spinner);
            showPasswordCheckBox = (CheckBox) view.findViewById(R.id.show_password);
            passwordArea = (LinearLayout) view.findViewById(R.id.password_area);
        }

        final EditText passwordEditText;
        final EditText ssidEditText;
        final Spinner securitySpinner;
        final CheckBox showPasswordCheckBox;
        final LinearLayout passwordArea;
    }

    private AtomicReference<ViewHolder> mViewHolder = new AtomicReference<ViewHolder>();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        mWifiManager = new WifiApManager(getActivity());


        View view = inflater.inflate(R.layout.dialog_password, null);
        mViewHolder.set(new ViewHolder(view));

        KeyManagement.put("None", 0);
        KeyManagement.put("WPA PSK", 1);
        KeyManagement.put("WPA2 PSK", 4);

        mViewHolder.get().passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ( (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) || actionId == EditorInfo.IME_ACTION_DONE) {
                    if (checkPasswordIsCorrect()) {
                        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(mViewHolder.get().passwordEditText.getWindowToken(), 0);
                    }
                }
                return true;
            }
        });

        mViewHolder.get().ssidEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ( (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) || actionId == EditorInfo.IME_ACTION_DONE) {
                    if (checkSSIDIsCorrect()) {
                        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(mViewHolder.get().ssidEditText.getWindowToken(), 0);
                        /*View next_view = v.focusSearch(View.FOCUS_FORWARD);
                        next_view.requestFocus();*/
                    }
                }
                return true;
            }
        });

        adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,
                KeyManagement.keySet().toArray(new String[KeyManagement.size()]));
        mViewHolder.get().securitySpinner.setAdapter(adapter);

        mViewHolder.get().securitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (((Spinner)parent).getSelectedItem().equals("None")){
                    mViewHolder.get().passwordArea.setVisibility(View.GONE);
                    enableSaveButtonIfPossible();
                } else {
                    mViewHolder.get().passwordArea.setVisibility(View.VISIBLE);
                    enableSaveButtonIfPossible();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mViewHolder.get().showPasswordCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((CheckBox)v).isChecked()){
                    mViewHolder.get().passwordEditText.setInputType(EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    mViewHolder.get().passwordEditText.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
                }
            }
        });

        initDialog();

        mViewHolder.get().ssidEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count > 0) {
                    enableSaveButtonIfPossible();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mViewHolder.get().passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count > 0) {
                    enableSaveButtonIfPossible();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });




        builder.setView(view)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {


                        if ((keyManagementIsNone() && checkSSIDIsCorrect()) || (!keyManagementIsNone() && checkPasswordIsCorrect() && checkSSIDIsCorrect())){

                            String spinnerValue = (String)mViewHolder.get().securitySpinner.getSelectedItem();

                            myConfig.SSID = mViewHolder.get().ssidEditText.getText().toString();

                            if (KeyManagement.get(spinnerValue) != 0){ // no password
                                myConfig.preSharedKey = mViewHolder.get().passwordEditText.getText().toString().trim();
                            }


                            myConfig.hiddenSSID = false;
                            myConfig.status = WifiConfiguration.Status.ENABLED;
                            myConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                            myConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);

                            //myConfig.allowedKeyManagement.set(4);  //WifiConfiguration.KeyMgmt.WPA_PSK (strings)
                            myConfig.allowedKeyManagement.clear();
                            myConfig.allowedKeyManagement.set(KeyManagement.get(spinnerValue));

                            myConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                            myConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                            myConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                            myConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);


                            mWifiManager.setWifiApConfiguration(myConfig);

                            if (mWifiManager.isWifiApEnabled()) {
                                mWifiManager.setWifiApEnabled(myConfig, false);
                                mWifiManager.setWifiApEnabled(myConfig, true);
                            }
                        }
                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        DialogPassword.this.getDialog().cancel();
                    }
                })
                .setTitle("Set up Wi-Fi hotspot");


        return builder.create();
    }

    private void initDialog(){
        myConfig = mWifiManager.getWifiApConfiguration();

        management:{
            for (Map.Entry<String, Integer> entry : KeyManagement.entrySet()) {
                if (myConfig.allowedKeyManagement.get(entry.getValue())) {
                    mViewHolder.get().securitySpinner.setSelection(adapter.getPosition(entry.getKey()));
                    break management;
                }
            }
            mViewHolder.get().securitySpinner.setSelection(adapter.getPosition("WPA PSK"));
        }

        mViewHolder.get().ssidEditText.setText( myConfig.SSID );

        if (getDialog() != null) {
            ((ScrollView) getDialog().findViewById(R.id.scroll_view)).fullScroll(ScrollView.FOCUS_UP);
        }
    }

    private boolean checkPasswordIsCorrect(){
        String password = mViewHolder.get().passwordEditText.getText().toString().trim();
        return password.length() >= 8;
    }

    private boolean checkSSIDIsCorrect(){
        String SSID = mViewHolder.get().ssidEditText.getText().toString().trim();
        return SSID.length() >= 1;
    }

    private boolean keyManagementIsNone(){
        return mViewHolder.get().securitySpinner.getSelectedItem().equals("None");
    }

    private void enableSaveButtonIfPossible(){
        if ((keyManagementIsNone() && checkSSIDIsCorrect()) || (!keyManagementIsNone() && checkPasswordIsCorrect() && checkSSIDIsCorrect())){
            ((AlertDialog)DialogPassword.this.getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
        } else {
            ((AlertDialog)DialogPassword.this.getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        enableSaveButtonIfPossible();

        initDialog();



        /*InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);*/
    }

}
