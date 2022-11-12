package ark.noah.wtwtviewer20;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainListAndEpisodesListSortViewModel extends ViewModel {
    MutableLiveData<Integer> sortMain = new MutableLiveData<>();
    MutableLiveData<Boolean> sortDescMain = new MutableLiveData<>();
    MutableLiveData<Boolean> sortDescEpisodes = new MutableLiveData<>();
}
