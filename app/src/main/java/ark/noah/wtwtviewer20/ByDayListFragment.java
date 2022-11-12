package ark.noah.wtwtviewer20;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

import ark.noah.wtwtviewer20.databinding.FragmentByDayListBinding;

public class ByDayListFragment extends Fragment implements AddNewDialog.DialogInterface {
    public static final int INDEX = 1;

    private FragmentByDayListBinding binding;

    private DBHelper dbHelper;
    private SharedPreferences sharedPreferences;

    private Drawable foreground, ic_up, ic_down;
    BlendModeColorFilter iconColorFilter;

    private View lastInteractedView;
    private ByDayViewModel byDayViewModel;

    private AddNewDialog addNewDialogFragment;

    private MainListAndEpisodesListSortViewModel mainListAndEpisodesListSortViewModel;
    int lastSortMethod = 0;
    boolean descending = false;

    public static ByDayListFragment newInstance() {
        return new ByDayListFragment();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentByDayListBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        TypedValue value = new TypedValue();
        requireContext().getTheme().resolveAttribute(R.attr.ButtonFocusedForegroundTransparent, value, true);
        foreground = new ColorDrawable(value.data);

        value = new TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.textColor, value, true);
        iconColorFilter = new BlendModeColorFilter(value.data, BlendMode.SRC_ATOP);
        ic_up = requireContext().getDrawable(R.drawable.ic_baseline_arrow_drop_up_24).mutate();
        ic_down = requireContext().getDrawable(R.drawable.ic_baseline_arrow_drop_down_24).mutate();
        ic_up.setColorFilter(iconColorFilter);
        ic_down.setColorFilter(iconColorFilter);

        dbHelper = new DBHelper(requireContext().getApplicationContext());

        sharedPreferences = requireActivity().getApplicationContext().getSharedPreferences(getString(R.string.shared_pref_key), MODE_PRIVATE);

        binding.sBydayShowhidden.setChecked(sharedPreferences.getBoolean(getString(R.string.shared_pref_showhidden_key), false));
        binding.sBydayShowcompleted.setChecked(sharedPreferences.getBoolean(getString(R.string.shared_pref_showcompleted_key), false));

        mainListAndEpisodesListSortViewModel = new ViewModelProvider(requireActivity()).get(MainListAndEpisodesListSortViewModel.class);
        Boolean sortDescMain = mainListAndEpisodesListSortViewModel.sortDescMain.getValue();
        if(sortDescMain != null) descending = sortDescMain;
        mainListAndEpisodesListSortViewModel.sortDescMain.observe(getViewLifecycleOwner(), o -> {
            Boolean result = mainListAndEpisodesListSortViewModel.sortDescMain.getValue();
            if(result == null) return;
            if(result != descending) descending = result;
        });
        Integer sortMain = mainListAndEpisodesListSortViewModel.sortMain.getValue();
        if(sortMain != null) lastSortMethod = sortMain;
        mainListAndEpisodesListSortViewModel.sortMain.observe(getViewLifecycleOwner(), o -> {
            Integer result = mainListAndEpisodesListSortViewModel.sortMain.getValue();
            if(result == null) return;
            if(result != lastSortMethod)
                lastSortMethod = result;
        });

        byDayViewModel = new ViewModelProvider(requireActivity()).get(ByDayViewModel.class);
        if(byDayViewModel.hasBeenFlipped.getValue() == null) {
            byDayViewModel.hasBeenFlipped.setValue(Boolean.TRUE);
            byDayViewModel.dayToShow.setValue(ToonsContainer.ReleaseDay.getToday());
        }
        byDayViewModel.dayToShow.observe(getViewLifecycleOwner(), o -> loadRecyclerItemFiltered());

        assignButtonListeners();

        addNewDialogFragment = new AddNewDialog(this);

        binding.fabByday.setOnClickListener(v -> addNewDialogFragment.show(getParentFragmentManager(), AddNewDialog.TAG));

        Bundle receivedBundle = getArguments();
        if(receivedBundle != null)
            if(receivedBundle.getByte(getString(R.string.bundle_junk)) == 0)
                loadRecyclerItemFiltered();

