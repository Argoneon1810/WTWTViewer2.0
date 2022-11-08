package ark.noah.wtwtviewer20;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

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
import java.util.Iterator;
import java.util.Objects;

import ark.noah.wtwtviewer20.databinding.FragmentEpisodesListBinding;

public class EpisodesListFragment extends Fragment implements ExecutorRunner.Callback<Document>, EpisodesAdapter.IDDifferenceCallback {

    private EpisodesListViewModel mViewModel;
    private FragmentEpisodesListBinding binding;

    private DBHelper dbHelper;

    private ToonsContainer currentContainer;

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

        mViewModel = new ViewModelProvider(this).get(EpisodesListViewModel.class);
        binding = FragmentEpisodesListBinding.inflate(inflater, container, false);

        View view = binding.getRoot();

        dbHelper = new DBHelper(requireContext());

        currentContainer = requireArguments().getParcelable(getString(R.string.bundle_toons));

        ArrayList<EpisodesContainer> mData = new ArrayList<>();
        EpisodesAdapter adapter = new EpisodesAdapter(mData, currentContainer, this::onClick, this);
        binding.recEpisodes.setAdapter(adapter);

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

    void onClick(View v, int position) {
        Bundle bundle = new Bundle();
        bundle.putString(
                getString(R.string.bundle_link),
                LinkGetter.Instance.getEntryPoint() + currentContainer.toonType + 2 + "?toon=" + currentContainer.toonID + "&num=" + currentContainer.episodeID
        );
        Navigation.findNavController(this.requireView()).navigate(R.id.action_episodesListFragment_to_toonViewerFragment, bundle);
    }

    @Override
    public void onComplete(Document result) {
        ArrayList<EpisodesContainer> containers = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 0; i < titles.size(); ++i) {
            EpisodesContainer container = new EpisodesContainer();
            container.link = links.get(i);
            container.number = Integer.parseInt(numbers.get(i));
            container.title = titles.get(i);
            container.date = LocalDate.parse(dates.get(i), formatter);
            containers.add(container);
        }
        binding.recEpisodes.setAdapter(new EpisodesAdapter(containers, currentContainer, this::onClick, this));
    }

    @Override
    public void onError(Exception e) {

    }

    @Override
    public void onIDDifferent(int id) {
        currentContainer.episodeID = id;
        dbHelper.editToonContent(currentContainer);
    }
}