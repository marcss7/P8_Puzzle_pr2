package edu.uoc.resolvers;

/*
    Esta clase define el esquema de la base de datos.
 */
class BBDDEsquema {

    private BBDDEsquema() {}

    static final String NOMBRE_TABLA = "puntuaciones";
    private static final String _ID = "id";
    static final String COLUMNA_FECHA = "fecha";
    static final String COLUMNA_NIVEL = "nivel";
    static final String COLUMNA_PUNTOS = "puntos";

    static final String SQL_CREAR_ENTRADAS =
            "CREATE TABLE " + BBDDEsquema.NOMBRE_TABLA + " (" +
                    BBDDEsquema._ID + " INTEGER PRIMARY KEY," +
                    BBDDEsquema.COLUMNA_FECHA + " TEXT," +
                    BBDDEsquema.COLUMNA_NIVEL + " INT," +
                    BBDDEsquema.COLUMNA_PUNTOS + " FLOAT)";

    static final String SQL_BORRAR_ENTRADAS =
            "DROP TABLE IF EXISTS " + NOMBRE_TABLA;
}
