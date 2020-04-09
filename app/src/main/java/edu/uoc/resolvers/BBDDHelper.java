package edu.uoc.resolvers;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.Context;

/*
    Esta clase permite administrar la BBDD.
 */
public class BBDDHelper extends SQLiteOpenHelper {
    // Si cambiamos el esquema de la BBDD es necesario incrementar la versión.
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "Puntuaciones.db";

    BBDDHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(BBDDEsquema.SQL_BORRAR_ENTRADAS);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(BBDDEsquema.SQL_CREAR_ENTRADAS);
    }

}
