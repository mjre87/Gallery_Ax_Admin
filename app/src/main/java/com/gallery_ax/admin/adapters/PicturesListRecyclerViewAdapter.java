package com.gallery_ax.admin.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.gallery_ax.admin.PicturesListActivity;
import com.gallery_ax.admin.R;
import com.gallery_ax.admin.bottomsheets.ChooseFunctionBottomSheetFragment;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.List;

public class PicturesListRecyclerViewAdapter extends RecyclerView.Adapter<PictureViewHolder> {

    private final List<DocumentSnapshot> picturesList;
    private final PicturesListActivity activity;

    // Storage
    private final FirebaseStorage storage = FirebaseStorage.getInstance("gs://gallery-ax.appspot.com");
    private final StorageReference storageRef = storage.getReference();

    public PicturesListRecyclerViewAdapter (PicturesListActivity activity, List<DocumentSnapshot> picturesList) {
        this.activity = activity;
        this.picturesList = picturesList;
    }

    @NonNull
    @Override
    public PictureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // ساخت لایه مربوط به هر آیتم
        LayoutInflater inflater = LayoutInflater.from(activity);
        View itemView = inflater.inflate(R.layout.picture_item_layout, parent, false);
        return new PictureViewHolder(itemView, storageRef, activity);
    }

    @Override
    public void onBindViewHolder(@NonNull PictureViewHolder holder, int position) {
        // ارسال اطلاعات به کلاس ViewHolder برای جایگذاری در هر آیتم
        holder.bind(picturesList.get(position), position < 3);
    }

    @Override
    public int getItemCount() {
        return picturesList.size();
    }
}

class PictureViewHolder extends RecyclerView.ViewHolder {

    private final PicturesListActivity activity;
    private final Context context;
    private final StorageReference storageRef;
    private final ConstraintLayout mainFrame;
    private final ImageView pictureIv;
    private final MaterialCardView moreFunctionCard;

    public PictureViewHolder(View itemView, StorageReference storageRef, PicturesListActivity activity) {
        super(itemView);

        this.activity = activity;
        context = itemView.getContext();
        this.storageRef = storageRef;
        // اتصال المان های لایه آیتم به کد جاوا
        mainFrame = itemView.findViewById(R.id.picture_item_main_layout);
        pictureIv = itemView.findViewById(R.id.picture_item_iv);
        moreFunctionCard = itemView.findViewById(R.id.picture_item_more_mcv);
    }

    public void bind(DocumentSnapshot pictureDocument, boolean isTopThreeItem) {
        // تعیین فاصله هر آیتم از اطراف بر اساس جایگاه آن در لیست
        if (isTopThreeItem) {
            mainFrame.setPadding(0, 58, 0, 0);
        } else {
            mainFrame.setPadding(0, 0, 0, 0);
        }

        // لود کردن تصویر و قراردادن آن در آیتم
        StorageReference imageRef =
                storageRef.child("pictures/" + pictureDocument.getString("categoryId") + "/" + pictureDocument.getString("imageName"));
        Glide.with(context).load(imageRef).into(pictureIv);

        // باز کردن bottomSheet حذف/ویرایش با کلیک روز سه نقطه
        moreFunctionCard.setOnClickListener(v -> new ChooseFunctionBottomSheetFragment(activity, pictureDocument)
                .show(activity.getSupportFragmentManager(), "ChooseFunctionForPicture"));
    }
}
