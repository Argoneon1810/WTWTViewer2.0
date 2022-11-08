package ark.noah.wtwtviewer20;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import ark.noah.wtwtviewer20.databinding.FragmentToonViewerBinding;

public class ToonViewerFragment extends Fragment implements ExecutorRunner.Callback {

    private ToonViewerViewModel mViewModel;
    FragmentToonViewerBinding binding;

    String url;
    ArrayList<String> imageURLs = new ArrayList<>();

    public static ToonViewerFragment newInstance() {
        return new ToonViewerFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentToonViewerBinding.inflate(inflater, container, false);
        mViewModel = new ViewModelProvider(this).get(ToonViewerViewModel.class);

        View view = binding.getRoot();

        url = requireArguments().getString(getString(R.string.bundle_link));

        Log.i("DebugLog", url);

        new ExecutorRunner().execute(()-> {
            Document document = null;
            try {
                document = Jsoup.connect(url).get();

                Elements element = document.select("div.wbody").select("img");

                for (Element imgElement : element) {
                    String link = imgElement.attr("src");
                    Log.i("DebugLog", link);
                    imageURLs.add(link);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return document;
        }, this);

        return view;
    }

    @Override
    public void onComplete(Object result) {
        Log.i("DebugLog", String.valueOf(imageURLs.size()));
        ArrayList<ToonViewerContainer> containers = new ArrayList<>();
        for (int i = 0; i < imageURLs.size(); ++i) {
            ToonViewerContainer container = new ToonViewerContainer();
            container.imageURL = imageURLs.get(i);
            containers.add(container);
        }
        binding.listViewer.setAdapter(new ToonViewerAdapter(containers, requireContext()));
    }

    @Override
    public void onError(Exception e) {

    }
}