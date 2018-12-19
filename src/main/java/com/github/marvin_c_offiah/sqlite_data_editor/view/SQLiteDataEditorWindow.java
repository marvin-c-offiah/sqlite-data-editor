package com.github.marvin_c_offiah.sqlite_data_editor.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import com.github.marvin_c_offiah.data_utils.SQLiteInMemoryDatabase;
import com.github.marvin_c_offiah.sqlite_data_editor.controller.SQLiteDataEditorController;

public class SQLiteDataEditorWindow implements Observer {

    public static final Color DEFAULT_TABLE_ROW_SELECTION_COLOR = new Color(0.0f, 0.0f, 1.0f, 0.2f);

    public static final Color TABLE_ROW_INSERT_COLOR = new Color(0.0f, 0.6f, 0.0f, 0.2f);

    public static final Color TABLE_ROW_UPDATE_COLOR = new Color(1.0f, 1.0f, 0.0f, 0.2f);

    public static final Color TABLE_ROW_ERROR_COLOR = new Color(1.0f, 0.0f, 0.0f, 0.2f);

    protected class SQLiteDatabaseFileChooser extends JFileChooser {

	public SQLiteDatabaseFileChooser(boolean isOpen) {

	    super();
	    this.setDialogTitle(isOpen ? "Open" : "Save as");
	    setFileSelectionMode(JFileChooser.FILES_ONLY);
	    addChoosableFileFilter(new FileFilter() {

		@Override
		public boolean accept(File f) {
		    if (f.isDirectory()) {
			return true;
		    }
		    String extension = null;
		    String s = f.getName();
		    int i = s.lastIndexOf('.');

		    if (i > 0 && i < s.length() - 1) {
			extension = s.substring(i + 1).toLowerCase();
		    }

		    if (extension != null) {
			if (extension.equals("db") || extension.equals("sdb") || extension.equals("sqlite")
				|| extension.equals("db3") || extension.equals("s3db") || extension.equals("sqlite3")
				|| extension.equals("sl3") || extension.equals("db2") || extension.equals("s2db")
				|| extension.equals("sqlite2") || extension.equals("sl2")) {
			    return true;
			} else {
			    return false;
			}
		    }
		    return false;
		}

		@Override
		public String getDescription() {
		    return "All SQLite databases (db, sdb, sqlite, db3, s3db, sqlite3, sl3, db2, s2db, sqlite2, sl2)";
		}

	    });
	    setAcceptAllFileFilterUsed(false);
	}

    }

    protected class SelectAllCellEditor extends DefaultCellEditor {

	public SelectAllCellEditor() {
	    super(new JTextField());
	    JTextField textField = (JTextField) getComponent();
	    textField.addFocusListener(new FocusAdapter() {

		public void focusGained(final FocusEvent e) {
		    textField.selectAll();
		}
	    });
	}
    }

    protected SQLiteDataEditorController controller;

    protected JFrame frmSqliteDataEditor;

    protected JTabbedPane tpnTables;

    protected JTable[] tblTables;

    protected JMenuItem mntmSave;

    protected JMenuBar menuBar;

    private JButton btnAddRow;

    private JButton btnDeleteRow;

    private JPanel pnlButtons;

    protected boolean isInserting = false;

    protected boolean isUpdating = false;

    protected boolean isHandlingValueChange = false;

    protected int lastSelectedTableRow = -1;

    public SQLiteDataEditorWindow(SQLiteDataEditorController controller, SQLiteInMemoryDatabase model)
	    throws Exception {
	if (controller == null)
	    throw new Exception("The controller must be provided to the data editor.");
	initialize();
	this.controller = controller;
	if (model != null)
	    setModel(model);
	frmSqliteDataEditor.setVisible(true);
    }

