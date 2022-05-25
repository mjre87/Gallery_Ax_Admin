package com.gallery_ax.admin.bottomsheets;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.gallery_ax.admin.MainActivity;
import com.gallery_ax.admin.PicturesListActivity;
import com.gallery_ax.admin.databinding.BottomSheetFragmentChooseFunctionBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentSnapshot;

public class ChooseFunctionBottomSheetFragment extends BottomSheetDialogFragment {

    private BottomSheetFragmentChooseFunctionBinding binding;

    private MainActivity mainActivity;
    private PicturesListActivity picturesListActivity;
    private final DocumentSnapshot document;
    private final int DOCUMENT_TYPE;
    private final int CATEGORY_TYPE = 0;
    private final int PICTURE_TYPE = 1;

    // سازنده کلاس برای دسته بندی ها
    public ChooseFunctionBottomSheetFragment(MainActivity mainActivity, DocumentSnapshot categoryDocument) {
        this.mainActivity = mainActivity;
        this.document = categoryDocument;
        this.DOCUMENT_TYPE = CATEGORY_TYPE;
    }
    // سازنده کلاس برای تصاویر
    public ChooseFunctionBottomSheetFragment(PicturesListActivity picturesListActivity, DocumentSnapshot pictureDocument) {
        this.picturesListActivity = picturesListActivity;
        this.document = pictureDocument;
        this.DOCUMENT_TYPE = PICTURE_TYPE;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = BottomSheetFragmentChooseFunctionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.editMcv.setOnClickListener(v -> {
            // باز کردن BottomSheet ویرایش بر اساس نوع سند که تصویر باشد یا دسته بندی
            if (DOCUMENT_TYPE == CATEGORY_TYPE) {
                new UpdateDocumentBottomSheetFragment(mainActivity, document)
                        .show(getParentFragmentManager(), "UpdateCategory");
            } else if (DOCUMENT_TYPE == PICTURE_TYPE) {
                new UpdateDocumentBottomSheetFragment(picturesListActivity, document)
                        .show(getParentFragmentManager(), "UpdatePicture");
            }

            dismiss();
        });

        binding.deleteMcv.setOnClickListener(v -> {
            // باز کردن BottomSheet حذف بر اساس نوع سند که تصویر باشد یا دسته بندی
            if (DOCUMENT_TYPE == CATEGORY_TYPE) {
                new DeleteCategoryBottomSheetFragment(mainActivity, document.getId(), document.getString("imageName"))
                        .show(getParentFragmentManager(), "DeleteCategory");
            } else if (DOCUMENT_TYPE == PICTURE_TYPE) {
                new DeletePictureBottomSheetFragment(
                        picturesListActivity,
                        document
                ).show(getParentFragmentManager(), "UpdatePicture");
            }

            dismiss();
        });
    }
}