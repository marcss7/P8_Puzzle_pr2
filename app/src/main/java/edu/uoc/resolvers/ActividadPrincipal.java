package edu.uoc.resolvers;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.text.SimpleDateFormat;
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
