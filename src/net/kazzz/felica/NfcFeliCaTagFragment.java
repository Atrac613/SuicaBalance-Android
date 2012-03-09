/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.kazzz.felica;


import io.atrac613.AbstractNfcTagFragment;
import io.atrac613.R;
import net.kazzz.felica.command.ReadResponse;
import net.kazzz.felica.lib.FeliCaLib;
import net.kazzz.felica.lib.FeliCaLib.IDm;
import net.kazzz.felica.lib.FeliCaLib.ServiceCode;
import net.kazzz.felica.suica.Suica;
import net.kazzz.nfc.NfcTag;
import android.nfc.tech.NfcF;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

/**
 * NfcでFeliCa(FeliCa Lite)Tagを読み込むためのフラグメントを提供します
 * 
 * @author Kazzz.
 * @date 2011/06/24
 * @since Android API Level 9
 *
 */

public class NfcFeliCaTagFragment extends AbstractNfcTagFragment {
    public static final String TAG = "NfcFeliCaTagFragment";
    
    /**
     * コンストラクタ 
     * @param activity アクティビティをセット
     */
    public NfcFeliCaTagFragment(FragmentActivity activity) {
        super(activity, NfcFeliCaTagFragment.TAG);
        
        // FeliCa及びFeliCaLiteは NFC-F のみ
        mTechList = new String[][]{ new String[] { NfcF.class.getName() }};
    }
    /**
     * FeliCa Liteデバイスか否かを検査します
     * @return boolean 読み込み対象がFeliCa Liteの場合trueが戻ります
     * @throws FeliCaException
     */
    public boolean isFeliCaLite()  {
        FeliCaTag f = new FeliCaTag(mNfcTag); 
        //polling は IDm、PMmを取得するのに必要
        IDm idm;
        try {
            idm = f.pollingAndGetIDm(FeliCaLib.SYSTEMCODE_FELICA_LITE);
        } catch (FeliCaException e) {
            return false;
        }
        return idm != null;
    }
    /**
     * FeliCatTagクラスのインスタンスを生成します
     * @return FeliCaTag 生成したFeliCaTagクラスのインスタンスが戻ります
     */
    public FeliCaTag createFeliCaTag() {
        return new FeliCaTag(mNfcTag);
    }
    /**
     * FeliCaLiteTagクラスのインスタンスを生成します
     * @return FeliCaLiteTag 生成したFeliCaLiteTagクラスのインスタンスが戻ります
     */
    public FeliCaLiteTag createFeliCaLiteTag() {
        return new FeliCaLiteTag(mNfcTag);
    }
    /* (non-Javadoc)
     * @see net.kazzz.AbstractNfcTagFragment#createNfcTag()
     */
    @Override
    public NfcTag createNfcTag() {
        return this.isFeliCaLite()
                ? this.createFeliCaLiteTag()
                : this.createFeliCaTag();
    }
    
    /**
     * Suica の残高をダンプします
     * @return
     */
    public String dumpFeliCaLatestBalance() throws Exception {
        try {
            if ( this.isFeliCaLite() ) {
                throw new FeliCaException("Tag is not FeliCa (maybe FeliCaLite)");
            }
            FeliCaTag f = this.createFeliCaTag();

            //polling は IDm、PMmを取得するのに必要
            f.polling(FeliCaLib.SYSTEMCODE_PASMO);

            //read
            ServiceCode sc = new ServiceCode(FeliCaLib.SERVICE_SUICA_HISTORY);
            byte addr = 0;
            ReadResponse result = f.readWithoutEncryption(sc, addr);

            StringBuilder sb = new StringBuilder();
            while ( result != null && result.getStatusFlag1() == 0  ) {
                Suica.History s = new Suica.History(result.getBlockData(), this.getActivity());
                sb.append(String.format((String) getResources().getText(R.string.balance), s.getBalance()));

                addr++;
                result = f.readWithoutEncryption(sc, addr);
                break;
            }

            String str = sb.toString();
            Log.d(TAG, str);
            return str;
        } catch (FeliCaException e) {
            e.printStackTrace();
            Log.e(TAG, "readHistoryData", e);
            throw e;
        }
    }
    
}
