package com.happproxy;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.Locale;

public class GlassVpnView extends View {
    public interface OnToggleRequestListener {
        void onToggleRequested(boolean shouldConnect);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mainCard = new RectF();
    private final RectF connectButton = new RectF();
    private final RectF tempRect = new RectF();
    private final float[] nodeXs = new float[7];
    private final float[] nodeYs = new float[7];

    private OnToggleRequestListener toggleRequestListener;
    private boolean connected;
    private boolean busy;
    private boolean pressed;
    private String message = "Готово к локальному запуску";
    private long connectedAt = SystemClock.elapsedRealtime();
    private float pulse;

    public GlassVpnView(Context context) {
        super(context);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        setFocusable(true);
        setClickable(true);

        ValueAnimator pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(2600L);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setInterpolator(new DecelerateInterpolator());
        pulseAnimator.addUpdateListener(animation -> {
            pulse = (float) animation.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.start();
    }

    public void setOnToggleRequestListener(OnToggleRequestListener listener) {
        toggleRequestListener = listener;
    }

    public void setVpnState(boolean isConnected, boolean isBusy, String statusMessage) {
        if (!connected && isConnected) {
            connectedAt = SystemClock.elapsedRealtime();
        }
        connected = isConnected;
        busy = isBusy;
        if (statusMessage != null && !statusMessage.isEmpty()) {
            message = statusMessage;
        }
        invalidate();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        nodeXs[0] = width * 0.12f;
        nodeYs[0] = height * 0.20f;
        nodeXs[1] = width * 0.32f;
        nodeYs[1] = height * 0.12f;
        nodeXs[2] = width * 0.75f;
        nodeYs[2] = height * 0.18f;
        nodeXs[3] = width * 0.88f;
        nodeYs[3] = height * 0.44f;
        nodeXs[4] = width * 0.69f;
        nodeYs[4] = height * 0.78f;
        nodeXs[5] = width * 0.23f;
        nodeYs[5] = height * 0.84f;
        nodeXs[6] = width * 0.08f;
        nodeYs[6] = height * 0.56f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        float density = getResources().getDisplayMetrics().density;

        drawBackground(canvas, width, height);
        drawLinkedGlass(canvas, density);
        drawMainCard(canvas, width, height, density);
        drawHeader(canvas, density);
        drawShield(canvas, density);
        drawStatus(canvas, density);
        drawMetrics(canvas, density);
        drawConnectButton(canvas, density);
        drawFooter(canvas, width, height, density);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (busy) {
            return true;
        }

        boolean inside = connectButton.contains(event.getX(), event.getY());
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (inside) {
                    pressed = true;
                    invalidate();
                    return true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                pressed = false;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                boolean shouldClick = pressed && inside;
                pressed = false;
                invalidate();
                if (shouldClick) {
                    performClick();
                    return true;
                }
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        if (toggleRequestListener != null && !busy) {
            toggleRequestListener.onToggleRequested(!connected);
        }
        return true;
    }

    private void drawBackground(Canvas canvas, int width, int height) {
        paint.setShader(new LinearGradient(
                0f,
                0f,
                width,
                height,
                new int[]{
                        Color.rgb(5, 12, 26),
                        Color.rgb(9, 24, 47),
                        Color.rgb(18, 48, 77),
                        Color.rgb(6, 13, 28)
                },
                new float[]{0f, 0.38f, 0.72f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(0f, 0f, width, height, paint);
        paint.setShader(null);

        drawGlow(canvas, width * 0.20f, height * 0.22f, width * 0.45f, Color.argb(120, 45, 212, 191));
        drawGlow(canvas, width * 0.82f, height * 0.18f, width * 0.38f, Color.argb(105, 99, 102, 241));
        drawGlow(canvas, width * 0.72f, height * 0.82f, width * 0.52f, Color.argb(90, 14, 165, 233));
    }

    private void drawGlow(Canvas canvas, float cx, float cy, float radius, int color) {
        paint.setShader(new RadialGradient(
                cx,
                cy,
                radius,
                color,
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, radius, paint);
        paint.setShader(null);
    }

    private void drawLinkedGlass(Canvas canvas, float density) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(1.4f * density);

        for (int i = 0; i < nodeXs.length; i++) {
            int next = (i + 1) % nodeXs.length;
            paint.setColor(Color.argb(52, 208, 250, 255));
            canvas.drawLine(nodeXs[i], nodeYs[i], nodeXs[next], nodeYs[next], paint);
        }
        paint.setColor(Color.argb(34, 255, 255, 255));
        canvas.drawLine(nodeXs[1], nodeYs[1], nodeXs[4], nodeYs[4], paint);
        canvas.drawLine(nodeXs[2], nodeYs[2], nodeXs[5], nodeYs[5], paint);

        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < nodeXs.length; i++) {
            float radius = (7f + (i % 3) * 2f + pulse * 2f) * density;
            paint.setColor(Color.argb(38, 103, 232, 249));
            canvas.drawCircle(nodeXs[i], nodeYs[i], radius * 2.8f, paint);
            paint.setColor(Color.argb(172, 214, 249, 255));
            canvas.drawCircle(nodeXs[i], nodeYs[i], radius, paint);
        }
    }

    private void drawMainCard(Canvas canvas, int width, int height, float density) {
        float horizontal = 24f * density;
        float top = Math.max(72f * density, height * 0.11f);
        float bottom = Math.min(height - 72f * density, top + 620f * density);
        mainCard.set(horizontal, top, width - horizontal, bottom);

        paint.setStyle(Paint.Style.FILL);
        paint.setShadowLayer(32f * density, 0f, 20f * density, Color.argb(90, 0, 0, 0));
        paint.setColor(Color.argb(52, 255, 255, 255));
        canvas.drawRoundRect(mainCard, 34f * density, 34f * density, paint);
        paint.clearShadowLayer();

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.2f * density);
        paint.setColor(Color.argb(105, 255, 255, 255));
        canvas.drawRoundRect(mainCard, 34f * density, 34f * density, paint);

        tempRect.set(mainCard.left + 1.5f * density, mainCard.top + 1.5f * density,
                mainCard.right - 1.5f * density, mainCard.bottom - 1.5f * density);
        paint.setColor(Color.argb(44, 103, 232, 249));
        canvas.drawRoundRect(tempRect, 32f * density, 32f * density, paint);
    }

    private void drawHeader(Canvas canvas, float density) {
        float x = mainCard.left + 28f * density;
        float y = mainCard.top + 52f * density;

        drawPill(canvas, x, y - 30f * density, "LOCAL DEVICE", Color.argb(54, 45, 212, 191), density);
        drawPill(canvas, x + 130f * density, y - 30f * density, "GLASS LINK", Color.argb(48, 125, 211, 252), density);

        textPaint.setShader(new LinearGradient(
                x,
                y,
                mainCard.right,
                y,
                Color.WHITE,
                Color.rgb(103, 232, 249),
                Shader.TileMode.CLAMP
        ));
        textPaint.setTextSize(34f * density);
        textPaint.setFakeBoldText(true);
        canvas.drawText("Happ VPN", x, y + 34f * density, textPaint);
        textPaint.setShader(null);
        textPaint.setFakeBoldText(false);

        textPaint.setColor(Color.argb(182, 231, 245, 255));
        textPaint.setTextSize(14f * density);
        canvas.drawText("Linked glass защита на устройстве", x, y + 60f * density, textPaint);
    }

    private void drawPill(Canvas canvas, float x, float y, String label, int fillColor, float density) {
        textPaint.setTextSize(10f * density);
        textPaint.setFakeBoldText(true);
        float width = textPaint.measureText(label) + 24f * density;
        tempRect.set(x, y, x + width, y + 24f * density);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fillColor);
        canvas.drawRoundRect(tempRect, 12f * density, 12f * density, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0.8f * density);
        paint.setColor(Color.argb(80, 255, 255, 255));
        canvas.drawRoundRect(tempRect, 12f * density, 12f * density, paint);

        textPaint.setColor(Color.argb(228, 240, 253, 255));
        canvas.drawText(label, x + 12f * density, y + 16f * density, textPaint);
        textPaint.setFakeBoldText(false);
    }

    private void drawShield(Canvas canvas, float density) {
        float cx = mainCard.centerX();
        float cy = mainCard.top + 234f * density;
        float baseRadius = 92f * density;

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new RadialGradient(
                cx,
                cy,
                baseRadius * 1.7f,
                connected ? Color.argb(116, 45, 212, 191) : Color.argb(74, 99, 102, 241),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, baseRadius * (1.35f + pulse * 0.12f), paint);
        paint.setShader(null);

        paint.setShader(new LinearGradient(
                cx - baseRadius,
                cy - baseRadius,
                cx + baseRadius,
                cy + baseRadius,
                connected ? Color.rgb(45, 212, 191) : Color.rgb(125, 211, 252),
                connected ? Color.rgb(34, 197, 94) : Color.rgb(99, 102, 241),
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, baseRadius, paint);
        paint.setShader(null);

        paint.setColor(Color.argb(74, 255, 255, 255));
        canvas.drawCircle(cx - 22f * density, cy - 28f * density, 22f * density, paint);

        Path shield = new Path();
        shield.moveTo(cx, cy - 46f * density);
        shield.lineTo(cx + 43f * density, cy - 27f * density);
        shield.lineTo(cx + 35f * density, cy + 15f * density);
        shield.cubicTo(cx + 29f * density, cy + 42f * density,
                cx + 12f * density, cy + 57f * density,
                cx, cy + 66f * density);
        shield.cubicTo(cx - 12f * density, cy + 57f * density,
                cx - 29f * density, cy + 42f * density,
                cx - 35f * density, cy + 15f * density);
        shield.lineTo(cx - 43f * density, cy - 27f * density);
        shield.close();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(210, 7, 16, 32));
        canvas.drawPath(shield, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(6f * density);
        paint.setColor(Color.WHITE);
        if (connected) {
            Path check = new Path();
            check.moveTo(cx - 21f * density, cy + 4f * density);
            check.lineTo(cx - 5f * density, cy + 20f * density);
            check.lineTo(cx + 25f * density, cy - 17f * density);
            canvas.drawPath(check, paint);
        } else {
            canvas.drawLine(cx - 18f * density, cy, cx + 18f * density, cy, paint);
            canvas.drawLine(cx, cy - 18f * density, cx, cy + 18f * density, paint);
        }
    }

    private void drawStatus(Canvas canvas, float density) {
        float y = mainCard.top + 362f * density;

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(25f * density);
        textPaint.setColor(Color.WHITE);
        canvas.drawText(busy ? "Настройка профиля" : connected ? "VPN активен" : "VPN готов", mainCard.centerX(), y, textPaint);

        textPaint.setFakeBoldText(false);
        textPaint.setTextSize(14f * density);
        textPaint.setColor(Color.argb(188, 231, 245, 255));
        canvas.drawText(message, mainCard.centerX(), y + 30f * density, textPaint);

        textPaint.setTextSize(12f * density);
        textPaint.setColor(Color.argb(138, 231, 245, 255));
        String detail = connected
                ? "Сессия: " + formatDuration(SystemClock.elapsedRealtime() - connectedAt)
                : "Без аккаунта, сервера и отправки данных";
        canvas.drawText(detail, mainCard.centerX(), y + 54f * density, textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawMetrics(Canvas canvas, float density) {
        float top = mainCard.top + 438f * density;
        float gap = 10f * density;
        float width = (mainCard.width() - 56f * density - gap * 2f) / 3f;
        float left = mainCard.left + 28f * density;

        drawMetric(canvas, left, top, width, "Режим", "Локальный", density);
        drawMetric(canvas, left + width + gap, top, width, "Маршрут", "Без туннеля", density);
        drawMetric(canvas, left + (width + gap) * 2f, top, width, "Данные", "На устройстве", density);
    }

    private void drawMetric(Canvas canvas, float left, float top, float width, String label, String value, float density) {
        tempRect.set(left, top, left + width, top + 84f * density);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(42, 255, 255, 255));
        canvas.drawRoundRect(tempRect, 22f * density, 22f * density, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0.9f * density);
        paint.setColor(Color.argb(66, 255, 255, 255));
        canvas.drawRoundRect(tempRect, 22f * density, 22f * density, paint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(11f * density);
        textPaint.setColor(Color.argb(142, 231, 245, 255));
        canvas.drawText(label, tempRect.centerX(), top + 30f * density, textPaint);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(13f * density);
        textPaint.setColor(Color.WHITE);
        canvas.drawText(value, tempRect.centerX(), top + 56f * density, textPaint);
        textPaint.setFakeBoldText(false);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawConnectButton(Canvas canvas, float density) {
        float horizontal = mainCard.left + 32f * density;
        float top = mainCard.bottom - 112f * density;
        connectButton.set(horizontal, top, mainCard.right - 32f * density, top + 64f * density);

        int start = connected ? Color.rgb(248, 113, 113) : Color.rgb(45, 212, 191);
        int end = connected ? Color.rgb(244, 63, 94) : Color.rgb(14, 165, 233);
        if (busy) {
            start = Color.rgb(148, 163, 184);
            end = Color.rgb(71, 85, 105);
        }

        float inset = pressed ? 3f * density : 0f;
        tempRect.set(connectButton.left + inset, connectButton.top + inset,
                connectButton.right - inset, connectButton.bottom - inset);

        paint.setStyle(Paint.Style.FILL);
        paint.setShadowLayer(18f * density, 0f, 10f * density, Color.argb(115, 8, 47, 73));
        paint.setShader(new LinearGradient(
                tempRect.left,
                tempRect.top,
                tempRect.right,
                tempRect.bottom,
                start,
                end,
                Shader.TileMode.CLAMP
        ));
        canvas.drawRoundRect(tempRect, 24f * density, 24f * density, paint);
        paint.setShader(null);
        paint.clearShadowLayer();

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(16f * density);
        textPaint.setColor(Color.WHITE);
        String label = busy ? "ПОДОЖДИТЕ..." : connected ? "ОТКЛЮЧИТЬ" : "ВКЛЮЧИТЬ VPN";
        canvas.drawText(label, tempRect.centerX(), tempRect.centerY() + 6f * density, textPaint);
        textPaint.setFakeBoldText(false);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawFooter(Canvas canvas, int width, int height, float density) {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(12f * density);
        textPaint.setColor(Color.argb(128, 231, 245, 255));
        canvas.drawText("Для реального шифрования подключите собственный VPN-сервер.",
                width / 2f,
                Math.min(height - 28f * density, mainCard.bottom + 36f * density),
                textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    private String formatDuration(long millis) {
        long totalSeconds = millis / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}
