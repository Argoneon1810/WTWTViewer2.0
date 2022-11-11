package ark.noah.wtwtviewer20;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

import ark.noah.wtwtviewer20.databinding.FragmentByDayListBinding;

public class ByDayListFragment extends Fragment implements AddNewDialog.DialogInterface {
    public static final int INDEX = 1;

    private FragmentByDayListBinding binding;

    private DBHelper dbHelper;
    private SharedPreferences sharedPreferences;

    private boolean isDebug = true;

    private Drawable foreground;

    private View lastInteractedView;
    private ByDayViewModel byDayViewModel;

    private AddNewDialog addNewDialogFragment;

    public static ByDayListFragment newInstance() {
        return new ByDayListFragment();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentByDayListBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        isDebug = MainActivity.Instance.isDebug;

        TypedValue value = new TypedValue();
        requireContext().getTheme().resolveAttribute(R.attr.ButtonFocusedForegroundTransparent, value, true);
        foreground = new ColorDrawable(value.data);

        dbHelper = new DBHelper(requireContext().getApplicationContext());

        sharedPreferences = requireActivity().getApplicationContext().getSharedPreferences(getString(R.string.shared_pref_key), MODE_PRIVATE);

        binding.sBydayShowhidden.setChecked(sharedPreferences.getBoolean(getString(R.string.shared_pref_showhidden_key), false));
        binding.sBydayShowcompleted.setChecked(sharedPreferences.getBoolean(getString(R.string.shared_pref_showcompleted_key), false));

        byDayViewModel = new ViewModelProvider(requireActivity()).get(ByDayViewModel.class);
        if(byDayViewModel.hasBeenFlipped.getValue() == null) {
            byDayViewModel.hasBeenFlipped.setValue(Boolean.TRUE);
            byDayViewModel.dayToShow.setValue(ToonsContainer.ReleaseDay.getToday());
        }
        byDayViewModel.dayToShow.observe(getViewLifecycleOwner(), o -> loadRecyclerItemFiltered());

        assignButtonListeners();
        highlightLastSelectionOrToday();
        loadRecyclerItemFiltered();

        addNewDialogFragment = new AddNewDialog(this);

        binding.fabByday.setOnClickListener(v -> addNewDialogFragment.show(getParentFragmentManager(), AddNewDialog.TAG));

        Bundle receivedBundle = getArguments();
        if(receivedBundle != null) {
            byte junk = receivedBundle.getByte(getString(R.string.bundle_junk));
            if(junk == 0) {
                loadRecyclerItemFiltered();
            }
        }

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

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if(isDebug) Log.i("DebugLog","onStart() of ByDayListFragment");

        binding.sBydayShowhidden.setChecked(sharedPreferences.getBoolean(getString(R.string.shared_pref_showhidden_key), false));
        binding.sBydayShowcompleted.setChecked(sharedPreferences.getBoolean(getString(R.string.shared_pref_showcompleted_key), false));
        if(isDebug) Log.i("DebugLog","SwitchCompat states of ByDayListFragment are recovered");

        binding.sBydayShowhidden.setOnCheckedChangeListener(this::onCheckedChangedShowHidden);
        binding.sBydayShowcompleted.setOnCheckedChangeListener(this::onCheckedChangedShowCompleted);
        if(isDebug) Log.i("DebugLog","OnCheckedChangeListeners of ByDayListFragment is reloaded");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(isDebug) Log.i("DebugLog","onStop() of ByDayListFragment");

        binding.sBydayShowhidden.setOnCheckedChangeListener(null);
        binding.sBydayShowcompleted.setOnCheckedChangeListener(null);
        if(isDebug) Log.i("DebugLog","OnCheckedChangeListeners of ByDayListFragment are unloaded");
    }

    private void loadRecyclerItemFiltered() {
        if(isDebug) Log.i("DebugLog", "day to show: " + byDayViewModel.dayToShow.getValue());
        if(isDebug) Log.i("DebugLog", "day to show (int): " + Objects.requireNonNull(byDayViewModel.dayToShow.getValue()).getValue());
        ArrayList<ToonsContainer> containers = dbHelper.getAllToonsFiltered(
                byDayViewModel.dayToShow.getValue(),
                sharedPreferences.getBoolean(getString(R.string.shared_pref_showhidden_key), false),
                sharedPreferences.getBoolean(getString(R.string.shared_pref_showcompleted_key), false),
                false,
                false
        );
        containers.sort(Comparator.comparing(tc -> tc.toonName));
        binding.bydayRec.setAdapter(new ToonsAdapter(containers));
    }

    private void onCheckedChangedShowHidden(CompoundButton cb, boolean b) {
        if(isDebug) Log.i("DebugLog","Detected change in switch show hidden of ByDayListFragment");

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(getString(R.string.shared_pref_showhidden_key), b);
        editor.apply();

        loadRecyclerItemFiltered();
    }

    private void onCheckedChangedShowCompleted(CompoundButton cb, boolean b) {
        if(isDebug) Log.i("DebugLog","Detected change in switch show completed of ByDayListFragment");

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
        navController.navigate(R.id.action_byDayListFragment_to_reviewEntryFragment, bundle);
    }

    @Override
    public void onWebButtonClicked(View v) {
        NavController navController = Navigation.findNavController(this.requireView());
        Bundle bundle = new Bundle();
        bundle.putInt(getString(R.string.bundle_from), INDEX);
        navController.navigate(R.id.action_byDayListFragment_to_addByWebFragment, bundle);
    }

    private void assignButtonListeners()
    {
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

    private void highlightLastSelectionOrToday()
    {
        highlightView(getButtonOfDay(Objects.requireNonNull(byDayViewModel.dayToShow.getValue())));
    }

    private void highlightView(View view)
    {
        lastInteractedView = view;
        applyHighlightState();
    }

    private void applyHighlightState()
    {
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

    class ByDayRecyclerGestureListener extends GestureDetector.SimpleOnGestureListener {
        public float SWIPE_DETECTION_MIN_DISTANCE;
        public float SWIPE_DETECTION_MIN_VELOCITY;

        public ByDayRecyclerGestureListener() {
            DeviceSizeGetter dsg = DeviceSizeGetter.Instance;
            SWIPE_DETECTION_MIN_DISTANCE = dsg.getDeviceWidth() * 0.05f;
            SWIPE_DETECTION_MIN_VELOCITY = dsg.getDeviceWidth() * 0.1f;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float swipeDistance = (e2.getX() - e1.getX());
            Log.i("DebugLog","swipeDistance: " + swipeDistance);
            Log.i("DebugLog","velocityX: " + velocityX);
            if(Math.abs(swipeDistance) > SWIPE_DETECTION_MIN_DISTANCE) {
                if(Math.abs(velocityX) > SWIPE_DETECTION_MIN_VELOCITY)
                    if(swipeDistance < 0)           //swiped right to left
                        Objects.requireNonNull(getButtonOfDay(Objects.requireNonNull(byDayViewModel.dayToShow.getValue()).getNext())).performClick();
                    else if(swipeDistance > 0)      //swiped left to right
                        Objects.requireNonNull(getButtonOfDay(Objects.requireNonNull(byDayViewModel.dayToShow.getValue()).getPrev())).performClick();
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }
}