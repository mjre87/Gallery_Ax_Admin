package com.gallery_ax.admin.bottomsheets;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.gallery_ax.admin.MainActivity;
import com.gallery_ax.admin.databinding.BottomSheetFragmentDeleteCategoryBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class DeleteCategoryBottomSheetFragment extends BottomSheetDialogFragment {

    private BottomSheetFragmentDeleteCategoryBinding binding;

    private final MainActivity mainActivity;
    private final String categoryId;
    private final String imageName;

    // FireStore
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    // Storage
    private final FirebaseStorage storage = FirebaseStorage.getInstance("gs://gallery-ax.appspot.com");

    public DeleteCategoryBottomSheetFragment(MainActivity activity, String categoryId, String imageName) {
        this.mainActivity = activity;
        this.categoryId = categoryId;
        this.imageName = imageName;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = BottomSheetFragmentDeleteCategoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.removeButton.setOnClickListener(v -> {
            showLoadingLayout();
            deleteAllCategoriesImageFromStorage();
        });

        binding.cancelButton.setOnClickListener(v -> dismiss());
    }

    // 1. حذف همه تصاویر دسته بندی از Storage
    private void deleteAllCategoriesImageFromStorage() {
        // تغییر وضعیت
        binding.statusTv.setText("درحال حذف تمام تصاویر دسته بندی از Storage");
        storage.getReference().child("pictures/" + categoryId + "/")
                .listAll()
                .addOnSuccessListener(listResult -> {
                    for (StorageReference itemRef : listResult.getItems()) {
                        itemRef.delete().addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) {
                                Toast.makeText(requireContext(), "در حذف تمام تصاویر دسته بندی مشکلی پیش آمده است :(", Toast.LENGTH_SHORT).show();
                                hideLoadingLayout();
                            }
                        });
                    }

                    deleteAllCategoriesImageFromFirestore();
                });
    }

    // 2. حذف همه تصاویر دسته بندی از دیتابیس
    private void deleteAllCategoriesImageFromFirestore() {
        // تغییر وضعیت
        binding.statusTv.setText("درحال حذف تمام تصاویر دسته بندی از پایگاه داده");
        db.collection("pictures")
                .whereEqualTo("categoryId", categoryId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            deleteImageFromFirestore(doc.getId());
                        }

                        deleteCategoryFromStorage();
                    }
                });
    }

    private void deleteImageFromFirestore(String imageId) {
        db.collection("pictures").document(imageId)
                .delete()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(requireContext(), "در حذف تمام تصاویر دسته بندی مشکلی پیش آمده است :(", Toast.LENGTH_SHORT).show();
                        hideLoadingLayout();
                    }
                });
    }

    // 3. حذف تصویر دسته بندی از Storage
    private void deleteCategoryFromStorage() {
        // تغییر وضعیت
        binding.statusTv.setText("درحال حذف تصویر دسته بندی از Storage");
        storage.getReference().child("categories_image/" + imageName)
                .delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        deleteCategoryFromFirestore();
                    } else {
                        Toast.makeText(requireContext(), "در حذف تصویر دسته بندی مشکلی پیش آمده است :(", Toast.LENGTH_SHORT).show();
                        hideLoadingLayout();
                    }
                });
    }

    // 4. حذف دسته بندی از دیتابیس
    private void deleteCategoryFromFirestore() {
        // تغییر وضعیت
        binding.statusTv.setText("درحال حذف دسته بندی از پایگاه داده");
        db.collection("categories").document(categoryId)
                .delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // تازه سازی لیست دسته بندی ها
                        mainActivity.refreshCategoriesList();
                        Toast.makeText(requireContext(), "دسته بندی با موفقیت حذف شد ;)", Toast.LENGTH_SHORT).show();
                        if (isResumed()) dismiss();
                    } else {
                        Toast.makeText(requireContext(), "در حذف دسته بندی مشکلی پیش آمده است :(", Toast.LENGTH_SHORT).show();
                        hideLoadingLayout();
                    }
                });
    }

    // نمایش یا مخفی کردن لودینگ
    private void showLoadingLayout() {
        binding.deleteCategoryMainLayout.animate().alpha(0f).setDuration(500).withEndAction(() -> {
            binding.deleteCategoryMainLayout.setVisibility(View.GONE);
        }).start();
        binding.progressLayout.animate().alpha(1f).setDuration(500).setStartDelay(500).start();
    }
    private void hideLoadingLayout() {
        binding.progressLayout.animate().alpha(1f).setDuration(500).start();
        binding.deleteCategoryMainLayout.animate().alpha(0f).setDuration(500).setStartDelay(500).withStartAction(() -> {
            binding.deleteCategoryMainLayout.setVisibility(View.VISIBLE);
        }).start();
    }
}