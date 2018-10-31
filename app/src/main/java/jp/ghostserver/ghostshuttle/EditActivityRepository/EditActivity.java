package jp.ghostserver.ghostshuttle.EditActivityRepository;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import com.example.denpa.ghostshuttle.R;
import jp.ghostserver.ghostshuttle.DataBaseAccesser.MemoDataBaseRecord;
import jp.ghostserver.ghostshuttle.DataBaseAccesser.MemoDatabaseAccessor;
import jp.ghostserver.ghostshuttle.DataBaseAccesser.NotifyDataBaseAccessor;
import jp.ghostserver.ghostshuttle.DataBaseAccesser.NotifyDateBaseRecord;
import jp.ghostserver.ghostshuttle.memofileaccessor.MemoFileManager;
import jp.ghostserver.ghostshuttle.notifyRepository.NotifyManager;

import java.util.Random;

public class EditActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    //変数宣言
    EditText titleField, memoField;

    boolean isEdited;
    int memoID;
    boolean isNotifyEnabled;

    String _memoBeforeEditing, _titleBeforeEditing;
    private NotifyDateBaseRecord _notifyRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        //intentの受け取り
        setViews.parseIntent(this);

        //画面上部の「戻るボタン」設定
        setViews.setActionBar(this);

        //初期設定系の関数
        setViews.findIDs(this);

        //EditTextへデフォルトのテキストを入れる
        setViews.setDefaultTexts(this);

        //編集前の状態を控える
        checkValues.setBeforeEditing(this);

        //レコードの初期化
        _notifyRecord = new NotifyDateBaseRecord();

        //編集状態であれば通知の有無を確認する
        if (isEdited) {
            _notifyRecord = NotifyDataBaseAccessor.getRecordByMemoID(this, memoID);

            //nullだったらレコードが見つからなかった→通知なし
            isNotifyEnabled = !(_notifyRecord == null);

            //通知が過去の日付だったら削除
            if (isNotifyEnabled && !checkValues.checkNotifyDate(_notifyRecord)) {
                NotifyManager.notifyDisableByMemoID(this, memoID);
                isNotifyEnabled = false;
            }
        }

        //通知の状態をToolBarに反映
        invalidateOptionsMenu();
    }

    //ActionBarのメニューを設定
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();

        //統合バックキーだったら保存ボタンを非表示にする
        if (PreferenceManager.getDefaultSharedPreferences(EditActivity.this).getBoolean("backKey_move", false)) {
            inflater.inflate(R.menu.edit_menu_unit, menu);
        } else {
            inflater.inflate(R.menu.edit_menu, menu);
        }

        //通知の有無によってベルマークを差し替える
        MenuItem Notify_item = menu.findItem(R.id.notifi);
        if (isNotifyEnabled) {
            Notify_item.setIcon(R.mipmap.notifi_b);
        } else {
            Notify_item.setIcon(R.mipmap.notifi_a);
        }

        return super.onCreateOptionsMenu(menu);
    }

    //ボタンに日付表示をする関数
    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        Button button = findViewById(R.id.date_b);
        button.setText(String.valueOf(year) + "/ " + String.valueOf(month + 1) + "/ " + String.valueOf(day));

        _notifyRecord.year = year;
        _notifyRecord.month = month + 1;
        _notifyRecord.date = day;
    }

    //ボタンに時刻表示をする関数
    @Override
    public void onTimeSet(TimePicker view, int hour, int min) {
        Button button = findViewById(R.id.time_b);
        button.setText(setViews.hour_convert(this, hour) + ":" + String.format("%02d", min));

        _notifyRecord.hour = hour;
        _notifyRecord.min = min;
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
                            NotifyManager.notifyDisableByMemoID(this, memoID);
                        }
                        finish();
                    }

                } else {
                    //旧バージョン挙動・確認ダイアログの表示
                    setViews.backDialog(this);
                }
                break;

            case R.id.save:
                if (db_save()) {
                    if (isNotifyEnabled) {
                        setNotify();
                    } else {
                        NotifyManager.notifyDisableByMemoID(this, memoID);
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
                    //通知解除確認ダイアログの表示
                    setViews.showCheckingDisableNotifyDialog(this);
                } else {
                    //通知設定ダイアログの表示
                    setViews.showNotifySettingDialog(this);
                }
                break;

            case R.id.edit_cancel:
                finish();
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
                    //戻るキーに統合しているときの挙動
                    if (titleField.length() <= 0 && memoField.length() <= 0) {
                        //タイトルとメモが空白の時
                        finish();

                    } else {
                        //保存が成功したら（trueが返って来たら）activityを閉じる
                        if (db_save()) {
                            //通知系の確認とセット
                            if (isNotifyEnabled) {
                                setNotify();
                            } else {
                                NotifyManager.notifyDisableByMemoID(this, memoID);
                            }
                            finish();
                        }
                    }
                } else {
                    //戻るキーを統合してない時の動作。
                    setViews.backDialog(this);
                }

                return false;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    boolean db_save() {
        // タイトルの取得（バリデーション済み）
        String title = EditActivityFunctions.getEditingMemoTitle(this);

        //メモデータをEditTextから取得
        String memoString = memoField.getText().toString();

        //ファイルパスの取得
        String filepath;
        Random rand = new Random();
        do {
            filepath = String.valueOf(rand.nextLong());
        } while (!MemoDatabaseAccessor.checkOverlapFilepath(this, filepath));

        //編集か、新規作成かの分岐
        //true=編集 false=新規作成
        if (isEdited) {
            //編集Mode

            MemoDataBaseRecord record = new MemoDataBaseRecord(
                    memoID,
                    title,
                    memoString,
                    null,
                    isNotifyEnabled,
                    null,
                    null
            );
            if (MemoDatabaseAccessor.updateRecord(this, memoID, record) == -1) {

            }

        } else {
            //else（新規作成されていた場合。）

            //レコード形式に詰める
            MemoDataBaseRecord record = new MemoDataBaseRecord(
                    memoID,
                    title,
                    memoString,
                    null,
                    isNotifyEnabled,
                    "paper",
                    "#ffffff"
            );

            MemoDatabaseAccessor.insertMemoRecord(this, record);
        }

        //メモをファイルへ保存
        MemoFileManager.saveFile(this, filepath, memoString);
        return true;
    }

    void setNotify() {
        //BDへぶち込む
        NotifyDataBaseAccessor.insertRecord(this, _notifyRecord);

        //通知の発行
        NotifyManager.setNotify(this, _notifyRecord, titleField.getText().toString());
    }


}