package gitcode.eu.compasstracker.ui.main;

import android.content.Intent;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Toast;

import gitcode.eu.compasstracker.R;
import gitcode.eu.compasstracker.app.base.BaseActivity;
import gitcode.eu.compasstracker.ui.main.fragment.CompassFragment;
import gitcode.eu.compasstracker.utils.LocalizationUtils;

/**
 * Main activity of application
 */
public class MainActivity extends BaseActivity implements CompassFragment.CompassFragmentListener,
        SensorEventListener {
    /**
     * Sensor manager
     */
    private SensorManager sensorManager;
    /**
     * Accelerometer sensor
     */
    private Sensor accelerometer;;
    /**
     * Magnometer sensor
     */
    private Sensor magnetometer;
    /**
     * Currently displayed compass degree
     */
    private float currentCompassDegree = 0f;
    /**
     * Currently displayed target arrow degree
     */
    private float currentArrowDegree = 0f;

    /**
     * Current accelerometer values
     */
    private float[] accelerometerValues;
    /**
     * Current magnetometer values
     */
    private float[] magnetometerValues;
    /**
     * Location manager using for calculate user and target position
     */
    LocationManager locationManager;
    /**
     * Target location latitude
     */
    private Float targetLatitude;
    /**
     * Target location longitude
     */
    private Float targetLongitude;
    /**
     * Indicates if gps localization is enabled
     */
    private boolean isLocationGPSEnabled = false;
    /**
     * Indicates if network localization is enabled
     */
    private boolean isLocationNetworkEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.base_activity);
        replaceFragment(R.id.base_activity_fragment_container, new CompassFragment(), CompassFragment.TAG).commit();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this, accelerometer);
        sensorManager.unregisterListener(this, magnetometer);
    }

    //region CompassFragment listener
    @Override
    public void onSetBtnClicked(float latitude, float longitude) {
        if (isLocationGPSEnabled || isLocationNetworkEnabled) {
            targetLatitude = latitude;
            targetLongitude = longitude;
        } else {
            Toast.makeText(this, getString(R.string.main_activity_cannot_find_your_localization)
                    , Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }
    }
    //endregion

    //region SensorEvent listener
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) {
            accelerometerValues = LocalizationUtils.filterSensorValues(event.values.clone(), accelerometerValues);
        } else if (event.sensor == magnetometer) {
            magnetometerValues = LocalizationUtils.filterSensorValues(event.values.clone(), magnetometerValues);
        }
        if (accelerometerValues != null && magnetometerValues != null) {
            RotateAnimation compassRotation = getCompassRotateAnimation();
            RotateAnimation arrowRotation = getTargetRotateAnimation();
            CompassFragment compassFragment = findFragment(CompassFragment.class);
            if (compassFragment != null) {
                compassFragment.rotateCompass(compassRotation);
                if (arrowRotation != null) {
                    compassFragment.rotateDestinationArrow(arrowRotation);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    //endregion

    /**
     * Get rotate animation for compass
     *
     * @return rotate animation for compass
     */
    private RotateAnimation getCompassRotateAnimation() {
        float[] rotationMatrix = new float[9];
        float[] deviceOrientation = new float[3];
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerValues, magnetometerValues);
        SensorManager.getOrientation(rotationMatrix, deviceOrientation);
        float azimuthInRadians = deviceOrientation[0];
        float azimuthInDegrees = (float) (Math.toDegrees(azimuthInRadians) + 360) % 360;
        RotateAnimation compassRotation = getRotateAnimation(currentCompassDegree,
                -azimuthInDegrees);
        currentCompassDegree = -azimuthInDegrees;
        return compassRotation;
    }

    /**
     * Get rotate animation for target arrow
     *
     * @return rotate animation for target arrow
     */
    private RotateAnimation getTargetRotateAnimation() {
        RotateAnimation arrowRotation = null;
        isLocationGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isLocationNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        CompassFragment compassFragment = findFragment(CompassFragment.class);
        if (compassFragment != null) {
            if (!isLocationGPSEnabled && !isLocationNetworkEnabled) {
                compassFragment.setLocalizationErrorVisibility(true);
            } else {
                compassFragment.setLocalizationErrorVisibility(false);
            }
        }
        if (targetLatitude != null && targetLongitude != null) {
            Location lastKnownLocation = LocalizationUtils.getLastBestLocation(locationManager,
                    isLocationGPSEnabled, isLocationNetworkEnabled);
            if (lastKnownLocation != null) {
                GeomagneticField geomagneticField = LocalizationUtils.getGeomagneticField(lastKnownLocation);
                Location targetLocation = new Location(lastKnownLocation);
                targetLocation.setLatitude(targetLatitude);
                targetLocation.setLongitude(targetLongitude);
                float azimuth = geomagneticField.getDeclination();
                float bearing = lastKnownLocation.bearingTo(targetLocation);
                float direction = azimuth - bearing;
                float newRotationDegree = -direction - 360 + (currentCompassDegree) % 360;
                arrowRotation = getRotateAnimation(currentArrowDegree, newRotationDegree);
                currentArrowDegree = newRotationDegree;
            }
        }
        return arrowRotation;
    }

    /**
     * Get rotate animation
     *
     * @param currentRotationDegree current rotation degree
     * @param newRotationDegree     new rotation degree
     * @return rotate animation
     */
    private RotateAnimation getRotateAnimation(float currentRotationDegree, float newRotationDegree) {
        RotateAnimation rotateAnimation = new RotateAnimation(
                currentRotationDegree,
                newRotationDegree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);
        rotateAnimation.setDuration(250);
        rotateAnimation.setFillAfter(true);
        return rotateAnimation;
    }
}