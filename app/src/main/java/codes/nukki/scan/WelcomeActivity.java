package codes.nukki.scan;


import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.icu.util.TimeZone;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import android.view.Menu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.security.Timestamp;
import java.util.Date;
import java.util.logging.Handler;

        import android.view.Menu;
        import android.widget.TextView;
import android.widget.Toast;


public class WelcomeActivity extends AppCompatActivity {

    // list of NFC technologies detected
    private final String[][] techList = new String[][] {
            new String[] {
                    NfcA.class.getName(),
                    NfcB.class.getName(),
                    NfcF.class.getName(),
                    NfcV.class.getName(),
                    IsoDep.class.getName(),
                    MifareClassic.class.getName(),
                    MifareUltralight.class.getName(), Ndef.class.getName()
            }
    };

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        TextView greetingText = (TextView)findViewById(R.id.welcomeText);
        TextView commandView = (TextView)findViewById(R.id.commandText);

        // chào
        String greeting = "Hello, Teacher. \n Welcome to XCard on this amazing";
        String command = "Please tap your ID card to log in.";

        int weekDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

        switch (weekDay) {
            case Calendar.MONDAY:
                greeting += " Monday.";
                break;
            case Calendar.TUESDAY:
                greeting += " Tuesday";
                break;
            case Calendar.WEDNESDAY:
                greeting += " Wednesday";
                break;
            case Calendar.THURSDAY:
                greeting += " Thursday";
                break;
            case Calendar.FRIDAY:
                greeting += " Friday";
                break;
            case Calendar.SATURDAY:
                greeting += " Saturday";
                break;
            case Calendar.SUNDAY:
                greeting += " Sunday!!!";
                break;
        }

        greetingText.setText(greeting);
        commandView.setText(command);

        greetingText.startAnimation(AnimationUtils.loadAnimation(WelcomeActivity.this, android.R.anim.fade_in));
        commandView.startAnimation(AnimationUtils.loadAnimation(WelcomeActivity.this, android.R.anim.slide_in_left));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // tạo tiền xử lý nfc
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        // dò nfc trên thiết bị (android.com)
        IntentFilter filter = new IntentFilter();
        filter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
        filter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED);
        // thiết bị có nfc -> pass
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, new IntentFilter[]{filter}, this.techList);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // thiết bị ko nfc -> crash
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            String tag = ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));

            //Toast.makeText(this, tag, Toast.LENGTH_LONG).show();
            //0440BF8B700000

            if (tag.equals("0440BF8B700000")) {
                Intent changeIntent = new Intent(this, OptionsActivity.class);
                startActivity(changeIntent);

                overridePendingTransition(R.anim.fadein, R.anim.fadeout);
            }
        }
    }

// chuyển mã chip sang hex
    private String ByteArrayToHexString(byte [] inarray) {
        int i, j, in;
        String [] hex = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
        String out= "";

        for(j = 0 ; j < inarray.length ; ++j)
        {
            in = (int) inarray[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out += hex[i];
            i = in & 0x0f;
            out += hex[i];
        }
        return out;
    }
}

