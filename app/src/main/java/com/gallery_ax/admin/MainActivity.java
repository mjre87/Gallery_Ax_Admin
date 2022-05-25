package com.gallery_ax.admin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.gallery_ax.admin.adapters.CategoriesRecyclerViewAdapter;
import com.gallery_ax.admin.bottomsheets.CreateNewDocumentBottomSheetFragment;
import com.gallery_ax.admin.databinding.ActivityMainBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.function.Function;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    // FireStore
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // تازه سازی لیست دسته بندی ها
    public void refreshCategoriesList() { getCategoriesDocument(); }

    // این تابع زمانی که کاربر تصویر را انتخاب میکند اجرا میشود تا مسیر را به BottomSheet ها بدهد
    private Function<Uri, Boolean> onImageSelected;
    public void setOnImageSelected(Function<Uri, Boolean> onImageSelected) {
        this.onImageSelected = onImageSelected;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // تعیین نوع چینش ایتم های RecyclerView
        binding.categoriesListRv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        // ارسال درخواست گرفتن دسته بندی ها
        getCategoriesDocument();

        binding.floatingAddButton.setOnClickListener(v -> {
            // چک کردن اجازه دسترسی به فایل ها
            if (isPermissionGranted()) {
                // باز کردن BottomSheet اضافه کردن دسته بندی جدید
                new CreateNewDocumentBottomSheetFragment(this).show(getSupportFragmentManager(), "NewCategory");
            } else {
                // درخواست اجازه دسترسی به فایل ها از کاربر
                takePermission();
            }
        });
    }

    private void getCategoriesDocument() {
        // مخفی کردن RecyclerView و نمایش ProgressBar
        binding.categoriesListRv.setVisibility(View.GONE);
        binding.categoriesListPb.setVisibility(View.VISIBLE);
        // گرفتن دسته بندی ها
        db.collection("categories").get()
                .addOnCompleteListener(task -> {
                    // اگر موفق بود
                    if (task.isSuccessful()) {
                        // مخفی کردن ProgressBar و نمایش RecyclerView
                        binding.categoriesListPb.setVisibility(View.GONE);
                        binding.categoriesListRv.setVisibility(View.VISIBLE);

                        List<DocumentSnapshot> result = task.getResult().getDocuments();
                        // چک کردن تعداد دسته بندی ها
                        if (result.size() > 0) {
                            // مخفی کردن پیغام خالی بودن لیست
                            binding.emptyCategoriesListLl.setVisibility(View.GONE);
                            // تعیین آداپتور RecyclerView
                            binding.categoriesListRv.setAdapter(new CategoriesRecyclerViewAdapter(this, result));
                        } else {
                            // نمایش پیغام خالی بودن لیست و مخفی کردن RecyclerView
                            binding.categoriesListRv.setVisibility(View.GONE);
                            binding.emptyCategoriesListLl.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Toast.makeText(this, "خطایی رخ داده است.", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    e.printStackTrace();
                    Toast.makeText(this, "خطایی رخ داده است.", Toast.LENGTH_SHORT).show();
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
    private void takePermission() {
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