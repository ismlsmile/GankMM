package com.maning.gankmm.ui.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.afollestad.materialdialogs.MaterialDialog;
import com.alibaba.sdk.android.feedback.impl.FeedbackAPI;
import com.maning.gankmm.R;
import com.maning.gankmm.bean.AppUpdateInfo;
import com.maning.gankmm.constant.Constants;
import com.maning.gankmm.skin.SkinBroadcastReceiver;
import com.maning.gankmm.skin.SkinManager;
import com.maning.gankmm.ui.base.BaseActivity;
import com.maning.gankmm.ui.fragment.CategoryFragment;
import com.maning.gankmm.ui.fragment.HistoryFragment;
import com.maning.gankmm.ui.fragment.WelFareFragment;
import com.maning.gankmm.ui.fragment.collect.CollectFragment;
import com.maning.gankmm.ui.iView.IMainView;
import com.maning.gankmm.ui.presenter.impl.MainPresenterImpl;
import com.maning.gankmm.utils.DialogUtils;
import com.maning.gankmm.utils.InstallUtils;
import com.maning.gankmm.utils.IntentUtils;
import com.maning.gankmm.utils.MySnackbar;
import com.maning.gankmm.utils.NetUtils;
import com.maning.gankmm.utils.SharePreUtil;
import com.socks.library.KLog;
import com.umeng.analytics.MobclickAgent;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.PermissionListener;

