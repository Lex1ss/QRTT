package com.example.qrtt;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.qrtt.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class MainViewModel extends ViewModel {

    private MainActivity mainActivity;

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    private DatabaseReference databaseReference;
    private DatabaseReference qrCodesReference;
    private MutableLiveData<List<UserScan>> scansLiveData = new MutableLiveData<>();
    private MutableLiveData<String> scanResultLiveData = new MutableLiveData<>();
    private MutableLiveData<User> userLiveData = new MutableLiveData<>();

    public MainViewModel() {
        databaseReference = FirebaseDatabase.getInstance().getReference("scans");
        qrCodesReference = FirebaseDatabase.getInstance().getReference("qr_codes");
    }

    public LiveData<List<UserScan>> getScansLiveData() {
        return scansLiveData;
    }

    public LiveData<String> getScanResultLiveData() {
        return scanResultLiveData;
    }

    public LiveData<User> getUserLiveData() {
        return userLiveData;
    }

    public void loadUserData(FirebaseUser currentUser) {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String userName = dataSnapshot.child("name").getValue(String.class);
                        String userEmail = dataSnapshot.child("email").getValue(String.class);

                        userLiveData.setValue(new User(userName, userEmail));
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) { }
            });
        }
    }

    public void handleScannedData(String scannedData) {
        qrCodesReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean isValidQrCode = false;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String qrCode = snapshot.getValue(String.class);
                    if (scannedData.equals(qrCode)) {
                        isValidQrCode = true;
                        break;
                    }
                }

                if (isValidQrCode) {
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        String userId = currentUser.getUid();
                        String userEmail = currentUser.getEmail();

                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        LocalDateTime now = LocalDateTime.now();

                        databaseReference.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                String scanKey = null;
                                if (dataSnapshot.exists()) {
                                    for (DataSnapshot scanSnapshot : dataSnapshot.getChildren()) {
                                        if (!scanSnapshot.hasChild("release_time")) {
                                            scanKey = scanSnapshot.getKey();
                                            break;
                                        }
                                    }
                                }

                                if (scanKey != null) {
                                    String entryTimeStr = dataSnapshot.child(scanKey).child("entry_time").getValue(String.class);
                                    LocalDateTime entryTime = LocalDateTime.parse(entryTimeStr, formatter);
                                    long timeDifference = ChronoUnit.HOURS.between(entryTime, now);

                                    databaseReference.child(userId).child(scanKey).child("release_time").setValue(now.format(formatter));
                                    databaseReference.child(userId).child(scanKey).child("time_hoursDifference").setValue(timeDifference)
                                            .addOnSuccessListener(aVoid -> {
                                                scanResultLiveData.setValue("Data updated in Firebase");
                                                loadStatistics(userId);
                                            })
                                            .addOnFailureListener(e -> scanResultLiveData.setValue("Failed to update data in Firebase"));
                                } else {
                                    scanKey = databaseReference.child(userId).push().getKey();
                                    UserScan userScan = new UserScan(userId, now.format(formatter), null, 0, userEmail);

                                    databaseReference.child(userId).child(scanKey).setValue(userScan)
                                            .addOnSuccessListener(aVoid -> {
                                                scanResultLiveData.setValue("Data saved to Firebase");
                                                loadStatistics(userId);
                                            })
                                            .addOnFailureListener(e -> scanResultLiveData.setValue("Failed to save data to Firebase"));
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                scanResultLiveData.setValue("Database error");
                            }
                        });
                    } else {
                        scanResultLiveData.setValue("Invalid QR code");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                scanResultLiveData.setValue("Database error");
            }
        });
    }

    public void loadStatistics(String userId) {
        databaseReference.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<UserScan> scans = new ArrayList<>();
                for (DataSnapshot scanSnapshot : dataSnapshot.getChildren()) {
                    UserScan userScan = scanSnapshot.getValue(UserScan.class);
                    if (userScan != null) {
                        scans.add(userScan);
                    }
                }
                scansLiveData.setValue(scans);

                Log.d("MainViewModel", "Loaded scans: " + scans.size()); // логирование для проверки загрузки данных

                if (mainActivity != null) {
                    mainActivity.updateStatisticsUI(scans);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("MainViewModel", "Database error: " + databaseError.getMessage());
            }
        });
    }
}