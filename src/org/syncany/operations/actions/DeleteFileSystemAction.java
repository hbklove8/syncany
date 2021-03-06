package org.syncany.operations.actions;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.FileVersion;

public class DeleteFileSystemAction extends FileSystemAction {
	public DeleteFileSystemAction(Config config, FileVersion fromFileVersion, FileVersion toDeleteFileVersion, Database localDatabase, Database winningDatabase) {
		super(config, localDatabase, winningDatabase, fromFileVersion, toDeleteFileVersion);
	}
	
	@Override
	public void execute() throws InconsistentFileSystemException, Exception {
		// Special case: locally unknown file to be deleted (= nothing to do!)
		if (fileVersion1 == null) {
			if (fileExists(fileVersion2)) {
				createConflictFile(fileVersion2);				
			}
		}
		
		// Normal case: locally known file to be deleted
		else {
			if (!fileAsExpected(fileVersion1)) {
				if (fileExists(fileVersion1)) {
					createConflictFile(fileVersion2);	
				}
			}
		}
			
		deleteFile(fileVersion2);
	}

	@Override
	public String toString() {
		return "DeleteFileSystemAction [file1=" + fileVersion1 + ", file2=" + fileVersion2 + "]";
	}
}

