package com.prestigio.launcher.mdm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.todddavies.components.progressbar.ProgressWheel;
import com.whitebyte.wifihotspotutils.WIFI_AP_STATE;
import com.whitebyte.wifihotspotutils.WifiApManager;

public class MdmLauncherActivity extends Activity {
	public static final int STATUS_BAR_DISABLE_BACK = 0x00400000;
	public static final int STATUS_BAR_DISABLE_HOME = 0x00200000;
	public static final int STATUS_BAR_DISABLE_RECENT = 0x01000000;
	public static final int STATUS_BAR_DISABLE_SEARCH = 0x02000000;
	public static final int STATUS_BAR_DISABLE_EXPAND = 0x00010000;

	// public static final String INTENT_ACTION =
	// "com.prestigio.intent.action.WIFI_MANAGEMENT";

	WifiApManager mWifiManager;
	private final Object mSyncWifi = new Object();

	AudioManager audioManager;

	Handler handler;

	ProgressWheel progress_spent;
	ProgressWheel progress_left;
	TextView mTextMbLeft;
	TextView mTextMbSpent;

	DialogPassword mDialogPassword;

	private TextView mHotSpotView;
	private TextView mPasswordView;
	private View mOnRouteInfo;
	private View mOnrouteComm;

	private static class PropertiesData {
		public PropertiesData() {
			refresh_time = 20;
			Internet_Status = 1;
			Mb_spent = 0;
			Mb_remaining = 1024;
		}

		int refresh_time;
		int Internet_Status;
		int Mb_spent;
		int Mb_remaining;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		progress_spent = (ProgressWheel) findViewById(R.id.pw_spinner_spent);
		progress_left = (ProgressWheel) findViewById(R.id.pw_spinner_left);
		mTextMbLeft = (TextView) findViewById(R.id.text_mb_left);
		mTextMbSpent = (TextView) findViewById(R.id.text_mb_spent);

		mHotSpotView = (TextView) findViewById(R.id.textViewWifi);
		mPasswordView = (TextView) findViewById(R.id.textViewPassword);
		mOnRouteInfo = findViewById(R.id.viewOnRouteInfo);
		mOnrouteComm = findViewById(R.id.viewOnrouteComm);

		mDialogPassword = new DialogPassword();

		final int flag = STATUS_BAR_DISABLE_HOME | STATUS_BAR_DISABLE_RECENT
				| STATUS_BAR_DISABLE_BACK | STATUS_BAR_DISABLE_SEARCH
				| STATUS_BAR_DISABLE_EXPAND;

		disable(flag);

		mWifiManager = new WifiApManager(this);

		initConfig();

		// audioManager = (AudioManager)
		// this.getSystemService(Context.AUDIO_SERVICE);

		/*
		 * StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
		 * .detectDiskReads() .detectDiskWrites() .detectNetwork() // or
		 * .detectAll() for all detectable problems .penaltyLog() .build());
		 * StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
		 * .detectLeakedSqlLiteObjects() //.detectLeakedClosableObjects()
		 * .penaltyLog() .penaltyDeath() .build());
		 */

