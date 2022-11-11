package ark.noah.wtwtviewer20;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.os.Bundle;

import com.google.android.material.navigation.NavigationView;

import java.util.Objects;

import ark.noah.wtwtviewer20.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements LinkGetter.Callback {
    public static MainActivity Instance;
    private LinkGetter linkGetter;

    private ActivityMainBinding binding;
    private AppBarConfiguration mAppBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Instance = this;

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.allListFragment, R.id.byDayListFragment, R.id.completedListFragment, R.id.settingsFragment)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        linkGetter = LinkGetter.Instance == null ? new LinkGetter(getApplicationContext(), this) : LinkGetter.Instance;
        new DeviceSizeGetter(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onEntryPointReady(String url) {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        navController.navigate(R.id.action_waitFragment_to_allListFragment3);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(linkGetter != null) {
            if(linkGetter.isReady()) {
                NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                if(Objects.requireNonNull(navController.getCurrentDestination()).getId() == R.id.waitFragment)
                    navController.navigate(R.id.action_waitFragment_to_allListFragment3);
            }
        }
    }
}