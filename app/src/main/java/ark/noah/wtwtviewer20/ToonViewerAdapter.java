package ark.noah.wtwtviewer20;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;
import java.util.Arrays;

public class ToonViewerAdapter extends BaseAdapter {
    ArrayList<ToonViewerContainer> mData;
    ArrayList<Bitmap> imageData;
    LayoutInflater mLayoutInflater;

    Drawable ic_loading, ic_error, ic_empty;

    @SuppressLint("UseCompatLoadingForDrawables")
    ToonViewerAdapter(ArrayList<ToonViewerContainer> list, Context context) {
        mData = list;
        imageData = new ArrayList<>(Arrays.asList(new Bitmap[list.size()]));
        mLayoutInflater = LayoutInflater.from(context);

        ic_loading = context.getDrawable(R.drawable.ic_baseline_downloading_24);
        ic_empty   = context.getDrawable(R.drawable.ic_baseline_device_unknown_24);
        ic_error   = context.getDrawable(R.drawable.ic_baseline_error_outline_24);

        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.textColorSecondary, value, true);
        BlendModeColorFilter greyColorFilter = new BlendModeColorFilter(value.data, BlendMode.SRC_ATOP);

        ic_loading.setColorFilter(greyColorFilter);
        ic_empty.setColorFilter(greyColorFilter);
        ic_error.setColorFilter(greyColorFilter);
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder viewHolder;

        if(view == null) {
            view = mLayoutInflater.inflate(R.layout.list_viewer_item, null);
            viewHolder = new ViewHolder(view);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        if(mData.get(position).loadedImageBitmap == null) {
            Glide.with(view.getContext())
                 .asBitmap()
                 .load(mData.get(position).imageURL)
                 .placeholder(ic_loading)
                 .error(ic_error)
                 .fallback(ic_empty)
                 .into(new CustomTarget<Bitmap>() {
                     @Override
                     public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                         mData.get(position).loadedImageBitmap = resource;
                         viewHolder.imageView.setImageBitmap(resource);
                     }
                     @Override
                     public void onLoadCleared(@Nullable Drawable placeholder) { }
                 }
            );
        } else {
            viewHolder.imageView.setImageBitmap(mData.get(position).loadedImageBitmap);
        }

        return view;
    }

    public static class ViewHolder {
        ImageView imageView;

        ViewHolder(View itemView) {
            imageView = itemView.findViewById(R.id.img_viewerslide);
        }
    }
}
