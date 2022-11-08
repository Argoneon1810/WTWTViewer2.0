package ark.noah.wtwtviewer20;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.graphics.Color;
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

import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.CompoundButton;

import com.google.android.material.button.MaterialButton;

import java.util.Calendar;
import java.util.Objects;

import ark.noah.wtwtviewer20.databinding.FragmentByDayListBinding;

public class ByDayListFragment extends Fragment implements AddNewDialog.DialogInterface {
    public static final int INDEX = 1;

    private ByDayListViewModel mViewModel;
    private FragmentByDayListBinding binding;

    private DBHelper dbHelper;
    private SharedPreferences sharedPreferences;

    private boolean isDebug = true;

    private Drawable foreground;

    private View lastInteractedView;
    private ToonsContainer.ReleaseDay dayToShow;

    private AddNewDialog addNewDialogFragment;

    public static ByDayListFragment newInstance() {
        return new ByDayListFragment();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        mViewModel = new ViewModelProvider(this).get(ByDayListViewModel.class);
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

        assignButtonListeners();
        highlightToday();
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
        binding.bydayRec.addOnItemTouchListener(new RecyclerTouchListener(requireContext().getApplicationContext(), binding.bydayRec, new AllListFragment.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Bundle bundle = new Bundle();
                bundle.putParcelable(getString(R.string.bundle_toons), ((ToonsAdapter) Objects.requireNonNull(binding.bydayRec.getAdapter())).getmData().get(position));
                Navigation.findNavController(fragment.requireView()).navigate(R.id.action_allListFragment_to_episodesListFragment, bundle);
            }

            @Override
            public void onLongClick(View view, int position) {
                PopupMenu popupMenu = new PopupMenu(requireContext(), view, Gravity.END);

                popupMenu.getMenuInflater().inflate(R.menu.main_popup, popupMenu.getMenu());

                Menu menu = popupMenu.getMenu();
                for (int i = 0; i < menu.size(); ++i) {
                    if(menu.getItem(i).getItemId() == R.id.action_showhidepopup) {
                        if(((ToonsAdapter) Objects.requireNonNull(binding.bydayRec.getAdapter())).getmData().get(position).hide)
                            menu.getItem(i).setTitle(R.string.action_set_show);
                        else
                            menu.getItem(i).setTitle(R.string.action_set_hide);
                        break;
                    }
                    else if(menu.getItem(i).getItemId() == R.id.action_completepopup) {
                        if(((ToonsAdapter) Objects.requireNonNull(binding.bydayRec.getAdapter())).getmData().get(position).completed)
                            menu.getItem(i).setTitle(R.string.action_set_incomplete);
                        else
                            menu.getItem(i).setTitle(R.string.action_set_complete);
                        break;
                    }
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
        if(isDebug) Log.i("DebugLog", "day to show: " + dayToShow);
        if(isDebug) Log.i("DebugLog", "day to show (int): " + dayToShow.getValue());
        binding.bydayRec.setAdapter(new ToonsAdapter(dbHelper.getAllToonsFiltered(
                dayToShow,
                sharedPreferences.getBoolean(getString(R.string.shared_pref_showhidden_key), false),
                sharedPreferences.getBoolean(getString(R.string.shared_pref_showcompleted_key), false)
        )));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
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

    private void assignButtonListeners()
    {
        binding.btnSun.setOnClickListener((v) -> {
            highlightView(v);
            dayToShow = ToonsContainer.ReleaseDay.SUN;
            loadRecyclerItemFiltered();
        });
        binding.btnMon.setOnClickListener((v) -> {
            highlightView(v);
            dayToShow = ToonsContainer.ReleaseDay.MON;
            loadRecyclerItemFiltered();
        });
        binding.btnTue.setOnClickListener((v) -> {
            highlightView(v);
            dayToShow = ToonsContainer.ReleaseDay.TUE;
            loadRecyclerItemFiltered();
        });
        binding.btnWed.setOnClickListener((v) -> {
            highlightView(v);
            dayToShow = ToonsContainer.ReleaseDay.WED;
            loadRecyclerItemFiltered();
        });
        binding.btnThu.setOnClickListener((v) -> {
            highlightView(v);
            dayToShow = ToonsContainer.ReleaseDay.THU;
            loadRecyclerItemFiltered();
        });
        binding.btnFri.setOnClickListener((v) -> {
            highlightView(v);
            dayToShow = ToonsContainer.ReleaseDay.FRI;
            loadRecyclerItemFiltered();
        });
        binding.btnSat.setOnClickListener((v) -> {
            highlightView(v);
            dayToShow = ToonsContainer.ReleaseDay.SAT;
            loadRecyclerItemFiltered();
        });
        binding.btnUnspecified.setOnClickListener((v) -> {
            highlightView(v);
            dayToShow = ToonsContainer.ReleaseDay.NON;
            loadRecyclerItemFiltered();
        });
    }

    private void highlightToday()
    {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        dayToShow = ToonsContainer.ReleaseDay.getDayFromCalendarDayOfWeek(day);
        switch(day)
        {
            default:
            case Calendar.SUNDAY:
                highlightView(binding.btnSun);
                break;
            case Calendar.MONDAY:
                highlightView(binding.btnMon);
                break;
            case Calendar.TUESDAY:
                highlightView(binding.btnTue);
                break;
            case Calendar.WEDNESDAY:
                highlightView(binding.btnWed);
                break;
            case Calendar.THURSDAY:
                highlightView(binding.btnThu);
                break;
            case Calendar.FRIDAY:
                highlightView(binding.btnFri);
                break;
            case Calendar.SATURDAY:
                highlightView(binding.btnSat);
                break;
        }
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
}