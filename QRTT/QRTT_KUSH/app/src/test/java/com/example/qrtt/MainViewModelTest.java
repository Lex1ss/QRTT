package com.example.qrtt;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.google.firebase.auth.FirebaseUser;

public class MainViewModelTest {

    private MainViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new MainViewModel();
    }

    @Test
    public void testHandleScannedData() {
        String testData = "test_data";
        viewModel.handleScannedData(testData);
        assertEquals("Scanned data: " + testData, viewModel.getScanResultLiveData().getValue());
    }

    @Test
    public void testLoadUserData() {
        FirebaseUser user = Mockito.mock(FirebaseUser.class);
        when(user.getUid()).thenReturn("test_uid");

        viewModel.loadUserData(user);
        // Добавьте дополнительные проверки для загрузки данных пользователя
    }
}