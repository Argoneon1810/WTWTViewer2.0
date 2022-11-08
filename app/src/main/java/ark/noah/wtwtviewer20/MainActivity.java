package ark.noah.wtwtviewer20;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.os.Bundle;
import android.util.Log;

import com.google.android.material.navigation.NavigationView;

import ark.noah.wtwtviewer20.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements LinkGetter.Callback {
    public boolean isDebug = true;

    public static MainActivity Instance;
    public LinkGetter linkGetter;

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
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onEntryPointReady(String url) {
        if(isDebug) Log.i("DebugLog", "entrypoint is: " + url);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        navController.navigate(R.id.action_waitFragment_to_allListFragment3);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isDebug) Log.i("DebugLog","onResume() of MainActivity Called");
        if(linkGetter != null) {
            if(isDebug) Log.i("DebugLog","linkgetter of MainActivity is not null");
            if(linkGetter.isReady()) {
                if(isDebug) Log.i("DebugLog","linkGetter of MainActivity is ready");
                if(isDebug) Log.i("DebugLog","entrypoint is: " + linkGetter.getEntryPoint());
                NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.action_waitFragment_to_allListFragment3);
            }
        }
    }
}

// 계획: 웹툰뷰어 개선하기
/* 문제1: 리스트 보기 방식 변경 필요
 * --내용: 요일 순서 정렬은 괜찮다. 다만 애초에 요일 분류가 된 리스트를 볼 수 있으면 좋겠다
 *        내비게이션 탭에 요일별 탭을 선택하면 앱 바에 드롭다운이 표시되어서 요일을 선택하여 볼 수 있으면 좋겠다.
 */
/* 문제2: 섬네일 부족
 * --내용: 에피소드 섬네일은 애초에 제공이 안되기 때문에 어쩔 수 없다지만, 웹툰 섬네일은 가능할텐데 그것조차 없다. 개선 필요
 */
/* 문제3: 완결 마킹 불가
 * --내용: 숨김 마킹과 별개로 내비게이션 탭 중에 완결 목록이 있었으면 좋겠다. 활성/비활성으로 분류해도 될듯
 */
/* 문제4: 웹툰 가져오기 방식 문제
 * --내용: 어차피 다중 링크 불러오기 방식은 잘 사용하지 않는다. 그러니 이것은 제거하고 대신 웹 불러오기 방식을 좀 더 개선하자
 *        제목 검색을 웹에 바로 중개해 줄 수 있도록 하고, 에피소드에 들어가서 웹툰 추가를 하는 것이 아니라 에피소드 리스트에서 추가하자
 */
/* 문제5: 코믹 여는 방식 수정
 * --내용: 코믹이 로딩이 안된다는 이유로 /c1인 경우 웹으로 연결되게 해 뒀는데,
 *        이미지 로딩이 안될 뿐 에피소드 리스트 진입은 가능하므로 에피소드 리스트 까지는 들어갈 수 있게 해 줘야 트래킹이 쉬움
 *        쉬운 구현을 위해 브라우저를 열게끔 했지만, 그냥 웹뷰를 하나 사용하는것이 사용성은 좋을 듯 함
 */
/* 문제6: 회차 이동 방식 수정
 * --내용: 회차 간 이동을 하려면 반드시 한번 빠져서 리스트를 봐야 하는데, 뷰어에서 바로 다음 화나 이전 화를 볼 수 있게 해야 함
 */
/* 문제7: 리스트 표시 방식 수정
 * --내용: 매번 웹에서 리스트를 긁어와서 표시하고 앱은 어느 회차를 마지막으로 봤는지만 기억하는데
 *        그 대신 한번 긁어온 리스트는 모두 기억하고, 회차를 읽은 적이 있는지, 어느 회차를 언제 읽었는지 등등도 기억하게 한 후
 *        웹에서는 새로운 회차가 있는지만 확인하고 그것만 추가
 *        이것으로 뷰어에서도 회차 리스트를 볼 수 있게 구현할 수 있음
 */