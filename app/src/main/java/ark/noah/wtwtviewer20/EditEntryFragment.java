package ark.noah.wtwtviewer20;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ark.noah.wtwtviewer20.databinding.FragmentEditEntryBinding;

public class EditEntryFragment extends Fragment {

    private EditEntryViewModel mViewModel;
    private FragmentEditEntryBinding binding;

    public static EditEntryFragment newInstance() {
        return new EditEntryFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        mViewModel = new ViewModelProvider(this).get(EditEntryViewModel.class);
        binding = FragmentEditEntryBinding.inflate(inflater, container, false);

        View view = binding.getRoot();

        return view;
    }
}