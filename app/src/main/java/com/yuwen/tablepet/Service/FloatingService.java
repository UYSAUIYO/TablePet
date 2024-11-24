package com.yuwen.tablepet.Service;

import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.yuwen.tablepet.FirstActivity;
import com.yuwen.tablepet.R;

public class FloatingService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;

    private GestureDetector gestureDetector;
    private Handler handler;
    private Runnable moveRunnable;

    private int screenWidth;
    private int screenHeight;

    private MediaPlayer mediaPlayer;
    private ValueAnimator xAnimator;
    private ValueAnimator yAnimator;

    @Override
    public void onCreate() {
        super.onCreate();

        // 启动前台服务
        startForeground(1, createNotification());

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_view, null);

        // 获取 ImageView 并加载 GIF
        ImageView floatingImage = floatingView.findViewById(R.id.floating_image);
        Glide.with(this)
                .asGif()
                .load(R.drawable.blob) // 替换为您的 GIF 资源
                .into(floatingImage);

        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        // 更新的 LayoutParams，调整了 flags
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                // 添加 FLAG_NOT_FOCUSABLE，移除 FLAG_LAYOUT_NO_LIMITS
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        // 设置初始位置，避免靠近屏幕边缘
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100; // 设置为 100，避免贴边
        params.y = 200;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(floatingView, params);

        // 获取屏幕尺寸
        Point size = new Point();
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        // 初始化手势检测器
        gestureDetector = new GestureDetector(this, new GestureListener());

        // 实现拖拽功能
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 传递触摸事件给手势检测器
                gestureDetector.onTouchEvent(event);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 停止随机移动
                        if (handler != null) {
                            handler.removeCallbacks(moveRunnable);
                        }

                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true; // 返回 true，表示我们处理了该事件
                    case MotionEvent.ACTION_MOVE:
                        // 计算新的位置
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        // 确保悬浮窗不超出屏幕
                        ensureInScreen();
                        // 更新悬浮窗的位置
                        windowManager.updateViewLayout(floatingView, params);
                        return true; // 返回 true，表示我们处理了该事件
                    case MotionEvent.ACTION_UP:
                        // 重新启动随机移动
                        if (handler != null) {
                            handler.postDelayed(moveRunnable, 1000);
                        }
                        return true; // 返回 true，表示我们处理了该事件
                }
                return false;
            }
        });

        // 初始化 Handler 和 Runnable，用于定时移动
        handler = new Handler();
        moveRunnable = new Runnable() {
            @Override
            public void run() {
                moveToRandomPosition();
                // 每秒移动一次
                handler.postDelayed(this, 1000);
            }
        };
        // 开始移动
        handler.post(moveRunnable);

        // 初始化 MediaPlayer
        mediaPlayer = MediaPlayer.create(this, R.raw.clip); // 替换为您的音频资源
    }

    private Notification createNotification() {
        // 创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("channel_id", "Foreground Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel_id")
                .setContentTitle("桌宠运行中")
                .setContentText("点击可返回应用")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setAutoCancel(false)
                .setOngoing(true);

        // 点击通知返回应用
        Intent intent = new Intent(this, FirstActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pendingIntent);

        return builder.build();
    }

    // 确保悬浮窗在屏幕范围内
    private void ensureInScreen() {
        // 获取浮窗的宽高
        floatingView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int viewWidth = floatingView.getMeasuredWidth();
        int viewHeight = floatingView.getMeasuredHeight();

        // 设置边界，避免靠近屏幕边缘
        int minX = 0;
        int minY = 0;
        int maxX = screenWidth - viewWidth;
        int maxY = screenHeight - viewHeight;

        if (params.x < minX) params.x = minX;
        if (params.y < minY) params.y = minY;
        if (params.x > maxX) params.x = maxX;
        if (params.y > maxY) params.y = maxY;
    }

    // 移动到随机位置，带有平滑动画
    private void moveToRandomPosition() {
        // 获取浮窗的宽高
        floatingView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int viewWidth = floatingView.getMeasuredWidth();
        int viewHeight = floatingView.getMeasuredHeight();

        int maxX = screenWidth - viewWidth;
        int maxY = screenHeight - viewHeight;

        int toX = (int) (Math.random() * maxX);
        int toY = (int) (Math.random() * maxY);

        // 创建动画并保存引用
        xAnimator = ValueAnimator.ofInt(params.x, toX);
        xAnimator.addUpdateListener(animation -> {
            params.x = (int) animation.getAnimatedValue();
            if (floatingView.isAttachedToWindow()) {
                windowManager.updateViewLayout(floatingView, params);
            }
        });

        yAnimator = ValueAnimator.ofInt(params.y, toY);
        yAnimator.addUpdateListener(animation -> {
            params.y = (int) animation.getAnimatedValue();
            if (floatingView.isAttachedToWindow()) {
                windowManager.updateViewLayout(floatingView, params);
            }
        });

        xAnimator.setInterpolator(new LinearInterpolator());
        yAnimator.setInterpolator(new LinearInterpolator());
        xAnimator.setDuration(1000);
        yAnimator.setDuration(1000);

        xAnimator.start();
        yAnimator.start();
    }


    // 手势监听器
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e) {
            // 双击播放音频
            if (mediaPlayer != null) {
                mediaPlayer.start();
            }
            return true;
        }

        @Override
        public void onLongPress(@NonNull MotionEvent e) {
            // 检查应用是否在前台
            if (!FirstActivity.isAppInForeground()) {
                // 应用不在前台，启动主界面
                Intent intent = new Intent(FloatingService.this, FirstActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        }

        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            // 必须返回 true，否则其他手势无法检测
            return true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 停止前台服务
        stopForeground(true);

        // 取消动画
        if (xAnimator != null && xAnimator.isRunning()) {
            xAnimator.cancel();
        }
        if (yAnimator != null && yAnimator.isRunning()) {
            yAnimator.cancel();
        }

        // 移除 Handler 回调
        if (handler != null) {
            handler.removeCallbacks(moveRunnable);
        }

        // 移除悬浮窗视图
        if (floatingView != null && floatingView.isAttachedToWindow()) {
            windowManager.removeView(floatingView);
        }

        // 释放媒体播放器
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
