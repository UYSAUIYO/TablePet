package com.yuwen.tablepet.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.yuwen.tablepet.Service.FloatingService;
import com.yuwen.tablepet.databinding.FragmentHomeBinding;
import com.yuwen.tablepet.R;

/**
 * @author yuwen
 */
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    private final ActivityResultLauncher<Intent> overlayPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.canDrawOverlays(getContext())) {
                        Toast.makeText(getContext(), "权限已授予", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "权限被拒绝", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 初始化视图
        initViews();

        // 检查并请求悬浮窗权限
        checkOverlayPermission();

        return root;
    }

    private void initViews() {
        ImageView petImageView = binding.previewImageview;
        Button stopFloatingButton = binding.stopPetButton;

        // 加载 GIF 图片
        Glide.with(this)
                .asGif()
                .load(R.drawable.blob)
                .into(petImageView);

        // 设置按钮点击事件
        stopFloatingButton.setOnClickListener(view -> stopFloatingService());
    }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(getContext())) {
                Toast.makeText(getContext(), "权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getContext().getPackageName()));
                overlayPermissionLauncher.launch(intent);
            }
        } else {
            Toast.makeText(getContext(), "权限被拒绝", Toast.LENGTH_SHORT).show();
        }
    }


       private void stopFloatingService() {
       Activity activity = getActivity();
       if (activity != null) {
           try {
               activity.stopService(new Intent(activity, FloatingService.class));
               Toast.makeText(activity, "宠物已停止", Toast.LENGTH_SHORT).show();
           } catch (Exception e) {
               e.printStackTrace();
               Toast.makeText(activity, "停止服务时发生错误", Toast.LENGTH_SHORT).show();
           }
       } else {
           Toast.makeText(getContext(), "活动已销毁，无法停止服务", Toast.LENGTH_SHORT).show();
       }
   }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
