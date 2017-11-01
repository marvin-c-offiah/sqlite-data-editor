# SQLite Data Editor

## Functionality

SQLite Data Editor is a simple GUI for editing only the table data in a given SQLite database file via a simple light-weight table editor. Makes changes to the database on DML-level only.  Makes no DDL-based (schema or metadata etc.) changes to the database. Database changes happen only by editing the contents in the table editor. No direct SQL-access in the GUI. Saving the table editor edits results in a simple clean and rewrite of all table data in the database, replacing it with the current data in the table editor.

## Recommended for

The limitations make editing SQLite data very intuitive for laymen without any knowledge or interest in all the possibilities of the underlying SQLite and database technology. This allows the GUI to be very light-weight and transparent, making it appear like editing a simple Excel-file: No complex views or menues for schema, indices, views, triggers, SQL input etc. Just some JTables in a Tab Pane, Open File, Save.

Recommended for providing a universal off-line configuration file editor for applications (e.g., for launch configs): Instead of laboriously implementing the parsing of multiple or syntactically complicated plain text config files, developers can think about implementing the reading of config data from an API-friendly SQLite database file. The arising downside of the end user no longer being able to edit the config with a simple text editor is then solved by this tool, since it presents a simple table editor to the user instead. It removes the additional need to implement a user-friendly application-specific GUI-editor for making configurations.
