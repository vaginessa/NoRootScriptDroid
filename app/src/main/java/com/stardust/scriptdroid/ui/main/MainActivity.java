package com.stardust.scriptdroid.ui.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.folderselector.FileChooserDialog;
import com.stardust.app.NotRemindAgainDialog;
import com.stardust.app.OnActivityResultDelegate;
import com.stardust.scriptdroid.Pref;
import com.stardust.scriptdroid.droid.script.file.ScriptFile;
import com.stardust.scriptdroid.droid.script.file.ScriptFileList;
import com.stardust.scriptdroid.droid.script.file.SharedPrefScriptFileList;
import com.stardust.scriptdroid.external.notification.record.ActionRecordSwitchNotification;
import com.stardust.scriptdroid.file.SampleFileManager;
import com.stardust.scriptdroid.record.inputevent.InputEventRecorder;
import com.stardust.scriptdroid.record.inputevent.InputEventToJsRecorder;
import com.stardust.scriptdroid.service.AccessibilityWatchDogService;
import com.stardust.scriptdroid.tool.AccessibilityServiceTool;
import com.stardust.scriptdroid.tool.ImageSelector;
import com.stardust.scriptdroid.ui.BaseActivity;
import com.stardust.scriptdroid.ui.settings.SettingsActivity;
import com.stardust.theme.dialog.ThemeColorMaterialDialogBuilder;
import com.stardust.view.ViewBinding;
import com.stardust.widget.SlidingUpPanel;
import com.stardust.scriptdroid.BuildConfig;
import com.stardust.scriptdroid.R;
import com.stardust.scriptdroid.file.FileUtils;
import com.stardust.scriptdroid.tool.BackPressedHandler;
import com.stardust.scriptdroid.ui.main.operation.ScriptFileOperation;
import com.stardust.view.ViewBinder;
import com.stardust.view.accessibility.AccessibilityServiceUtils;

import java.io.File;


public class MainActivity extends BaseActivity implements FileChooserDialog.FileCallback {

    private static final String EXTRA_ACTION = "EXTRA_ACTION";

    public static final String ACTION_NOTIFY_SCRIPT_LIST_CHANGE = "ACTION_NOTIFY_SCRIPT_LIST_CHANGE";
    private static final String ACTION_ON_ACTION_RECORD_STOPPED = "ACTION_ON_ACTION_RECORD_STOPPED";
    private static final String ARGUMENT_SCRIPT = "ARGUMENT_SCRIPT";
    private static final String ACTION_ON_ROOT_RECORD_STOPPED = "ACTION_ON_ROOT_RECORD_STOPPED";

