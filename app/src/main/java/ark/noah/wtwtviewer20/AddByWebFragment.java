package ark.noah.wtwtviewer20;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

import ark.noah.wtwtviewer20.databinding.FragmentAddByWebBinding;

public class AddByWebFragment extends Fragment implements ExecutorRunner.Callback<Document> {

    private boolean isDebug = true;

    private FragmentAddByWebBinding binding;
    private AddByWebViewModel mViewModel;

    private String entryPoint;
    private String currentlyVisibleUrlInString;

    private LinkValidater linkValidater;

    private int camefrom = -1;

    public static AddByWebFragment newInstance() {
        return new AddByWebFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddByWebBinding.inflate(inflater, container, false);
        mViewModel = new ViewModelProvider(this).get(AddByWebViewModel.class);
        View view = binding.getRoot();

        linkValidater = LinkValidater.Instance != null ? LinkValidater.Instance : new LinkValidater();

        isDebug = MainActivity.Instance.isDebug;

        camefrom = requireArguments().getInt(getString(R.string.bundle_from));

        binding.webView.setWebViewClient(new MyBrowser());
        binding.webView.getSettings().setJavaScriptEnabled(true);

        LinkGetter linkGetter = MainActivity.Instance.linkGetter;
        if(linkGetter != null)
            if(linkGetter.isReady())
                entryPoint = linkGetter.getEntryPoint();
        if(isDebug) Log.i("DebugLog","entrypoint: " + entryPoint);

        new ExecutorRunner().execute(() -> {
            Document document = null;
            try {
                document = Jsoup.connect(entryPoint).get();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return document;
        }, this);

        AddByWebFragment fragment = this;
        binding.btnWebAddthispage.setOnClickListener((v) -> {
            NavController navController = Navigation.findNavController(fragment.requireView());
            Bundle bundle = new Bundle();
            bundle.putInt(getString(R.string.bundle_from), camefrom);
            bundle.putString(getString(R.string.bundle_link), currentlyVisibleUrlInString);
            navController.navigate(R.id.action_addByWebFragment_to_reviewEntryFragment4, bundle);
        });

        return view;
    }

    @Override
    public void onComplete(Document result) {
        if(result == null) return;
        if(binding == null) return;

        binding.webView.loadDataWithBaseURL(entryPoint, result.toString(), "text/html", "utf-8", "");
        currentlyVisibleUrlInString = result.location();
        if(isDebug) Log.i("DebugLog","current url: " + currentlyVisibleUrlInString);

        if(linkValidater.isLinkValidEpisodeList(currentlyVisibleUrlInString))
            binding.btnWebAddthispage.setVisibility(View.VISIBLE);
        else
            binding.btnWebAddthispage.setVisibility(View.GONE);
    }

    @Override
    public void onError(Exception e) {
        e.printStackTrace();
    }

    private class MyBrowser extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            final Uri uri = request.getUrl();
            new ExecutorRunner().execute(
                    () -> {
                        Document document = null;
                        try {
                            document = Jsoup.connect(uri.toString()).get();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return document;
                    },
                    AddByWebFragment.this
            );
            return true;
        }
    }
}