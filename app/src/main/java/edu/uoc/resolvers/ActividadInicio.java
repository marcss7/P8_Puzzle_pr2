package edu.uoc.resolvers;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/*
    Esta clase representa la pantalla de bienvenida.
 */
public class ActividadInicio extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_inicio);
        getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.colorPrimary));

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

    // Este método hace que al hacer clic en el botón de inicio
    // se nos abra la pantalla principal del juego.
    private void abrirActividadPrincipal() {
        Intent intent = new Intent(this, ActividadPrincipal.class);
        startActivity(intent);
    }

}
