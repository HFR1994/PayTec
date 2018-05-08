package itesm.aabo.tech.paytec;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {


    NavigationView navigationView = null;
    Toolbar toolbar = null;

    private static final String TAG = "nfcinventory_simple";

    // NFC-related variables
    NfcAdapter nfcAdapter;
    PendingIntent mNfcPendingIntent;
    IntentFilter[] mReadWriteTagFilters;
    private Boolean mWriteMode=null;
    String[][]mTechList;
    AlertDialog mTagDialog;
    private boolean mPurchaseMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //InitializeFragment
        MainFragment fragment = new MainFragment();
        android.support.v4.app.FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment, "MY_FRAGMENT");
        fragmentTransaction.commit();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(nfcAdapter == null){
            Toast.makeText(this,
                    "Tu equipo no soporta NFC!",
                    Toast.LENGTH_LONG).show();
            finish();
        }

        checkNfcEnabled();

        // Handle foreground NFC scanning in this activity by creating a
        // PendingIntent with FLAG_ACTIVITY_SINGLE_TOP flag so each new scan
        // is not added to the Back Stack
        mNfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // Create intent filter to handle MIFARE NFC tags detected from inside our
        // application when in "read mode":
        IntentFilter mifareDetected = new IntentFilter();
        mifareDetected.addAction(NfcAdapter.ACTION_TECH_DISCOVERED);

        try {
            mifareDetected.addDataType("application/itesm.aabo.tech.mifarecontrol");
        } catch (IntentFilter.MalformedMimeTypeException e)
        {
            throw new RuntimeException("No se pudo añadir un tipo MIME.", e);
        }

        // create IntentFilter arrays:
        //mWriteTagFilters = new IntentFilter[] { tagDetected };
        mReadWriteTagFilters = new IntentFilter[] { mifareDetected };


        // Setup a tech list for all NfcF tags
        mTechList = new String[][] { new String[] { MifareClassic.class.getName() } };
    }

    /*
     * This is called for activities that set launchMode to "singleTop" or
     * "singleTask" in their manifest package, or if a client used the
     * FLAG_ACTIVITY_SINGLE_TOP flag when calling startActivity(Intent).
     */
    @Override
    public void onNewIntent(Intent intent)
    {
        Log.d(TAG,"onNew: "+String.valueOf(intent));
        Log.d(TAG,"mPurchase: "+String.valueOf(mPurchaseMode));
        Log.d(TAG,"mWrite: "+String.valueOf(mWriteMode));

        if(mTagDialog!=null && mTagDialog.isShowing()){
            mTagDialog.dismiss();
        }

        if (mPurchaseMode) {
            try {
                resolvePurchaseIntent(intent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            if(mWriteMode != null){
                if (!mWriteMode) {
                    // Currently in tag READING mode
                    try {
                        resolveReadIntent(intent);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    // Currently in tag WRITING mode
                    try {
                        resolveWriteIntent(intent);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /* Called when the activity will start interacting with the user. */
    @Override
    public void onResume()
    {
        super.onResume();
        checkNfcEnabled();
        Log.d(TAG,"onResume: "+String.valueOf(getIntent()));
        nfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mReadWriteTagFilters, mTechList);
    }


    /* Called when the system is about to start resuming a previous activity. */
    @Override
    public void onPause()
    {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);

    }


    void resolveReadIntent(Intent intent) throws IOException {
        String action = intent.getAction();
        BalanceFragment fragment_obj = null;
        Log.i(TAG, "Read:" + action);
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            MifareClassic mfc = MifareClassic.get(tagFromIntent);

            int ttype = mfc.getType();
            Log.d(TAG, "MifareClassic tag type: " + ttype);

            int tsize = mfc.getSize();
            Log.d(TAG, "tag size: " + tsize);

            int s_len = mfc.getSectorCount();
            Log.d(TAG, "tag sector count: " + s_len);

            int b_len = mfc.getBlockCount();
            Log.d(TAG, "tag block count: " + b_len);

            String id = ByteArrayToHexString(mfc.getTag().getId());
            Log.d(TAG, "Id: " + id);


            try {
                mfc.connect();
                if (mfc.isConnected()) {
                    boolean authA;
                    boolean authB;
                    fragment_obj = (BalanceFragment)  getSupportFragmentManager().findFragmentByTag("MY_FRAGMENT");
                    String hexkey = BalanceFragment.getWriteKey();

                    int sector = mfc.blockToSector(4);
                    byte[] datakeyA;
                    byte[] datakeyB;

                    if(hexkey != null){

                        datakeyA = hexStringToByteArray(hexkey);
                        datakeyB = hexStringToByteArray(hexkey);

                        authA = mfc.authenticateSectorWithKeyA(sector, datakeyA);
                        authB = mfc.authenticateSectorWithKeyB(sector, datakeyB);

                        if (authA && authB) {
                            int bloque = 4;
                            byte[] dataread = mfc.readBlock(bloque);

                            String blockread = new String(dataread,Charset.forName("UTF-8")).trim();

                            if (fragment_obj != null && fragment_obj.isVisible()) {
                                String val = "$"+blockread+" pts";
                                fragment_obj.currentBalace.setText(val);
                            }else{
                                Toast.makeText(this,
                                        "No pude conseguir la vista",
                                        Toast.LENGTH_LONG).show();
                            }

                            Toast.makeText(this,
                                    "Lectura de bloque EXITOSA.",
                                    Toast.LENGTH_LONG).show();

                        } else { // Authentication failed - Handle it
                            //Editable BlockField = mDataBloque.getText();
                            //BlockField.clear();
                            Toast.makeText(this,
                                    "Lectura de bloque FALLIDA dado autentificación fallida.",
                                    Toast.LENGTH_LONG).show();
                        }

                        mfc.close();
                        mTagDialog.cancel();
                    }
                }else{
                    Toast.makeText(this,
                            "Cerre Conexión",
                            Toast.LENGTH_LONG).show();
                }

            }catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this,
                        "Lectura de bloque EXITOSA.",
                        Toast.LENGTH_LONG).show();
                Integer value = GlobalVariables.hashValue(id);
                fragment_obj = (BalanceFragment)  getSupportFragmentManager().findFragmentByTag("MY_FRAGMENT");
                if (fragment_obj != null && fragment_obj.isVisible()) {
                    String val = "$"+value+" pts";
                    fragment_obj.currentBalace.setText(val);
                }else{
                    Toast.makeText(this,
                            "No pude conseguir la vista",
                            Toast.LENGTH_LONG).show();
                }
                if(mfc.isConnected()){
                    mfc.close();
                }
                if (mTagDialog.isShowing()){
                    mTagDialog.cancel();
                }
            }
        }

        mWriteMode=null;

    }

    private void enableTagPurchaseMode()
    {
        mPurchaseMode=true;
        nfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
                mReadWriteTagFilters, mTechList);
    }

    private void enableTagWriteMode()
    {
        mWriteMode = true;
        nfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
                mReadWriteTagFilters, mTechList);
    }

    private void enableTagReadMode()
    {
        mWriteMode = false;
        nfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
                mReadWriteTagFilters, mTechList);
    }

    void resolveWriteIntent(Intent intent) throws IOException {
        String action = intent.getAction();
        BalanceFragment fragment_obj = null;
        Log.i(TAG, "Write:" + action);
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            MifareClassic mfc = MifareClassic.get(tagFromIntent);

            int ttype = mfc.getType();
            Log.d(TAG, "MifareClassic tag type: " + ttype);

            int tsize = mfc.getSize();
            Log.d(TAG, "tag size: " + tsize);

            int s_len = mfc.getSectorCount();
            Log.d(TAG, "tag sector count: " + s_len);

            int b_len = mfc.getBlockCount();
            Log.d(TAG, "tag block count: " + b_len);

            String id = ByteArrayToHexString(mfc.getTag().getId());
            Log.d(TAG, "Id: " + id);


            try {
                mfc.connect();
                if (mfc.isConnected()) {
                    boolean authA;
                    boolean authB;
                    fragment_obj = (BalanceFragment)  getSupportFragmentManager().findFragmentByTag("MY_FRAGMENT");
                    String hexkey = BalanceFragment.getWriteKey();

                    int sector = mfc.blockToSector(4);
                    byte[] datakeyA;
                    byte[] datakeyB;

                    if(hexkey != null){

                        datakeyA = hexStringToByteArray(hexkey);
                        datakeyB = hexStringToByteArray(hexkey);

                        authA = mfc.authenticateSectorWithKeyA(sector, datakeyA);
                        authB = mfc.authenticateSectorWithKeyB(sector, datakeyB);

                        if (authA && authB) {
                            int bloque = 4;

                            byte[] dataread = mfc.readBlock(bloque);

                            Log.d(TAG,"Datos: "+Arrays.toString(dataread));

                            String blockread = new String(dataread, Charset.forName("UTF-8")).trim();
                            String writeblock = String.valueOf(fragment_obj.writeText.getText());

                            try{
                                int a=Integer.parseInt(blockread);
                                int b=Integer.parseInt(writeblock);
                                int c = a+b;

                                byte[] arr = String.valueOf(c).getBytes(Charset.forName("UTF-8"));
                                byte [] nuevo = new byte[16];

                                System.arraycopy(arr,0,nuevo,nuevo.length-arr.length, arr.length);

                                mfc.writeBlock(bloque,nuevo);

                                Toast.makeText(this,
                                        "Escritura a bloque EXITOSA.",
                                        Toast.LENGTH_LONG).show();

                                GlobalVariables.addTotal(id,c);
                                String val = "$"+GlobalVariables.hashValue(id)+" pts";

                                fragment_obj.currentBalace.setText(val);
                            }catch (Exception e){
                                Toast.makeText(this,
                                        "Hubo un pequeño problema.",
                                        Toast.LENGTH_LONG).show();
                            }

                        } else { // Authentication failed - Handle it
                            //Editable BlockField = mDataBloque.getText();
                            //BlockField.clear();
                            Toast.makeText(this,
                                    "Lectura de bloque FALLIDA dado autentificación fallida.",
                                    Toast.LENGTH_LONG).show();
                        }

                        mfc.close();
                        mTagDialog.cancel();
                    }
                }else{
                    Toast.makeText(this,
                            "Cerre Conexión",
                            Toast.LENGTH_LONG).show();
                }

            }catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this,
                        "Lectura de bloque EXITOSA.",
                        Toast.LENGTH_LONG).show();
                fragment_obj = (BalanceFragment)  getSupportFragmentManager().findFragmentByTag("MY_FRAGMENT");
                if (fragment_obj != null && fragment_obj.isVisible()) {
                    String nuevo = String.valueOf(fragment_obj.writeText.getText());
                    try {
                        GlobalVariables.addValue(id, Integer.parseInt(nuevo));
                    }catch (NumberFormatException f ){
                        f.printStackTrace();
                    }
                    String val = "$"+GlobalVariables.hashValue(id)+" pts";
                    fragment_obj.currentBalace.setText(val);
                }else{
                    Toast.makeText(this,
                            "No pude conseguir la vista",
                            Toast.LENGTH_LONG).show();
                }
                if(mfc.isConnected()){
                    mfc.close();
                }
                if (mTagDialog.isShowing()){
                    mTagDialog.cancel();
                }
            }
        }

        mWriteMode=null;

    }

    void resolvePurchaseIntent(Intent intent) throws IOException {
        String action = intent.getAction();
        Log.i(TAG, "Purchase:" + action);
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            MifareClassic mfc = MifareClassic.get(tagFromIntent);

            int ttype = mfc.getType();
            Log.d(TAG, "MifareClassic tag type: " + ttype);

            int tsize = mfc.getSize();
            Log.d(TAG, "tag size: " + tsize);

            int s_len = mfc.getSectorCount();
            Log.d(TAG, "tag sector count: " + s_len);

            int b_len = mfc.getBlockCount();
            Log.d(TAG, "tag block count: " + b_len);

            String id = ByteArrayToHexString(mfc.getTag().getId());
            Log.d(TAG, "Id: " + id);

            try {
                mfc.connect();
                if (mfc.isConnected()) {
                    boolean authA;
                    boolean authB;
                    String hexkey = BalanceFragment.getWriteKey();

                    int sector = mfc.blockToSector(4);
                    byte[] datakeyA;
                    byte[] datakeyB;

                    if(hexkey != null){

                        datakeyA = hexStringToByteArray(hexkey);
                        datakeyB = hexStringToByteArray(hexkey);

                        authA = mfc.authenticateSectorWithKeyA(sector, datakeyA);
                        authB = mfc.authenticateSectorWithKeyB(sector, datakeyB);

                        if (authA && authB) {
                            int bloque = 4;

                            Log.d(TAG, "Almacenamiento: " + GlobalVariables.hashValue(id));
                            Log.d(TAG, "Carrito: " + GlobalVariables.getCart());

                            if(GlobalVariables.subtractValue(id,Integer.parseInt(String.valueOf(GlobalVariables.getCart())))){

                                byte[] arr = String.valueOf(GlobalVariables.hashValue(id)).getBytes(Charset.forName("UTF-8"));
                                byte [] nuevo = new byte[16];

                                System.arraycopy(arr,0,nuevo,nuevo.length-arr.length, arr.length);

                                mfc.writeBlock(bloque,nuevo);

                                Toast.makeText(this,
                                        "Escritura a bloque EXITOSA.",
                                        Toast.LENGTH_LONG).show();
                            }else{
                                Toast.makeText(this,
                                        "No tienes sufficiente saldo.",
                                        Toast.LENGTH_LONG).show();
                            }

                        } else { // Authentication failed - Handle it
                            Toast.makeText(this,
                                    "Lectura de bloque FALLIDA dado autentificación fallida.",
                                    Toast.LENGTH_LONG).show();
                        }

                        mfc.close();
                    }else{
                        Toast.makeText(this,
                                "Porfavor primero asocia una cartera.",
                                Toast.LENGTH_LONG).show();
                    }
                }else{
                    Toast.makeText(this,
                            "Cerre Conexión",
                            Toast.LENGTH_LONG).show();
                }

            }catch (Exception e) {
                e.printStackTrace();
                if(GlobalVariables.subtractValue(id,Integer.parseInt(String.valueOf(GlobalVariables.getCart())))){
                    Toast.makeText(this,
                            "Escritura a bloque EXITOSA.",
                            Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(this,
                            "No tienes sufficiente saldo.",
                            Toast.LENGTH_LONG).show();
                }
                if(mfc.isConnected()){
                    mfc.close();
                }
            }

            mPurchaseMode=false;
        }

    }

    public static String getHexString(byte[] b, int length)
    {
        String result = "";
        Locale loc = Locale.getDefault();

        for (int i = 0; i < length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
            result += " ";
        }
        return result.toUpperCase(loc);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    /*
     * **** TAG READ METHODS ****
     */

    public void readtoRead(){
        mWriteMode=false;

        AlertDialog.Builder builder = new AlertDialog.Builder(
                MainActivity.this)
                .setTitle(getString(R.string.ready_to_read))
                .setMessage(getString(R.string.ready_to_instructions))
                .setCancelable(false)
                .setNegativeButton("Cancelar",
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog,
                                                int id)
                            {
                                dialog.cancel();
                            }
                        })
                .setOnCancelListener(new DialogInterface.OnCancelListener()
                {
                    @Override
                    public void onCancel(DialogInterface dialog)
                    {
                        mTagDialog.dismiss();
                    }
                });
        mTagDialog = builder.create();
        mTagDialog.show();
    }

    public void readtoPurchase(){

        mPurchaseMode=true;

        AlertDialog.Builder builder = new AlertDialog.Builder(
                MainActivity.this)
                .setTitle(getString(R.string.ready_to_write))
                .setMessage(getString(R.string.ready_to_instructions))
                .setCancelable(false)
                .setNegativeButton("Cancelar",
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog,
                                                int id)
                            {
                                dialog.cancel();
                            }
                        })
                .setOnCancelListener(new DialogInterface.OnCancelListener()
                {
                    @Override
                    public void onCancel(DialogInterface dialog)
                    {
                        mTagDialog.dismiss();
                    }
                });
        mTagDialog = builder.create();
        mTagDialog.show();
    }

    public void readtoWrite(){

        mWriteMode=true;
        AlertDialog.Builder builder = new AlertDialog.Builder(
                MainActivity.this)
                .setTitle(getString(R.string.ready_to_write))
                .setMessage(getString(R.string.ready_to_instructions))
                .setCancelable(false)
                .setNegativeButton("Cancelar",
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog,
                                                int id)
                            {
                                dialog.cancel();
                            }
                        })
                .setOnCancelListener(new DialogInterface.OnCancelListener()
                {
                    @Override
                    public void onCancel(DialogInterface dialog)
                    {
                        mTagDialog.dismiss();
                    }
                });
        mTagDialog = builder.create();
        mTagDialog.show();
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

    /*
     * **** TAG WRITE METHODS ****

    /*
     * **** HELPER METHODS ****
     */

    private void checkNfcEnabled()
    {
        Boolean nfcEnabled = nfcAdapter.isEnabled();
        if (!nfcEnabled)
        {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(getString(R.string.warning_nfc_is_off))
                    .setMessage(getString(R.string.turn_on_nfc))
                    .setCancelable(false)
                    .setPositiveButton("Actualizar Settings",
                            new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog,
                                                    int id)
                                {
                                    startActivity(new Intent(
                                            android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                                }
                            }).create().show();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_recarga) {
            BalanceFragment fragment = new BalanceFragment();
            android.support.v4.app.FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment, "MY_FRAGMENT");
            fragmentTransaction.commit();
        } else if(id == R.id.nav_home){
            MainFragment fragment = new MainFragment();
            android.support.v4.app.FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment, "MY_FRAGMENT");
            fragmentTransaction.commit();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}
