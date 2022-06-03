package com.example.reader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.nfc.tech.NfcF;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;

public class MainActivity extends AppCompatActivity {

    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("Test", "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = this.findViewById(R.id.text_view);

    }

    @Override
    protected void onResume() {
        Log.d("Test", "onResume");
        super.onResume();

        // NFC起動
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.enableReaderMode(this,
                new CustomReaderCallback(), NfcAdapter.FLAG_READER_NFC_F, null);
    }

    @Override
    protected void onPause() {
        Log.d("Test", "onResume");
        super.onPause();

        // NFC停止
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.disableReaderMode(this);

    }

    private class CustomReaderCallback implements NfcAdapter.ReaderCallback {

        String logText = "";

        @Override
        public void onTagDiscovered(Tag tag) {
            Log.d("Test", "onTagDiscovered");
            Log.d("Test", tag.toString());

            String text = "";
            String hex = "";

            NfcF nfc = NfcF.get(tag);
            try {
                // 接続
                nfc.connect();

                logText(String.format("getIDm: %s",toHex(tag.getId())));

                // 現在日時
                logText("-----------------------");
                LocalDateTime now = LocalDateTime.now();
                logText(now.toString());

                // ポーリング
                logText("Polling");
                byte[] req1 = createPollingCommand();
                logText(String.format("Polling(req): %s",toHex(req1)));
                byte[] pollingRes = nfc.transceive(req1);
                String pollingResStr = toHex(pollingRes);
                logText(String.format("Polling(res): %s", toHex(pollingRes)));
                logText("");
                //setText("bbbb");

                // iDm取得
                byte[] IDm = getIDm(pollingRes);
                logText(String.format("IDm: %s", toHex(IDm)));

                // Request Service
                logText("Request Service");
                byte[] req2 = createRequestService(IDm);
                logText(String.format("Request Service(res): %s", toHex(req2)));
                byte[] reqServiceRes = nfc.transceive(req2);
                logText(String.format("Request Service(res): %s", toHex(reqServiceRes)));
                logText("");

                // 切断
                nfc.close();

            } catch (Exception e) {
                Log.e("Test", e.getMessage() , e);
            }
        }

        // ポーリングコマンド生成
        private byte[] createPollingCommand() {
            byte[] array = new byte[6];

            // LEN
            array[0] = (byte)6;
            // コマンドコード(ポーリングは0x00)
            array[1] = (byte)0x00;
            // システムコード
            //array[2] = (byte)0xFE;
            array[2] = (byte)0x00;
            array[3] = (byte)0x03;
            // リクエストコード
            array[4] = (byte)0x00;
            // タイムスロット
            array[5] = (byte)0x0F;

            // 返却
            return array;
        }

        // RequestServiceコマンド生成
        private byte[] createRequestService(byte[] idm) {
            byte[] array = new byte[13];

            byte nodeList[] = new byte[2];
            nodeList[0] = (byte)0x00;
            nodeList[1] = (byte)0x8B;

            // LEN
            array[0] = (byte)13;
            // コマンドコード(RequestServiceは0x02)
            array[1] = (byte)0x02;
            // IDm
            array[2] = idm[0];
            array[3] = idm[1];
            array[4] = idm[2];
            array[5] = idm[3];
            array[6] = idm[4];
            array[7] = idm[5];
            array[8] = idm[6];
            array[9] = idm[7];
            // ノード数
            array[10] = (byte)1;
            // ノードリスト
            array[11] = (byte)0x8B;
            array[12] = (byte)0x00;

            // 返却
            return array;
        }

        // ポーリングのレスポンスよりIDmを取得
        private byte[] getIDm(byte[] pollingRes) {
            byte[] array = new byte[8];
            // コピー
            System.arraycopy(pollingRes, 2, array, 0, 8);
            // 返却
            return array;
        }

        // byte から hexへ変換
        private String toHex(byte[] bytes) {
            StringBuffer sb = new StringBuffer();
            for (byte b : bytes) {
                sb.append(String.format("%02x ", b));
            }

            return sb.toString();
        }

        private void logText(String s) {
            Log.d("Test", s);
            logText += s + "\n";

            //親スレッドのUIを更新するためごにょごにょ
            final Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    textView.setText(logText);
                }
            });

        }
    }
}

