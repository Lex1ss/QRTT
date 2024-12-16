package com.example.qrtt;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.qrtt.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity { // родительский класс

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private FirebaseAuth auth;
    private FirebaseDatabase db;
    private DatabaseReference users;
    private ConstraintLayout root;

    @SuppressLint("MissingInflatedId") // подавление предупреждений об объявлении идентификатора к элементу в XML-файле
    @Override // переопределяет метод из суперкласса
    protected void onCreate(Bundle savedInstanceState) { // создает состояние активности
        super.onCreate(savedInstanceState); // выполнение иницализаций в методе
        setContentView(R.layout.activity_login);

        emailEditText = findViewById(R.id.editEmail);
        passwordEditText = findViewById(R.id.editPassword);
        loginButton = findViewById(R.id.b_auth);
        root = findViewById(R.id.root);

        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        users = db.getReference("Users");

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || password.length() < 6) {
                    Snackbar.make(root, "Please fill in all fields and ensure the password is at least 6 characters long.", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                auth.signInWithEmailAndPassword(email, password) // метод является частью Firebase Authentication и используется для аутентификации пользователя с помощью его email и пароля
                        .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() { // метод добавляет слушателя, который будет вызван после завершения асинхронной операции
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = auth.getCurrentUser();
                                    Toast.makeText(LoginActivity.this, "Authentication successful.", Toast.LENGTH_SHORT).show();

                                    User userData = new User(); // в навигационное меню устанавливаем данные
                                    userData.setPassword("User Name"); // имя пользователя по-умолчанию
                                    userData.setEmail(email);

                                    // устанавливает данные пользователя в узел "users" в базе данных
                                    users.child(user.getUid()).setValue(userData).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            Snackbar.make(root, "User data added successfully.", Snackbar.LENGTH_SHORT).show();
                                        }
                                    });

                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                } else {
                                    Exception exception = task.getException();
                                    if (exception != null) {
                                        String errorMessage = getString(R.string.error_message, exception.getMessage());
                                        Snackbar.make(root, errorMessage, Snackbar.LENGTH_SHORT).show();
                                    } else {
                                        Snackbar.make(root, "Authentication failed.", Snackbar.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        });
            }
        });
    }
}