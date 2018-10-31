package jp.ghostserver.ghostshuttle.EditActivityRepository;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.example.denpa.ghostshuttle.R;
import jp.ghostserver.ghostshuttle.DataBaseAccesser.MemoDataBaseRecord;
import jp.ghostserver.ghostshuttle.DataBaseAccesser.MemoDatabaseAccessor;
import jp.ghostserver.ghostshuttle.DatePickerDialogFragment;
import jp.ghostserver.ghostshuttle.TimePickerFragment;
import jp.ghostserver.ghostshuttle.memofileaccessor.MemoFileManager;
import jp.ghostserver.ghostshuttle.notifyRepository.NotifyManager;

import java.util.Calendar;
import java.util.Objects;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

class setViews {
    static void findIDs(EditActivity activity) {
        activity.titleField = activity.findViewById(R.id.editText);
        activity.memoField = activity.findViewById(R.id.editmemo);
    }

    static void parseIntent(EditActivity activity) {
        Intent intent = activity.getIntent();
        activity.isEdited = intent.getBooleanExtra("isEditMode", false);
        activity.memoID = intent.getIntExtra("_ID", -1);
    }

    static void setActionBar(EditActivity activity) {
        Toolbar toolbar = activity.findViewById(R.id.toolbar);
        activity.setSupportActionBar(toolbar);
        Objects.requireNonNull(activity.getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        //画面上部のタイトル設定
        activity.setTitle(activity.getResources().getString(R.string.edit_title));
    }

    static String hour_convert(EditActivity activity, int hour) {
        String am_pm = activity.getResources().getString(R.string.am);

        if (hour >= 12) {
            am_pm = activity.getResources().getString(R.string.pm);
            hour = hour - 12;
        }
        return (am_pm + " " + hour);
    }

    static void setDefaultTexts(EditActivity activity) {
        if (activity.isEdited) {
            MemoDataBaseRecord record = MemoDatabaseAccessor.getRecordById(activity, activity.memoID);
            if (record == null) {
                return;
            }
            activity.titleField.setText(record.getMemoTitle());
            activity.memoField.setText(MemoFileManager.readFile(activity, record.getFilePath()));
        } else {
            //新規作成時の動作
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(activity);

            //テンプレートの適用
            activity.titleField.setText(activity.getResources().getString(R.string.titleTemplate));
            activity.memoField.setText(pref.getString(activity.getResources().getString(R.string.memoTemplate), ""));
        }
    }

    //日付・時刻設定ボタンの初期値設定
    static void setPrimary(EditActivity activity, Button dateButton, Button timeButton) {
        if (!activity.isNotifyEnabled) {
            Calendar calendar = Calendar.getInstance();
            int Month = calendar.get(Calendar.MONTH) + 1;
            dateButton.setText(calendar.get(Calendar.YEAR) + "/ " + Month + "/ " + calendar.get(Calendar.DAY_OF_MONTH));
            timeButton.setText(setViews.hour_convert(activity, calendar.get(Calendar.HOUR_OF_DAY)) + ":" + String.format("%02d", calendar.get(Calendar.MINUTE)));
        }
    }

    static void showNotifySettingDialog(final EditActivity activity) {
        // アラートダイアログ を生成
        LayoutInflater a_inflater = (LayoutInflater) activity.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View notify_dialog = a_inflater.inflate(R.layout.notifi_dialog, (ViewGroup) activity.findViewById(R.id.notifidialog_cl));

        Button dateDialogButton = notify_dialog.findViewById(R.id.date_b);
        dateDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialogFragment newDateFragment = new DatePickerDialogFragment();
                newDateFragment.show(activity.getFragmentManager(), "datePicker");
            }
        });

        Button timeDialogButton = notify_dialog.findViewById(R.id.time_b);
        timeDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerFragment newTimeFragment = new TimePickerFragment();
                newTimeFragment.show(activity.getSupportFragmentManager(), "timePicker");
            }
        });

        setPrimary(activity, dateDialogButton, timeDialogButton);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getResources().getString(R.string.Notify));
        builder.setView(notify_dialog);
        builder.setPositiveButton(activity.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // OK ボタンクリック処理
                activity.isNotifyEnabled = true;
                activity.invalidateOptionsMenu();
            }
        });
        builder.setNegativeButton(activity.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Cancel ボタンクリック処理
                activity.invalidateOptionsMenu();
            }
        });
        // 表示
        builder.create().show();
    }

    static void showCheckingDisableNotifyDialog(final EditActivity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getResources().getString(R.string.notify_disable));
        builder.setPositiveButton(activity.getResources().getString(R.string.disable), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // OK ボタンクリック処理
                activity.isNotifyEnabled = false;
                activity.invalidateOptionsMenu();
            }
        });
        builder.setNegativeButton(activity.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Cancel ボタンクリック処理
            }
        });
        // 表示
        builder.create().show();
    }

    static void showDatabaseErrorSnackBar(EditActivity activity) {
        ConstraintLayout cl = activity.findViewById(R.id.cl);
        Snackbar.make(cl, activity.getResources().getString(R.string.DB_failed), Snackbar.LENGTH_SHORT).show();
    }

    static void backDialog(final EditActivity activity) {

        if (activity._titleBeforeEditing.equals(activity.titleField.getText().toString()) && activity._memoBeforeEditing.equals(activity.memoField.getText().toString())) {

            activity.finish();

        } else {

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
            alertDialogBuilder.setTitle(activity.getResources().getString(R.string.edit_cancel));
            alertDialogBuilder.setMessage(activity.getResources().getString(R.string.cancel));

            //ポジティブボタン
            alertDialogBuilder.setPositiveButton(activity.getResources().getString(R.string.save),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (activity.db_save()) {
                                if (activity.isNotifyEnabled) {
                                    activity.setNotify();
                                } else {
                                    NotifyManager.notifyDisableByMemoID(activity, activity.memoID);
                                }
                                activity.finish();
                            }
                        }
                    });

            //ネガティブボタン
            alertDialogBuilder.setNegativeButton(activity.getResources().getString(R.string.edit_cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            activity.finish();
                        }
                    });

            alertDialogBuilder.setCancelable(true);
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();

        }

    }
}