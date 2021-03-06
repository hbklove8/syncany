package org.syncany.tests.scenarios;

import static org.junit.Assert.assertEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import java.io.File;
import java.util.Map;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class EvilCUpWithoutDownScenarioTest {	
	@Test
	public void testEvilC() throws Exception {
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);
		
		// Run 
		clientA.createNewFile("A1");
		clientA.upWithForceChecksum();
		clientA.moveFile("A1", "A2");
		clientA.upWithForceChecksum();	
		clientA.changeFile("A2");
		clientA.createNewFile("A3");
		clientA.upWithForceChecksum();
		clientA.deleteFile("A3");
		clientA.upWithForceChecksum();
		
		clientB.down();
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		
		clientB.createNewFile("A4,B1");
		clientB.up();
		
		clientC.createNewFile("C1"); // Evil. "up" without "down"
		clientC.changeFile("C1");
		clientC.up();
		clientC.changeFile("C1");
		clientC.up();
		clientC.changeFile("C1");
		clientC.up();
		
		clientA.down();
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		
		Map<String, File> beforeSyncDownBFileList = clientB.getLocalFiles();
		clientB.down();
		assertFileListEquals("No change in file lists expected. Nothing changed for 'B'.", beforeSyncDownBFileList, clientB.getLocalFiles()); 
				
		clientC.down();
		assertEquals("Client C is expected to have one more local file (C1)", clientA.getLocalFiles().size()+1, clientC.getLocalFiles().size());
		
		clientA.up();
		clientB.up();
		clientC.up();
		
		clientA.down();
		clientB.down();
		clientC.down();		
		
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		assertFileListEquals(clientB.getLocalFiles(), clientC.getLocalFiles());		
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
		clientC.cleanup();
	}
}
