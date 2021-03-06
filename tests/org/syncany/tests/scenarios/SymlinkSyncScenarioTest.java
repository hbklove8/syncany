package org.syncany.tests.scenarios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;
import org.syncany.operations.StatusOperation.StatusOperationResult;
import org.syncany.operations.UpOperation.UpOperationResult;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.util.FileUtil;

public class SymlinkSyncScenarioTest {
	@Test
	public void testSymlinkOneUpOneDown() throws Exception {
		if (!FileUtil.symlinksSupported()) {			
			return; // Skip test for Windows, no symlinks there!
		}
		
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		// Run 
		File symlinkFile = clientA.getLocalFile("symlink-name");
		FileUtil.createSymlink("/etc/hosts", symlinkFile);		
		
		assertTrue("Symlink should exist at "+symlinkFile, symlinkFile.exists());
		
		UpOperationResult upResult = clientA.up();
		StatusOperationResult statusResult = upResult.getStatusResult();
		
		// Test 1: Check result sets for inconsistencies
		assertTrue("Status should return changes.", statusResult.getChangeSet().hasChanges());
		assertTrue("File should be uploaded.", upResult.getChangeSet().hasChanges());
		
		// Test 2: Check database for inconsistencies
		Database database = clientA.loadLocalDatabase();
		DatabaseVersion databaseVersion = database.getLastDatabaseVersion();

		assertNotNull("File should be uploaded.", database.getFileHistory("symlink-name"));		
		assertNotNull("There should be a new database version, because file should not have been added.", databaseVersion);
		
		// Test 3: Check file system for inconsistencies
		File repoPath = ((LocalConnection) testConnection).getRepositoryPath();		
		assertEquals("Repository should contain only ONE database file, not multichunks.", 1, repoPath.list().length);	
		
		// B down
		clientB.down();
		assertEquals("Local folder should contain one file (link!)", 1, clientB.getLocalFiles().size());
		
		File localSymlinkFile = clientB.getLocalFile("symlink-name");
		assertTrue("Local symlink file should exist.", localSymlinkFile.exists());
		assertTrue("Local symlink file should be a SYMLINK.", FileUtil.isSymlink(localSymlinkFile));
		assertEquals("Local symlink file should point to actual target.", "/etc/hosts", FileUtil.readSymlinkTarget(localSymlinkFile));
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}
	
	@Test
	public void testSymlinkMultipleUpsAndDowns() throws Exception {
		if (!FileUtil.symlinksSupported()) {			
			return; // Skip test for Windows, no symlinks there!
		}

		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		// Run 
		clientA.createNewFile("symlink-target");

		File symlinkFile = clientA.getLocalFile("symlink-name");
		FileUtil.createSymlink("symlink-target", symlinkFile); // << relative target	
		
		assertTrue("Symlink should exist at "+symlinkFile, symlinkFile.exists());
		
		clientA.up();

		// B down
		clientB.down();
		assertEquals("Local folder should contain two files (symlink and target!)", 2, clientB.getLocalFiles().size());
		
		File localSymlinkFile = clientB.getLocalFile("symlink-name");
		
		assertTrue("Local symlink file should exist.", Files.exists(Paths.get(localSymlinkFile.getAbsolutePath()), LinkOption.NOFOLLOW_LINKS));
		assertTrue("Local symlink file should be a SYMLINK.", FileUtil.isSymlink(localSymlinkFile));
		assertEquals("Local symlink file should point to actual target.", "symlink-target", FileUtil.readSymlinkTarget(localSymlinkFile));
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}
	
	@Test
	public void testSymlinkSyncToNonExistingFolder() throws Exception {
		if (!FileUtil.symlinksSupported()) {			
			return; // Skip test for Windows, no symlinks there!
		}

		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		// A 
		clientA.createNewFolder("folder1");		
		clientA.up();

		// B
		clientB.down();
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		
		// A
		File symlinkFile = clientA.getLocalFile("folder1/symlink-name");
		FileUtil.createSymlink("/does/not/exist", symlinkFile); // << relative target	
		clientA.up();
		
		// B 
		clientB.deleteFile("folder1");
		clientB.down();
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}
}
