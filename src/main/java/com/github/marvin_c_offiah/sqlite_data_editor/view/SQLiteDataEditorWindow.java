package com.github.marvin_c_offiah.sqlite_data_editor.view;

import static java.awt.event.KeyEvent.VK_CANCEL;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.awt.event.KeyEvent.VK_KP_DOWN;
import static java.awt.event.KeyEvent.VK_KP_LEFT;
import static java.awt.event.KeyEvent.VK_KP_RIGHT;
import static java.awt.event.KeyEvent.VK_KP_UP;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.awt.event.KeyEvent.VK_TAB;
import static java.awt.event.KeyEvent.VK_UP;

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
import java.awt.event.KeyListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

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
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import com.github.marvin_c_offiah.data_utils.SQLiteInMemoryDatabase;
import com.github.marvin_c_offiah.sqlite_data_editor.controller.SQLiteDataEditorController;

public class SQLiteDataEditorWindow implements Observer {

    public static final int DEFAULT_STATE = 0;

    public static final int UPDATING_STATE = 1;

    public static final int INSERTING_STATE = 2;

    public static final int ERROR_UPDATING_STATE = 3;

    public static final int ERROR_INSERTING_STATE = 4;

    public static final int TABLE_NAVIGATION_KEY_CLASS = 0;

    public static final int TABLE_SAVE_EDITS_KEY_CLASS = 1;

    public static final int TABLE_CANCEL_EDITS_KEY_CLASS = 2;

    public static final int TABLE_EDIT_KEY_CLASS = 3;

    public static final int TABLE_DO_NOTHING_KEY_CLASS = 3;

    public static final int INSERT_TABLE_ROW_COMMAND = 0;

    public static final int DELETE_TABLE_ROW_COMMAND = 1;

    public static final Color DEFAULT_TABLE_ROW_SELECTION_COLOR = new Color(0.0f, 0.0f, 1.0f, 0.5f);

    public static final Color TABLE_ROW_INSERT_COLOR = new Color(0.0f, 0.6f, 0.0f, 0.5f);

    public static final Color TABLE_ROW_UPDATE_COLOR = new Color(1.0f, 1.0f, 0.0f, 0.5f);

    public static final Color TABLE_ROW_ERROR_COLOR = new Color(1.0f, 0.0f, 0.0f, 0.5f);

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

    protected class SQLiteTable extends JTable {

	protected int currentState = DEFAULT_STATE;

	protected int[] currentSelection = { -1, -1 };

	protected int currentKey = -1;

	public SQLiteTable(Object[][] data, String[] colNames) {
	    super(new DefaultTableModel(data, colNames));
	}

	@Override
	public void changeSelection(final int row, final int column, boolean toggle, boolean extend) {
	    super.changeSelection(row, column, toggle, extend);
	    this.editCellAt(row, column);
	    ((DefaultCellEditor) (this.getCellEditor(row, column))).getComponent().requestFocus();
	    requestStateChange(NO_COMMAND, this, row, column, currentKey);
	}

	protected int getkeyClass(int key) {
	    switch (key) {
	    case VK_ENTER:
		return currentState == DEFAULT_STATE ? TABLE_NAVIGATION_KEY_CLASS : TABLE_SAVE_EDITS_KEY_CLASS;
	    case VK_CANCEL:
		return currentState == DEFAULT_STATE ? TABLE_EDIT_KEY_CLASS : TABLE_CANCEL_EDITS_KEY_CLASS;
	    case VK_LEFT:
	    case VK_RIGHT:
	    case VK_UP:
	    case VK_DOWN:
	    case VK_KP_LEFT:
	    case VK_KP_RIGHT:
	    case VK_KP_UP:
	    case VK_KP_DOWN:
	    case VK_TAB:
		return TABLE_NAVIGATION_KEY_CLASS;
	    default:
		return TABLE_EDIT_KEY_CLASS;
	    }
	}

