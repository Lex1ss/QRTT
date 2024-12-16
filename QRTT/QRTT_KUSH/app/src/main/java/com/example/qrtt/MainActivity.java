package com.example.qrtt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.temporal.ChronoUnit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity { // родительский класс

    // константы final
    private static final String TAG = "MainActivity"; // для логов
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA}; // Manifest.permission класс в SDK, содержит константы для всех разрешений, определенных в манифесте. можно использовать несколько , поэтому массив
    private static final String ADMIN_PASSWORD = "admin";
//    private static final int SCAN_REQUEST_CODE = 123;

    private CaptureManager capture;
    private DecoratedBarcodeView camera;
//    private FrameLayout splashsrc;
//    private FrameLayout main;
//    private FrameLayout cameraContainer;

    private boolean isScanning = false; // флаг для определения, выполняется ли сканирование

    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private TextView statsText;
    private PieChart pieChart;
    private BarChart barChart;

    private NavigationView navigationView;
    private View headerView;
    private TextView userNameTextView;
    private TextView userEmailTextView;
    private TextView qr_TimeTracker;

    private DrawerLayout drawerLayout; // DrawerLayout — контейнер для отображения бокового меню
    private FloatingActionButton menuButton;

    private MainViewModel viewModel;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate called");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "User is not authenticated, redirecting to LoginActivity");
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        camera = findViewById(R.id.camera);
//        splashsrc = findViewById(R.id.splashsrc);
//        main = findViewById(R.id.main);
//        cameraContainer = findViewById(R.id.camera_container);

        pieChart = findViewById(R.id.pie_chart);
        barChart = findViewById(R.id.bar_chart);

        navigationView = findViewById(R.id.nav_view);
        headerView = navigationView.getHeaderView(0); // 0 - индекс заголовка навигационного меню, у нас один
        userNameTextView = headerView.findViewById(R.id.userNameTextView);
        userEmailTextView = headerView.findViewById(R.id.userEmailTextView);

        drawerLayout = findViewById(R.id.drawer_layout);
        menuButton = findViewById(R.id.menu_button);

        camera.setStatusText("");

        capture = new CaptureManager(this, camera); // инициализация сканирования
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        capture.decode(); // запуск сканирования

        if (allPermissionsGranted()) { // запрос разрешений на камеру
            Log.d(TAG, "All permissions granted, starting camera");
            startCamera();
        } else {
            Log.d(TAG, "Requesting camera permission");
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        // инициализация ViewModel. гарантирует, что ViewModel будет сохраняться и восстанавливаться при изменении конфигурации устройства (при повороте экрана)
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        if (currentUser != null) { // загрузка данных пользователя
            viewModel.loadUserData(currentUser);
        }

        observeUserData(); // позволяет изменять данные автоматичсеки при изменении данных в базе

        camera.decodeContinuous(new com.journeyapps.barcodescanner.BarcodeCallback() { // обработка результатов сканирования
            @Override
            public void barcodeResult(com.journeyapps.barcodescanner.BarcodeResult result) { // успешное распознавание qr-кода
                if (!isScanning) {
                    isScanning = true; // блокировка множественных сканирований
                    viewModel.handleScannedData(result.getText());
                }
            }

//            @Override // обнаруживает точки результата
//            public void possibleResultPoints(java.util.List<com.google.zxing.ResultPoint> resultPoints) {
//            }
        });

        qr_TimeTracker= findViewById(R.id.qr_TimeTracker);
        String text = "QRTT<br><small><small>QR Time Tracker</small></small>"; // текст у логотипа на главном экране
        qr_TimeTracker.setText(Html.fromHtml(text));

        LinearLayout bottomSheet = findViewById(R.id.bottom_sheet); // вкладка статистики
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        bottomSheetBehavior.setPeekHeight((int) (getResources().getDisplayMetrics().heightPixels * 0.45)); // начальная высота peekHeight вкладки статистики
        bottomSheet.setAlpha(0.8f); // ее прозрачность

        statsText = findViewById(R.id.stats_text);

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_EXPANDED: // вкладка развернута полностью
                        Log.d("BottomSheet", "STATE_EXPANDED");
                        pieChart.setVisibility(View.VISIBLE);
                        barChart.setVisibility(View.VISIBLE);
                        bottomSheet.setAlpha(1.0f); // 100% прозрачности
                        viewModel.loadStatistics(currentUser.getUid());
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED: // вкладка внизу
                        Log.d("BottomSheet", "STATE_HALF_EXPANDED");
                        bottomSheetBehavior.setPeekHeight(getResources().getDimensionPixelSize(R.dimen.bottom_sheet_peek_height)); // шапка
                        pieChart.setVisibility(View.GONE);
                        barChart.setVisibility(View.GONE);
                        bottomSheetBehavior.setPeekHeight((int) (getResources().getDisplayMetrics().heightPixels * 0.15));
                        bottomSheet.setAlpha(0.8f); // 80% прозрачности
                        statsText.setText("Статистика"); // текст "Статистика"
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN: // скрыта полностью
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) { // обработка сдвига вкладки, где -1: скрыта, 0: частично видна, 1: на весь экран
                float alpha = 0.7f + (0.3f * slideOffset); // изменение прозрачности в зависимости от сдвига
                bottomSheet.setAlpha(alpha);
            }
        });

        viewModel.loadStatistics(currentUser.getUid()); // загрузка статистики
        statsText.setText("Статистика");

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() { // обработка нажатий на элементы меню
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_generate_qr) {
                    showPasswordDialog();
                    return true;
                } else if (id == R.id.nav_home) {
                    startActivity(new Intent(MainActivity.this, MainActivity.class));
                    return true;
                } else if (id == R.id.nav_logout) {
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_reports) {
                    startActivity(new Intent(MainActivity.this, ReportsActivity.class));
                    return true;
                }
                return false;
            }
        });

        menuButton.setOnClickListener(new View.OnClickListener() { // обработка нажатия на кнопку вызова навигационного меню
            @Override // DrawerLayout может содержать два основных дочерних элемента: основное содержимое и боковое меню
            public void onClick(View v) { drawerLayout.openDrawer(navigationView); }
        });

        viewModel.loadUserData(currentUser); // загрузка данных пользователя в навигационное меню

        viewModel.getScansLiveData().observe(this, scans -> { // метод уведомляет активность об изменениях в данных
            updateStatisticsUI(scans); // если произошло изменение, то вызывается метод и данные статистики обновляются
        });

        viewModel.getScanResultLiveData().observe(this, result -> {
            showScanResultDialog(result); // отображение результата сканирования
        });
    }

    private boolean allPermissionsGranted() { // запрос разрешений на камеру
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startCamera() {
        camera.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() { // возобновление активности
        super.onResume();
        if (capture != null) {
            capture.onResume(); // возобновления работы камеры
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (capture != null) {
            capture.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (capture != null) {
            capture.onDestroy();
        }
        FirebaseAuth.getInstance().signOut(); // выход из учетной записи пользователя при закрытии приложения
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (capture != null) {
            capture.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Log.d(TAG, "Camera permission granted, starting camera");
                startCamera();
            } else {
                Log.d(TAG, "Camera permission not granted, finishing activity");
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void observeUserData() {
        viewModel.getUserLiveData().observe(this, user -> {
            if (user != null) {
                userNameTextView.setText(user.getPassword());
                userEmailTextView.setText(user.getEmail());
            }
        });
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
                    startActivity(new Intent(MainActivity.this, GenerateQrActivity.class));
                    return true;
                } else {
                    Toast.makeText(MainActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
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
                startActivity(new Intent(MainActivity.this, GenerateQrActivity.class));
            } else {
                Toast.makeText(MainActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
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

        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background); // темный фон для диалогового окна
    }

    private void showScanResultDialog(String message) { // метод для отображения диалога с результатом сканирования
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogStyle);
        builder.setTitle("Scan Result");
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> {
            isScanning = false; // сканирование завершено
        });
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.parseColor("#80086300"));

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(dialog.getWindow().getAttributes());
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = (int) (displayMetrics.widthPixels * 0.8);
        layoutParams.width = width;
        dialog.getWindow().setAttributes(layoutParams);

        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
    }

    public void updateStatisticsUI(List<UserScan> scans) { // метод для обновления UI с данными статистики
        Log.d("MainActivity", "Updating statistics UI with " + scans.size() + " scans");

        long totalHoursThisWeek = 0;
        long totalMinutesThisWeek = 0;
        long totalHoursThisMonth = 0;
        long totalMinutesThisMonth = 0;

        for (UserScan userScan : scans) {
            if (userScan.entry_time != null && userScan.release_time != null) {
                LocalDateTime entryTime = LocalDateTime.parse(userScan.entry_time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                LocalDateTime releaseTime = LocalDateTime.parse(userScan.release_time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                long hoursDifference = ChronoUnit.HOURS.between(entryTime, releaseTime);
                long minutesDifference = ChronoUnit.MINUTES.between(entryTime, releaseTime) % 60;

                if (entryTime.isAfter(LocalDateTime.now().minusWeeks(1))) {
                    totalHoursThisWeek += hoursDifference;
                    totalMinutesThisWeek += minutesDifference;
                }

                if (entryTime.isAfter(LocalDateTime.now().minusMonths(1))) {
                    totalHoursThisMonth += hoursDifference;
                    totalMinutesThisMonth += minutesDifference;
                }
            }
        }

        totalHoursThisWeek += totalMinutesThisWeek / 60; // приводим минуты к часам и минутам
        totalMinutesThisWeek %= 60;
        totalHoursThisMonth += totalMinutesThisMonth / 60;
        totalMinutesThisMonth %= 60;

        String weekStats = String.format("За неделю: %d часов %d минут", totalHoursThisWeek, totalMinutesThisWeek);
        String monthStats = String.format("За месяц: %d часов %d минут", totalHoursThisMonth, totalMinutesThisMonth);

        statsText.setText(weekStats + "\n" + monthStats);

        List<PieEntry> pieEntries = new ArrayList<>(); // заполнение круговой диаграммы
        pieEntries.add(new PieEntry(totalHoursThisMonth, "Отработано"));
        pieEntries.add(new PieEntry(160 - totalHoursThisMonth, "Осталось"));

        PieDataSet pieDataSet = new PieDataSet(pieEntries, "Статистика за месяц");
        pieDataSet.setColors(Color.parseColor("#61ad5a"), Color.parseColor("#32662d"), Color.parseColor("#006400"));
        PieData pieData = new PieData(pieDataSet);
        pieChart.setData(pieData);
        pieChart.animateY(1000); // анимация для круговой диаграммы
        pieChart.invalidate(); // обновление диаграммы

        List<BarEntry> barEntries = new ArrayList<>(); // заполнение столбчатой диаграммы
        String[] daysOfWeek = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        for (int i = 0; i < 7; i++) {
            long hours = 0;
            for (UserScan userScan : scans) {
                if (userScan.entry_time != null && userScan.release_time != null) {
                    LocalDateTime entryTime = LocalDateTime.parse(userScan.entry_time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    if (entryTime.getDayOfWeek().getValue() == i + 1) {
                        hours += ChronoUnit.HOURS.between(entryTime, LocalDateTime.parse(userScan.release_time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    }
                }
            }
            barEntries.add(new BarEntry(i, hours));
        }

        BarDataSet barDataSet = new BarDataSet(barEntries, "Статистика за неделю");
        barDataSet.setColors(Color.parseColor("#2E2E2E"), Color.parseColor("#666666"), Color.parseColor("#004F00"));
        BarData barData = new BarData(barDataSet);
        barChart.setData(barData);
        barChart.animateY(1000); // анимация для столбчатой диаграммы

        XAxis xAxis = barChart.getXAxis(); // настройка оси X для отображения дней недели
        xAxis.setValueFormatter(new IndexAxisValueFormatter(daysOfWeek));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(7);

        YAxis yAxis = barChart.getAxisLeft(); // настройка оси Y для отображения часов от 0 до 8
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(8f);
        yAxis.setLabelCount(9);
        yAxis.setGranularity(1f); // устанавливаем шаг 1 для отображения целых чисел

        barChart.getAxisRight().setEnabled(false);
        barChart.invalidate(); // обновление диаграммы
    }
}