import net.youmi.android.normal.spot.SpotManager;
import net.youmi.android.normal.video.VideoAdManager;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends BaseActivity implements IMainView {

    @Bind(R.id.navigationView)
    NavigationView navigationView;
    @Bind(R.id.drawerLayout)
    DrawerLayout drawerLayout;
    @Bind(R.id.toolbar)
    Toolbar toolbar;


    private Context context;
    private WelFareFragment welFareFragment;
    private CollectFragment collectFragment;
    private CategoryFragment categoryFragment;
    private HistoryFragment timeFragment;

    private int navigationCheckedItemId = R.id.nav_fuli;
    private String navigationCheckedTitle = "福利";
    private static final String savedInstanceStateItemId = "navigationCheckedItemId";
    private static final String savedInstanceStateTitle = "navigationCheckedTitle";

    private long exitTime = 0;

    private MainPresenterImpl mainPresenter;
    //夜间模式的广播
    private SkinBroadcastReceiver skinBroadcastReceiver;
    private MaterialDialog dialogUpdate;
    private MaterialDialog dialogCloseWarn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        KLog.i("onCreate");

        context = this;

        initMyToolBar();

        initNavigationView();

        mainPresenter = new MainPresenterImpl(this, this);
        mainPresenter.initAppUpdate();

        initIntent();

        initOtherDatas(savedInstanceState);

        setDefaultFragment();

        //注册夜间模式广播监听
        registerSkinReceiver();

        //申请权限
        requestSomePermission();

    }

    private void requestSomePermission() {

        // 先判断是否有权限。
        if (!AndPermission.hasPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) || !AndPermission.hasPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE)) {
            // 申请权限。
            AndPermission.with(MainActivity.this)
                    .requestCode(100)
                    .permission(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE)
                    .send();
        }

    }

    private void initOtherDatas(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.getInt(savedInstanceStateItemId) != 0) {
            navigationCheckedItemId = savedInstanceState.getInt(savedInstanceStateItemId);
            navigationCheckedTitle = savedInstanceState.getString(savedInstanceStateTitle);
        }
    }

    private void initMyToolBar() {
        int currentSkinType = SkinManager.getCurrentSkinType(this);
        Button rightBtnSearch = (Button) toolbar.findViewById(R.id.right_btn);
        rightBtnSearch.setVisibility(View.VISIBLE);
        if (SkinManager.THEME_DAY == currentSkinType) {
            initToolBar(toolbar, Constants.FlagWelFare, R.drawable.icon_menu2);
        } else {
            initToolBar(toolbar, Constants.FlagWelFare, R.drawable.icon_menu2_night);
        }
        rightBtnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SearchActivity.class));
            }
        });
    }

    private void initIntent() {
        Intent intent = getIntent();
        String pushMessage = intent.getStringExtra(IntentUtils.PushMessage);
        if (!TextUtils.isEmpty(pushMessage)) {
            DialogUtils.showMyDialog(this,
                    getString(R.string.gank_dialog_title_notify),
                    pushMessage,
                    getString(R.string.gank_dialog_confirm),
                    "", null);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 设置默认的Fragment显示：如果savedInstanceState不是空，证明activity被后台销毁重建了，之前有fragment，就不再创建了
     */
    private void setDefaultFragment() {
        setMenuSelection(navigationCheckedItemId);
    }

    private void setMenuSelection(int flag) {
        toolbar.setTitle(navigationCheckedTitle);

        // 开启一个Fragment事务
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        // 先隐藏掉所有的Fragment，以防止有多个Fragment显示在界面上的情况
        hideFragments(fragmentTransaction);
        switch (flag) {
            case R.id.nav_fuli:
                if (welFareFragment == null) {
                    welFareFragment = WelFareFragment.newInstance();
                    fragmentTransaction.add(R.id.frame_content, welFareFragment);
                } else {
                    fragmentTransaction.show(welFareFragment);
                }
                break;
            case R.id.nav_history:
                if (timeFragment == null) {
                    timeFragment = HistoryFragment.newInstance();
                    fragmentTransaction.add(R.id.frame_content, timeFragment);
                } else {
                    fragmentTransaction.show(timeFragment);
                }
                break;
            case R.id.nav_category:
                if (categoryFragment == null) {
                    categoryFragment = CategoryFragment.newInstance();
                    fragmentTransaction.add(R.id.frame_content, categoryFragment);
                } else {
                    fragmentTransaction.show(categoryFragment);
                }
                break;
            case R.id.nav_collect:
                if (collectFragment == null) {
                    collectFragment = CollectFragment.newInstance();
                    fragmentTransaction.add(R.id.frame_content, collectFragment);
                } else {
                    fragmentTransaction.show(collectFragment);
                }
                break;

        }
        fragmentTransaction.commit();

    }

    private void hideFragments(FragmentTransaction transaction) {
        if (welFareFragment != null) {
            transaction.hide(welFareFragment);
        }
        if (collectFragment != null) {
            transaction.hide(collectFragment);
        }
        if (categoryFragment != null) {
            transaction.hide(categoryFragment);
        }
        if (timeFragment != null) {
            transaction.hide(timeFragment);
        }

    }


    /**
     * -----------------------------------------
     */
    private void initNavigationView() {
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                setTitle(menuItem.getTitle()); // 改变页面标题，标明导航状态
                drawerLayout.closeDrawers(); // 关闭导航菜单
                switch (menuItem.getItemId()) {
                    case R.id.nav_fuli:
                    case R.id.nav_history:
                    case R.id.nav_category:
                    case R.id.nav_collect:
                        navigationCheckedItemId = menuItem.getItemId();
                        navigationCheckedTitle = menuItem.getTitle().toString();
                        setMenuSelection(menuItem.getItemId());
                        break;
                    case R.id.nav_codes:
                        menuItem.setCheckable(false);
                        //泡在网上的日子
                        Intent intent01 = new Intent(MainActivity.this, CodesActivity.class);
                        intent01.putExtra(CodesActivity.IntentType, CodesActivity.IntentTypeName_Jcode);
                        startActivity(intent01);
                        break;
                    case R.id.nav_cocoa_china:
                        menuItem.setCheckable(false);
                        //CocoaChina
                        Intent intent = new Intent(MainActivity.this, CodesActivity.class);
                        intent.putExtra(CodesActivity.IntentType, CodesActivity.IntentTypeName_CocoaChina);
                        startActivity(intent);
                        break;
                    case R.id.about:
                        menuItem.setChecked(false); // 改变item选中状态
                        //跳转
                        IntentUtils.startAboutActivity(context);
                        break;
                    case R.id.setting:
                        menuItem.setChecked(false); // 改变item选中状态
                        //跳转
                        IntentUtils.startSettingActivity(context);
                        break;
                    case R.id.share_app:
                        menuItem.setChecked(false); // 改变item选中状态
                        //分享
                        IntentUtils.startAppShareText(context, "干货营", "干货营Android客户端：" + getString(R.string.download_url));
                        break;
                }
                return true;
            }
        });
    }

    private void registerSkinReceiver() {
        if (skinBroadcastReceiver == null) {
            skinBroadcastReceiver = new SkinBroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    welFareFragment = null;
                    collectFragment = null;
                    categoryFragment = null;
                    timeFragment = null;
                    recreate();
                }
            };
            SkinManager.registerSkinReceiver(MainActivity.this, skinBroadcastReceiver);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(savedInstanceStateItemId, navigationCheckedItemId);
        outState.putString(savedInstanceStateTitle, navigationCheckedTitle);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawers();
            return;
        }
        long currtTime = System.currentTimeMillis();
        if (currtTime - exitTime > 2000) {
            MySnackbar.makeSnackBarBlack(toolbar, getString(R.string.gank_hint_exit_app));
            exitTime = currtTime;
            return;
        }

        // 不退出程序，进入后台
        moveTaskToBack(true);
        //使 App 进入后台而不是关闭
