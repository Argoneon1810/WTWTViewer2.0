package ark.noah.wtwtviewer20;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat {
    BackupRestoreDBManager backupRestoreDBManager;
    DBHelper dbHelper;

    ActivityResultLauncher<Intent> dumpSaveActivityResultLauncher;
    ActivityResultLauncher<Intent> restoreDBActivityResultLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        backupRestoreDBManager = new BackupRestoreDBManager(requireContext().getApplicationContext());
        dbHelper = new DBHelper(requireContext().getApplicationContext());

        dumpSaveActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if(result.getResultCode() == Activity.RESULT_OK) {
                Intent intent = result.getData();
                if(intent == null) return;

                Uri uri = intent.getData();
                if(uri == null) return;

                if(backupRestoreDBManager.backup(uri,
                        dbHelper.getAllToonsFiltered(
                                ToonsContainer.ReleaseDay.ALL,
                                true,
                                true,
                                false,
                                false
                        )
                )) Toast.makeText(requireContext().getApplicationContext(), requireContext().getText(R.string.txt_backup_in_progress), Toast.LENGTH_SHORT).show();
            }
        });

        restoreDBActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if(result.getResultCode()==Activity.RESULT_OK) {
                Intent intent = result.getData();
                if(intent == null) return;

                Uri uri = intent.getData();
                if(uri == null) return;

                if(backupRestoreDBManager.restore(uri))
                    Toast.makeText(requireContext().getApplicationContext(), requireContext().getText(R.string.txt_restore_in_progress), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, getString(R.string.pref_key_root));
        Preference btn_backup = findPreference(getString(R.string.pref_key_btn_backup));
        Preference btn_restore = findPreference(getString(R.string.pref_key_btn_restore));
        Preference btn_clear_all = findPreference(getString(R.string.pref_key_btn_clear_all));
        Objects.requireNonNull(btn_backup).setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/xml");
            intent.putExtra(Intent.EXTRA_TITLE, "databaseDump.xml");

            dumpSaveActivityResultLauncher.launch(intent);
            return true;
        });
        Objects.requireNonNull(btn_restore).setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/xml");

            restoreDBActivityResultLauncher.launch(intent);
            return true;
        });
        Objects.requireNonNull(btn_clear_all).setOnPreferenceClickListener(preference -> {
            //code for what you want it to do
            dbHelper.factoryReset();
            return true;
        });
    }
}