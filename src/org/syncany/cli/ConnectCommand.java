package org.syncany.cli;

import static java.util.Arrays.asList;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.apache.commons.codec.binary.Base64;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.ConfigTO.ConnectionTO;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.crypto.CipherUtil;
import org.syncany.operations.ConnectOperation.ConnectOperationListener;
import org.syncany.operations.ConnectOperation.ConnectOperationOptions;
import org.syncany.operations.ConnectOperation.ConnectOperationResult;

public class ConnectCommand extends AbstractInitCommand implements ConnectOperationListener {
	public static final Pattern LINK_PATTERN = Pattern.compile("^syncany://storage/1/(not-encrypted/)?(.+)$");	
	
	private String password;
	
	public ConnectCommand() {
		super();
	}

	@Override
	public boolean initializedLocalDirRequired() {	
		return false;
	}
	
	@Override
	public int execute(String[] operationArgs) throws Exception {
		ConnectOperationOptions operationOptions = parseConnectOptions(operationArgs);
		ConnectOperationResult operationResult = client.connect(operationOptions, this);
		
		printResults(operationResult);
		
		return 0;		
	}

	private ConnectOperationOptions parseConnectOptions(String[] operationArguments) throws OptionException, Exception {
		ConnectOperationOptions operationOptions = new ConnectOperationOptions();

		OptionParser parser = new OptionParser();			
		OptionSpec<String> optionPlugin = parser.acceptsAll(asList("p", "plugin")).withRequiredArg();
		OptionSpec<String> optionPluginOpts = parser.acceptsAll(asList("P", "plugin-option")).withRequiredArg();
		
		OptionSet options = parser.parse(operationArguments);	
		List<?> nonOptionArgs = options.nonOptionArguments();		
		
		// Plugin
		ConnectionTO connectionTO = new ConnectionTO();
		
		if (nonOptionArgs.size() == 1) {
			connectionTO = initPluginWithLink((String) nonOptionArgs.get(0));			
		}
		else if (options.has(optionPlugin)) {
			connectionTO = initPluginWithOptions(options, optionPlugin, optionPluginOpts);
		}
		else if (nonOptionArgs.size() == 0) {
			String pluginStr = askPlugin();
			Map<String, String> pluginSettings = askPluginSettings(pluginStr);

			connectionTO.setType(pluginStr);
			connectionTO.setSettings(pluginSettings);
		}
		else {
			throw new Exception("Invalid syntax.");
		}
		
		ConfigTO configTO = createConfigTO(localDir, password, connectionTO);
		
		operationOptions.setLocalDir(localDir);
		operationOptions.setConfigTO(configTO);
		
		return operationOptions;		
	}	

	private void printResults(ConnectOperationResult operationResult) {
		// Nothing
	}
	
	private ConnectionTO initPluginWithLink(String link) throws Exception {
		Matcher linkMatcher = LINK_PATTERN.matcher(link);
		
		if (!linkMatcher.matches()) {
			throw new Exception("Invalid link provided, must start with syncany:// and match link pattern.");
		}
		
		String notEncryptedFlag = linkMatcher.group(1);
		String ciphertext = linkMatcher.group(2);
		String plaintext = null;
		
		boolean isEncryptedLink = notEncryptedFlag == null;
		
		if (isEncryptedLink) {
			password = askPassword();
			
			byte[] ciphertextBytes = Base64.decodeBase64(ciphertext);
			ByteArrayInputStream encryptedStorageConfig = new ByteArrayInputStream(ciphertextBytes);
			
			plaintext = CipherUtil.decryptToString(encryptedStorageConfig, password);					
		}
		else {
			plaintext = new String(Base64.decodeBase64(ciphertext));
		}
		
		//System.out.println(plaintext);

		Serializer serializer = new Persister();
		ConnectionTO connectionTO = serializer.read(ConnectionTO.class, plaintext);		
		
		Plugin plugin = Plugins.get(connectionTO.getType());
		
		if (plugin == null) {
			throw new Exception("Link contains unknown connection type '"+connectionTO.getType()+"'. Corresponding plugin not found.");
		}
		
		return connectionTO;			
	}

	private String askPassword() { 
		String password = null;
		
		while (password == null) {
			char[] passwordChars = console.readPassword("Password: ");			
			password = new String(passwordChars);			
		}	
		
		return password;
	}

	@Override
	public String getPasswordCallback() {
		return askPassword();
	}
}
