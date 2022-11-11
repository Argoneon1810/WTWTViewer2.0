package ark.noah.wtwtviewer20;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ByDayViewModel extends ViewModel {
    MutableLiveData<ToonsContainer.ReleaseDay> dayToShow = new MutableLiveData<>();
    MutableLiveData<Boolean> hasBeenFlipped = new MutableLiveData<>();
}