		handler = new Handler(new Handler.Callback() {
			long mLastRun = 0;
			int mRefreshTime = 0;
			int mRefreshSTime = 0;
			long mLastSRun = 0;
			
			@Override
			public boolean handleMessage(Message msg) {
				/*
				 * TrafficRecord device = new TrafficRecord();
				 * 
				 * long total = 1000 * 1024 * 1000; long current = device.rx +
				 * device.tx; if (current > total){ current = total; }
				 * 
				 * mTextMbSpent.setText( String.format("%.1f", (float)current /
				 * 1000 / 1024) ); // current / total
				 * progress_spent.setProgress((int) ((float)current / total *
				 * 360));
				 * 
				 * mTextMbLeft.setText( String.format("%.1f", (float)(total -
				 * current) / 1000 / 1024) ); progress_left.setProgress((int)
				 * ((float) (total - current) / total * 360));
				 */
				
				long tm = SystemClock.elapsedRealtime();
				
				if (tm - mLastSRun > mRefreshSTime * 1000) {
					
				try{
					mRefreshSTime = 300;
					mLastSRun = tm;
					Intent intent = new Intent();
					intent.setClassName("org.onroute.balancemanager", "org.onroute.balancemanager.BalanceManager");
					startService(intent);
					
					
				}catch
				(Exception e){
					Log.d("EEE", "start service "+e.getMessage());
				}
				}
				
				if (tm - mLastRun > mRefreshTime * 1000) {
					PropertiesData data = getInfo();// readProperties();

					int total = data.Mb_spent + data.Mb_remaining;
					mTextMbSpent.setText(String.format("%.1f",
							(float) data.Mb_spent / (1024 * 1024))); // current
																		// /
																		// total
					progress_spent.setProgress((int) ((float) data.Mb_spent
							/ total * 360));
					float left = (float) data.Mb_remaining ;
					if (left<0) left = 0;
					mTextMbLeft.setText(String.format("%.1f",
							(float) left / (1024 * 1024)));
					
					float p_left= (float) (total - data.Mb_spent);
					if (p_left<0) p_left=0;
					progress_left
							.setProgress((int) (p_left
									/ total * 360));

					mRefreshTime = data.refresh_time;
					mLastRun = tm;

					if (data.Internet_Status == 1) {
						findViewById(R.id.textViewInternet).setEnabled(true);
					}

					if (data.Internet_Status == 0) {
						findViewById(R.id.textViewInternet).setEnabled(false);
						if (getDataState() == TelephonyManager.DATA_CONNECTED) {
							changeMobileData(true);
						}
					}
				}

				handler.sendEmptyMessageDelayed(0, 1000);

				return false;
			}
		});

		changeInternetIcon(isMobileDataEnabled());
		changeWifiIcon(mWifiManager.getWifiApState() == WIFI_AP_STATE.WIFI_AP_STATE_ENABLED);
		changeAirplaneIcon(isAirplaneModeOn(this));

		TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		tm.listen(new PhoneStateListener() {
			public void onServiceStateChanged(ServiceState serviceState) {
				if (serviceState.getState() == ServiceState.STATE_IN_SERVICE) {
					changeInternetIcon(true);
				} else {
					changeInternetIcon(false);
				}
			}

			public void onDataConnectionStateChanged(int state) {
				if (state == TelephonyManager.DATA_CONNECTED) {
					changeInternetIcon(true);
				} else {
					changeInternetIcon(false);
				}
			}
		}, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);

