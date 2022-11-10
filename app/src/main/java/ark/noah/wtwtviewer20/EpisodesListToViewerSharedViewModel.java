package ark.noah.wtwtviewer20;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class EpisodesListToViewerSharedViewModel extends ViewModel {
    MutableLiveData<ToonsContainer> dataToShare = new MutableLiveData<>();

    public void updateData(ToonsContainer toonsContainer) {
        dataToShare.setValue(toonsContainer);
    }
}
