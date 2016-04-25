package gitcode.eu.compasstracker.ui.main;

import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;

import gitcode.eu.compasstracker.R;
import gitcode.eu.compasstracker.app.base.BaseActivity;
import gitcode.eu.compasstracker.ui.main.fragment.CompassFragment;

/**
 * Main activity of application
 */
public class MainActivity extends BaseActivity implements CompassFragment.CompassFragmentListener,
        SensorEventListener {
    /**
     * Time smoothing constant for low-pass filter 0 ≤ alpha ≤ 1 ; a smaller value basically means
     * more smoothing
     */
    static final float ALPHA = 0.15f;
    /**
     * Sensor manager
     */
    private SensorManager sensorManager;
    /**
     * Accelerometer sensor
     */
    private Sensor accelerometer;
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
     * Rotation matrix
     */
    private float[] rotationMatrix = new float[9];
    /**
     * Device orientation values
     */
    private float[] deviceOrientation = new float[3];
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
        targetLatitude = latitude;
        targetLongitude = longitude;
    }
    //endregion

    //region SensorEvent listener
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) {
            accelerometerValues = filterSensorValues(event.values.clone(), accelerometerValues);
        } else if (event.sensor == magnetometer) {
            magnetometerValues = filterSensorValues(event.values.clone(), magnetometerValues);
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
        GeomagneticField geomagneticField = getGeomagneticField();
        RotateAnimation arrowRotation = null;
        if (targetLatitude != null && targetLongitude != null) {
            Location targetLocation = getLastBestLocation();
            targetLocation.setLatitude(targetLatitude);
            targetLocation.setLongitude(targetLongitude);
            float azimuth = geomagneticField.getDeclination();
            float bearing = getLastBestLocation().bearingTo(targetLocation);
            float direction = azimuth - bearing;
            float newRotationDegree = -direction - 360 + (currentCompassDegree) % 360;
            arrowRotation = getRotateAnimation(currentArrowDegree, newRotationDegree);
            currentArrowDegree = newRotationDegree;
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

    /**
     * Get geomagnetic field based on device location
     *
     * @return geomagnetic field object based on device location
     */
    private GeomagneticField getGeomagneticField() {
        Location myLastLocation = getLastBestLocation();
        return new GeomagneticField(
                Double.valueOf(myLastLocation.getLatitude()).floatValue(),
                Double.valueOf(myLastLocation.getLongitude()).floatValue(),
                Double.valueOf(myLastLocation.getAltitude()).floatValue(),
                System.currentTimeMillis()
        );
    }

    /**
     * Filter sensor values using low pass filter
     *
     * @param input  sensor values
     * @param output filtered sensor values
     * @return filtered sensor values
     */
    private float[] filterSensorValues(float[] input, float[] output) {
        if (output != null) {
            for (int i = 0; i < input.length; i++) {
                output[i] = output[i] + ALPHA * (input[i] - output[i]);
            }
            return output;
        }
        return input;
    }

    /**
     * Get the last know location based on GPS or Internet (choose newer time value)
     *
     * @return the last know best location
     */
    private Location getLastBestLocation() {
        Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        long GPSLocationTime = 0;
        long NetLocationTime = 0;
        if (null != locationGPS) {
            GPSLocationTime = locationGPS.getTime();
        }
        if (null != locationNet) {
            NetLocationTime = locationNet.getTime();
        }

        return 0 < GPSLocationTime - NetLocationTime ? locationGPS : locationNet;
    }
}