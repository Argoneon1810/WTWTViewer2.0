package ark.noah.wtwtviewer20;

import static android.content.Context.MODE_PRIVATE;

import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import java.util.Objects;

import ark.noah.wtwtviewer20.databinding.FragmentAllListBinding;

public class AllListFragment extends Fragment implements AddNewDialog.DialogInterface {
    public static final int INDEX = 0;

    private AllListViewModel mViewModel;
    private FragmentAllListBinding binding;

    private DBHelper dbHelper;
    private SharedPreferences sharedPreferences;

    private boolean isDebug = true;

    private AddNewDialog addNewDialogFragment;

    public static AllListFragment newInstance() {
        return new AllListFragment();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        mViewModel = new ViewModelProvider(this).get(AllListViewModel.class);
        binding = FragmentAllListBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        isDebug = MainActivity.Instance.isDebug;

        dbHelper = new DBHelper(requireContext().getApplicationContext());

        sharedPreferences = requireActivity().getApplicationContext().getSharedPreferences(getString(R.string.shared_pref_key), MODE_PRIVATE);

        binding.sAlllistShowhidden.setChecked(sharedPreferences.getBoolean(getString(R.string.shared_pref_showhidden_key), false));
        binding.sAlllistShowcompleted.setChecked(sharedPreferences.getBoolean(getString(R.string.shared_pref_showcompleted_key), false));

        loadRecyclerItemFiltered();

        addNewDialogFragment = new AddNewDialog(this);

        binding.fabAlllist.setOnClickListener(v -> addNewDialogFragment.show(getParentFragmentManager(), AddNewDialog.TAG));

        Bundle receivedBundle = getArguments();
        if(receivedBundle != null) {
            byte junk = receivedBundle.getByte(getString(R.string.bundle_junk));
            if(junk == 0) {
                loadRecyclerItemFiltered();
            }
        }

        AllListFragment fragment = this;
        binding.alllistRec.addOnItemTouchListener(new RecyclerTouchListener(requireContext().getApplicationContext(), binding.alllistRec, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Bundle bundle = new Bundle();
                ToonsContainer toonsContainer = ((ToonsAdapter) Objects.requireNonNull(binding.alllistRec.getAdapter())).getmData().get(position);
                bundle.putParcelable(getString(R.string.bundle_toons), toonsContainer);
                Navigation.findNavController(fragment.requireView()).navigate(R.id.action_allListFragment_to_episodesListFragment, bundle);
            }

            @Override
            public void onLongClick(View view, int position) {
                PopupMenu popupMenu = new PopupMenu(requireContext(), view, Gravity.END);

                popupMenu.getMenuInflater().inflate(R.menu.main_popup, popupMenu.getMenu());

                Menu menu = popupMenu.getMenu();
                for (int i = 0; i < menu.size(); ++i) {
                    if(menu.getItem(i).getItemId() == R.id.action_showhidepopup) {
                        if(((ToonsAdapter) Objects.requireNonNull(binding.alllistRec.getAdapter())).getmData().get(position).hide)
                            menu.getItem(i).setTitle(R.string.action_set_show);
                        else
                            menu.getItem(i).setTitle(R.string.action_set_hide);
                        break;
                    }
                    else if(menu.getItem(i).getItemId() == R.id.action_completepopup) {
                        if(((ToonsAdapter) Objects.requireNonNull(binding.alllistRec.getAdapter())).getmData().get(position).completed)
                            menu.getItem(i).setTitle(R.string.action_set_incomplete);
                        else
                            menu.getItem(i).setTitle(R.string.action_set_complete);
                        break;
                    }
                }

                popupMenu.setOnMenuItemClickListener(menuItem -> {
                    ToonsContainer currentItem = ((ToonsAdapter) Objects.requireNonNull(binding.alllistRec.getAdapter())).getmData().get(position);
                    if (menuItem.getTitle().equals(requireContext().getText(R.string.action_set_hide))) {
                        currentItem.hide = true;
                        dbHelper.editToonContent(currentItem);
                        loadRecyclerItemFiltered();
                    } else if (menuItem.getTitle().equals(requireContext().getText(R.string.action_set_show))) {
                        currentItem.hide = false;
                        dbHelper.editToonContent(currentItem);
                        loadRecyclerItemFiltered();
                    } else if(menuItem.getTitle().equals(requireContext().getText(R.string.action_set_complete))) {
                        currentItem.completed = true;
                        dbHelper.editToonContent(currentItem);
                        loadRecyclerItemFiltered();
                    } else if(menuItem.getTitle().equals(requireContext().getText(R.string.action_set_incomplete))) {
                        currentItem.completed = false;
                        dbHelper.editToonContent(currentItem);
                        loadRecyclerItemFiltered();
                    } else if(menuItem.getTitle().equals(requireContext().getText(R.string.menu_delete))) {
                        dbHelper.deleteToonContent(((ToonsAdapter) Objects.requireNonNull(binding.alllistRec.getAdapter())).deleteAndGetDBIDof(currentItem));
                    } else if (menuItem.getTitle().equals(requireContext().getText(R.string.menu_edit))) {
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(getString(R.string.bundle_toons), currentItem);
                        Navigation.findNavController(fragment.requireView()).navigate(R.id.action_allListFragment_to_editEntryFragment, bundle);
                    }
                    return false;
                });

                popupMenu.show();
            }
        }));

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if(isDebug) Log.i("DebugLog","onStart() of AllListFragment");

        binding.sAlllistShowhidden.setChecked(sharedPreferences.getBoolean(getString(R.string.shared_pref_showhidden_key), false));
        binding.sAlllistShowcompleted.setChecked(sharedPreferences.getBoolean(getString(R.string.shared_pref_showcompleted_key), false));
        if(isDebug) Log.i("DebugLog","SwitchCompat states of ByDayListFragment are recovered");

        binding.sAlllistShowhidden.setOnCheckedChangeListener(this::onCheckedChangedShowHidden);
        binding.sAlllistShowcompleted.setOnCheckedChangeListener(this::onCheckedChangedShowCompleted);
        if(isDebug) Log.i("DebugLog","OnCheckedChangeListeners of AllListFragment are reloaded");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(isDebug) Log.i("DebugLog","onStop() of AllListFragment");

        binding.sAlllistShowhidden.setOnCheckedChangeListener(null);
        binding.sAlllistShowcompleted.setOnCheckedChangeListener(null);
        if(isDebug) Log.i("DebugLog","OnCheckedChangeListeners of AllListFragment are unloaded");
    }

    private void loadRecyclerItemFiltered() {
        binding.alllistRec.setAdapter(new ToonsAdapter(dbHelper.getAllToonsFiltered(
                ToonsContainer.ReleaseDay.ALL,
                sharedPreferences.getBoolean(getString(R.string.shared_pref_showhidden_key), false),
                sharedPreferences.getBoolean(getString(R.string.shared_pref_showcompleted_key), false)
        )));
    }

    private void onCheckedChangedShowHidden(CompoundButton cb, boolean b) {
        if(isDebug) Log.i("DebugLog","Detected change in switch show hidden of AllListFragment");

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(getString(R.string.shared_pref_showhidden_key), b);
        editor.apply();

        loadRecyclerItemFiltered();
    }

    private void onCheckedChangedShowCompleted(CompoundButton cb, boolean b) {
        if(isDebug) Log.i("DebugLog","Detected change in switch show completed of AllListFragment");

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(getString(R.string.shared_pref_showcompleted_key), b);
        editor.apply();

        loadRecyclerItemFiltered();
    }

    @Override
    public void onProceedButtonClicked(View v, String validatedUrl) {
        NavController navController = Navigation.findNavController(this.requireView());
        Bundle bundle = new Bundle();
        bundle.putInt(getString(R.string.bundle_from), INDEX);
        bundle.putString(getString(R.string.bundle_link), validatedUrl);
        navController.navigate(R.id.action_allListFragment_to_reviewEntryFragment, bundle);
    }

    @Override
    public void onWebButtonClicked(View v) {
        NavController navController = Navigation.findNavController(this.requireView());
        Bundle bundle = new Bundle();
        bundle.putInt(getString(R.string.bundle_from), INDEX);
        navController.navigate(R.id.action_allListFragment_to_addByWebFragment, bundle);
    }

    public interface ClickListener {
        void onClick(View view, int position);
        void onLongClick(View view, int position);
    }
}