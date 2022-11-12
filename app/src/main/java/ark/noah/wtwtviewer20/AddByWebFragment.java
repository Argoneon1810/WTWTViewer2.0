package ark.noah.wtwtviewer20;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

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
    private FragmentAddByWebBinding binding;

    private String entryPoint;
    private String currentlyVisibleUrlInString;

    private LinkValidater linkValidater;

    private int camefrom = -1;

    public static AddByWebFragment newInstance() {
        return new AddByWebFragment();
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddByWebBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        linkValidater = LinkValidater.Instance != null ? LinkValidater.Instance : new LinkValidater();

        camefrom = requireArguments().getInt(getString(R.string.bundle_from));

        binding.webView.setWebViewClient(new MyBrowser());
        binding.webView.getSettings().setJavaScriptEnabled(true);

        LinkGetter linkGetter = LinkGetter.Instance;
        if(linkGetter != null)
            if(linkGetter.isReady())
                entryPoint = linkGetter.getEntryPoint();

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

        binding.webView.loadDataWithBaseURL(entryPoint, result.toString(), "text/html", "utf-8", "");
        currentlyVisibleUrlInString = result.location();

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