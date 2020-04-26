package edu.uoc.resolvers;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.CalendarContract;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/*
    Esta clase representa la pantalla de juego.
 */
public class ActividadPrincipal extends AppCompatActivity implements Runnable {
    private PuzzleLayout pl;
    private int numCortes = 2;
    private int imagen = R.mipmap.img_02;
    private long tInicio, tFin, tDelta;
    private String fechaActual;
    private String patronFecha = "dd/MM/yyyy";
    private SimpleDateFormat sdf;
    private double segTranscurridos;
    private static final int SECOND_ACTIVITY_REQUEST_CODE = 0;
    private static final int NIVELES = 5;
    HomeWatcher mHomeWatcher;
    private static final int READ_REQUEST_CODE = 42;
    public static final String Broadcast_PLAY_NEW_AUDIO = "edu.uoc.resolvers";
    private boolean isChecked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_principal);

        // Asociamos el servicio de música
        doBindService();
        Intent music = new Intent();
        music.setClass(this, ServicioMusica.class);
        startService(music);

        // Iniciamos el HomeWatcher
        mHomeWatcher = new HomeWatcher(this);
        mHomeWatcher.setOnHomePressedListener(new HomeWatcher.OnHomePressedListener() {
            @Override
            public void onHomePressed() {
                if (mServ != null) {
                    mServ.pauseMusic();
                }
            }

            @Override
            public void onHomeLongPressed() {
                if (mServ != null) {
                    mServ.pauseMusic();
                }
            }
        });
        mHomeWatcher.startWatch();

        pl = findViewById(R.id.tablero_juego);
        pl.establecerImagen(imagen, numCortes);

        // Empezamos a contar el tiempo
        tInicio = System.currentTimeMillis();
        final BBDDHelper dbHelper = new BBDDHelper(this);

        // Cuando se completa el puzzle
        pl.setOnCompleteCallback(new PuzzleLayout.OnCompleteCallback() {
            @Override
            public void onComplete() {
                // Paramos el tiempo
                tFin = System.currentTimeMillis();
                sdf = new SimpleDateFormat(patronFecha);
                fechaActual = sdf.format(new Date(tFin));
                tDelta = tFin - tInicio;
                segTranscurridos = tDelta / 1000.0;

                agregarEventoCalendario(numCortes - 1, segTranscurridos);

                // Obtenemos la BBDD
                SQLiteDatabase db = dbHelper.getWritableDatabase();

                // Insertamos la puntuación en la BBDD
                ContentValues valores = new ContentValues();
                valores.put(BBDDEsquema.COLUMNA_FECHA, String.valueOf(fechaActual));
                valores.put(BBDDEsquema.COLUMNA_NIVEL, numCortes - 1);
                valores.put(BBDDEsquema.COLUMNA_PUNTOS, segTranscurridos);

                long idNuevaFila = db.insert(BBDDEsquema.NOMBRE_TABLA, null, valores);

                // Mostramos mensaje al completar puzzle
                Toast.makeText(ActividadPrincipal.this, "¡Bravo! Tu tiempo " + String.format("%.2f", segTranscurridos).replace(".", ",") + "s", Toast.LENGTH_SHORT).show();

                // Esperamos 3 segundos para cargar el siguiente puzzle
                pl.postDelayed(ActividadPrincipal.this, 3000);
            }
        });
    }

    private void agregarEventoCalendario(int nivel, double tiempo) {
        if (recurperarPuntuacionesCalendario(nivel, tiempo)) {
            long fecha_record = System.currentTimeMillis();
            ContentResolver cr = getContentResolver();
            ContentValues values = new ContentValues();
            values.put(CalendarContract.Events.DTSTART, fecha_record);
            values.put(CalendarContract.Events.DTEND, fecha_record);
            values.put(CalendarContract.Events.TITLE, "TR - ¡Nuevo récord N" + nivel + "!");
            values.put(CalendarContract.Events.DESCRIPTION, String.format("%.2f", tiempo).replace(".", ","));
            values.put(CalendarContract.Events.CALENDAR_ID, 3);
            values.put(CalendarContract.Events.EVENT_TIMEZONE, "Confinado");
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);
        }
    }

    private boolean recurperarPuntuacionesCalendario(int nivel, double tiempo) {
        ContentResolver contentResolver = getContentResolver();
            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();

            Calendar beginTime = Calendar.getInstance();
            beginTime.set(2000, Calendar.JANUARY, 1, 0, 0);
            long startMills = beginTime.getTimeInMillis();
            long endMills = System.currentTimeMillis();

            ContentUris.appendId(builder, startMills);
            ContentUris.appendId(builder, endMills);
            String[] args = new String[]{"3"};

            Cursor eventCursor = contentResolver.query(builder.build(), new String[]{CalendarContract.Instances.TITLE,
                            CalendarContract.Instances.BEGIN, CalendarContract.Instances.END, CalendarContract.Instances.DESCRIPTION},
                    CalendarContract.Instances.CALENDAR_ID + " = ?", args, null);

            boolean isRecord = false;
            boolean hayRegistros = false;

            while (eventCursor.moveToNext()) {
                final String title = eventCursor.getString(0);
                final Date begin = new Date(eventCursor.getLong(1));
                final Date end = new Date(eventCursor.getLong(2));
                final String description = eventCursor.getString(3);

                //Log.i("Nivel", Integer.toString(title.length()));

                if (title.length() == 22 && title.substring(title.length() - 2, title.length() - 1).equals(Integer.toString(nivel))) {
                    hayRegistros = true;
                    if (tiempo < Double.parseDouble(description.replace(",", "."))) {
                        isRecord = true;
                    } else {
                        isRecord = false;
                    }
                }
                //Log.i("Cursor", "Title: " + title + "\tDescription: " + description + "\tBegin: " + begin + "\tEnd: " + end);
            }

            //Log.i("Record", Boolean.toString(isRecord));
            if (hayRegistros) {
                return isRecord;
            } else {
                return !isRecord;
            }

    }

    // Asociamos el servicio de música
    private boolean mIsBound = false;
    private ServicioMusica mServ;
    private ServiceConnection Scon = new ServiceConnection(){

        public void onServiceConnected(ComponentName name, IBinder
                binder) {
            mServ = ((ServicioMusica.ServiceBinder)binder).getService();
        }

        public void onServiceDisconnected(ComponentName name) {
            mServ = null;
        }
    };

    void doBindService(){
        bindService(new Intent(this,ServicioMusica.class),
                Scon, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService()
    {
        if(mIsBound)
        {
            unbindService(Scon);
            mIsBound = false;
        }
    }

    // Este método crea el menú selección de la barra de acción
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_principal, menu);
        return true;
    }

    // Este método dispara la acción correspondiente al elegir cada opción del menú.
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ayuda:
                // Se abre la WebView con la ayuda
                Intent ayuda = new Intent(this, ActividadAyuda.class);
                startActivity(ayuda);
                return true;
            case R.id.selector_musica:
                // Se abre el selector de música
                performFileSearch();
                return true;
            case R.id.checkable_menu:
                isChecked = !item.isChecked();
                item.setChecked(isChecked);
                if (isChecked) {
                    AudioManager amanager=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
                    amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                } else {
                    AudioManager amanager=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
                    amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem checkable = menu.findItem(R.id.checkable_menu);
        checkable.setChecked(isChecked);
        return true;
    }

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    public void performFileSearch() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("*/*");

        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                ServicioMusica.audioUri = uri;
                Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
                sendBroadcast(broadcastIntent);
            }
        }
    }

    @Override
    public void run() {
        numCortes++;
        imagen++;
        // Si llegamos al último puzzle muestra el dialogo del fin del juego
        // Si no carga el siguiente puzzle
        if(numCortes > NIVELES + 1){
            showDialog();
        }else {
            pl.establecerImagen(imagen, numCortes);
        }
    }

    // Este método muestra el diálogo de finalización del juego.
    private void showDialog() {
        new AlertDialog.Builder(ActividadPrincipal.this)
                .setTitle(R.string.exito)
                .setMessage(R.string.reiniciar)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                numCortes = 2;
                                imagen = R.mipmap.img_02;
                                pl.establecerImagen(imagen, numCortes);
                                tInicio = System.currentTimeMillis();
                            }
                        }).setNegativeButton(R.string.salir,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(ActividadPrincipal.this, ActividadInicio.class);
                        startActivityForResult(i, SECOND_ACTIVITY_REQUEST_CODE);
                        finish();
                    }
                }).show();
    }

    // Este método reanuda la música
    @Override
    protected void onResume() {
        super.onResume();

        if (mServ != null) {
            mServ.resumeMusic();
        }
    }

    // Este método pone la música en pausa
    @Override
    protected void onPause() {
        super.onPause();

        //Detect idle screen
        PowerManager pm = (PowerManager)
                getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = false;
        if (pm != null) {
            isScreenOn = pm.isScreenOn();
        }

        if (!isScreenOn) {
            if (mServ != null) {
                mServ.pauseMusic();
            }
        }
    }

    // Este método desasocia el servicio de música cuando no lo necesitamos
    @Override
    protected void onDestroy() {
        super.onDestroy();

        //UNBIND music service
        doUnbindService();
        Intent music = new Intent();
        music.setClass(this,ServicioMusica.class);
        stopService(music);
    }
}
