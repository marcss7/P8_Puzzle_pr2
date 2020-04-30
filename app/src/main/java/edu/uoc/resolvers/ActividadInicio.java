package edu.uoc.resolvers;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/*
    Esta clase representa la pantalla de bienvenida.
 */
public class ActividadInicio extends AppCompatActivity {

    HomeWatcher mHomeWatcher;
    private String patronFecha = "dd/MM/yyyy";
    private SimpleDateFormat sdf;
    private String fechaActual;
    private int CALENDAR_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_inicio);
        getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.colorPrimary));

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

        //Solicita los permisos de escritura y lectura en el Calendario.
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR}, CALENDAR_PERMISSION_CODE);

        Button botonInicio = findViewById(R.id.botonInicio);
        botonInicio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirActividadPrincipal();
            }
        });

        TextView puntuaciones = findViewById(R.id.puntuaciones);

/*        BBDDHelper dbHelper = new BBDDHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Construimos la consulta a la BBDD
        String[] projection = {
                BBDDEsquema.COLUMNA_FECHA,
                BBDDEsquema.COLUMNA_NIVEL,
                "MIN(" + BBDDEsquema.COLUMNA_PUNTOS + ")"
        };

        Cursor cursor = db.query(
                BBDDEsquema.NOMBRE_TABLA,   // Tabla a consultar
                projection,                 // Las columnas que queremos que devuelva
                null,
                null,
                BBDDEsquema.COLUMNA_NIVEL,  // Agrupamos por nivel
                null,
                null
        );

        // Pintamos las puntuaciones
        while(cursor.moveToNext()) {
            puntuaciones.append(cursor.getString(1) + "     " + cursor.getString(0) + " " + String.format("%.2f", cursor.getDouble(2)).replace(".", ",") +  "\n");
        }
        cursor.close();*/

        //Comprueba si se han concedido los permisos de lectura y escritura en el Calendario.
        if (ContextCompat.checkSelfPermission(ActividadInicio.this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(ActividadInicio.this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            obtenerPuntuaciones(puntuaciones); //Con los permisos concedidos, se muestran las puntuaciones en el TextView.
        } else {
            //Sin la concesión del permiso, se muestra el siguiente mensaje en el TextView.
            puntuaciones.append("Debe dar permisos de lectura y escritura al Calendario para poder visualizar y registrar nuevas puntuaciones.");
        }

    }
    private void obtenerPuntuaciones(TextView puntuaciones) {
        puntuaciones.append("");
        for (int i = 0; i < 5; i++) {
            ContentResolver contentResolver = getContentResolver();
            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();

            Calendar beginTime = Calendar.getInstance();
            beginTime.set(2000, Calendar.JANUARY, 1, 0, 0);
            long startMills = beginTime.getTimeInMillis();
            long endMills = System.currentTimeMillis();

            String titulo = "TR - ¡Nuevo récord N" + Integer.toString(i + 1) + "!";
            ContentUris.appendId(builder, startMills);
            ContentUris.appendId(builder, endMills);
            String[] args = new String[]{"3", titulo};

            Cursor eventCursor = contentResolver.query(builder.build(), new String[]{CalendarContract.Instances.TITLE,
                            CalendarContract.Instances.BEGIN, CalendarContract.Instances.END, CalendarContract.Instances.DESCRIPTION},
                    CalendarContract.Instances.CALENDAR_ID + " = ? AND " + CalendarContract.Instances.TITLE + " = ?", args, null);
            if (eventCursor.getCount() > 0) {
                double min = Double.MAX_VALUE;
                Date minDate = new Date();

                while (eventCursor.moveToNext()) {
                    final String title = eventCursor.getString(0);
                    final Date begin = new Date(eventCursor.getLong(1));
                    final Date end = new Date(eventCursor.getLong(2));
                    final String description = eventCursor.getString(3);

                    if (Double.parseDouble(description.replace(",", ".")) < min) {
                        min = Double.parseDouble(description.replace(",", "."));
                        minDate = begin;
                    }
                    Log.i("Minimo", "Descripcion: " + description);
                    //Log.i("Nivel", Integer.toString(title.length()));
                }
                sdf = new SimpleDateFormat(patronFecha);
                fechaActual = sdf.format(minDate);

                puntuaciones.append(Integer.toString(i + 1) + "     " + fechaActual + " " + String.format("%.2f", min).replace(".", ",") + "\n");
            }
        }
    }

    // Asociamos el servicio de música
    private boolean mIsBound = false;
    private ServicioMusica mServ;
    private ServiceConnection Scon = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder
                binder) {
            mServ = ((ServicioMusica.ServiceBinder) binder).getService();
        }

        public void onServiceDisconnected(ComponentName name) {
            mServ = null;
        }
    };

    void doBindService() {
        bindService(new Intent(this, ServicioMusica.class),
                Scon, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            unbindService(Scon);
            mIsBound = false;
        }
    }

    // Este método hace que al hacer clic en el botón de inicio
    // se nos abra la pantalla principal del juego.
    private void abrirActividadPrincipal() {
        Intent intent = new Intent(this, ActividadPrincipal.class);
        startActivity(intent);
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
        music.setClass(this, ServicioMusica.class);
        stopService(music);
    }

}
