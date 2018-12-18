package com.github.marvin_c_offiah.sqlite_data_editor.controller;

import java.util.TreeMap;

import com.github.marvin_c_offiah.data_utils.SQLiteInMemoryDatabase;
import com.github.marvin_c_offiah.data_utils.SQLiteInMemoryDatabaseIO;
import com.github.marvin_c_offiah.sqlite_data_editor.view.SQLiteDataEditorWindow;

public class SQLiteDataEditorController {

    protected SQLiteInMemoryDatabaseIO io;

    protected SQLiteInMemoryDatabase model;

    protected SQLiteDataEditorWindow view;

    public SQLiteDataEditorController() throws Exception {
	io = null;
	model = null;
	view = new SQLiteDataEditorWindow(this, model);
    }

    public SQLiteDataEditorController(String databaseFilePath) throws Exception {
	io = new SQLiteInMemoryDatabaseIO(databaseFilePath);
	model = io.read();
	view = new SQLiteDataEditorWindow(this, model);
    }

    public String[] getPrimaryKey(String tableName) throws Exception {
	return model.getPrimaryKey(tableName);
    }

    public void insertIntoTable(String name, TreeMap<String, Object> values) throws Exception {
	model.insertIntoTable(name, values);
	view.updateMenuBar(true);
    }

    public void updateInTable(String name, TreeMap<String, Object> primaryKey, TreeMap<String, Object> line)
	    throws Exception {
	model.updateInTable(name, primaryKey, line);
	view.updateMenuBar(true);
    }

    public void deleteFromTable(String name, TreeMap<String, Object> primaryKey) throws Exception {
	model.deleteFromTable(name, primaryKey);
	view.updateMenuBar(true);
    }

    public void saveChanges() throws Exception {
	io.write(model);
	view.updateMenuBar(false);
    }

    public void openFile(String filePath) throws Exception {
	io = new SQLiteInMemoryDatabaseIO(filePath);
	model = io.read();
	view.setModel(model);
    }

    public void saveAs(String filePath) throws Exception {
	new SQLiteInMemoryDatabaseIO(filePath).write(model);
    }

}