    public void setModel(SQLiteInMemoryDatabase model) {
	model.addObserver(this);
	update(model, null);
	updateMenuBar(false);
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {

	frmSqliteDataEditor = new JFrame();
	frmSqliteDataEditor.setTitle("SQLite Data Editor");
	frmSqliteDataEditor.setBounds(100, 100, 874, 702);
	frmSqliteDataEditor.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	menuBar = new JMenuBar();
	frmSqliteDataEditor.setJMenuBar(menuBar);

	JMenu mnNewMenu = new JMenu("File");
	menuBar.add(mnNewMenu);

	JMenuItem mntmOpen = new JMenuItem("Open...");
	mntmOpen.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent arg0) {
		JFileChooser fc = new SQLiteDatabaseFileChooser(true);
		int returnVal = fc.showOpenDialog(frmSqliteDataEditor);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
		    File file = fc.getSelectedFile();
		    try {
			controller.openFile(file.getAbsolutePath());
		    } catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(frmSqliteDataEditor, e.getMessage(), "Error",
				JOptionPane.ERROR_MESSAGE);
		    }
		}
	    }
	});
	mnNewMenu.add(mntmOpen);

	mntmSave = new JMenuItem("Save");
	mntmSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
	mnNewMenu.add(mntmSave);

	JMenuItem mntmSaveAs = new JMenuItem("Save as...");
	mntmSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK));
	mnNewMenu.add(mntmSaveAs);

	JMenuItem mntmExit = new JMenuItem("Exit");
	mntmExit.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent arg0) {
		System.exit(0);
	    }
	});
	mntmExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
	mnNewMenu.add(mntmExit);
	frmSqliteDataEditor.getContentPane().setLayout(new BorderLayout(5, 5));

	tpnTables = new JTabbedPane(JTabbedPane.BOTTOM);
	tpnTables.addChangeListener(new ChangeListener() {

	    public void stateChanged(ChangeEvent e) {
		lastSelectedTableRow = -1;
		switchToDefaultMode(tblTables[tpnTables.getSelectedIndex()]);
	    }
	});
	frmSqliteDataEditor.getContentPane().add(tpnTables, BorderLayout.CENTER);

	pnlButtons = new JPanel();
	frmSqliteDataEditor.getContentPane().add(pnlButtons, BorderLayout.PAGE_END);
	pnlButtons.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

	btnDeleteRow = new JButton("Delete row");
	btnDeleteRow.setEnabled(false);
	btnDeleteRow.setSize(new Dimension(85, 23));
	btnDeleteRow.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent arg0) {
		int selectedIdx = tpnTables.getSelectedIndex();
		try {
		    if (selectedIdx > -1) {
			JTable table = tblTables[selectedIdx];
			int[] selectedRows = table.getSelectedRows();
			if (selectedRows.length > 0) {
			    for (int row : selectedRows) {
				TreeMap<String, Object> primKeyVal = new TreeMap<String, Object>();
				primKeyVal.put("_rowid_",
					(String) table.getValueAt(row, table.getModel().getColumnCount() - 1));
				controller.deleteFromTable(table.getName(), primKeyVal);
			    }
			}
		    }
		} catch (Exception e) {
		    e.printStackTrace();
		    JOptionPane.showMessageDialog(frmSqliteDataEditor, e.getMessage(), "Error",
			    JOptionPane.ERROR_MESSAGE);
		}
	    }
	});
	pnlButtons.add(btnDeleteRow);
	btnDeleteRow.setFont(new Font("Tahoma", Font.BOLD, 11));

	btnAddRow = new JButton("Add row");
	btnAddRow.setEnabled(false);
	btnAddRow.setPreferredSize(new Dimension(85, 23));
	btnAddRow.setSize(new Dimension(85, 23));
	btnAddRow.setMaximumSize(new Dimension(85, 23));
	btnAddRow.setMinimumSize(new Dimension(85, 23));
	btnAddRow.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent arg0) {
		isHandlingValueChange = true;
		int selectedIdx = tpnTables.getSelectedIndex();
		try {
		    if (selectedIdx > -1) {
			isInserting = true;
			((DefaultTableModel) tblTables[selectedIdx].getModel())
				.addRow(new String[tblTables[selectedIdx].getColumnCount() + 1]);
			int row = tblTables[selectedIdx].getRowCount() - 1;
			tblTables[selectedIdx].setRowSelectionInterval(row, row);
			lastSelectedTableRow = row;
			tblTables[selectedIdx].setSelectionBackground(TABLE_ROW_INSERT_COLOR);
			tblTables[selectedIdx]
				.scrollRectToVisible(new Rectangle(tblTables[selectedIdx].getCellRect(row, 0, true)));
		    }
		} catch (Exception e) {
		    tblTables[selectedIdx].setSelectionBackground(TABLE_ROW_ERROR_COLOR);
		    e.printStackTrace();
		    JOptionPane.showMessageDialog(frmSqliteDataEditor, e.getMessage(), "Error",
			    JOptionPane.ERROR_MESSAGE);
		}
		isHandlingValueChange = false;
	    }
	});
	pnlButtons.add(btnAddRow);
	btnAddRow.setFont(new Font("Tahoma", Font.BOLD, 11));

    }

    @Override
    public void update(Observable database, Object object) {

	try {

	    TreeMap<String, TreeMap<String, Object[]>> tables = ((SQLiteInMemoryDatabase) database).getTables();
	    String[] tblNames = tables.keySet().toArray(new String[0]);
	    int selectedTab = tpnTables.getSelectedIndex();
	    tpnTables.removeAll();

	    tblTables = new JTable[tblNames.length];

	    for (int i = 0; i < tblNames.length; i++) {
		String tblName = tblNames[i];
		TreeMap<String, Object[]> table = tables.get(tblName);
		int colCount = table.size();
		Set<String> ksColNames = table.keySet();
		String[] colNames = ksColNames.toArray(new String[ksColNames.size()]);
		int rowCount = table.get(colNames[0]).length;
		TreeMap<String, TreeMap<String, String>> importedKeys = ((SQLiteInMemoryDatabase) database)
			.getImportedKeys(tblName);
		String[] refTblNames = importedKeys.keySet().toArray(new String[importedKeys.size()]);

		Object[][] data = new Object[rowCount][colCount];
		for (int j = 0; j < colCount; j++)
		    for (int k = 0; k < rowCount; k++)
			data[k][j] = table.get(colNames[j])[k];
		JTable tbl = new JTable(new DefaultTableModel(data, colNames));

		tbl.getColumnModel()
			.removeColumn(tbl.getColumnModel().getColumn(tbl.getColumnModel().getColumnIndex("_rowid_")));
		tbl.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tbl.setName(tblName);
		tblTables[i] = tbl;

		TableColumnModel tcm = tbl.getColumnModel();
		for (String refTblName : refTblNames) {
		    ArrayList<String> pks = new ArrayList<String>(importedKeys.get(refTblName).keySet());
		    for (int j = 0; j < tbl.getColumnCount(); j++) {
			String colName = tbl.getColumnName(j);
			if (pks.contains(colName)) {
			    tcm.getColumn(tcm.getColumnIndex(importedKeys.get(refTblName).get(colName))).setCellEditor(
				    new DefaultCellEditor(new JComboBox(((SQLiteInMemoryDatabase) database)
					    .selectColumns(refTblName, new String[] { colName }, true).get(colName))));
			} else {
			    tcm.getColumn(tcm.getColumnIndex(colName)).setCellEditor(new SelectAllCellEditor());
			}
		    }
		}
		tbl.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

		    @Override
		    public void valueChanged(ListSelectionEvent evt) {
			if (isHandlingValueChange || evt.getValueIsAdjusting()) {
			    return;
			}
			try {
			    isHandlingValueChange = true;
			    int selectedRow = tbl.getSelectedRow();
			    if (selectedRow != lastSelectedTableRow) {
				if (isInserting) {
				    tbl.setSelectionBackground(DEFAULT_TABLE_ROW_SELECTION_COLOR);
				    TreeMap<String, Object> vals = new TreeMap<String, Object>();
				    for (int i = 0; i < tbl.getColumnCount(); i++) {
					vals.put(tbl.getColumnName(i), tbl.getValueAt(lastSelectedTableRow, i));
				    }
				    controller.insertIntoTable(tbl.getName(), vals);
				    switchToDefaultMode(tbl);
				} else if (isUpdating) {
				    tbl.setSelectionBackground(DEFAULT_TABLE_ROW_SELECTION_COLOR);
				    TreeMap<String, Object> line = new TreeMap<String, Object>();
				    List<String> primKeyElems = Arrays.asList(controller.getPrimaryKey(tbl.getName()));
				    for (int i = 0; i < tbl.getColumnCount(); i++) {
					String colName = tbl.getColumnName(i);
					if (!primKeyElems.contains(colName))
					    line.put(colName, tbl.getValueAt(lastSelectedTableRow, i));
				    }
				    int rowId = (int) tbl.getModel().getValueAt(lastSelectedTableRow,
					    tbl.getModel().getColumnCount() - 1);
				    TreeMap<String, Object> pk = new TreeMap<String, Object>();
				    pk.put("_rowid_", rowId);
				    controller.updateInTable(tbl.getName(), pk, line);
				    switchToDefaultMode(tbl);
				}
				lastSelectedTableRow = selectedRow;
			    }
			} catch (Exception e) {
			    switchToErrorMode(tbl);
			    tbl.setRowSelectionInterval(lastSelectedTableRow, lastSelectedTableRow);
			    tbl.scrollRectToVisible(new Rectangle(tbl.getCellRect(lastSelectedTableRow, 0, true)));
			    e.printStackTrace();
			    JOptionPane.showMessageDialog(frmSqliteDataEditor, e.getMessage(), "Error",
				    JOptionPane.ERROR_MESSAGE);
			}
			isHandlingValueChange = false;
		    }

		});
		tbl.getModel().addTableModelListener(new TableModelListener() {

		    @Override
		    public void tableChanged(TableModelEvent e) {
			if (!isInserting) {
			    switchToUpdatingMode(tbl);
			}
		    }

		});

		tpnTables.addTab(tblName, new JScrollPane(tbl));

	    }

	    tpnTables.setSelectedIndex(selectedTab > -1 ? selectedTab : 0);
	    switchToDefaultMode(tblTables[selectedTab > -1 ? selectedTab : 0]);

	} catch (Exception e) {
	    e.printStackTrace();
	    JOptionPane.showMessageDialog(frmSqliteDataEditor, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
	}

    }

    public void updateMenuBar(boolean saveEnabled) {
	mntmSave.setEnabled(saveEnabled);
    }

    protected void switchToDefaultMode(JTable table) {
	table.setSelectionBackground(DEFAULT_TABLE_ROW_SELECTION_COLOR);
	btnAddRow.setEnabled(true);
	btnDeleteRow.setEnabled(true);
	tpnTables.setEnabled(true);
	isInserting = false;
	isUpdating = false;
    }

    protected void switchToUpdatingMode(JTable table) {
	table.setSelectionBackground(TABLE_ROW_UPDATE_COLOR);
	btnAddRow.setEnabled(false);
	btnDeleteRow.setEnabled(false);
	tpnTables.setEnabled(false);
	isUpdating = true;
    }

    protected void switchToInsertingMode(JTable table) {
	table.setSelectionBackground(TABLE_ROW_INSERT_COLOR);
	btnAddRow.setEnabled(false);
	btnDeleteRow.setEnabled(false);
	tpnTables.setEnabled(false);
	isInserting = true;
    }

    protected void switchToErrorMode(JTable table) {
	table.setSelectionBackground(TABLE_ROW_ERROR_COLOR);
	btnAddRow.setEnabled(false);
	btnDeleteRow.setEnabled(false);
	tpnTables.setEnabled(false);
    }

}
