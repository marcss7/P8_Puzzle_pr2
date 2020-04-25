package edu.uoc.resolvers;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
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
    private ServiceConnection Scon =new ServiceConnection(){

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
            default:
                return super.onOptionsItemSelected(item);
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

    // Este métodod desasocia el servicio de música cuando no lo necesitamos
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
