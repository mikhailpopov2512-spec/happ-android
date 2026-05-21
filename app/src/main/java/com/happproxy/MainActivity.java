package com.happproxy;

import android.app.Activity;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import com.happproxy.vpn.LocalVpnService;

public class MainActivity extends Activity {
    private static final int REQUEST_VPN = 42;

    private GlassVpnView glassVpnView;

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!LocalVpnService.ACTION_STATUS_CHANGED.equals(intent.getAction())) {
                return;
            }

            boolean connected = intent.getBooleanExtra(LocalVpnService.EXTRA_CONNECTED, false);
            String message = intent.getStringExtra(LocalVpnService.EXTRA_MESSAGE);
            glassVpnView.setVpnState(connected, false, message);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();

        glassVpnView = new GlassVpnView(this);
        glassVpnView.setOnToggleRequestListener(this::onToggleRequested);
        glassVpnView.setVpnState(
                LocalVpnService.isConnected(this),
                false,
                LocalVpnService.isConnected(this)
                        ? "Локальный VPN-профиль активен"
                        : "Готово к локальному запуску"
        );
        setContentView(glassVpnView);
    }

    @Override
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(LocalVpnService.ACTION_STATUS_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(statusReceiver, filter);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(statusReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_VPN) {
            return;
        }

        if (resultCode == RESULT_OK) {
            startVpn();
        } else {
            glassVpnView.setVpnState(false, false, "Разрешение VPN не выдано");
        }
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
        window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.getAttributes().layoutInDisplayCutoutMode =
                    android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
    }

    private void onToggleRequested(boolean shouldConnect) {
        if (shouldConnect) {
            Intent prepareIntent = VpnService.prepare(this);
            if (prepareIntent != null) {
                glassVpnView.setVpnState(false, true, "Ожидание системного разрешения VPN");
                startActivityForResult(prepareIntent, REQUEST_VPN);
            } else {
                startVpn();
            }
        } else {
            glassVpnView.setVpnState(false, true, "Отключение локального профиля");
            startService(new Intent(this, LocalVpnService.class)
                    .setAction(LocalVpnService.ACTION_DISCONNECT));
        }
    }

    private void startVpn() {
        glassVpnView.setVpnState(false, true, "Запуск локального VPN-профиля");
        startService(new Intent(this, LocalVpnService.class)
                .setAction(LocalVpnService.ACTION_CONNECT));
    }
}
