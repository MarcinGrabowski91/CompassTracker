package gitcode.eu.compasstracker.ui.main.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import gitcode.eu.compasstracker.R;
import gitcode.eu.compasstracker.app.base.BaseFragment;
import gitcode.eu.compasstracker.utils.StringUtils;

/**
 * Compass fragment.
 */
public class CompassFragment extends BaseFragment<CompassFragment.CompassFragmentListener> {
    public static final String TAG = CompassFragment.class.getName();

    @Bind(R.id.compass_fragment_compass_img)
    ImageView compassImg;

    @Bind(R.id.compass_fragment_arrow_img)
    ImageView arrowImg;

    @Bind(R.id.compass_fragment_latitude_edit)
    EditText latitudeEdit;

    @Bind(R.id.compass_fragment_longitude_edit)
    EditText longitudeEdit;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.compass_fragment, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @OnClick(R.id.compass_fragment_set_btn)
    void onSetBtnClick() {
        String latitude = latitudeEdit.getText().toString();
        String longitude = longitudeEdit.getText().toString();
        if (!StringUtils.isNullOrEmpty(latitude) && !StringUtils.isNullOrEmpty(longitude)) {
            float latitudeValue = Float.parseFloat(latitude);
            float longitudeValue = Float.parseFloat(longitude);
            if (latitudeValue < 90 && latitudeValue > -90
                    && longitudeValue < 180 && longitudeValue > -180) {
                arrowImg.setVisibility(View.VISIBLE);
                listener.onSetBtnClicked(Float.parseFloat(latitude), Float.parseFloat(longitude));
            } else {
                Toast.makeText(getActivity(), getString(R.string.main_activity_incorrect_target_values), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getActivity(), getString(R.string.main_activity_enter_target_value), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Perform rotation on compass image
     *
     * @param rotateAnimation rotate animation to perform
     */
    public void rotateCompass(RotateAnimation rotateAnimation) {
        compassImg.startAnimation(rotateAnimation);
    }

    /**
     * Perform rotation on arrow image
     *
     * @param rotateAnimation rotate animation to perform
     */
    public void rotateDestinationArrow(RotateAnimation rotateAnimation) {
        arrowImg.startAnimation(rotateAnimation);
    }

    /**
     * Compass fragment listener
     */
    public interface CompassFragmentListener {
        /**
         * Action invoked after set button click
         *
         * @param latitude  target latitude
         * @param longitude target longitude
         */
        void onSetBtnClicked(float latitude, float longitude);
    }
}