	protected boolean trySaveUpdate() {
	    try {
		TreeMap<String, Object> line = new TreeMap<String, Object>();
		List<String> primKeyElems = Arrays.asList(controller.getPrimaryKey(getName()));
		for (int i = 0; i < getColumnCount(); i++) {
		    String colName = getColumnName(i);
		    if (!primKeyElems.contains(colName))
			line.put(colName, getValueAt(row, i));
		}
		int rowId = (int) getModel().getValueAt(row, getModel().getColumnCount() - 1);
		TreeMap<String, Object> pk = new TreeMap<String, Object>();
		pk.put("_rowid_", rowId);
		controller.updateInTable(getName(), pk, line);
	    } catch (Exception e) {

		e.printStackTrace();
		JOptionPane.showMessageDialog(frmSqliteDataEditor, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		return false;
	    }
	    return true;
	}

	protected boolean trySaveInsert() {
	    try {
		if (isInserting) {
		    TreeMap<String, Object> vals = new TreeMap<String, Object>();
		    for (int i = 0; i < getColumnCount(); i++) {
			vals.put(getColumnName(i), getValueAt(row, i));
		    }
		    controller.insertIntoTable(getName(), vals);
		} else if (isUpdating) {
		    TreeMap<String, Object> line = new TreeMap<String, Object>();
		    List<String> primKeyElems = Arrays.asList(controller.getPrimaryKey(getName()));
		    for (int i = 0; i < getColumnCount(); i++) {
			String colName = getColumnName(i);
			if (!primKeyElems.contains(colName))
			    line.put(colName, getValueAt(row, i));
		    }
		    int rowId = (int) getModel().getValueAt(row, getModel().getColumnCount() - 1);
		    TreeMap<String, Object> pk = new TreeMap<String, Object>();
		    pk.put("_rowid_", rowId);
		    controller.updateInTable(getName(), pk, line);
		}
	    } catch (Exception e) {
		switchToErrorMode(table);
		setRowSelectionInterval(row, row);
		scrollRectToVisible(new Rectangle(getCellRect(row, 0, true)));
		e.printStackTrace();
		JOptionPane.showMessageDialog(frmSqliteDataEditor, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		return false;
	    }
	    return true;
	}

    }

    protected class SelectAllCellEditor extends DefaultCellEditor {

	protected JTable table;

	protected boolean stoppedEditingWithoutSaving = false;

	protected int[] stopEditingWithoutSavingKeys = { KeyEvent.VK_TAB, KeyEvent.VK_ESCAPE, KeyEvent.VK_LEFT,
		KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN };

	protected boolean cellEditingStarted = false;

	protected boolean cellEditingFinished = false;

	public SelectAllCellEditor(JTable table) {
	    super(new JTextField());
	    this.table = table;
	    JTextField textField = (JTextField) getComponent();
	    textField.setBorder(new EmptyBorder(0, 0, 0, 0));
	    textField.setOpaque(false);
	    textField.addFocusListener(new FocusAdapter() {

		public void focusGained(final FocusEvent e) {
		    Color[] colors = getCurrentSelectionColors(table);
		    textField.setSelectionColor(colors[0]);
		    textField.setSelectedTextColor(colors[1]);
		    textField.selectAll();
		}
	    });
	    textField.addKeyListener(new KeyListener() {

		@Override
		public void keyTyped(KeyEvent e) {
		    handleKeyEvent(e);
		}

		@Override
		public void keyPressed(KeyEvent e) {
		    handleKeyEvent(e);
		}

		@Override
		public void keyReleased(KeyEvent e) {
		    handleKeyEvent(e);
		}

		protected void handleKeyEvent(KeyEvent e) {
		    if (cellEditingFinished) {
			return;
		    }
		    if (!cellEditingStarted) {
			cellEditingStarted = true;
			return;
		    }
		    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			stoppedEditingWithoutSaving = false;
			return;
		    }
		    if (!(isUpdating || isInserting)) {
			if (Arrays.binarySearch(stopEditingWithoutSavingKeys, e.getKeyCode()) < 0) {
			    stoppedEditingWithoutSaving = false;
			    cellEditingStarted = true;
			    cellEditingFinished = false;
			    switchToUpdatingMode(table);
			} else {
			    stoppedEditingWithoutSaving = true;
			}
		    }

		}

	    });
	    this.setClickCountToStart(0);
	}

	@Override
	public boolean stopCellEditing() {
	    if (!stoppedEditingWithoutSaving) {
		if (finishRowEditing(table, table.getSelectedRow())) {
		    cellEditingStarted = false;
		    cellEditingFinished = true;
		    getComponent().requestFocus();
		    return true;
		} else {
		    return false;
		}
	    } else {
		return true;
	    }
	}

    }

