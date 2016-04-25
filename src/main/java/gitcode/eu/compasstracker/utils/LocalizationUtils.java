package gitcode.eu.compasstracker.utils;

import android.hardware.GeomagneticField;
import android.location.Location;
import android.location.LocationManager;
/**
 * Helper class for operations on localization
 */
public final class LocalizationUtils {
    /**
     * Time smoothing constant for low-pass filter 0 ≤ alpha ≤ 1 ; a smaller value basically means
     * more smoothing
     */
    static final float ALPHA = 0.15f;

    /**
     * Get geomagnetic field based on device location
     *
     * @return geomagnetic field object based on device location
     */
    public static GeomagneticField getGeomagneticField(Location location) {
        return new GeomagneticField(
                Double.valueOf(location.getLatitude()).floatValue(),
                Double.valueOf(location.getLongitude()).floatValue(),
                Double.valueOf(location.getAltitude()).floatValue(),
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
    public static float[] filterSensorValues(float[] input, float[] output) {
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
    public static Location getLastBestLocation(LocationManager locationManager, boolean isLocationGPSEnabled,
                                               boolean isLocationNetworkEnabled) {
        if (!isLocationGPSEnabled && !isLocationNetworkEnabled) {
            return null;
        } else {
            Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location locationNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            long GPSLocationTime = 0;
            long NetworkLocationTime = 0;
            if (null != locationGPS) {
                GPSLocationTime = locationGPS.getTime();
            }
            if (null != locationNetwork) {
                NetworkLocationTime = locationNetwork.getTime();
            }
            return 0 < GPSLocationTime - NetworkLocationTime ? locationGPS : locationNetwork;
        }
    }
}
