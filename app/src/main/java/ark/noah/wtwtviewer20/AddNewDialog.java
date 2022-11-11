package ark.noah.wtwtviewer20;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Objects;

public class AddNewDialog extends DialogFragment {
    DialogInterface dialogInterface;
    public static String TAG = "AddNewDialog";

    private LinkValidater linkValidater;

    public AddNewDialog(DialogInterface dialogInterface) {
        this.dialogInterface = dialogInterface;
        linkValidater = LinkValidater.Instance != null ? LinkValidater.Instance : new LinkValidater();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.dialog_addnew, null);

        FloatingActionButton fab_proceed = view.findViewById(R.id.fab_dialog_proceed);
        MaterialButton btn_fromweb = view.findViewById(R.id.btn_dialog_fromWeb);
        EditText etxt_url = view.findViewById(R.id.etxt_dialog_url);
        TextView txt_title = view.findViewById(R.id.txt_dialog_desc_addnew);

        txt_title.setText(R.string.dialog_desc_addnew);
        etxt_url.setHint(R.string.dialog_txt_url);
        btn_fromweb.setText(R.string.dialog_from_web);
        btn_fromweb.setOnClickListener(v -> {
            dialogInterface.onWebButtonClicked(v);
            this.dismiss();
        });
        fab_proceed.setOnClickListener(v -> {
            String urlInString = Objects.requireNonNull(etxt_url).getText().toString();
            if(linkValidater.isLinkValidEpisodeList(urlInString)) {
                dialogInterface.onProceedButtonClicked(v, urlInString);
                this.dismiss();
            } else {
                Toast.makeText(requireContext(), "Invalid url", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        DeviceSizeGetter dsg = DeviceSizeGetter.Instance;
        WindowManager.LayoutParams params = Objects.requireNonNull(getDialog()).getWindow().getAttributes();
        int portraitWidth = Math.min(dsg.getDeviceWidth(), dsg.getDeviceHeight());
        int dimenWidth = getResources().getDimensionPixelSize(R.dimen.dialog_width);
        int dimenTargetDeviceWidth = getResources().getDimensionPixelSize(R.dimen.dev_target_device_width);
        params.width = (int)(((float)dimenWidth/(float) dimenTargetDeviceWidth) * portraitWidth);
        Objects.requireNonNull(getDialog()).getWindow().setAttributes(params);
    }

    interface DialogInterface {
        void onProceedButtonClicked(View v, String url);
        void onWebButtonClicked(View v);
    }
}
