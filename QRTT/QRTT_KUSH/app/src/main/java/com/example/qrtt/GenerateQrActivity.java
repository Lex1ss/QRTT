package com.example.qrtt;

import static androidx.camera.core.CameraXThreads.TAG;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.UUID;

public class GenerateQrActivity extends AppCompatActivity {

    private ImageView qrCodeImageView;
    private FloatingActionButton menuButton;
    private DrawerLayout drawerLayout; // DrawerLayout — контейнер для отображения бокового меню
    private NavigationView navigationView;
    private DatabaseReference databaseReference;
    private TextView qr_TimeTracker;

    private MainViewModel viewModel;

    private static final String ADMIN_PASSWORD = "admin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "User is not authenticated, redirecting to LoginActivity");
            startActivity(new Intent(GenerateQrActivity.this, LoginActivity.class));
            finish();
            return;
        }

        qrCodeImageView = findViewById(R.id.qrCodeImageView);
        menuButton = findViewById(R.id.menu_button);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        databaseReference = FirebaseDatabase.getInstance().getReference("qr_codes");

        String qrCodeData = UUID.randomUUID().toString(); // генерируем рандомный UUID
        databaseReference.push().setValue(qrCodeData);

        Bitmap bitmap = generateTransparentQrCode(qrCodeData, 500, 500);
        qrCodeImageView.setImageBitmap(bitmap);

        qr_TimeTracker= findViewById(R.id.qr_TimeTracker);
        String text = "QRTT<br><small><small>QR Time Tracker</small></small>";
        qr_TimeTracker.setText(Html.fromHtml(text));

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() { // обработка нажатий на элементы навигационного меню
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_generate_qr) {
                    showPasswordDialog();
                    return true;
                } else if (id == R.id.nav_home) {
                    startActivity(new Intent(GenerateQrActivity.this, MainActivity.class));
                    return true;
                } else if (id == R.id.nav_logout) {
                    startActivity(new Intent(GenerateQrActivity.this, LoginActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_reports) {
                    startActivity(new Intent(GenerateQrActivity.this, ReportsActivity.class));
                    return true;
                }
                return false;
            }
        });

        menuButton.setOnClickListener(new View.OnClickListener() { // обработка нажатия на кнопку вызова навигационного меню
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(navigationView);
            }
        });

        // инициализация ViewModel. гарантирует, что ViewModel будет сохраняться и восстанавливаться при изменении конфигурации устройства (при повороте экрана)
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

//        viewModel.loadUserData(currentUser); // загрузка данных пользователя в навигационное меню
        loadUserData(currentUser);
    }

    private Bitmap generateTransparentQrCode(String data, int width, int height) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, width, height);
            int[] pixels = new int[width * height];

            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = bitMatrix.get(x, y) ? Color.BLACK : Color.TRANSPARENT;
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showPasswordDialog() { // метод для отображения диалога ввода пароля
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogStyle);
        builder.setTitle("Enter Password");

        LinearLayout layout = new LinearLayout(this); // создаем LinearLayout для размещения EditText с отступами
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 10, 20, 10); // отступы слева и справа

        final EditText input = new EditText(this);
        input.setHint("Password");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); // звездочки
        input.setBackgroundResource(R.drawable.edittext_background);

        input.setOnKeyListener((v, keyCode, event) -> { // слушатель нажатий на клавиатуру по кнопке enter
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                String password = input.getText().toString().trim();
                if (password.equals(ADMIN_PASSWORD)) {
                    startActivity(new Intent(GenerateQrActivity.this, GenerateQrActivity.class));
                    return true;
                } else {
                    Toast.makeText(GenerateQrActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
            return false;
        });

        layout.addView(input); // отображение ввода в диалоге

        builder.setView(layout);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String password = input.getText().toString().trim();
            if (password.equals(ADMIN_PASSWORD)) {
                startActivity(new Intent(GenerateQrActivity.this, GenerateQrActivity.class));
            } else {
                Toast.makeText(GenerateQrActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE); // цвет текста и фона для кнопок
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.parseColor("#80086300"));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.parseColor("#80086300"));

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(); // ширина диалогового окна на 80% ширины экрана
        layoutParams.copyFrom(dialog.getWindow().getAttributes());
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = (int) (displayMetrics.widthPixels * 0.8);
        layoutParams.width = width;
        dialog.getWindow().setAttributes(layoutParams);

        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
    }

    private void loadUserData(FirebaseUser currentUser) {
        if (currentUser != null) {
            String email = currentUser.getEmail();
            int index = email.indexOf('@');
            String displayName = email.substring(0, index);

            View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                TextView userNameTextView = headerView.findViewById(R.id.userNameTextView);
                TextView userEmailTextView = headerView.findViewById(R.id.userEmailTextView);

                if (userNameTextView != null) {
                    userNameTextView.setText(displayName != null ? displayName : "User");
                } else {
                    Log.e(TAG, "userNameTextView is null");
                }

                if (userEmailTextView != null) {
                    userEmailTextView.setText(email != null ? email : "user@example.com");
                } else {
                    Log.e(TAG, "userEmailTextView is null");
                }
            } else {
                Log.e(TAG, "headerView is null");
            }
        }
    }
}