		IntentFilter mIntentFilter = new IntentFilter(
				"android.net.wifi.WIFI_AP_STATE_CHANGED");
		registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(action)) {
					int state = intent.getIntExtra("wifi_state", 0);
					if (state == 13) { // WifiManager.WIFI_AP_STATE_ENABLED
						changeWifiIcon(true);
					} else {
						changeWifiIcon(false);
					}
				}
			}
		}, mIntentFilter);

		IntentFilter intentFilter = new IntentFilter(
				"android.intent.action.SERVICE_STATE");
		registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				changeAirplaneIcon(isAirplaneModeOn(MdmLauncherActivity.this));
				changeInternetIcon(isMobileDataEnabled());
			}
		}, intentFilter);

		SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
		String enabled = prefs.getString("enable", "true");
		if (!enabled.equals("true")) {
			enableAppFeatures(false);
		}
	}

	protected void onNewIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		if (extras != null /*
							 * && intent.getAction() != null &&
							 * intent.getAction().equals(INTENT_ACTION)
							 */) {
			if ((extras.containsKey("disable") && extras.getBoolean("disable"))
					|| (extras.containsKey("enable") && !extras
							.getBoolean("enable"))) {
				enableAppFeatures(false);
			} else if ((extras.containsKey("disable") && !extras
					.getBoolean("disable"))
					|| (extras.containsKey("enable") && extras
							.getBoolean("enable"))) {
				enableAppFeatures(true);
			}
		}
		handler.sendEmptyMessage(0);
	}

	private void enableAppFeatures(boolean enable) {
		synchronized (mSyncWifi) {
			if (enable) {
				SharedPreferences prefs = getSharedPreferences("settings",
						MODE_PRIVATE);
				SharedPreferences.Editor editor = prefs.edit();
				editor.putString("enable", "true");
				editor.commit();
				mHotSpotView.setClickable(true);
				changeWifiIcon(false);
				mPasswordView.setClickable(true);
				changePasswordIcon(true);
				mOnRouteInfo.setClickable(true);
				mOnRouteInfo.setBackgroundColor(0xFF9b59b6);
				mOnrouteComm.setClickable(true);
				mOnrouteComm.setBackgroundColor(0xFF16a085);
				
				if (prefs.getString("wifiEnabled", "false").equals("true")) {
					mWifiManager.setWifiApEnabled(
							mWifiManager.getWifiApConfiguration(), true);
				} else {
					mWifiManager.setWifiApEnabled(
							mWifiManager.getWifiApConfiguration(), false);
				}
				
				
			} else {
				SharedPreferences prefs = getSharedPreferences("settings",
						MODE_PRIVATE);
				SharedPreferences.Editor editor = prefs.edit();
				editor.putString("enable", "false");
				if (mWifiManager.isWifiApEnabled()) {
					editor.putString("wifiEnabled", "true");
				} else {
					editor.putString("wifiEnabled", "false");
				}
				editor.commit();
				mHotSpotView.setClickable(false);
				
				mHotSpotView.setCompoundDrawablesWithIntrinsicBounds(0,
						R.drawable.img_wifi_soff, 0, 0);
				mPasswordView.setClickable(false);
				mPasswordView.setCompoundDrawablesWithIntrinsicBounds(0,
						R.drawable.img_password_soff, 0, 0);
				mOnRouteInfo.setClickable(false);
				mOnRouteInfo.setBackgroundColor(0x111111);
				mOnrouteComm.setBackgroundColor(0x111111);
				mOnrouteComm.setClickable(false);
				mWifiManager.setWifiApEnabled(
						mWifiManager.getWifiApConfiguration(), false);
			}
		}
	}

	public void enableTest(View v) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				Intent intent = new Intent(/* INTENT_ACTION */);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				intent.setClassName("com.prestigio.launcher.mdm",
						"com.prestigio.launcher.mdm.MdmLauncherActivity");
				intent.putExtra("enable", true); // enable
				// intent.putExtra("enable", false); // disable
				// intent.putExtra("disable", true); // disable
				// intent.putExtra("disable", false); // enable
				startActivity(intent);
			}
		}).start();

	}

	public void disableTest(View v) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				Intent intent = new Intent(/* INTENT_ACTION */);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				intent.setClassName("com.prestigio.launcher.mdm",
						"com.prestigio.launcher.mdm.MdmLauncherActivity");
				intent.putExtra("enable", false); // enable
				// intent.putExtra("enable", false); // disable
				// intent.putExtra("disable", true); // disable
				// intent.putExtra("disable", false); // enable
				startActivity(intent);
			}
		}).start();

	}

	private PropertiesData readProperties() {
		PropertiesData data = new PropertiesData();

		Properties props = new Properties();
		try {
			props.load(new BufferedInputStream(new FileInputStream(new File(
					getFilesDir(), "launcher_config.ini"))));
			String refresh_time = props.getProperty("refresh_time", "20");
			refresh_time = refresh_time.replaceAll("[^\\d]", "");
			data.refresh_time = Integer.parseInt(refresh_time);
		} catch (IOException e) {
			e.printStackTrace();
		}

		props = new Properties();
		try {
			props.load(new BufferedInputStream(new FileInputStream(new File(
					getFilesDir(), "launcher_display.ini"))));
			String Internet_Status = props.getProperty("Internet_Status", "1");
			String Mb_spent = props.getProperty("Mb_spent", "0");
			String Mb_remaining = props.getProperty("Mb_remaining", "1024");

			Internet_Status = Internet_Status.replaceAll("[^\\d]", "");
			Mb_spent = Mb_spent.replaceAll("[^\\d]", "");
			Mb_remaining = Mb_remaining.replaceAll("[^\\d]", "");

			data.Internet_Status = Integer.parseInt(Internet_Status);
			data.Mb_spent = Integer.parseInt(Mb_spent);
			// data.Mb_spent = (int) (Math.random() * 300);
			data.Mb_remaining = Integer.parseInt(Mb_remaining);
			// data.Mb_remaining = (int) (Math.random() * 500);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return data;
	}

	private PropertiesData getInfo() {
		final Uri TRAFFIC_URI = Uri
				.parse("content://org.onroute.balancemanager.BalanceContentProvider/traffic");
		String[] arrInfo = new String[3];
		arrInfo[0] = "all_traffic";
		arrInfo[1] = "limit_traffic";
		arrInfo[2] = "status_traffic";

		String[] data = null;
		ContentResolver myContentResolver = this.getContentResolver();
		Cursor dataCursor = myContentResolver.query(TRAFFIC_URI, arrInfo, null,
				data, null);
		PropertiesData p_data = new PropertiesData();

		boolean dataEx = dataCursor.moveToFirst();

		if (dataEx) {
			int traf = dataCursor.getColumnIndex(arrInfo[1]);
			int receiv = dataCursor.getColumnIndex(arrInfo[0]);
			int status = dataCursor.getColumnIndex(arrInfo[2]);

			p_data.refresh_time = 20;
			p_data.Internet_Status = dataCursor.getInt(status);
			p_data.Mb_spent = dataCursor.getInt(receiv);
			// data.Mb_spent = (int) (Math.random() * 300);
			p_data.Mb_remaining = dataCursor.getInt(traf)
					- dataCursor.getInt(receiv);
		}
		dataCursor.close();
		return p_data;

	}

	private void initConfig() {
		initFile("launcher_config.ini");
		initFile("launcher_display.ini");
	}

	private void initFile(String file_name) {
		File config = new File(getFilesDir(), file_name);

		if (!config.exists()) {
			try {
				boolean created = config.createNewFile();
				if (created) {
					BufferedOutputStream out_stream = new BufferedOutputStream(
							new FileOutputStream(config));
					InputStream in_stream = new BufferedInputStream(getAssets()
							.open(file_name));

					int data = in_stream.read();
					while (data != -1) {
						out_stream.write(data);
						data = in_stream.read();
					}
					in_stream.close();
					out_stream.close();
				} else {
					// TODO: error creating file. low space, no access?
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void changeIcon(int id, boolean enabled) {
		TextView view = (TextView) findViewById(id);
		if (enabled) {
			view.setTextColor(0xFFffffff);
		} else {
			view.setTextColor(0xFF717171);
		}
		switch (id) {
		case R.id.textViewInternet:
			if (enabled) {
				view.setCompoundDrawablesWithIntrinsicBounds(0,
						R.drawable.img_data_on, 0, 0);
				view.setText(R.string.internet);
			} else {
				view.setCompoundDrawablesWithIntrinsicBounds(0,
						R.drawable.img_data_off, 0, 0);
				view.setText(R.string.internet_off);
			}
			break;
		case R.id.textViewWifi:
			if (enabled) {
				view.setCompoundDrawablesWithIntrinsicBounds(0,
						R.drawable.img_wifi_on, 0, 0);
				view.setText(R.string.hotspot);
			} else {
				view.setCompoundDrawablesWithIntrinsicBounds(0,
						R.drawable.img_wifi_off, 0, 0);
				view.setText(R.string.hotspot_off);
			}
			break;
		case R.id.textViewAirplane:
			if (enabled) {
				view.setCompoundDrawablesWithIntrinsicBounds(0,
						R.drawable.img_airplane_on, 0, 0);
				view.setText(R.string.airplane);
			} else {
				view.setCompoundDrawablesWithIntrinsicBounds(0,
						R.drawable.img_airplane_off, 0, 0);
				view.setText(R.string.airplane_off);
			}
			break;
		case R.id.textViewPassword:
			if (enabled) {
				view.setCompoundDrawablesWithIntrinsicBounds(0,
						R.drawable.img_password_on, 0, 0);
			} else {
				view.setCompoundDrawablesWithIntrinsicBounds(0,
						R.drawable.img_password_off, 0, 0);
			}
			break;
		case R.id.textViewPowerOff:
			if (enabled) {
				view.setCompoundDrawablesWithIntrinsicBounds(0,
						R.drawable.img_power_on, 0, 0);
			} else {
				view.setCompoundDrawablesWithIntrinsicBounds(0,
						R.drawable.img_power_off, 0, 0);
			}
			break;
		}
	}

	private void changeInternetIcon(boolean enabled) {
		changeIcon(R.id.textViewInternet, enabled);
	}

	private void changeWifiIcon(boolean enabled) {
		changeIcon(R.id.textViewWifi, enabled);
	}

	private void changeAirplaneIcon(boolean enabled) {
		changeIcon(R.id.textViewAirplane, enabled);
	}

	private void changePasswordIcon(boolean enabled) {
		changeIcon(R.id.textViewPassword, enabled);
	}

	private void changePowerIcon(boolean enabled) {
		changeIcon(R.id.textViewPowerOff, enabled);
	}

	private void disable(int val) {
		Object object = this.getSystemService("statusbar");

		Class<?> statusBarManager;
		try {
			statusBarManager = Class.forName("android.app.StatusBarManager");
			Method[] method = statusBarManager.getMethods();
			Field fld[] = statusBarManager.getDeclaredFields();
			for (int i = 0; i < fld.length; i++) {
				String s = fld[i].toString();
				Log.d("class", "field " + s);
			}
			Class name = object.getClass();

			Field field = statusBarManager.getDeclaredField("DISABLE_HOME");

			// android.util.Log.i("MainActivity", "value of::: " + field);

			Method disable = statusBarManager.getMethod("disable",
					new Class[] { int.class });

			disable.setAccessible(true);

			field.setAccessible(true);
			disable.invoke(object, val);// 262144));

		} catch (ClassNotFoundException e) {
			// Log.i("MAinActivity::", "No Such Class Found Exception");
			e.printStackTrace();
		} catch (Exception e) {
			// Log.i("MAinActivity::", " Exception occur");
			e.printStackTrace();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		handler.sendEmptyMessage(0);
	}

	@Override
	public void onPause() {
		super.onPause();
		handler.removeCallbacksAndMessages(null);
	}

	@Override
	public void onStop() {
		super.onStop();
		handler.removeCallbacksAndMessages(null);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		disable(0);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_HOME:
				return true;
				/*
				 * case KeyEvent.KEYCODE_VOLUME_DOWN: if
				 * (isPropertyEnabled(DUMP_STATE_PROPERTY)) { dumpState();
				 * return true; } break;
				 */
			}
		} else if (event.getAction() == KeyEvent.ACTION_UP) {
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_HOME:
				return true;
			}
		}

		return super.dispatchKeyEvent(event);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		/*
		 * Intent intent = new Intent("android.intent.action.MAIN");
		 * intent.setClassName("com.android.settings",
		 * "com.android.settings.Settings$SimManagementActivity");
		 * startActivity(intent);
		 */

		/*
		 * Reflections reflections = new Reflections("com.android.settings");
		 * 
		 * Set<Class<? extends Object>> allClasses =
		 * reflections.getSubTypesOf(Object.class);
		 * 
		 * for(Class<?> cl: allClasses){ Log.d("MdmLauncher", cl.getName()); }
		 */
		// com.mediatek.gemini.SimManagement
		// com.mediatek.CellConnService.CellConnMgr

		if ((keyCode == KeyEvent.KEYCODE_MENU)) {
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			overridePendingTransition(android.R.anim.fade_in,
					android.R.anim.fade_out);
		}
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			return false;
		}

		return super.onKeyDown(keyCode, event);
	}

	public void onClickInternet(View v) {
		TextView tv = (TextView)v;
		tv.setText("");
		tv.setCompoundDrawablesWithIntrinsicBounds(0,
				R.drawable.img_data_soff, 0, 0);
	
		changeMobileData();
	}

	public void onClickHotspot(View v) {
		synchronized (mSyncWifi) {
			TextView tv = (TextView)v;
			tv.setText("");
			tv.setCompoundDrawablesWithIntrinsicBounds(0,
					R.drawable.img_wifi_soff, 0, 0);
			
			if (mWifiManager.isWifiApEnabled()) {
				mWifiManager.setWifiApEnabled(
						mWifiManager.getWifiApConfiguration(), false);
			} else {
				mWifiManager.setWifiApEnabled(
						mWifiManager.getWifiApConfiguration(), true);
			}
			
		}
	}

	public void onClickAirplane(View v) {
		boolean isEnabled = isAirplaneModeOn(this);
		// Log.d("MdmLauncher", String.valueOf(isEnabled));
		TextView tv = (TextView)v;
		tv.setText("");
		tv.setCompoundDrawablesWithIntrinsicBounds(0,
				R.drawable.img_airplane_soff, 0, 0);
		// Toggle airplane mode.
		setSettings(this, isEnabled ? 0 : 1);
		// Post an intent to reload.
		Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
		intent.putExtra("state", !isEnabled);
		this.sendBroadcast(intent);
	}

	public void onClickPassword(View v) {

		/*
		 * Intent intent = new Intent("android.net.conn.TETHER_STATE_CHANGED");
		 * intent.setClassName("com.android.settings",
		 * "com.android.settings.wifi.hotspot.TetherWifiSettings"); //
		 * "com.android.settings.wifi.hotspot.TetherWifiSettings"); // if
		 * (tryEnablingWifi()) // localIntent1.putExtra("only_access_points",
		 * true);
		 * 
		 * startActivity(intent);
		 */

		mDialogPassword.show(getFragmentManager(), "dialog_password");
	}

    public void onClickRefil(View v) {

    	//startWiFi(this);
        Intent intent = new Intent();
        intent.setClassName("org.onroute.balancemanager",
                "org.onroute.balancemanager.TestActivity");
        startActivity(intent);

    }
    
	public void onClickPoweroff(View v) {
		try {
			PowerManager pm;
			pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			// pm.reboot("null");
			// pm.goToSleep(SystemClock.uptimeMillis());

			final Class shutdownThread = Class
					.forName("com.android.server.power.ShutdownThread");

			Class[] args = new Class[2];
			args[0] = Context.class;
			args[1] = Boolean.TYPE;
			Method m = shutdownThread.getDeclaredMethod("shutdown", args);
			m.setAccessible(true); // if security settings allow this
			Object o = m.invoke(null, this, false); // use null if the method is
													// static

			/*
			 * Method[] method = statusBarManager.getMethods(); Field fld[] =
			 * statusBarManager.getDeclaredFields(); for (int i = 0; i <
			 * fld.length; i++) { String s = fld[i].toString(); Log.d("class",
			 * "field " + s); } Class name = object.getClass();
			 * 
			 * Field field = statusBarManager.getDeclaredField("DISABLE_HOME");
			 * 
			 * //android.util.Log.i("MainActivity", "value of::: " + field);
			 * 
			 * Method disable = statusBarManager.getMethod("disable", new
			 * Class[] { int.class });
			 * 
			 * disable.setAccessible(true);
			 * 
			 * field.setAccessible(true); disable.invoke(object, new
			 * Integer(val));// 262144));
			 */

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onClickAllAppsInfo(View v) {
		Intent intent = new Intent(this, AllAppsActivity.class);
		intent.putExtra("app_list", "info");
		startActivity(intent);
		overridePendingTransition(android.R.anim.fade_in,
				android.R.anim.fade_out);
	}

	public void onClickAllAppsCommunications(View v) {
		Intent intent = new Intent(this, AllAppsActivity.class);
		intent.putExtra("app_list", "communication");
		startActivity(intent);
		overridePendingTransition(android.R.anim.fade_in,
				android.R.anim.fade_out);
	}

	public void onAdvancedOptions(View v) {
		Intent intent = new Intent("android.net.conn.TETHER_STATE_CHANGED");
		intent.setClassName("com.android.settings",
				"com.android.settings.wifi.hotspot.TetherWifiSettings");
		startActivity(intent);

		/*
		 * final Intent intent = new Intent(Intent.ACTION_MAIN, null);
		 * intent.addCategory(Intent.CATEGORY_LAUNCHER); final ComponentName cn
		 * = new ComponentName("com.android.settings",
		 * "com.android.settings.TetherSettings"); intent.setComponent(cn);
		 * intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(
		 * intent);
		 */

		// startActivity(new
		// Intent("com.android.settings.wifi.hotspot.TetherWifiSettings"));

		overridePendingTransition(android.R.anim.fade_in,
				android.R.anim.fade_out);
	}

	public boolean isWifiDataEnabled() { // ??? work
		final ConnectivityManager connMgr = (ConnectivityManager) this
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		final android.net.NetworkInfo wifi = connMgr
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		// final android.net.NetworkInfo mobile =
		// connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		if (wifi.isAvailable()) {
			return true;
		}
		return false;
	}

	public boolean isMobileDataEnabled() {
		TelephonyManager telephonyManager = (TelephonyManager) this
				.getSystemService(Context.TELEPHONY_SERVICE);
		switch (telephonyManager.getDataState()) {
		case TelephonyManager.DATA_CONNECTED:
			return true;
		case TelephonyManager.DATA_DISCONNECTED:
			return false;
		}
		return false;
	}

	private final Object syncData = new Object();

	public void changeMobileData(boolean disable) {
		synchronized (syncData) {
			if (disable) {
				setMobileDataEnabled(this, false);
			} else {
				setMobileDataEnabled(this, true);
			}
		}
	}

	private int getDataState() {
		TelephonyManager telephonyManager = (TelephonyManager) this
				.getSystemService(Context.TELEPHONY_SERVICE);
		return telephonyManager.getDataState();
	}

	public void changeMobileData() {
		synchronized (syncData) {
			switch (getDataState()) {
			case TelephonyManager.DATA_CONNECTED:
				setMobileDataEnabled(this, false);
				break;
			case TelephonyManager.DATA_DISCONNECTED:
				setMobileDataEnabled(this, true);
				break;
			}
		}
	}

	private void setMobileDataEnabled(Context context, boolean enabled) {
		final ConnectivityManager conman = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		final Class conmanClass;
		try {
			conmanClass = Class.forName(conman.getClass().getName());
			final Field iConnectivityManagerField = conmanClass
					.getDeclaredField("mService");
			iConnectivityManagerField.setAccessible(true);
			final Object iConnectivityManager = iConnectivityManagerField
					.get(conman);
			final Class iConnectivityManagerClass = Class
					.forName(iConnectivityManager.getClass().getName());
			final Method setMobileDataEnabledMethod = iConnectivityManagerClass
					.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
			setMobileDataEnabledMethod.setAccessible(true);
			setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
	}

	private void setSettings(Context context, int value) {
		// Log.d("MdmLauncher", "setSettings: " + value);

		Settings.System.putInt(context.getContentResolver(),
				Settings.System.AIRPLANE_MODE_ON, value);
		Settings.Global.putInt(context.getContentResolver(),
				Settings.Global.AIRPLANE_MODE_ON, value);
	}

	public boolean isAirplaneModeOn(Context context) {
		Log.d("MdmMdmLauncher",
				"isAirplaneModeOn: "
						+ Settings.System.getInt(context.getContentResolver(),
								Settings.System.AIRPLANE_MODE_ON, 0)
						+ Settings.Global.getInt(context.getContentResolver(),
								Settings.Global.AIRPLANE_MODE_ON, 0));

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
			return Settings.System.getInt(context.getContentResolver(),
					Settings.System.AIRPLANE_MODE_ON, 0) != 0;
		} else {
			return Settings.Global.getInt(context.getContentResolver(),
					Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
		}
	}
}