//        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
//        launcherIntent.addCategory(Intent.CATEGORY_HOME);
//        startActivity(launcherIntent);

    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);       //统计时长
        KLog.i("onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
        KLog.i("onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        KLog.i("onStop");
    }

    @Override
    protected void onDestroy() {
        KLog.i("onDestroy");
        //退出
        SpotManager.getInstance(this).onAppExit();
        VideoAdManager.getInstance(this).onAppExit();

        mainPresenter.detachView();
        SkinManager.unregisterSkinReceiver(this, skinBroadcastReceiver);
        skinBroadcastReceiver = null;
        super.onDestroy();
    }

    @Override
    public void showFeedBackDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DialogUtils.showMyDialog(MainActivity.this,
                        getString(R.string.gank_dialog_title_notify),
                        getString(R.string.gank_dialog_msg_feedback),
                        "查看", "等一会去", new DialogUtils.OnDialogClickListener() {
                            @Override
                            public void onConfirm() {
                                SharePreUtil.saveBooleanData(context, Constants.SPFeedback, false);
                                IntentUtils.startToFeedBackPage(MainActivity.this);
                            }

                            @Override
                            public void onCancel() {

                            }
                        });
            }
        });
    }

    @Override
    public void showAppUpdateDialog(final AppUpdateInfo appUpdateInfo) {
        String title = "检测到新版本:V" + appUpdateInfo.getVersionShort();
        Double appSize = Double.parseDouble(appUpdateInfo.getBinary().getFsize() + "") / 1024 / 1024;
        DecimalFormat df = new DecimalFormat(".##");
        String resultSize = df.format(appSize) + "M";
        boolean isWifi = NetUtils.isWifiConnected(this);
        String content = appUpdateInfo.getChangelog() +
                "\n\n新版大小：" + resultSize +
                "\n当前网络：" + (isWifi ? "wifi" : "非wifi环境(注意)");

        DialogUtils.showMyDialog(this,
                title, content, "立马更新", "稍后更新",
                new DialogUtils.OnDialogClickListener() {
                    @Override
                    public void onConfirm() {
                        //更新版本
                        showDownloadDialog(appUpdateInfo);
                    }

                    @Override
                    public void onCancel() {

                    }
                });
    }

    private void showDownloadDialog(AppUpdateInfo appUpdateInfo) {
        dialogUpdate = new MaterialDialog.Builder(MainActivity.this)
                .title("正在下载最新版本")
                .content("请稍等")
                .canceledOnTouchOutside(false)
                .progress(false, 100, false)
                .cancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(final DialogInterface dialog) {
                        if (dialogUpdate.isCancelled()) {
                            return;
                        }
                        dialogCloseWarn = DialogUtils.showMyDialog(MainActivity.this, "警告", "当前正在下载APK，是否关闭进度框？", "关闭", "取消", new DialogUtils.OnDialogClickListener() {
                            @Override
                            public void onConfirm() {
                                dialog.dismiss();
                            }

                            @Override
                            public void onCancel() {

                            }
                        });
                    }
                })
                .show();

        new InstallUtils(context, appUpdateInfo.getInstall_url(), Constants.UpdateAPKPath, "GankMM_" + appUpdateInfo.getVersionShort(), new InstallUtils.DownloadCallBack() {
            @Override
            public void onStart() {
                KLog.i("onStart");
                if (dialogUpdate.isCancelled()) {
                    return;
                }
                dialogUpdate.setProgress(0);
            }

            @Override
            public void onComplete(String path) {
                KLog.i("onComplete:" + path);
                InstallUtils.installAPK(context, path);
                if (dialogCloseWarn != null) {
                    dialogCloseWarn.dismiss();
                }
                if (dialogUpdate.isCancelled()) {
                    return;
                }
                dialogUpdate.dismiss();
            }

            @Override
            public void onLoading(long total, long current) {
                KLog.i("onLoading:-----total:" + total + ",current:" + current);
                if (dialogUpdate.isCancelled()) {
                    return;
                }
                dialogUpdate.setProgress((int) (current * 100 / total));
            }

            @Override
            public void onFail(Exception e) {
                if (dialogCloseWarn != null) {
                    dialogCloseWarn.dismiss();
                }
                if (dialogUpdate.isCancelled()) {
                    return;
                }
                dialogUpdate.dismiss();
            }

        }).downloadAPK();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 只需要调用这一句，其它的交给AndPermission吧，最后一个参数是PermissionListener。
        AndPermission.onRequestPermissionsResult(requestCode, permissions, grantResults, listener);
    }

    private PermissionListener listener = new PermissionListener() {
        @Override
        public void onSucceed(int requestCode, List<String> grantedPermissions) {
            MySnackbar.makeSnackBarBlack(toolbar, "权限申请成功");
        }

        @Override
        public void onFailed(int requestCode, List<String> deniedPermissions) {
            // 权限申请失败回调。
            // 用户否勾选了不再提示并且拒绝了权限，那么提示用户到设置中授权。
            if (AndPermission.hasAlwaysDeniedPermission(MainActivity.this, deniedPermissions)) {
                // 第二种：用自定义的提示语。
                AndPermission.defaultSettingDialog(MainActivity.this, 300)
                        .setTitle("权限申请失败")
                        .setMessage("我们需要的一些权限被您拒绝或者系统发生错误申请失败，请您到设置页面手动授权，否则功能无法正常使用！")
                        .setPositiveButton("好，去设置")
                        .show();
            }
        }
    };

}
