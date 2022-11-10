package ark.noah.wtwtviewer20;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.util.Log;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import ark.noah.wtwtviewer20.databinding.FragmentToonViewerBinding;

public class ToonViewerFragment extends Fragment implements ExecutorRunner.Callback<Pair<Document, Integer>> {

    public static final int RESULT_CODE_UNSUPPORTED_TYPE = 1;
    public static final int RESULT_CODE_NOT_AN_URL = 2;

    boolean isDebug = true;

    FragmentToonViewerBinding binding;

    LinkValidater linkValidater;
    ArrayList<String> imageURLs = new ArrayList<>();

    EpisodesListToViewerSharedViewModel sharedViewModel;

    ToonsContainer currentContainer;
    ToonsContainer oldContainerIfExists;

    boolean showNavigator = true;
    int oldMargin = 0;

    public static ToonViewerFragment newInstance() {
        return new ToonViewerFragment();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentToonViewerBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        isDebug = MainActivity.Instance.isDebug;
        linkValidater = LinkValidater.Instance != null ? LinkValidater.Instance : new LinkValidater();

        Bundle receivedBundle = requireArguments();
        sharedViewModel = new ViewModelProvider(requireActivity()).get(EpisodesListToViewerSharedViewModel.class);
        currentContainer = receivedBundle.getParcelable(getString(R.string.bundle_toons));
        oldContainerIfExists = receivedBundle.getParcelable(getString(R.string.bundle_toons_old));

        new ExecutorRunner().execute(()-> {
            Document document = null;
            int resultcode = 0;
            try {
                String urlInString = currentContainer.asLink();
                document = Jsoup.connect(urlInString).get();

                LinkValidater.Info info = linkValidater.extractInfo(urlInString);
                if(info.toonType.equals("/c"))
                    throw new UnsupportedTypeException();

                Elements element = document.select("div.wbody").select("img");

                int count = 0;
                for (Element imgElement : element) {
                    ++count;
                    String link = imgElement.attr("src");
                    if(isDebug) Log.i("DebugLog", link);
                    imageURLs.add(link);
                }
                if(count == 0)
                    throw new InvalidEpisodeException();
            } catch (UnsupportedTypeException e) {
                e.printStackTrace();
                resultcode = RESULT_CODE_UNSUPPORTED_TYPE;
            } catch (InvalidEpisodeException e) {
                e.printStackTrace();
                resultcode = RESULT_CODE_NOT_AN_URL;
            } catch (IOException e) {
                e.printStackTrace();
                resultcode = -1;
            }
            return new Pair<>(document, resultcode);
        }, this);

        GestureDetector gestureDetector = new GestureDetector(this.getContext(), new MyGetureListener());
        binding.listViewer.setOnTouchListener((view1, motionEvent) -> {
            gestureDetector.onTouchEvent(motionEvent);
            return false;
        });

        ToonViewerFragment fragment = this;
        binding.viewerNext.setOnClickListener((v) -> {
            ToonsContainer next = currentContainer.getNext();
            Bundle bundle = new Bundle();
            bundle.putParcelable(getString(R.string.bundle_toons), next);
            bundle.putParcelable(getString(R.string.bundle_toons_old), currentContainer);
            Navigation.findNavController(fragment.requireView()).navigate(R.id.action_toonViewerFragment_self, bundle);
        });

        binding.viewerPrev.setOnClickListener((v) -> {
            ToonsContainer prev = currentContainer.getPrev();
            Bundle bundle = new Bundle();
            bundle.putParcelable(getString(R.string.bundle_toons), prev);
            bundle.putParcelable(getString(R.string.bundle_toons_old), currentContainer);
            Navigation.findNavController(fragment.requireView()).navigate(R.id.action_toonViewerFragment_self, bundle);
        });

        return view;
    }

    private void goBackWithToast(String message) {
        Toast.makeText(requireContext().getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Bundle bundle = new Bundle();
        if(oldContainerIfExists != null)
            bundle.putParcelable(getString(R.string.bundle_toons), oldContainerIfExists);
        else {
            bundle.putParcelable(getString(R.string.bundle_toons), currentContainer);
            bundle.putByte(getString(R.string.bundle_junk), (byte) 1);
        }
        Navigation.findNavController(requireView()).navigate(R.id.action_toonViewerFragment_to_episodesListFragment, bundle);
    }

    @Override
    public void onComplete(Pair<Document, Integer> result) {
        if(result.second == 0) {
            if(isDebug) Log.i("DebugLog", String.valueOf(imageURLs.size()));
            ArrayList<ToonViewerContainer> containers = new ArrayList<>();
            for (int i = 0; i < imageURLs.size(); ++i) {
                ToonViewerContainer container = new ToonViewerContainer();
                container.imageURL = imageURLs.get(i);
                containers.add(container);
            }
            binding.listViewer.setAdapter(new ToonViewerAdapter(containers, requireContext()));
            sharedViewModel.dataToShare.setValue(currentContainer);
        }
        else if(result.second == RESULT_CODE_UNSUPPORTED_TYPE)
            goBackWithToast(getString(R.string.txt_unsupported_toon));
        else if(result.second == RESULT_CODE_NOT_AN_URL)
            goBackWithToast(getString(R.string.txt_invalid_episode_id));
    }

    @Override
    public void onError(Exception e) {
    }

    class MyGetureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if(showNavigator) {
                showNavigator = false;
                hideToolBar();
                binding.viewerNavigator.setVisibility(View.INVISIBLE);
            } else {
                showNavigator = true;
                showToolBar();
                binding.viewerNavigator.setVisibility(View.VISIBLE);
            }
            return super.onSingleTapUp(e);
        }
    }

    private void hideToolBar() {
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).hide();
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) binding.viewerRoot.getLayoutParams();
        oldMargin = lp.topMargin;
        lp.topMargin = 0;
        binding.viewerRoot.setLayoutParams(lp);
    }

    private void showToolBar() {
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).show();
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) binding.viewerRoot.getLayoutParams();
        lp.topMargin = oldMargin;
        oldMargin = 0;
        binding.viewerRoot.setLayoutParams(lp);
    }

    @Override
    public void onStop() {
        super.onStop();
        showToolBar();
    }

    static class UnsupportedTypeException extends Exception {}
    static class InvalidEpisodeException extends Exception {}
}