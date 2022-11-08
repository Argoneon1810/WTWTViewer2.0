package ark.noah.wtwtviewer20;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class ReviewEntryAdapter extends RecyclerView.Adapter<ReviewEntryAdapter.ViewHolder> {

    ArrayList<ToonsContainer> mList;

    private Drawable foreground;

    public ReviewEntryAdapter(ArrayList<ToonsContainer> mList) {
        this.mList = mList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.recycler_item_addnew_review, parent, false);

        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.ButtonFocusedForegroundTransparent, value, true);
        foreground = new ColorDrawable(value.data);

        return new ReviewEntryAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.item = mList.get(position);

        Log.i("DebugLog", "position:\t" + position);
        Log.i("DebugLog", "toon name:\t" + holder.item.toonName);
        Log.i("DebugLog", "toon id:\t" + holder.item.toonID);
        Log.i("DebugLog", "episode id:\t" + holder.item.episodeID);
        Log.i("DebugLog", "release day:\t" + holder.item.releaseWeekdays);

        holder.ToonName.setText(holder.item.toonName);
        holder.ToonID.setText(String.valueOf(holder.item.toonID));
        holder.EpisodeID.setText(String.valueOf(holder.item.episodeID));
        holder.ToonType.setText(String.valueOf(holder.item.toonType));

        holder.Sun.setForeground((holder.item.releaseWeekdays & (1 << ToonsContainer.ReleaseDay.SUN.getValue())) != 0 ? foreground : null);
        holder.Mon.setForeground((holder.item.releaseWeekdays & (1 << ToonsContainer.ReleaseDay.MON.getValue())) != 0 ? foreground : null);
        holder.Tue.setForeground((holder.item.releaseWeekdays & (1 << ToonsContainer.ReleaseDay.TUE.getValue())) != 0 ? foreground : null);
        holder.Wed.setForeground((holder.item.releaseWeekdays & (1 << ToonsContainer.ReleaseDay.WED.getValue())) != 0 ? foreground : null);
        holder.Thu.setForeground((holder.item.releaseWeekdays & (1 << ToonsContainer.ReleaseDay.THU.getValue())) != 0 ? foreground : null);
        holder.Fri.setForeground((holder.item.releaseWeekdays & (1 << ToonsContainer.ReleaseDay.FRI.getValue())) != 0 ? foreground : null);
        holder.Sat.setForeground((holder.item.releaseWeekdays & (1 << ToonsContainer.ReleaseDay.SAT.getValue())) != 0 ? foreground : null);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public void add(String title, String toonType, int toonID, int episodeID, int releaseWeekday, String thumbnailUrlInString) {
        mList.add(new ToonsContainer(
                -1,
                title,
                toonType,
                toonID,
                episodeID,
                releaseWeekday,
                false,
                false,
                thumbnailUrlInString
        ));
        notifyItemInserted(mList.size()-1);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ToonsContainer item;

        EditText ToonName;
        TextView ToonID;
        TextView EpisodeID;
        TextView ToonType;
        MaterialButton Mon, Tue, Wed, Thu, Fri, Sat, Sun;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            ToonName = itemView.findViewById(R.id.etxt_rec_review_title);
            ToonID = itemView.findViewById(R.id.tv_rec_review_toonid);
            EpisodeID = itemView.findViewById(R.id.tv_rec_review_epiid);
            ToonType = itemView.findViewById(R.id.tv_rec_review_toontype);

            Sun = itemView.findViewById(R.id.btn_rec_review_sun);
            Mon = itemView.findViewById(R.id.btn_rec_review_mon);
            Tue = itemView.findViewById(R.id.btn_rec_review_tue);
            Wed = itemView.findViewById(R.id.btn_rec_review_wed);
            Thu = itemView.findViewById(R.id.btn_rec_review_thu);
            Fri = itemView.findViewById(R.id.btn_rec_review_fri);
            Sat = itemView.findViewById(R.id.btn_rec_review_sat);

            Sun.setOnClickListener((v) -> Sun.setForeground(item.tryChangeFlagWeekday((1 << ToonsContainer.ReleaseDay.SUN.getValue())) ? foreground : null));
            Mon.setOnClickListener((v) -> Mon.setForeground(item.tryChangeFlagWeekday((1 << ToonsContainer.ReleaseDay.MON.getValue())) ? foreground : null));
            Tue.setOnClickListener((v) -> Tue.setForeground(item.tryChangeFlagWeekday((1 << ToonsContainer.ReleaseDay.TUE.getValue())) ? foreground : null));
            Wed.setOnClickListener((v) -> Wed.setForeground(item.tryChangeFlagWeekday((1 << ToonsContainer.ReleaseDay.WED.getValue())) ? foreground : null));
            Thu.setOnClickListener((v) -> Thu.setForeground(item.tryChangeFlagWeekday((1 << ToonsContainer.ReleaseDay.THU.getValue())) ? foreground : null));
            Fri.setOnClickListener((v) -> Fri.setForeground(item.tryChangeFlagWeekday((1 << ToonsContainer.ReleaseDay.FRI.getValue())) ? foreground : null));
            Sat.setOnClickListener((v) -> Sat.setForeground(item.tryChangeFlagWeekday((1 << ToonsContainer.ReleaseDay.SAT.getValue())) ? foreground : null));
        }
    }
}
