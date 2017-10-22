package com.myblackphoenix.phoenixdatabase;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Praba on 9/3/2017.
 */
public abstract class DatabaseTable {

    private String _tableName;
    private ArrayList<COLUMN> _columnList = null;
    private static String DELIMITER_COMMA = ",";
    private static String DELIMITER_SPACE = " ";
    public static final String TABLE_KEY_COLUMN_ID = "id";

    public DatabaseTable(String tableName){
        this._tableName = tableName;
        this._columnList = new ArrayList<>();
        this._columnList.add(this.getPrimaryColumn());
        onCreate();
    }

    public abstract void onCreate();

    /*
    *****************************************************************************
    Primary Methods
    *******************************************************************************
     */



    public String getQueryCreateTable(){
        String tableColumns = "";
        for (COLUMN column : this._columnList){
            if(column.getName().equals(TABLE_KEY_COLUMN_ID)){
                tableColumns += (DELIMITER_SPACE + column.getName() + column.getRule());
            } else {
                tableColumns += (DELIMITER_COMMA + DELIMITER_SPACE + column.getName() + column.getRule());
            }
        }

        String tableQuery = "create table "
                + this._tableName + "("
                + tableColumns
                + " );";

        Log.e("DBTable",""+tableQuery);
        return tableQuery;
    }

    public String getQueryDropTable() {
        return "DROP TABLE IF EXISTS "+_tableName;
    }

    public String getTableName(){
        return this._tableName;
    }

    public String getTableKeyColumnId() {
        return TABLE_KEY_COLUMN_ID;
    }

    public boolean addColumn(String columnName, TABLE_RULE... columnRules){
        String columnRuleAsString = "";

        if(!(columnRules.length>0)){
            columnRuleAsString = this.getDefaultRule();
        } else {
            for(TABLE_RULE rule: columnRules){
                columnRuleAsString += DELIMITER_SPACE + rule.toString();
            }
        }

        COLUMN column = new COLUMN(columnName,columnRuleAsString);
        this._columnList.add(column);

        return true;
    }

    public ArrayList<String> getColumnList(){

        ArrayList<String> columnNameList = new ArrayList<>();
        for (COLUMN column : this._columnList){
            columnNameList.add(column.getName());
        }

        return columnNameList;
    }

    /***************************************************************/
    // Supporting Classes and Method

    private class COLUMN {
        private String NAME = null;
        private String RULE = null;

        public COLUMN(String columnName, String columnRule){
            this.NAME = columnName;
            this.RULE = columnRule;
        }

        public String getName() {
            return this.NAME;
        }
        public String getRule() {
            return this.RULE;
        }
    }

    public enum TABLE_RULE {
        DEFAULT("text"),
        DATA_INTEGER("integer"),
        DATA_TEXT("text"),
        DATA_REAL("real"),
        DATA_BLOB("blob"),
        NOT_NULL("not null");

        String keyword;

        TABLE_RULE(String key){
            this.keyword = key;
        }

        @Override
        public String toString(){
            return this.keyword;
        }
    }

    private enum TABLE_RULE_ADVANCED {
        AUTO_INCREMENT("autoincrement"),
        PRIMARY_KEY("primary key");

        String keyword;

        TABLE_RULE_ADVANCED(String key){
            this.keyword = key;
        }

        @Override
        public String toString(){
            return this.keyword;
        }
    }

    private String getDefaultRule(){
        return TABLE_RULE.DATA_TEXT.toString();
    }

    private COLUMN getPrimaryColumn(){
        String primaryKeyRule = DELIMITER_SPACE + TABLE_RULE.DATA_INTEGER.toString()
                + DELIMITER_SPACE + TABLE_RULE_ADVANCED.PRIMARY_KEY.toString();

        return new COLUMN(TABLE_KEY_COLUMN_ID,primaryKeyRule);
    }
}
