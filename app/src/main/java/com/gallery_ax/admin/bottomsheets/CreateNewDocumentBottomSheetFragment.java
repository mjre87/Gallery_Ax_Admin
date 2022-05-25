package com.gallery_ax.admin.bottomsheets;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.gallery_ax.admin.Constants;
import com.gallery_ax.admin.MainActivity;
import com.gallery_ax.admin.PicturesListActivity;
import com.gallery_ax.admin.R;
import com.gallery_ax.admin.databinding.BottomSheetFragmentNewDocumentBinding;
import com.gallery_ax.admin.modules.AspectRatio;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.zxy.tiny.Tiny;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateNewDocumentBottomSheetFragment extends BottomSheetDialogFragment {

    private BottomSheetFragmentNewDocumentBinding binding;
    private MainActivity mainActivity;
    private PicturesListActivity picturesListActivity;
    private final String categoryId;
    private File croppedImageFile;
    private File compressedImageFile;

    private boolean isImageCropperFilled = false;
    private final int DOCUMENT_TYPE;
    private final int CATEGORY_TYPE = 0;
    private final int PICTURE_TYPE = 1;
    private final String[] firebaseStoragePath = {"categories_image/", "pictures/"};
    private final String[] firebaseCollectionPath = {"categories", "pictures"};

    // FireStore
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    // Storage
    private final FirebaseStorage storage = FirebaseStorage.getInstance("gs://gallery-ax.appspot.com");

    // سازنده کلاس برای دسته بندی ها
    public CreateNewDocumentBottomSheetFragment(MainActivity activity) {
        this.mainActivity = activity;
        this.categoryId = "NULL";
        this.DOCUMENT_TYPE = CATEGORY_TYPE;
    }
    // سازنده کلاس برای تصاویر
    public CreateNewDocumentBottomSheetFragment(PicturesListActivity activity, String categoryId) {
        this.picturesListActivity = activity;
        this.categoryId = categoryId;
        this.DOCUMENT_TYPE = PICTURE_TYPE;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = BottomSheetFragmentNewDocumentBinding.inflate(inflater, container, false);
        getDialog().setOnShowListener(dialogInterface -> setupFullHeight((BottomSheetDialog) dialogInterface));
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // تعیین عنوان و متن راهنمای EditText با توجه به نوع سند که تصویر باشد یا دسته بندی
        if (DOCUMENT_TYPE == CATEGORY_TYPE) {
            binding.newDocumentTitleTv.setText("اضافه کردن دسته بندی جدید");
            binding.documentTitleEt.setHint("عنوان دسته بندی");
        } else {
            binding.newDocumentTitleTv.setText("اضافه کردن تصویر جدید");
            binding.documentTitleEt.setHint("عنوان تصویر");
        }

        binding.loadNewImageIb.setOnClickListener(v -> {
            // هدایت کاریر به صفحه انتخاب فایل با کلیک روی دکمه لود تصویر
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            getActivity().startActivityForResult(intent, Constants.FILE_PICKER_REQUEST_CODE);
        });

        // قرار دادن تابع onImageSelected به عنوان شنونده تا مسیر تصویر انتخاب شده را به این BottomSheet برگرداند
        if (DOCUMENT_TYPE == CATEGORY_TYPE) {
            mainActivity.setOnImageSelected(this::onImageSelected);
        } else if (DOCUMENT_TYPE == PICTURE_TYPE) {
            picturesListActivity.setOnImageSelected(this::onImageSelected);
        }

        binding.cancelButton.setOnClickListener(v -> dismiss());
        binding.addButton.setOnClickListener(v -> {
            // چک کردن ورودی ها
            if (isAllInputFilled()) {
                // اگر همه ورودی ها پر شده بود عملیات اضافه کردن آغاز شود
                showLoadingLayout();
                compressCroppedImage();
            }
        });
    }

    // برای تمام صفحه کردن BottomSheet
    private void setupFullHeight(BottomSheetDialog bottomSheetDialog) {
        View bottomSheet = bottomSheetDialog.findViewById(R.id.design_bottom_sheet);
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
        ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();

        // تمام صفحه کردن BottomSheet
        if (layoutParams != null) layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        bottomSheet.setLayoutParams(layoutParams);
        // باز کردن آن به صورت کامل
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        // خاموش کردن قابلیت جابجایی آن
        behavior.setDraggable(false);
    }

    // 1. گرفتن مسیر تصویر و لود کردن آن در ImageCropper
    private boolean onImageSelected(Uri uri) {
        loadImageFileToCropper(uri);
        return true;
    }

    private void loadImageFileToCropper(Uri uri) {
        // ذخیره ابعاد Cropper بر اساس نوع سند که تصویر باشد یا دسته بندی
        AspectRatio aspectRatio = getImageCropperAspectRatio();
        // لود کردن تصویر در ImageCropper
        binding.imageCropper.of(uri)
                .withAspect(aspectRatio.x, aspectRatio.y)
                .initialize(requireContext());

        isImageCropperFilled = true;
    }

    private AspectRatio getImageCropperAspectRatio() {
        // گرفتن ابعاد Cropper بر اساس نوع سند که تصویر باشد یا دسته بندی
        if (DOCUMENT_TYPE == CATEGORY_TYPE) {
            return new AspectRatio(3, 1);
        } else {
            return new AspectRatio(9, 16);
        }
    }

    // 2. چک کردن فیلد های ورودی
    private boolean isAllInputFilled() {
        String nameText = binding.documentTitleEt.getText().toString().trim();
        return (nameText.length() > 0 && isImageCropperFilled);
    }

    // 3. فشرده سازی تصویر Crop شده
    private File getCroppedImageFile() {
        try {
            // ساخت پوشه برای فایل های Crop شده
            File croppedImagesDirectory = new File(
                    Environment.getExternalStorageDirectory(),
                    "Pictures/Gallery Ax/admin/cropped_images"
            );
            if (!croppedImagesDirectory.exists()) croppedImagesDirectory.mkdirs();

            // ساخت فایل موقتی برای ذخیره تصویر Crop شده
            croppedImageFile = new File(croppedImagesDirectory.getAbsolutePath(), "ci_temp.png");
            croppedImageFile.createNewFile();

            // تبدیل bitmap به ByteArray
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            binding.imageCropper.getOutput().compress(Bitmap.CompressFormat.PNG, 100, bos);
            byte[] bimapBytes = bos.toByteArray();

            // ذخیره تصویر Crop شده تا بتوان آنرا فشده کرد
            FileOutputStream fos = new FileOutputStream(croppedImageFile);
            fos.write(bimapBytes);
            fos.flush();
            fos.close();

            return croppedImageFile;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "مشکلی با وارد کردن فایل پیش اومده.", Toast.LENGTH_SHORT).show();
        }

        return null;
    }

    private void compressCroppedImage() {
        // تغییر وضعیت
        binding.statusTv.setText("درحال فشرده سازی تصویر");
        // فشرده کردن تصویر
        Tiny.getInstance()
                .source(getCroppedImageFile()).asFile()
                .withOptions(new Tiny.FileCompressOptions())
                .compress((isSuccess, outfile, throwable) -> {
                    if (isSuccess) {
                        compressedImageFile = new File(outfile);
                        uploadImage(Uri.fromFile(compressedImageFile));
                    } else {
                        Toast.makeText(requireContext(), "مشکلی با فشرده سازی تصویر پیش اومده.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 4. آپلود تصویر
    private void uploadImage(Uri compressedImageUri) {
        // تغییر وضعیت
        binding.statusTv.setText("درحال آپلود تصویر");
        // ساخت نام یکتا برای تصویر
        String imageName = UUID.randomUUID().toString()+".png";
        // اگر سند از نوع تصویر باشد آنرا در پوشه مربوط به دسته بندی آپلود میکنیم
        String categoryFolder = (categoryId.equals("NULL"))? "" : (categoryId + "/");
        StorageReference imagesRef = storage.getReference()
                .child(firebaseStoragePath[DOCUMENT_TYPE] + categoryFolder + imageName);

        UploadTask uploadTask = imagesRef.putFile(compressedImageUri);
        uploadTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // اگر عملیات موفق بود عملیات  اضافه کردن به دیتابیس اغاز میشود
                addToFirestore(imageName);
                // و تصاویر موقتی حذف میشوند
                deleteTempFiles();
            } else {
                Toast.makeText(requireContext(), "در آپلود تصویر مشکلی بوجود آمده است :(", Toast.LENGTH_SHORT).show();
                hideLoadingLayout();
            }
        });
    }

    // 5. حذف تصاویر موقتی
    private void deleteTempFiles() {
        if (compressedImageFile.exists()) compressedImageFile.delete();
        if (croppedImageFile.exists()) croppedImageFile.delete();
    }

    // 6. ثبت سند در دیتابیس
    private void addToFirestore(String imageName) {
        // تغییر وضعیت
        binding.statusTv.setText("درحال اضافه کردن به پایگاه داده");
        // ساخت سند برای ذخیره
        String documentId = getDocumentId();
        Map<String, Object> document = new HashMap<>();
        document.put("id", documentId);
        document.put("title", binding.documentTitleEt.getText().toString().trim());
        document.put("imageName", imageName);
        // اگر سند از نوع تصویر بود id دسته بندی، تعداد لایک ها و لیست علاقه مندان هم به سند اضافه میشود
        if (DOCUMENT_TYPE == PICTURE_TYPE) {
            document.put("categoryId", categoryId);
            document.put("likes", 0);
            document.put("lovers", new ArrayList<String>());
        }

        // ثبت در دیتابیس
        db.collection(firebaseCollectionPath[DOCUMENT_TYPE])
                .document(documentId)
                .set(document)
                .addOnSuccessListener(voi_d -> {
                    // بر اساس نوع سند که دسته بندی باشد یا تصویر لیست را تازه سازی میکنیم
                    if (DOCUMENT_TYPE == CATEGORY_TYPE) {
                        mainActivity.refreshCategoriesList();
                    } else if (DOCUMENT_TYPE == PICTURE_TYPE) {
                        picturesListActivity.refreshPicturesList();
                    }
                    Toast.makeText(requireContext(), "با موفقیت اضافه شد ;)", Toast.LENGTH_SHORT).show();
                    if (isResumed()) dismiss();
                })
                .addOnFailureListener(e -> {
                    hideLoadingLayout();
                    Toast.makeText(requireContext(), "در اضافه کردن به پایگاه داده مشکلی پیش آمده است :(", Toast.LENGTH_SHORT).show();
                    Log.i("IMAGE_ADD", e.getMessage());
                });
    }

    // ساخت id یکتا بر اساس تارخ فعلی
    private String getDocumentId() {
        return String.valueOf(new Date().getTime());
    }

    // نمایش یا مخفی کردن لودینگ
    private void showLoadingLayout() {
        binding.newDocumentMainLayout.animate().alpha(0f).setDuration(500).withEndAction(() -> {
            binding.newDocumentMainLayout.setVisibility(View.GONE);
        }).start();
        binding.progressLayout.animate().alpha(1f).setDuration(500).setStartDelay(500).start();
    }
    private void hideLoadingLayout() {
        binding.progressLayout.animate().alpha(1f).setDuration(500).start();
        binding.newDocumentMainLayout.animate().alpha(0f).setDuration(500).setStartDelay(500).withStartAction(() -> {
            binding.newDocumentMainLayout.setVisibility(View.VISIBLE);
        }).start();
    }
}