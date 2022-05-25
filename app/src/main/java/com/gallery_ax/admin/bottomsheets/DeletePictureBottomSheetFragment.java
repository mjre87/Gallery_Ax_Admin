package com.gallery_ax.admin.bottomsheets;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.gallery_ax.admin.PicturesListActivity;
import com.gallery_ax.admin.databinding.BottomSheetFragmentDeletePictureBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

public class DeletePictureBottomSheetFragment extends BottomSheetDialogFragment {

    private BottomSheetFragmentDeletePictureBinding binding;

    private final PicturesListActivity activity;
    private final String categoryId;
    private final String pictureId;
    private final String imageName;

    // FireStore
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    // Storage
    private final FirebaseStorage storage = FirebaseStorage.getInstance("gs://gallery-ax.appspot.com");

    public DeletePictureBottomSheetFragment(PicturesListActivity activity, DocumentSnapshot pictureDocument) {
        this.activity = activity;
        this.categoryId = pictureDocument.getString("categoryId");
        this.pictureId = pictureDocument.getId();
        this.imageName = pictureDocument.getString("imageName");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = BottomSheetFragmentDeletePictureBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.cancelButton.setOnClickListener(v -> dismiss());
        binding.deleteButton.setOnClickListener(v -> {
            showLoadingLayout();
            deletePictureFromStorage();
        });
    }

    // 1. حذف تصویر از Storage
    private void deletePictureFromStorage() {
        // تغییر وضعیت
        binding.statusTv.setText("درحال حذف تصویر از Storage");
        storage.getReference().child("pictures/" + categoryId + "/" + imageName)
                .delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        deletePictureFromFirestore();
                    } else {
                        Toast.makeText(requireContext(), "در حذف تصویر مشکلی پیش آمده است :(", Toast.LENGTH_SHORT).show();
                        hideLoadingLayout();
                    }
                }).addOnFailureListener(e -> e.printStackTrace());
    }

    // 2. حذف تصویر از دیتابیس
    private void deletePictureFromFirestore() {
        // تغییر وضعیت
        binding.statusTv.setText("درحال حذف تصویر از پایگاه داده");
        db.collection("pictures").document(pictureId)
                .delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), "تصویر با موفقیت حذف شد ;)", Toast.LENGTH_SHORT).show();
                        activity.refreshPicturesList();
                        if (isResumed()) dismiss();
                    } else {
                        Toast.makeText(requireContext(), "در حذف تصویر مشکلی پیش آمده است :(", Toast.LENGTH_SHORT).show();
                        hideLoadingLayout();
                    }
                }).addOnFailureListener(e -> e.printStackTrace());
    }

    // نمایش یا مخفی کردن لودینگ
    private void showLoadingLayout() {
        binding.deletePictureMainLayout.animate().alpha(0f).setDuration(500).withEndAction(() -> {
            binding.deletePictureMainLayout.setVisibility(View.GONE);
        }).start();
        binding.progressLayout.animate().alpha(1f).setDuration(500).setStartDelay(500).start();
    }
    private void hideLoadingLayout() {
        binding.progressLayout.animate().alpha(1f).setDuration(500).start();
        binding.deletePictureMainLayout.animate().alpha(0f).setDuration(500).setStartDelay(500).withStartAction(() -> {
            binding.deletePictureMainLayout.setVisibility(View.VISIBLE);
        }).start();
    }
}