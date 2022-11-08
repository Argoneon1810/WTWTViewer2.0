package ark.noah.wtwtviewer20;

import static android.content.Context.MODE_PRIVATE;

import androidx.lifecycle.ViewModelProvider;

import android.content.SharedPreferences;
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
import android.widget.CompoundButton;

import ark.noah.wtwtviewer20.databinding.FragmentByDayListBinding;
import ark.noah.wtwtviewer20.databinding.FragmentCompletedListBinding;

public class CompletedListFragment extends Fragment implements AddNewDialog.DialogInterface {
    public static final int INDEX = 2;

    private CompletedListViewModel mViewModel;
    private FragmentCompletedListBinding binding;

    private DBHelper dbHelper;
    private SharedPreferences sharedPreferences;

    private AddNewDialog addNewDialogFragment;

    private boolean isDebug = true;

    public static CompletedListFragment newInstance() {
        return new CompletedListFragment();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        mViewModel = new ViewModelProvider(this).get(CompletedListViewModel.class);
        binding = FragmentCompletedListBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        isDebug = MainActivity.Instance.isDebug;

        dbHelper = new DBHelper(requireContext().getApplicationContext());

        sharedPreferences = requireActivity().getApplicationContext().getSharedPreferences(getString(R.string.shared_pref_key), MODE_PRIVATE);

        binding.sCompletedShowhidden.setChecked(sharedPreferences.getBoolean(getString(R.string.shared_pref_showcompleted_key), false));

        loadRecyclerItemFiltered();

        addNewDialogFragment = new AddNewDialog(this);

        binding.fabCompleted.setOnClickListener(v -> addNewDialogFragment.show(getParentFragmentManager(), AddNewDialog.TAG));

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if(isDebug) Log.i("DebugLog","onStart() of CompletedListFragment");

        binding.sCompletedShowhidden.setChecked(sharedPreferences.getBoolean(getString(R.string.shared_pref_showhidden_key), false));
        if(isDebug) Log.i("DebugLog","SwitchCompat state of CompletedListFragment is recovered");

        binding.sCompletedShowhidden.setOnCheckedChangeListener(this::onCheckedChangedShowHidden);
        if(isDebug) Log.i("DebugLog","OnCheckedChangeListener of CompletedListFragment is reloaded");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(isDebug) Log.i("DebugLog","onStop() of ByDayListFragment");

        binding.sCompletedShowhidden.setOnCheckedChangeListener(null);
        if(isDebug) Log.i("DebugLog","OnCheckedChangeListener of CompletedListFragment is unloaded");
    }

    private void loadRecyclerItemFiltered() {
        binding.completedRec.setAdapter(new ToonsAdapter(dbHelper.getAllToonsFiltered(
                ToonsContainer.ReleaseDay.ALL,
                sharedPreferences.getBoolean(getString(R.string.shared_pref_showhidden_key), false),
                true
        )));
    }

    private void onCheckedChangedShowHidden(CompoundButton cb, boolean b) {
        if(isDebug) Log.i("DebugLog","Detected change in switch show hidden of CompletedListFragment");

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(getString(R.string.shared_pref_showhidden_key), b);
        editor.apply();

        loadRecyclerItemFiltered();
    }

    @Override
    public void onProceedButtonClicked(View v, String validatedUrl) {
        NavController navController = Navigation.findNavController(this.requireView());
        Bundle bundle = new Bundle();
        bundle.putInt(getString(R.string.bundle_from), INDEX);
        bundle.putString(getString(R.string.bundle_link), validatedUrl);
        navController.navigate(R.id.action_completedListFragment_to_reviewEntryFragment, bundle);
    }

    @Override
    public void onWebButtonClicked(View v) {
        NavController navController = Navigation.findNavController(this.requireView());
        Bundle bundle = new Bundle();
        bundle.putInt(getString(R.string.bundle_from), INDEX);
        navController.navigate(R.id.action_completedListFragment_to_addByWebFragment, bundle);
    }
}