package sk.mizik.enviromon;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    public static final String TAG = "ENVIROMON";
    public static final String ID = "ID";

    private static final String COMMAND_CODE_TOGGLE_DISPLAY = "TD";
    private static final String COMMAND_CODE_TOGGLE_DEBUG_MODE = "TDM";
    private static final String COMMAND_CODE_TOGGLE_LAZY_MODE = "TLM";
    private static final String COMMAND_CODE_TOGGLE_SILENT_MODE = "TSM";
    private static final String COMMAND_CODE_SET_NOISE_THRESHOLD = "SNT";
    private static final String COMMAND_CODE_SET_LIGHT_THRESHOLD = "SLT";
    private static final String COMMAND_CODE_SET_TEMPERATURE_CORRECTION = "STC";
    private static final String SP_CODE_OUTDOOR_SENSOR_TEMPERATURE = "OST";
    private static final String SP_CODE_OUTDOOR_SENSOR_HUMIDITY = "OSH";
    private static final String OUTDOOR_SENSOR_DATE = "OSD";
    private static final String SP_CODE_LAST_SENSOR_DATA_UPLOAD_DATE = "USD";
    private static final int DTO_KEY_SOUND = 111;
    private static final int DTO_KEY_POWER = 112;
    private static final int DTO_KEY_BATTERY = 113;
    private static final int DTO_KEY_TEMPERATURE = 114;
    private static final int DTO_KEY_HUMIDITY = 115;

    private TextView dateView;
    private TextView timeView;
    private ImageView inDataLabelView;
    private ImageView inTempLabelView;
    private TextView inTempView;
    private ImageView inHumidityLabelView;
    private TextView inHumidityView;
    private ImageView outDataLabelView;
    private ImageView outTempLabelView;
    private TextView outTempView;
    private ImageView outHumidityLabelView;
    private TextView outHumidityView;
    private TextView deviceIdLabelView;
    private TextView deviceIdView;
    private TextView debugLabelView;
    private TextView debugView;
    private TextView silentLabelView;
    private TextView silentView;
    private TextView lazyLabelView;
    private TextView lazyView;
    private TextView correctionLabelView;
    private TextView correctionView;
    private TextView sltLabelView;
    private TextView sltView;
    private TextView lightLabelView;
    private TextView lightView;
    private TextView sntLabelView;
    private TextView sntView;
    private TextView soundLabelView;
    private TextView soundView;
    private TextView conLabelView;
    private TextView conView;
    private TextView verLabelView;
    private TextView verView;

    private boolean isDisplayManuallyTurnedOff = false;
    private boolean isDisplayOff = false;
    private boolean inUpgradeMode = false;

    private MediaRecorder noiseRecorder;
    private long lastNoiseRecordTimestamp;
    private Date serverDate = new Date(0);
    private Map<Integer, Float> sensorData;

    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm", Locale.US);
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd.MM", Locale.US);
    private final SimpleDateFormat dateHeaderFormatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
    private final SimpleDateFormat serverDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    private String serverUrl;
    public int appVersion;
    private boolean inLazyMode;
    private boolean inDebugMode;
    private boolean inSilentMode;
    private float temperatureCorrection;
    private int soundLevelThreshold;
    private int lightLevelThreshold;

    @Override
    @SuppressLint({"HardwareIds", "UnspecifiedRegisterReceiverFlag"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, TAG + " INIT");

        // CHECK PERMISSIONS
        int ok = PackageManager.PERMISSION_GRANTED;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.CAMERA) != ok &&
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) != ok &&
                checkSelfPermission(Manifest.permission.INTERNET) != ok &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != ok) {
            Toast.makeText(this, "PERMISSIONS NOT GRANTED", Toast.LENGTH_LONG).show();
            finish();
        }
        Log.i(TAG, "Permissions granted");

        // LOAD DEFAULT CONFIG
        Properties config;
        try {
            config = new Properties();
            config.load(getResources().openRawResource(R.raw.config));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config file", e);
        }
        serverUrl = config.getProperty("server.url");
        appVersion = Integer.parseInt(config.getProperty("app.version"));
        inDebugMode = Boolean.parseBoolean(config.getProperty("mode.debug"));
        inLazyMode = Boolean.parseBoolean(config.getProperty("mode.lazy"));
        inSilentMode = Boolean.parseBoolean(config.getProperty("mode.silent"));
        temperatureCorrection = Integer.parseInt(config.getProperty("correction.temperature"));
        soundLevelThreshold = Integer.parseInt(config.getProperty("threshold.sound"));
        lightLevelThreshold = Integer.parseInt(config.getProperty("threshold.light"));
        Log.i(TAG, "Config loaded:");
        Log.i(TAG, "Version: " + appVersion);
        Log.i(TAG, "Debug: " + inDebugMode);
        Log.i(TAG, "Silent: " + inSilentMode);
        Log.i(TAG, "Lazy: " + inLazyMode);

        // LOAD PERSISTED DATA
        SharedPreferences sp = getSharedPreferences(TAG, MODE_PRIVATE);
        isDisplayManuallyTurnedOff = sp.getBoolean(COMMAND_CODE_TOGGLE_DISPLAY, isDisplayManuallyTurnedOff);
        inDebugMode = sp.getBoolean(COMMAND_CODE_TOGGLE_DEBUG_MODE, inDebugMode);
        inLazyMode = sp.getBoolean(COMMAND_CODE_TOGGLE_LAZY_MODE, inLazyMode);
        inSilentMode = sp.getBoolean(COMMAND_CODE_TOGGLE_SILENT_MODE, inSilentMode);
        temperatureCorrection = sp.getFloat(COMMAND_CODE_SET_TEMPERATURE_CORRECTION, temperatureCorrection);
        soundLevelThreshold = sp.getInt(COMMAND_CODE_SET_NOISE_THRESHOLD, soundLevelThreshold);
        lightLevelThreshold = sp.getInt(COMMAND_CODE_SET_LIGHT_THRESHOLD, lightLevelThreshold);
        Log.i(TAG, "Shared preferences loaded");

        if (inSilentMode) {
            // GO SILENT TO SAVE BATTERY (OFFLINE + BLACK SCREEN)
            setContentView(R.layout.activity_none);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        } else {
            // INIT UI
            setContentView(R.layout.activity_main);
            // DATE & TIME
            dateView = findViewById(R.id.date);
            timeView = findViewById(R.id.time);
            // INDOOR DATA
            inDataLabelView = findViewById(R.id.in_data_label);
            inTempLabelView = findViewById(R.id.in_data_temp_label);
            inTempView = findViewById(R.id.in_data_temp);
            inHumidityLabelView = findViewById(R.id.in_data_humidity_label);
            inHumidityView = findViewById(R.id.in_data_humidity);
            // OUTDOOR DATA
            outDataLabelView = findViewById(R.id.out_data_label);
            outTempLabelView = findViewById(R.id.out_data_temp_label);
            outTempView = findViewById(R.id.out_data_temp);
            outHumidityLabelView = findViewById(R.id.out_data_humidity_label);
            outHumidityView = findViewById(R.id.out_data_humidity);
            // BOTTOM DEBUG LINE
            deviceIdLabelView = findViewById(R.id.device_id_label);
            deviceIdView = findViewById(R.id.device_id);
            debugLabelView = findViewById(R.id.debug_label);
            debugView = findViewById(R.id.debug);
            silentLabelView = findViewById(R.id.silent_label);
            silentView = findViewById(R.id.silent);
            lazyLabelView = findViewById(R.id.lazy_label);
            lazyView = findViewById(R.id.lazy);
            correctionLabelView = findViewById(R.id.correction_label);
            correctionView = findViewById(R.id.correction);
            sltLabelView = findViewById(R.id.slt_label);
            sltView = findViewById(R.id.slt);
            lightLabelView = findViewById(R.id.light_label);
            lightView = findViewById(R.id.light);
            sntLabelView = findViewById(R.id.snt_label);
            sntView = findViewById(R.id.snt);
            soundLabelView = findViewById(R.id.sound_label);
            soundView = findViewById(R.id.sound);
            conLabelView = findViewById(R.id.con_label);
            conView = findViewById(R.id.con);
            verLabelView = findViewById(R.id.ver_label);
            verView = findViewById(R.id.ver);

            // INIT SENSORS
            sensorData = new HashMap<>();
            int sensorDelay = SensorManager.SENSOR_DELAY_NORMAL;
            SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            List<Integer> sensors = Arrays.asList(
                    Sensor.TYPE_AMBIENT_TEMPERATURE,
                    Sensor.TYPE_RELATIVE_HUMIDITY,
                    Sensor.TYPE_PRESSURE,
                    Sensor.TYPE_LIGHT
            );
            for (Integer sensorType : sensors) {
                Sensor sensor = sensorManager.getDefaultSensor(sensorType);
                sensorManager.registerListener(this, sensor, sensorDelay);
            }

            // INIT NOISE DETECTION (TO TURN ON THE SCREEN ONLY IF THERE IS SOMEONE IN THE ROOM)
            initNoiseRecorder();

            // INIT MAIN APPLICATION LOOP
            if (!inSilentMode) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Calendar now = Calendar.getInstance();
                        switch (now.get(Calendar.SECOND)) {
                            case 0:
                                runEverySecond();
                                runEvery60Seconds();
                            case 5:
                            case 15:
                            case 25:
                            case 35:
                            case 45:
                            case 55:
                                runEvery10Seconds();
                            default:
                                runEverySecond();
                        }
                    }
                }, 1000, 1000);
            }
        }
        Log.i(TAG, "GUI initialized");
        Log.i(TAG, "Successfully started");
    }

    // OPEN "ID SELECT" DIALOG WHEN CLICKED ON "ID LABEL" IN THE DEBUG LINE
    public void openIdDialog(View v) {
        DialogFragment dialog = new IdDialog();
        dialog.show(getSupportFragmentManager(), "idDialog");
    }

    public String getId() {
        return getSharedPreferences(MainActivity.TAG, MODE_PRIVATE).getString(ID, "UNSET");
    }

    private void runEvery60Seconds() {
        getBatteryStatus();
        uploadSensorData();
        checkForApkUpgrade();
    }

    private void runEvery10Seconds() {
        if (!isDisplayOff) {
            refreshTemperatureAndHumidity();
            refreshDebugData();
        }
    }

    private void runEverySecond() {
        final long now = System.currentTimeMillis();
        // RESET LAST NOISE TIMESTAMP IF THERE IS NOISE
        int noiseValue = noiseRecorder.getMaxAmplitude();
        sensorData.put(DTO_KEY_SOUND, Integer.valueOf(noiseValue).floatValue());
        if (noiseValue > soundLevelThreshold) {
            lastNoiseRecordTimestamp = now;
        }
        // DECIDE DISPLAY STATE
        int v;
        if (isDisplayManuallyTurnedOff) {
            v = View.INVISIBLE;
        } else {
            v = (lastNoiseRecordTimestamp + 30000 > now) ? View.VISIBLE : View.INVISIBLE;
        }

        // UPDATE UI IF NEEDED
        if (!isDisplayOff || v == View.VISIBLE) {
            // IF DISPLAY WAS OFF, WE ARE GOING TO TURN IT ON NOW, SO WE NEED TO REFRESH DATA
            if (isDisplayOff) {
                refreshTemperatureAndHumidity();
                refreshDebugData();
            }
            // UPDATE DISPLAY STATE VARIABLE
            isDisplayOff = (v == View.INVISIBLE);
            // UPDATE UI
            runOnUiThread(() -> {
                // REFRESH TIME
                String time = timeFormatter.format(serverDate);
                String date = dateFormatter.format(serverDate);
                timeView.setText(time);
                dateView.setText(date);

                // REFRESH SOUND AND LIGHT
                String l = String.valueOf(sensorData.get(Sensor.TYPE_LIGHT));
                lightView.setText(l);
                String s = String.valueOf(sensorData.get(DTO_KEY_SOUND));
                soundView.setText(s);

                // SET DISPLAY DATA VISIBILITY BASED ON NOISE SENSOR DATA
                dateView.setVisibility(v);
                timeView.setVisibility(v);
                inDataLabelView.setVisibility(v);
                inTempLabelView.setVisibility(v);
                inTempView.setVisibility(v);
                inHumidityLabelView.setVisibility(v);
                inHumidityView.setVisibility(v);
                outDataLabelView.setVisibility(v);
                outTempLabelView.setVisibility(v);
                outTempView.setVisibility(v);
                outHumidityLabelView.setVisibility(v);
                outHumidityView.setVisibility(v);
                if (inDebugMode || v == View.INVISIBLE) {
                    deviceIdLabelView.setVisibility(v);
                    deviceIdView.setVisibility(v);
                    debugLabelView.setVisibility(v);
                    debugView.setVisibility(v);
                    silentLabelView.setVisibility(v);
                    silentView.setVisibility(v);
                    lazyLabelView.setVisibility(v);
                    lazyView.setVisibility(v);
                    correctionLabelView.setVisibility(v);
                    correctionView.setVisibility(v);
                    sltLabelView.setVisibility(v);
                    sltView.setVisibility(v);
                    lightLabelView.setVisibility(v);
                    lightView.setVisibility(v);
                    sntLabelView.setVisibility(v);
                    sntView.setVisibility(v);
                    soundLabelView.setVisibility(v);
                    soundView.setVisibility(v);
                    conLabelView.setVisibility(v);
                    conView.setVisibility(v);
                    verLabelView.setVisibility(v);
                    verView.setVisibility(v);
                }

                // SET DISPLAY BRIGHTNESS BASED ON LIGHT SENSOR DATA
                float newBrightness;
                if (v == View.INVISIBLE) {
                    // IF UI SHOULD BE OFF, DIM TO LOWEST POSSIBLE VALUE
                    newBrightness = 0.01f;
                } else {
                    Float light = sensorData.get(Sensor.TYPE_LIGHT);
                    newBrightness = (float) (light != null && light < lightLevelThreshold ? 0.01 : 1);
                }
                WindowManager.LayoutParams attributes = getWindow().getAttributes();
                if (attributes.screenBrightness != newBrightness) {
                    Log.i(TAG, "setting brightness to: " + newBrightness);
                    attributes.screenBrightness = newBrightness;
                    getWindow().setAttributes(attributes);
                    getWindow().addFlags(WindowManager.LayoutParams.FLAGS_CHANGED);
                }
            });
        }
    }

    private void initNoiseRecorder() {
        try {
            noiseRecorder = new MediaRecorder();
            noiseRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
            noiseRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            noiseRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            noiseRecorder.setOutputFile(Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath() + "/noise.3gp");
            noiseRecorder.prepare();
            noiseRecorder.start();
        } catch (IOException e) {
            Log.e(TAG, "failed to prepare audio recorder", e);
        }
    }

    private void getBatteryStatus() {
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        sensorData.put(DTO_KEY_BATTERY, Integer.valueOf(batLevel).floatValue());

        Intent i = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int status = i != null ? i.getIntExtra(BatteryManager.EXTRA_STATUS, -1) : -1;
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
        sensorData.put(DTO_KEY_POWER, isCharging ? 1F : 0F);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm != null ? cm.getActiveNetworkInfo() : null;
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void checkForApkUpgrade() {
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled() && !inUpgradeMode) {
            ((Runnable) () -> {
                HttpURLConnection c = null;
                try {
                    String deviceId = getId();
                    URL url = new URL(serverUrl + "/upgrade");
                    c = (HttpURLConnection) url.openConnection();
                    c.setRequestMethod("GET");
                    c.setConnectTimeout(2000);
                    c.setRequestProperty("Content-Type", "application/vnd.android.package-archive");
                    c.setRequestProperty("User-Agent", TAG + "/" + appVersion + " " + deviceId);
                    c.setDoInput(true);
                    //noinspection StatementWithEmptyBody
                    if (c.getResponseCode() == 204) {
                        // REQUEST SUCCESS, BUT NO UPGRADE AVAILABLE
                    } else if (c.getResponseCode() == 200) {
                        // REQUEST SUCCESS. UPGRADE IS AVAILABLE. SAVE IT TO CACHE FOLDER
                        File downloadDir;
                        if (Build.VERSION.SDK_INT >= 29) {
                            downloadDir = getCacheDir();
                        } else {
                            downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        }
                        File file = new File(downloadDir, "enviromon.apk");
                        try (BufferedInputStream bis = new BufferedInputStream(c.getInputStream()); FileOutputStream fos = new FileOutputStream(file)) {
                            byte[] buffer = new byte[1024];
                            int count;
                            while ((count = bis.read(buffer, 0, 1024)) != -1) {
                                fos.write(buffer, 0, count);
                            }
                        }
                        Log.w(TAG, "Upgrade is ready to install: " + " (" + file.length() / 1024 + ") " + file.getAbsolutePath());
                        // CALL ACTION_INSTALL_PACKAGE INTENT TO SHOW THE APP INSTALLATION PROMPT
                        Uri fileUri;
                        Intent intent;
                        if (Build.VERSION.SDK_INT >= 24) {
                            fileUri = FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID + ".provider", file);
                            intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                            intent.setData(fileUri);
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } else {
                            fileUri = Uri.fromFile(file);
                            intent = new Intent(Intent.ACTION_VIEW, fileUri);
                            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
                            intent.setDataAndType(fileUri, "application/vnd.android.package-archive");
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        }
                        startActivity(intent);
                        inUpgradeMode = true;
                    } else {
                        Log.e(TAG, "Wrong response code: " + c.getResponseCode());
                    }
                    // UPDATE SERVER DATE VARIABLE
                    String dateHeader = c.getHeaderField("Date");
                    if (dateHeader != null) {
                        try {
                            serverDate = dateHeaderFormatter.parse(dateHeader);
                        } catch (ParseException e) {
                            Log.w(TAG, "Failed to parse server date header: " + dateHeader);
                        }
                    }
                } catch (SocketTimeoutException ste) {
                    Log.w(TAG, "Server connection timeout");
                    serverDate = new Date();
                } catch (IOException | NullPointerException e) {
                    Log.e(TAG, "Error posting data: " + e.getMessage(), e);
                } finally {
                    if (c != null) {
                        c.disconnect();
                    }
                }
            }).run();
        }
    }

    private void uploadSensorData() {
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled()) {
            ((Runnable) () -> {
                // ASSEMBLE JSON PAYLOAD
                String deviceId = getId();
                JSONObject jsonParam = new JSONObject();
                try {
                    jsonParam.put("device", deviceId);
                    jsonParam.put("temperature", sensorData.get(DTO_KEY_TEMPERATURE));
                    jsonParam.put("pressure", sensorData.get(Sensor.TYPE_PRESSURE));
                    jsonParam.put("humidity", sensorData.get(DTO_KEY_HUMIDITY));
                    jsonParam.put("light", sensorData.get(Sensor.TYPE_LIGHT));
                    jsonParam.put("power", sensorData.get(DTO_KEY_POWER));
                    jsonParam.put("battery", sensorData.get(DTO_KEY_BATTERY));
                } catch (JSONException je) {
                    Log.e(TAG, "Error assembling json data: " + je.getMessage(), je);
                    return;
                }
                // UPLOAD
                HttpURLConnection c = null;
                try {
                    URL url = new URL(serverUrl + "/sensors");
                    c = (HttpURLConnection) url.openConnection();
                    c.setRequestMethod("POST");
                    c.setConnectTimeout(2000);
                    c.setRequestProperty("Content-Type", "application/json");
                    c.setRequestProperty("User-Agent", TAG + "/" + appVersion + " " + deviceId);
                    c.setDoOutput(true);
                    c.setDoInput(true);

                    DataOutputStream os = new DataOutputStream(c.getOutputStream());
                    os.writeBytes(jsonParam.toString());
                    os.flush();
                    os.close();

                    if (c.getResponseCode() != 200) {
                        Log.e(TAG, "Wrong response code: " + c.getResponseCode());
                    } else {
                        // SENSOR UPLOAD SUCCESS
                        // WE ARE GETTING BACK DATA FROM OUTDOOR SENSOR, TO SHOW ON DISPLAY
                        InputStreamReader isr = new InputStreamReader(c.getInputStream());
                        BufferedReader reader = new BufferedReader(isr);
                        StringBuilder sb = new StringBuilder();

                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                        // SAVE TO SHARED PREFS FOR LATER DISPLAY
                        JSONObject values = new JSONArray(sb.toString()).getJSONObject(0);
                        SharedPreferences.Editor sp = getSharedPreferences(TAG, MODE_PRIVATE).edit();
                        sp.putLong(SP_CODE_LAST_SENSOR_DATA_UPLOAD_DATE, System.currentTimeMillis());
                        sp.putFloat(SP_CODE_OUTDOOR_SENSOR_TEMPERATURE, (float) values.getDouble("temperature"));
                        sp.putInt(SP_CODE_OUTDOOR_SENSOR_HUMIDITY, values.getInt("humidity"));
                        String cd = values.getString("creation_date");
                        try {
                            sp.putLong(OUTDOOR_SENSOR_DATE, serverDateFormatter.parse(cd).getTime());
                        } catch (ParseException e) {
                            Log.w(TAG, "Failed to parse outdoor sensor creation date: " + cd);
                        }
                        sp.apply();
                    }
                } catch (SocketTimeoutException ste) {
                    Log.w(TAG, "Server connection timeout");
                } catch (IOException | NullPointerException e) {
                    Log.e(TAG, "Error posting data: " + e.getMessage(), e);
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing response: " + e.getMessage(), e);
                } finally {
                    if (c != null) {
                        c.disconnect();
                    }
                }
            }).run();
        }
    }

    @SuppressLint("SetTextI18n")
    private void refreshTemperatureAndHumidity() {
        runOnUiThread(() -> {
            SharedPreferences sp = getSharedPreferences(TAG, MODE_PRIVATE);
            // INDOOR
            inTempView.setText(String.valueOf(roundToHalf(sensorData.get(DTO_KEY_TEMPERATURE))));
            inHumidityView.setText(String.valueOf(sensorData.get(DTO_KEY_HUMIDITY)));
            long lastUploadDate = sp.getLong(SP_CODE_LAST_SENSOR_DATA_UPLOAD_DATE, 0);
            if (System.currentTimeMillis() > (lastUploadDate + 200 * 1000)) {
                inDataLabelView.setColorFilter(Color.parseColor("#81ABDB"));
            } else {
                inDataLabelView.setColorFilter(Color.parseColor("#FFFFFF"));
            }
            // OUTDOOR
            outTempView.setText(String.valueOf(roundToHalf(sp.getFloat(SP_CODE_OUTDOOR_SENSOR_TEMPERATURE, 0))));
            outHumidityView.setText(sp.getInt(SP_CODE_OUTDOOR_SENSOR_HUMIDITY, 0) + ".0");
            long outDate = sp.getLong(OUTDOOR_SENSOR_DATE, 0);
            if (System.currentTimeMillis() > (outDate + 3600 * 1000)) {
                outDataLabelView.setColorFilter(Color.parseColor("#81ABDB"));
            } else {
                outDataLabelView.setColorFilter(Color.parseColor("#FFFFFF"));
            }
        });
    }

    private double roundToHalf(Float d) {
        return d == null ? 0.0 : Math.round(d * 2) / 2.0;
    }

    private void refreshDebugData() {
        runOnUiThread(() -> {
            deviceIdView.setText(String.valueOf(getId()));
            debugView.setText(String.valueOf(inDebugMode));
            silentView.setText(String.valueOf(inSilentMode));
            lazyView.setText(String.valueOf(inLazyMode));
            correctionView.setText(String.valueOf(temperatureCorrection));
            sltView.setText(String.valueOf(lightLevelThreshold));
            sntView.setText(String.valueOf(soundLevelThreshold));
            conView.setText(String.valueOf(isNetworkAvailable()));
            verView.setText(String.valueOf(appVersion));
        });
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onSensorChanged(SensorEvent event) {
        float reading = (float) (Math.round(event.values[0] * 10.0) / 10.0);
        int type = event.sensor.getType();
        switch (type) {
            case Sensor.TYPE_LIGHT:
            case Sensor.TYPE_PRESSURE:
                sensorData.put(type, reading);
                break;
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                sensorData.put(Sensor.TYPE_AMBIENT_TEMPERATURE, reading);
                sensorData.put(DTO_KEY_TEMPERATURE, reading + temperatureCorrection);
                if (sensorData.get(Sensor.TYPE_RELATIVE_HUMIDITY) != null) {
                    sensorData.put(
                            DTO_KEY_HUMIDITY,
                            calculateRelativeHumidityChange(
                                    reading,
                                    reading + temperatureCorrection,
                                    sensorData.get(Sensor.TYPE_RELATIVE_HUMIDITY)
                            )
                    );
                }
                break;
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                sensorData.put(Sensor.TYPE_RELATIVE_HUMIDITY, reading);
                if (sensorData.get(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {
                    sensorData.put(
                            DTO_KEY_HUMIDITY,
                            calculateRelativeHumidityChange(
                                    sensorData.get(Sensor.TYPE_AMBIENT_TEMPERATURE),
                                    sensorData.get(DTO_KEY_TEMPERATURE),
                                    reading
                            )
                    );
                }
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // DO NOTHING
    }

    /**
     * The Magnus formula relates the saturation vapour pressure and dew point.
     * At a temperature T (in °C), the saturation vapour pressure EW (in hPa) over liquid water
     * For the range from –45°C to 60°C, Magnus parameters are given by
     * α =6.112 hPa,
     * β =17.62 and
     * λ =243.12 °C.
     */
    private double magnus(double t) {
        return 6.112 * Math.exp(17.62 * t / (243.12 + t));
    }

    private float calculateRelativeHumidityChange(float t1, float t2, float rh1) {
        double m1 = magnus(t1);
        double e1 = rh1 * m1 / 100;
        double m2 = magnus(t2);
        return Math.min(Math.round(e1 / m2 * 100), 100);
    }
}