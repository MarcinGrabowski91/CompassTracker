package gitcode.eu.compasstracker.app.base;

import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

/**
 * Base activity class, parent of all app activities.
 */
public class BaseActivity extends AppCompatActivity {
    /**
     * Append fragment replacement action inside view container to new FragmentTransaction.
     *
     * @param containerViewId id of view container.
     * @param fragment        fragment to replace.
     * @param fragmentTag     tag of fragment, used to detect whether this fragment has been already
     *                        added
     * @return new fragmentTransaction (from v4 package). You must commit it (and you can append
     * something to it before).
     */
    protected FragmentTransaction replaceFragment(int containerViewId, BaseFragment fragment, String fragmentTag) {
        return getSupportFragmentManager().beginTransaction().replace(containerViewId, fragment, fragmentTag);
    }

    @SuppressWarnings("unchecked")
    protected <T extends BaseFragment> T findFragment(Class<T> fragment) {
        return (T) getSupportFragmentManager().findFragmentByTag(fragment.getName());
    }
}
