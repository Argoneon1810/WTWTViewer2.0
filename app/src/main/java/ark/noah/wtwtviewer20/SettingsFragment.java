package ark.noah.wtwtviewer20;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat implements ExecutorRunner.Callback<Void> {
    BackupRestoreDBManager backupRestoreDBManager;
    DBHelper dbHelper;

    ActivityResultLauncher<Intent> dumpSaveActivityResultLauncher;
    ActivityResultLauncher<Intent> restoreDBActivityResultLauncher;
    ActivityResultLauncher<Intent> restoreOldDBActivityResultLauncher;

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

                if(!getMimeType(requireContext(), uri).equals("xml")) {
                    Toast.makeText(requireContext().getApplicationContext(), requireContext().getText(R.string.txt_invalid_file_type), Toast.LENGTH_SHORT).show();
                    return;
                }

                if(backupRestoreDBManager.restore(uri))
                    Toast.makeText(requireContext().getApplicationContext(), requireContext().getText(R.string.txt_restore_in_progress), Toast.LENGTH_SHORT).show();
            }
        });

        restoreOldDBActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if(result.getResultCode()==Activity.RESULT_OK) {
                Intent intent = result.getData();
                if(intent == null) return;

                Uri uri = intent.getData();
                if(uri == null) return;

                if(!getMimeType(requireContext(), uri).equals("txt")) {
                    Toast.makeText(requireContext().getApplicationContext(), requireContext().getText(R.string.txt_invalid_file_type), Toast.LENGTH_SHORT).show();
                    return;
                }

                if(backupRestoreDBManager.restoreOld(uri))
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
        Preference btn_restore_old = findPreference(getString(R.string.pref_btn_key_restore_old));
        Preference btn_rebuild_thumbnail = findPreference(getString(R.string.pref_btn_key_rebuild_thumbnail));

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
            new MaterialAlertDialogBuilder(requireContext()).setTitle(getString(R.string.dialog_title_warning))
                    .setMessage(getString(R.string.dialog_body_warning))
                    .setPositiveButton(getString(R.string.dialog_btn_cancel_positive), ((dialogInterface, i) -> dialogInterface.cancel()))
                    .setNegativeButton(getString(R.string.dialog_btn_clear_negative), (dialogInterface, i) -> {
                        dbHelper.factoryReset();
                        dialogInterface.dismiss();
                    })
                    .create().show();
            return true;
        });

        Objects.requireNonNull(btn_restore_old).setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");

            restoreOldDBActivityResultLauncher.launch(intent);
            return true;
        });

        Objects.requireNonNull(btn_rebuild_thumbnail).setOnPreferenceClickListener(preference -> {
            Toast.makeText(requireContext().getApplicationContext(), getString(R.string.txt_rebuild_thumnail_started), Toast.LENGTH_SHORT).show();
            new ExecutorRunner().execute(() -> {
                ArrayList<ToonsContainer> tcs = dbHelper.getAllToonsFiltered(ToonsContainer.ReleaseDay.ALL,true, true, false, false);
                Document doc;
                for(ToonsContainer tc : tcs) {
                    try {
                        doc = Jsoup.connect(LinkGetter.Instance.getEntryPoint() + tc.toonType + "1?toon=" + tc.toonID).get();
                        tc.thumbnailURL = doc.select("div.img-box").select("img").attr("src");
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                    dbHelper.editToonContent(tc);
                }
                return null;
            }, this);

            return true;
        });
    }

    public static String getMimeType(Context context, Uri uri) {
        String extension;

        //Check uri format to avoid null
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            //If scheme is a content
            final MimeTypeMap mime = MimeTypeMap.getSingleton();
            extension = mime.getExtensionFromMimeType(context.getContentResolver().getType(uri));
        } else {
            //If scheme is a File
            //This will replace white spaces with %20 and also other special characters. This will avoid returning null values on file name with spaces and special characters.
            extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(uri.getPath())).toString());
        }

        return extension;
    }

    @Override
    public void onComplete(Void result) {
        Toast.makeText(requireContext().getApplicationContext(), getString(R.string.txt_rebuild_thumnail_complete), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError(Exception e) {

    }
}