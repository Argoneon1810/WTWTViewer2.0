package ark.noah.wtwtviewer20;

import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;

import ark.noah.wtwtviewer20.databinding.FragmentEpisodesListBinding;

public class EpisodesListFragment extends Fragment implements ExecutorRunner.Callback<Document>, EpisodesAdapter.IDDifferenceCallback {

    private FragmentEpisodesListBinding binding;

    private DBHelper dbHelper;

    private ToonsContainer currentContainer;
    EpisodesListToViewerSharedViewModel sharedViewModel;

    private ArrayList<String> links = new ArrayList<>();
    private ArrayList<String> numbers = new ArrayList<>();
    private ArrayList<String> titles = new ArrayList<>();
    private ArrayList<String> dates = new ArrayList<>();

    private boolean descending = false;

    public static EpisodesListFragment newInstance() {
        return new EpisodesListFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentEpisodesListBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        dbHelper = new DBHelper(requireContext());

        Bundle receivedBundle = requireArguments();
        currentContainer = receivedBundle.getParcelable(getString(R.string.bundle_toons));

        byte junkForWeb = receivedBundle.getByte(getString(R.string.bundle_junk));
        if(junkForWeb != 0) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentContainer.asLink()));
            startActivity(browserIntent);
        }

        ArrayList<EpisodesContainer> mData = dbHelper.getAllEpisodes(currentContainer);
        if(mData.size() == 0) binding.tvRecEpisodesWait.setVisibility(View.VISIBLE);
        else {
            binding.tvRecEpisodesWait.setVisibility(View.GONE);
            mData.sort(Comparator.comparing((ec) -> ec.number));
        }
        if(descending) Collections.reverse(mData);
        EpisodesAdapter adapter = new EpisodesAdapter(mData, currentContainer, this::onClick, this);
        binding.recEpisodes.setAdapter(adapter);
        RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(requireContext()) {
            @Override
            protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_START;
            }
        };
        smoothScroller.setTargetPosition(currentContainer.episodeID-1);
        binding.recEpisodes.scrollToPosition(getPreScrollIndex(currentContainer.episodeID-1, getCenterViewingIndex(binding.recEpisodes)));
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
                document = Jsoup.connect(LinkGetter.Instance.getEntryPoint() + currentContainer.toonType + 1 + "?toon=" + currentContainer.toonID).get();

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

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.sort_menu_episode, menu);
            }

            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if(menuItem.getItemId() == R.id.menu_sort) {
                    descending = !descending;
                    ArrayList<EpisodesContainer> containers = adapter.getEditableMData();
                    containers.sort(Comparator.comparing((ec) -> ec.number));
                    if(descending) Collections.reverse(containers);
                    adapter.replaceAllData(containers);
                    int targetPos = adapter.getPositionOfEpisode(currentContainer.episodeID);
                    binding.recEpisodes.scrollToPosition(getPreScrollIndex(targetPos, getCenterViewingIndex(binding.recEpisodes)));
                    binding.recEpisodes.smoothScrollToPosition(targetPos);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        return view;
    }

    private int getCenterViewingIndex(RecyclerView rv) {
        LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
        if(lm != null) {
            int first = lm.findFirstVisibleItemPosition();
            int last = lm.findLastVisibleItemPosition();
            return (first + last) / 2;
        }
        return 0;
    }

    private int getPreScrollIndex(int rawIndex, int lastViewingIndex) {
        int tryIndex = 0;
        if(rawIndex > lastViewingIndex) {
            tryIndex = rawIndex - 30;
            return Math.max(tryIndex, lastViewingIndex);
        }
        else if(rawIndex < lastViewingIndex) {
            tryIndex = rawIndex + 30;
            return Math.min(tryIndex, lastViewingIndex);
        }
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
                containers.sort(Comparator.comparing((ec) -> ec.number));
                binding.tvRecEpisodesWait.setVisibility(View.GONE);
                EpisodesAdapter adapter = (EpisodesAdapter) binding.recEpisodes.getAdapter();
                if(adapter != null) {
                    int targetPos;
                    if (descending) {
                        Collections.reverse(containers);
                        adapter.addAtFront(containers);
                        EpisodesContainer ec = containers.get(0);
                        if(ec.number == currentContainer.episodeID + 1)
                            targetPos = adapter.getPositionOfEpisode(ec.number);
                        else
                            targetPos = adapter.getPositionOfEpisode(currentContainer.episodeID);
                    }
                    else {
                        adapter.add(containers);
                        EpisodesContainer ec = containers.get(containers.size()-1);
                        if(ec.number == currentContainer.episodeID + 1)
                            targetPos = adapter.getPositionOfEpisode(ec.number);
                        else
                            targetPos = adapter.getPositionOfEpisode(currentContainer.episodeID);
                    }
                    binding.recEpisodes.scrollToPosition(getPreScrollIndex(targetPos, getCenterViewingIndex(binding.recEpisodes)));
                    binding.recEpisodes.smoothScrollToPosition(targetPos);
                }
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