package org.syncany.tests.scenarios.framework;

import java.io.File;

import org.syncany.util.FileUtil;

public class ChangeTypeFileToSymlinkWithNonExistingTarget extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		if (!FileUtil.symlinksSupported()) {
			return; // no symbolic links on Windows
		}
		
		File file = pickFile(2111);
		
		log(this, file.getAbsolutePath());
		
		file.delete();
		FileUtil.createSymlink("/does/not/exist", file);
	}		
}	
