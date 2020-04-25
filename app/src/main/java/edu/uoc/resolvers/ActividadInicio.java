package edu.uoc.resolvers;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/*
    Esta clase representa la pantalla de bienvenida.
 */
public class ActividadInicio extends AppCompatActivity {

    HomeWatcher mHomeWatcher;

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

        Button botonInicio = findViewById(R.id.botonInicio);
        botonInicio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirActividadPrincipal();
            }
        });

        TextView puntuaciones = findViewById(R.id.puntuaciones);

        BBDDHelper dbHelper = new BBDDHelper(this);
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
        cursor.close();
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
