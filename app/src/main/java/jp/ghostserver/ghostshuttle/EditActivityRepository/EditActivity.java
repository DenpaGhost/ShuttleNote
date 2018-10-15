package jp.ghostserver.ghostshuttle.EditActivityRepository;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import com.example.denpa.ghostshuttle.R;
import jp.ghostserver.ghostshuttle.AlarmBroadcastReceiver;
import jp.ghostserver.ghostshuttle.DataBaseAccesser.MemoDBHelper;
import jp.ghostserver.ghostshuttle.DatePickerDialogFragment;
import jp.ghostserver.ghostshuttle.TimePickerFragment;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Random;
import java.util.TimeZone;

public class EditActivity extends AppCompatActivity implements View.OnClickListener, DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    //変数宣言
    EditText titleField, memoField;
    Button date_b, time_b;
    private boolean isEdited;
    boolean isNotifyEnabled = false;
    int db_id;
    int year, month, day, hour, min;

    String _memoBeforeEditing, _titleBeforeEditing;

    MemoDBHelper DBHelper = new MemoDBHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        //画面上部の「戻るボタン」設定
        setViews.setActionBar(this);

        //初期設定系の関数
        setViews.findIDs(this);

        Intent intent = getIntent();
        isEdited = intent.getBooleanExtra("isEditMode", false);

        if (isEdited) {
            titleField.setText(intent.getStringExtra("TITLE"));
            memoField.setText(intent.getStringExtra("MEMO"));
            this.db_id = intent.getIntExtra("_ID", 1);

            if (intent.getIntExtra("Notify", 0) == 1) {
                isNotifyEnabled = true;

                //データベースの取得・クエリ実行
                SQLiteDatabase read_db = DBHelper.getReadableDatabase();
                Cursor cursor = read_db.query("NOTIFICATION", new String[]{"notifi_year", "notifi_month", "notifi_day", "notifi_hour", "notifi_min"}, "_id = '" + db_id + "'", null, null, null, null, null);
                cursor.moveToFirst();
                Log.d("test", String.valueOf(db_id));

                year = cursor.getInt(0);
                month = cursor.getInt(1);
                day = cursor.getInt(2);
                hour = cursor.getInt(3);
                min = cursor.getInt(4);

                cursor.close();
                read_db.close();

                //通知時間の確認（過去だったらFalse）
                Calendar calendar = Calendar.getInstance();
                if (calendar.get(Calendar.YEAR) - year > 0) {
                    isNotifyEnabled = false;
                } else if (calendar.get(Calendar.MONTH) - month > 0) {
                    isNotifyEnabled = false;
                } else if (calendar.get(Calendar.DAY_OF_MONTH) - day > 0) {
                    isNotifyEnabled = false;
                } else if (calendar.get(Calendar.HOUR_OF_DAY) - hour > 0) {
                    isNotifyEnabled = false;
                } else if (calendar.get(Calendar.MINUTE) - min >= 0) {
                    isNotifyEnabled = false;
                }

            }

        } else {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            titleField.setText(getResources().getString(R.string.titleTemplate));
            memoField.setText(pref.getString(getResources().getString(R.string.memoTemplate), ""));
        }

        checkValues.setBeforeEditing(this);
        invalidateOptionsMenu();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            //日付を設定するやつ
            case R.id.date_b:

                DatePickerDialogFragment newDateFragment = new DatePickerDialogFragment();
                newDateFragment.show(getFragmentManager(), "datePicker");

                break;

            //時間設定するやつ
            case R.id.time_b:

                TimePickerFragment newTimeFragment = new TimePickerFragment();
                newTimeFragment.show(getSupportFragmentManager(), "timePicker");

                break;

            default:
                break;
        }

    }

    //ActionBarのメニューを設定
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();

        if (PreferenceManager.getDefaultSharedPreferences(EditActivity.this).getBoolean("backKey_move", false)) {
            inflater.inflate(R.menu.edit_menu_unit, menu);
        } else {
            inflater.inflate(R.menu.edit_menu, menu);
        }

        if (isNotifyEnabled) {

            MenuItem Notify_item = menu.findItem(R.id.notifi);
            Notify_item.setIcon(R.mipmap.notifi_b);

        } else {

            MenuItem Notify_item = menu.findItem(R.id.notifi);
            Notify_item.setIcon(R.mipmap.notifi_a);

        }

        return super.onCreateOptionsMenu(menu);
    }

    //日付・時刻設定ボタンの初期値設定
    private void setPrimary() {
        if (!isNotifyEnabled) {
            Calendar calendar = Calendar.getInstance();
            int Month = calendar.get(Calendar.MONTH) + 1;
            date_b.setText(calendar.get(Calendar.YEAR) + "/ " + Month + "/ " + calendar.get(Calendar.DAY_OF_MONTH));
            time_b.setText(setViews.hour_convert(this, calendar.get(Calendar.HOUR_OF_DAY)) + ":" + String.format("%02d", calendar.get(Calendar.MINUTE)));

            this.year = calendar.get(Calendar.YEAR);
            this.month = calendar.get(Calendar.MONTH);
            this.day = calendar.get(Calendar.DAY_OF_MONTH);

            this.hour = calendar.get(Calendar.HOUR_OF_DAY);
            this.min = calendar.get(Calendar.MINUTE);
        }

    }

    //ボタンに日付表示をする関数
    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        date_b.setText(String.valueOf(year) + "/ " + String.valueOf(month + 1) + "/ " + String.valueOf(day));

        this.year = year;
        this.month = month;
        this.day = day;
    }

    //ボタンに時刻表示をする関数
    @Override
    public void onTimeSet(TimePicker view, int hour, int min) {
        time_b.setText(setViews.hour_convert(this, hour) + ":" + String.format("%02d", min));

        this.hour = hour;
        this.min = min;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        boolean result;
        switch (id) {

            //「戻るボタン」のクリックイベント
            case android.R.id.home:

                if (PreferenceManager.getDefaultSharedPreferences(EditActivity.this).getBoolean("backKey_move", false)) {

                    //統合戻るキーの挙動
                    if (db_save()) {
                        if (isNotifyEnabled) {
                            setNotify();
                        } else {
                            Notify_cancel();
                        }

                        finish();
                    }
                } else {
                    //旧バージョン挙動・確認ダイアログの表示
                    backDialog();
                }

                break;

            case R.id.save:

                if (db_save()) {
                    if (isNotifyEnabled) {
                        setNotify();
                    } else {
                        Notify_cancel();
                    }
                    finish();
                }
                break;

            case R.id.now_save:

                db_save();
                checkValues.setBeforeEditing(this);

                isEdited = true;
                break;

            case R.id.notifi:

                if (isNotifyEnabled) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getResources().getString(R.string.notify_disable));
                    builder.setPositiveButton(getResources().getString(R.string.disable), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // OK ボタンクリック処理
                            isNotifyEnabled = false;
                            invalidateOptionsMenu();
                        }
                    });
                    builder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Cancel ボタンクリック処理
                        }
                    });
                    // 表示
                    builder.create().show();

                } else {
                    // アラートダイアログ を生成
                    LayoutInflater a_inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
                    final View notify_dialog = a_inflater.inflate(R.layout.notifi_dialog, (ViewGroup) findViewById(R.id.notifidialog_cl));

                    date_b = notify_dialog.findViewById(R.id.date_b);
                    time_b = notify_dialog.findViewById(R.id.time_b);
                    date_b.setOnClickListener(this);
                    time_b.setOnClickListener(this);

                    setPrimary();

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getResources().getString(R.string.Notify));
                    builder.setView(notify_dialog);
                    builder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // OK ボタンクリック処理
                            isNotifyEnabled = true;
                            invalidateOptionsMenu();
                        }
                    });
                    builder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Cancel ボタンクリック処理
                            invalidateOptionsMenu();
                        }
                    });
                    // 表示
                    builder.create().show();
                }

                break;

            case R.id.edit_cancel:

                finish();

                break;

            default:

                break;

        }
        result = super.onOptionsItemSelected(item);
        return result;

    }

    //戻るキーのクリックイベント
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {

                if (PreferenceManager.getDefaultSharedPreferences(EditActivity.this).getBoolean("backKey_move", false)) {
                    //統合戻るキーの挙動
                    if (titleField.length() <= 0 && memoField.length() <= 0) {
                        //タイトルかメモが空白の時
                        finish();

                    } else {

                        if (db_save()) {
                            if (isNotifyEnabled) {
                                setNotify();
                            } else {
                                Notify_cancel();
                            }

                            finish();

                        }

                    }

                } else {
                    //旧バージョン挙動・確認ダイアログの表示
                    backDialog();
                }

                return false;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void backDialog() {

        if (_titleBeforeEditing.equals(titleField.getText().toString()) && _memoBeforeEditing.equals(memoField.getText().toString())) {

            finish();

        } else {

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(getResources().getString(R.string.edit_cancel));
            alertDialogBuilder.setMessage(getResources().getString(R.string.cancel));
            alertDialogBuilder.setPositiveButton(getResources().getString(R.string.save),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (db_save()) {
                                if (isNotifyEnabled) {
                                    setNotify();
                                } else {
                                    Notify_cancel();
                                }
                                finish();
                            }
                        }
                    });

            alertDialogBuilder.setNegativeButton(getResources().getString(R.string.edit_cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });

            alertDialogBuilder.setCancelable(true);
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();

        }

    }

    private boolean db_save() {
        //データベースオブジェクトの取得（書き込み可能）
        SQLiteDatabase memo_db = DBHelper.getWritableDatabase();

        //メモデータをEditTextから取得
        String memo_raw = memoField.getText().toString();

        String title_raw;
        boolean title_not = true;
        int count = 0;
        Random rand = new Random();
        long filepath = rand.nextLong();

        if (titleField.length() != 0) {
            // タイトルの取得
            title_raw = titleField.getText().toString();
        } else {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            title_raw = pref.getString("default_title", "");
            title_not = false;
        }

        //データベースに保存するレコードの用意
        ContentValues values = new ContentValues();
        values.put("titleField", title_raw);
        values.put("filepath", String.valueOf(filepath));

        if (isNotifyEnabled == true) {
            values.put("notifi_enabled", true);
        } else {
            values.put("notifi_enabled", false);
        }

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        String timestamp = String.format("%d-%02d-%02d %02d:%02d:%02d", calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));

        values.put("data_modified", timestamp);

        //編集か、新規作成かの分岐
        //true=編集 false=新規作成
        if (isEdited) {
            //編集Mode

            String where_words = "_id = " + db_id;

            while (true) {
                try {
                    memo_db.update("memo", values, where_words, null);
                    break;
                } catch (Exception e) {
                    if (title_not) {
                        //データベースへ追加に失敗したときの処理
                        ConstraintLayout cl = findViewById(R.id.cl);
                        Snackbar.make(cl, getResources().getString(R.string.DB_failed), Snackbar.LENGTH_SHORT).show();
                        return false;
                    } else {
                        count++;
                        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
                        title_raw = pref.getString("default_title", "") + "(" + count + ")";
                        values.put("titleField", title_raw);
                        titleField.setText(title_raw);
                    }
                }
            }

        } else {
            //else（新規作成されていた場合。）

            values.put("icon_img", "paper");
            values.put("icon_color", "#ffffff");

            //データベースへ保存する記述
            long db_id = memo_db.insert("memo", null, values);

            if (db_id == -1) {
                if (title_not) {
                    //データベースへ追加に失敗したときの処理
                    ConstraintLayout cl = findViewById(R.id.cl);
                    Snackbar.make(cl, getResources().getString(R.string.DB_failed), Snackbar.LENGTH_SHORT).show();
                    return false;
                } else {
                    while (db_id == -1) {
                        count++;
                        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
                        title_raw = pref.getString("default_title", "") + "(" + count + ")";
                        values.put("titleField", title_raw);
                        filepath = rand.nextLong();
                        values.put("filepath", String.valueOf(filepath));
                        db_id = memo_db.insert("memo", null, values);
                    }
                    titleField.setText(title_raw);
                }
            }

            Log.d("test", String.valueOf(db_id));
            this.db_id = (int) db_id;
        }

        memo_db.close();

        saveFile(String.valueOf(filepath), memo_raw);
        return true;
    }

    private void setNotify() {

        //データベースオブジェクトの取得（書き込み可能）
        SQLiteDatabase Notifi_db = DBHelper.getWritableDatabase();

        //データベースに保存するレコードの用意
        ContentValues values = new ContentValues();

        values.put("_id", this.db_id);
        values.put("notifi_year", this.year);
        values.put("notifi_month", this.month);
        values.put("notifi_day", this.day);
        values.put("notifi_hour", this.hour);
        values.put("notifi_min", this.min);

        long test = Notifi_db.insert("NOTIFICATION", null, values);
        if (test == -1) {
            String where_words = "_ID = '" + this.db_id + "'";
            Notifi_db.update("NOTIFICATION", values, where_words, null);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, this.year);
        calendar.set(Calendar.MONTH, this.month);// 7=>8月
        calendar.set(Calendar.DATE, this.day);
        calendar.set(Calendar.HOUR_OF_DAY, this.hour);
        calendar.set(Calendar.MINUTE, this.min);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Intent intent = new Intent(getApplicationContext(), AlarmBroadcastReceiver.class);
        intent.putExtra("ID", this.db_id);
        intent.putExtra("titleField", titleField.getText().toString());

        PendingIntent pending = PendingIntent.getBroadcast(getApplicationContext(), this.db_id, intent, 0);
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pending);

        Notifi_db.close();


    }

    private void Notify_cancel() {
        Intent intent = new Intent(getApplicationContext(), AlarmBroadcastReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(getApplicationContext(), this.db_id, intent, 0);

        // アラームを解除する
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.cancel(pending);

    }

    public void saveFile(String filepath, String memo) {
        try {
            String str = memo;
            FileOutputStream out = openFileOutput(filepath + ".gs", MODE_PRIVATE);
            out.write(str.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}