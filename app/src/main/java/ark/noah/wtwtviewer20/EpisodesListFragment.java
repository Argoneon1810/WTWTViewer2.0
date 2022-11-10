package ark.noah.wtwtviewer20;

import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;

import ark.noah.wtwtviewer20.databinding.FragmentEpisodesListBinding;

public class EpisodesListFragment extends Fragment implements ExecutorRunner.Callback<Document>, EpisodesAdapter.IDDifferenceCallback {

    private FragmentEpisodesListBinding binding;

    private DBHelper dbHelper;

    private ToonsContainer currentContainer;
    EpisodesListToViewerSharedViewModel sharedViewModel;

    boolean isDebug = true;

    private ArrayList<String> links = new ArrayList<>();
    private ArrayList<String> numbers = new ArrayList<>();
    private ArrayList<String> titles = new ArrayList<>();
    private ArrayList<String> dates = new ArrayList<>();

    public static EpisodesListFragment newInstance() {
        return new EpisodesListFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentEpisodesListBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        dbHelper = new DBHelper(requireContext());

        isDebug = MainActivity.Instance.isDebug;

        Bundle receivedBundle = requireArguments();
        currentContainer = receivedBundle.getParcelable(getString(R.string.bundle_toons));

        byte junkForWeb = receivedBundle.getByte(getString(R.string.bundle_junk));
        if(junkForWeb != 0) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentContainer.asLink()));
            startActivity(browserIntent);
        }

        ArrayList<EpisodesContainer> mData = dbHelper.getAllEpisodes(currentContainer);
        mData.sort(Comparator.comparing((ec) -> ec.number));
        EpisodesAdapter adapter = new EpisodesAdapter(mData, currentContainer, this::onClick, this);
        binding.recEpisodes.setAdapter(adapter);
        RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(requireContext()) {
            @Override
            protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_START;
            }
        };
        smoothScroller.setTargetPosition(currentContainer.episodeID-1);
        binding.recEpisodes.scrollToPosition(getSuitableIndex(currentContainer.episodeID-1));
        Objects.requireNonNull(binding.recEpisodes.getLayoutManager()).startSmoothScroll(smoothScroller);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(EpisodesListToViewerSharedViewModel.class);
        sharedViewModel.dataToShare.observe(getViewLifecycleOwner(), o -> {
            if(!o.isSameToon(currentContainer)) return;
            if(o.equals(currentContainer)) return;
            adapter.updateCurrentToon(o);
            dbHelper.editToonContent(o);
        });

        new ExecutorRunner().execute(()->{
            Document document = null;
            try {
                document = Jsoup.connect(MainActivity.Instance.linkGetter.getEntryPoint() + currentContainer.toonType + 1 + "?toon=" + currentContainer.toonID).get();

                Elements element = document.select("div.left-box").select("ul.list");

                Iterator<Element> elementIteratorLinks = element.select("a").iterator();
                Iterator<Element> elementIteratorDatas = element.select("div.list-box").iterator();
                Iterator<Element> elementIteratorTitles = element.select("div.subject").iterator();

                while (elementIteratorLinks.hasNext()) {
                    links.add(elementIteratorLinks.next().attr("href"));
                    String[] allText = elementIteratorDatas.next().text().split(" ");
                    numbers.add(allText[0]);
                    titles.add(elementIteratorTitles.next().ownText());
                    dates.add(allText[allText.length-1]);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return document;
        }, this);

        return view;
    }

    private int getSuitableIndex(int rawIndex) {
        int tryIndex = rawIndex - 30;
        if(tryIndex < 0) tryIndex = 0;
        return tryIndex;
    }

    @Override
    public void onComplete(Document result) {
        try {
            ArrayList<EpisodesContainer> containers = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(getString(R.string.date_format));
            for (int i = 0; i < titles.size(); ++i) {
                EpisodesContainer container = new EpisodesContainer();
                container.number = Integer.parseInt(numbers.get(i));
                container.dbIDofToon = currentContainer.dbID;
                container.link = links.get(i);
                container.title = titles.get(i);
                container.date = LocalDate.parse(dates.get(i), formatter);
                if (dbHelper.tryInsertEpisodeContent(currentContainer, container))
                    containers.add(container);
                else break;
            }

            if (containers.size() > 0) {
                EpisodesAdapter adapter = (EpisodesAdapter) binding.recEpisodes.getAdapter();
                Objects.requireNonNull(adapter).addAtFront(containers);
                binding.recEpisodes.scrollToPosition(adapter.getPositionOfEpisode(containers.get(0).number));
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(Exception e) {

    }

    void onClick(View v, int position) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(
                getString(R.string.bundle_toons),
                currentContainer
        );
        Navigation.findNavController(this.requireView()).navigate(R.id.action_episodesListFragment_to_toonViewerFragment, bundle);
    }

    @Override
    public void onIDDifferent(int id) {
        currentContainer.episodeID = id;
        dbHelper.editToonContent(currentContainer);
    }
}