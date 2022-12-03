package ark.noah.wtwtviewer20;

import static android.content.Context.MODE_PRIVATE;

import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SearchView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

import ark.noah.wtwtviewer20.databinding.FragmentCompletedListBinding;

public class CompletedListFragment extends Fragment implements AddNewDialog.DialogInterface, ToonsAdapter.ToonsAdapterFilterResultNotifier {
    public static final int INDEX = 2;

    private FragmentCompletedListBinding binding;

    private DBHelper dbHelper;
    private SharedPreferences sharedPreferences;

    private Drawable ic_up, ic_down;
    BlendModeColorFilter iconColorFilter;

    private AddNewDialog addNewDialogFragment;

    private MainListAndEpisodesListSortViewModel mainListAndEpisodesListSortViewModel;
    int lastSortMethod = 0;
    boolean descending = false;

    public static CompletedListFragment newInstance() {
        return new CompletedListFragment();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentCompletedListBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        TypedValue value = new TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.textColor, value, true);
        iconColorFilter = new BlendModeColorFilter(value.data, BlendMode.SRC_ATOP);
        ic_up = requireContext().getDrawable(R.drawable.ic_baseline_arrow_drop_up_24).mutate();
        ic_down = requireContext().getDrawable(R.drawable.ic_baseline_arrow_drop_down_24).mutate();
        ic_up.setColorFilter(iconColorFilter);
        ic_down.setColorFilter(iconColorFilter);

