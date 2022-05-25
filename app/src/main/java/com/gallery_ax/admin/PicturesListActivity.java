package com.gallery_ax.admin;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.gallery_ax.admin.adapters.PicturesListRecyclerViewAdapter;
import com.gallery_ax.admin.bottomsheets.CreateNewDocumentBottomSheetFragment;
import com.gallery_ax.admin.databinding.ActivityPicturesListBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.function.Function;

public class PicturesListActivity extends AppCompatActivity {

    private ActivityPicturesListBinding binding;
    private String categoryId;

    // FireStore
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // تازه سازی لیست تصاویر
    public void refreshPicturesList() { getPicturesDocument(); }

    // این تابع زمانی که کاربر تصویر را انتخاب میکند اجرا میشود تا مسیر را به BottomSheet ها بدهد
    private Function<Uri, Boolean> onImageSelected;
    public void setOnImageSelected(Function<Uri, Boolean> onImageSelected) {
        this.onImageSelected = onImageSelected;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPicturesListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // تعیین نوع چینش ایتم های RecyclerView
        binding.picturesListRv.setLayoutManager(new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false));

        Bundle bundle = getIntent().getExtras();
        // قراردادن عنوان دسته بندی در عنوان صفحه
        binding.categoryTitle.setText(getString(R.string.pictures_list_page_title, bundle.getString("categoryTitle")));
        // ذخیره id دسته بندی
        categoryId = bundle.getString("categoryId");

        // ارسال درخواست گرفتن تصویر دسته بندی
        getPicturesDocument();

        binding.backButtonIb.setOnClickListener(v -> finish());
        binding.floatingAddPictureButton.setOnClickListener(v -> {
            // چک کردن اجازه دسترسی به فایل ها
            if (isPermissionGranted()) {
                // باز کردن BottomSheet اضافه کردن تصویر جدید
                new CreateNewDocumentBottomSheetFragment(this, categoryId).show(getSupportFragmentManager(), "NewPictureBottomSheet");
            } else {
                // درخواست اجازه دسترسی به فایل ها از کاربر
                takePermission();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.FILE_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                // ارسال مسیر تصویر به BottomSheet
                onImageSelected.apply(data.getData());
            } else {
                Toast.makeText(this, "امکان بازکردن فایل انتخابی وجود ندارد :(", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getPicturesDocument() {
        // مخفی کردن RecyclerView و نمایش ProgressBar
        binding.picturesListRv.setVisibility(View.GONE);
        binding.picturesListPb.setVisibility(View.VISIBLE);
        // گرفتن دسته بندی ها
        db.collection("pictures")
                .whereEqualTo("categoryId", categoryId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // مخفی کردن ProgressBar و نمایش RecyclerView
                        binding.picturesListPb.setVisibility(View.GONE);
                        binding.picturesListRv.setVisibility(View.VISIBLE);

                        List<DocumentSnapshot> result = task.getResult().getDocuments();
                        // چک کردن تعداد تصاویر دسته بندی
                        if (result.size() > 0) {
                            // مخفی کردن پیغام خالی بودن لیست
                            binding.emptyPicturesListLl.setVisibility(View.GONE);
                            // تعیین آداپتور RecyclerView
                            binding.picturesListRv.setAdapter(new PicturesListRecyclerViewAdapter(this, result));
                        } else {
                            // نمایش پیغام خالی بودن لیست و مخفی کردن RecyclerView
                            binding.picturesListRv.setVisibility(View.GONE);
                            binding.emptyPicturesListLl.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Log.e("GetPicturesDocs", task.getException().getMessage());
                        Toast.makeText(this, "خطایی رخ داده است.", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    e.printStackTrace();
                    Toast.makeText(this, "خطایی رخ داده است.", Toast.LENGTH_SHORT).show();
                });
    }

    // چک کردن اینکه ایا قبلا کاربر اجازه دسترسی به فایل ها داده یا نه
    private boolean isPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            // برای اندروید 11 به بالا
            return Environment.isExternalStorageManager();
        } else {
            // برای اندروید 10 به پائین
            return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }
    // گرفتن اجازه دسترسی به فایلها از کاربر
    private void takePermission(){
        // برای اندروید 11 به بالا
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            try {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                startActivityForResult(intent, Constants.READ_EXTERNAL_PERMISSION_REQUEST_CODE);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, Constants.READ_EXTERNAL_PERMISSION_REQUEST_CODE);
            }
        } else {
            // برای اندروید 10 به پائین
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.READ_EXTERNAL_PERMISSION_REQUEST_CODE);
        }
    }

}