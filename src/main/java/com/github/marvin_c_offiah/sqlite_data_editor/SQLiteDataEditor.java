package com.github.marvin_c_offiah.sqlite_data_editor;

import javax.swing.JOptionPane;

import com.github.marvin_c_offiah.sqlite_data_editor.controller.SQLiteDataEditorController;

public class SQLiteDataEditor {

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
	try {
	    String filePath = null;
	    if (args != null) {
		if (args.length > 1) {
		    throw new Exception("Invalid number of input arguments: " + args.length
			    + ". Only 1 allowed for database file path.");
		}
		if (args.length == 1)
		    filePath = args[0];
	    }
	    if (filePath == null)
		new SQLiteDataEditorController();
	    else
		new SQLiteDataEditorController(filePath);
	} catch (Exception e) {
	    e.printStackTrace();
	    JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
	}
    }

}
