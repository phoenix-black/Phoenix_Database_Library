package com.myblackphoenix.phoenixdatabase;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.util.Log;


import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Created by Praba on 9/3/2017.
 *
 */
public abstract class DatabaseManager {

    private String _databaseName;
    private int _databaseVersion;

    private SQLiteOpenHelper databaseHelper = null;
    private SQLiteDatabase db = null;
    private ArrayList<DatabaseTable> _databaseTableArrayList;
    private static DatabaseManager _databaseManager = null;
    public abstract void onCreateTable();
    public DatabaseManager(Context context, String databaseName, int databaseVersion, final ArrayList<DatabaseTable> databaseTableArrayList) throws SQLiteException {
        databaseHelper = new SQLiteOpenHelper(context,
                databaseName,
                null,
                databaseVersion) {

            @Override
            public void onCreate(SQLiteDatabase sqLiteDatabase) {
                for(DatabaseTable table: databaseTableArrayList) {
                    sqLiteDatabase.execSQL(table.getQueryCreateTable());
                }
            }

            @Override
            public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
                for(DatabaseTable table: databaseTableArrayList) {
                    sqLiteDatabase.execSQL(table.getQueryDropTable());
                }
                onCreate(sqLiteDatabase);
            }
        };

        _databaseManager = this;
    }

    public DatabaseManager(String databaseName, int databaseVersion){
        this._databaseName = databaseName;
        this._databaseVersion = databaseVersion;
        this._databaseTableArrayList = new ArrayList<>();
        onCreateTable();
        _databaseManager = this;
    }

    public void addTable(@NonNull DatabaseTable databaseTable){
        this._databaseTableArrayList.add(databaseTable);
    }

    public void create(Context context) throws SQLiteException {
        databaseHelper = new SQLiteOpenHelper(context, _databaseName, null, _databaseVersion) {
            @Override
            public void onCreate(SQLiteDatabase sqLiteDatabase) {
                for(DatabaseTable table: _databaseTableArrayList) {
                    sqLiteDatabase.execSQL(table.getQueryCreateTable());
                }
            }

            @Override
            public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
                for(DatabaseTable table: _databaseTableArrayList) {
                    sqLiteDatabase.execSQL(table.getQueryDropTable());
                }
                onCreate(sqLiteDatabase);
            }
        };
    }


    public static synchronized DatabaseManager getInstance() throws NullPointerException {
        if(_databaseManager !=null) {
            return _databaseManager;
        }
        throw new NullPointerException("DatabaseManager is Not Instantiated");
    }

    public DatabaseTable getTableInstance(String tableName){
        for(DatabaseTable databaseTable : _databaseTableArrayList){
            if(databaseTable.getTableName().equals(tableName)){
                return databaseTable;
            }
        }
        return null;
    }

    public long Insert(String TABLE_NAME, ContentValues values) throws DatabaseException {

        if(databaseHelper == null){
            throw new DatabaseException("DatabaseHelper is Not Instantiated");
        }
        db = databaseHelper.getWritableDatabase();
        long r = db.insert(TABLE_NAME, null, values);
        db.close();
        return r;
    }

    public long Update(String TABLE_NAME, String rowKey, String rowID, ContentValues values) throws DatabaseException{

        if(databaseHelper == null){
            throw new DatabaseException("DatabaseHelper is Not Instantiated");
        }

        if(values.size()>0) {
            String whereClause = rowKey + " = ? ";
            String whereArgs[] = {
                    rowID,
            };
            db = databaseHelper.getWritableDatabase();
            long r = db.update(TABLE_NAME, values,whereClause,whereArgs);
            db.close();
            return r;
        }
        return 0;
    }

    public long Delete(String TABLE_NAME, String rowKey, String rowID) throws DatabaseException {

        if(databaseHelper == null){
            throw new DatabaseException("DatabaseHelper is Not Instantiated");
        }
        db = databaseHelper.getWritableDatabase();

        String whereClause = rowKey + " = ? ";
        String[] whereArgs = {
                rowID
        };
        int numRowsDeleted = db.delete(TABLE_NAME,whereClause,whereArgs);
        db.close();
        return numRowsDeleted;
    }

    public void DeleteAll(String TABLE_NAME) throws DatabaseException {
        if(databaseHelper == null){
            throw new DatabaseException("DatabaseHelper is Not Instantiated");
        }
        db = databaseHelper.getWritableDatabase();
        db.execSQL("delete from "+ TABLE_NAME);
        db.close();
    }


    public int Count(String TABLE_NAME) throws DatabaseException {

        if(databaseHelper == null){
            throw new DatabaseException("DatabaseHelper is Not Instantiated");
        }
        db = databaseHelper.getWritableDatabase();

        String countQueryClause =  QUERY.SELECT_FROM(TABLE_NAME,null,null);
        Cursor cursor = db.rawQuery(countQueryClause,null);
        if(cursor!= null) {
            int count = cursor.getCount();
            cursor.close();
            return count;
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    public <T>ArrayList<T> getDataListByID(String TABLE_NAME,
                                                  String whereClause,
                                                  String whereArgs,
                                                  Class<T> dataClass,
                                                  ArrayList<String> dataList) throws Exception {

        if(databaseHelper == null){
            throw new DatabaseException("DatabaseHelper is Not Instantiated");
        }
        db = databaseHelper.getWritableDatabase();

        ArrayList<T> tableDataList = new ArrayList<>();

        Class<?>[] getStringParameterList =  new Class[]{
                String.class,
                String.class
        };
        Method setString = dataClass.getMethod("setString",getStringParameterList);


        String selectQuery =  QUERY.SELECT_FROM(TABLE_NAME,whereClause,whereArgs);
        Cursor cursor = db.rawQuery(selectQuery,null);

//
//                        Class<?> clazz = Class.forName("com.foo.MyClass");
//                        Constructor<?> constructor = clazz.getConstructor(String.class, Integer.class);
//                        Object instance = constructor.newInstance("stringparam", 42);

        if(cursor!=null) {
            if (cursor.moveToFirst()) {
                do {

                    Object dataClassObject = dataClass.newInstance();

                    for(String key: dataList){
                        setString.invoke(dataClassObject,key,
                                cursor.getString(cursor.getColumnIndex(key)));
                    }
                    tableDataList.add((T)dataClassObject);

                } while (cursor.moveToNext());
                cursor.close();
                return tableDataList;
            }
            Log.e("getDataListByID","Data Query Failed, Cursor Move To First Failed");
            cursor.close();
        } else {
            Log.e("getDataListByID","Data Query Failed, Cursor NULL");
        }
        return tableDataList;
    }

    @SuppressWarnings("unchecked")
    public <T>ArrayList<T> getDataListByValue(String TABLE_NAME,
                                                     String whereClause,
                                                     String whereArgs,
                                                     Class<T> dataClass,
                                                     ArrayList<String> dataList) throws Exception {

        if(databaseHelper == null){
            throw new DatabaseException("DatabaseHelper is Not Instantiated");
        }
        db = databaseHelper.getWritableDatabase();

        ArrayList<T> tableDataList = new ArrayList<>();

        Class<?>[] getStringParameterList =  new Class[]{
                String.class,
                String.class
        };

        Method setString = dataClass.getMethod("setString",getStringParameterList);
        /*Field field = dataClass.getField("KEY");
        Log.e("DBO","Field "+field.toString());*/


        String whereArgsArray[] = {
                whereArgs,
        };

        Cursor cursor = db.query(TABLE_NAME,null,whereClause+" =? ",whereArgsArray,null,null,whereClause);

        if(cursor!=null) {
            if (cursor.moveToFirst()) {
                do {

                    Object dataClassObject = dataClass.newInstance();

                    for(String key: dataList){
                        setString.invoke(dataClassObject,key,
                                cursor.getString(cursor.getColumnIndex(key)));
                    }
                    tableDataList.add((T)dataClassObject);

                } while (cursor.moveToNext());
                cursor.close();
                return tableDataList;
            }
            Log.e("getDataListByValue","Data Query Failed, Cursor Move To First Failed");
            cursor.close();
        } else {
            Log.e("getDataListByValue","Data Query Failed, Cursor NULL");
        }
        return tableDataList;
    }

    /*

     */
    public ArrayList<ContentValues> getDataListByID(DatabaseTable table,
                                                  String whereClause,
                                                  String whereArgs,
                                                  ArrayList<String> columnList) throws DatabaseException {

        if(databaseHelper == null){
            throw new DatabaseException("DatabaseHelper is Not Instantiated");
        }
        db = databaseHelper.getWritableDatabase();

        ArrayList<ContentValues> valuesArrayList = new ArrayList<>();

        String selectQuery =  QUERY.SELECT_FROM(table.getTableName(),whereClause,whereArgs);
        Cursor cursor = db.rawQuery(selectQuery,null);

        if(cursor!=null) {
            if (cursor.moveToFirst()) {
                do {
                    ContentValues values = new ContentValues();
                    for(String key: columnList){
                        values.put(key,
                                cursor.getString(cursor.getColumnIndex(key)));
                    }
                    valuesArrayList.add(values);

                } while (cursor.moveToNext());
                cursor.close();
                return valuesArrayList;
            }
            Log.e("DataQueryByID","Data Query Failed, Cursor Move To First Failed");
            cursor.close();
        } else {
            Log.e("DataQueryByID","Data Query Failed, Cursor NULL");
        }
        return valuesArrayList;
    }


    /*
        ******************************************************************************************************************
     */


    /*
     *************************************************************************************************************
     */


    /*
    ****************************************************************************************************
     */

    public ContentValues getDataByID(String tableName,
                                     String whereClause,
                                     String whereArgs,
                                     String[] columnList) throws DatabaseException {

        if(databaseHelper == null){
            throw new DatabaseException("DatabaseHelper is Not Instantiated");
        }
        db = databaseHelper.getWritableDatabase();

        if(whereClause == null){
            whereClause = DatabaseTable.TABLE_KEY_COLUMN_ID;
        }

        String selectQuery =  QUERY.SELECT_FROM(tableName,whereClause,whereArgs);
        Cursor cursor = db.rawQuery(selectQuery,null);

        ContentValues values = new ContentValues();

        if(cursor!=null) {
            if (cursor.moveToFirst()) {
                for(String key: columnList){
                    values.put(key,
                            cursor.getString(cursor.getColumnIndex(key)));
                }
                cursor.close();
                return values;
            }
            Log.e("DataQuery","Data Query Failed, Cursor Move To First Failed");
            cursor.close();
        } else {
            Log.e("DataQuery","Data Query Failed, Cursor NULL");
        }
        return values;
    }

    public ArrayList<ContentValues> getDataListByValue(String TABLE_NAME,
                                                       String whereClause,
                                                       String whereArgs,
                                                       String[] columnList) throws DatabaseException {

        if(databaseHelper == null){
            throw new DatabaseException("DatabaseHelper is Not Instantiated");
        }
        db = databaseHelper.getWritableDatabase();

        ArrayList<ContentValues> valuesArrayList = new ArrayList<>();

        String whereArgsArray[] = {
                whereArgs,
        };

        Cursor cursor = db.query(TABLE_NAME,columnList,whereClause+" =? ",whereArgsArray,null,null,whereClause);

        if(cursor!=null) {
            if (cursor.moveToFirst()) {
                do {
                    ContentValues values = new ContentValues();
                    for(String key: columnList){
                        values.put(key,
                                cursor.getString(cursor.getColumnIndex(key)));
                    }
                    valuesArrayList.add(values);

                } while (cursor.moveToNext());
                cursor.close();
                return valuesArrayList;
            }
            Log.e("DataQueryByValue","Data Query Failed, Cursor Move To First Failed");
            cursor.close();
        } else {
            Log.e("DataQueryByValue","Data Query Failed, Cursor NULL");
        }
        return valuesArrayList;
    }


    /*
    ************************************************************************************
     */


    public ContentValues getDataByID(String tableName,
                                     String whereClause,
                                     String whereArgs,
                                     ArrayList<String> columnList) throws DatabaseException {

        if(databaseHelper == null){
            throw new DatabaseException("DatabaseHelper is Not Instantiated");
        }
        db = databaseHelper.getWritableDatabase();

        if(whereClause == null){
            whereClause = DatabaseTable.TABLE_KEY_COLUMN_ID;
        }

        String selectQuery =  QUERY.SELECT_FROM(tableName,whereClause,whereArgs);
        Cursor cursor = db.rawQuery(selectQuery,null);

        ContentValues values = new ContentValues();

        if(cursor!=null) {
            if (cursor.moveToFirst()) {
                for(String key: columnList){
                    values.put(key,
                            cursor.getString(cursor.getColumnIndex(key)));
                }
                cursor.close();
                return values;
            }
            Log.e("DataQuery","Data Query Failed, Cursor Move To First Failed");
            cursor.close();
        } else {
            Log.e("DataQuery","Data Query Failed, Cursor NULL");
        }
        return values;
    }

    public ArrayList<ContentValues> getDataListByValue(String TABLE_NAME,
                                                            String whereClause,
                                                            String whereArgs,
                                                            ArrayList<String> columnList) throws DatabaseException {

        if(databaseHelper == null){
            throw new DatabaseException("DatabaseHelper is Not Instantiated");
        }
        db = databaseHelper.getWritableDatabase();

        ArrayList<ContentValues> valuesArrayList = new ArrayList<>();

        String whereArgsArray[] = {
                whereArgs,
        };

        String[] columnsArray = columnList.toArray(new String[columnList.size()]);

        Cursor cursor = db.query(TABLE_NAME,columnsArray,whereClause+" =? ",whereArgsArray,null,null,whereClause);

        if(cursor!=null) {
            if (cursor.moveToFirst()) {
                do {
                    ContentValues values = new ContentValues();
                    for(String key: columnList){
                        values.put(key,
                                cursor.getString(cursor.getColumnIndex(key)));
                    }
                    valuesArrayList.add(values);

                } while (cursor.moveToNext());
                cursor.close();
                return valuesArrayList;
            }
            Log.e("DataQueryByValue","Data Query Failed, Cursor Move To First Failed");
            cursor.close();
        } else {
            Log.e("DataQueryByValue","Data Query Failed, Cursor NULL");
        }
        return valuesArrayList;
    }


    /*
    **************************************************************************************************************
     */


    public ContentValues getDataByID(DatabaseTable table,
                                     String whereArgs,
                                     ArrayList<String> columnList) throws DatabaseException {

        if(databaseHelper == null){
            throw new DatabaseException("DatabaseHelper is Not Instantiated");
        }
        db = databaseHelper.getWritableDatabase();

        String whereClause = DatabaseTable.TABLE_KEY_COLUMN_ID;

        if(columnList == null){
            columnList = table.getColumnList();
        }

        String selectQuery =  QUERY.SELECT_FROM(table.getTableName(),whereClause,whereArgs);
        Cursor cursor = db.rawQuery(selectQuery,null);

        ContentValues values = new ContentValues();

        if(cursor!=null) {
            if (cursor.moveToFirst()) {
                for(String key: columnList){
                    values.put(key,
                            cursor.getString(cursor.getColumnIndex(key)));
                }
                cursor.close();
                return values;
            }
            Log.e("DataQuery","Data Query Failed, Cursor Move To First Failed");
            cursor.close();
        } else {
            Log.e("DataQuery","Data Query Failed, Cursor NULL");
        }
        return values;
    }

    public ArrayList<ContentValues> getDataListByValue(DatabaseTable table,
                                                       String whereClause,
                                                       String whereArgs,
                                                       ArrayList<String> columnList) throws DatabaseException {

        if(databaseHelper == null){
            throw new DatabaseException("DatabaseHelper is Not Instantiated");
        }
        db = databaseHelper.getWritableDatabase();

        ArrayList<ContentValues> valuesArrayList = new ArrayList<>();

        if(columnList == null){
            columnList = table.getColumnList();
        }

        String whereArgsArray[] = {
                whereArgs,
        };

        String[] columnsArray = columnList.toArray(new String[columnList.size()]);

        Cursor cursor = db.query(table.getTableName(),columnsArray,whereClause+" =? ",whereArgsArray,null,null,whereClause);

        if(cursor!=null) {
            if (cursor.moveToFirst()) {
                do {
                    ContentValues values = new ContentValues();
                    for(String key: columnList){
                        values.put(key,
                                cursor.getString(cursor.getColumnIndex(key)));
                    }
                    valuesArrayList.add(values);

                } while (cursor.moveToNext());
                cursor.close();
                return valuesArrayList;
            }
            Log.e("DataQueryByValue","Data Query Failed, Cursor Move To First Failed");
            cursor.close();
        } else {
            Log.e("DataQueryByValue","Data Query Failed, Cursor NULL");
        }
        return valuesArrayList;
    }


    /*
    *********************************************************************************************************
     */


    public ContentValues getDataByID(DatabaseTable table,
                                     String whereArgs,
                                     String[] columnList) throws DatabaseException {

        if(databaseHelper == null){
            throw new DatabaseException("DatabaseHelper is Not Instantiated");
        }

        db = databaseHelper.getWritableDatabase();

        String whereClause = DatabaseTable.TABLE_KEY_COLUMN_ID;

        if(columnList == null){
            columnList = table.getColumnList().toArray(new String[table.getColumnList().size()]);
        }

        String selectQuery =  QUERY.SELECT_FROM(table.getTableName(),whereClause,whereArgs);
        Cursor cursor = db.rawQuery(selectQuery,null);

        ContentValues values = new ContentValues();

        if(cursor!=null) {
            if (cursor.moveToFirst()) {
                for(String key: columnList){
                    values.put(key,
                            cursor.getString(cursor.getColumnIndex(key)));
                }
                cursor.close();
                return values;
            }
            Log.e("DataQuery","Data Query Failed, Cursor Move To First Failed");
            cursor.close();
        } else {
            Log.e("DataQuery","Data Query Failed, Cursor NULL");
        }
        return values;
    }


    public ArrayList<ContentValues> getDataListByValue(DatabaseTable table,
                                                       String whereClause,
                                                       String whereArgs,
                                                       String[] columnList) throws DatabaseException {

        if(databaseHelper == null){
            throw new DatabaseException("DatabaseHelper is Not Instantiated");
        }
        db = databaseHelper.getWritableDatabase();

        ArrayList<ContentValues> valuesArrayList = new ArrayList<>();

        if(columnList == null){
            columnList = table.getColumnList().toArray(new String[table.getColumnList().size()]);
        }

        String whereArgsArray[] = {
                whereArgs,
        };


        Cursor cursor = db.query(table.getTableName(),columnList,whereClause+" =? ",whereArgsArray,null,null,whereClause);

        if(cursor!=null) {
            if (cursor.moveToFirst()) {
                do {
                    ContentValues values = new ContentValues();
                    for(String key: columnList){
                        values.put(key,
                                cursor.getString(cursor.getColumnIndex(key)));
                    }
                    valuesArrayList.add(values);

                } while (cursor.moveToNext());
                cursor.close();
                return valuesArrayList;
            }
            Log.e("DataQueryByValue","Data Query Failed, Cursor Move To First Failed");
            cursor.close();
        } else {
            Log.e("DataQueryByValue","Data Query Failed, Cursor NULL");
        }
        return valuesArrayList;
    }


    public ContentValues getDataByID(DatabaseTable table,
                                     String whereArgs) throws DatabaseException {

        if(databaseHelper == null){
            throw new DatabaseException("DatabaseHelper is Not Instantiated");
        }

        db = databaseHelper.getWritableDatabase();

        String whereClause = DatabaseTable.TABLE_KEY_COLUMN_ID;

        String selectQuery =  QUERY.SELECT_FROM(table.getTableName(),whereClause,whereArgs);
        Cursor cursor = db.rawQuery(selectQuery,null);

        ContentValues values = new ContentValues();

        if(cursor!=null) {
            if (cursor.moveToFirst()) {
                for(String key: table.getColumnList()){
                    values.put(key,
                            cursor.getString(cursor.getColumnIndex(key)));
                }
                cursor.close();
                return values;
            }
            Log.e("DataQuery","Data Query Failed, Cursor Move To First Failed");
            cursor.close();
        } else {
            Log.e("DataQuery","Data Query Failed, Cursor NULL");
        }
        return values;
    }


    public ArrayList<ContentValues> getDataListByValue(DatabaseTable table,
                                                       String whereClause,
                                                       String whereArgs) throws DatabaseException {

        if(databaseHelper == null){
            throw new DatabaseException("DatabaseHelper is Not Instantiated");
        }
        db = databaseHelper.getWritableDatabase();

        ArrayList<ContentValues> valuesArrayList = new ArrayList<>();


        String[]  columnList = table.getColumnList().toArray(new String[table.getColumnList().size()]);


        String whereArgsArray[] = {
                whereArgs,
        };


        Cursor cursor = db.query(table.getTableName(),columnList,whereClause+" =? ",whereArgsArray,null,null,whereClause);

        if(cursor!=null) {
            if (cursor.moveToFirst()) {
                do {
                    ContentValues values = new ContentValues();
                    for(String key: table.getColumnList()){
                        values.put(key,
                                cursor.getString(cursor.getColumnIndex(key)));
                    }
                    valuesArrayList.add(values);

                } while (cursor.moveToNext());
                cursor.close();
                return valuesArrayList;
            }
            Log.e("DataQueryByValue","Data Query Failed, Cursor Move To First Failed");
            cursor.close();
        } else {
            Log.e("DataQueryByValue","Data Query Failed, Cursor NULL");
        }
        return valuesArrayList;
    }

    /*
    *****************************************************************************************
    * Supporting classes, Methods and fields
     */

    public static class QUERY {

        public static String SELECT_FROM(String tableName, String whereClause, String whereArgs) throws NullPointerException {
            if(tableName != null) {
                if (whereArgs == null || whereClause == null) {
                    // whereArgs or whereArgs both is null
                    return "select * from " + tableName;
                } else {
                    // whereArgs and whereArgs both are not null
                    return "select * from " + tableName + " where " + whereClause + " = " + whereArgs;
                }
            } else {
                // Table name is null
                throw new NullPointerException("Unable to perform DB_QUERY Action - SELECT_FROM. Error: Table name is null");
            }
        }

        public static String TABLE_ROW_COUNT(String tableName) throws NullPointerException {
            if(tableName!=null) {
                return "SELECT COUNT(*) FROM " + tableName;
            } else {
                throw new NullPointerException("Unable to perform DB_QUERY Action - TABLE_ROW_COUNT. Error: Table name is null");
            }
        }
    }

}