        ByDayListFragment fragment = this;
        GestureDetector gestureDetector = new GestureDetector(requireContext(), new ByDayRecyclerGestureListener());
        binding.bydayRec.addOnItemTouchListener(new RecyclerTouchListener(requireContext().getApplicationContext(), binding.bydayRec, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Bundle bundle = new Bundle();
                bundle.putParcelable(getString(R.string.bundle_toons), ((ToonsAdapter) Objects.requireNonNull(binding.bydayRec.getAdapter())).getmData().get(position));
                Navigation.findNavController(fragment.requireView()).navigate(R.id.action_byDayListFragment_to_episodesListFragment, bundle);
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

                ToonsAdapter adapter = (ToonsAdapter) Objects.requireNonNull(binding.bydayRec.getAdapter());

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
                    ToonsContainer currentItem = ((ToonsAdapter) Objects.requireNonNull(binding.bydayRec.getAdapter())).getmData().get(position);
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
                        dbHelper.deleteToonContent(((ToonsAdapter) Objects.requireNonNull(binding.bydayRec.getAdapter())).deleteAndGetDBIDof(currentItem));
                    } else if (menuItem.getTitle().equals(requireContext().getText(R.string.menu_edit))) {
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(getString(R.string.bundle_toons), currentItem);
                        bundle.putInt(getString(R.string.bundle_from), INDEX);
                        Navigation.findNavController(fragment.requireView()).navigate(R.id.action_byDayListFragment_to_editEntryFragment, bundle);
                    }
                    return false;
                });

                popupMenu.show();
            }
        }) {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                gestureDetector.onTouchEvent(e);
                return super.onInterceptTouchEvent(rv,e);
            }
        });

        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                clickAndFocus(Objects.requireNonNull(getButtonOfDay(Objects.requireNonNull(byDayViewModel.dayToShow.getValue()))));
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        requireActivity().addMenuProvider(new MenuProvider() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                ((MenuBuilder) menu).setOptionalIconsVisible(true);
                menuInflater.inflate(R.menu.sort_by_day, menu);
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

        binding.sBydayShowhidden.setChecked(sharedPreferences.getBoolean(getString(R.string.shared_pref_showhidden_key), false));
        binding.sBydayShowcompleted.setChecked(sharedPreferences.getBoolean(getString(R.string.shared_pref_showcompleted_key), false));

        binding.sBydayShowhidden.setOnCheckedChangeListener((cb, b) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(getString(R.string.shared_pref_showhidden_key), b);
            editor.apply();

            loadRecyclerItemFiltered();
        });
        binding.sBydayShowcompleted.setOnCheckedChangeListener((cb, b) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(getString(R.string.shared_pref_showcompleted_key), b);
            editor.apply();

            loadRecyclerItemFiltered();
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        binding.sBydayShowhidden.setOnCheckedChangeListener(null);
        binding.sBydayShowcompleted.setOnCheckedChangeListener(null);
    }

    private void loadRecyclerItemFiltered() {
        ArrayList<ToonsContainer> containers = dbHelper.getAllToonsFiltered(
                byDayViewModel.dayToShow.getValue(),
                sharedPreferences.getBoolean(getString(R.string.shared_pref_showhidden_key), false),
                sharedPreferences.getBoolean(getString(R.string.shared_pref_showcompleted_key), false),
                false,
                false
        );
        if(lastSortMethod == ToonsAdapter.INDEX_SORT_BY_NAME) containers.sort(Comparator.comparing(tc -> tc.toonName));
        else if(lastSortMethod == ToonsAdapter.INDEX_SORT_BY_ID) containers.sort(Comparator.comparing(tc -> tc.dbID));
        if(descending) Collections.reverse(containers);
        binding.bydayRec.setAdapter(new ToonsAdapter(containers));
    }

    private void assignButtonListeners() {
        binding.btnSun.setOnClickListener((v) -> {
            highlightView(v);
            byDayViewModel.dayToShow.setValue(ToonsContainer.ReleaseDay.SUN);
        });
        binding.btnMon.setOnClickListener((v) -> {
            highlightView(v);
            byDayViewModel.dayToShow.setValue(ToonsContainer.ReleaseDay.MON);
        });
        binding.btnTue.setOnClickListener((v) -> {
            highlightView(v);
            byDayViewModel.dayToShow.setValue(ToonsContainer.ReleaseDay.TUE);
        });
        binding.btnWed.setOnClickListener((v) -> {
            highlightView(v);
            byDayViewModel.dayToShow.setValue(ToonsContainer.ReleaseDay.WED);
        });
        binding.btnThu.setOnClickListener((v) -> {
            highlightView(v);
            byDayViewModel.dayToShow.setValue(ToonsContainer.ReleaseDay.THU);
        });
        binding.btnFri.setOnClickListener((v) -> {
            highlightView(v);
            byDayViewModel.dayToShow.setValue(ToonsContainer.ReleaseDay.FRI);
        });
        binding.btnSat.setOnClickListener((v) -> {
            highlightView(v);
            byDayViewModel.dayToShow.setValue(ToonsContainer.ReleaseDay.SAT);
        });
        binding.btnUnspecified.setOnClickListener((v) -> {
            highlightView(v);
            byDayViewModel.dayToShow.setValue(ToonsContainer.ReleaseDay.NON);
        });
    }

    private void clickAndFocus(View view) {
        view.performClick();
        view.requestRectangleOnScreen(new Rect(0, 0, view.getWidth(), view.getHeight()), false);
    }

    private void highlightView(View view) {
        lastInteractedView = view;
        applyHighlightState();
    }

    private void applyHighlightState() {
        binding.btnSun.setForeground(binding.btnSun.equals(lastInteractedView) ? foreground : null);
        binding.btnMon.setForeground(binding.btnMon.equals(lastInteractedView) ? foreground : null);
        binding.btnTue.setForeground(binding.btnTue.equals(lastInteractedView) ? foreground : null);
        binding.btnWed.setForeground(binding.btnWed.equals(lastInteractedView) ? foreground : null);
        binding.btnThu.setForeground(binding.btnThu.equals(lastInteractedView) ? foreground : null);
        binding.btnFri.setForeground(binding.btnFri.equals(lastInteractedView) ? foreground : null);
        binding.btnSat.setForeground(binding.btnSat.equals(lastInteractedView) ? foreground : null);
        binding.btnUnspecified.setForeground(binding.btnUnspecified.equals(lastInteractedView) ? foreground : null);
    }

    private View getButtonOfDay(ToonsContainer.ReleaseDay releaseDay) {
        switch(releaseDay) {
            case SUN:
                return binding.btnSun;
            case MON:
                return binding.btnMon;
            case TUE:
                return binding.btnTue;
            case WED:
                return binding.btnWed;
            case THU:
                return binding.btnThu;
            case FRI:
                return binding.btnFri;
            case SAT:
                return binding.btnSat;
        }
        return null;
    }

    @Override
    public void onProceedButtonClicked(View v, String validatedUrl) {
        NavController navController = Navigation.findNavController(this.requireView());
        Bundle bundle = new Bundle();
        bundle.putInt(getString(R.string.bundle_from), INDEX);
        bundle.putString(getString(R.string.bundle_link), validatedUrl);
        navController.navigate(R.id.action_byDayListFragment_to_reviewEntryFragment, bundle);
    }

    @Override
    public void onWebButtonClicked(View v) {
        NavController navController = Navigation.findNavController(this.requireView());
        Bundle bundle = new Bundle();
        bundle.putInt(getString(R.string.bundle_from), INDEX);
        navController.navigate(R.id.action_byDayListFragment_to_addByWebFragment, bundle);
    }

    class ByDayRecyclerGestureListener extends GestureDetector.SimpleOnGestureListener {
        public float SWIPE_DETECTION_OFF_PATH;
        public float SWIPE_DETECTION_MIN_DISTANCE;
        public float SWIPE_DETECTION_MIN_VELOCITY;

        public ByDayRecyclerGestureListener() {
            DeviceSizeGetter dsg = DeviceSizeGetter.Instance;
            SWIPE_DETECTION_OFF_PATH = dsg.getDeviceWidth() * 0.2f;
            SWIPE_DETECTION_MIN_DISTANCE = dsg.getDeviceWidth() * 0.2f;
            SWIPE_DETECTION_MIN_VELOCITY = dsg.getDeviceWidth() * 0.1f;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if(Math.abs(e1.getY() - e2.getY()) > SWIPE_DETECTION_OFF_PATH)
                return false;
            float swipeDistance = (e2.getX() - e1.getX());
            if(Math.abs(swipeDistance) > SWIPE_DETECTION_MIN_DISTANCE)
                if(Math.abs(velocityX) > SWIPE_DETECTION_MIN_VELOCITY)
                    //swiped right to left
                    if (swipeDistance < 0) clickAndFocus(getButtonOfTomorrow());
                    //swiped left to right
                    else if (swipeDistance > 0) clickAndFocus(getButtonOfYesterday());
            return super.onFling(e1, e2, velocityX, velocityY);
        }

        private View getButtonOfTomorrow() {
            if(lastInteractedView == binding.btnSun)
                return binding.btnMon;
            else if(lastInteractedView == binding.btnMon)
                return binding.btnTue;
            else if(lastInteractedView == binding.btnTue)
                return binding.btnWed;
            else if(lastInteractedView == binding.btnWed)
                return binding.btnThu;
            else if(lastInteractedView == binding.btnThu)
                return binding.btnFri;
            else if(lastInteractedView ==  binding.btnFri)
                return binding.btnSat;
            else if(lastInteractedView ==  binding.btnSat)
                return binding.btnUnspecified;
            else if(lastInteractedView ==  binding.btnUnspecified)
                return binding.btnSun;
            else return binding.btnUnspecified;
        }

        private View getButtonOfYesterday() {
            if(lastInteractedView == binding.btnSun)
                return binding.btnUnspecified;
            else if(lastInteractedView == binding.btnMon)
                return binding.btnSun;
            else if(lastInteractedView == binding.btnTue)
                return binding.btnMon;
            else if(lastInteractedView == binding.btnWed)
                return binding.btnTue;
            else if(lastInteractedView == binding.btnThu)
                return binding.btnWed;
            else if(lastInteractedView ==  binding.btnFri)
                return binding.btnThu;
            else if(lastInteractedView ==  binding.btnSat)
                return binding.btnFri;
            else if(lastInteractedView ==  binding.btnUnspecified)
                return binding.btnSat;
            else return binding.btnUnspecified;
        }
    }
}