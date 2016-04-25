package gitcode.eu.compasstracker.app.base;

import android.app.Activity;
import android.support.v4.app.Fragment;
/**
 * Base Fragment class, parent of all app fragments
 */
public class BaseFragment<T> extends Fragment {
    /**
     * Fragment listener
     */
    protected T listener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.listener = getFragmentListener(activity);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.listener = null;
    }

    /**
     * Get listener to communicate with parent component, default return activity cast to listener
     *
     * @param activity activity to cast on listener
     * @return listener to communicate with parent component
     */
    @SuppressWarnings("unchecked")
    private T getFragmentListener(Activity activity) {
        return (T) activity;
    }
}
