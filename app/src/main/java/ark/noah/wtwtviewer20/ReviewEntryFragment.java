package ark.noah.wtwtviewer20;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

import ark.noah.wtwtviewer20.databinding.FragmentReviewEntryBinding;

public class ReviewEntryFragment extends Fragment implements ExecutorRunner.Callback {

    private FragmentReviewEntryBinding binding;
    private ReviewEntryViewModel mViewModel;

    private int cameFrom;
    private String receivedURL;

    private String title;
    private String thumbnailURL;
    private int releaseDay;

    private DBHelper dbHelper;

    private LinkValidater linkValidater;

    public static ReviewEntryFragment newInstance() {
        return new ReviewEntryFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mViewModel = new ViewModelProvider(this).get(ReviewEntryViewModel.class);
        binding = FragmentReviewEntryBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        dbHelper = new DBHelper(requireContext().getApplicationContext());

        linkValidater = LinkValidater.Instance != null ? LinkValidater.Instance : new LinkValidater();

        Bundle receivedBundle = requireArguments();
        cameFrom = receivedBundle.getInt(getString(R.string.bundle_from));
        receivedURL = receivedBundle.getString(getString(R.string.bundle_link));

        ArrayList<ToonsContainer> mList = new ArrayList<>();
        ReviewEntryAdapter adapter = new ReviewEntryAdapter(mList);
        binding.recReview.setAdapter(adapter);

        if(linkValidater.isLinkValidAll(receivedURL)) {
            new ExecutorRunner().execute(() -> {
                Document document = null;
                try {
                    document = Jsoup.connect(receivedURL).get();

                    String header = document.select("div.whead").select("h1").text();
                    header = header.substring(header.indexOf(") ") + 1);
                    title = header.substring(0, header.lastIndexOf(' '));
                    releaseDay = 0;
                    thumbnailURL = "";
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return document;
            }, this);
        }
        else {
            new ExecutorRunner().execute(() -> {
                Document document = null;
                try {
                    document = Jsoup.connect(receivedURL).get();

                    Iterator<Element> headers = document.select("div.whead").select("h1").iterator();
                    while(headers.hasNext()) {
                        String header = headers.next().text();
                        if(!header.equals("인기웹툰")) title = header;
                    }

                    Calendar calendar = Calendar.getInstance();
                    int[] occurance = { 0, 0, 0, 0, 0, 0, 0 };
                    Iterator<Element> dates = document.select("div.date").iterator();
                    while(dates.hasNext()) {
                        String formattedDate = dates.next().text();
                        int firstDash = formattedDate.indexOf('-');
                        int year = Integer.parseInt(formattedDate.substring(0, firstDash));
                        String monthAnDayOfMonth = formattedDate.substring(firstDash+1);
                        int secondDash = monthAnDayOfMonth.indexOf('-');
                        int month = Integer.parseInt(monthAnDayOfMonth.substring(0, secondDash));
                        int dayOfMonth = Integer.parseInt(monthAnDayOfMonth.substring(secondDash + 1));
                        calendar.set(year, month, dayOfMonth);
                        ++occurance[calendar.get(Calendar.DAY_OF_WEEK)-1];
                    }
                    int max = -1;
                    int maxIndex = -1;
                    for (int i = 0; i < occurance.length; ++i) {
                        max = Math.max(max, occurance[i]);
                        maxIndex = i;
                    }
                    releaseDay = 1 << (maxIndex-1);

                    thumbnailURL = document.select("div.img-box").select("img").attr("src");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return document;
            }, this);
        }

        ReviewEntryFragment fragment = this;
        binding.fabReviewProceed.setOnClickListener((v) -> {
            dbHelper.insertToonContent(adapter.mList.get(0));       //always 1 entry
            NavController navController = Navigation.findNavController(fragment.requireView());
            Bundle bundle = new Bundle();
            bundle.putByte(getString(R.string.bundle_junk), (byte) 0);
            switch(cameFrom) {
                default:
                case 0: //to alllist
                    navController.navigate(R.id.action_reviewEntryFragment_to_allListFragment, bundle);
                    break;
                case 1: //to byday
                    navController.navigate(R.id.action_reviewEntryFragment_to_byDayListFragment, bundle);
                    break;
                case 2: //to completed
                    navController.navigate(R.id.action_reviewEntryFragment_to_completedListFragment2, bundle);
                    break;
            }
        });

        return view;
    }

    @Override
    public void onComplete(Object result) {
        ReviewEntryAdapter adapter = (ReviewEntryAdapter) binding.recReview.getAdapter();
        LinkValidater.Info info = linkValidater.extractInfo(receivedURL);
        if(adapter != null && info != null)
            adapter.add(title, info.toonType, info.toonID, info.episodeID == -1 ? 1 : info.episodeID, releaseDay, thumbnailURL);
    }

    @Override
    public void onError(Exception e) {

    }
}