package com.happproxy.vpn;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;

public class LocalVpnService extends VpnService {
    public static final String ACTION_CONNECT = "com.happproxy.vpn.CONNECT";
    public static final String ACTION_DISCONNECT = "com.happproxy.vpn.DISCONNECT";
    public static final String ACTION_STATUS_CHANGED = "com.happproxy.vpn.STATUS_CHANGED";
    public static final String EXTRA_CONNECTED = "connected";
    public static final String EXTRA_MESSAGE = "message";

    private static final String TAG = "LocalVpnService";
    private static final String PREFS = "vpn_state";
    private static final String KEY_CONNECTED = "connected";

    private ParcelFileDescriptor vpnInterface;

    public static boolean isConnected(Context context) {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_CONNECTED, false);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_DISCONNECT.equals(action)) {
            disconnect("Локальный VPN-профиль отключен");
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ACTION_CONNECT.equals(action)) {
            connect();
            return START_STICKY;
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onRevoke() {
        disconnect("Системное разрешение VPN отозвано");
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        disconnect("Локальный VPN-профиль остановлен");
        super.onDestroy();
    }

    private void connect() {
        disconnect(null);

        try {
            Builder builder = new Builder()
                    .setSession("Happ VPN Glass")
                    .setMtu(1280)
                    .addAddress("10.42.0.2", 32);

            vpnInterface = builder.establish();
            if (vpnInterface == null) {
                setConnected(false);
                broadcast(false, "Не удалось создать VPN-интерфейс");
                return;
            }

            setConnected(true);
            broadcast(true, "Локальный VPN-профиль активен");
        } catch (RuntimeException exception) {
            Log.w(TAG, "Unable to start local VPN profile", exception);
            closeInterface();
            setConnected(false);
            broadcast(false, "Ошибка запуска локального VPN-профиля");
        }
    }

    private void disconnect(String message) {
        closeInterface();
        setConnected(false);
        if (message != null) {
            broadcast(false, message);
        }
    }

    private void closeInterface() {
        if (vpnInterface == null) {
            return;
        }

        try {
            vpnInterface.close();
        } catch (IOException exception) {
            Log.w(TAG, "Unable to close VPN interface", exception);
        } finally {
            vpnInterface = null;
        }
    }

    private void setConnected(boolean connected) {
        SharedPreferences preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        preferences.edit().putBoolean(KEY_CONNECTED, connected).apply();
    }

    private void broadcast(boolean connected, String message) {
        Intent status = new Intent(ACTION_STATUS_CHANGED)
                .setPackage(getPackageName())
                .putExtra(EXTRA_CONNECTED, connected)
                .putExtra(EXTRA_MESSAGE, message);
        sendBroadcast(status);
    }
}
