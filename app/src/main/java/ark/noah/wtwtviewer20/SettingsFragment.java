package ark.noah.wtwtviewer20;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, getString(R.string.pref_key_root));
        Preference btn_backup = findPreference(getString(R.string.pref_key_btn_backup));
        Preference btn_restore = findPreference(getString(R.string.pref_key_btn_restore));
        Preference btn_clear_all = findPreference(getString(R.string.pref_key_btn_clear_all));
        Objects.requireNonNull(btn_backup).setOnPreferenceClickListener(preference -> {
            //code for what you want it to do
            return true;
        });
        Objects.requireNonNull(btn_restore).setOnPreferenceClickListener(preference -> {
            //code for what you want it to do
            return true;
        });
        Objects.requireNonNull(btn_clear_all).setOnPreferenceClickListener(preference -> {
            //code for what you want it to do
            return true;
        });
    }
}