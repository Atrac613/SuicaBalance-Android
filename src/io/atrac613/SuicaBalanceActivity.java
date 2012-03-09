package io.atrac613;

import io.atrac613.AbstractNfcTagFragment.INfcTagListener;
import net.kazzz.felica.FeliCaException;
import net.kazzz.felica.NfcFeliCaTagFragment;
import net.kazzz.felica.lib.FeliCaLib;
import net.kazzz.iso15693.ISO15693TagFragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class SuicaBalanceActivity extends FragmentActivity implements OnClickListener, INfcTagListener {
    private String TAG = "Suica Balance";
    private AbstractNfcTagFragment mLastFragment;
    private NfcFeliCaTagFragment mFeliCafragment;
    private ISO15693TagFragment mISO15693Fragment;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        //IMEを自動起動しない
        this.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        
        setContentView(R.layout.main);
        
        //使用するタグフラグメントを登録
        //FeliCa, FeliCaLite
        mFeliCafragment = new NfcFeliCaTagFragment(this);
        mFeliCafragment.addNfcTagListener(this);
        
        //ISO15693
        mISO15693Fragment = new ISO15693TagFragment(this);
        mISO15693Fragment.addNfcTagListener(this);
        
        //インテントから起動された際の処理
        Intent intent = this.getIntent();
        this.onNewIntent(intent);
    }
    
    public void onClick(final View v) {
        try {
            final int id = v.getId();
            
            final ProgressDialog dialog = new ProgressDialog(this);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setIndeterminate(true);

            AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
                @Override
                protected void onPreExecute() {
                    switch (id) {
                    case R.id.btn_read:
                        dialog.setMessage((String) getResources().getText(R.string.now_loading));
                        break;
                    }
                    dialog.show();
                }

                @Override
                protected String doInBackground(Void... arg0) {
                    switch (id) {
                    case R.id.btn_read:
                        try {
                            if ( mLastFragment != null && mLastFragment instanceof NfcFeliCaTagFragment) {
                                NfcFeliCaTagFragment nfcf = (NfcFeliCaTagFragment)mLastFragment;
                                return nfcf.dumpFeliCaLatestBalance();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    return "";
                }

                /* (non-Javadoc)
                 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
                 */
                @Override
                protected void onPostExecute(String result) {
                    dialog.dismiss();
                    TextView tv_tag = (TextView) findViewById(R.id.result_tv);
                    if (result != null && result.length() > 0) {
                        tv_tag.setText(result);
                        tv_tag.setTextSize(40);
                    }
                }
            };
            
            task.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /* (non-Javadoc)
     * @see android.app.Activity#onNewIntent(android.content.Intent)
     */
    @Override
    protected void onNewIntent(Intent intent) {
        if ( mFeliCafragment != null ) {
            mFeliCafragment.onNewIntent(intent);
        }
        
        if ( mISO15693Fragment != null ) {
            mISO15693Fragment.onNewIntent(intent);
        }
    }
    /* (non-Javadoc)
     * @see net.kazzz.NfcTagFragment.INfcTagListener#onTagDiscovered(android.content.Intent, android.os.Parcelable)
     */
    @Override
    public void onTagDiscovered(Intent intent, Parcelable nfcTag, AbstractNfcTagFragment fragment) {
       Button btnRead = (Button) findViewById(R.id.btn_read);
       btnRead.setOnClickListener(this);

       try {
           
           mLastFragment = fragment;
           
           //フラグメントの判定
           if ( mLastFragment instanceof NfcFeliCaTagFragment ) {
               NfcFeliCaTagFragment nff = (NfcFeliCaTagFragment)mLastFragment;
               boolean isFeliCatLite = nff.isFeliCaLite();
               try {
                   FeliCaLib.IDm idm = 
                       new FeliCaLib.IDm(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));

                   if ( idm == null ) {
                       throw new FeliCaException("Felica IDm を取得できませんでした");
                   }
                   
                   btnRead.performClick();
               } catch (Exception e) {
                   e.printStackTrace();
                   Log.e(TAG, e.toString());
               }
           } else {
               btnRead.performClick();
           }
           
       } catch ( Exception e ) {
           e.printStackTrace();
           Log.e(TAG, e.toString());
       }
    }
}