package ark.noah.wtwtviewer20;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class ReviewEntryAdapter extends RecyclerView.Adapter<ReviewEntryAdapter.ViewHolder> {
    ArrayList<ToonsContainer> mList;

    private Drawable foreground;

    Drawable ic_loading, ic_error, ic_empty;

    public ReviewEntryAdapter(ArrayList<ToonsContainer> mList) {
        this.mList = mList;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.recycler_item_addnew_review, parent, false);

        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.ButtonFocusedForegroundTransparent, value, true);
        foreground = new ColorDrawable(value.data);

        ic_loading = context.getDrawable(R.drawable.ic_baseline_downloading_24);
        ic_empty   = context.getDrawable(R.drawable.ic_baseline_device_unknown_24);
        ic_error   = context.getDrawable(R.drawable.ic_baseline_error_outline_24);

        TypedValue value2 = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.textColorSecondary, value2, true);
        BlendModeColorFilter greyColorFilter = new BlendModeColorFilter(value2.data, BlendMode.SRC_ATOP);

        ic_loading.setColorFilter(greyColorFilter);
        ic_empty.setColorFilter(greyColorFilter);
        ic_error.setColorFilter(greyColorFilter);

        return new ReviewEntryAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.item = mList.get(position);

        Glide.with(holder.Thumbnail)
                .load(holder.item.thumbnailURL)
                .placeholder(ic_loading)
                .error(ic_error)
                .fallback(ic_empty)
                .into(holder.Thumbnail);

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

        ImageView Thumbnail;
        EditText ToonName;
        TextView ToonID;
        TextView EpisodeID;
        TextView ToonType;
        MaterialButton Mon, Tue, Wed, Thu, Fri, Sat, Sun;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            Thumbnail = itemView.findViewById(R.id.img_rec_review_thumbnail);
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

            ToonName.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
                @Override
                public void afterTextChanged(Editable editable) {
                    Context context = itemView.getContext().getApplicationContext();
                    String testTitle = editable.toString();
                    if(testTitle.equals("")) Toast.makeText(context, context.getString(R.string.txt_invalid_title), Toast.LENGTH_SHORT).show();
                    else item.toonName = testTitle;
                }
            });

            Sun.setOnClickListener((v) -> highlightButton(v, ToonsContainer.ReleaseDay.SUN));
            Mon.setOnClickListener((v) -> highlightButton(v, ToonsContainer.ReleaseDay.MON));
            Tue.setOnClickListener((v) -> highlightButton(v, ToonsContainer.ReleaseDay.TUE));
            Wed.setOnClickListener((v) -> highlightButton(v, ToonsContainer.ReleaseDay.WED));
            Thu.setOnClickListener((v) -> highlightButton(v, ToonsContainer.ReleaseDay.THU));
            Fri.setOnClickListener((v) -> highlightButton(v, ToonsContainer.ReleaseDay.FRI));
            Sat.setOnClickListener((v) -> highlightButton(v, ToonsContainer.ReleaseDay.SAT));
        }

        private void highlightButton(View view, ToonsContainer.ReleaseDay releaseDay) {
            view.setForeground(item.tryChangeFlagWeekday((1 << releaseDay.getValue())) ? foreground : null);
        }
    }
}