    protected SQLiteDataEditorController controller;

    protected JFrame frmSqliteDataEditor;

    protected JTabbedPane tpnTables;

    protected SQLiteTable[] tblTables;

    protected JMenuItem mntmSave;

    protected JMenuBar menuBar;

    private JButton btnAddRow;

    private JButton btnDeleteRow;

    private JPanel pnlButtons;

    protected boolean isInserting = false;

    protected boolean isUpdating = false;

    protected boolean isErrorHandling = false;

    protected boolean isHandlingValueChange = false;

    protected boolean isTabbing = false;

    protected int lastSelectedTableRow = -1;

    public SQLiteDataEditorWindow(SQLiteDataEditorController controller, SQLiteInMemoryDatabase model)
	    throws Exception {
	if (controller == null)
	    throw new Exception("The controller must be provided to the data editor.");
	UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
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
		    isErrorHandling = true;
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

	    TreeMap<String, ArrayList<TreeMap<String, Object>>> tables = ((SQLiteInMemoryDatabase) database)
		    .getTables();
	    String[] tblNames = tables.keySet().toArray(new String[0]);
	    int selectedTab = tpnTables.getSelectedIndex();
	    tpnTables.removeAll();

	    tblTables = new SQLiteTable[tblNames.length];

	    for (int i = 0; i < tblNames.length; i++) {

		String tblName = tblNames[i];
		ArrayList<TreeMap<String, Object>> table = tables.get(tblName);
		int rowCount = table.size() - 1;
		int colCount = table.get(0).size();
		Set<String> ksColNames = table.get(0).keySet();
		String[] colNames = ksColNames.toArray(new String[ksColNames.size()]);
		TreeMap<String, TreeMap<String, String>> importedKeys = ((SQLiteInMemoryDatabase) database)
			.getImportedKeys(tblName);
		String[] refTblNames = importedKeys.keySet().toArray(new String[importedKeys.size()]);

		Object[][] data = new Object[rowCount][colCount];
		for (int j = 0; j < rowCount; j++)
		    for (int k = 0; k < colCount; k++)
			data[j][k] = table.get(j).get(colNames[k]);
		SQLiteTable tbl = new SQLiteTable(data, colNames);
		tbl.setRowHeight(25);

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
			    ArrayList<TreeMap<String, Object>> colResult = ((SQLiteInMemoryDatabase) database)
				    .selectColumns(refTblName, null, new String[] { colName }, true);
			    Vector<Object> col = new Vector<Object>();
			    for (TreeMap<String, Object> val : colResult) {
				col.add(val);
			    }
			    tcm.getColumn(tcm.getColumnIndex(importedKeys.get(refTblName).get(colName)))
				    .setCellEditor(new DefaultCellEditor(new JComboBox(col)));
			} else {
			    tcm.getColumn(tcm.getColumnIndex(colName)).setCellEditor(new SelectAllCellEditor(tbl));
			}
		    }
		}

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

