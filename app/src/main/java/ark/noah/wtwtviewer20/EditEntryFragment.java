package ark.noah.wtwtviewer20;

import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import ark.noah.wtwtviewer20.databinding.FragmentEditEntryBinding;

public class EditEntryFragment extends Fragment {

    private FragmentEditEntryBinding binding;

    private DBHelper dbHelper;

    public static EditEntryFragment newInstance() {
        return new EditEntryFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentEditEntryBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        dbHelper = new DBHelper(requireContext().getApplicationContext());

        Bundle receivedBundle = requireArguments();
        ToonsContainer toonsContainer = receivedBundle.getParcelable(getString(R.string.bundle_toons));
        int cameFrom = receivedBundle.getInt(getString(R.string.bundle_from));

        ReviewEntryAdapter adapter = new ReviewEntryAdapter(new ArrayList<>(Collections.singletonList(toonsContainer)));
        binding.recEdit.setAdapter(adapter);

        EditEntryFragment fragment = this;
        binding.fabEdit.setOnClickListener((v) -> {
            Context context = v.getContext().getApplicationContext();
            ReviewEntryAdapter.ViewHolder vh = (ReviewEntryAdapter.ViewHolder) binding.recEdit.findViewHolderForAdapterPosition(0); //always 1 entry
            if(vh == null) {
                Toast.makeText(context, context.getString(R.string.txt_something_went_wrong), Toast.LENGTH_SHORT).show();
                return;
            }
            if(vh.ToonName.getText().toString().equals("")) {
                Toast.makeText(context, context.getString(R.string.txt_invalid_title), Toast.LENGTH_SHORT).show();
                return;
            }
            dbHelper.editToonContent(adapter.mList.get(0));                                                                         //always 1 entry
            NavController navController = Navigation.findNavController(fragment.requireView());
            Bundle bundle = new Bundle();
            bundle.putByte(getString(R.string.bundle_junk), (byte) 0);
            //이 위는 정상 확인
            switch(cameFrom) {
                default:
                case 0: //to alllist
                    navController.navigate(R.id.action_editEntryFragment_to_allListFragment, bundle);
                    break;
                case 1: //to byday
                    navController.navigate(R.id.action_editEntryFragment_to_byDayListFragment, bundle);
                    break;
                case 2: //to completed
                    navController.navigate(R.id.action_editEntryFragment_to_completedListFragment, bundle);
                    break;
            }
        });

        return view;
    }
}