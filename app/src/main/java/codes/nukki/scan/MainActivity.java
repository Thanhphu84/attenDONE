package codes.nukki.scan;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.Window;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends Activity {
    HashMap<String, Student> students = new HashMap<>();
    ListView attendance_list;
    public static final String PREFS_NAME = "MyPrefsFile";

    // list of NFC technologies detected:
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        readStudentsFromJson();
        attendance_list = (ListView) findViewById(R.id.attendance_sheet);
        attendance_list.setAdapter(new AttendeeListAdapter(this, students));
    }



    @Override
    protected void onResume() {
        super.onResume();
        // tạo intent:
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        // xử lý NFC events:
        IntentFilter filter = new IntentFilter();
        filter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
        filter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED);
        // giao thức đọc thẻ NFC
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, new IntentFilter[]{filter}, this.techList);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // disabling foreground dispatch:
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            String tag = ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));

            if (students.containsKey(tag)) {
                students.get(tag).status = true;
                attendance_list.setAdapter(new AttendeeListAdapter(this, students));
            }
        }
    }

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

    private void addNewStudent(String name, String id, String seriaNum){
        Student newStudent = new Student();
        newStudent.name = name;
        newStudent.ID = id;
        newStudent.status = false;

        students.put(seriaNum, newStudent);
    }

    public void onDoneClicked(View v) {
        SharedPreferences sharedPref = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = sharedPref.edit();
        ArrayList<String> missed = new ArrayList<>();
        Iterator it = students.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            // tag is key
            // student is value
            String tag = (String) pair.getKey();
            Student st = (Student) pair.getValue();
            if(st.status == false) {
                Log.e("Trying ", "to put into prefs");
                int numberMissed = sharedPref.getInt(st.name,0);
                editor.putInt(st.name, numberMissed + 1);
                editor.commit();

            }
            it.remove(); // kt ngoại lệ
        }


//     ghi vào lịch sử;
        Intent intent = new Intent(this, OptionsActivity.class);
        intent.putExtra("missed", (Serializable) sharedPref.getAll());
        startActivity(intent);
    }



    public String loadJSONFromAsset(String filename) {
        String json = null;
        try {

            InputStream is = getAssets().open(filename);

            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();

            json = new String(buffer, "UTF-8");


        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;

    }

    public void readStudentsFromJson() {
        try {
            Log.e("INSIDE TRY", "to");
            JSONObject obj = new JSONObject(loadJSONFromAsset("students.json"));
            JSONArray arr = obj.getJSONArray("students");
            for (int i=0; i<arr.length(); i++) {
                JSONObject entry = arr.getJSONObject(i);
                String tag = entry.getString("tag");
                JSONObject stud =  entry.getJSONObject("student");
                String name = stud.getString("name");
                String id = stud.getString("id");
                Log.e("STUFF ADDED", name + " " + id + " "+ tag);
                addNewStudent(name,id,tag);
            }
        }
        catch(JSONException e){
            e.printStackTrace();
        }
    }

    public void writeAttendanceToHistory(){
        try {
            Log.e("INSIDE TRY", "to");
            JSONObject obj = new JSONObject(loadJSONFromAsset("history.json"));
            JSONArray history = obj.getJSONArray("history");
            JSONArray attendenceSheet = new JSONArray();

            JSONObject studentEntry = new JSONObject();

            Iterator it = students.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                // tag is key
                // student is value
                String tag = (String) pair.getKey();
                Student st = (Student) pair.getValue();

                JSONObject aStud = new JSONObject();
                aStud.put("name", st.name);
                aStud.put("id", st.ID );
                aStud.put("status", st.status);

                studentEntry.put("tag", tag);
                studentEntry.put("student", aStud);
                it.remove(); // xóa
            }

            history.put(attendenceSheet);

        }
        catch(JSONException e){
            e.printStackTrace();
        }
    }

}