    private SlidingUpPanel mAddFilePanel;
    private ScriptListRecyclerView mScriptListRecyclerView;
    private ScriptFileList mScriptFileList;
    private DrawerLayout mDrawerLayout;
    private Receiver mReceiver;
    private OnActivityResultDelegate.Manager mManager = new OnActivityResultDelegate.Manager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpUI();
        checkPermissions();
        registerReceivers();
        handleIntent(getIntent());
    }

    private void registerReceivers() {
        mReceiver = new Receiver();
        registerReceiver(mReceiver, new IntentFilter(ACTION_NOTIFY_SCRIPT_LIST_CHANGE));
    }

    private void checkPermissions() {
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        goToAccessibilityPermissionSettingIfDisabled();
    }

    private void goToAccessibilityPermissionSettingIfDisabled() {
        if (!AccessibilityServiceUtils.isAccessibilityServiceEnabled(this, AccessibilityWatchDogService.class)) {
            new NotRemindAgainDialog.Builder(this, "goToAccessibilityPermissionSettingIfDisabled")
                    .title(R.string.text_alert)
                    .content(R.string.explain_accessibility_permission)
                    .positiveText(R.string.text_go_to_setting)
                    .negativeText(R.string.text_cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            AccessibilityServiceTool.enableAccessibilityService();
                        }
                    }).show();
        }
    }

    private void addScriptFile(final String path) {
        new ThemeColorMaterialDialogBuilder(this).title(R.string.text_name)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(getString(R.string.text_please_input_name), new File(path).getName(), new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        MainActivity.this.addScriptFile(input.toString(), path);
                    }
                }).show();
    }

    private void addScriptFile(String name, String path) {
        mScriptFileList.add(new ScriptFile(name, path));
        mScriptListRecyclerView.getAdapter().notifyItemInserted(mScriptFileList.size() - 1);
    }

    private void setUpUI() {
        mDrawerLayout = (DrawerLayout) View.inflate(this, R.layout.activity_main, null);
        setContentView(mDrawerLayout);
        SlideMenuFragment.setFragment(this, R.id.fragment_slide_menu);
        mAddFilePanel = $(R.id.bottom_menu);

        setUpToolbar();
        setUpDrawerHeader();
        setUpScriptList();
        ViewBinder.bind(this);

    }

    @SuppressLint("SetTextI18n")
    private void setUpDrawerHeader() {
        TextView version = $(R.id.version);
        version.setText("Version " + BuildConfig.VERSION_NAME);
        String path = Pref.def().getString(Pref.KEY_DRAWER_HEADER_IMAGE_PATH, null);
        if (path != null) {
            setDrawerHeaderImage(path);
        }
        $(R.id.drawer).setFitsSystemWindows(false);
    }

    private void setDrawerHeaderImage(String path) {
        Drawable d = BitmapDrawable.createFromPath(path);
        if (d != null)
            ((ImageView) $(R.id.drawer_header_img)).setImageDrawable(d);
    }

    private void setUpToolbar() {
        Toolbar toolbar = $(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string._app_name);

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.text_drawer_open,
                R.string.text_drawer_close);
        drawerToggle.syncState();
        mDrawerLayout.addDrawerListener(drawerToggle);
    }

    private void setUpScriptList() {
        mScriptListRecyclerView = $(R.id.script_list);
        mScriptFileList = SharedPrefScriptFileList.getInstance();
        mScriptListRecyclerView.setScriptFileList(mScriptFileList);
    }

    @ViewBinding.Click(R.id.add)
    private void showAddFilePanel() {
        mAddFilePanel.show();
    }

    @ViewBinding.Click(R.id.create_new_file)
    private void createScriptFile() {
        createScriptFileForScript(null);
    }


    private void createScriptFileForScript(final String script) {
        new ThemeColorMaterialDialogBuilder(this).title(R.string.text_name)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(getString(R.string.text_please_input_name), "", new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        String path = FileUtils.generateNotExistingPath(ScriptFile.DEFAULT_FOLDER + input, ".js");
                        MainActivity.this.createScriptFile(input.toString(), path, script);
                    }
                })
                .show();
    }

    private void createScriptFile(String name, String path, String script) {
        if (FileUtils.createFileIfNotExists(path)) {
            if (script != null) {
                if (!FileUtils.writeString(path, script)) {
                    Snackbar.make(mDrawerLayout, R.string.text_file_write_fail, Snackbar.LENGTH_LONG).show();
                }
            }
            addScriptFile(name, path);
            new ScriptFileOperation.Edit().operate(mScriptListRecyclerView, mScriptFileList, mScriptFileList.size() - 1);
        } else {
            Snackbar.make(mDrawerLayout, R.string.text_file_create_fail, Snackbar.LENGTH_LONG).show();
        }
    }

    @ViewBinding.Click(R.id.import_from_file)
    private void showFileChooser() {
        new FileChooserDialog.Builder(this)
                .initialPath(ScriptFile.DEFAULT_FOLDER)
                .extensionsFilter(".js", ".txt")
                .show();
    }

    @ViewBinding.Click(R.id.record)
    private void startScriptRecord() {
        if (AccessibilityWatchDogService.getInstance() == null) {
            Snackbar.make(mDrawerLayout, R.string.text_need_enable_accessibility_service, Snackbar.LENGTH_SHORT).show();
            return;
        }
        ActionRecordSwitchNotification.showOrUpdateNotification();
        Snackbar.make(mDrawerLayout, R.string.hint_start_record, Snackbar.LENGTH_SHORT).show();
    }

    private InputEventRecorder mInputEventRecorder;

    @ViewBinding.Click(R.id.root_record)
    private void startRootRecord() {
        if (mInputEventRecorder == null) {
            mInputEventRecorder = new InputEventToJsRecorder();
            mInputEventRecorder.listen();
        }
        Snackbar.make(mDrawerLayout, R.string.hint_start_root_record, Snackbar.LENGTH_SHORT).show();
    }

    @ViewBinding.Click(R.id.setting)
    private void startSettingActivity() {
        startActivity(new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @ViewBinding.Click(R.id.exit)
    public void finish() {
        super.finish();
    }

    @ViewBinding.Click(R.id.drawer_header_img)
    public void selectHeaderImage() {
        new ImageSelector(this, mManager, new ImageSelector.ImageSelectorCallback() {
            @Override
            public void onImageSelected(String path) {
                setDrawerHeaderImage(path);
                Pref.def().edit().putString(Pref.KEY_DRAWER_HEADER_IMAGE_PATH, path).apply();
            }
        }).select();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            SampleFileManager.getInstance().copySampleScriptFileIfNeeded();
            mScriptListRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void onFileSelection(@NonNull FileChooserDialog dialog, @NonNull File file) {
        addScriptFile(file.getPath());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getStringExtra(EXTRA_ACTION);
        if (action == null)
            return;
        switch (action) {
            case ACTION_ON_ACTION_RECORD_STOPPED:
                handleRecordedScript(intent.getStringExtra(ARGUMENT_SCRIPT));
                break;
            case ACTION_ON_ROOT_RECORD_STOPPED:
                if (mInputEventRecorder != null) {
                    mInputEventRecorder.stop();
                    handleRecordedScript(mInputEventRecorder.getCode());
                    mInputEventRecorder = null;
                }
                break;
        }
    }

    private void handleRecordedScript(final String script) {
        new ThemeColorMaterialDialogBuilder(this)
                .title(R.string.text_recorded)
                .items(getString(R.string.text_new_file), getString(R.string.text_copy_to_clip))
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                        if (position == 0) {
                            createScriptFileForScript(script);
                        } else {
                            ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
                                    .setPrimaryClip(ClipData.newPlainText("script", script));
                            Toast.makeText(MainActivity.this, R.string.text_already_copy_to_clip, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .negativeText(R.string.text_cancel)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .canceledOnTouchOutside(false)
                .show();

    }

    private BackPressedHandler mBackPressedHandler = new BackPressedHandler.DoublePressExit(this);

    @Override
    public void onBackPressed() {
        if (mScriptListRecyclerView.getScriptFileOperationPopupMenu().isShowing()) {
            mScriptListRecyclerView.getScriptFileOperationPopupMenu().dismiss();
        } else if (mAddFilePanel.isShowing()) {
            mAddFilePanel.dismiss();
        } else {
            mBackPressedHandler.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mManager.onActivityResult(requestCode, resultCode, data);
    }

    public static void onActionRecordStopped(Context context, String script) {
        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_ACTION, ACTION_ON_ACTION_RECORD_STOPPED)
                .putExtra(ARGUMENT_SCRIPT, script);
        context.startActivity(intent);
    }


    public static void onRootRecordStopped(Context context) {
        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_ACTION, ACTION_ON_ROOT_RECORD_STOPPED);
        context.startActivity(intent);
    }


    private class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_NOTIFY_SCRIPT_LIST_CHANGE)) {
                mScriptListRecyclerView.getAdapter().notifyDataSetChanged();
            }
        }
    }
}