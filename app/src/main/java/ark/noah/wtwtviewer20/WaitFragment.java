package ark.noah.wtwtviewer20;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class WaitFragment extends Fragment {

    public WaitFragment() {
    }
    public static WaitFragment newInstance() {
        return new WaitFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wait, container, false);
    }
}