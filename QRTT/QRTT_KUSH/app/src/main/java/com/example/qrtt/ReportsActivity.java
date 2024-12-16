package com.example.qrtt;

import static androidx.camera.core.CameraXThreads.TAG;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.example.qrtt.models.User;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeParseException;
import org.threeten.bp.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReportsActivity extends AppCompatActivity {

    private TextView userInfoTextView, hoursTextView, missedTextView;
    private Button filterButton;
    private ScatterChart scatterChart; // точечная диаграмма
    private TextView startDateEditText, endDateEditText;
    private NavigationView navigationView;
    private static final String ADMIN_PASSWORD = "admin";

    private MainViewModel viewModel;
    private TextView qr_TimeTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        Log.d(TAG, "onCreate called");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "User is not authenticated, redirecting to LoginActivity");
            startActivity(new Intent(ReportsActivity.this, LoginActivity.class));
            finish();
            return;
        }

        userInfoTextView = findViewById(R.id.userInfoTextView);
        hoursTextView = findViewById(R.id.hoursTextView);
        missedTextView = findViewById(R.id.missedTextView);
        filterButton = findViewById(R.id.filterButton);
        scatterChart = findViewById(R.id.scatterChart);
        startDateEditText = findViewById(R.id.startDateEditText);
        endDateEditText = findViewById(R.id.endDateEditText);
        navigationView = findViewById(R.id.nav_view);

        qr_TimeTracker= findViewById(R.id.qr_TimeTracker); // хтмл форма лого
        String text = "QRTT<br><small><small>QR Time Tracker</small></small>";
        qr_TimeTracker.setText(Html.fromHtml(text));

        userInfoTextView.setText("USER INFO");
        hoursTextView.setText("Hours Worked: ");
        missedTextView.setText("Missed Work: ");

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        viewModel.loadStatistics(currentUser.getUid());

        viewModel.getScansLiveData().observe(this, scans -> { // метод уведомляет активность об изменениях в данных
            // если произошло изменение, то вызывается метод и данные статистики обновляются
            updateStatisticsUI(scans, startDateEditText.getText().toString(), endDateEditText.getText().toString());
        });

        filterButton.setOnClickListener(new View.OnClickListener() { // кнопка "фильтр"
            @Override
            public void onClick(View v) {
                hideKeyboard(); // скрывается клавиатура

                String startDate = startDateEditText.getText().toString();
                String endDate = endDateEditText.getText().toString();
                updateStatisticsUI(viewModel.getScansLiveData().getValue(), startDate, endDate);
            }
        });

        startDateEditText.setOnClickListener(new View.OnClickListener() { // слушатель на календарь
            @Override
            public void onClick(View v) {
                showDatePickerDialog(startDateEditText);
            }
        });

        endDateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(endDateEditText);
            }
        });

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_generate_qr) {
                    showPasswordDialog();
                    return true;
                } else if (id == R.id.nav_home) {
                    startActivity(new Intent(ReportsActivity.this, MainActivity.class));
                    return true;
                } else if (id == R.id.nav_logout) {
                    startActivity(new Intent(ReportsActivity.this, LoginActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_reports) {
                    startActivity(new Intent(ReportsActivity.this, ReportsActivity.class));
                    return true;
                }
                return false;
            }
        });

        FloatingActionButton menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNavigationDrawer();
            }
        });

        loadUserData(currentUser);
    }

    private void loadUserData(FirebaseUser currentUser) {
        if (currentUser != null) {
            String email = currentUser.getEmail();
            int index = email.indexOf('@');
            String displayName = email.substring(0, index);

            View headerView = navigationView.getHeaderView(0);
            TextView userNameTextView = headerView.findViewById(R.id.userNameTextView);
            TextView userEmailTextView = headerView.findViewById(R.id.userEmailTextView);

            userNameTextView.setText(displayName != null ? displayName : "User");
            userEmailTextView.setText(email != null ? email : "user@example.com");
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
    }

    private void updateStatisticsUI(List<UserScan> scans, String startDate, String endDate) {
        long totalHours = 0;
        long totalMinutes = 0;
        int missedWorkDays = 0;
        List<Entry> scatterEntries = new ArrayList<>();
        String[] daysOfWeek = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};

        if (startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(this, "Please enter valid dates", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!startDate.contains(" ")) {
            startDate += " 00:00:00";
        }
        if (!endDate.contains(" ")) {
            endDate += " 23:59:59";
        }

        try {
            LocalDateTime start = LocalDateTime.parse(startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            LocalDateTime end = LocalDateTime.parse(endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            Set<LocalDate> daysInRange = new HashSet<>(); // создаем множество дней, которые должны быть учтены
            LocalDateTime current = start;
            while (!current.isAfter(end)) {
                daysInRange.add(current.toLocalDate());
                current = current.plusDays(1);
            }

            for (UserScan userScan : scans) { // удаляем дни, когда были записи Missed Work
                if (userScan.entry_time != null && userScan.release_time != null) {
                    LocalDateTime entryTime = LocalDateTime.parse(userScan.entry_time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    LocalDateTime releaseTime = LocalDateTime.parse(userScan.release_time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                    if (entryTime.isAfter(start) && releaseTime.isBefore(end)) {
                        long hoursDifference = ChronoUnit.HOURS.between(entryTime, releaseTime);
                        long minutesDifference = ChronoUnit.MINUTES.between(entryTime, releaseTime) % 60;
                        totalHours += hoursDifference;
                        totalMinutes += minutesDifference;

                        int dayOfWeek = entryTime.getDayOfWeek().getValue() - 1;
                        scatterEntries.add(new Entry(dayOfWeek, hoursDifference));

                        daysInRange.remove(entryTime.toLocalDate()); // удаляем день из множества, если есть запись в базе данных
                    }
                }
            }

            missedWorkDays = daysInRange.size(); // пропущенные дни - это дни, которые остались в множестве

            totalHours += totalMinutes / 60;
            totalMinutes %= 60;

            String hoursWorked = String.format("Hours Worked: %d hours %d minutes", totalHours, totalMinutes);
            hoursTextView.setText(hoursWorked);

            String missedWork = String.format("Missed Work: %d days", missedWorkDays);
            missedTextView.setText(missedWork);

            ScatterDataSet scatterDataSet = new ScatterDataSet(scatterEntries, "Статистика за неделю"); // заполнение точечной диаграммы
            scatterDataSet.setColors(getResources().getColor(R.color.colorPrimary), getResources().getColor(R.color.colorAccent));
            ScatterData scatterData = new ScatterData(scatterDataSet);
            scatterChart.setData(scatterData);
            scatterChart.animateY(1000);

            XAxis xAxis = scatterChart.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(daysOfWeek));
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setLabelCount(7);

            YAxis yAxis = scatterChart.getAxisLeft();
            yAxis.setAxisMinimum(0f);
            yAxis.setAxisMaximum(8f);
            yAxis.setLabelCount(9);
            yAxis.setGranularity(1f);

            scatterChart.getAxisRight().setEnabled(false);
            scatterChart.invalidate(); // обновление диаграммы
        } catch (DateTimeParseException e) {
            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
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
                    startActivity(new Intent(ReportsActivity.this, GenerateQrActivity.class));
                    return true;
                } else {
                    Toast.makeText(ReportsActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
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
                startActivity(new Intent(ReportsActivity.this, GenerateQrActivity.class));
            } else {
                Toast.makeText(ReportsActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
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

    private void showDatePickerDialog(final TextView textView) { // функция календаря
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                R.style.CustomDatePickerDialog,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        String selectedDate = String.format("%04d-%02d-%02d", year, monthOfYear + 1, dayOfMonth);
                        textView.setText(selectedDate);
                    }
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void openNavigationDrawer() {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.openDrawer(GravityCompat.START);
    }
}