        dbHelper = new DBHelper(requireContext().getApplicationContext());
        sharedPreferences = requireActivity().getApplicationContext().getSharedPreferences(getString(R.string.shared_pref_key), MODE_PRIVATE);
        SharedPreferences settingPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext().getApplicationContext());

        binding.sCompletedShowhidden.setChecked(sharedPreferences.getBoolean(getString(R.string.shared_pref_showcompleted_key), false));

        mainListAndEpisodesListSortViewModel = new ViewModelProvider(requireActivity()).get(MainListAndEpisodesListSortViewModel.class);
        Boolean sortDescMain = mainListAndEpisodesListSortViewModel.sortDescMain.getValue();
        if(sortDescMain != null) descending = sortDescMain;
        else descending = settingPreferences.getString(
                    getString(R.string.pref_key_default_sorting_dir),                   //selected value
                    getResources().getStringArray(R.array.default_sorting_dir)[1]       //if non, use default value
            ).equals(getResources().getStringArray(R.array.default_sorting_dir)[1]);
        mainListAndEpisodesListSortViewModel.sortDescMain.observe(getViewLifecycleOwner(), o -> {
            Boolean result = mainListAndEpisodesListSortViewModel.sortDescMain.getValue();
            if(result == null) return;
            if(result != descending) descending = result;
        });
        Integer sortMain = mainListAndEpisodesListSortViewModel.sortMain.getValue();
        if(sortMain != null) lastSortMethod = sortMain;
        else lastSortMethod = getSortingMethod(settingPreferences.getString(
                    getString(R.string.pref_key_default_sorting_cat),                   //selected value
                    getResources().getStringArray(R.array.default_sorting_cat)[0]       //if non, use default value
            ));
        mainListAndEpisodesListSortViewModel.sortMain.observe(getViewLifecycleOwner(), o -> {
            Integer result = mainListAndEpisodesListSortViewModel.sortMain.getValue();
            if(result == null) return;
            if(result != lastSortMethod)
                lastSortMethod = result;
        });

        loadRecyclerItemFiltered();

        addNewDialogFragment = new AddNewDialog(this);
        binding.fabCompleted.setOnClickListener(v -> addNewDialogFragment.show(getParentFragmentManager(), AddNewDialog.TAG));

        Bundle receivedBundle = getArguments();
        if(receivedBundle != null) {
            byte junk = receivedBundle.getByte(getString(R.string.bundle_junk));
            if(junk == 0) {
                loadRecyclerItemFiltered();
            }
        }

        CompletedListFragment fragment = this;
        binding.completedRec.addOnItemTouchListener(new RecyclerTouchListener(requireContext().getApplicationContext(), binding.completedRec, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Bundle bundle = new Bundle();
                bundle.putParcelable(getString(R.string.bundle_toons), ((ToonsAdapter) Objects.requireNonNull(binding.completedRec.getAdapter())).getmData().get(position));
                Navigation.findNavController(fragment.requireView()).navigate(R.id.action_completedListFragment_to_episodesListFragment, bundle);
            }

            @Override
            public void onLongClick(View view, int position) {
                Context context;
                try {
                    context = requireContext();
                } catch(IllegalStateException e) {
                    e.printStackTrace();
                    return;
                }

                PopupMenu popupMenu = new PopupMenu(context, view, Gravity.END);
                popupMenu.getMenuInflater().inflate(R.menu.main_popup, popupMenu.getMenu());

                ToonsAdapter adapter = (ToonsAdapter) Objects.requireNonNull(binding.completedRec.getAdapter());

                Menu menu = popupMenu.getMenu();
                for (int i = 0; i < menu.size(); ++i) {
                    MenuItem menuItem = menu.getItem(i);
                    if(menuItem.getItemId() == R.id.action_showhidepopup)
                        if (adapter.getmData().get(position).hide)
                            menu.getItem(i).setTitle(R.string.action_set_show);
                        else menu.getItem(i).setTitle(R.string.action_set_hide);
                    if(menuItem.getItemId() == R.id.action_completepopup)
                        if (adapter.getmData().get(position).completed)
                            menu.getItem(i).setTitle(R.string.action_set_incomplete);
                        else menu.getItem(i).setTitle(R.string.action_set_complete);
                }

                popupMenu.setOnMenuItemClickListener(menuItem -> {
                    ToonsContainer currentItem = ((ToonsAdapter) Objects.requireNonNull(binding.completedRec.getAdapter())).getmData().get(position);
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
                        dbHelper.deleteToonContent(((ToonsAdapter) Objects.requireNonNull(binding.completedRec.getAdapter())).deleteAndGetDBIDof(currentItem));
                    } else if (menuItem.getTitle().equals(requireContext().getText(R.string.menu_edit))) {
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(getString(R.string.bundle_toons), currentItem);
                        bundle.putInt(getString(R.string.bundle_from), INDEX);
                        Navigation.findNavController(fragment.requireView()).navigate(R.id.action_completedListFragment_to_editEntryFragment, bundle);
                    }
                    return false;
                });

                popupMenu.show();
            }
        }));

        requireActivity().addMenuProvider(new MenuProvider() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                ((MenuBuilder) menu).setOptionalIconsVisible(true);
                menuInflater.inflate(R.menu.sort, menu);
                MenuItem searchMenuItem = menu.findItem(R.id.search);
                SearchView searchView = (SearchView) searchMenuItem.getActionView();
                searchView.setQueryHint(getString(R.string.txt_search));
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String query) {
                        ToonsAdapter adapter = (ToonsAdapter) binding.completedRec.getAdapter();
                        Objects.requireNonNull(adapter).getFilter().filter(query);
                        return false;
                    }
                });
            }

            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                MenuProvider.super.onPrepareMenu(menu);
                for(int i = 0; i < menu.size(); ++i) {
                    MenuItem menuItem = menu.getItem(i);
                    if(menuItem.getItemId() == R.id.menu_by_name) {
                        if (lastSortMethod == ToonsAdapter.INDEX_SORT_BY_NAME) {
                            if (descending) menuItem.setIcon(ic_down);
                            else menuItem.setIcon(ic_up);
                        } else menuItem.setIcon(null);
                    }
                    else if(menuItem.getItemId() == R.id.menu_by_day) {
                        if (lastSortMethod == ToonsAdapter.INDEX_SORT_BY_DAY) {
                            if (descending) menuItem.setIcon(ic_down);
                            else menuItem.setIcon(ic_up);
                        } else menuItem.setIcon(null);
                    }
                    else if(menuItem.getItemId() == R.id.menu_by_id) {
                        if (lastSortMethod == ToonsAdapter.INDEX_SORT_BY_ID) {
                            if (descending) menuItem.setIcon(ic_down);
                            else menuItem.setIcon(ic_up);
                        } else menuItem.setIcon(null);
                    }
                }
            }

            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                boolean toReturn = false;
                switch(menuItem.getItemId()) {
                    case R.id.menu_by_name:
                        if(lastSortMethod != ToonsAdapter.INDEX_SORT_BY_NAME) {
                            lastSortMethod = ToonsAdapter.INDEX_SORT_BY_NAME;
                            descending = false;
                            mainListAndEpisodesListSortViewModel.sortMain.setValue(lastSortMethod);
                        }
                        else descending = !descending;
                        mainListAndEpisodesListSortViewModel.sortDescMain.setValue(descending);
                        toReturn = true;
                        break;
                    case R.id.menu_by_day:
                        if(lastSortMethod != ToonsAdapter.INDEX_SORT_BY_DAY) {
                            lastSortMethod = ToonsAdapter.INDEX_SORT_BY_DAY;
                            descending = false;
                            mainListAndEpisodesListSortViewModel.sortMain.setValue(lastSortMethod);
                        }
                        else descending = !descending;
                        mainListAndEpisodesListSortViewModel.sortDescMain.setValue(descending);
                        toReturn = true;
                        break;
                    case R.id.menu_by_id:
                        if(lastSortMethod != ToonsAdapter.INDEX_SORT_BY_ID) {
                            lastSortMethod = ToonsAdapter.INDEX_SORT_BY_ID;
                            descending = false;
                            mainListAndEpisodesListSortViewModel.sortMain.setValue(lastSortMethod);
                        }
                        else descending = !descending;
                        mainListAndEpisodesListSortViewModel.sortDescMain.setValue(descending);
                        toReturn = true;
                        break;
                    default:
                        break;
                }
                if(toReturn) loadRecyclerItemFiltered();
                return toReturn;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        binding.sCompletedShowhidden.setChecked(sharedPreferences.getBoolean(getString(R.string.shared_pref_showhidden_key), false));

        binding.sCompletedShowhidden.setOnCheckedChangeListener(this::onCheckedChangedShowHidden);
    }

    @Override
    public void onStop() {
        super.onStop();
        binding.sCompletedShowhidden.setOnCheckedChangeListener(null);
    }

    private void loadRecyclerItemFiltered() {
        ArrayList<ToonsContainer> containers = dbHelper.getAllToonsFiltered(
                ToonsContainer.ReleaseDay.ALL,
                sharedPreferences.getBoolean(getString(R.string.shared_pref_showhidden_key), false),
                true,
                false,
                true
        );
        if(lastSortMethod == ToonsAdapter.INDEX_SORT_BY_NAME) containers.sort(Comparator.comparing(tc -> tc.toonName));
        else if(lastSortMethod == ToonsAdapter.INDEX_SORT_BY_DAY) containers.sort(Comparator.comparing(tc -> tc.releaseWeekdays));
        else if(lastSortMethod == ToonsAdapter.INDEX_SORT_BY_ID) containers.sort(Comparator.comparing(tc -> tc.dbID));
        if(descending) Collections.reverse(containers);
        ToonsAdapter adapter = new ToonsAdapter(containers);
        adapter.assignNotifier(this);
        binding.completedRec.setAdapter(adapter);
    }

    private int getSortingMethod(String value) {
        if(value.equals(getResources().getStringArray(R.array.default_sorting_cat)[0]))
            return ToonsAdapter.INDEX_SORT_BY_NAME;
        else if(value.equals(getResources().getStringArray(R.array.default_sorting_cat)[1]))
            return ToonsAdapter.INDEX_SORT_BY_DAY;
        else /*if(value.equals(getResources().getStringArray(R.array.default_sorting_cat)[2]))*/
            return ToonsAdapter.INDEX_SORT_BY_ID;
    }

    private void onCheckedChangedShowHidden(CompoundButton cb, boolean b) {
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

    @Override
    public void isEmpty(boolean state) {
        if(state)
            binding.txtCompletedSearchempty.setVisibility(View.VISIBLE);
        else
            binding.txtCompletedSearchempty.setVisibility(View.INVISIBLE);
    }
}