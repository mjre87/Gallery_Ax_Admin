package com.gallery_ax.admin.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.gallery_ax.admin.MainActivity;
import com.gallery_ax.admin.PicturesListActivity;
import com.gallery_ax.admin.R;
import com.gallery_ax.admin.bottomsheets.ChooseFunctionBottomSheetFragment;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class CategoriesRecyclerViewAdapter extends RecyclerView.Adapter<CategoryItemViewHolder> {

    private final MainActivity activity;
    private final List<DocumentSnapshot> categories;

    // Storage
    private final FirebaseStorage storage = FirebaseStorage.getInstance("gs://gallery-ax.appspot.com");
    private final StorageReference storageRef = storage.getReference();

    public CategoriesRecyclerViewAdapter(MainActivity activity, List<DocumentSnapshot> categories) {
        this.activity = activity;
        this.categories = categories;
    }

    @NonNull
    @Override
    public CategoryItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // ساخت لایه مربوط به هر آیتم
        LayoutInflater inflater = LayoutInflater.from(activity);
        View itemView = inflater.inflate(R.layout.category_item_layout, parent, false);
        return new CategoryItemViewHolder(itemView, storageRef, activity);
    }

    @Override
    public void onBindViewHolder(CategoryItemViewHolder holder, int position) {
        // ارسال اطلاعات به کلاس ViewHolder برای جایگذاری در هر آیتم
        holder.bind(categories.get(position), position == 0, position == (getItemCount() - 1));
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }
}

class CategoryItemViewHolder extends RecyclerView.ViewHolder {

    private final Context context;
    private final MainActivity activity;
    private final ConstraintLayout mainFrame;
    private final MaterialCardView mainMcv;
    private final TextView titleTv;
    private final ImageView imageViewIv;
    private final StorageReference storageRef;
    private final MaterialCardView moreFunctionCard;

    public CategoryItemViewHolder(View itemView, StorageReference storageRef, MainActivity activity) {
        super(itemView);
        this.storageRef = storageRef;
        this.activity = activity;
        context = itemView.getContext();
        // اتصال المان های لایه آیتم به کد جاوا
        mainFrame = itemView.findViewById(R.id.category_item_main_layout_fl);
        mainMcv = itemView.findViewById(R.id.categories_item_mcv);
        titleTv = itemView.findViewById(R.id.category_item_title_tv);
        imageViewIv = itemView.findViewById(R.id.category_item_image_iv);
        moreFunctionCard = itemView.findViewById(R.id.category_item_more_mcv);
    }

    public void bind(DocumentSnapshot categoryDocument, boolean isFirstItem, boolean isLastItem) {
        // تعیین فاصله هر آیتم از اطراف بر اساس جایگاه آن در لیست
        if (isFirstItem) {
            mainFrame.setPadding(0, 58, 0, 0);
        } else if (isLastItem) {
            mainFrame.setPadding(0, 0, 0, 58);
        } else {
            mainFrame.setPadding(0, 0, 0, 0);
        }

        // قرار دادن اطلاعات مربوط به آیتم در لایه آن
        titleTv.setText(categoryDocument.getString("title"));
        loadImage(categoryDocument.getString("imageName"));

        mainMcv.setOnClickListener(v -> {
            // هدایت کاربر به صفحه لیست تصاویر با کلیک روی هر آیتم
            Intent intent = new Intent(context, PicturesListActivity.class);
            intent.putExtra("categoryId", categoryDocument.getId());
            intent.putExtra("categoryTitle", categoryDocument.getString("title"));
            context.startActivity(intent);
        });

        // باز کردن bottomSheet حذف/ویرایش با کلیک روز سه نقطه
        moreFunctionCard.setOnClickListener(v -> new ChooseFunctionBottomSheetFragment(activity, categoryDocument)
                .show(activity.getSupportFragmentManager(), "ChooseFunctionForCategory"));
    }

    private void loadImage(String imageName) {
        // لود کردن تصویر
        StorageReference imageRef = storageRef.child("categories_image/" + imageName);
        Glide.with(context).load(imageRef).into(imageViewIv);
    }
}