    protected void requestStateChange(int command, SQLiteTable table, int newRow, int newColumn, int key) {

	if (command == INSERT_TABLE_ROW_COMMAND) {
	    switchToInsertingState(table);
	    return;
	}
	if (command == DELETE_TABLE_ROW_COMMAND) {
	    deleteRow(table);
	    return;
	}

	boolean rowChanged = newRow != table.currentSelection[0];
	int keyClass = table.getkeyClass(key);
	boolean saveSuccessful = false;

	switch (table.currentState) {
	case DEFAULT_STATE:
	    if (command == DELETE_TABLE_ROW_COMMAND) {
		// TODO: delete row + select previous row
		return;
	    }
	    if (keyClass != TABLE_NAVIGATION_KEY_CLASS && keyClass != TABLE_DO_NOTHING_KEY_CLASS) {
		switchToUpdatingMode(table);
		return;
	    }
	case UPDATING_STATE:
	    if (!(rowChanged || keyClass == TABLE_SAVE_EDITS_KEY_CLASS)) {
		return;
	    }
	    if (keyClass == TABLE_CANCEL_EDITS_KEY_CLASS
		    || ((rowChanged || keyClass == TABLE_SAVE_EDITS_KEY_CLASS) && tryApplyUpdate())) {
		switchToDefaultMode(table);
		return;
	    }
	    if ((rowChanged || keyClass == TABLE_SAVE_EDITS_KEY_CLASS) && !trySaveChanges()) {
		switchToErrorMode(table);
		return;
	    }
	case ERROR_UPDATING_STATE:

	}

    }

    protected void switchToDefaultState(SQLiteTable table) {
	table.setSelectionBackground(DEFAULT_TABLE_ROW_SELECTION_COLOR);
	table.setSelectionForeground(Color.WHITE);
	btnAddRow.setEnabled(true);
	btnDeleteRow.setEnabled(true);
	tpnTables.setEnabled(true);
	menuBar.setEnabled(true);
	table.currentState = DEFAULT_STATE;
    }

    protected void switchToUpdatingState(SQLiteTable table) {
	table.setSelectionBackground(TABLE_ROW_UPDATE_COLOR);
	table.setSelectionForeground(Color.BLACK);
	btnAddRow.setEnabled(false);
	btnDeleteRow.setEnabled(false);
	tpnTables.setEnabled(false);
	menuBar.setEnabled(false);
	table.currentState = UPDATING_STATE;
    }

    protected void switchToInsertingState(SQLiteTable table) {
	table.setSelectionBackground(TABLE_ROW_INSERT_COLOR);
	table.setSelectionForeground(Color.WHITE);
	btnAddRow.setEnabled(false);
	btnDeleteRow.setEnabled(false);
	tpnTables.setEnabled(false);
	menuBar.setEnabled(false);
	table.currentState = INSERTING_STATE;
    }

    protected void switchToErrorUpdatingState(SQLiteTable table, int row) {
	table.setSelectionBackground(TABLE_ROW_ERROR_COLOR);
	table.setSelectionForeground(Color.WHITE);
	table.setRowSelectionInterval(row, row);
	table.scrollRectToVisible(new Rectangle(table.getCellRect(row, 0, true)));
	btnAddRow.setEnabled(false);
	btnDeleteRow.setEnabled(false);
	tpnTables.setEnabled(false);
	menuBar.setEnabled(false);
	table.currentState = ERROR_UPDATING_STATE;
    }

    protected void switchToErrorInsertingState(SQLiteTable table, int row) {
	table.setSelectionBackground(TABLE_ROW_ERROR_COLOR);
	table.setSelectionForeground(Color.WHITE);
	table.setRowSelectionInterval(row, row);
	table.scrollRectToVisible(new Rectangle(table.getCellRect(row, 0, true)));
	btnAddRow.setEnabled(false);
	btnDeleteRow.setEnabled(false);
	tpnTables.setEnabled(false);
	menuBar.setEnabled(false);
	table.currentState = ERROR_INSERTING_STATE;
    }

    protected Color[] getCurrentSelectionColors(JTable table) {
	return isUpdating ? new Color[] { TABLE_ROW_UPDATE_COLOR, Color.BLACK }
		: isInserting ? new Color[] { TABLE_ROW_INSERT_COLOR, Color.WHITE }
			: isErrorHandling ? new Color[] { TABLE_ROW_ERROR_COLOR, Color.WHITE }
				: new Color[] { DEFAULT_TABLE_ROW_SELECTION_COLOR, Color.WHITE };
    }

}
